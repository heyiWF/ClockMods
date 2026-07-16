package com.clockmods.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class SegmentedSelector extends LinearLayout {
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int index);
    }

    private final TextView[] segments;
    private final int selectedColor;
    private final int unselectedColor;
    private final int selectedTextColor;
    private final int unselectedTextColor;
    private OnSelectionChangedListener listener;
    private int selectedIndex;

    public SegmentedSelector(Context context, CharSequence[] labels, int selectedColor,
            int unselectedColor, int selectedTextColor, int unselectedTextColor) {
        super(context);
        if (labels == null || labels.length < 2) {
            throw new IllegalArgumentException("SegmentedSelector requires at least two labels");
        }
        this.selectedColor = selectedColor;
        this.unselectedColor = unselectedColor;
        this.selectedTextColor = selectedTextColor;
        this.unselectedTextColor = unselectedTextColor;
        this.segments = new TextView[labels.length];
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(3), dp(3), dp(3), dp(3));
        setBackground(createBackground(unselectedColor, dp(10), 0, Color.TRANSPARENT));

        for (int index = 0; index < labels.length; index++) {
            final int segmentIndex = index;
            TextView segment = new TextView(context);
            segment.setText(labels[index]);
            segment.setTextSize(14);
            segment.setGravity(Gravity.CENTER);
            segment.setSingleLine(true);
            segment.setClickable(true);
            segment.setFocusable(true);
            segment.setOnClickListener(view -> setSelectedIndex(segmentIndex));
            segments[index] = segment;
            addView(segment, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        }
        updateAppearance(false);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= segments.length || selectedIndex == index) {
            return;
        }
        selectedIndex = index;
        updateAppearance(true);
        if (listener != null) {
            listener.onSelectionChanged(index);
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    private void updateAppearance(boolean animate) {
        for (int index = 0; index < segments.length; index++) {
            TextView segment = segments[index];
            boolean selected = index == selectedIndex;
            segment.setSelected(selected);
            segment.setTextColor(selected ? selectedTextColor : unselectedTextColor);
            segment.setBackground(selected
                    ? createBackground(selectedColor, dp(8), 0, Color.TRANSPARENT)
                    : createBackground(Color.TRANSPARENT, dp(8), 0, Color.TRANSPARENT));
            if (animate && selected) {
                segment.setAlpha(0.72f);
                segment.animate().alpha(1f).setDuration(140L).start();
            } else {
                segment.setAlpha(1f);
            }
        }
    }

    private GradientDrawable createBackground(int fillColor, float radius, int strokeWidth,
            int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(radius);
        if (strokeWidth > 0) {
            background.setStroke(strokeWidth, strokeColor);
        }
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}