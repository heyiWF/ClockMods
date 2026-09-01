package com.clockmods.pro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * The number inside one month cell, drawn on Canvas: the day, an optional 休/班 badge beside it
 * and an optional disc behind it for today. Public so the layouts in
 * {@link com.clockmods.pro.style} can build grid cells out of it.
 */
public final class CalendarDayNumberView extends View {
    private final Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Reused by {@link #inkCenteredBaseline()}; onDraw must not allocate. */
    private final Rect inkBounds = new Rect();
    private String day = "";
    private String badge = "";
    private boolean filled;
    /** Pinned text size in px, or 0 to derive one from the cell. See {@link #setDayTextSize}. */
    private float pinnedTextSize;

    public CalendarDayNumberView(Context context) { this(context, null); }

    public CalendarDayNumberView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dayPaint.setTextAlign(Paint.Align.CENTER);
        dayPaint.setTextSize(1f);
        badgePaint.setTextAlign(Paint.Align.LEFT);
        badgePaint.setTextSize(dayPaint.getTextSize() * 0.48f);
        fillPaint.setStyle(Paint.Style.FILL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setDay(String day, int color) {
        this.day = day;
        dayPaint.setColor(color);
        invalidate();
    }

    public void setBadge(String badge, int color) {
        this.badge = badge == null ? "" : badge;
        badgePaint.setColor(color);
        invalidate();
    }

    /** Disc drawn behind the number, used to mark today. {@code 0} draws the number alone. */
    public void setFill(int color) {
        filled = color != 0;
        fillPaint.setColor(color);
        invalidate();
    }

    public void setTypefaces(Typeface dayTypeface, Typeface badgeTypeface) {
        dayPaint.setTypeface(dayTypeface);
        badgePaint.setTypeface(badgeTypeface);
        invalidate();
    }

    /**
     * Pins the number's text size instead of deriving it from the cell; {@code 0} restores the
     * derived size. The dashboard leaves this alone — its cell shares height with a lunar label, so
     * deriving from the cell is right. The poster pins it: its cells are the whole grid and the
     * derived size would swell until the numbers touched.
     */
    public void setDayTextSize(float textSizePx) {
        pinnedTextSize = textSizePx > 0f ? textSizePx : 0f;
        applyTextSize(getWidth(), getHeight());
    }

    private void applyTextSize(int width, int height) {
        dayPaint.setTextSize(pinnedTextSize > 0f ? pinnedTextSize
                : CalendarDashboardSizing.monthDaySize(width, height,
                        getResources().getDisplayMetrics().density));
        badgePaint.setTextSize(dayPaint.getTextSize() * 0.48f);
        invalidate();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        applyTextSize(width, height);
    }

    /**
     * Baseline that puts the number's <em>ink</em> in the middle of the view, not its font metrics.
     * Digits have no descender, so centring the ascent-to-descent span leaves the empty descender
     * room below the glyph and pushes the ink upwards — which is what made the gap under a strip
     * number visibly wider than the gap above it. Measuring the drawn bounds removes the bias.
     */
    private float inkCenteredBaseline() {
        inkBounds.setEmpty();
        if (day.length() > 0) dayPaint.getTextBounds(day, 0, day.length(), inkBounds);
        if (inkBounds.height() == 0) {
            inkBounds.setEmpty();
            return (getHeight() - dayPaint.ascent() - dayPaint.descent()) / 2f;
        }
        return getHeight() / 2f - (inkBounds.top + inkBounds.bottom) / 2f;
    }

    /** Height of the drawn digits, which is what a caller aligning against them needs. */
    public float inkHeight() {
        if (day.length() == 0) return 0f;
        dayPaint.getTextBounds(day, 0, day.length(), inkBounds);
        return inkBounds.height();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float dayBaseline = inkCenteredBaseline();
        if (filled) {
            // Wide enough to read as a halo around the number, but still clear of the cell edge
            // on short rows and of the rest/work badge on wide ones. Centred on the ink rather
            // than on the view, so the disc sits on the glyph instead of drifting above it.
            float radius = Math.min(dayPaint.getTextSize() * 0.78f,
                    Math.min(getWidth(), getHeight()) / 2f);
            float inkCenterY = inkBounds.isEmpty() ? getHeight() / 2f
                    : dayBaseline + (inkBounds.top + inkBounds.bottom) / 2f;
            if (radius > 0f) canvas.drawCircle(centerX, inkCenterY, radius, fillPaint);
        }
        canvas.drawText(day, centerX, dayBaseline, dayPaint);
        if (badge.length() > 0) {
            float dayHalfWidth = dayPaint.measureText(day) / 2f;
            float gap = 2f * getResources().getDisplayMetrics().density;
            float badgeBaseline = dayBaseline + dayPaint.ascent() * 0.42f;
            canvas.drawText(badge, centerX + dayHalfWidth + gap, badgeBaseline, badgePaint);
        }
    }
}
