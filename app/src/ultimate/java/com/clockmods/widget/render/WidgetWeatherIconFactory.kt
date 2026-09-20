package com.clockmods.widget.render

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.clockmods.ui.WidgetWeatherBitmap

object WidgetWeatherIconFactory {
    private val cache = object : LruCache<String, Bitmap>(1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    @JvmStatic fun boundedSize(size: Int): Int = size.coerceIn(16, 144)

    @JvmStatic fun normalizedCode(code: String?): String =
        if (code != null && Regex("[0-9]{3,4}").matches(code)) code else "999"

    @JvmStatic @Synchronized fun render(
        context: Context,
        code: String?,
        fill: Boolean,
        color: Int,
        sizePx: Int,
    ): Bitmap {
        val size = boundedSize(sizePx)
        val normalized = normalizedCode(code)
        val key = "$normalized/$fill/$color/$size"
        return cache.get(key) ?: WidgetWeatherBitmap.render(context, normalized, fill, color, size).also {
            cache.put(key, it)
        }
    }
}
