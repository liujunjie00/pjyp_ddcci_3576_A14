#include <jni.h>
#include <string>


extern "C" JNIEXPORT jstring JNICALL
Java_com_htsm_bjpyddcci2_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MainActivity_getDPBrightness(JNIEnv *env, jclass clazz) {
    // TODO: implement getDPBrightness()
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MainActivity_setDPBrightness(JNIEnv *env, jclass clazz, jint value) {
    // TODO: implement setDPBrightness()
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MainActivity_getDPSound(JNIEnv *env, jclass clazz) {
    // TODO: implement getDPSound()
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MainActivity_setDPSound(JNIEnv *env, jclass clazz, jint value) {
    // TODO: implement setDPSound()
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MainActivity_getChargingStatus(JNIEnv *env, jclass clazz) {
    // TODO: implement getChargingStatus()
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_htsm_bjpyddcci2_MainActivity_getCurrentBatteryLevel(JNIEnv *env, jclass clazz) {
    // TODO: implement getCurrentBatteryLevel()
}