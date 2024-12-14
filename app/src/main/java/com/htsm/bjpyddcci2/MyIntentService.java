package com.htsm.bjpyddcci2;

import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
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
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyIntentService extends Service {
    private static final String TAG = "MyIntentService";
    private int DPBrightness =0; // 保存当前机器的亮度值
    private boolean DPMode = false; // 标志亮度值是否保存
    private static final String G1_DP_BATTERY_LEVEL = "g1dpbatterylevel";
    private static final int G1_DP_NOT_CONNECT = 5<<8;
    private static int CharStatus = 0;
    private final Handler handler = new Handler();
    private boolean isCheck = false;
    private final Lock lock = new ReentrantLock();
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())){
                setBacklightSwitch(0);
                Log.d(TAG, "onReceive: 打开dp 显示器背光");

            }else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
                setBacklightSwitch(1);
                Log.d(TAG, "onReceive: 关闭dp 显示器背光");
            }

        }
    };

    // Used to load the 'bjpyddcci2' library on application startup.
    static {
        System.loadLibrary("bjpyddcci2");
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

    //BACKLIGHT
    public static native void setBacklightSwitch(int value);
    /**
     * 循环*/
    private final Runnable readBatteryStatsRunnable = new Runnable() {
        @Override
        public void run() {

            CharStatus = getChargingStatus();
            DPMode =  CharStatus != -1;

            sendDateControl();
            handler.postDelayed(readBatteryStatsRunnable,3000); //循环

        }
    };

    private void sendDateControl(){
        lock.lock();
        isCheck = true;
        if (DPMode){
            Log.d(TAG, "sendDateControl: sleep");

            lockScreen();
            int batteryLevel = getCurrentBatteryLevel();
            int batteryStatus = CharStatus;  // 1是待机 2是充电

            Log.d(TAG, "run: readBatteryStatsRunnable dp 连接 batteryLevel:" +batteryLevel +", BatteryStatus:"+batteryStatus);
            if (batteryLevel == -1){
                lock.unlock();
                return;
            }
            int date  = ((batteryStatus & 0xff)<<8) | (batteryLevel & 0xff);
            Settings.System.putInt(getContentResolver(),G1_DP_BATTERY_LEVEL,date);
            Intent intent = new Intent(G1_DP_BATTERY_LEVEL);
            intent.putExtra("date",date);
            sendBroadcast(intent);

        }else {
            unLockScreen();
            Log.d(TAG, "run: readBatteryStatsRunnable dp 没有连接");
            Intent intent = new Intent(G1_DP_BATTERY_LEVEL);
            intent.putExtra("date",G1_DP_NOT_CONNECT);
            sendBroadcast(intent);
        }
        isCheck = false;
        lock.unlock();


    }


    private void lockScreen(){
        if ( wakeLock != null && !wakeLock.isHeld()){
            Log.d(TAG, "lockScreen: 持有锁");
            wakeLock.acquire();
        }
    }

    private void unLockScreen(){
        if (wakeLock != null && wakeLock.isHeld()){
            Log.d(TAG, "lockScreen: 释放锁");
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStatus();
        initScreenListen();
        initBatteryStausListen();
        initlock();
        int brightness = Settings.System.getInt(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                0
        );
        DPBrightness = Math.round((float) brightness /255 * 100);
    }

    PowerManager.WakeLock wakeLock;
    @SuppressLint("InvalidWakeLockTag")
    private void initlock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock =   powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,"MyWakelockTag");
    }




    private void initBatteryStausListen() {
        handler.postDelayed(readBatteryStatsRunnable,1000); //  开启 循环
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


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    /**
     * 监听屏幕亮度
     * */
    private void initStatus() {
        BrightnessObserver brightnessObserver = new BrightnessObserver(getApplicationContext());
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                true,
                brightnessObserver
        );

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
            if (DPMode){
                DPBrightness = Math.round((float) brightness /255 * 100);
                handler.removeCallbacks(readBatteryStatsRunnable);
                setDPBrightness(DPBrightness);
                handler.postDelayed(readBatteryStatsRunnable,2*1000);


            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, MyIntentService.class);
        startService(intent);
    }
}