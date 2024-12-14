#include <jni.h>
#include <string>

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <time.h>

#include "i2c-dev.h"

/* ddc/ci defines */
#define DDCCI_COMMAND_READ	0x01	/* read ctrl value */
#define DDCCI_REPLY_READ	0x02	/* read ctrl value reply */
#define DDCCI_COMMAND_WRITE	0x03	/* write ctrl value */

#define DDCCI_COMMAND_SAVE	0x0c	/* save current settings */

#define DDCCI_REPLY_CAPS	0xe3	/* get monitor caps reply */
#define DDCCI_COMMAND_CAPS	0xf3	/* get monitor caps */
#define DDCCI_COMMAND_PRESENCE	0xf7	/* ACCESS.bus presence check */

/* control numbers */
#define DDCCI_CTRL_BRIGHTNESS	0x10

/* samsung specific, magictune starts with writing 1 to this register */
#define DDCCI_CTRL		0xf5
#define DDCCI_CTRL_ENABLE	0x0001
#define DDCCI_CTRL_DISABLE	0x0000

/* ddc/ci iface tunables */
#define DEFAULT_DDCCI_ADDR	0x37	/* samsung ddc/ci logic sits at 0x37 */
#define MAX_BYTES		127	/* max message length */
#define DELAY   		30000	/* uS to wait after write */
#define RETRYS			3	/* number of retry */

/* magic numbers */
#define MAGIC_1	0x51	/* first byte to send, host address */
#define MAGIC_2	0x80	/* second byte to send, ored with length */
#define MAGIC_XOR 0x50	/* initial xor for received frame */

static int verbosity = 0;

/* debugging */
void dumphex(FILE *f, unsigned char *buf, unsigned char len)
{
    int i, j;

    for (j = 0; j < len; j +=16) {
        if (len > 16) {
            fprintf(f, "%04x: ", j);
        }

        for (i = 0; i < 16; i++) {
            if (i + j < len) fprintf(f, "%02x ", buf[i + j]);
            else fprintf(f, "   ");
        }

        fprintf(f, "| ");

        for (i = 0; i < 16; i++) {
            if (i + j < len) fprintf(f, "%c",
                                     buf[i + j] >= ' ' && buf[i + j] < 127 ? buf[i + j] : '.');
            else fprintf(f, " ");
        }

        fprintf(f, "\n");
    }
}

/* write len bytes (stored in buf) to i2c address addr */
/* return 0 on success, -1 on failure */
int i2c_write(int fd, unsigned int addr,  char *buf, unsigned char len)
{
    int i;
    struct i2c_rdwr_ioctl_data msg_rdwr;
    struct i2c_msg             i2cmsg;

    /* done, prepare message */
    msg_rdwr.msgs = &i2cmsg;
    msg_rdwr.nmsgs = 1;

    i2cmsg.addr  = addr;
    i2cmsg.flags = 0;
    i2cmsg.len   = len;
    i2cmsg.buf   = buf;

    if ((i = ioctl(fd, I2C_RDWR, &msg_rdwr)) < 0 )
    {
        perror("ioctl()");
        fprintf(stderr,"ioctl returned %d\n",i);
        return -1;
    }

    return i;

}

/* read at most len bytes from i2c address addr, to buf */
/* return -1 on failure */
int i2c_read(int fd, unsigned int addr, char *buf, unsigned char len)
{
    struct i2c_rdwr_ioctl_data msg_rdwr;
    struct i2c_msg             i2cmsg;
    int i;

    msg_rdwr.msgs = &i2cmsg;
    msg_rdwr.nmsgs = 1;

    i2cmsg.addr  = addr;
    i2cmsg.flags = I2C_M_RD;
    i2cmsg.len   = len;
    i2cmsg.buf   = buf;

    if ((i = ioctl(fd, I2C_RDWR, &msg_rdwr)) < 0)
    {
        perror("ioctl()");
        fprintf(stderr,"ioctl returned %d\n",i);
        return -1;
    }

    return i;
}


/* write len bytes (stored in buf) to ddc/ci at address addr */
/* return 0 on success, -1 on failure */
int ddcci_write(int fd, unsigned int addr,  char *buf, unsigned char len)
{
    int i = 0;
    unsigned char _buf[MAX_BYTES + 3];
    unsigned xor1 = ((unsigned char)addr << 1);	/* initial xor value */

    if (verbosity > 1) {
        fprintf(stderr, "Send: ");
        dumphex(stderr, reinterpret_cast<unsigned char *>(buf), len);
    }

    /* put first magic */
    xor1 ^= (_buf[i++] = MAGIC_1);

    /* second magic includes message size */
    xor1 ^= (_buf[i++] = MAGIC_2 | len);

    while (len--) /* bytes to send */
        xor1 ^= (_buf[i++] = *buf++);

    /* finally put checksum */
    _buf[i++] = xor1;

    return i2c_write(fd, addr, reinterpret_cast<char *>(_buf), i);
}

/* read ddc/ci formatted frame from ddc/ci at address addr, to buf */
int ddcci_read(int fd, unsigned int addr, unsigned char *buf, unsigned char len)
{
    unsigned char _buf[MAX_BYTES];
    unsigned char xor1 = MAGIC_XOR;
    int i, _len;

    if (i2c_read(fd, addr, reinterpret_cast<char *>(_buf), len + 3) <= 0 ||
        _buf[0] == 0x51 || _buf[0] == 0xff) // busy ???
    {
        return -1;
    }

    /* validate answer */
    if (_buf[0] != addr * 2) {
        dumphex(stderr, _buf, sizeof(_buf));
        fprintf(stderr, "Invalid response, first byte is 0x%02x, should be 0x%02x\n",
                _buf[0], addr * 2);
        return -1;
    }

    if ((_buf[1] & MAGIC_2) == 0) {
        fprintf(stderr, "Invalid response, magic is 0x%02x\n", _buf[1]);
        return -1;
    }

    _len = _buf[1] & ~MAGIC_2;
    if (_len > len || _len > sizeof(_buf)) {
        fprintf(stderr, "Invalid response, length is %d, should be %d at most\n",
                _len, len);
        return -1;
    }

    /* get the xor value */
    for (i = 0; i < _len + 3; i++) {
        xor1 ^= _buf[i];
    }

    if (xor1 != 0) {
        fprintf(stderr, "Invalid response, corrupted data - xor is 0x%02x, length 0x%02x\n", xor1, _len);
        for (i = 0; i < _len + 3; i++) {
            fprintf(stderr, "0x%02x ", _buf[i]);
        }
        fprintf(stderr, "\n");

        return -1;
    }

    /* copy payload data */
    memcpy(buf, _buf + 2, _len);

    if (verbosity > 1) {
        fprintf(stderr, "Recv: ");
        dumphex(stderr, buf, _len);
    }

    return _len;
}


/* write value to register ctrl of ddc/ci at address addr */
int ddcci_writectrl(int fd, unsigned int addr, unsigned char ctrl, unsigned short value)
{
    unsigned char buf[4];

    buf[0] = DDCCI_COMMAND_WRITE;
    buf[1] = ctrl;
    buf[2] = (value >> 8);
    buf[3] = (value & 255);

    return ddcci_write(fd, addr, reinterpret_cast<char *>(buf), sizeof(buf));
}

/* read register ctrl raw data of ddc/ci at address addr */
int ddcci_readctrl(int fd, unsigned int addr,
                   unsigned char ctrl, unsigned char *buf, unsigned char len)
{
    unsigned char _buf[2];

    _buf[0] = DDCCI_COMMAND_READ;
    _buf[1] = ctrl;

    if (ddcci_write(fd, addr, reinterpret_cast<char *>(_buf), sizeof(_buf)) < 0)
    {
        return -1;
    }

    usleep(DELAY);

    return ddcci_read(fd, addr, buf, len);
}
/* read capabilities raw data of ddc/ci at address addr starting at offset to buf */
int ddcci_caps(int fd, unsigned int addr,
               unsigned int offset, unsigned char *buf, unsigned char len)
{
    unsigned char _buf[3];

    _buf[0] = DDCCI_COMMAND_CAPS;
    _buf[1] = offset >> 8;
    _buf[2] = offset & 255;

    if (ddcci_write(fd, addr, reinterpret_cast<char *>(_buf), sizeof(_buf)) < 0)
    {
        return -1;
    }

    usleep(DELAY);

    return ddcci_read(fd, addr, buf, len);
}

/* save current settings */
int ddcci_command(int fd, unsigned int addr, unsigned char cmd)
{
    unsigned char _buf[1];

    _buf[0] = cmd;

    return ddcci_write(fd, addr, reinterpret_cast<char *>(_buf), sizeof(_buf));
}
/* get ctrlname based on id */
char *ctrlname(unsigned char ctrl)
{
    switch (ctrl) {
        case 0x00: return "Degauss";	/* ACCESS.bus */
        case 0x01: return "Degauss";	/* USB */
        case 0x02: return "Secondary Degauss";	/* ACCESS.bus */
        case 0x04: return "Reset Factory Defaults";
        case 0x05: return "SAM: Reset Brightness and Contrast";	/* ??? */
        case 0x06: return "Reset Factory Geometry";
        case 0x08: return "Reset Factory Default Color";	/* ACCESS.bus */
        case 0x0a: return "Reset Factory Default Position";	/* ACCESS.bus */
        case 0x0c: return "Reset Factory Default Size";		/* ACCESS.bus */
        case 0x0e: return "SAM: Image Lock Coarse";	/* ??? */
        case 0x10: return "Brightness";
        case 0x12: return "Contrast";
        case 0x14: return "Select Color Preset";	/* ACCESS.bus */
        case 0x16: return "Red Video Gain";
        case 0x18: return "Green Video Gain";
        case 0x1a: return "Blue Video Gain";
        case 0x1c: return "Focus";	/* ACCESS.bus */
        case 0x1e: return "SAM: Auto Size Center";	/* ??? */
        case 0x20: return "Horizontal Position";
        case 0x22: return "Horizontal Size";
        case 0x24: return "Horizontal Pincushion";
        case 0x26: return "Horizontal Pincushion Balance";
        case 0x28: return "Horizontal Misconvergence";
        case 0x2a: return "Horizontal Linearity";
        case 0x2c: return "Horizontal Linearity Balance";
        case 0x30: return "Vertical Position";
        case 0x32: return "Vertical Size";
        case 0x34: return "Vertical Pincushion";
        case 0x36: return "Vertical Pincushion Balance";
        case 0x38: return "Vertical Misconvergence";
        case 0x3a: return "Vertical Linearity";
        case 0x3c: return "Vertical Linearity Balance";
        case 0x3e: return "SAM: Image Lock Fine";	/* ??? */
        case 0x40: return "Parallelogram Distortion";
        case 0x42: return "Trapezoidal Distortion";
        case 0x44: return "Tilt (Rotation)";
        case 0x46: return "Top Corner Distortion Control";
        case 0x48: return "Top Corner Distortion Balance";
        case 0x4a: return "Bottom Corner Distortion Control";
        case 0x4c: return "Bottom Corner Distortion Balance";
        case 0x50: return "Hue";	/* ACCESS.bus */
        case 0x52: return "Saturation";	/* ACCESS.bus */
        case 0x54: return "Color Curve Adjust";	/* ACCESS.bus */
        case 0x56: return "Horizontal Moire";
        case 0x58: return "Vertical Moire";
        case 0x5a: return "Auto Size Center Enable/Disable";	/* ACCESS.bus */
        case 0x5c: return "Landing Adjust";	/* ACCESS.bus */
        case 0x5e: return "Input Level Select";	/* ACCESS.bus */
        case 0x60: return "Input Source Select";
        case 0x62: return "Audio Speaker Volume Adjust";	/* ACCESS.bus */
        case 0x64: return "Audio Microphone Volume Adjust";	/* ACCESS.bus */
        case 0x66: return "On Screen Display Enable/Disable";	/* ACCESS.bus */
        case 0x68: return "Language Select";	/* ACCESS.bus */
        case 0x6c: return "Red Video Black Level";
        case 0x6e: return "Green Video Black Level";
        case 0x70: return "Blue Video Black Level";
        case 0xa2: return "Auto Size Center";	/* USB */
        case 0xa4: return "Polarity Horizontal Synchronization";	/* USB */
        case 0xa6: return "Polarity Vertical Synchronization";	/* USB */
        case 0xa8: return "Synchronization Type";	/* USB */
        case 0xaa: return "Screen Orientation";	/* USB */
        case 0xac: return "Horizontal Frequency";	/* USB */
        case 0xae: return "Vertical Frequency";	/* USB */
        case 0xb0: return "Settings";
/*	case 0xb6: return "b6 r/o";
	case 0xc6: return "c6 r/o";
	case 0xc8: return "c8 r/o";
	case 0xc9: return "c9 r/o";*/
        case 0xca: return "On Screen Display";	/* USB */
        case 0xcc: return "SAM: On Screen Display Language";	/* ??? */
        case 0xd4: return "Stereo Mode";	/* USB */
        case 0xd6: return "SAM: DPMS control (1 - on/4 - stby)";
        case 0xdc: return "SAM: MagicBright (1 - text/2 - internet/3 - entertain/4 - custom)";
        case 0xdf: return "VCP Version";	/* ??? */
        case 0xe0: return "SAM: Color preset (0 - normal/1 - warm/2 - cool)";
        case 0xe1: return "SAM: Power control (0 - off/1 - on)";
/*	case 0xe2: return "e2 r/w";*/
        case 0xed: return "SAM: Red Video Black Level";
        case 0xee: return "SAM: Green Video Black Level";
        case 0xef: return "SAM: Blue Video Black Level";
        case 0xf5: return "SAM: VCP Enable";
    }

    return "???";
}
int ddcci_dumpctrl(int fd, unsigned int addr,
                   unsigned char ctrl, int force)
{
    unsigned char buf[8];

    int len = ddcci_readctrl(fd, addr, ctrl, buf, sizeof(buf));

    if (len == sizeof(buf) && buf[0] == DDCCI_REPLY_READ &&
        buf[2] == ctrl && (force || !buf[1])) /* buf[1] is validity (0 - valid, 1 - invalid) */
    {
        int current = buf[6] * 256 + buf[7];
        int maximum = buf[4] * 256 + buf[5];

        fprintf(stdout, "Control 0x%02x: %c/%d/%d\t[%s]\n", ctrl,
                buf[1] ? '-' : '+',  current, maximum, ctrlname(ctrl));
        if (verbosity) {
            fprintf(stderr, "Raw: ");
            dumphex(stderr, buf, sizeof(buf));
        }
        if (current != -1){
            return current;
        }
    }

    return len;
}

static  char *fn = "/dev/i2c-10";
static  int  addr_ctr;
extern "C"
JNIEXPORT jstring JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

//./dcccibin -a 0x37 -r 0x10 /dev/i2c-10
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_getDPBrightness(JNIEnv *env, jclass clazz) {
    // TODO: implement getDPBrightness()
    int fd;
    jint re;
    addr_ctr = 0x10;
    // 打开节点
    if ((fd = open(fn, O_RDWR)) < 0)
    {
        perror(fn);
        fprintf(stderr, "Be sure you've modprobed i2c-dev and correct i2c device.\n");
        return -1;
    }
    re = ddcci_dumpctrl(fd,DEFAULT_DDCCI_ADDR,addr_ctr,1);
    close(fd);


    return re;

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_setDPBrightness(JNIEnv *env, jclass clazz, jint value) {
    // TODO: implement setDPBrightness()  ddcci_writectrl(fd, addr, ctrl, value);
    int fd;
    jint re;
    addr_ctr = 0x10;
    // 打开节点
    if ((fd = open(fn, O_RDWR)) < 0)
    {
        perror(fn);
        fprintf(stderr, "Be sure you've modprobed i2c-dev and correct i2c device.\n");
        return -1;
    }
    re = ddcci_writectrl(fd,DEFAULT_DDCCI_ADDR,addr_ctr,value);
    close(fd);

    return re;

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_getDPSound(JNIEnv *env, jclass clazz) {
    // TODO: implement getDPSound()
    int fd;
    jint re;
    addr_ctr = 0x62;
    // 打开节点
    if ((fd = open(fn, O_RDWR)) < 0)
    {
        perror(fn);
        fprintf(stderr, "Be sure you've modprobed i2c-dev and correct i2c device.\n");
        return -1;
    }
    re = ddcci_dumpctrl(fd,DEFAULT_DDCCI_ADDR,addr_ctr,1);
    close(fd);

    return re;

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_setDPSound(JNIEnv *env, jclass clazz, jint value) {
    // TODO: implement setDPSound()
    int fd;
    jint re;
    addr_ctr = 0x62;
    // 打开节点
    if ((fd = open(fn, O_RDWR)) < 0)
    {
        perror(fn);
        fprintf(stderr, "Be sure you've modprobed i2c-dev and correct i2c device.\n");
        return -1;
    }
    re = ddcci_writectrl(fd,DEFAULT_DDCCI_ADDR,addr_ctr,value);
    close(fd);

    return re;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_getChargingStatus(JNIEnv *env, jclass clazz) {
    int fd;
    jint re;
    addr_ctr = 0x65;
    // 打开节点
    if ((fd = open(fn, O_RDWR)) < 0) // 需要关闭selinux
    {
        perror(fn);
        fprintf(stderr, "Be sure you've modprobed i2c-dev and correct i2c device.\n");
        return -1;
    }
    re = ddcci_dumpctrl(fd,DEFAULT_DDCCI_ADDR,addr_ctr,1);
    close(fd);

    return re;
    // TODO: implement getChargingStatus()
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_getCurrentBatteryLevel(JNIEnv *env, jclass clazz) {
    int fd;
    jint re;
    addr_ctr = 0x61;
    // 打开节点
    if ((fd = open(fn, O_RDWR)) < 0)
    {
        perror(fn);
        fprintf(stderr, "Be sure you've modprobed i2c-dev and correct i2c device.\n");
        return -1;
    }
    re = ddcci_dumpctrl(fd,DEFAULT_DDCCI_ADDR,addr_ctr,1);
    close(fd);

    return re;
    // TODO: implement getCurrentBatteryLevel()
}


extern "C"
JNIEXPORT void JNICALL
Java_com_htsm_bjpyddcci2_MyIntentService_setBacklightSwitch(JNIEnv *env, jclass clazz,jint value) {
    // TODO: implement setBacklightSwitch()
    int fd;
    addr_ctr = 0x67;
    // 打开节点
    if ((fd = open(fn, O_RDWR)) < 0)
    {
        perror(fn);
        fprintf(stderr, "Be sure you've modprobed i2c-dev and correct i2c device.\n");
        return ;
    }
    ddcci_writectrl(fd,DEFAULT_DDCCI_ADDR,addr_ctr,value);
    close(fd);
}