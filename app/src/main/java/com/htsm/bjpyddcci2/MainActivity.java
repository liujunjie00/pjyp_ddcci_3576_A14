package com.htsm.bjpyddcci2;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SeekBar br,sound,batt;
    private TextView charing;
    private final static String i2c_addr = "/dev/i2c-10";
    private MediaPlayer mediaPlayer;

    // Used to load the 'bjpyddcci2' library on application startup.
    static {
        System.loadLibrary("bjpyddcci2");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initView();
        initStatus();
    }

    private void initStatus() {
        /*BatteryStatus batteryStatus = new BatteryStatus();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryStatus, filter);*/

        Intent intent = new Intent();
        intent.setClass(this,MyIntentService.class);
        startService(intent);

    }

/*    private void initView() {
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
       // initjunjie(getApplicationContext());
    }*/





}