package com.clockmods.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

object ClockTimeText {
    private const val COLON = ':'

    @JvmStatic
    fun colonBaselineOffset(paint: Paint): Float {
        val digitBounds = Rect()
        val colonBounds = Rect()
        paint.getTextBounds("0", 0, 1, digitBounds)
        paint.getTextBounds(COLON.toString(), 0, 1, colonBounds)
        return colonBaselineOffset(
            digitBounds.top,
            digitBounds.bottom,
            colonBounds.top,
            colonBounds.bottom,
        )
    }

    @JvmStatic
    fun colonBaselineOffset(
        digitTop: Int,
        digitBottom: Int,
        colonTop: Int,
        colonBottom: Int,
    ): Float {
        if (digitBottom <= digitTop || colonBottom <= colonTop) return 0f
        return (digitTop + digitBottom) / 2f - (colonTop + colonBottom) / 2f
    }

    @JvmStatic
    fun draw(canvas: Canvas, text: String?, x: Float, baseline: Float, paint: Paint) {
        if (text.isNullOrEmpty()) return
        val offset = if (text.indexOf(COLON) < 0) 0f else colonBaselineOffset(paint)
        if (offset == 0f) {
            canvas.drawText(text, x, baseline, paint)
            return
        }

        val align = paint.textAlign
        var cursor = x
        if (align == Paint.Align.CENTER) cursor = x - measure(text, paint) / 2f
        else if (align == Paint.Align.RIGHT) cursor = x - measure(text, paint)
        paint.textAlign = Paint.Align.LEFT
        try {
            var index = 0
            while (index < text.length) {
                var colonStart = text.indexOf(COLON, index)
                if (colonStart < 0) colonStart = text.length
                cursor = drawRun(canvas, text, index, colonStart, cursor, baseline, paint)
                index = skipColons(text, colonStart)
                cursor = drawRun(canvas, text, colonStart, index, cursor, baseline + offset, paint)
            }
        } finally {
            paint.textAlign = align
        }
    }

    @JvmStatic
    fun align(text: CharSequence?): CharSequence? {
        if (text == null) return null
        var spanned: SpannableString? = null
        for (index in text.indices) {
            if (text[index] != COLON) continue
            if (spanned == null) spanned = SpannableString(text)
            spanned.setSpan(ColonSpan(), index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spanned ?: text
    }

    private fun drawRun(
        canvas: Canvas,
        text: String,
        start: Int,
        end: Int,
        cursor: Float,
        baseline: Float,
        paint: Paint,
    ): Float {
        if (end <= start) return cursor
        canvas.drawText(text, start, end, cursor, baseline, paint)
        return cursor + paint.measureText(text, start, end)
    }

    private fun measure(text: String, paint: Paint): Float {
        var width = 0f
        var index = 0
        while (index < text.length) {
            var colonStart = text.indexOf(COLON, index)
            if (colonStart < 0) colonStart = text.length
            width += paint.measureText(text, index, colonStart)
            index = skipColons(text, colonStart)
            width += paint.measureText(text, colonStart, index)
        }
        return width
    }

    private fun skipColons(text: String, start: Int): Int {
        var index = start
        while (index < text.length && text[index] == COLON) index++
        return index
    }

    private class ColonSpan : MetricAffectingSpan() {
        override fun updateDrawState(paint: TextPaint) = shift(paint)
        override fun updateMeasureState(paint: TextPaint) = shift(paint)

        private fun shift(paint: TextPaint) {
            paint.baselineShift += kotlin.math.round(colonBaselineOffset(paint)).toInt()
        }
    }
}
