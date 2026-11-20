package com.clockmods.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.clockmods.platform.ApiCompat;

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
    private final float baseTextSizePx;
    private final float minTextSizePx;
    private final Paint measurePaint = new Paint();
    private float fittedTextSizePx;

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
        this.baseTextSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f,
                context.getResources().getDisplayMetrics());
        this.minTextSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f,
                context.getResources().getDisplayMetrics());
        this.fittedTextSizePx = baseTextSizePx;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(3), dp(3), dp(3), dp(3));
        setViewBackground(this, createBackground(unselectedColor, dp(10), 0, Color.TRANSPARENT));

        for (int index = 0; index < labels.length; index++) {
            final int segmentIndex = index;
            TextView segment = new TextView(context);
            segment.setText(labels[index]);
            segment.setTextSize(14);
            segment.setGravity(Gravity.CENTER);
            segment.setSingleLine(true);
            segment.setClickable(true);
            // Breathing room between the label and the pill edge; the fitter below accounts
            // for it when it decides how far the label has to shrink.
            segment.setPadding(dp(4), 0, dp(4), 0);
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

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        fitSegmentText();
    }

    /**
     * Shrinks every label by the same amount so all segments still fit their equal-width
     * cells. With three long options ("Follow system / Force portrait / Force landscape")
     * the labels used to run into each other and the last one was cut off at the card edge,
     * because a single-line TextView happily paints outside its own bounds.
     */
    private void fitSegmentText() {
        if (segments.length == 0 || segments[0].getWidth() <= 0) return;
        float target = baseTextSizePx;
        for (int index = 0; index < segments.length; index++) {
            TextView segment = segments[index];
            int available = segment.getWidth() - segment.getCompoundPaddingLeft()
                    - segment.getCompoundPaddingRight();
            CharSequence text = segment.getText();
            if (available <= 0 || text == null || text.length() == 0) return;
            measurePaint.setTypeface(segment.getTypeface());
            measurePaint.setTextSize(baseTextSizePx);
            float width = measurePaint.measureText(text.toString());
            if (width > available) {
                target = Math.min(target, baseTextSizePx * available / width);
            }
        }
        target = Math.max(minTextSizePx, target);
        if (Math.abs(target - fittedTextSizePx) < 0.25f) return;
        fittedTextSizePx = target;
        for (int index = 0; index < segments.length; index++) {
            segments[index].setTextSize(TypedValue.COMPLEX_UNIT_PX, target);
        }
    }

    private void updateAppearance(boolean animate) {
        for (int index = 0; index < segments.length; index++) {
            TextView segment = segments[index];
            boolean selected = index == selectedIndex;
            segment.setSelected(selected);
            segment.setTextColor(selected ? selectedTextColor : unselectedTextColor);
            setViewBackground(segment, selected
                    ? createBackground(selectedColor, dp(8), 0, Color.TRANSPARENT)
                    : createBackground(Color.TRANSPARENT, dp(8), 0, Color.TRANSPARENT));
            if (animate && selected) {
                ApiCompat.setAlphaCompat(segment, 0.72f);
                // ViewPropertyAnimator needs API 12; the helper degrades to a static change.
                ApiCompat.animateAlpha(segment, 1f, 140L);
            } else {
                ApiCompat.setAlphaCompat(segment, 1f);
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

    private static void setViewBackground(View view, Drawable drawable) {
        ApiCompat.setBackgroundCompat(view, drawable);
    }
}
