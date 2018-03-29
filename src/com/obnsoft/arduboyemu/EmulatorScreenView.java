package com.obnsoft.arduboyemu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class EmulatorScreenView extends View {

    private static final int WIDTH = 128;
    private static final int HEIGHT = 64;

    private Bitmap      mBitmap;
    private Matrix      mMatrix;
    private Paint       mPaint;
    private Thread      mEmulationThread;
    private boolean     mIsAvailable;

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
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mBitmap.isRecycled()) {
            canvas.drawBitmap(mBitmap, mMatrix, mPaint);
        }
    }

    public synchronized boolean startEmulation(String path) {
        boolean ret = false;
        if (mEmulationThread == null && Native.setup(path, true)) {
            mEmulationThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int[] pixels = new int[WIDTH * HEIGHT];
                    long baseTime = System.currentTimeMillis();
                    long frames = 0;
                    while (mIsAvailable) {
                        Native.loop(pixels);
                        if (!mBitmap.isRecycled()) {
                            mBitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
                            postInvalidate();
                        }
                        if (++frames == 60) {
                            baseTime += 1000;
                            frames = 0;
                        }
                        long currentTime = System.currentTimeMillis();
                        long targetTime = baseTime + frames * 1000 / 60;
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
                    Native.teardown();
                }
            });
            if (mEmulationThread != null) {
                mIsAvailable = true;
                mEmulationThread.start();
                ret = true;
            }
        }
        return ret;
    }

    public synchronized void stopEmulation() {
        if (mEmulationThread != null) {
            try {
                mIsAvailable = false;
                mEmulationThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mEmulationThread = null;
            if (!mBitmap.isRecycled()) {
                mBitmap.eraseColor(Color.BLACK);
                postInvalidate();
            }
        }
    }

    public synchronized void onDestroy() {
        stopEmulation();
        mBitmap.recycle();
    }

}
