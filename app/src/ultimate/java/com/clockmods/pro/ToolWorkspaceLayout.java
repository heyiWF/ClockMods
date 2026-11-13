package com.clockmods.pro;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/** Two panes follow the available window, including density changes and split screen. */
public final class ToolWorkspaceLayout extends LinearLayout {
    private boolean expandedDetails;

    public ToolWorkspaceLayout(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setExpandedDetails(boolean expanded) {
        if (expandedDetails != expanded) { expandedDetails = expanded; requestLayout(); }
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        float density = getResources().getDisplayMetrics().density;
        boolean wide = width > height && width / density >= 520;
        // Keep a deliberate reading width on spacious tablets and high-resolution windows.
        int inset = Math.max(Math.round(24 * density), (width - Math.round((wide ? 1100 : 640) * density)) / 2);
        if (getPaddingLeft() != inset) setPadding(inset, getPaddingTop(), inset, getPaddingBottom());
        setOrientation(wide ? HORIZONTAL : VERTICAL);
        if (getChildCount() == 2) {
            int gap = Math.round(24 * density);
            View face = getChildAt(0), details = getChildAt(1);
            LayoutParams first = (LayoutParams) face.getLayoutParams();
            LayoutParams second = (LayoutParams) details.getLayoutParams();
            first.width = wide ? 0 : LayoutParams.MATCH_PARENT;
            first.height = wide ? LayoutParams.MATCH_PARENT : 0;
            first.weight = 1;
            second.width = wide ? 0 : LayoutParams.MATCH_PARENT;
            second.height = wide ? LayoutParams.MATCH_PARENT : expandedDetails ? 0 : LayoutParams.WRAP_CONTENT;
            second.weight = wide || expandedDetails ? 1 : 0;
            second.setMarginStart(wide ? gap : 0);
            second.topMargin = wide ? 0 : gap;
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
