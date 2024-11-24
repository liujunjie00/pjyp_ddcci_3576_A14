package com.htsm.bjpyddcci2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private static final String G1_DP_BATTERY_STATUS = "g1dpbatterystatus";
    private static final String G1_DP_BATTERY_LEVEL = "g1dpbatterylevel";
    private static final int G1_DP_NOT_CONNECT = -1;
    DisplayManager displayManager;
    private final Handler handler = new Handler();
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())){
                MainActivity.setBacklightSwitch(0);
                Log.d(TAG, "onReceive: 打开dp 显示器背光");

            }else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
                MainActivity.setBacklightSwitch(1);
                Log.d(TAG, "onReceive: 关闭dp 显示器背光");
            }

        }
    };

    private final Runnable readBatteryStatsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDPConnect()){
                int batteryLevel = MainActivity.getCurrentBatteryLevel();
                int BatteryStatus = MainActivity.getChargingStatus();  // 1是待机 2是充电
                Log.d(TAG, "run: readBatteryStatsRunnable G1 写入 Settings  batteryLevel :" +batteryLevel +", BatteryStatus:"+BatteryStatus);
                Settings.System.putInt(getContentResolver(),G1_DP_BATTERY_LEVEL,batteryLevel);
                Settings.System.putInt(getContentResolver(),G1_DP_BATTERY_STATUS,BatteryStatus);
            }else {
                Settings.System.putInt(getContentResolver(),G1_DP_BATTERY_STATUS,G1_DP_NOT_CONNECT);
                Log.d(TAG, "run: readBatteryStatsRunnable G1 没有连接 ");
            }

            handler.postDelayed(readBatteryStatsRunnable,3000); //循环

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

    private final Runnable runnable = new Runnable() {
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
                MainActivity.setBacklightSwitch(1);
            }else if (!isDPConnect() && DPMode){
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,brightness);
                Log.d(TAG, "G1ConnectStatus: 切换到平板 模式 配置显示器亮度："+brightness);
                DPMode = false;

            }

        }
    };

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
        initScreenListen();
        initBatteryStausListen();
    }

    private void initBatteryStausListen() {
        handler.postDelayed(readBatteryStatsRunnable,1000);
    }


    /**
     * 监听屏幕变化
     * */
    private void initScreenListen() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver,intentFilter);
    }





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