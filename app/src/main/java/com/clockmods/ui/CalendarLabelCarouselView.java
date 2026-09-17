package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
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
 * frame — only labels that actually scroll or transition redraw continuously. Every cell reads
 * the same monotonic timeline so their vertical transitions always share one frame phase.
 */
public final class CalendarLabelCarouselView extends View {
    private static final long SCROLL_PAUSE_MS = 1000L;
    private static final long FRAME_DELAY_MS = 16L;
    private static final float SCROLL_DP_PER_SECOND = 40f;
    private static final float HORIZONTAL_PADDING_DP = 2f;
    private static final int MAX_STATIC_CHARS = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;
    private List<String> items = Collections.emptyList();
    private boolean active;
    private float preferredTextSize;

    public CalendarLabelCarouselView(Context context) { this(context, null); }

    public CalendarLabelCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        paint.setColor(Color.WHITE);
        preferredTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                13f, getResources().getDisplayMetrics());
        paint.setTextSize(preferredTextSize);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public void setItems(List<String> values) {
        items = Collections.unmodifiableList(new ArrayList<>(values));
        setContentDescription(TextUtils.join("，", items));
        invalidate();
    }

    public void setTextColor(int color) { paint.setColor(color); invalidate(); }

    public void setTextSizePx(float size) {
        if (size == preferredTextSize) return;
        preferredTextSize = size;
        paint.setTextSize(size);
        // The view now measures to its own line, so a new size is a new height.
        requestLayout();
        invalidate();
    }

    /**
     * Measures one line of the label rather than whatever the parent offers.
     *
     * <p>A plain {@link View} reports the whole {@code AT_MOST} spec as its measured height, which
     * is harmless where the carousel is the only thing in its slot but not in the portrait 周程
     * strip: there the cell stacks this label under a weighted number, so the carousel claimed the
     * entire cell, the cell overflowed, and {@link android.widget.LinearLayout} took the excess out
     * of the label above and the number in the middle — leaving the number no box to draw its glyph
     * in and the weekday squashed to a few pixels. The line the label actually needs is the paint's
     * ascent-to-descent span, which is also what {@link #drawItem} centres on.</p>
     */
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int lineHeight = Math.max(1, Math.round(paint.descent() - paint.ascent()));
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(lineHeight, heightMeasureSpec));
    }

    public void setTypeface(Typeface typeface) {
        if (typeface == paint.getTypeface()) return;
        paint.setTypeface(typeface);
        // A different family has different font metrics, so the measured line can change.
        requestLayout();
        invalidate();
    }

    public void setActive(boolean value) {
        if (value == active) return;
        active = value;
        if (active) invalidate();
    }

    @Override protected void onDetachedFromWindow() {
        active = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty()) return;
        long now = SystemClock.uptimeMillis();

        if (!active) {
            drawItem(canvas, items.get(0), 0f, 0L);
            return;
        }

        long elapsed = CalendarCarouselTimeline.elapsedAt(now);
        int index = CalendarCarouselTimeline.indexAt(now, items.size());
        String current = items.get(index);
        if (items.size() == 1) {
            drawItem(canvas, current, 0f, elapsed);
            if (overflow(current)) postInvalidateDelayed(FRAME_DELAY_MS);
            return;
        }

        String next = items.get((index + 1) % items.size());
        if (elapsed < CalendarCarouselTimeline.HOLD_MS) {
            drawItem(canvas, current, 0f, elapsed);
            // Only a scrolling label needs per-frame redraws; a static one just waits.
            postInvalidateDelayed(overflow(current) ? FRAME_DELAY_MS
                    : CalendarCarouselTimeline.HOLD_MS - elapsed);
        } else {
            float progress = (float) (elapsed - CalendarCarouselTimeline.HOLD_MS)
                    / CalendarCarouselTimeline.TRANSITION_MS;
            float slide = slideDistance();
            drawItem(canvas, current, -progress * slide, CalendarCarouselTimeline.HOLD_MS);
            drawItem(canvas, next, (1f - progress) * slide, 0L);
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

    private float slideDistance() {
        paint.setTextSize(preferredTextSize);
        return paint.descent() - paint.ascent();
    }
}
