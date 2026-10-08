package com.clockmods.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/** Keeps a usable calendar canvas in short windows while exposing every row by scrolling. */
public final class ResponsiveCalendarHost extends ScrollView {
    public ResponsiveCalendarHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFillViewport(true);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (getChildCount() == 0) return;
        View content = getChildAt(0);
        int width = Math.max(0, getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
        int viewport = Math.max(0, getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
        float density = getResources().getDisplayMetrics().density;
        int minimum = Math.round((width > viewport ? 320f : 480f) * density);
        content.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.max(viewport, minimum), MeasureSpec.EXACTLY));
    }
}
