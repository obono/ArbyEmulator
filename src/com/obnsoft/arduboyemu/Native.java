package com.obnsoft.arduboyemu;

public class Native {
    static {
        System.loadLibrary("ArduboyEmulatorNative");
    }
    public static native boolean setup(String hexFilePath);
    public static native void buttonEvent(int key, boolean isPress);
    public static native boolean loop(int[] colors);
    public static native void teardown();
}
