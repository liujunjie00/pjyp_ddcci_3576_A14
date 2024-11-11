package com.htsm.bjpyddcci2;

import static com.htsm.bjpyddcci2.SystemUtil.execShellCmdForRoot;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SeekBar br,sound,batt;
    private TextView charing;
    private final static String i2c_addr = "/dev/i2c-10";

    // Used to load the 'bjpyddcci2' library on application startup.
    static {
        System.loadLibrary("bjpyddcci2");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setI2c777();
        initView();
    }

    /**
     * debug 版本 配置 */
    private void setI2c777() {
        String st = execShellCmdForRoot("chown system:system "+i2c_addr);
        Log.d(TAG, "setI2c777: st"+st);


    }

    private void initView() {
        br = findViewById(R.id.screen_br);
        sound = findViewById(R.id.screen_sound);
        batt = findViewById(R.id.screen_batt_status);
        charing = findViewById(R.id.screen_batt_charing);

        br.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: 亮度调节:"+progress);
                setDPBrightness(progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: 声音调节:"+progress);
                setDPSound(progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        int battLevel = getCurrentBatteryLevel();
        batt.setProgress(battLevel);
        int br_i = getDPBrightness();
        br.setProgress(br_i);
        int sound_i = getDPSound();
        sound.setProgress(sound_i);
        int cur = getChargingStatus();
        charing.setText(cur >0?"正在充电":"正在待机");
    }


    /**
     * A native method that is implemented by the 'bjpyddcci2' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    // 获取显示器的亮度
    public static native int getDPBrightness(); //./dcccibin -a 0x37 -r 0x10 /dev/i2c-10

    // 设置显示器的亮度
    public static native int setDPBrightness(int value);

    // 获取显示器的声音大小
    public static native int getDPSound();

    // 设置显示器的 大小
    public static native int setDPSound(int value);

    // 获取当前的充电状态
    public static native  int getChargingStatus();

    // 获取当前电量
    public static native int getCurrentBatteryLevel();
}