package com.htsm.bjpyddcci2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.provider.Settings;
import android.widget.LinearLayout;

public class TestView extends LinearLayout {
    public TestView(Context context) {
        super(context);
    }

    public static final String G1_DP_BATTERY_LEVEL = "g1dpbatterylevel";

    private void initjunjie(Context applicationContext) {
        IntentFilter intentFilter = new IntentFilter(G1_DP_BATTERY_LEVEL);
        getContext().registerReceiver(batteryG1dp,intentFilter, Context.RECEIVER_EXPORTED);



    }
    private final BroadcastReceiver batteryG1dp = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int date = intent.getIntExtra("date",5<<8);
            handleBrightnessChange(date);


        }
    };

    private void handleBrightnessChange(int date) {
        int batteryStatus =   date>>8;
        int batteryLevel = date&0xff;
        if (batteryStatus == 5){
            android.util.Log.d("liujunjie", "handleBrightnessChange:  删除电池图标 ");

        }else {
            android.util.Log.d("liujunjie", "handleBrightnessChange:  添加电池图标 ");
            //addImageView();
            onBatteryLevelChanged(batteryLevel,batteryStatus == 2);
        }

    }

    public void onBatteryLevelChanged(int ss,boolean ii){

    }
}
