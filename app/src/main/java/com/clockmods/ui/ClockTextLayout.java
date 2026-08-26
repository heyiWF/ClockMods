package com.clockmods.ui;

import android.graphics.Paint;
import android.graphics.Rect;

final class ClockTextLayout {
    private ClockTextLayout() { }

    static float stableTextWidth(String text, Paint paint) {
        return stableTextWidth(text, paint, widestDigitWidth(paint));
    }

    static float stableTextWidth(String text, Paint paint, float digitWidth) {
        float width = 0f;
        for (int index = 0; index < text.length(); index++) {
            width += stableCharacterWidth(text.substring(index, index + 1), paint, digitWidth);
        }
        return width;
    }

    static float stableCharacterWidth(String character, Paint paint, float digitWidth) {
        if (character.length() != 1 || character.charAt(0) < '0' || character.charAt(0) > '9') {
            return paint.measureText(character);
        }
        return digitWidth;
    }

    static float widestDigitWidth(Paint paint) {
        float widestDigit = 0f;
        for (char digit = '0'; digit <= '9'; digit++) {
            widestDigit = Math.max(widestDigit, paint.measureText(String.valueOf(digit)));
        }
        return widestDigit;
    }

    static int stableTextHeight(int top, int ascent, int descent, int bottom,
            boolean includeFontPadding, int paddingTop, int paddingBottom, int minimumHeight) {
        int fontTop = includeFontPadding ? top : ascent;
        int fontBottom = includeFontPadding ? bottom : descent;
        return Math.max(minimumHeight, fontBottom - fontTop + paddingTop + paddingBottom);
    }

    static int stableBaseline(int measuredHeight, int fontTop, int fontBottom,
            int paddingTop, int paddingBottom, int verticalGravity) {
        int textHeight = fontBottom - fontTop;
        int availableHeight = Math.max(0, measuredHeight - paddingTop - paddingBottom);
        int contentTop = paddingTop;
        if (verticalGravity == android.view.Gravity.BOTTOM) {
            contentTop += Math.max(0, availableHeight - textHeight);
        } else if (verticalGravity == android.view.Gravity.CENTER_VERTICAL) {
            contentTop += Math.max(0, availableHeight - textHeight) / 2;
        }
        return contentTop - fontTop;
    }

    static float alignedCharacterBaseline(String character, float baseline, Paint paint) {
        if (!":".equals(character)) return baseline;
        return baseline + ClockTimeText.colonBaselineOffset(paint);
    }

    static float bottomAlignedBaseline(float mainBaseline, Paint mainPaint, Paint accessoryPaint) {
        Rect mainDigitBounds = new Rect();
        Rect accessoryDigitBounds = new Rect();
        mainPaint.getTextBounds("0", 0, 1, mainDigitBounds);
        accessoryPaint.getTextBounds("0", 0, 1, accessoryDigitBounds);
        return mainBaseline + mainDigitBounds.bottom - accessoryDigitBounds.bottom;
    }
}
