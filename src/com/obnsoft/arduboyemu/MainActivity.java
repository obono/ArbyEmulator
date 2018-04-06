package com.obnsoft.arduboyemu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

    private static final int REQUEST_OPENHEX = 1;

    EmulatorScreenView mEmulatorScreenView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
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
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_EXTENSIONS, new String[] {"hex"});
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_WRITEMODE, false);
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_DIRECTORY,
                    SettingsActivity.getPathFlash(this));
            startActivityForResult(intent, REQUEST_OPENHEX);
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
        case REQUEST_OPENHEX:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(FilePickerActivity.INTENT_EXTRA_SELECTPATH);
                mEmulatorScreenView.openHexFile(path);
                SettingsActivity.setPathFlash(this, path);
            }
            break;
        }
    }

    @Override
    protected void onPause() {
        mEmulatorScreenView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mEmulatorScreenView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mEmulatorScreenView.onDestroy();
        super.onDestroy();
    }

}
