package com.htsm.bjpyddcci2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import androidx.annotation.Nullable;
public class MyIntentService extends Service {
    public static final String screen_brightness = "screen_brightness";
    private static final String TAG = "MyIntentService";
    private int brightness =0; // 保存当前机器的亮度值
    private boolean DpModel = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStatus();
        initManger();
    }

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            Log.d(TAG, "onDisplayAdded: "+displayId);

        }

        @Override
        public void onDisplayRemoved(int displayId) {
            Log.d(TAG, "onDisplayRemoved: "+displayId);

        }

        @Override
        public void onDisplayChanged(int displayId) {

            Display display = displayManager.getDisplay(displayId);
            int height = display.getHeight();
            int width = display.getWidth();
            Log.d(TAG, "onDisplayChanged: "+displayId+ "   , height:"+height+" ,width:"+width);
            boolean isG1 =  width == 720 || height == 1440 || width == 1440 || height == 720;
            boolean isConnect = !isG1;
            if (isConnect ){
                try {
                    brightness =   Settings.System.getInt(getContentResolver(),screen_brightness);
                } catch (Settings.SettingNotFoundException e) {
                    throw new RuntimeException(e);
                }
                int dpBrightness = MainActivity.getDPBrightness();
                Settings.System.putInt(getContentResolver(),screen_brightness,dpBrightness);
                Log.d(TAG, "G1ConnectStatus: 切换到dp 模式 配置显示器亮度："+dpBrightness+" , 保存平板状态："+brightness);
            }else {
                    Settings.System.putInt(getContentResolver(),screen_brightness,brightness);
                    Log.d(TAG, "G1ConnectStatus: 切换到平板 模式 配置显示器亮度："+brightness);

            }


        }
    };
    DisplayManager displayManager;
    private void initManger() {

        displayManager = getApplicationContext().getSystemService(DisplayManager.class);
        displayManager.registerDisplayListener(displayListener,null);
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

                int st = Math.round((float) brightness /255 * 100);
                Log.d(TAG, "handleBrightnessChange: " +st);
                MainActivity.setDPBrightness(st);
            }

        }
    }


}