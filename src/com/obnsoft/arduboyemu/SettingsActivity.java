/*
 * Copyright (C) 2018 OBONO
 * http://d.hatena.ne.jp/OBONO/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.obnsoft.arduboyemu;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {

    private static final String PREFS_KEY_REFRESH   = "refresh";
    private static final String PREFS_KEY_TUNING    = "tuning";
    private static final String PREFS_KEY_ABOUT     = "about";
    private static final String PREFS_KEY_LICENSE   = "license";
    private static final String PREFS_KEY_WEBSITES  = "websites";
    private static final String PREFS_DEFAULT       = "default";

    private static final Uri URI_GPL3 = Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html");

    private MyApplication   mApp;

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
            } else if (PREFS_KEY_WEBSITES.equals(pref.getKey())) {
                showUrlList();
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
            SharedPreferences sharedPrefs = mApp.getSharedPreferences();
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
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, mFragment)
            .commit();
        mApp = (MyApplication) getApplication();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return false;
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
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        mFragment.setSummary(key);
        if (PREFS_KEY_REFRESH.equals(key)) {
            Native.setRefreshTiming(prefs.getBoolean(PREFS_KEY_REFRESH, false));
        }
        if (PREFS_KEY_TUNING.equals(key)) {
            Utils.showMessageDialog(this, android.R.drawable.ic_dialog_alert, R.string.prefsTuning,
                    R.string.messageNoticeTuning, null);
        }
    }

    /*-----------------------------------------------------------------------*/

    private void showUrlList() {
        final String[] items = getResources().getStringArray(R.array.bookmarkArray);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Uri uri = Uri.parse(items[which]);
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Utils.showListDialog(this, 0, R.string.prefsWebsites, items, listener);
    }

}
