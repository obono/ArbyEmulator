/*
 * Arduboy emulator using simavr on Android platform.
 *
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

import java.io.File;
import java.util.Locale;

import com.obnsoft.arduboyemu.MyAsyncTaskWithDialog.Result;
import com.obnsoft.arduboyemu.Utils.ResultHandler;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

    private static final int REQUEST_OPEN_FLASH = 1;

    private MyApplication       mApp;
    private ArduboyEmulator     mArduboyEmulator;
    private EmulatorScreenView  mEmulatorScreenView;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mApp = (MyApplication) getApplication();
        mArduboyEmulator = mApp.getArduboyEmulator();
        mEmulatorScreenView = (EmulatorScreenView) findViewById(R.id.emulatorScreenView);
        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menuMainOpen:
            Intent intent = new Intent(this, FilePickerActivity.class);
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_EXTENSIONS,
                    FilePickerActivity.EXTS_FLASH);
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_WRITEMODE, false);
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_DIRECTORY, mApp.getPathFlash());
            startActivityForResult(intent, REQUEST_OPEN_FLASH);
            return true;
        case R.id.menuMainEeprom:
            startActivity(new Intent(this, EepromActivity.class));
            return true;
        case R.id.menuMainSettings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_OPEN_FLASH:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(FilePickerActivity.INTENT_EXTRA_SELECTPATH);
                mApp.setPathFlash(Utils.getParentPath(path));
                startEmulation(path);
            }
            break;
        }
    }

    @Override
    public void onBackPressed() {
        Utils.showMessageDialog(this, 0, R.string.menuQuit, R.string.messageConfirmQuit,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
        });
    }

    @Override
    protected void onPause() {
        mArduboyEmulator.stopEmulation();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mArduboyEmulator.bindEmulatorView(mEmulatorScreenView);
        mArduboyEmulator.startEmulation();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mArduboyEmulator.bindEmulatorView(null);
        mArduboyEmulator.finishEmulation();
        mEmulatorScreenView.onDestroy();
        Utils.cleanCacheFiles(this);
        super.onDestroy();
    }

    /*-----------------------------------------------------------------------*/

    private void startEmulation(String path) {
        if (mArduboyEmulator.initializeEmulation(path)) {
            mArduboyEmulator.startEmulation();
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Uri uri = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            Utils.downloadFile(this, uri, new ResultHandler() {
                @Override
                public void handleResult(Result result, File file) {
                    switch (result) {
                    case FAILED:
                        Utils.showToast(MainActivity.this, R.string.messageDownloadFailed);
                        // go to following code
                    default:
                    case CANCELLED:
                        file.delete();
                        break;
                    case SUCCEEDED:
                        final String path = file.getAbsolutePath();
                        if (path.toLowerCase(Locale.getDefault())
                                .endsWith(FilePickerActivity.EXT_EEPROM)) {
                            Utils.showMessageDialog(MainActivity.this, 0, R.string.menuEeprom,
                                    R.string.messageConfirmLoad, new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mArduboyEmulator.restoreEeprom(path);
                                        }
                            });
                        } else {
                            Utils.showMessageDialog(MainActivity.this, 0, R.string.menuOpen,
                                    R.string.messageConfirmLoad, new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startEmulation(path);
                                        }
                            });
                        }
                        break;
                    }
                }
            });
        }
    }

}
