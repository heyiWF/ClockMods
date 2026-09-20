package com.clockmods.sdk.clock

import android.graphics.Bitmap

/** Optional host-provided background for a clock frame. */
class ClockBackground private constructor(
    private val bitmap: Bitmap?,
    private val color: Int,
    private val dimmed: Boolean,
    private val mode: Mode,
) {
    enum class Mode { THEME, COLOR, IMAGE }

    fun hasImage(): Boolean = mode == Mode.IMAGE && bitmap != null && !bitmap.isRecycled
    fun getBitmap(): Bitmap? = bitmap
    fun getColor(): Int = color
    fun isDimmed(): Boolean = dimmed
    fun getMode(): Mode = mode
    fun usesThemeSurface(): Boolean = mode == Mode.THEME

    companion object {
        @JvmStatic
        fun theme(dimmed: Boolean) = ClockBackground(null, 0, dimmed, Mode.THEME)

        @JvmStatic
        fun color(color: Int, dimmed: Boolean) = ClockBackground(null, color, dimmed, Mode.COLOR)

        @JvmStatic
        fun image(bitmap: Bitmap?, fallbackColor: Int, dimmed: Boolean): ClockBackground =
            if (bitmap == null || bitmap.isRecycled) color(fallbackColor, dimmed)
            else ClockBackground(bitmap, fallbackColor, dimmed, Mode.IMAGE)
    }
}
