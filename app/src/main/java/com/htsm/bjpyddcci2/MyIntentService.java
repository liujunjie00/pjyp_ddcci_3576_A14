package com.htsm.bjpyddcci2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
public class MyIntentService extends Service {
    private static final String TAG = "MyIntentService";
    private int brightness =0; // 保存当前机器的亮度值
    private boolean DPMode = false; // 标志亮度值是否保存

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initDpconfig();
        initStatus();
        initManger();
    }

    /**
     * 初始化dp 标识位
     * */
    private void initDpconfig() {
    }


    private Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isDPConnect() && !DPMode){
                try {
                    brightness = Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS);
                } catch (Settings.SettingNotFoundException e) {
                    throw new RuntimeException(e);
                }
                int dpBrightness = MainActivity.getDPBrightness(); //100
                int st = Math.round((float) dpBrightness /100 * 255);
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,st);
                Log.d(TAG, "G1ConnectStatus: 切换到dp 模式 配置显示器亮度："+dpBrightness+" , 保存平板状态："+brightness);
                DPMode = true;
            }else if (!isDPConnect() && DPMode){
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,brightness);
                Log.d(TAG, "G1ConnectStatus: 切换到平板 模式 配置显示器亮度："+brightness);
                DPMode = false;

            }

        }
    };

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
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable,2*1000);
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