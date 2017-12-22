package com.obnsoft.arduboyemu;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class MainActivity extends Activity {

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
        mEmulatorScreenView.startEmulation(
                Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/ArduboyUtility/Flash/OBN-soft/hollow_v0.31.hex");
    }

    @Override
    protected void onDestroy() {
        mEmulatorScreenView.stopEmulation();
        super.onDestroy();
    }

}
