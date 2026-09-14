package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * One 宜/忌 line: a bold leading glyph followed by the day's items, always on a single line.
 * The glyph is the line's identity, so it is drawn bold and pinned at the left edge — when the
 * items are wider than the view, only they scroll horizontally past it; the line never wraps.
 *
 * <p>The scroll is an endless belt: the tail is followed by a blank and then the head again, so it
 * never snaps back to the start. That is the difference from {@link CalendarFooterCarouselView},
 * which shows one line at a time and treats reaching the tail as the cue to move on — a line with
 * no end could never give it that cue. Here both lines are on screen permanently and there is
 * nothing to move on to.
 */
public final class AlmanacLineView extends View {
    private static final long SCROLL_PAUSE_MS = 1000L;
    private static final long FRAME_DELAY_MS = 16L;
    private static final float SCROLL_DP_PER_SECOND = 40f;
    private static final float HORIZONTAL_PADDING_DP = 2f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;

    private String prefix = "";
    private String content = "";
    private int lineColor = Color.WHITE;
    private float preferredTextSize;
    private Typeface typeface = Typeface.DEFAULT;
    /** Derived once per typeface; fonts with no bold face fall back to the fake-bold render. */
    private Typeface boldTypeface = Typeface.DEFAULT_BOLD;
    private boolean active;
    private long shownAt;

    public AlmanacLineView(Context context) { this(context, null); }

    public AlmanacLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        preferredTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                13f, getResources().getDisplayMetrics());
        paint.setTextSize(preferredTextSize);
    }

    /**
     * Sets the line from its assembled form. {@code leadingHint} is the localized 宜/忌 prefix; a
     * line that starts with it gets it split out as the pinned bold glyph, anything else renders
     * as one plain run.
     */
    public void setLine(String line, String leadingHint) {
        line = line == null ? "" : line;
        String hint = leadingHint == null ? "" : leadingHint;
        if (hint.length() > 0 && line.startsWith(hint) && line.length() > hint.length()) {
            prefix = hint;
            content = line.substring(hint.length());
        } else {
            prefix = "";
            content = line;
        }
        setContentDescription(line);
        shownAt = 0L;
        invalidate();
    }

    public void setColor(int color) { lineColor = color; invalidate(); }

    public void setTextSizePx(float size) {
        if (size == preferredTextSize) return;
        preferredTextSize = size;
        paint.setTextSize(size);
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface value) {
        Typeface resolved = value == null ? Typeface.DEFAULT : value;
        if (resolved.equals(typeface)) return;
        typeface = resolved;
        boldTypeface = Typeface.create(resolved, Typeface.BOLD);
        requestLayout();
        invalidate();
    }

    public void setActive(boolean value) {
        if (value == active) return;
        active = value;
        if (active) {
            shownAt = 0L;
            invalidate();
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        paint.setTextSize(preferredTextSize);
        float lineHeight = paint.descent() - paint.ascent();
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                Math.max(Math.round(lineHeight), getSuggestedMinimumHeight()));
    }

    @Override protected void onDetachedFromWindow() {
        active = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (prefix.length() == 0 && content.length() == 0) return;
        long now = SystemClock.uptimeMillis();
        if (shownAt == 0L) shownAt = now;
        paint.setColor(lineColor);
        paint.setTextSize(preferredTextSize);
        paint.setTypeface(typeface);
        float centerY = getHeight() / 2f;
        float baseline = centerY - (paint.descent() + paint.ascent()) / 2f;
        float padding = HORIZONTAL_PADDING_DP * density;
        float prefixWidth = prefixWidth();
        paint.setTypeface(typeface);
        float contentWidth = paint.measureText(content);
        float room = Math.max(1f, getWidth() - padding * 2f - prefixWidth);
        paint.setTextAlign(Paint.Align.LEFT);
        if (contentWidth <= room) {
            // A line that fits never redraws: nothing moves, so no frames are scheduled.
            drawRuns(canvas, baseline, padding);
            return;
        }
        // One lap is the items plus the blank that separates the tail from the head. Travelling
        // exactly this far lands the second copy where the first began, so the wrap back to zero
        // is pixel-identical and the line reads as one continuous belt rather than snapping home.
        float loopWidth = contentWidth + scrollGap();
        float offset = 0f;
        if (active) {
            long scrollMs = (long) Math.ceil(loopWidth / (SCROLL_DP_PER_SECOND * density) * 1000f);
            if (scrollMs <= 0L) scrollMs = 1L;
            long cycle = SCROLL_PAUSE_MS + scrollMs;
            long elapsed = (now - shownAt) % cycle;
            // The pause is at the head of each lap, which is the only moment the line is still.
            offset = elapsed < SCROLL_PAUSE_MS ? 0f
                    : loopWidth * (elapsed - SCROLL_PAUSE_MS) / scrollMs;
            postInvalidateDelayed(FRAME_DELAY_MS);
        }
        // The glyph stays put while the items slide under the clip to its right.
        canvas.save();
        canvas.clipRect(padding + prefixWidth, 0f, getWidth() - padding, (float) getHeight());
        paint.setTypeface(typeface);
        float x = padding + prefixWidth - offset;
        canvas.drawText(content, x, baseline, paint);
        canvas.drawText(content, x + loopWidth, baseline, paint);
        canvas.restore();
        paint.setTypeface(boldTypeface);
        canvas.drawText(prefix, padding, baseline, paint);
    }

    /** Blank between the tail of one pass and the head of the next, so they never read as one run. */
    private float scrollGap() {
        return Math.max(density * 24f, preferredTextSize * 1.5f);
    }

    private void drawRuns(Canvas canvas, float baseline, float padding) {
        // Measure first: prefixWidth() leaves the bold face on the paint, and Java evaluates
        // arguments before the call — so measuring inline after setting the regular face drew the
        // whole line bold, which is exactly what hid the 宜/忌 emphasis on lines that fit.
        float leading = prefixWidth();
        if (prefix.length() > 0) {
            paint.setTypeface(boldTypeface);
            canvas.drawText(prefix, padding, baseline, paint);
        }
        paint.setTypeface(typeface);
        canvas.drawText(content, padding + leading, baseline, paint);
    }

    private float prefixWidth() {
        if (prefix.length() == 0) return 0f;
        paint.setTypeface(boldTypeface);
        return paint.measureText(prefix);
    }
}
