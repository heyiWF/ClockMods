package com.clockmods.ui

import android.graphics.Canvas

/** A left-to-right sweep with a feathered opacity boundary. */
object ClockScanTransition {
    private const val FEATHER_STEPS = 8

    fun draw(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float,
        edge: Float, feather: Float, revealing: Boolean, content: (Float) -> Unit) {
        if (right <= left || bottom <= top) return
        val soft = maxOf(1f, feather)
        if (revealing) {
            band(canvas, left, minOf(right, edge - soft), top, bottom, 1f, content)
            repeat(FEATHER_STEPS) { i ->
                val x0 = edge - soft + soft * i / FEATHER_STEPS
                val x1 = edge - soft + soft * (i + 1) / FEATHER_STEPS
                band(canvas, maxOf(left, x0), minOf(right, x1), top, bottom,
                    1f - (i + .5f) / FEATHER_STEPS, content)
            }
        } else {
            band(canvas, maxOf(left, edge + soft), right, top, bottom, 1f, content)
            repeat(FEATHER_STEPS) { i ->
                val x0 = edge + soft * i / FEATHER_STEPS
                val x1 = edge + soft * (i + 1) / FEATHER_STEPS
                band(canvas, maxOf(left, x0), minOf(right, x1), top, bottom,
                    (i + .5f) / FEATHER_STEPS, content)
            }
        }
    }

    private fun band(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float,
        opacity: Float, content: (Float) -> Unit) {
        if (right <= left || opacity <= 0f) return
        val save = canvas.save()
        canvas.clipRect(left, top, right, bottom)
        content(opacity)
        canvas.restoreToCount(save)
    }
}
