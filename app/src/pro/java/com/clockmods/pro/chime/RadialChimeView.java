package com.clockmods.pro.chime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.ui.ClockTimeFormatter;
import com.clockmods.ui.ClockTypefaceResolver;

import java.util.Calendar;

public final class RadialChimeView extends View {
    private static final long DURATION = 5000L;
    private static final float EXPAND_END = 0.52f;
    private static final float TEXT_START = 0.28f;
    private static final float TEXT_END = 0.48f;
    private static final float FADE_START = 0.82f;
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private long startedAt;
    private String displayedTime;

    public RadialChimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        circlePaint.setColor(0xFFF4C430);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void startChime(BackgroundRepository repository) {
        startedAt = SystemClock.uptimeMillis();
        Calendar now = Calendar.getInstance();
        displayedTime = ClockTimeFormatter.formatHourlyChime(
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
            repository.isUse24Hour(), repository.isClockUseEnglish());
        textPaint.setTypeface(ClockTypefaceResolver.resolveTime(getContext(),
                repository.getFontFamily(), repository.isBoldText()));
        setVisibility(VISIBLE);
        invalidate();
    }

    public void stopChime() {
        setVisibility(GONE);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float progress = Math.min(1f, (SystemClock.uptimeMillis() - startedAt) / (float) DURATION);
        float expand = easeOutQuint(Math.min(1f, progress / EXPAND_END));
        float textProgress = smoothStep(TEXT_START, TEXT_END, progress);
        float opacity = 1f - smoothStep(FADE_START, 1f, progress);
        circlePaint.setAlpha(Math.round(255f * opacity));
        textPaint.setAlpha(Math.round(255f * textProgress * opacity));
        float maxRadius = (float) Math.hypot(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, maxRadius * expand, circlePaint);
        if (textProgress > 0f) {
            float textScale = 0.94f + 0.06f * easeOutQuint(textProgress);
            textPaint.setTextSize(Math.min(getWidth() * 0.24f, getHeight() * 0.24f) * textScale);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(displayedTime, getWidth() / 2f, baseline, textPaint);
        }
        if (progress < 1f) postInvalidateDelayed(16L);
        else stopChime();
    }

    private static float smoothStep(float start, float end, float value) {
        float progress = Math.max(0f, Math.min(1f, (value - start) / (end - start)));
        return progress * progress * (3f - 2f * progress);
    }

    private static float easeOutQuint(float value) {
        float remaining = 1f - value;
        return 1f - remaining * remaining * remaining * remaining * remaining;
    }
}