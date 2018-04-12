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
import java.util.Locale;

import com.obnsoft.arduboyemu.Utils.CancelCallback;

import android.content.Context;

public class ArduboyEmulator {

    public static final int SCREEN_WIDTH = 128;
    public static final int SCREEN_HEIGHT = 64;
    public static final int EEPROM_SIZE = 1024;

    private static final int PIXELS_SIZE = SCREEN_WIDTH * SCREEN_HEIGHT;
    private static final int ONE_SECOND = 1000;
    private static final String FLASH_WORK_FILE_NAME = "work.hex";
    private static final String EEPROM_FILE_NAME = "eeprom.bin";

    private static final CancelCallback EEPROM_CALLBACK = new CancelCallback() {
        @Override
        public boolean isCencelled(long length) {
            return (length >= EEPROM_SIZE);
        }
    };

    private MyApplication       mApp;
    private EmulatorScreenView  mEmulatorView;

    private Thread      mEmulationThread;
    private boolean     mIsEmulationAvailable;
    private boolean     mIsEmulating;
    private byte[]      mEeprom;

    /*-----------------------------------------------------------------------*/

    public ArduboyEmulator(MyApplication app) {
        mApp = app;
        loadEeprom();
    }

    /*-----------------------------------------------------------------------*/

    public synchronized boolean initializeEmulation(String path) {
        if (mIsEmulationAvailable) {
            finishEmulation();
        }
        if (path.toLowerCase(Locale.getDefault()).endsWith(FilePickerActivity.EXT_ARDUBOY)) {
            File outFile = Utils.generateTempFile(mApp, FLASH_WORK_FILE_NAME);
            if (!ArduboyUtils.extractHexFromArduboy(new File(path), outFile)) {
                mIsEmulationAvailable = false;
                outFile.delete();
                return false;
            }
            path = outFile.getAbsolutePath();
        }
        mIsEmulationAvailable = Native.setup(path, mApp.getEmulationTuning());
        return mIsEmulationAvailable;
    }

    public synchronized void bindEmulatorView(EmulatorScreenView emulatorView) {
        mEmulatorView = emulatorView;
    }

    public synchronized boolean startEmulation() {
        if (!mIsEmulationAvailable) {
            return false;
        }
        if (mEmulationThread != null) {
            stopEmulation();
        }
        mEmulationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int fps = mApp.getEmulationFps();
                int[] pixels = new int[PIXELS_SIZE];
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
                    if (mEmulatorView != null) {
                        mEmulatorView.updateScreen(pixels);
                    }
                    if (++frames >= fps) {
                        baseTime += ONE_SECOND;
                        frames = 0;
                    }
                    long currentTime = System.currentTimeMillis();
                    long targetTime = baseTime + frames * ONE_SECOND / fps;
                    if (currentTime < targetTime) {
                        try {
                            Thread.sleep(targetTime - currentTime);
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                    } else {
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

}
