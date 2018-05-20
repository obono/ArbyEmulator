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

import java.io.File;
import java.util.Locale;

import com.obnsoft.arduboyemu.MyAsyncTaskWithDialog.Result;
import com.obnsoft.arduboyemu.Utils.ResultHandler;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;

public class MainActivity extends Activity {

    private static final int REQUEST_OPEN_FLASH = 1;
    private static final String FLASH_WORK_FILE_NAME = "work.hex";

    private MyApplication       mApp;
    private ArduboyEmulator     mArduboyEmulator;
    private EmulatorScreenView  mEmulatorScreenView;
    private RelativeLayout      mLayoutToolbar;
    private Spinner             mSpinnerToolFps;
    private ImageButton         mButtonToolCaptureMovie;
    private String              mCurrentPath;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mApp = (MyApplication) getApplication();
        mArduboyEmulator = mApp.getArduboyEmulator();

        mEmulatorScreenView = (EmulatorScreenView) findViewById(R.id.emulatorScreenView);
        mLayoutToolbar = (RelativeLayout) findViewById(R.id.relativeLayoutToolBar);
        mSpinnerToolFps = (Spinner) findViewById(R.id.spinnerToolFps);
        mButtonToolCaptureMovie = (ImageButton) findViewById(R.id.buttonToolCaptureMovie);

        mSpinnerToolFps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mApp.setEmulationFpsByItemPos(position);
                mArduboyEmulator.setFps(mApp.getEmulationFps());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

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
        if (mApp.getConfirmQuit()) {
            Utils.showMessageDialog(this, 0, R.string.menuQuit, R.string.messageConfirmQuit,
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.super.onBackPressed();
                        }
            });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (mArduboyEmulator.isCapturing()) {
            mArduboyEmulator.stopCapturing();
        }
        mArduboyEmulator.stopEmulation();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLayoutToolbar.setVisibility((mApp.getShowToolbar()) ? View.VISIBLE : View.INVISIBLE);
        mSpinnerToolFps.setSelection(mApp.getEmulationFpsItemPos(), false);
        refreshCaptureVideoButtonColor();
        mArduboyEmulator.bindEmulatorView(mEmulatorScreenView);
        mArduboyEmulator.startEmulation();
    }

    @Override
    protected void onDestroy() {
        mArduboyEmulator.bindEmulatorView(null);
        mArduboyEmulator.finishEmulation();
        mEmulatorScreenView.onDestroy();
        Utils.cleanCacheFiles(this);
        super.onDestroy();
    }

    public void onClickReset(View v) {
        if (mArduboyEmulator.isEmulating()) {
            startEmulation(mCurrentPath);
        }
    }

    public void onClickCaptureShot(View v) {
        if (mArduboyEmulator.isEmulating()) {
            mArduboyEmulator.requestOneShot();
        }
    }

    public void onClickCaptureMovie(View v) {
        if (mArduboyEmulator.isEmulating()) {
            if (!mArduboyEmulator.isCapturing()) {
                mArduboyEmulator.startCapturing();
            } else {
                mArduboyEmulator.stopCapturing();
            }
            refreshCaptureVideoButtonColor();
        }
    }

    /*-----------------------------------------------------------------------*/

    private void startEmulation(String path) {
        if (path.toLowerCase(Locale.getDefault()).endsWith(FilePickerActivity.EXT_ARDUBOY)) {
            File outFile = Utils.generateTempFile(mApp, FLASH_WORK_FILE_NAME);
            if (ArduboyUtils.extractHexFromArduboy(new File(path), outFile)) {
                path = outFile.getAbsolutePath();
            } else {
                outFile.delete();
                path = null;
            }
        }
        if (path != null && mArduboyEmulator.initializeEmulation(path)) {
            mCurrentPath = path;
            mArduboyEmulator.startEmulation();
        } else {
            Utils.showToast(this, R.string.messageEmulateFailed);
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

    private void refreshCaptureVideoButtonColor() {
        if (mArduboyEmulator.isEmulating()) {
            if (mArduboyEmulator.isCapturing()) {
                mButtonToolCaptureMovie.setColorFilter(Color.RED);
            } else {
                mButtonToolCaptureMovie.setColorFilter(null);
            }
        }
    }

}
