package com.clockmods.pro.chime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.ultimate.clock.UltimateClockPreferences;
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
    private final Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private String animation = ClockPreferences.CHIME_RADIAL;
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
        animation = repository.getChimeAnimation();
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
        String styleId = new UltimateClockPreferences(getContext()).getStyleId();
        timeFontScale = repository.getTimeFontScale(styleId);
        textPaint.setTypeface(ClockTypefaceResolver.resolve(getContext(),
                repository.getFontFamily(styleId), repository.getFontWeight(styleId)));
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
        boolean alternative = ClockPreferences.CHIME_AURORA.equals(animation)
                || ClockPreferences.CHIME_ORBIT.equals(animation)
                || ClockPreferences.CHIME_COMET.equals(animation);
        float textProgress = smoothStep(alternative ? .13f : TEXT_START,
                alternative ? .31f : TEXT_END, progress);
        float opacity = 1f - smoothStep(FADE_START, 1f, progress);
        circlePaint.setAlpha(Math.round(255f * opacity));
        float maxRadius = (float) Math.hypot(getWidth(), getHeight()) / 2f;
        drawEffect(canvas, progress, expand, opacity, maxRadius);
        if (textProgress > 0f) {
            float textScale = 0.94f + 0.06f * easeOutQuint(textProgress);
            textPaint.setColor(alternative ? Color.WHITE : Color.BLACK);
            textPaint.setAlpha(Math.round(255f * textProgress * opacity));
            textPaint.setTextSize(resolveBaseTextSize() * textScale);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
            ClockTimeText.draw(canvas, displayedTime, getWidth() / 2f, baseline, textPaint);
        }
        if (progress < 1f) postInvalidateDelayed(16L);
        else stopChime();
    }

    private void drawEffect(Canvas canvas, float progress, float expand, float opacity,
            float maxRadius) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        if (ClockPreferences.CHIME_RIPPLE.equals(animation)) {
            canvas.drawColor(Color.argb(Math.round(255f * opacity), 244, 196, 48));
            effectPaint.setColor(Color.argb(Math.round(61f * opacity), 0, 0, 0));
            effectPaint.setStyle(Paint.Style.STROKE);
            effectPaint.setStrokeWidth(dp(3f));
            for (int index = 0; index < 3; index++) {
                float wave = easeOutQuint(clamp(progress * 1.36f - index * .25f));
                effectPaint.setAlpha(Math.round(61f * (1f - wave) * opacity));
                canvas.drawCircle(cx, cy, maxRadius * wave, effectPaint);
            }
            effectPaint.setStyle(Paint.Style.FILL);
        } else if (ClockPreferences.CHIME_PULSE.equals(animation)) {
            canvas.drawColor(Color.argb(Math.round(255f * opacity), 244, 196, 48));
            float pulse = Math.abs((float) Math.sin(progress * Math.PI * 4));
            effectPaint.setColor(Color.argb(Math.round(255f * (.045f + .035f * pulse)
                    * opacity), 0, 0, 0));
            canvas.drawCircle(cx, cy, maxRadius * (.54f + .20f * pulse), effectPaint);
        } else if (ClockPreferences.CHIME_AURORA.equals(animation)) {
            drawAurora(canvas, progress, opacity, cx, cy);
        } else if (ClockPreferences.CHIME_ORBIT.equals(animation)) {
            drawOrbit(canvas, progress, opacity, cx, cy);
        } else if (ClockPreferences.CHIME_COMET.equals(animation)) {
            drawComet(canvas, progress, opacity, cx, cy);
        } else {
            canvas.drawCircle(cx, cy, maxRadius * expand, circlePaint);
        }
    }

    private void drawAurora(Canvas canvas, float progress, float opacity, float cx, float cy) {
        float glow = easeOutQuint(clamp(progress / .27f)) * opacity;
        effectPaint.setColor(Color.argb(Math.round(97f * glow), 7, 13, 32));
        canvas.drawRect(0, 0, getWidth(), getHeight(), effectPaint);
        float unit = Math.min(getWidth(), getHeight());
        float drift = easeOutQuint(progress) * getWidth() * .22f;
        int[] colors = {0xFF4DE4F2, 0xFFA78BFA, 0xFFFF7FB0};
        float[] xs = {getWidth() * .22f + drift, getWidth() * .76f - drift * .6f,
                cx + drift * .35f};
        float[] ys = {getHeight() * .42f, getHeight() * .57f, getHeight() * .25f};
        float[] radii = {unit * .78f, unit * .83f, unit * .56f};
        for (int index = 0; index < colors.length; index++) {
            int color = colors[index] & 0x00FFFFFF;
            effectPaint.setColor(Color.WHITE);
            effectPaint.setShader(new RadialGradient(xs[index], ys[index], radii[index],
                    new int[]{color | (Math.round(110f * glow) << 24),
                            color | (Math.round(36f * glow) << 24), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(xs[index], ys[index], radii[index], effectPaint);
            effectPaint.setShader(null);
        }
        for (int index = 0; index < 22; index++) {
            float x = getWidth() * ((index * .6180339f + .11f) % 1f);
            float y = getHeight() * ((index * .381966f + .19f) % 1f);
            float rise = easeOutQuint(clamp(progress * 1.3f - index * .019f));
            effectPaint.setColor(withAlpha(index % 2 == 0 ? colors[0] : colors[2],
                    (.25f + .35f * rise) * glow));
            canvas.drawCircle(x, y - rise * getHeight() * .12f,
                    dp(1.4f + index % 3), effectPaint);
        }
    }

    private void drawOrbit(Canvas canvas, float progress, float opacity, float cx, float cy) {
        float strength = easeOutQuint(clamp(progress / .24f)) * opacity;
        float unit = Math.min(getWidth(), getHeight());
        float radius = unit * (.34f + .16f * easeOutQuint(clamp(progress / .24f)));
        float stroke = Math.max(dp(1.4f), unit * .002f);
        int[] colors = {0xFF4DE4F2, 0xFFA78BFA, 0xFFFF7FB0};
        effectPaint.setColor(withAlpha(colors[0], .09f * strength));
        canvas.drawCircle(cx, cy, radius * 1.22f, effectPaint);
        effectPaint.setStyle(Paint.Style.STROKE);
        for (int index = 0; index < 3; index++) {
            float ring = radius * (1f + index * .18f);
            effectPaint.setColor(withAlpha(colors[index], (.54f - index * .10f) * strength));
            effectPaint.setStrokeWidth(stroke);
            canvas.drawCircle(cx, cy, ring, effectPaint);
            effectPaint.setColor(withAlpha(colors[(index + 2) % 3], .86f * strength));
            effectPaint.setStrokeWidth(stroke * (2.6f - index * .35f));
            arcBounds.set(cx - ring, cy - ring, cx + ring, cy + ring);
            canvas.drawArc(arcBounds,
                    -105f + easeOutQuint(progress) * (185f + index * 76f) + index * 115f,
                    42f + index * 12f, false, effectPaint);
        }
        effectPaint.setStyle(Paint.Style.FILL);
        for (int index = 0; index < 12; index++) {
            double angle = Math.toRadians(index * 30f + 105f + easeOutQuint(progress) * 100f);
            float distance = radius * (1f + (index % 3) * .18f);
            effectPaint.setColor(withAlpha(colors[index % 2], strength));
            canvas.drawCircle(cx + (float) Math.cos(angle) * distance,
                    cy + (float) Math.sin(angle) * distance, dp(2.5f + index % 3), effectPaint);
        }
    }

    private void drawComet(Canvas canvas, float progress, float opacity, float cx, float cy) {
        float strength = easeOutQuint(clamp(progress / .20f)) * opacity;
        float travel = smoothStep(0f, 1f, progress);
        float diagonal = getWidth() + getHeight();
        for (int index = 0; index < 9; index++) {
            float lane = (index + .5f) / 9f;
            float x = -getWidth() * .25f + travel * diagonal * (1.06f + index * .012f)
                    + index * getWidth() * .057f;
            float y = getHeight() * lane - travel * getHeight() * .42f;
            float tailX = x - getWidth() * (.13f + index % 3 * .035f);
            float tailY = y + getHeight() * .11f;
            int tint = index % 2 == 0 ? 0xFF4DE4F2 : 0xFFA78BFA;
            effectPaint.setColor(Color.WHITE);
            effectPaint.setShader(new LinearGradient(tailX, tailY, x, y,
                    new int[]{Color.TRANSPARENT, withAlpha(tint, .36f * strength),
                            withAlpha(Color.WHITE, .85f * strength)},
                    new float[]{0f, .7f, 1f}, Shader.TileMode.CLAMP));
            effectPaint.setStrokeWidth(dp(2.1f + index % 3));
            canvas.drawLine(tailX, tailY, x, y, effectPaint);
            effectPaint.setShader(null);
            effectPaint.setColor(withAlpha(Color.WHITE, .8f * strength));
            canvas.drawCircle(x, y, dp(2f + index % 2), effectPaint);
        }
        float sweep = cx - getWidth() * .6f + travel * getWidth() * 1.2f;
        float radius = Math.min(getWidth(), getHeight()) * .43f;
        effectPaint.setColor(Color.WHITE);
        effectPaint.setShader(new RadialGradient(sweep, cy, radius,
                new int[]{withAlpha(0xFFFF7FB0, .18f * strength), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(sweep, cy, radius, effectPaint);
        effectPaint.setShader(null);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(255f * clamp(alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
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
