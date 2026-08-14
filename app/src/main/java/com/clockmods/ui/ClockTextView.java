package com.clockmods.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

// Shared by every flavor, including the compat flavor which has no AppCompat dependency,
// so this deliberately extends the framework TextView instead of AppCompatTextView.
@SuppressLint("AppCompatCustomView")
public final class ClockTextView extends TextView {
    private boolean includeFontPadding = true;
    private int stableBaseline = -1;

    public ClockTextView(Context context) { this(context, null); }

    public ClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(
                attrs, new int[] {android.R.attr.includeFontPadding});
        includeFontPadding = attributes.getBoolean(0, true);
        attributes.recycle();
        getPaint().setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void setIncludeFontPadding(boolean includePadding) {
        super.setIncludeFontPadding(includePadding);
        includeFontPadding = includePadding;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        stableBaseline = -1;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Paint.FontMetricsInt metrics = getPaint().getFontMetricsInt();
        int fontTop = includeFontPadding ? metrics.top : metrics.ascent;
        int fontBottom = includeFontPadding ? metrics.bottom : metrics.descent;
        int desiredWidth = getMeasuredWidth();
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            desiredWidth = Math.round(ClockTextLayout.stableTextWidth(
                    getText().toString(), getPaint())) + getCompoundPaddingLeft()
                    + getCompoundPaddingRight();
        }
        int desiredHeight = ClockTextLayout.stableTextHeight(metrics.top, metrics.ascent,
                metrics.descent, metrics.bottom, includeFontPadding,
                getCompoundPaddingTop(), getCompoundPaddingBottom(), getSuggestedMinimumHeight());
        int measuredWidth = resolveSizeAndState(desiredWidth, widthMeasureSpec, getMeasuredState());
        int measuredHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
                ? getMeasuredHeight()
                : resolveSizeAndState(desiredHeight, heightMeasureSpec,
                        getMeasuredState() << MEASURED_HEIGHT_STATE_SHIFT);
        setMeasuredDimension(measuredWidth, measuredHeight);
        stableBaseline = ClockTextLayout.stableBaseline(getMeasuredHeight(), fontTop, fontBottom,
                getCompoundPaddingTop(), getCompoundPaddingBottom(),
                getGravity() & Gravity.VERTICAL_GRAVITY_MASK);
    }

    @Override
    public int getBaseline() {
        return stableBaseline >= 0 ? stableBaseline : super.getBaseline();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String text = getText().toString();
        Paint paint = getPaint();
        paint.setColor(getCurrentTextColor());
        float digitWidth = ClockTextLayout.widestDigitWidth(paint);
        float textWidth = ClockTextLayout.stableTextWidth(text, paint, digitWidth);
        int horizontalGravity = getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
        float cursor;
        if (horizontalGravity == Gravity.RIGHT || horizontalGravity == Gravity.END) {
            cursor = getWidth() - getCompoundPaddingRight() - textWidth;
        } else if (horizontalGravity == Gravity.CENTER_HORIZONTAL) {
            cursor = (getWidth() - textWidth) / 2f;
        } else {
            cursor = getCompoundPaddingLeft();
        }
        float baseline = getBaseline();
        for (int index = 0; index < text.length(); index++) {
            String character = text.substring(index, index + 1);
            float characterWidth = ClockTextLayout.stableCharacterWidth(
                    character, paint, digitWidth);
            canvas.drawText(character, cursor + characterWidth / 2f,
                    ClockTextLayout.alignedCharacterBaseline(character, baseline, paint), paint);
            cursor += characterWidth;
        }
    }
}
