package com.obnsoft.arduboyemu;

public class Native {
    static {
        System.loadLibrary("ArduboyEmulatorNative");
    }
    public static native boolean setup(String hexFilePath, boolean isTuned);
    public static native byte[] getEEPROM();
    public static native boolean setEEPROM(byte[] ary);
    public static native void buttonEvent(int key, boolean isPress);
    public static native boolean loop(int[] pixels);
    public static native boolean getLEDState(int[] leds);
    public static native void teardown();
}
