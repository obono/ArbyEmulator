package com.obnsoft.arduboyemu;

import java.io.File;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class MyApplication extends Application {

    private static final String PREFS_KEY_FPS           = "fps";
    private static final String PREFS_KEY_TUNING        = "tuning";
    private static final String PREFS_KEY_PATH_FLASH    = "path_flash";
    private static final String PREFS_KEY_PATH_EEPROM   = "path_eeprom";

    private static final String PREFS_DEFAULT_FPS       = "60";
    private static final boolean PREFS_DEFAULT_TUNING   = false;

    private ArduboyEmulator mArduboyEmulator;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate() {
        super.onCreate();
        mArduboyEmulator = new ArduboyEmulator(this);
    }

    @Override
    public void onTerminate() {
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
