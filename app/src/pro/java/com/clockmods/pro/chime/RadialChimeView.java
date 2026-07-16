package com.clockmods.pro.chime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class RadialChimeView extends View {
    private static final long DURATION = 5000L;
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private long startedAt;

    public RadialChimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        circlePaint.setColor(0xFFF4C430);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-light",
                android.graphics.Typeface.NORMAL));
    }

    public void startChime() {
        startedAt = SystemClock.uptimeMillis();
        setVisibility(VISIBLE);
        invalidate();
    }

    public void stopChime() {
        setVisibility(GONE);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float progress = Math.min(1f, (SystemClock.uptimeMillis() - startedAt) / (float) DURATION);
        float expand = progress < 0.62f
                ? 1f - (float) Math.pow(1f - progress / 0.62f, 4)
                : 1f;
        float maxRadius = (float) Math.hypot(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, maxRadius * expand, circlePaint);
        if (expand > 0.72f) {
            textPaint.setTextSize(Math.min(getWidth() * 0.24f, getHeight() * 0.24f));
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(time, getWidth() / 2f, baseline, textPaint);
        }
        if (progress < 1f) postInvalidateDelayed(16L);
        else stopChime();
    }
}