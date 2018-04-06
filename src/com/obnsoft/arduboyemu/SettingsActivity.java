package com.obnsoft.arduboyemu;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {

    private static final String PREFS_KEY_FPS           = "fps";
    private static final String PREFS_KEY_TUNING        = "tuning";
    private static final String PREFS_KEY_ABOUT         = "about";
    private static final String PREFS_KEY_LICENSE       = "license";
    private static final String PREFS_KEY_PATH_FLASH    = "path_flash";
    private static final String PREFS_KEY_PATH_EEPROM   = "path_eeprom";

    private static final String PREFS_DEFAULT           = "default";
    private static final String PREFS_DEFAULT_FPS       = "60";
    private static final boolean PREFS_DEFAULT_TUNING   = false;

    private static final Uri URI_GPL3 = Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html");
                                                //"https://www.gnu.org/licenses/gpl-3.0.txt";

    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getEmulationFps(Context context) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        return Integer.parseInt(sharedPrefs.getString(PREFS_KEY_FPS, PREFS_DEFAULT_FPS));
    }

    public static boolean getEmulationTuning(Context context) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        return sharedPrefs.getBoolean(PREFS_KEY_TUNING, PREFS_DEFAULT_TUNING);
    }

    public static String getPathFlash(Context context) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        String path = sharedPrefs.getString(PREFS_KEY_PATH_FLASH, null);
        if (path == null || !(new File(path).exists())) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return path;
    }

    public static boolean setPathFlash(Context context, String path) {
        return putStringToSharedPreferences(context, PREFS_KEY_PATH_FLASH, getParentPath(path));
    }

    public static String getPathEeprom(Context context) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        return sharedPrefs.getString(PREFS_KEY_PATH_EEPROM, getPathFlash(context));
    }

    public static boolean setPathEeprom(Context context, String path) {
        return putStringToSharedPreferences(context, PREFS_KEY_PATH_EEPROM, getParentPath(path));
    }

    public static String getParentPath(String path) {
        return new File(path).getParent();
    }

    public static boolean putStringToSharedPreferences(Context context, String key, String path) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, path);
        return editor.commit();
    }

    /*-----------------------------------------------------------------------*/

    private class MyPreferenceFragment extends PreferenceFragment
            implements OnPreferenceClickListener {

        public MyPreferenceFragment() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            refreshSummary();
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            if (PREFS_KEY_ABOUT.equals(pref.getKey())) {
                Utils.showVersion(SettingsActivity.this);
            } else if (PREFS_KEY_LICENSE.equals(pref.getKey())) {
                Intent intent = new Intent(Intent.ACTION_VIEW, URI_GPL3);
                SettingsActivity.this.startActivity(intent);
            } else {
                refreshSummary();
            }
            return true;
        }

        public void refreshSummary() {
            refreshSummary(getPreferenceScreen());
        }

        public void refreshSummary(PreferenceGroup prefGroup) {
            for (int i = 0; i < prefGroup.getPreferenceCount(); i++) {
                setSummary(prefGroup.getPreference(i));
            }
        }

        public void setSummary(Preference pref) {
            if (pref != null) {
                setSummary(pref, pref.getKey());
            }
        }

        public void setSummary(String key) {
            if (key != null) {
                setSummary(findPreference(key), key);
            }
        }

        public void setSummary(Preference pref, String key) {
            SharedPreferences sharedPrefs = getSharedPreferences(SettingsActivity.this);
            if (pref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) pref;
                listPref.setValue(sharedPrefs.getString(key, PREFS_DEFAULT));
                listPref.setSummary(listPref.getEntry());
            } else if (pref instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPref = (CheckBoxPreference) pref;
                checkBoxPref.setChecked(sharedPrefs.getBoolean(key, false));
            } else if (pref instanceof EditTextPreference) {
                EditTextPreference editTextPref = (EditTextPreference) pref;
                editTextPref.setText(sharedPrefs.getString(key, PREFS_DEFAULT));
                editTextPref.setSummary(editTextPref.getText());
            } else if (pref instanceof PreferenceGroup) {
                refreshSummary((PreferenceGroup) pref);
            } else {
                pref.setOnPreferenceClickListener(this);
            }
        }
    }

/*-----------------------------------------------------------------------*/

    private MyPreferenceFragment mFragment = new MyPreferenceFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, mFragment)
            .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = mFragment.getPreferenceScreen().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = mFragment.getPreferenceScreen().getSharedPreferences();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mFragment.setSummary(key);
    }

}
