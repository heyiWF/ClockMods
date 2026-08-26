package com.clockmods.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

/**
 * Vertically centres the ':' separators of a time string on the digits beside them.
 *
 * Almost every typeface centres the colon on the x-height while digits are drawn to
 * cap/figure height, so a literal ':' sits visibly below the middle of "12:34". The
 * correction is derived from the glyph bounds of the caller's own Paint rather than
 * from a hand-tuned constant, so it follows whatever typeface, weight and size is in
 * effect. Because the colon only moves to the digits' optical centre it can never rise
 * above their cap height, so the shift cannot clip against a tight container.
 */
public final class ClockTimeText {
    private static final char COLON = ':';

    private ClockTimeText() { }

    /**
     * Baseline delta in pixels that centres ':' on the digits of {@code paint}; negative
     * values raise the colon. Returns 0 when either glyph has no ink to measure.
     */
    public static float colonBaselineOffset(Paint paint) {
        Rect digitBounds = new Rect();
        Rect colonBounds = new Rect();
        paint.getTextBounds("0", 0, 1, digitBounds);
        paint.getTextBounds(String.valueOf(COLON), 0, 1, colonBounds);
        return colonBaselineOffset(digitBounds.top, digitBounds.bottom,
                colonBounds.top, colonBounds.bottom);
    }

    /** Pure geometry behind {@link #colonBaselineOffset(Paint)}; bounds are baseline relative. */
    static float colonBaselineOffset(int digitTop, int digitBottom,
            int colonTop, int colonBottom) {
        if (digitBottom <= digitTop || colonBottom <= colonTop) return 0f;
        return (digitTop + digitBottom) / 2f - (colonTop + colonBottom) / 2f;
    }

    /**
     * Draws {@code text} like {@link Canvas#drawText}, honouring the paint's text
     * alignment, with every ':' raised onto the digits' optical centre.
     */
    public static void draw(Canvas canvas, String text, float x, float baseline, Paint paint) {
        if (text == null || text.length() == 0) return;
        float offset = text.indexOf(COLON) < 0 ? 0f : colonBaselineOffset(paint);
        if (offset == 0f) {
            canvas.drawText(text, x, baseline, paint);
            return;
        }
        // Each run is positioned by hand, so resolve the alignment origin once and lay the
        // runs out from the left; the caller's own alignment is restored on the way out.
        Paint.Align align = paint.getTextAlign();
        float cursor = x;
        if (align == Paint.Align.CENTER) {
            cursor = x - measure(text, paint) / 2f;
        } else if (align == Paint.Align.RIGHT) {
            cursor = x - measure(text, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        try {
            int index = 0;
            while (index < text.length()) {
                int colonStart = text.indexOf(COLON, index);
                if (colonStart < 0) colonStart = text.length();
                cursor = drawRun(canvas, text, index, colonStart, cursor, baseline, paint);
                index = skipColons(text, colonStart);
                cursor = drawRun(canvas, text, colonStart, index, cursor,
                        baseline + offset, paint);
            }
        } finally {
            paint.setTextAlign(align);
        }
    }

    /**
     * Returns {@code text} with every ':' wrapped in a baseline shifting span, for
     * TextViews that display a time. The shift is recomputed from the view's own paint
     * every time it draws, so a later typeface or text size change stays aligned.
     */
    public static CharSequence align(CharSequence text) {
        if (text == null) return null;
        SpannableString spanned = null;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) != COLON) continue;
            if (spanned == null) spanned = new SpannableString(text);
            spanned.setSpan(new ColonSpan(), index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spanned == null ? text : spanned;
    }

    private static float drawRun(Canvas canvas, String text, int start, int end, float cursor,
            float baseline, Paint paint) {
        if (end <= start) return cursor;
        canvas.drawText(text, start, end, cursor, baseline, paint);
        return cursor + paint.measureText(text, start, end);
    }

    /** Width of {@code text} as {@link #draw} lays it out, so the two stay consistent. */
    private static float measure(String text, Paint paint) {
        float width = 0f;
        int index = 0;
        while (index < text.length()) {
            int colonStart = text.indexOf(COLON, index);
            if (colonStart < 0) colonStart = text.length();
            width += paint.measureText(text, index, colonStart);
            index = skipColons(text, colonStart);
            width += paint.measureText(text, colonStart, index);
        }
        return width;
    }

    private static int skipColons(String text, int start) {
        int index = start;
        while (index < text.length() && text.charAt(index) == COLON) index++;
        return index;
    }

    private static final class ColonSpan extends MetricAffectingSpan {
        @Override
        public void updateDrawState(TextPaint paint) {
            shift(paint);
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            shift(paint);
        }

        private void shift(TextPaint paint) {
            paint.baselineShift += Math.round(colonBaselineOffset(paint));
        }
    }
}
