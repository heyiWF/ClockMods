package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.widget.ScrollView;

/**
 * A scroll view whose indicator is hand-drawn.
 *
 * <p>The framework scrollbar is a near-white bar with a grey track. On the dark sheet it reads as
 * a bright stripe in the right-hand gutter and is the only element that still looks like a stock
 * widget. Android 1.0 cannot tint a scrollbar, so this class hides it and draws a dim 3dp thumb at
 * a fixed inset instead. The thumb is only drawn while there is something to scroll.</p>
 */
public class PanelScrollView extends ScrollView {
    private final Paint paint = new Paint();
    private final RectF bounds = new RectF();
    private final int thumbWidth;
    private final int inset;
    private final int minThumbHeight;

    public PanelScrollView(Context context) {
        super(context);
        setVerticalScrollBarEnabled(false);
        paint.setAntiAlias(true);
        paint.setColor(PanelTheme.SCROLL_THUMB);
        thumbWidth = dp(context, 3);
        inset = dp(context, 3);
        minThumbHeight = dp(context, 24);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (getChildCount() == 0) {
            return;
        }
        int contentHeight = getChildAt(0).getHeight();
        int viewHeight = getHeight();
        if (contentHeight <= viewHeight) {
            return;
        }
        float thumbHeight = Math.max(minThumbHeight,
                (float) viewHeight * viewHeight / contentHeight);
        float travel = viewHeight - thumbHeight;
        int scrollable = contentHeight - viewHeight;
        float top = inset;
        if (scrollable > 0) {
            top += travel * Math.min(1f, (float) getScrollY() / scrollable);
        }
        bounds.set(getWidth() - inset - thumbWidth, top, getWidth() - inset, top + thumbHeight);
        canvas.drawRoundRect(bounds, thumbWidth / 2f, thumbWidth / 2f, paint);
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }
}
