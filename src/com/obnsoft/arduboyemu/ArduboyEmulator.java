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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import com.obnsoft.arduboyemu.Utils.CancelCallback;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;

public class ArduboyEmulator {

    public static final int SCREEN_WIDTH = 128;
    public static final int SCREEN_HEIGHT = 64;
    public static final int EEPROM_SIZE = 1024;

    private static final int PIXELS_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT;

    private static final int LED_RED    = 0;
    private static final int LED_GREEN  = 1;
    private static final int LED_BLUE   = 2;
    private static final int LED_RX     = 3;
    private static final int LED_TX     = 4;
    private static final int LEDS_SIZE  = 5;

    private static final int ONE_SECOND = 1000;

    private static final String EEPROM_FILE_NAME = "eeprom.bin";
    private static final CancelCallback EEPROM_CALLBACK = new CancelCallback() {
        @Override
        public boolean isCencelled(long length) {
            return (length >= EEPROM_SIZE);
        }
    };

    private static final String CAPTURE_DIR_NAME = "ArbyEmulator";
    private static final String CAPTURE_WORK_FILE_NAME = "temp.gif";
    private static final String CAPTURE_FILE_NAME_FORMAT = "yyyyMMddkkmmss'.gif'";
    private static final File CAPTURE_DIR = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            CAPTURE_DIR_NAME);
    private static final File CAPTURE_WORK_FILE = new File(CAPTURE_DIR, CAPTURE_WORK_FILE_NAME);

    private MyApplication       mApp;
    private EmulatorScreenView  mEmulatorView;

    private Thread      mEmulationThread;
    private boolean     mIsEmulationAvailable;
    private boolean     mIsEmulating;
    private boolean     mIsCharging;
    private boolean     mIsOneShot;
    private boolean     mIsCapturing;
    private int         mFps;
    private byte[]      mEeprom;
    private GifEncoder  mGifEncoder;

    /*-----------------------------------------------------------------------*/
    /*                              Emulation                                */
    /*-----------------------------------------------------------------------*/

    public ArduboyEmulator(MyApplication app) {
        mApp = app;
        loadEeprom();
        mGifEncoder = new GifEncoder();
    }

    public boolean isEmulating() {
        return mIsEmulating;
    }

    public void setFps(int fps) {
        mFps = fps;
    }

    public synchronized void setCharging(boolean isCharging) {
        mIsCharging = isCharging;
        if (!mIsEmulating && mEmulatorView != null) {
            mEmulatorView.updateLed(Color.BLACK, false, false, mIsCharging);
            mEmulatorView.postInvalidate();
        }
    }

    public synchronized void bindEmulatorView(EmulatorScreenView emulatorView) {
        mEmulatorView = emulatorView;
        setCharging(mIsCharging);
    }

    public synchronized boolean initializeEmulation(String path) {
        if (mIsEmulationAvailable) {
            finishEmulation();
        }
        mIsEmulationAvailable = Native.setup(path, mApp.getEmulationTuning());
        Native.setRefreshTiming(mApp.getEmulationPostRefresh());
        return mIsEmulationAvailable;
    }

    public synchronized boolean startEmulation() {
        if (!mIsEmulationAvailable) {
            return false;
        }
        if (mEmulationThread != null) {
            stopEmulation();
        }
        final Handler handler = new Handler();
        mEmulationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int fps = mFps;
                int[] pixels = new int[PIXELS_SIZE];
                int[] leds = new int[LEDS_SIZE];
                long baseTime = System.currentTimeMillis();
                long frames = 0;

                Native.setEeprom(mEeprom);
                while (mIsEmulating) {
                    if (mEmulatorView != null) {
                        boolean[] buttonState = mEmulatorView.updateButtonState();
                        for (int buttonIdx = 0; buttonIdx < Native.BUTTON_MAX; buttonIdx++) {
                            Native.buttonEvent(buttonIdx, buttonState[buttonIdx]);
                        }
                    }
                    Native.loop(pixels);
                    Native.getLedState(leds);
                    if (mEmulatorView != null) {
                        mEmulatorView.updateScreen(pixels);
                        mEmulatorView.updateLed(
                                Color.rgb(leds[LED_RED], leds[LED_GREEN], leds[LED_BLUE]),
                                (leds[LED_RX] != 0), (leds[LED_TX] != 0), mIsCharging);
                        mEmulatorView.postInvalidate();
                    }
                    if (mIsOneShot) {
                        final File file = generateCaptureFile();
                        if (mGifEncoder.oneShot(file, pixels)) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    notifyCaptured(file, false);
                                }
                            });
                        }
                        mIsOneShot = false;
                    }
                    if (mIsCapturing) {
                        mGifEncoder.addFrame(pixels);
                    }
                    if (++frames >= fps) {
                        baseTime += ONE_SECOND;
                        frames = 0;
                    }
                    long currentTime = System.currentTimeMillis();
                    long targetTime = baseTime + frames * ONE_SECOND / fps;
                    if (mFps == fps && currentTime < targetTime) {
                        try {
                            Thread.sleep(targetTime - currentTime);
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                    } else {
                        fps = mFps;
                        baseTime = currentTime;
                        frames = 0;
                    }
                }
                Native.getEeprom(mEeprom);
                saveEeprom();
            }
        });
        if (mEmulationThread == null) {
            return false;
        }
        mIsEmulating = true;
        mEmulationThread.start();
        return true;
    }

    public synchronized void stopEmulation() {
        if (mEmulationThread != null) {
            mIsEmulating = false;
            try {
                mEmulationThread.join(ONE_SECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mEmulationThread = null;
        }
    }

    public synchronized void finishEmulation() {
        if (mIsEmulationAvailable) {
            stopEmulation();
            Native.teardown();
            mIsEmulationAvailable = false;
        }
    }

    /*-----------------------------------------------------------------------*/
    /*                            Control EEPROM                             */
    /*-----------------------------------------------------------------------*/

    public byte[] getEeprom() {
        return mEeprom;
    }

    public void loadEeprom() {
        try {
            inputEeprom(mApp.openFileInput(EEPROM_FILE_NAME), true);
        } catch (Exception e) {
            e.printStackTrace();
            defaultEeprom();
        }
    }

    public void clearEeprom() {
        defaultEeprom();
        if (mIsEmulating) {
            Native.setEeprom(mEeprom);
        } else {
            saveEeprom();
        }
    }

    public boolean restoreEeprom(String path) {
        try {
            boolean ret = inputEeprom(new FileInputStream(new File(path)), false);
            if (ret) {
                if (mIsEmulating) {
                    Native.setEeprom(mEeprom);
                } else {
                    saveEeprom();
                }
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean backupEeprom(String path) {
        try {
            return outputEeprom(new FileOutputStream(new File(path)));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void saveEeprom() {
        try {
            outputEeprom(mApp.openFileOutput(EEPROM_FILE_NAME, Context.MODE_PRIVATE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean inputEeprom(InputStream in, boolean isInternal)
            throws FileNotFoundException, IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(EEPROM_SIZE);
            long length = Utils.transferBytes(in, out, EEPROM_CALLBACK);
            if (length >= EEPROM_SIZE) {
                mEeprom = out.toByteArray();
                return true;
            } else if (isInternal) {
                defaultEeprom();
                return true;
            } else {
                return false;
            }
        } catch (FileNotFoundException e) {
            if (isInternal) {
                defaultEeprom();
                return true;
            } else {
                throw e;
            }
        }
    }

    private boolean outputEeprom(OutputStream out) throws IOException {
        long length = Utils.transferBytes(new ByteArrayInputStream(mEeprom), out, EEPROM_CALLBACK);
        return (length >= EEPROM_SIZE);
    }

    private void defaultEeprom() {
        mEeprom = new byte[EEPROM_SIZE];
        for (int i = 0; i < EEPROM_SIZE; i++) {
            mEeprom[i] = (byte) 0xFF;
        }
    }

    /*-----------------------------------------------------------------------*/
    /*                            Screen Capture                             */
    /*-----------------------------------------------------------------------*/

    public boolean isCapturing() {
        return mIsCapturing;
    }

    public synchronized boolean requestOneShot() {
        if (mIsEmulating) {
            mIsOneShot = true;
        }
        return mIsOneShot;
    }

    public synchronized boolean startCapturing() {
        if (!mIsEmulating || mIsCapturing) {
            return false;
        }
        if (mGifEncoder.start(getCaptureWorkFile())) {
            Utils.showToast(mApp, R.string.messageCaptureStart);
            mIsCapturing = true;
        }
        return mIsCapturing;
    }

    public synchronized boolean stopCapturing() {
        if (!mIsEmulating || !mIsCapturing) {
            return false;
        }
        mIsCapturing = false;
        File file = generateCaptureFile();
        if (mGifEncoder.finish(file)) {
            notifyCaptured(file, true);
            return true;
        } else {
            Utils.showToast(mApp, R.string.messageCaptureFailed);
            return false;
        }
    }

    private void ensureCaptureDir() {
        if (!CAPTURE_DIR.exists()) {
            CAPTURE_DIR.mkdirs();
        }
    }

    private File getCaptureWorkFile() {
        ensureCaptureDir();
        return CAPTURE_WORK_FILE;

    }

    private File generateCaptureFile() {
        ensureCaptureDir();
        return new File(CAPTURE_DIR, DateFormat.format(
                CAPTURE_FILE_NAME_FORMAT, Calendar.getInstance()).toString());
    }

    private void notifyCaptured(File file, boolean isMovie) {
        MediaScannerConnection.scanFile(mApp, new String[] { file.getAbsolutePath() }, null, null);
        int stringId = (isMovie) ? R.string.messageCaptureMovie : R.string.messageCaptureShot;
        String message = String.format(mApp.getString(stringId), file.getName());
        Utils.showToast(mApp, message);
    }
}
