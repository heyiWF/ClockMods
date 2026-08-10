package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-cell calendar carousel: cycles the lunar date and festival labels with the same
 * upward-slide + horizontal-marquee motion as the footer 宜忌 carousel
 * ({@link CalendarFooterCarouselView}). A label longer than {@link #MAX_STATIC_CHARS} characters
 * scrolls horizontally while it is shown; shorter labels stay centered (never scroll).
 *
 * <p>Unlike the single footer instance, up to 42 of these are on screen at once, so a settled
 * static label schedules only one wake-up (at its next transition) instead of animating every
 * frame — only labels that actually scroll or transition redraw continuously.
 */
public final class CalendarLabelCarouselView extends View {
    private static final long HOLD_MS = 3000L;
    private static final long TRANSITION_MS = 200L;
    private static final long SCROLL_PAUSE_MS = 1000L;
    private static final long FRAME_DELAY_MS = 16L;
    private static final float SCROLL_DP_PER_SECOND = 40f;
    private static final float HORIZONTAL_PADDING_DP = 2f;
    private static final int MAX_STATIC_CHARS = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;
    private List<String> items = Collections.emptyList();
    private int index;
    private long cycleStartedAt;
    private boolean active;
    private float preferredTextSize;

    public CalendarLabelCarouselView(Context context) { this(context, null); }

    public CalendarLabelCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        paint.setColor(Color.WHITE);
        preferredTextSize = 13f * getResources().getDisplayMetrics().scaledDensity;
        paint.setTextSize(preferredTextSize);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public void setItems(List<String> values) {
        items = Collections.unmodifiableList(new ArrayList<>(values));
        index = 0;
        cycleStartedAt = 0L;
        setContentDescription(TextUtils.join("，", items));
        invalidate();
    }

    public void setTextColor(int color) { paint.setColor(color); invalidate(); }

    public void setTextSizePx(float size) {
        preferredTextSize = size;
        paint.setTextSize(size);
        invalidate();
    }

    public void setTypeface(Typeface typeface) { paint.setTypeface(typeface); invalidate(); }

    public void setActive(boolean value) {
        if (value == active) return;
        active = value;
        if (active) {
            cycleStartedAt = 0L;
            invalidate();
        }
    }

    @Override protected void onDetachedFromWindow() {
        active = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (cycleStartedAt == 0L) cycleStartedAt = now;
        boolean animate = active && animationsEnabled();
        String current = items.get(index);

        if (items.size() == 1 || !animate) {
            drawItem(canvas, current, 0f, animate ? now - cycleStartedAt : 0L);
            if (animate && overflow(current)) postInvalidateDelayed(FRAME_DELAY_MS);
            return;
        }

        long holdMs = holdDurationFor(current);
        long elapsed = now - cycleStartedAt;
        String next = items.get((index + 1) % items.size());
        if (elapsed < holdMs) {
            drawItem(canvas, current, 0f, elapsed);
            // Only a scrolling label needs per-frame redraws; a static one just waits.
            postInvalidateDelayed(overflow(current) ? FRAME_DELAY_MS : holdMs - elapsed);
        } else if (elapsed < holdMs + TRANSITION_MS) {
            float progress = (float) (elapsed - holdMs) / TRANSITION_MS;
            float slide = slideDistance();
            drawItem(canvas, current, -progress * slide, holdMs);
            drawItem(canvas, next, (1f - progress) * slide, 0L);
            postInvalidateDelayed(FRAME_DELAY_MS);
        } else {
            index = (index + 1) % items.size();
            cycleStartedAt = now;
            drawItem(canvas, items.get(index), 0f, 0L);
            postInvalidateDelayed(FRAME_DELAY_MS);
        }
    }

    private void drawItem(Canvas canvas, String text, float verticalOffset, long itemElapsed) {
        if (text == null || text.length() == 0) return;
        paint.setTextSize(preferredTextSize);
        float centerY = getHeight() / 2f;
        float lineHeight = paint.descent() - paint.ascent();
        float bandTop = Math.max(0f, centerY - lineHeight / 2f);
        float bandBottom = Math.min(getHeight(), centerY + lineHeight / 2f);
        float baseline = centerY - (paint.descent() + paint.ascent()) / 2f + verticalOffset;
        float padding = HORIZONTAL_PADDING_DP * density;
        float available = Math.max(1f, getWidth() - padding * 2f);
        float textWidth = paint.measureText(text);
        canvas.save();
        if (textWidth <= available || !scrollable(text)) {
            // Fits, or a short (<=3 char) label: keep centered, shrinking a rare overflow to fit.
            if (textWidth > available && textWidth > 0f) {
                paint.setTextSize(preferredTextSize * available / textWidth);
            }
            canvas.clipRect(0f, bandTop, getWidth(), bandBottom);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, getWidth() / 2f, baseline, paint);
            paint.setTextSize(preferredTextSize);
        } else {
            float overflow = textWidth - available;
            canvas.clipRect(padding, bandTop, padding + available, bandBottom);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(text, padding - overflow * scrollProgress(itemElapsed, overflow),
                    baseline, paint);
        }
        canvas.restore();
    }

    /** A label scrolls horizontally only when it is longer than {@link #MAX_STATIC_CHARS}. */
    private boolean scrollable(String text) {
        return text.codePointCount(0, text.length()) > MAX_STATIC_CHARS;
    }

    private boolean overflow(String text) {
        if (!scrollable(text)) return false;
        paint.setTextSize(preferredTextSize);
        float padding = HORIZONTAL_PADDING_DP * density;
        return paint.measureText(text) > Math.max(1f, getWidth() - padding * 2f);
    }

    private float scrollProgress(long itemElapsed, float overflow) {
        if (overflow <= 0f) return 0f;
        long scrollMs = (long) Math.ceil(overflow / (SCROLL_DP_PER_SECOND * density) * 1000f);
        if (scrollMs <= 0L) return 1f;
        return Math.max(0f, Math.min(1f, (itemElapsed - SCROLL_PAUSE_MS) / (float) scrollMs));
    }

    private long holdDurationFor(String text) {
        if (!scrollable(text)) return HOLD_MS;
        paint.setTextSize(preferredTextSize);
        float padding = HORIZONTAL_PADDING_DP * density;
        float overflow = paint.measureText(text) - Math.max(1f, getWidth() - padding * 2f);
        if (overflow <= 0f) return HOLD_MS;
        long scrollMs = (long) Math.ceil(overflow / (SCROLL_DP_PER_SECOND * density) * 1000f);
        return Math.max(HOLD_MS, SCROLL_PAUSE_MS * 2L + scrollMs);
    }

    private float slideDistance() {
        paint.setTextSize(preferredTextSize);
        return paint.descent() - paint.ascent();
    }

    @SuppressWarnings("deprecation")
    private boolean animationsEnabled() {
        try {
            float scale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                    ? Settings.Global.getFloat(getContext().getContentResolver(),
                            Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                    : Settings.System.getFloat(getContext().getContentResolver(),
                            Settings.System.ANIMATOR_DURATION_SCALE, 1f);
            return scale > 0f;
        } catch (RuntimeException ignored) { return true; }
    }
}
