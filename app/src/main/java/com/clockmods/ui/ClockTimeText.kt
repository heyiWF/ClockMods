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
        val align = paint.textAlign
        var cursor = when (align) {
            Paint.Align.CENTER -> x - stableWidth(text, paint) / 2f
            Paint.Align.RIGHT -> x - stableWidth(text, paint)
            else -> x
        }
        paint.textAlign = Paint.Align.CENTER
        try {
            text.forEachIndexed { index, character ->
                val width = slotWidth(text, index, paint)
                if (character != ' ') {
                    canvas.drawText(character.toString(), cursor + width / 2f,
                        baseline + if (character == COLON) offset else 0f, paint)
                }
                cursor += width
            }
        } finally {
            paint.textAlign = align
        }
    }

    /** Keep every numeric position the same width while retaining the selected typeface. */
    @JvmStatic
    fun stableWidth(text: String, paint: Paint): Float =
        text.indices.sumOf { slotWidth(text, it, paint).toDouble() }.toFloat()

    @JvmStatic
    fun slotWidth(text: String, index: Int, paint: Paint): Float {
        val character = text[index]
        return when {
            character in '0'..'9' -> ('0'..'9').maxOf { paint.measureText(it.toString()) }
            character == ' ' && ((index > 0 && text[index - 1].isDigit()) ||
                (index + 1 < text.length && text[index + 1].isDigit())) -> paint.measureText(":")
            else -> paint.measureText(character.toString())
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

    private class ColonSpan : MetricAffectingSpan() {
        override fun updateDrawState(paint: TextPaint) = shift(paint)
        override fun updateMeasureState(paint: TextPaint) = shift(paint)

        private fun shift(paint: TextPaint) {
            paint.baselineShift += kotlin.math.round(colonBaselineOffset(paint)).toInt()
        }
    }
}
