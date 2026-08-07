package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single-line carousel for the calendar footer. It cycles through the selected date, the
 * day's 宜 line and 忌 line using the same upward-slide motion as the per-cell lunar carousel
 * ({@link CalendarLabelCarouselView}) — one line is shown at a time. A line wider than the view
 * scrolls horizontally (as the clock's detailed-weather line does) instead of wrapping or
 * shrinking, and a single item is never split across frames.
 */
public final class CalendarFooterCarouselView extends View {
    /** One carousel line with its own colour. */
    public static final class Item {
        final String text;
        final int color;

        public Item(String text, int color) {
            this.text = text == null ? "" : text;
            this.color = color;
        }
    }

    private static final long HOLD_MS = 3000L;
    private static final long TRANSITION_MS = 200L;
    private static final long SCROLL_PAUSE_MS = 1000L;
    private static final long FRAME_DELAY_MS = 16L;
    private static final float SCROLL_DP_PER_SECOND = 40f;
    private static final float HORIZONTAL_PADDING_DP = 8f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;
    private List<Item> items = Collections.emptyList();
    private int index;
    private long cycleStartedAt;
    private boolean active;
    private float preferredTextSize;

    public CalendarFooterCarouselView(Context context) { this(context, null); }

    public CalendarFooterCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        preferredTextSize = 14f * getResources().getDisplayMetrics().scaledDensity;
        paint.setTextSize(preferredTextSize);
        paint.setColor(Color.WHITE);
    }

    public void setItems(List<Item> values) {
        items = Collections.unmodifiableList(new ArrayList<>(values));
        index = 0;
        cycleStartedAt = 0L;
        List<String> descriptions = new ArrayList<>();
        for (Item item : items) descriptions.add(item.text);
        setContentDescription(TextUtils.join("，", descriptions));
        invalidate();
    }

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
        Item current = items.get(index);

        if (items.size() == 1 || !animate) {
            drawItem(canvas, current, 0f, animate ? now - cycleStartedAt : 0L);
            if (animate && overflow(current)) postInvalidateDelayed(FRAME_DELAY_MS);
            return;
        }

        long holdMs = holdDurationFor(current);
        long elapsed = now - cycleStartedAt;
        Item next = items.get((index + 1) % items.size());
        if (elapsed < holdMs) {
            drawItem(canvas, current, 0f, elapsed);
        } else if (elapsed < holdMs + TRANSITION_MS) {
            float progress = (float) (elapsed - holdMs) / TRANSITION_MS;
            float slide = slideDistance();
            drawItem(canvas, current, -progress * slide, holdMs);
            drawItem(canvas, next, (1f - progress) * slide, 0L);
        } else {
            index = (index + 1) % items.size();
            cycleStartedAt = now;
            drawItem(canvas, items.get(index), 0f, 0L);
        }
        postInvalidateDelayed(FRAME_DELAY_MS);
    }

    private void drawItem(Canvas canvas, Item item, float verticalOffset, long itemElapsed) {
        if (item == null || item.text.length() == 0) return;
        paint.setColor(item.color);
        paint.setTextSize(preferredTextSize);
        float centerY = getHeight() / 2f;
        float lineHeight = paint.descent() - paint.ascent();
        float bandTop = Math.max(0f, centerY - lineHeight / 2f);
        float bandBottom = Math.min(getHeight(), centerY + lineHeight / 2f);
        float baseline = centerY - (paint.descent() + paint.ascent()) / 2f + verticalOffset;
        float padding = HORIZONTAL_PADDING_DP * density;
        float available = Math.max(1f, getWidth() - padding * 2f);
        float textWidth = paint.measureText(item.text);
        canvas.save();
        if (textWidth <= available) {
            canvas.clipRect(0f, bandTop, getWidth(), bandBottom);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(item.text, getWidth() / 2f, baseline, paint);
        } else {
            float overflow = textWidth - available;
            canvas.clipRect(padding, bandTop, padding + available, bandBottom);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(item.text, padding - overflow * scrollProgress(itemElapsed, overflow),
                    baseline, paint);
        }
        canvas.restore();
    }

    /** Vertical travel of one carousel step: the text line height (matches the cell carousel). */
    private float slideDistance() {
        paint.setTextSize(preferredTextSize);
        return paint.descent() - paint.ascent();
    }

    private boolean overflow(Item item) {
        paint.setTextSize(preferredTextSize);
        float padding = HORIZONTAL_PADDING_DP * density;
        return paint.measureText(item.text) > Math.max(1f, getWidth() - padding * 2f);
    }

    private float scrollProgress(long itemElapsed, float overflow) {
        if (overflow <= 0f) return 0f;
        long scrollMs = (long) Math.ceil(overflow / (SCROLL_DP_PER_SECOND * density) * 1000f);
        if (scrollMs <= 0L) return 1f;
        return Math.max(0f, Math.min(1f, (itemElapsed - SCROLL_PAUSE_MS) / (float) scrollMs));
    }

    private long holdDurationFor(Item item) {
        paint.setTextSize(preferredTextSize);
        float padding = HORIZONTAL_PADDING_DP * density;
        float overflow = paint.measureText(item.text) - Math.max(1f, getWidth() - padding * 2f);
        if (overflow <= 0f) return HOLD_MS;
        long scrollMs = (long) Math.ceil(overflow / (SCROLL_DP_PER_SECOND * density) * 1000f);
        return Math.max(HOLD_MS, SCROLL_PAUSE_MS * 2L + scrollMs);
    }

    private boolean animationsEnabled() {
        try {
            return Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (RuntimeException ignored) { return true; }
    }
}
