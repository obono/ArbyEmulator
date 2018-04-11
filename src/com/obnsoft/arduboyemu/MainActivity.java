package com.obnsoft.arduboyemu;

import android.app.Activity;
import android.content.Intent;
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
                if (mArduboyEmulator.initializeEmulation(path)) {
                    mArduboyEmulator.startEmulation();
                }
            }
            break;
        }
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

}
