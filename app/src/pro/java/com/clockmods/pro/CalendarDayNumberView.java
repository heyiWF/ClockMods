package com.clockmods.pro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.clockmods.R;

final class CalendarDayNumberView extends View {
    private final Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float maximumDayTextSize;
    private String day = "";
    private String badge = "";

    CalendarDayNumberView(Context context) { this(context, null); }

    CalendarDayNumberView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dayPaint.setTextAlign(Paint.Align.CENTER);
        maximumDayTextSize = getResources().getDimension(R.dimen.calendar_dashboard_day_size);
        dayPaint.setTextSize(maximumDayTextSize);
        badgePaint.setTextAlign(Paint.Align.LEFT);
        badgePaint.setTextSize(dayPaint.getTextSize() * 0.48f);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setDay(String day, int color) {
        this.day = day;
        dayPaint.setColor(color);
        invalidate();
    }

    void setBadge(String badge, int color) {
        this.badge = badge == null ? "" : badge;
        badgePaint.setColor(color);
        invalidate();
    }

    void setTypefaces(Typeface dayTypeface, Typeface badgeTypeface) {
        dayPaint.setTypeface(dayTypeface);
        badgePaint.setTypeface(badgeTypeface);
        invalidate();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float availableSize = Math.min(width * 0.46f, height * 0.72f);
        dayPaint.setTextSize(Math.min(maximumDayTextSize, availableSize));
        badgePaint.setTextSize(dayPaint.getTextSize() * 0.48f);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float dayBaseline = (getHeight() - dayPaint.ascent() - dayPaint.descent()) / 2f;
        canvas.drawText(day, centerX, dayBaseline, dayPaint);
        if (badge.length() > 0) {
            float dayHalfWidth = dayPaint.measureText(day) / 2f;
            float gap = 2f * getResources().getDisplayMetrics().density;
            float badgeBaseline = dayBaseline + dayPaint.ascent() * 0.42f;
            canvas.drawText(badge, centerX + dayHalfWidth + gap, badgeBaseline, badgePaint);
        }
    }
}