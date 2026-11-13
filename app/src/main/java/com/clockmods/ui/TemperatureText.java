package com.clockmods.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ReplacementSpan;

/** Half-height temperature units, aligned by glyph ink rather than a font's line box. */
public final class TemperatureText {
    private TemperatureText() { }
    public static float unitScale(Paint paint, String unit) { return unitScale(paint, unit, "0"); }
    public static float unitScale(Paint paint, String unit, String number) {
        Rect digit = new Rect(), symbol = new Rect();
        paint.getTextBounds(number, 0, number.length(), digit);
        paint.getTextBounds(unit, 0, unit.length(), symbol);
        return digit.height() > 0 && symbol.height() > 0 ? digit.height() * .5f / symbol.height() : .5f;
    }
    public static float topOffset(Paint digits, Paint unit, String symbol) { return topOffset(digits, unit, symbol, "0"); }
    public static float topOffset(Paint digits, Paint unit, String symbol, String value) {
        Rect number = new Rect(), suffix = new Rect();
        digits.getTextBounds(value, 0, value.length(), number);
        unit.getTextBounds(symbol, 0, symbol.length(), suffix);
        return number.top - suffix.top;
    }
    public static CharSequence align(CharSequence text) {
        if (text == null || text.length() == 0) return text;
        String value = text.toString();
        int start = value.endsWith("℃") || value.endsWith("℉") ? value.length() - 1
                : value.endsWith("°C") || value.endsWith("°F") ? value.length() - 2 : -1;
        if (start < 0) return text;
        SpannableString result = new SpannableString(text);
        result.setSpan(new UnitSpan(), start, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }
    private static final class UnitSpan extends ReplacementSpan {
        private Paint small(Paint source, String symbol, String number) {
            Paint result = new Paint(source);
            result.setTextSize(source.getTextSize() * unitScale(source, symbol, number));
            return result;
        }
        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt metrics) {
            if (metrics != null) paint.getFontMetricsInt(metrics);
            String unit = text.subSequence(start, end).toString();
            return (int) Math.ceil(small(paint, unit, text.subSequence(0, start).toString()).measureText(unit));
        }
        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                float x, int top, int y, int bottom, Paint paint) {
            String unit = text.subSequence(start, end).toString();
            String number = text.subSequence(0, start).toString();
            Paint scaled = small(paint, unit, number);
            canvas.drawText(unit, x, y + topOffset(paint, scaled, unit, number), scaled);
        }
    }
}
