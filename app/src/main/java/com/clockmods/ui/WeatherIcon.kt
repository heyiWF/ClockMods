package com.clockmods.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.max

class WeatherIcon private constructor(private val path: Path) {
    private val matrix = Matrix()
    private val scaled = Path()
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bitmap: Bitmap? = null
    private var bitmapSize = 0
    private var bitmapColor = 0
    private var bitmapAlpha = 0

    @Synchronized
    fun draw(canvas: Canvas, left: Float, top: Float, size: Float, paint: Paint) {
        val targetSize = max(1, ceil(size.toDouble()).toInt())
        val color = paint.color
        val alpha = paint.alpha
        if (bitmap == null || bitmapSize != targetSize || bitmapColor != color || bitmapAlpha != alpha) {
            bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val bitmapCanvas = Canvas(bitmap!!)
            matrix.reset()
            matrix.setScale(targetSize / 16f, targetSize / 16f)
            scaled.reset()
            path.transform(matrix, scaled)
            iconPaint.color = color
            iconPaint.alpha = alpha
            iconPaint.style = Paint.Style.FILL
            bitmapCanvas.drawPath(scaled, iconPaint)
            bitmapSize = targetSize
            bitmapColor = color
            bitmapAlpha = alpha
        }
        canvas.drawBitmap(bitmap!!, left, top, null)
    }

    companion object {
        private val pathPattern = Pattern.compile("<path[^>]*d=\"([^\"]+)\"")
        private val cache = HashMap<String, WeatherIcon?>()

        @JvmStatic
        fun load(context: Context, code: String?, fill: Boolean): WeatherIcon? {
            if (code == null || !code.matches(Regex("[0-9]+"))) return null
            val asset = if (fill) "$code-fill" else code
            synchronized(cache) {
                if (cache.containsKey(asset)) return cache[asset]
                return read(context, asset).also { cache[asset] = it }
            }
        }

        private fun read(context: Context, asset: String): WeatherIcon? = try {
            context.assets.open("qweather-icons/$asset.svg").use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    val svg = buildString {
                        var line = reader.readLine()
                        while (line != null) {
                            append(line)
                            line = reader.readLine()
                        }
                    }
                    val matcher = pathPattern.matcher(svg)
                    val combined = Path()
                    var found = false
                    while (matcher.find()) {
                        combined.addPath(SvgPath.parse(matcher.group(1)))
                        found = true
                    }
                    if (found) WeatherIcon(combined) else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
