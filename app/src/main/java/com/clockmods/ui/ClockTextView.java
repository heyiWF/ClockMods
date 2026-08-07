package com.clockmods.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

// Shared by every flavor, including the compat flavor which has no AppCompat dependency,
// so this deliberately extends the framework TextView instead of AppCompatTextView.
@SuppressLint("AppCompatCustomView")
public final class ClockTextView extends TextView {
    public ClockTextView(Context context) { this(context, null); }

    public ClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getPaint().setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) return;
        int desiredWidth = Math.round(ClockTextLayout.stableTextWidth(
                getText().toString(), getPaint())) + getCompoundPaddingLeft()
                + getCompoundPaddingRight();
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), getMeasuredHeight());
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