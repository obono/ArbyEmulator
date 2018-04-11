package com.obnsoft.arduboyemu;

public class Native {
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
