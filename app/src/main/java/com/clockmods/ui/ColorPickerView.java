package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.os.Bundle;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

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
    private CharSequence accessibilityLabel;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(dp(2));
        markerPaint.setColor(Color.WHITE);
        setMinimumHeight((int) dp(128));
        setFocusable(true);
        updateAccessibilityDescription();
    }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        rebuildSaturationGradient();
        updateAccessibilityDescription();
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public void setAccessibilityLabel(CharSequence label) {
        accessibilityLabel = label;
        updateAccessibilityDescription();
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

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.SeekBar.class.getName());
        if (Build.VERSION.SDK_INT >= 19) {
            info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0f, 360f, hsv[0]));
        }
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        if (Build.VERSION.SDK_INT >= 24) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (Build.VERSION.SDK_INT >= 24
            && action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()
            && arguments != null) {
            float hue = arguments.getFloat(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, hsv[0]);
            setHue(hue);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            setHue(hsv[0] - 5f);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            setHue(hsv[0] + 5f);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            setHue(hsv[0] - 5f);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            setHue(hsv[0] + 5f);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        updateAccessibilityDescription();
        if (listener != null) {
            listener.onColorChanged(getColor());
        }
    }

    private void setHue(float hue) {
        hsv[0] = (hue % 360f + 360f) % 360f;
        rebuildSaturationGradient();
        invalidate();
        updateAccessibilityDescription();
        if (listener != null) listener.onColorChanged(getColor());
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    private void updateAccessibilityDescription() {
        String value = String.format(java.util.Locale.ROOT, "#%06X", getColor() & 0xFFFFFF);
        if (Build.VERSION.SDK_INT >= 30) {
            setContentDescription(accessibilityLabel);
            setStateDescription(value);
        } else if (accessibilityLabel != null) {
            setContentDescription(accessibilityLabel + "，" + value);
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