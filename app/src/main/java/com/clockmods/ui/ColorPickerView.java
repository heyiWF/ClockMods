package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF saturationValueRect = new RectF();
    private final RectF hueRect = new RectF();
    private final float[] hsv = {210f, 0.75f, 0.35f};
        private final float[] fullColorHsv = {210f, 1f, 1f};
        private final int[] hueColors = {
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
            Color.BLUE, Color.MAGENTA, Color.RED
        };
        private LinearGradient saturationGradient;
        private LinearGradient valueGradient;
        private LinearGradient hueGradient;
    private OnColorChangedListener listener;
    private boolean adjustingHue;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(dp(2));
        markerPaint.setColor(Color.WHITE);
        setMinimumHeight((int) dp(128));
    }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        rebuildSaturationGradient();
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gap = dp(12);
        float hueHeight = dp(28);
        saturationValueRect.set(0, 0, getWidth(), getHeight() - hueHeight - gap);
        hueRect.set(0, saturationValueRect.bottom + gap, getWidth(), getHeight());

        if (saturationGradient == null || valueGradient == null || hueGradient == null) {
            rebuildGradients();
        }
        paint.setShader(saturationGradient);
        canvas.drawRect(saturationValueRect, paint);
        paint.setShader(valueGradient);
        canvas.drawRect(saturationValueRect, paint);

        paint.setShader(hueGradient);
        canvas.drawRect(hueRect, paint);
        paint.setShader(null);

        float selectorX = saturationValueRect.left + hsv[1] * saturationValueRect.width();
        float selectorY = saturationValueRect.top + (1f - hsv[2]) * saturationValueRect.height();
        markerPaint.setColor(hsv[2] > 0.55f ? Color.BLACK : Color.WHITE);
        canvas.drawCircle(selectorX, selectorY, dp(8), markerPaint);

        markerPaint.setColor(Color.WHITE);
        float hueX = hueRect.left + hsv[0] / 360f * hueRect.width();
        canvas.drawRect(hueX - dp(3), hueRect.top - dp(2), hueX + dp(3), hueRect.bottom + dp(2), markerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            adjustingHue = event.getY() >= hueRect.top;
            updateFromTouch(event.getX(), event.getY());
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            updateFromTouch(event.getX(), event.getY());
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            updateFromTouch(event.getX(), event.getY());
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateFromTouch(float x, float y) {
        if (adjustingHue) {
            hsv[0] = clamp(x / Math.max(1f, hueRect.width())) * 360f;
            rebuildSaturationGradient();
        } else {
            hsv[1] = clamp(x / Math.max(1f, saturationValueRect.width()));
            hsv[2] = 1f - clamp(y / Math.max(1f, saturationValueRect.height()));
        }
        invalidate();
        if (listener != null) {
            listener.onColorChanged(getColor());
        }
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float gap = dp(12);
        float hueHeight = dp(28);
        saturationValueRect.set(0, 0, width, height - hueHeight - gap);
        hueRect.set(0, saturationValueRect.bottom + gap, width, height);
        rebuildGradients();
    }

    private void rebuildGradients() {
        rebuildSaturationGradient();
        valueGradient = new LinearGradient(
                0, saturationValueRect.top, 0, saturationValueRect.bottom,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
        hueGradient = new LinearGradient(
                hueRect.left, 0, hueRect.right, 0, hueColors, null, Shader.TileMode.CLAMP);
    }

    private void rebuildSaturationGradient() {
        fullColorHsv[0] = hsv[0];
        saturationGradient = new LinearGradient(
                saturationValueRect.left, 0, saturationValueRect.right, 0,
                Color.WHITE, Color.HSVToColor(fullColorHsv), Shader.TileMode.CLAMP);
    }
}