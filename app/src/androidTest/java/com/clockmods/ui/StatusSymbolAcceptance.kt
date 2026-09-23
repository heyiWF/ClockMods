package com.clockmods.ui

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle

/** Checks Android's real font renderer, including variable axes and supplementary glyphs. */
object StatusSymbolAcceptance {
    fun run(instrumentation: Instrumentation) {
        val context = instrumentation.targetContext
        var checks = 0
        fun render(style: StatusIconStyle, codePoint: Int): IntArray {
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            val drawn = StatusSymbolRenderer.draw(
                Canvas(bitmap), context, codePoint, style, 12f, 12f, 72f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK },
            )
            check(drawn) { "Font could not draw ${codePoint.toString(16)}" }
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            check(pixels.any { it != Color.TRANSPARENT }) {
                "Empty glyph ${codePoint.toString(16)}"
            }
            checks++
            bitmap.recycle()
            return pixels
        }

        val regular = StatusIconStyle.defaults()
        val icons = intArrayOf(
            StatusSymbolRenderer.BATTERY_0, StatusSymbolRenderer.BATTERY_1,
            StatusSymbolRenderer.BATTERY_2, StatusSymbolRenderer.BATTERY_3,
            StatusSymbolRenderer.BATTERY_4, StatusSymbolRenderer.BATTERY_5,
            StatusSymbolRenderer.BATTERY_6, StatusSymbolRenderer.BATTERY_FULL,
            StatusSymbolRenderer.BATTERY_BOLT, StatusSymbolRenderer.WIFI_0,
            StatusSymbolRenderer.WIFI_1, StatusSymbolRenderer.WIFI_2,
            StatusSymbolRenderer.WIFI_3, StatusSymbolRenderer.WIFI_4,
            StatusSymbolRenderer.WIFI_FULL, StatusSymbolRenderer.CELLULAR_0,
            StatusSymbolRenderer.CELLULAR_1, StatusSymbolRenderer.CELLULAR_2,
            StatusSymbolRenderer.CELLULAR_3, StatusSymbolRenderer.CELLULAR_4,
            StatusSymbolRenderer.CELL_TOWER, StatusSymbolRenderer.ETHERNET,
            StatusSymbolRenderer.GLOBE_CANCEL, StatusSymbolRenderer.PUBLIC,
        )
        icons.forEach { render(regular, it) }
        val sample = StatusSymbolRenderer.BATTERY_0
        val outlined = render(regular.withFill(StatusIconStyle.FILL_OUTLINE), sample)
        fun differs(style: StatusIconStyle, axis: String, codePoint: Int = sample) {
            val baseline = if (codePoint == sample) outlined else render(regular.withFill(StatusIconStyle.FILL_OUTLINE), codePoint)
            check(!baseline.contentEquals(render(style, codePoint))) { "$axis did not alter the glyph" }
            checks++
        }
        differs(regular.withFill(StatusIconStyle.FILL_SOLID), "FILL")
        differs(regular.withFill(StatusIconStyle.FILL_OUTLINE).withWeight(700), "wght")
        differs(regular.withFill(StatusIconStyle.FILL_OUTLINE).withGrade(200), "GRAD")
        differs(regular.withFill(StatusIconStyle.FILL_OUTLINE).withOpticalSize(48), "opsz")
        differs(regular.withFill(StatusIconStyle.FILL_OUTLINE).withFamily(StatusIconStyle.ROUNDED), "Rounded", StatusSymbolRenderer.ETHERNET)
        differs(regular.withFill(StatusIconStyle.FILL_OUTLINE).withFamily(StatusIconStyle.SHARP), "Sharp")
        instrumentation.finish(Activity.RESULT_OK, Bundle().apply {
            putInt("status_symbol_checks", checks)
        })
    }
}
