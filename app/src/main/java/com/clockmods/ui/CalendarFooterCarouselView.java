package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
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
 *
 * <p>A line that starts with its 宜/忌 hint keeps that glyph bold and pinned at the left edge
 * while only the items scroll past it. The scroll stops at the tail rather than wrapping around:
 * here the end of a line is the cue to hand over to the next one, which is the opposite of what
 * a permanent almanac line wants — it shows 宜 and 忌 side by side for good, with nothing to hand
 * over to, so there it runs as an endless belt.</p>
 */
public final class CalendarFooterCarouselView extends View {
    /** One carousel line with its own colour. */
    public static final class Item {
        final String text;
        final int color;
        /** When non-empty and {@code text} starts with it, drawn bold and never scrolled past. */
        String pinnedPrefix = "";

        public Item(String text, int color) {
            this.text = text == null ? "" : text;
            this.color = color;
        }

        /** Splits {@code hint} out of the head of the text as the bold pinned glyph. */
        public Item withPinnedPrefix(String hint) {
            if (hint != null && hint.length() > 0 && text.startsWith(hint)
                    && text.length() > hint.length()) {
                pinnedPrefix = hint;
            }
            return this;
        }
    }

    private static final long TRANSITION_MS = 200L;
    private static final long FRAME_DELAY_MS = 16L;
    private static final float HORIZONTAL_PADDING_DP = 8f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect badgeBounds = new Rect();
    private com.clockmods.sdk.style.MarqueeSpec marquee = com.clockmods.sdk.style.MarqueeSpec.DEFAULT;
    private final Runnable redraw = this::invalidate;

    public void setMarqueeSpec(com.clockmods.sdk.style.MarqueeSpec value) {
        if (value == null) value = com.clockmods.sdk.style.MarqueeSpec.DEFAULT;
        if (marquee.equals(value)) return;
        marquee = value;
        cycleStartedAt = 0L;
        removeCallbacks(redraw);
        invalidate();
    }

    private void scheduleFrame(long delay) {
        removeCallbacks(redraw);
        if (active && isShown() && getWindowVisibility() == VISIBLE
                && delay != com.clockmods.sdk.style.MarqueeSpec.IDLE) postDelayed(redraw, delay);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (redraw == null) return;
        removeCallbacks(redraw);
        if (visibility == VISIBLE && active) invalidate();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (redraw == null) return;
        removeCallbacks(redraw);
        if (isShown() && active) invalidate();
    }

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
        preferredTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                14f, getResources().getDisplayMetrics());
        paint.setTextSize(preferredTextSize);
        paint.setColor(Color.WHITE);
        boldPaint.setTextSize(preferredTextSize);
        boldPaint.setColor(Color.WHITE);
    }

    public void setItems(List<Item> values) {
        boolean unchanged = items.size() == values.size();
        for (int i = 0; unchanged && i < items.size(); i++) {
            Item before = items.get(i), after = values.get(i);
            unchanged = before.text.equals(after.text) && before.color == after.color
                    && before.pinnedPrefix.equals(after.pinnedPrefix);
        }
        if (unchanged) return;
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
        boldPaint.setTextSize(size);
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        paint.setTypeface(typeface);
        // The pinned 宜/忌 glyph is the bold of whatever face the line itself runs in.
        boldPaint.setTypeface(Typeface.create(typeface == null ? Typeface.DEFAULT : typeface,
                Typeface.BOLD));
        invalidate();
    }

    public void setActive(boolean value) {
        if (value == active) return;
        active = value;
        removeCallbacks(redraw);
        if (active) {
            cycleStartedAt = 0L;
            invalidate();
        }
    }

    @Override protected void onDetachedFromWindow() {
        active = false;
        removeCallbacks(redraw);
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        removeCallbacks(redraw);
        if (items.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (cycleStartedAt == 0L) cycleStartedAt = now;
        Item current = items.get(index);
        if (!animationsEnabled()) { drawItem(canvas, current, 0f, 0L); return; }

        if (items.size() == 1 || !active) {
            drawItem(canvas, current, 0f, active ? now - cycleStartedAt : 0L);
            if (active) scheduleFrame(marquee.scrollFrameDelay(now - cycleStartedAt,
                    bodyWidth(current) - bodyRoom(current), density));
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
        long itemElapsed = now - cycleStartedAt;
        Item visible = items.get(index);
        long holdRemaining = holdDurationFor(visible) - itemElapsed;
        scheduleFrame(holdRemaining > 0L
                ? Math.min(holdRemaining, marquee.scrollFrameDelay(itemElapsed,
                        bodyWidth(visible) - bodyRoom(visible), density)) : FRAME_DELAY_MS);
    }

    private void drawItem(Canvas canvas, Item item, float verticalOffset, long itemElapsed) {
        if (item == null || item.text.isEmpty()) return;
        paint.setColor(item.color);
        paint.setTextSize(preferredTextSize);
        paint.setTextAlign(Paint.Align.LEFT);
        boldPaint.setTextSize(preferredTextSize * .74f);
        float centerY = getHeight() / 2f;
        float lineHeight = paint.descent() - paint.ascent();
        float top = Math.max(0f, centerY - lineHeight / 2f);
        float bottom = Math.min(getHeight(), centerY + lineHeight / 2f);
        float baseline = centerY - (paint.descent() + paint.ascent()) / 2f + verticalOffset;
        float padding = HORIZONTAL_PADDING_DP * density;
        float leading = prefixWidth(item);
        String body = item.text.substring(item.pinnedPrefix.length()).trim();
        float room = Math.max(1f, getWidth() - padding * 2f - leading);
        float width = paint.measureText(body);
        float excess = Math.max(0f, width - room);
        float x = excess == 0f ? (getWidth() - leading - width) / 2f : padding;
        int save = canvas.save();
        canvas.clipRect(0f, top, getWidth(), bottom);
        if (leading > 0f) {
            float diameter = lineHeight * .94f;
            AlmanacBadge.draw(canvas, item.pinnedPrefix, x + diameter / 2f,
                    centerY + verticalOffset, diameter, item.color,
                    boldPaint, badgePaint, badgeBounds);
        }
        canvas.save();
        canvas.clipRect(x + leading, top, getWidth() - padding, bottom);
        canvas.drawText(body, x + leading
                - marquee.scrollOffset(itemElapsed, excess, density), baseline, paint);
        canvas.restore();
        canvas.restoreToCount(save);
    }

    private float prefixWidth(Item item) {
        if (item.pinnedPrefix.isEmpty()) return 0f;
        paint.setTextSize(preferredTextSize);
        return (paint.descent() - paint.ascent()) * ( .94f + .30f );
    }

    /** Vertical travel of one carousel step: the text line height (matches the cell carousel). */
    private float slideDistance() {
        paint.setTextSize(preferredTextSize);
        return paint.descent() - paint.ascent();
    }

    private boolean overflow(Item item) {
        return bodyWidth(item) > bodyRoom(item);
    }

    /** Read the head, scroll once, and hold the tail before sliding to the next line. */
    private long holdDurationFor(Item item) {
        return marquee.holdMillis(bodyWidth(item) - bodyRoom(item), density);
    }

    /** Where the scrolling body of an item may draw: beside the pinned glyph, inside the padding. */
    private float bodyRoom(Item item) {
        float padding = HORIZONTAL_PADDING_DP * density;
        float prefixWidth = prefixWidth(item);
        return Math.max(1f, getWidth() - padding * 2f - prefixWidth);
    }

    private float bodyWidth(Item item) {
        paint.setTextSize(preferredTextSize);
        return paint.measureText(item.text.substring(item.pinnedPrefix.length()).trim());
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
