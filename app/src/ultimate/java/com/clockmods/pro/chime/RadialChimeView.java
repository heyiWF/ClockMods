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
import com.clockmods.ui.ClockTimeText;
import com.clockmods.ui.ClockTypefaceResolver;
import com.clockmods.ui.ClockView;

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
    // The clock's time layout for this instant, used only to size the burst so it
    // matches the clock's current time font size (type and weight already match via
    // the resolved typeface). Cached base size is recomputed when the view resizes.
    private ClockTimeFormatter.DisplayTime clockTime;
    private float timeFontScale;
    private float baseTextSize;
    private int sizedWidth;
    private int sizedHeight;

    public RadialChimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        circlePaint.setColor(0xFFF4C430);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
        setOnClickListener(view -> stopChime());
    }

    public void startChime(BackgroundRepository repository, long chimeAtMillis) {
        startedAt = SystemClock.uptimeMillis();
        Calendar now = Calendar.getInstance(HourlyChimeController.resolveTimeZone(
                repository.getTimeZoneId()));
        now.setTimeInMillis(chimeAtMillis);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        boolean use24Hour = repository.isUse24Hour();
        boolean useEnglish = repository.isClockUseEnglish();
        displayedTime = ClockTimeFormatter.formatHourlyChime(hour, minute, use24Hour, useEnglish);
        // Reproduce the clock's time string for this instant so the burst can be sized
        // to the exact font size the clock is currently displaying.
        clockTime = ClockTimeFormatter.format(hour, minute, now.get(Calendar.SECOND),
                repository.isShowSeconds(), repository.isBlinkColon(),
                repository.isSmallSeconds(), use24Hour, useEnglish);
        timeFontScale = repository.getTimeFontScale();
        textPaint.setTypeface(ClockTypefaceResolver.resolveTime(getContext(),
                repository.getFontFamily(), repository.isBoldText()));
        baseTextSize = 0f;
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
            textPaint.setTextSize(resolveBaseTextSize() * textScale);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
            ClockTimeText.draw(canvas, displayedTime, getWidth() / 2f, baseline, textPaint);
        }
        if (progress < 1f) postInvalidateDelayed(16L);
        else stopChime();
    }

    /**
     * Lazily resolves and caches the base (unanimated) text size: the exact size the
     * clock renders its time at for this view's dimensions, then capped so the chime
     * string never spills past the same width fraction the clock itself respects.
     * Recomputed whenever the view is resized.
     */
    private float resolveBaseTextSize() {
        int width = getWidth();
        int height = getHeight();
        if (baseTextSize > 0f && width == sizedWidth && height == sizedHeight) {
            return baseTextSize;
        }
        float size = ClockView.measureTimeTextSize(width, height, timeFontScale,
                clockTime, textPaint.getTypeface());
        textPaint.setTextSize(size);
        float maxWidth = width * ClockView.TIME_MAX_WIDTH_FRACTION;
        float measured = textPaint.measureText(displayedTime);
        if (measured > maxWidth) {
            size *= maxWidth / measured;
        }
        sizedWidth = width;
        sizedHeight = height;
        baseTextSize = size;
        return size;
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
