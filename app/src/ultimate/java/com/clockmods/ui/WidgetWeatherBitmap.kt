package com.clockmods.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Bounded bitmap facade shared by RemoteViews widgets and the legacy weather view. */
object WidgetWeatherBitmap {
    @JvmStatic
    fun render(context: Context, code: String?, fill: Boolean, color: Int, size: Int): Bitmap {
        val boundedSize = size.coerceAtLeast(1)
        val icon = WeatherIcon.load(context, code, fill) ?: WeatherIcon.load(context, "999", fill)
        val bitmap = Bitmap.createBitmap(boundedSize, boundedSize, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val canvas = Canvas(bitmap)
        if (icon != null) {
            icon.draw(canvas, 0f, 0f, boundedSize.toFloat(), paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = boundedSize / 12f
            canvas.drawCircle(boundedSize / 2f, boundedSize / 2f, boundedSize / 3f, paint)
        }
        return bitmap
    }
}
