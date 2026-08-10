package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public final class WeatherIconView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private WeatherIcon icon;

    public WeatherIconView(Context context) { this(context, null); }

    public WeatherIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
    }

    public void setIconCode(String code) {
        setIconCode(code, true);
    }

    public void setIconCode(String code, boolean fill) {
        icon = WeatherIcon.load(getContext(), code, fill);
        setVisibility(icon == null ? INVISIBLE : VISIBLE);
        invalidate();
    }

    public void setIconColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (icon == null) return;
        float size = Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
                getHeight() - getPaddingTop() - getPaddingBottom());
        float left = getPaddingLeft() + (getWidth() - getPaddingLeft() - getPaddingRight() - size) / 2f;
        float top = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom() - size) / 2f;
        icon.draw(canvas, left, top, size, paint);
    }
}