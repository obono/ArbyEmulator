package com.obnsoft.arduboyemu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class EmulatorScreenView extends View {

    private static final int FPS = 60;
    private static final int ONE_SECOND = 1000;

    private static final int WIDTH = 128;
    private static final int HEIGHT = 64;

    private Bitmap      mBitmap;
    private Matrix      mMatrix;
    private Paint       mPaint;
    private Thread      mEmulationThread;
    private boolean     mIsAvailable;
    private boolean     mIsExecuting;

    public EmulatorScreenView(Context context) {
        this(context, null);
    }

    public EmulatorScreenView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmulatorScreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        mMatrix = new Matrix();
        mPaint = new Paint(0); // No ANTI_ALIAS_FLAG, No FILTER_BITMAP_FLAG
        setFocusable(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int scale = Math.min(w / WIDTH, h / HEIGHT);
        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate((w - WIDTH * scale) / 2, (h - HEIGHT * scale) / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (mBitmap) {
            if (!mBitmap.isRecycled()) {
                canvas.drawBitmap(mBitmap, mMatrix, mPaint);
            }
        }
    }

    /*-----------------------------------------------------------------------*/

    public void openHexFile(String path) {
        initializeEmulation(path);
        startEmulation();
    }

    public void onResume() {
        startEmulation();
    }

    public void onPause() {
        stopEmulation();
    }

    public void onDestroy() {
        finishEmulation();
        synchronized (mBitmap) {
            mBitmap.recycle();
        }
    }

    /*-----------------------------------------------------------------------*/

    private synchronized boolean initializeEmulation(String path) {
        if (mIsAvailable) {
            finishEmulation();
        }
        mIsAvailable = Native.setup(path, false);
        return mIsAvailable;
    }

    private synchronized boolean startEmulation() {
        if (!mIsAvailable) {
            return false;
        }
        if (mEmulationThread != null) {
            stopEmulation();
        }
        mEmulationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int[] pixels = new int[WIDTH * HEIGHT];
                long baseTime = System.currentTimeMillis();
                long frames = 0;
                while (mIsExecuting) {
                    Native.loop(pixels);
                    synchronized (mBitmap) {
                        if (!mBitmap.isRecycled()) {
                            mBitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
                        }
                        postInvalidate();
                        if (++frames == FPS) {
                            baseTime += ONE_SECOND;
                            frames = 0;
                        }
                        long currentTime = System.currentTimeMillis();
                        long targetTime = baseTime + frames * ONE_SECOND / FPS;
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
                }
            }
        });
        if (mEmulationThread == null) {
            return false;
        }
        mIsExecuting = true;
        mEmulationThread.start();
        return true;
    }

    private synchronized void stopEmulation() {
        if (mEmulationThread != null) {
            mIsExecuting = false;
            try {
                mEmulationThread.join(ONE_SECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mEmulationThread = null;
        }
    }

    private synchronized void finishEmulation() {
        if (mIsAvailable) {
            stopEmulation();
            Native.teardown();
            mIsAvailable = false;
        }
    }

}
