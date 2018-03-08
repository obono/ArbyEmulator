package com.obnsoft.arduboyemu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class MainActivity extends Activity {

    private static final int REQUEST_OPENHEX = 1;

    EmulatorScreenView mEmulatorScreenView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        OnTouchListener listener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int key;
                switch (v.getId()) {
                case R.id.buttonUp:    key = 0; break;
                case R.id.buttonDown:  key = 1; break;
                case R.id.buttonLeft:  key = 2; break;
                case R.id.buttonRight: key = 3; break;
                case R.id.buttonA:     key = 4; break;
                case R.id.buttonB:     key = 5; break;
                default:               key = -1; break;
                }
                if (key != -1) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        Native.buttonEvent(key, true);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        Native.buttonEvent(key, false);
                    }
                }
                return true;
            }
        };
        findViewById(R.id.buttonUp).setOnTouchListener(listener);
        findViewById(R.id.buttonDown).setOnTouchListener(listener);
        findViewById(R.id.buttonLeft).setOnTouchListener(listener);
        findViewById(R.id.buttonRight).setOnTouchListener(listener);
        findViewById(R.id.buttonA).setOnTouchListener(listener);
        findViewById(R.id.buttonB).setOnTouchListener(listener);

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
                    Environment.getExternalStorageDirectory().getAbsolutePath());
            startActivityForResult(intent, REQUEST_OPENHEX);
            return true;
        case R.id.menuMainAbout:
            Utils.showVersion(this);
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_OPENHEX:
            if (resultCode == RESULT_OK) {
                mEmulatorScreenView.openHexFile(
                        data.getStringExtra(FilePickerActivity.INTENT_EXTRA_SELECTPATH));
            }
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
