package com.clockmods.ultimate.clock

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockThemeTokens
import java.util.HashMap
import java.util.WeakHashMap

/** A background-anchored blur sampled through moving card masks, including software previews. */
class GaussianGlass private constructor(
    private val bitmap: Bitmap,
    sourceWidth: Int,
    sourceHeight: Int,
    @JvmField val strength: Int,
    @JvmField val brightness: Int,
    context: ClockRenderContext,
) {
    private val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    private val imageToBackground = Matrix()
    private val sampling = Matrix()
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgeBounds = RectF()
    private val left = context.getLeft()
    private val top = context.getTop()
    private val right = context.getRight()
    private val bottom = context.getBottom()
    private val imageLeft: Float
    private val imageTop: Float
    private val scaleX: Float
    private val scaleY: Float
    private val materialPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val palettes = HashMap<Int, ClockPalette>()
    private val defaultPalette: ClockPalette
    private var activePalette: ClockPalette? = null

    init {
        materialPaint.shader = shader
        val transform = brightnessTransform(brightness)
        if (brightness != 50) {
            val scale = transform[0]
            val offset = transform[1]
            materialPaint.colorFilter = ColorMatrixColorFilter(floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ))
        }
        val scale = maxOf(context.getWidth() / sourceWidth, context.getHeight() / sourceHeight)
        scaleX = sourceWidth * scale / bitmap.width
        scaleY = sourceHeight * scale / bitmap.height
        imageLeft = context.getCenterX() - sourceWidth * scale * .5f
        imageTop = context.getCenterY() - sourceHeight * scale * .5f
        imageToBackground.setScale(scaleX, scaleY)
        imageToBackground.postTranslate(imageLeft, imageTop)
        edge.style = Paint.Style.STROKE
        edge.strokeWidth = maxOf(1f, context.getDensity())
        edge.color = 0x38FFFFFF
        defaultPalette = paletteAt(left, top, right - left, bottom - top)
    }

    fun palette(): ClockPalette {
        activePalette = defaultPalette
        return defaultPalette
    }

    /** Each moving city card derives its foreground hue from the image directly underneath it. */
    fun paletteAt(x: Float, y: Float, width: Float, height: Float): ClockPalette {
        var red = 0
        var green = 0
        var blue = 0
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val ix = ((x + width * (col + .5f) / 3f - imageLeft) / scaleX)
                    .toInt().coerceIn(0, bitmap.width - 1)
                val iy = ((y + height * (row + .5f) / 3f - imageTop) / scaleY)
                    .toInt().coerceIn(0, bitmap.height - 1)
                val color = bitmap.getPixel(ix, iy)
                red += (color ushr 16) and 255
                green += (color ushr 8) and 255
                blue += color and 255
            }
        }
        // Small buckets prevent foreground shimmer and keep the scrolling paint cache bounded.
        val tint = 0xFF000000.toInt() or ((red / 9 and 248) shl 16) or
            ((green / 9 and 248) shl 8) or (blue / 9 and 248)
        activePalette = palettes[tint]
        if (activePalette == null) {
            if (palettes.size >= 64) palettes.clear()
            activePalette = ClockPalette.glass(brightness, tint)
            palettes[tint] = activePalette!!
        }
        return activePalette!!
    }

    fun paint(surface: Int): Paint = paint(surface, 0f, 0f)

    /** Origin of the card's local canvas in the background view, updated on every scroll frame. */
    fun paint(surface: Int, originX: Float, originY: Float): Paint {
        sampling.set(imageToBackground)
        sampling.postTranslate(-originX, -originY)
        shader.setLocalMatrix(sampling)
        return material(surface)
    }

    private fun material(@Suppress("UNUSED_PARAMETER") surface: Int): Paint = materialPaint

    fun roundRect(
        canvas: Canvas,
        bounds: RectF,
        rx: Float,
        ry: Float,
        surface: Int,
        originX: Float,
        originY: Float,
        rotation: Float,
    ) {
        val fill = paint(surface, originX, originY)
        if (rotation != 0f) {
            sampling.postRotate(-rotation, bounds.centerX(), bounds.centerY())
            shader.setLocalMatrix(sampling)
        }
        canvas.drawRoundRect(bounds, rx, ry, fill)
        val inset = edge.strokeWidth * .5f
        edgeBounds.set(bounds)
        edgeBounds.inset(inset, inset)
        canvas.drawRoundRect(edgeBounds, maxOf(0f, rx - inset), maxOf(0f, ry - inset), edge)
    }

    fun circle(canvas: Canvas, cx: Float, cy: Float, radius: Float, surface: Int) {
        canvas.drawCircle(cx, cy, radius, paint(surface))
        canvas.drawCircle(cx, cy, maxOf(0f, radius - edge.strokeWidth * .5f), edge)
    }

    fun path(canvas: Canvas, path: Path, surface: Int) {
        canvas.drawPath(path, paint(surface))
        val save = canvas.save()
        canvas.clipPath(path)
        canvas.drawPath(path, edge)
        canvas.restoreToCount(save)
    }

    companion object {
        private const val BASE_BLUR_EDGE = 192
        // A weak blur preserves fine detail, so its working bitmap must also preserve enough detail.
        private const val MAX_DETAIL_EDGE = 1024
        // Above this sigma there is little quality benefit in adding taps instead of downsampling.
        private const val TARGET_WORKING_SIGMA = 4f
        private const val SIGMA_PER_STRENGTH = .06f
        // Keep texture visible at both slider ends instead of fading the blur to solid black/white.
        private const val MAX_BRIGHTNESS_OVERLAY_ALPHA = .55f
        // Keys do not keep the host's full-size background alive after replacement or view disposal.
        private val CACHE = WeakHashMap<Bitmap, GaussianGlass>()

        @JvmStatic
        @Synchronized
        fun create(context: ClockRenderContext, theme: ClockThemeTokens): GaussianGlass? {
            val background = context.getBackground()
            if (!theme.isGaussianBlur() || background == null || !background.hasImage()) return null
            val source = background.getBitmap() ?: return null
            val cached = CACHE[source]
            if (cached != null && cached.left == context.getLeft() && cached.top == context.getTop() &&
                cached.right == context.getRight() && cached.bottom == context.getBottom() &&
                cached.strength == theme.getBlurStrength() && cached.brightness == theme.getBlurBrightness()
            ) return cached
            val blur = if (cached != null && cached.strength == theme.getBlurStrength()) {
                cached.bitmap
            } else {
                blurred(source, theme.getBlurStrength())
            }
            return GaussianGlass(
                blur, source.width, source.height,
                theme.getBlurStrength(), theme.getBlurBrightness(), context,
            ).also { CACHE[source] = it }
        }

        private fun blurred(source: Bitmap, strength: Int): Bitmap {
            if (strength == 0) {
                // Preserve full detail at zero, without retaining the weak cache's source key.
                val copy = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                Canvas(copy).apply {
                    drawColor(0xFF000000.toInt())
                    drawBitmap(source, 0f, 0f, null)
                }
                return copy
            }
            val sourceLongEdge = maxOf(source.width, source.height)
            val targetLongEdge = workingLongEdge(sourceLongEdge, strength)
            val scale = minOf(1f, targetLongEdge / sourceLongEdge.toFloat())
            val width = maxOf(1, Math.round(source.width * scale))
            val height = maxOf(1, Math.round(source.height * scale))
            val small = Bitmap.createScaledBitmap(source, width, height, true)
            val pixels = IntArray(width * height)
            small.getPixels(pixels, 0, width, 0, 0, width, height)
            if (small !== source) small.recycle()
            val sigma = blurSigma(strength, maxOf(width, height), sourceLongEdge)
            return Bitmap.createBitmap(blurPixels(pixels, width, height, sigma), width, height,
                Bitmap.Config.ARGB_8888)
        }

        /** Selects an adaptive working resolution for source detail and bounded memory use. */
        @JvmStatic
        fun workingLongEdge(sourceLongEdge: Int, strength: Int): Int {
            val sourceEdge = maxOf(1, sourceLongEdge)
            val value = strength.coerceIn(0, 100)
            if (value == 0) return sourceEdge
            val baseEdge = minOf(sourceEdge, BASE_BLUR_EDGE)
            val qualityEdge = Math.round(baseEdge * TARGET_WORKING_SIGMA /
                (value * SIGMA_PER_STRENGTH))
            return minOf(sourceEdge, minOf(MAX_DETAIL_EDGE, maxOf(baseEdge, qualityEdge)))
        }

        /** Keeps the Gaussian radius in source-image coordinates stable as resolution changes. */
        @JvmStatic
        fun blurSigma(strength: Int, workingLongEdge: Int, sourceLongEdge: Int): Float {
            val value = strength.coerceIn(0, 100)
            val sourceEdge = maxOf(1, sourceLongEdge)
            val baseEdge = minOf(sourceEdge, BASE_BLUR_EDGE)
            val workingEdge = workingLongEdge.coerceIn(1, sourceEdge)
            return value * SIGMA_PER_STRENGTH * workingEdge / baseEdge
        }

        @JvmStatic
        fun blurPixels(pixels: IntArray, width: Int, height: Int): IntArray =
            blurPixels(pixels, width, height, ClockThemeTokens.DEFAULT_BLUR_STRENGTH)

        @JvmStatic
        fun blurPixels(pixels: IntArray, width: Int, height: Int, strength: Int): IntArray {
            val sigma = strength.coerceIn(0, 100) * SIGMA_PER_STRENGTH
            return blurPixels(pixels, width, height, sigma)
        }

        private fun blurPixels(pixels: IntArray, width: Int, height: Int, sigma: Float): IntArray {
            val radius = kotlin.math.ceil(sigma * 3f).toInt()
            val kernel = kernel(radius, sigma)
            val horizontal = FloatArray(pixels.size * 3)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val offset = (y * width + x) * 3
                    for (k in -radius..radius) {
                        val color = pixels[y * width + (x + k).coerceIn(0, width - 1)]
                        val weight = kernel[k + radius] * (color ushr 24) / 255f
                        for (c in 0..2) {
                            horizontal[offset + c] += ((color ushr (c * 8)) and 255) * weight
                        }
                    }
                }
            }
            val result = IntArray(pixels.size)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var color = 0xFF000000.toInt()
                    for (c in 0..2) {
                        var value = 0f
                        for (k in -radius..radius) {
                            val offset = ((y + k).coerceIn(0, height - 1) * width + x) * 3
                            value += horizontal[offset + c] * kernel[k + radius]
                        }
                        color = color or (minOf(255, Math.round(value)) shl (c * 8))
                    }
                    result[y * width + x] = color
                }
            }
            return result
        }

        private fun kernel(radius: Int, sigma: Float): FloatArray {
            if (radius == 0) return floatArrayOf(1f)
            val weights = FloatArray(radius * 2 + 1)
            var sum = 0f
            for (i in -radius..radius) {
                weights[i + radius] = Math.exp(-i * i / (2.0 * sigma * sigma)).toFloat()
                sum += weights[i + radius]
            }
            for (i in weights.indices) weights[i] /= sum
            return weights
        }

        /** Models a translucent black/white layer over the already blurred image. */
        @JvmStatic
        fun brightnessTransform(brightness: Int): FloatArray {
            val value = brightness.coerceIn(0, 100)
            val alpha = kotlin.math.abs(value - 50) / 50f * MAX_BRIGHTNESS_OVERLAY_ALPHA
            return floatArrayOf(1f - alpha, if (value > 50) 255f * alpha else 0f)
        }

        /** Applies the same transform used by the rendering paint to a sampled color. */
        @JvmStatic
        fun applyBrightnessOverlay(color: Int, brightness: Int): Int {
            val transform = brightnessTransform(brightness)
            var result = color and 0xFF000000.toInt()
            var shift = 0
            while (shift <= 16) {
                val channel = (color ushr shift) and 255
                val adjusted = Math.round(channel * transform[0] + transform[1]).coerceIn(0, 255)
                result = result or (adjusted shl shift)
                shift += 8
            }
            return result
        }
    }
}
