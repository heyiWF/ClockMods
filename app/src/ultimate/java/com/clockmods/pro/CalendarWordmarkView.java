package com.clockmods.pro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * The poster style's masthead: a month word set as large as its own glyphs allow, with the year
 * beneath it. Public so {@link com.clockmods.pro.style.PosterCalendarLayout} can build it.
 *
 * <p>Canvas rather than two TextViews, for two reasons. {@code ProFontApplier} caches the weight it
 * first sees on a TextView, so a masthead built from TextViews would freeze at its XML weight and
 * stop following the bold-text setting. And a TextView's auto-sizing fits a fixed box, which would
 * set 八月 and SEPTEMBER at wildly different optical weights — here both are fitted to the same
 * width budget instead, which is the whole point of the composition.</p>
 */
public final class CalendarWordmarkView extends View {
    /** Share of the usable width the month word is fitted to. */
    private static final float MONTH_WIDTH_SHARE = 0.94f;
    /** Same, for the year. Deliberately much narrower so the two lines read as a hierarchy. */
    private static final float YEAR_WIDTH_SHARE = 0.30f;

    private final Paint monthPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint yearPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint lunarPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint festivalPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private String month = "";
    private String year = "";
    private String lunar = "";
    private String festivals = "";

    public CalendarWordmarkView(Context context) { this(context, null); }

    public CalendarWordmarkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        monthPaint.setTextAlign(Paint.Align.LEFT);
        yearPaint.setTextAlign(Paint.Align.LEFT);
        lunarPaint.setTextAlign(Paint.Align.LEFT);
        festivalPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setText(String month, String year) {
        this.month = month == null ? "" : month;
        this.year = year == null ? "" : year;
        updateDescription();
        invalidate();
    }

    public void setSelectedDetails(String lunar, String festivals) {
        this.lunar = lunar == null ? "" : lunar;
        this.festivals = festivals == null ? "" : festivals;
        updateDescription();
        invalidate();
    }

    private void updateDescription() {
        setContentDescription(lunar + " " + festivals + " " + month + " " + year);
    }

    public void setColors(int monthColor, int yearColor) {
        monthPaint.setColor(monthColor);
        yearPaint.setColor(yearColor);
        lunarPaint.setColor(yearColor);
        festivalPaint.setColor(monthColor);
        invalidate();
    }

    public void setTypefaces(Typeface monthTypeface, Typeface yearTypeface) {
        monthPaint.setTypeface(monthTypeface);
        yearPaint.setTypeface(yearTypeface);
        lunarPaint.setTypeface(yearTypeface);
        festivalPaint.setTypeface(yearTypeface);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float boxWidth = usableWidth();
        float boxHeight = usableHeight();
        if (boxWidth <= 0f || boxHeight <= 0f) return;
        float density = getResources().getDisplayMetrics().density;
        float lunarSize = Math.min(16f * density, boxHeight * .09f);
        float festivalSize = Math.min(13f * density, boxHeight * .075f);
        lunarPaint.setTextSize(shrinkToFit(lunarPaint, lunar, lunarSize, boxWidth));
        festivalPaint.setTextSize(shrinkToFit(festivalPaint, festivals, festivalSize, boxWidth));
        float detailHeight = lunar.length() == 0 ? 0f : lunarPaint.descent() - lunarPaint.ascent();
        float festivalHeight = festivals.length() == 0 ? 0f
                : festivalPaint.descent() - festivalPaint.ascent();
        float detailGap = detailHeight == 0f ? 0f : 3f * density;
        float headingHeight = detailHeight + (festivalHeight == 0f ? 0f : detailGap + festivalHeight);
        if (lunar.length() > 0) {
            canvas.drawText(lunar, getPaddingLeft(), getPaddingTop() - lunarPaint.ascent(), lunarPaint);
        }
        if (festivals.length() > 0) {
            canvas.drawText(festivals, getPaddingLeft(),
                    getPaddingTop() + detailHeight + detailGap - festivalPaint.ascent(), festivalPaint);
        }
        float remainingHeight = Math.max(1f, boxHeight - headingHeight);
        float fittedMonthBound = CalendarDashboardSizing.posterWordmarkSize(
                boxWidth, remainingHeight, density);
        monthPaint.setTextSize(shrinkToFit(monthPaint, month, fittedMonthBound,
                boxWidth * MONTH_WIDTH_SHARE));
        float fittedYearBound = CalendarDashboardSizing.posterYearSize(
                monthPaint.getTextSize(), remainingHeight, density);
        yearPaint.setTextSize(shrinkToFit(yearPaint, year, fittedYearBound,
                boxWidth * YEAR_WIDTH_SHARE));
        // Ascent/descent rather than the full font height: the gap wants to track the visible
        // glyphs, not the line box, or the year drifts away from the word above it.
        float monthHeight = monthPaint.descent() - monthPaint.ascent();
        float yearHeight = year.length() == 0 ? 0f : yearPaint.descent() - yearPaint.ascent();
        float gap = year.length() == 0 ? 0f : monthPaint.getTextSize() * 0.12f;
        float top = getPaddingTop() + headingHeight
                + (remainingHeight - monthHeight - gap - yearHeight) / 2f;
        float left = getPaddingLeft();
        canvas.drawText(month, left, top - monthPaint.ascent(), monthPaint);
        if (year.length() > 0) {
            canvas.drawText(year, left,
                    top + monthHeight + gap - yearPaint.ascent(), yearPaint);
        }
    }

    /** Shrinks {@code bound} until {@code text} fits {@code budget}; never grows past the bound. */
    private static float shrinkToFit(Paint paint, String text, float bound, float budget) {
        if (text.length() == 0 || budget <= 0f) return bound;
        paint.setTextSize(bound);
        float measured = paint.measureText(text);
        if (measured <= budget) return bound;
        return Math.max(1f, bound * budget / measured);
    }

    private float usableWidth() { return getWidth() - getPaddingLeft() - getPaddingRight(); }

    private float usableHeight() { return getHeight() - getPaddingTop() - getPaddingBottom(); }
}
