package com.obnsoft.arduboyemu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class EmulatorScreenView extends View {

    private static final int ONE_SECOND = 1000;

    private static final int WIDTH = 128;
    private static final int HEIGHT = 64;

    private static final int BUTTON_MAX = 6;
    private static final int BUTTON_SIZE_UNIT_BASE = 24;
    private static final int TOUCH_STATE_MAX = 10;

    private Bitmap      mBitmap;
    private Matrix      mMatrix;
    private Paint       mPaint;
    private Thread      mEmulationThread;
    private boolean     mIsAvailable;
    private boolean     mIsExecuting;

    private boolean[]   mButtonState = new boolean[BUTTON_MAX];
    private PointF[]    mButtonPosition = new PointF[BUTTON_MAX];
    private float       mButtonSizeUnit;
    private PointF[]    mTouchPoint = new PointF[TOUCH_STATE_MAX];
    private int         mTouchPointCount;

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

        for (int buttonIdx = 0; buttonIdx < BUTTON_MAX; buttonIdx++) {
            mButtonPosition[buttonIdx] = new PointF();
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mButtonSizeUnit = BUTTON_SIZE_UNIT_BASE * displayMetrics.density;
        for (int touchIdx = 0; touchIdx < TOUCH_STATE_MAX; touchIdx++) {
            mTouchPoint[touchIdx] = new PointF();
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        mTouchPointCount = (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                ? 0 : event.getPointerCount();
        for (int touchIdx = 0; touchIdx < mTouchPointCount; touchIdx++) {
            mTouchPoint[touchIdx].set(event.getX(touchIdx), event.getY(touchIdx));
        }
        if (!mIsExecuting) {
            postInvalidate();
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int scale = Math.min(w / WIDTH, h / HEIGHT);
        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate((w - WIDTH * scale) / 2, (h - HEIGHT * scale) / 2);

        mButtonPosition[0].set(mButtonSizeUnit * 4, h - mButtonSizeUnit * 6);       // up
        mButtonPosition[1].set(mButtonSizeUnit * 4, h - mButtonSizeUnit * 2);       // down
        mButtonPosition[2].set(mButtonSizeUnit * 2, h - mButtonSizeUnit * 4);       // left
        mButtonPosition[3].set(mButtonSizeUnit * 6, h - mButtonSizeUnit * 4);       // right
        mButtonPosition[4].set(w - mButtonSizeUnit * 5, h - mButtonSizeUnit * 3);   // A
        mButtonPosition[5].set(w - mButtonSizeUnit * 2, h - mButtonSizeUnit * 3);   // B
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /*  OLED  */
        mPaint.setColor(Color.WHITE);
        synchronized (mBitmap) {
            if (!mBitmap.isRecycled()) {
                canvas.drawBitmap(mBitmap, mMatrix, mPaint);
            }
        }

        /*  Buttons  */
        mPaint.setStyle(Paint.Style.FILL);
        for (int buttonIdx = 0; buttonIdx < BUTTON_MAX; buttonIdx++) {
            int color = mButtonState[buttonIdx] ? Color.GRAY : Color.LTGRAY;
            mPaint.setColor(color & 0x7FFFFFFF);
            canvas.drawCircle(mButtonPosition[buttonIdx].x, mButtonPosition[buttonIdx].y,
                    mButtonSizeUnit, mPaint);
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
        mIsAvailable = Native.setup(path, SettingsActivity.getEmulationTuning(getContext()));
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
                int fps = SettingsActivity.getEmulationFps(getContext());
                int[] pixels = new int[WIDTH * HEIGHT];
                long baseTime = System.currentTimeMillis();
                long frames = 0;
                while (mIsExecuting) {
                    refreshButtonState();
                    Native.loop(pixels);
                    synchronized (mBitmap) {
                        if (!mBitmap.isRecycled()) {
                            mBitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
                        }
                        postInvalidate();
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

    private void refreshButtonState() {
        boolean[] buttonState = new boolean[BUTTON_MAX];
        for (int touchIdx = 0; touchIdx < mTouchPointCount; touchIdx++) {
            for (int buttonIdx = 0; buttonIdx < BUTTON_MAX; buttonIdx++) {
                if (PointF.length(mTouchPoint[touchIdx].x - mButtonPosition[buttonIdx].x,
                        mTouchPoint[touchIdx].y - mButtonPosition[buttonIdx].y) <= mButtonSizeUnit) {
                    buttonState[buttonIdx] = true;
                }
            }
        }
        for (int buttonIdx = 0; buttonIdx < BUTTON_MAX; buttonIdx++) {
            if (mButtonState[buttonIdx] != buttonState[buttonIdx]) {
                Native.buttonEvent(buttonIdx, buttonState[buttonIdx]);
                mButtonState[buttonIdx] = buttonState[buttonIdx];
            }
        }
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
