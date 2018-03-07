package com.obnsoft.arduboyemu;

public class Native {
    static {
        System.loadLibrary("ArduboyEmulatorNative");
    }
    public static native boolean setup(String hexFilePath, int cpuFreq);
    public static native byte[] getEEPROM();
    public static native boolean setEEPROM(byte[] ary);
    public static native void buttonEvent(int key, boolean isPress);
    public static native byte getLEDState();
    public static native boolean loop(int[] colors);
    public static native void teardown();
}
