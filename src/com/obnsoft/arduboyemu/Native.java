package com.obnsoft.arduboyemu;

public class Native {

    public static final int BUTTON_UP   = 0;
    public static final int BUTTON_DOWN = 1;
    public static final int BUTTON_LEFT = 2;
    public static final int BUTTON_RIGHT= 3;
    public static final int BUTTON_A    = 4;
    public static final int BUTTON_B    = 5;
    public static final int BUTTON_MAX  = 6;

    static {
        System.loadLibrary("ArduboyEmulatorNative");
    }

    public static native boolean setup(String hexFilePath, boolean isTuned);
    public static native boolean getEeprom(byte[] ary);
    public static native boolean setEeprom(byte[] ary);
    public static native boolean buttonEvent(int key, boolean isPress);
    public static native boolean loop(int[] pixels);
    public static native boolean getLedState(int[] leds);
    public static native void teardown();
}
