package com.htsm.bjpyddcci2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
public class MyIntentService extends Service {
    public static final String screen_brightness = "screen_brightness";
    private int brightness =0; // 保存当前机器的亮度值

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStatus();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void initStatus() {
        BatteryStatus batteryStatus = new BatteryStatus();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryStatus, filter);
        batteryStatus.setConnectInterface(new BatteryStatus.G1ConnectInterface() {
            @Override
            public void G1ConnectStatus(boolean isConnect) {
                if (isConnect){

                    try {
                        brightness =   Settings.System.getInt(getContentResolver(),screen_brightness);
                    } catch (Settings.SettingNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    int dpBrightness = MainActivity.getDPBrightness();
                    Settings.System.putInt(getContentResolver(),screen_brightness,dpBrightness);
                }else {
                    Settings.System.putInt(getContentResolver(),screen_brightness,brightness);

                }

            }
        });
        BrightnessObserver brightnessObserver = new BrightnessObserver(getApplicationContext());
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                true,
                brightnessObserver
        );

    }
    private boolean isDPConnect(){
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return batteryManager.isCharging() && !BatteryStatus.getWindowIsG1(getApplicationContext());

    }

    class BrightnessObserver extends ContentObserver {
        private Context mContext;

        public BrightnessObserver(Context context) {
            super(null);
            mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int brightness = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    0
            );

            handleBrightnessChange(brightness);
        }

        private void handleBrightnessChange(int brightness) {
            if (isDPConnect()){
                MainActivity.setDPBrightness(brightness);
            }

        }
    }


}