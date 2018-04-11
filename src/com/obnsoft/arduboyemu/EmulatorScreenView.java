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


    private static final int WIDTH  = ArduboyEmulator.SCREEN_WIDTH;
    private static final int HEIGHT = ArduboyEmulator.SCREEN_HEIGHT;

    private static final int BUTTON_SIZE_UNIT_BASE = 24;
    private static final int TOUCH_STATE_MAX = 10;

    private Bitmap      mBitmap;
    private Matrix      mMatrix;
    private Paint       mPaint;

    private boolean[]   mButtonState = new boolean[Native.BUTTON_MAX];
    private PointF[]    mButtonPosition = new PointF[Native.BUTTON_MAX];
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

        for (int buttonIdx = 0; buttonIdx < Native.BUTTON_MAX; buttonIdx++) {
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
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int scale = Math.min(w / WIDTH, h / HEIGHT);
        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate((w - WIDTH * scale) / 2, (h - HEIGHT * scale) / 4);

        mButtonPosition[Native.BUTTON_UP   ].set(mButtonSizeUnit * 4, h - mButtonSizeUnit * 6);
        mButtonPosition[Native.BUTTON_DOWN ].set(mButtonSizeUnit * 4, h - mButtonSizeUnit * 2);
        mButtonPosition[Native.BUTTON_LEFT ].set(mButtonSizeUnit * 2, h - mButtonSizeUnit * 4);
        mButtonPosition[Native.BUTTON_RIGHT].set(mButtonSizeUnit * 6, h - mButtonSizeUnit * 4);
        mButtonPosition[Native.BUTTON_A    ].set(w - mButtonSizeUnit * 5, h - mButtonSizeUnit * 3);
        mButtonPosition[Native.BUTTON_B    ].set(w - mButtonSizeUnit * 2, h - mButtonSizeUnit * 3);
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
        for (int buttonIdx = 0; buttonIdx < Native.BUTTON_MAX; buttonIdx++) {
            int color = mButtonState[buttonIdx] ? Color.GRAY : Color.LTGRAY;
            mPaint.setColor(color & 0x7FFFFFFF);
            canvas.drawCircle(mButtonPosition[buttonIdx].x, mButtonPosition[buttonIdx].y,
                    mButtonSizeUnit, mPaint);
        }
    }

    /*-----------------------------------------------------------------------*/

    public boolean[] updateButtonState() {
        for (int buttonIdx = 0; buttonIdx < Native.BUTTON_MAX; buttonIdx++) {
            mButtonState[buttonIdx] = false;
        }
        float threshold = mButtonSizeUnit * 1.5f;
        for (int touchIdx = 0; touchIdx < mTouchPointCount; touchIdx++) {
            for (int buttonIdx = 0; buttonIdx < Native.BUTTON_MAX; buttonIdx++) {
                if (PointF.length(mTouchPoint[touchIdx].x - mButtonPosition[buttonIdx].x,
                        mTouchPoint[touchIdx].y - mButtonPosition[buttonIdx].y) <= threshold) {
                    mButtonState[buttonIdx] = true;
                }
            }
        }
        return mButtonState;
    }

    public void updateScreen(int[] pixels) {
        synchronized (mBitmap) {
            if (!mBitmap.isRecycled()) {
                mBitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
            }
            postInvalidate();
        }
    }

    public void onDestroy() {
        synchronized (mBitmap) {
            mBitmap.recycle();
        }
    }

}
