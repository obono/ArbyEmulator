package com.obnsoft.arduboyemu;

import java.io.File;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Environment;
import android.preference.PreferenceManager;

public class MyApplication extends Application {

    private static final String PREFS_KEY_FPS           = "fps";
    private static final String PREFS_KEY_TUNING        = "tuning";
    private static final String PREFS_KEY_PATH_FLASH    = "path_flash";
    private static final String PREFS_KEY_PATH_EEPROM   = "path_eeprom";

    private static final String PREFS_DEFAULT_FPS       = "60";
    private static final boolean PREFS_DEFAULT_TUNING   = false;

    private ArduboyEmulator     mArduboyEmulator;
    private BroadcastReceiver   mChargeStateReveiver;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate() {
        super.onCreate();
        mArduboyEmulator = new ArduboyEmulator(this);

        /*  Get current charging status  */
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        mArduboyEmulator.setCharging((status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL));

        /*  Detect power connection  */
        mChargeStateReveiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                mArduboyEmulator.setCharging(Intent.ACTION_POWER_CONNECTED.equals(action));
            }
        };
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mChargeStateReveiver, ifilter);
    }

    @Override
    public void onTerminate() {
        unregisterReceiver(mChargeStateReveiver);
        super.onTerminate();
    }

    public ArduboyEmulator getArduboyEmulator() {
        return mArduboyEmulator;
    };

    /*-----------------------------------------------------------------------*/

    public SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    public int getEmulationFps() {
        return Integer.parseInt(getSharedPreferences().getString(PREFS_KEY_FPS, PREFS_DEFAULT_FPS));
    }

    public boolean getEmulationTuning() {
        return getSharedPreferences().getBoolean(PREFS_KEY_TUNING, PREFS_DEFAULT_TUNING);
    }

    public String getPathFlash() {
        SharedPreferences sharedPrefs = getSharedPreferences();
        String path = sharedPrefs.getString(PREFS_KEY_PATH_FLASH, null);
        if (path == null || !(new File(path).exists())) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return path;
    }

    public boolean setPathFlash(String path) {
        return putStringToSharedPreferences(PREFS_KEY_PATH_FLASH, path);
    }

    public String getPathEeprom() {
        SharedPreferences sharedPrefs = getSharedPreferences();
        return sharedPrefs.getString(PREFS_KEY_PATH_EEPROM, getPathFlash());
    }

    public boolean setPathEeprom(String path) {
        return putStringToSharedPreferences(PREFS_KEY_PATH_EEPROM, path);
    }

    public boolean putStringToSharedPreferences(String key, String path) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, path);
        return editor.commit();
    }

}
