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
