package com.clockmods.ui;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

final class ClockTextLayout {
    private static final String DIGITS = "0123456789";
    private static final String COLON = ":";
    // The widest-digit width is measured 10 times per call and the clock asks for it
    // several times per frame, always for the same paint/size/typeface. A tiny cache
    // (one slot per paint in use) keeps that off the draw path.
    private static final int DIGIT_WIDTH_SLOTS = 4;
    private static final Paint[] digitWidthPaints = new Paint[DIGIT_WIDTH_SLOTS];
    private static final Typeface[] digitWidthTypefaces = new Typeface[DIGIT_WIDTH_SLOTS];
    private static final float[] digitWidthSizes = new float[DIGIT_WIDTH_SLOTS];
    private static final float[] digitWidthValues = new float[DIGIT_WIDTH_SLOTS];
    // The colon is the only non-digit glyph on the time line, and its advance plus its baseline
    // offset are asked for on every frame and every animation step. They live in the same slots.
    private static final float[] colonWidthValues = new float[DIGIT_WIDTH_SLOTS];
    private static final float[] colonOffsetValues = new float[DIGIT_WIDTH_SLOTS];
    private static final boolean[] colonOffsetsSet = new boolean[DIGIT_WIDTH_SLOTS];
    private static int digitWidthNext;
    // getTextBounds is called from onDraw on the UI thread; reuse the scratch rects
    // instead of allocating two Rect objects for every colon and accessory line.
    private static final Rect SCRATCH_A = new Rect();
    private static final Rect SCRATCH_B = new Rect();

    private ClockTextLayout() { }

    static float stableTextWidth(String text, Paint paint) {
        return stableTextWidth(text, paint, widestDigitWidth(paint));
    }

    static float stableTextWidth(String text, Paint paint, float digitWidth) {
        float width = 0f;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            width += (c >= '0' && c <= '9')
                    ? digitWidth : paint.measureText(text, index, index + 1);
        }
        return width;
    }

    /** Advance of ':' at the paint's current size and typeface, measured once per slot. */
    static float colonWidth(Paint paint) {
        int slot = findSlot(paint);
        if (slot < 0) {
            // Fills the slot with both values; a slot carrying a zero digit width would make
            // every digit collapse to nothing on the next lookup.
            widestDigitWidth(paint);
            slot = findSlot(paint);
            if (slot < 0) {
                return paint.measureText(COLON, 0, 1);
            }
        }
        return colonWidthValues[slot];
    }

    private static int findSlot(Paint paint) {
        float textSize = paint.getTextSize();
        Typeface typeface = paint.getTypeface();
        for (int slot = 0; slot < DIGIT_WIDTH_SLOTS; slot++) {
            if (digitWidthPaints[slot] == paint && digitWidthSizes[slot] == textSize
                    && digitWidthTypefaces[slot] == typeface) {
                return slot;
            }
        }
        return -1;
    }

    private static void storeSlot(Paint paint, float digitWidth, float colonWidth) {
        int slot = digitWidthNext;
        digitWidthNext = (digitWidthNext + 1) % DIGIT_WIDTH_SLOTS;
        digitWidthPaints[slot] = paint;
        digitWidthTypefaces[slot] = paint.getTypeface();
        digitWidthSizes[slot] = paint.getTextSize();
        digitWidthValues[slot] = digitWidth;
        colonWidthValues[slot] = colonWidth;
        colonOffsetsSet[slot] = false;
    }

    static float widestDigitWidth(Paint paint) {
        int slot = findSlot(paint);
        if (slot >= 0) {
            return digitWidthValues[slot];
        }
        float widestDigit = 0f;
        for (int index = 0; index < DIGITS.length(); index++) {
            widestDigit = Math.max(widestDigit, paint.measureText(DIGITS, index, index + 1));
        }
        storeSlot(paint, widestDigit, paint.measureText(COLON, 0, 1));
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

    static float alignedCharacterBaseline(char character, float baseline, Paint paint) {
        if (character != ':') return baseline;
        return baseline + colonBaselineOffset(paint);
    }

    /**
     * Vertical offset that centres ':' on the digits' optical centre. Two {@code getTextBounds}
     * calls per colon per frame were measurable, so the offset is cached per paint/slot; the
     * values only change with the typeface or the text size.
     */
    static float colonBaselineOffset(Paint paint) {
        int slot = findSlot(paint);
        if (slot < 0) {
            widestDigitWidth(paint);
            slot = findSlot(paint);
            if (slot < 0) {
                // Cache thrash (four paints sharing one small table); measure directly.
                return measureColonOffset(paint);
            }
        }
        if (colonOffsetsSet[slot]) {
            return colonOffsetValues[slot];
        }
        float offset = measureColonOffset(paint);
        colonOffsetValues[slot] = offset;
        colonOffsetsSet[slot] = true;
        return offset;
    }

    private static float measureColonOffset(Paint paint) {
        paint.getTextBounds(DIGITS, 0, 1, SCRATCH_A);
        paint.getTextBounds(COLON, 0, 1, SCRATCH_B);
        float digitCenter = (SCRATCH_A.top + SCRATCH_A.bottom) / 2f;
        float colonCenter = (SCRATCH_B.top + SCRATCH_B.bottom) / 2f;
        return digitCenter - colonCenter;
    }

    static float bottomAlignedBaseline(float mainBaseline, Paint mainPaint, Paint accessoryPaint) {
        mainPaint.getTextBounds(DIGITS, 0, 1, SCRATCH_A);
        accessoryPaint.getTextBounds(DIGITS, 0, 1, SCRATCH_B);
        return mainBaseline + SCRATCH_A.bottom - SCRATCH_B.bottom;
    }
}
