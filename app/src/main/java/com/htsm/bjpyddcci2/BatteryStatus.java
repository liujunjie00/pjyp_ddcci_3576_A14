package com.htsm.bjpyddcci2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.BatteryManager;
import android.util.Log;
import android.view.WindowManager;


public class BatteryStatus extends BroadcastReceiver {
    private static final String TAG = "BatteryStatus";
    private G1ConnectInterface connectInterface;
    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieves battery status from the Intent
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        //Log.d(TAG, "onReceive  status: "+status);
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            if (!getWindowIsG1(context) && connectInterface != null){
               // Log.d(TAG, "onReceive: ====BATTERY_STATUS_CHARGING== ");
                connectInterface.G1ConnectStatus(true);
            }
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            if (getWindowIsG1(context) && connectInterface != null){
               // Log.d(TAG, "onReceive: ====BATTERY_STATUS_DISCHARGING== ");
                connectInterface.G1ConnectStatus(false);
            }
        }
    }

    public void setConnectInterface(G1ConnectInterface connectInterface) {
        this.connectInterface = connectInterface;
    }

    interface G1ConnectInterface{
        void G1ConnectStatus(boolean isConnect);
    }

    public static boolean getWindowIsG1(Context context) { //720x1440 G1
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        int width = size.x;
        int height = size.y;
       // android.util.Log.d("liujunjie", "============getWindowSzie: "+size);
        return width == 720 || height == 1440 || width == 1440 || height == 720;

    }
}
