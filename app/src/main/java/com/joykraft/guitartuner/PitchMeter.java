package com.joykraft.guitartuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class PitchMeter extends View {

    private float mPitchError;
    private Paint mPaint = new Paint();
    private float[] mHSV = new float[] {0, 1, 1};
    private boolean mPitchDetected = false;

    public PitchMeter(Context context) {
        super(context);
        init(null, 0);
    }

    public PitchMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PitchMeter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    void setPitchError(float error) {
        mPitchError = error;
        mPitchDetected = true;
        invalidate();
    }

    void setPitchDetected(boolean pitchDetected) {
        mPitchDetected = pitchDetected;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        drawGauge(canvas, width, height);

        if (mPitchDetected) {
            drawNeedle(canvas, width, height);
        }
    }

    private void drawGauge(Canvas canvas, float width, float height) {
        canvas.save();

        mPaint.setStrokeWidth(5);
        mPaint.setColor(Color.rgb(127, 0, 0));
        canvas.rotate(-30f, width / 2, height);
        canvas.drawLine(width / 2, height, width / 2, height / 20, mPaint);
        canvas.rotate(60f, width / 2, height);
        canvas.drawLine(width / 2, height, width / 2, height / 20, mPaint);

        canvas.restore();

        int green = 0;

        if (mPitchDetected) {
            /* black to green as target is reached */
            green = (int) (255f * (1f - 10f * Math.abs(mPitchError)));
            green = Math.max(0, Math.min(green, 255));
        }

        mPaint.setColor(Color.rgb(0, green, 0));
        canvas.drawCircle(width / 2, height / 20, height / 20, mPaint);
    }

    private void drawNeedle(Canvas canvas, float width, float height) {
        /* 1 semitone = 60 degrees (clip range to keep visibility) */
        float angle = Math.max(-60, Math.min(60 * mPitchError, 60));

        canvas.save();
        canvas.rotate(angle, width / 2, height);

        /* opaque when error is low, transparent when error is high */
        int alpha = (int) (1020f * (0.75f - Math.abs(mPitchError)));
        alpha = Math.max(26, Math.min(alpha, 255));

        /* red to yellow to green as target is reached */
        mHSV[0] = 120f * (1f - 2f * Math.abs(mPitchError));
        mHSV[0] = Math.max(0f, Math.min(mHSV[0], 120f));

        mPaint.setStrokeWidth(15);
        mPaint.setColor(Color.HSVToColor(alpha, mHSV));
        canvas.drawLine(width / 2, height, width / 2, height / 20, mPaint);

        canvas.restore();
    }
}
