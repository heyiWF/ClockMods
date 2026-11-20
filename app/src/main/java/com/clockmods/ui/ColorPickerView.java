package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;

import com.clockmods.platform.ApiCompat;
import android.view.KeyEvent;
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

    /**
     * Creates a picker. On API 14+ the {@code ColorPickerViewA11y} subclass supplies the
     * accessibility node info overrides; Android 1.0 must never link those API 14 types, so it
     * gets the plain view. The subclass is resolved reflectively to keep it out of this class's
     * verifier graph.
     */
    public static ColorPickerView create(Context context) {
        if (ApiCompat.SDK_INT >= 14) {
            try {
                Object view = Class.forName("com.clockmods.ui.ColorPickerViewA11y")
                        .getConstructor(Context.class)
                        .newInstance(context);
                if (view instanceof ColorPickerView) {
                    return (ColorPickerView) view;
                }
            } catch (Throwable ignored) {
                // Fall back to the plain picker.
            }
        }
        return new ColorPickerView(context);
    }

    /** Current hue in degrees (0-360), used by the accessibility subclass. */
    public float getHue() {
        return hsv[0];
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

    protected void setHue(float hue) {
        hsv[0] = (hue % 360f + 360f) % 360f;
        rebuildSaturationGradient();
        invalidate();
        updateAccessibilityDescription();
        if (listener != null) listener.onColorChanged(getColor());
        ApiCompat.sendAccessibilityEventCompat(this,
                android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    private void updateAccessibilityDescription() {
        String value = String.format(java.util.Locale.US, "#%06X", getColor() & 0xFFFFFF);
        if (ApiCompat.SDK_INT >= 30) {
            com.clockmods.platform.ApiCompat.setContentDescriptionCompat(
                    this, accessibilityLabel);
            // View.setStateDescription(CharSequence) requires API 30 / compileSdk 30.
            try {
                android.view.View.class.getMethod("setStateDescription", CharSequence.class)
                        .invoke(this, (CharSequence) value);
            } catch (Throwable ignored) {
                // Older platforms use the combined content description below.
            }
        } else if (accessibilityLabel != null) {
            com.clockmods.platform.ApiCompat.setContentDescriptionCompat(
                    this, accessibilityLabel + "，" + value);
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