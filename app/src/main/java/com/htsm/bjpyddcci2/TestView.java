package com.htsm.bjpyddcci2;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.Settings;
import android.widget.LinearLayout;

public class TestView extends LinearLayout {
    public TestView(Context context) {
        super(context);
    }

    public static final String G1_DP_BATTERY_LEVEL = "g1dpbatterylevel";
    private final BrightnessObserver brightnessObserver = new BrightnessObserver(getContext());
    private void initjunjie(Context applicationContext) {

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(G1_DP_BATTERY_LEVEL),
                true,
                brightnessObserver
        );


    }
     class BrightnessObserver extends ContentObserver {
        private final Context mContext;

        public BrightnessObserver(Context context) {
            super(null);
            mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int brightness = Settings.System.getInt(
                    mContext.getContentResolver(),
                    G1_DP_BATTERY_LEVEL,
                    0
            );

            handleBrightnessChange(brightness);
        }
        private void handleBrightnessChange(int date) {
            int batteryStatus =   date>>8;
            int   batteryLevel = date&0xff;
            if (batteryStatus == -1){
            }else {
                setVisibility(INVISIBLE);
                onBatteryLevelChanged(batteryLevel,batteryStatus == 2);
            }


        }
    }

    public void onBatteryLevelChanged(int ss,boolean ii){

    }
}
