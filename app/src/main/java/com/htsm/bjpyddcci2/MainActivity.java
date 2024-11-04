package com.htsm.bjpyddcci2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.htsm.bjpyddcci2.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'bjpyddcci2' library on application startup.
    static {
        System.loadLibrary("bjpyddcci2");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'bjpyddcci2' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    // 获取显示器的亮度
    public static native int getDPBrightness();

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