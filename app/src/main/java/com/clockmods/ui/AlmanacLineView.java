package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
    private static final long FRAME_DELAY_MS = 16L;
    private static final float HORIZONTAL_PADDING_DP = 2f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect badgeBounds = new Rect();
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
            content = line.substring(hint.length()).trim();
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
                Math.max(Math.round(Math.max(lineHeight, preferredTextSize * 1.55f)), getSuggestedMinimumHeight()));
    }

    @Override protected void onDetachedFromWindow() {
        active = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (prefix.isEmpty() && content.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (shownAt == 0L) shownAt = now;
        paint.setColor(lineColor);
        paint.setTextSize(preferredTextSize);
        paint.setTypeface(typeface);
        paint.setTextAlign(Paint.Align.LEFT);
        float centerY = getHeight() / 2f;
        float baseline = centerY - (paint.descent() + paint.ascent()) / 2f;
        float padding = HORIZONTAL_PADDING_DP * density;
        float diameter = Math.min(getHeight(), preferredTextSize * 1.55f);
        float leading = prefix.isEmpty() ? 0f : diameter + density * 4f;
        float room = Math.max(1f, getWidth() - padding * 2f - leading);
        float contentWidth = paint.measureText(content);
        float distance = CalendarMarqueeTiming.loopDistance(contentWidth, preferredTextSize, density);
        boolean scroll = active && contentWidth > room;
        float offset = scroll ? CalendarMarqueeTiming.loopOffset(now - shownAt, distance, density) : 0f;
        int save = canvas.save();
        canvas.clipRect(padding + leading, 0f, getWidth() - padding, getHeight());
        canvas.drawText(content, padding + leading - offset, baseline, paint);
        if (scroll) canvas.drawText(content, padding + leading - offset + distance, baseline, paint);
        canvas.restoreToCount(save);
        if (!prefix.isEmpty()) {
            glyphPaint.setTypeface(boldTypeface);
            glyphPaint.setTextSize(preferredTextSize * .85f);
            AlmanacBadge.draw(canvas, prefix, padding + diameter / 2f, centerY,
                    diameter, lineColor, glyphPaint, badgePaint, badgeBounds);
        }
        if (scroll) postInvalidateDelayed(FRAME_DELAY_MS);
    }
}
