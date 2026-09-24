package com.clockmods.ultimate.clock

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.clockmods.sdk.clock.ClockBackground
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockRenderer
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockStyle
import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.ClockStyleMetadata
import com.clockmods.sdk.clock.ClockStyleRegistry
import com.clockmods.sdk.clock.ClockThemeTokens
import com.clockmods.R
import com.clockmods.ui.ClockTimeText
import java.text.BreakIterator
import java.util.Calendar
import java.util.Locale

/** Kotlin entry point for the built-in Ultimate clock styles. */
object UltimateClockStyles {
    const val STYLE_PRO_CLASSIC = "pro.classic"
    const val STYLE_GLASS_ATELIER = "glass.atelier"
    const val STYLE_NOIR_INSTRUMENT = "noir.instrument"
    const val STYLE_PAPER_STATION = "paper.station"
    const val STYLE_ORBIT_NEON = "orbit.neon"
    const val STYLE_DIGITAL_GRID = "digital.grid"
    const val STYLE_TYPOGRAPHIC = "typographic.poster"
    const val STYLE_DUAL_BLOCKS = "ultimate.dual_blocks"
    const val STYLE_ORBIT = "ultimate.orbit"
    const val STYLE_BUBBLES = "ultimate.bubbles"
    const val STYLE_BLEND = "ultimate.blend"
    const val STYLE_RIBBON = "ultimate.ribbon"

    private val sharedRegistryHolder: ClockStyleRegistry by lazy(::createRegistry)

    @JvmStatic
    fun secondProgress(second: Int, millisecond: Int, motion: ClockState.SecondHandMotion): Float {
        val normalizedSecond = second.coerceIn(0, 59)
        return if (motion == ClockState.SecondHandMotion.SWEEP) {
            normalizedSecond + millisecond.coerceIn(0, 999) / 1000f
        } else {
            normalizedSecond.toFloat()
        }
    }

    @JvmStatic
    fun worldClockStripBounds(left: Float, top: Float, right: Float, bottom: Float,
        density: Float, bottomInset: Float): RectF {
        val height = maxOf(0f, bottom - top)
        val safeInset = minOf(maxOf(0f, bottomInset), height * .08f)
        val usableBottom = bottom - safeInset - minOf(density * 12f, height * .035f)
        val preferredHeight = height * .19f
        val minimumHeight = maxOf(1f, density) * 80f
        val maximumHeight = height * .25f
        val stripHeight = minOf(maximumHeight, maxOf(preferredHeight, minOf(minimumHeight, maximumHeight)))
        return RectF().also {
            // android.jar's mock RectF constructor is a no-op in local JVM tests.
            it.left = left
            it.top = usableBottom - stripHeight
            it.right = right
            it.bottom = usableBottom
        }
    }

    @JvmStatic
    fun worldClockCardWidth(viewportWidth: Float, viewportHeight: Float, density: Float): Float {
        val unit = minOf(maxOf(0f, viewportWidth), maxOf(0f, viewportHeight))
        return maxOf(unit * .30f, maxOf(1f, density) * 145f)
    }

    @JvmStatic
    fun worldClockCardGap(viewportWidth: Float, viewportHeight: Float, density: Float): Float {
        val unit = minOf(maxOf(0f, viewportWidth), maxOf(0f, viewportHeight))
        return maxOf(maxOf(1f, density) * 10f, unit * .018f)
    }

    @JvmStatic
    fun worldClockContentInset(styleId: String?, width: Float, faceHeight: Float, density: Float): Float {
        val portrait = faceHeight > width
        return when (styleId) {
            STYLE_DUAL_BLOCKS -> width * if (portrait) .055f else .029f
            STYLE_BUBBLES -> width * if (portrait) .09f else .029f
            STYLE_BLEND -> width * if (portrait) .05f else .030f
            STYLE_RIBBON -> width * .05f
            else -> maxOf(density * 20f, width * .029f)
        }
    }

    @JvmStatic
    fun worldClockContentWidth(count: Int, width: Float, height: Float, density: Float): Float =
        if (count <= 0) 0f else count * worldClockCardWidth(width, height, density) +
            (count - 1) * worldClockCardGap(width, height, density)

    @JvmStatic
    fun statusCapsuleBounds(styleId: String?, width: Float, height: Float, density: Float,
        marginX: Float, marginY: Float, capsuleWidth: Float, capsuleHeight: Float): FloatArray {
        if (styleId == STYLE_DUAL_BLOCKS) {
            if (width >= height) {
                val panelRight = width - width * .029f
                val panelWidth = panelRight - (width * .5f + width * .022f * .5f)
                return capsuleInPanel(panelRight, panelWidth * .05f, height * .037f,
                    height * .926f, density, capsuleWidth, capsuleHeight)
            }
            val gap = width * .035f
            return capsuleInPanel(width - width * .055f, width * .89f * .05f, height * .037f,
                height * .463f - gap * .5f, density, capsuleWidth, capsuleHeight)
        }
        if (styleId == STYLE_BLEND) {
            if (width >= height) {
                val panelRight = width - width * .027f
                val panelWidth = panelRight - width * .550f
                return capsuleInPanel(panelRight, panelWidth * .045f, height * .046f,
                    height * .920f, density, capsuleWidth, capsuleHeight)
            }
            return capsuleInPanel(width - width * .05f, width * .90f * .045f, height * .035f,
                height * .515f, density, capsuleWidth, capsuleHeight)
        }
        if (styleId == STYLE_ORBIT) {
            val left = width * CONTEXT_ROW_INSET
            val rowTop = height * .072f - minOf(width, height) * .027f * 1.15f
            val top = maxOf(marginY, rowTop - density * 4f - capsuleHeight)
            return floatArrayOf(left, top, left + capsuleWidth, top + capsuleHeight)
        }
        val fraction = endAlignedInsetFraction(styleId, width >= height)
        val endInset = if (fraction > 0f) width * fraction else marginX
        val right = width - endInset
        val top = topMetadataTop(styleId, width, height, density, marginY)
        return floatArrayOf(right - capsuleWidth, top, right, top + capsuleHeight)
    }

    @JvmStatic
    fun drawWorldClockCards(canvas: Canvas, context: ClockRenderContext, state: ClockState,
        theme: ClockThemeTokens, height: Float, originX: Float, originY: Float) =
        MigratedRenderer.drawWorldClockCards(canvas, context, state, theme, height, originX, originY)

    @JvmStatic
    fun ribbonSecondsTextSize(timeTextSize: Float): Float = maxOf(0f, timeTextSize) * .5f

    @JvmStatic
    fun bubblesPortraitGeometry(width: Float, height: Float, density: Float): FloatArray {
        val top = height * .13f
        val bottom = height * .91f
        val gap = maxOf(maxOf(1f, density) * 14f, width * .05f)
        val span = maxOf(1f, bottom - top)
        val shapeSpace = maxOf(1f, span - gap)
        val hourHeight = minOf(width * .50f, shapeSpace * .42f)
        val minuteDiameter = minOf(width * .74f, maxOf(1f, shapeSpace - hourHeight))
        val occupied = hourHeight + gap + minuteDiameter
        val hourTop = top + maxOf(0f, span - occupied) * .25f
        val hourBottom = hourTop + hourHeight
        val minuteRadius = minuteDiameter * .5f
        return floatArrayOf(hourTop, hourBottom, hourBottom + gap + minuteRadius, minuteRadius, gap)
    }

    @JvmStatic
    fun ribbonPortraitGeometry(width: Float, height: Float): FloatArray {
        val centerY = height * .53f
        val outerHeight = minOf(height * .36f, width * .42f)
        val ribbonHeight = minOf(outerHeight * .64f, width * .27f)
        return floatArrayOf(centerY, outerHeight, ribbonHeight)
    }

    @JvmStatic
    fun splitDateAndLunar(value: String?): Array<String> {
        val clean = value?.trim().orEmpty()
        val divider = clean.lastIndexOf(" / ")
        if (divider <= 0 || divider + 3 >= clean.length) return arrayOf(clean, "")
        val lunar = clean.substring(divider + 3).trim()
        if ('年' !in lunar || '月' !in lunar) return arrayOf(clean, "")
        return arrayOf(clean.substring(0, divider).trim(), lunar)
    }

    @JvmStatic
    fun dateLines(value: String?, paint: Paint, maxWidth: Float, stackLunar: Boolean,
        locale: Locale): Array<String> {
        val clean = value?.trim().orEmpty()
        val lunar = splitDateAndLunar(clean)
        if (stackLunar && lunar[1].isNotEmpty()) return lunar
        if (paint.measureText(clean) <= maxWidth) return arrayOf(clean, "")
        if (lunar[1].isNotEmpty()) return lunar

        val breaks = BreakIterator.getLineInstance(locale)
        breaks.setText(clean)
        var split = 0
        var bestWidth = Float.MAX_VALUE
        var index = breaks.first()
        while (index != BreakIterator.DONE) {
            if (index > 0 && index < clean.length) {
                val first = clean.substring(0, index).trim()
                val second = clean.substring(index).trim()
                if (first.isNotEmpty() && second.isNotEmpty()) {
                    val width = maxOf(paint.measureText(first), paint.measureText(second))
                    if (width < bestWidth) {
                        bestWidth = width
                        split = index
                    }
                }
            }
            index = breaks.next()
        }
        return if (split == 0) arrayOf(clean, "")
        else arrayOf(clean.substring(0, split).trim(), clean.substring(split).trim())
    }

    private const val CONTEXT_ROW_INSET = .029f
    private const val TYPOGRAPHIC_SECONDS_INSET = .07f
    private const val DIGITAL_GRID_SECONDS_INSET = .07f

    private fun topMetadataTop(styleId: String?, width: Float, height: Float, density: Float,
        marginY: Float): Float = if (styleId == STYLE_BUBBLES && width < height) {
        minOf(minOf(width, height) * .045f, density * 20f)
    } else {
        marginY
    }

    private fun endAlignedInsetFraction(styleId: String?, landscape: Boolean): Float = when (styleId) {
        STYLE_RIBBON -> CONTEXT_ROW_INSET
        STYLE_BUBBLES -> if (landscape) CONTEXT_ROW_INSET else 0f
        STYLE_TYPOGRAPHIC -> TYPOGRAPHIC_SECONDS_INSET
        STYLE_DIGITAL_GRID -> if (landscape) DIGITAL_GRID_SECONDS_INSET else 0f
        else -> 0f
    }

    private fun capsuleInPanel(panelRight: Float, rowInset: Float, panelTop: Float,
        panelHeight: Float, density: Float, capsuleWidth: Float, capsuleHeight: Float): FloatArray {
        val right = panelRight - rowInset
        val top = panelTop + minOf(density * 20f, panelHeight * .05f)
        return floatArrayOf(right - capsuleWidth, top, right, top + capsuleHeight)
    }

    internal abstract class RendererBase : ClockRenderer {
        internal class PaintPool {
            internal var photoText = false
            internal var motionState: ClockState? = null
            internal var digitTracker: ClockDigitTransitionTracker? = null
            internal var timeTextSizeObserver: ((Float) -> Unit)? = null
            private val paints = ArrayList<Paint>()
            private var nextIndex = 0
            private var frameDepth = 0

            internal fun beginFrame(): Int {
                if (frameDepth == 0) nextIndex = 0
                val marker = nextIndex
                frameDepth++
                return marker
            }

            internal fun endFrame(marker: Int) {
                nextIndex = marker
                if (frameDepth > 0) frameDepth--
                if (frameDepth == 0) nextIndex = 0
            }

            internal fun obtain(): Paint {
                if (nextIndex == paints.size) paints.add(Paint())
                return paints[nextIndex++]
            }
        }

        internal companion object {
            @JvmField
            internal val PAINT_POOL: ThreadLocal<PaintPool> = object : ThreadLocal<PaintPool>() {
                override fun initialValue() = PaintPool()
            }

            internal fun beginPaintFrame(): Int = PAINT_POOL.get().beginFrame()

            internal fun endPaintFrame(marker: Int) = PAINT_POOL.get().endFrame(marker)

            internal fun supportingTypefaceFor(theme: ClockThemeTokens, style: Int): Typeface? =
                theme.getSupportingTypeface()
                    ?: Typeface.create(theme.getSupportingFontFamily(), style)
        }

        protected fun fill(color: Int): Paint = PAINT_POOL.get().obtain().also {
            it.reset()
            it.flags = Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG
            it.style = Paint.Style.FILL
            it.color = color
        }

        protected fun stroke(color: Int, width: Float): Paint = PAINT_POOL.get().obtain().also {
            it.reset()
            it.flags = Paint.ANTI_ALIAS_FLAG
            it.style = Paint.Style.STROKE
            it.strokeWidth = width
            it.strokeCap = Paint.Cap.ROUND
            it.color = color
        }

        protected fun alpha(color: Int, value: Int): Int =
            Color.argb(value, Color.red(color), Color.green(color), Color.blue(color))

        protected fun typeface(family: String?, style: Int): Typeface? =
            Typeface.create(family ?: "sans-serif", style)

        protected fun displayTypeface(theme: ClockThemeTokens, style: Int): Typeface? =
            theme.getDisplayTypeface() ?: typeface(theme.getDisplayFontFamily(), style)

        protected fun supportingTypeface(theme: ClockThemeTokens, style: Int): Typeface? =
            theme.getSupportingTypeface() ?: typeface(theme.getSupportingFontFamily(), style)

        protected fun background(canvas: Canvas, context: ClockRenderContext,
            theme: ClockThemeTokens) = background(canvas, context, theme, true)

        protected fun background(canvas: Canvas, context: ClockRenderContext,
            theme: ClockThemeTokens, respectDimming: Boolean) {
            val hostBackground = context.getBackground()
            if (hostBackground != null && !hostBackground.usesThemeSurface()) {
                val left = context.getLeft()
                val top = context.getTop()
                val right = context.getRight()
                val bottom = context.getBottom()
                if (hostBackground.hasImage()) {
                    val bitmap: Bitmap = hostBackground.getBitmap()!!
                    val width = maxOf(1f, right - left)
                    val height = maxOf(1f, bottom - top)
                    val scale = maxOf(width / bitmap.width, height / bitmap.height)
                    val drawWidth = bitmap.width * scale
                    val drawHeight = bitmap.height * scale
                    val destination = RectF(
                        left + (width - drawWidth) * .5f,
                        top + (height - drawHeight) * .5f,
                        left + (width + drawWidth) * .5f,
                        top + (height + drawHeight) * .5f,
                    )
                    val imagePaint = fill(Color.WHITE)
                    imagePaint.isFilterBitmap = true
                    canvas.drawBitmap(bitmap, null, destination, imagePaint)
                } else {
                    canvas.drawRect(left, top, right, bottom, fill(hostBackground.getColor()))
                }
                if (hostBackground.isDimmed() && respectDimming) {
                    canvas.drawRect(left, top, right, bottom, fill(0x66000000))
                }
                return
            }
            val paint = fill(Color.WHITE)
            paint.shader = LinearGradient(
                context.getLeft(), context.getTop(), context.getRight(), context.getBottom(),
                theme.getBackgroundStartColor(), theme.getBackgroundEndColor(), Shader.TileMode.CLAMP,
            )
            canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(),
                context.getBottom(), paint)
            if (hostBackground?.isDimmed() == true) {
                canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(),
                    context.getBottom(), fill(0x66000000))
            }
        }

        protected fun text(canvas: Canvas, value: String?, x: Float, baseline: Float,
            size: Float, color: Int, align: Paint.Align, face: Typeface?) {
            if (value.isNullOrEmpty() || size <= 0f) return
            val paint = fill(color)
            paint.textSize = size
            paint.textAlign = align
            paint.typeface = face
            if (PAINT_POOL.get().photoText) {
                photoTextOutline(paint, size)
                canvas.drawText(value, x, baseline, paint)
                photoTextFill(paint, color, size)
            }
            canvas.drawText(value, x, baseline, paint)
        }

        private fun photoTextOutline(paint: Paint, size: Float) {
            val inkAlpha = Color.alpha(paint.color)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxOf(1f, minOf(2f, size * .012f))
            paint.color = Color.argb(inkAlpha * 230 / 255, 0, 0, 0)
        }

        private fun photoTextFill(paint: Paint, color: Int, size: Float) {
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.setShadowLayer(maxOf(2f, size * .035f), 0f, 1f,
                Color.argb(Color.alpha(color) * 153 / 255, 0, 0, 0))
        }

        protected fun fitText(value: String?, maxWidth: Float, size: Float, face: Typeface?): Float {
            if (value.isNullOrEmpty()) return size
            val paint = fill(Color.WHITE)
            paint.typeface = face
            paint.textSize = size
            val measured = paint.measureText(value)
            return if (measured > maxWidth && measured > 0f) size * maxWidth / measured else size
        }

        protected fun fittedText(canvas: Canvas, value: String?, x: Float, baseline: Float,
            maxWidth: Float, preferredSize: Float, color: Int, align: Paint.Align,
            face: Typeface?) {
            if (maxWidth <= 0f) return
            text(canvas, value, x, baseline, fitText(value, maxWidth, preferredSize, face),
                color, align, face)
        }

        protected fun readableSize(context: ClockRenderContext, preferredSize: Float,
            minimumSp: Float): Float {
            val unit = minOf(context.getWidth(), context.getHeight())
            val compactFloor = unit * if (minimumSp >= 12f) .045f else .038f
            val scaledFloor = context.getScaledDensity() * minimumSp
            return maxOf(preferredSize, minOf(scaledFloor, compactFloor))
        }

        protected fun readableText(canvas: Canvas, context: ClockRenderContext,
            value: String, x: Float, baseline: Float, maxWidth: Float, preferredSize: Float,
            minimumSp: Float, color: Int, align: Paint.Align, face: Typeface?) {
            if (maxWidth <= 0f) return
            val floor = readableSize(context, 0f, minimumSp)
            val size = maxOf(floor, fitText(value, maxWidth, maxOf(floor, preferredSize), face))
            val state = PAINT_POOL.get().motionState
            val progress = state?.getWeatherTransitionProgress() ?: 1f
            val previous = state?.getPreviousWeatherText().orEmpty()
            if (state != null && progress < 1f && previous.isNotEmpty() &&
                value == contextText(state)) {
                val oldSize = maxOf(floor,
                    fitText(previous, maxWidth, maxOf(floor, preferredSize), face))
                val oldColor = alpha(color, (Color.alpha(color) * (1f - progress)).toInt())
                val newColor = alpha(color, (Color.alpha(color) * progress).toInt())
                text(canvas, ellipsize(previous, maxWidth, oldSize, face), x, baseline, oldSize,
                    oldColor, align, face)
                text(canvas, ellipsize(value, maxWidth, size, face), x, baseline, size,
                    newColor, align, face)
            } else {
                text(canvas, ellipsize(value, maxWidth, size, face), x, baseline, size,
                    color, align, face)
            }
        }

        protected fun readableDate(canvas: Canvas, context: ClockRenderContext,
            state: ClockState, x: Float, lowerBaseline: Float, maxWidth: Float,
            preferredSize: Float, color: Int, align: Paint.Align, face: Typeface?,
            stackLunar: Boolean) {
            val inset = minOf(minOf(context.getWidth(), context.getHeight()) * .045f,
                context.getDensity() * 20f)
            val safeLeft = context.getLeft() + inset
            val safeRight = context.getRight() - inset
            val safeX = maxOf(safeLeft, minOf(safeRight, x))
            val availableWidth = when (align) {
                Paint.Align.LEFT -> safeRight - safeX
                Paint.Align.RIGHT -> safeX - safeLeft
                else -> 2f * minOf(safeX - safeLeft, safeRight - safeX)
            }
            val safeMaxWidth = minOf(maxWidth, availableWidth)
            if (safeMaxWidth <= 0f) return
            val floor = readableSize(context, 0f, 12f)
            val requested = maxOf(floor, preferredSize * state.getDateScale())
            val growth = (state.getDateScale() - 1f).coerceAtLeast(0f)
            val raisedBaseline = if (lowerBaseline < context.getCenterY()) {
                lowerBaseline - minOf(preferredSize * growth * .38f, context.getDensity() * 14f)
            } else lowerBaseline
            val requestedPaint = fill(Color.WHITE)
            requestedPaint.typeface = face
            requestedPaint.textSize = requested
            val lines = dateLines(
                state.getDateText(), requestedPaint, safeMaxWidth,
                stackLunar || state.isDateLunarDualLine(), state.getLocale(),
            )
            fun clearStatusOverlay(firstTop: Float, lastBottom: Float, size: Float): Float {
                val overlay = context.getStatusOverlay() ?: return 0f
                val rowLeft = when (align) {
                    Paint.Align.RIGHT -> safeX - safeMaxWidth
                    Paint.Align.CENTER -> safeX - safeMaxWidth * .5f
                    else -> safeX
                }
                if (!overlay.spansHorizontally(rowLeft, rowLeft + safeMaxWidth) ||
                    !overlay.spansVertically(firstTop, lastBottom)
                ) return 0f
                return overlay.getBottom() + maxOf(context.getDensity() * 4f, size * .25f) - firstTop
            }
            if (lines[1].isEmpty()) {
                val size = maxOf(floor, fitText(state.getDateText(), safeMaxWidth, requested, face))
                val metricsPaint = fill(Color.WHITE)
                metricsPaint.typeface = face
                metricsPaint.textSize = size
                val metrics = metricsPaint.fontMetrics
                var safeBaseline = maxOf(raisedBaseline, context.getTop() + inset - metrics.ascent)
                safeBaseline = minOf(safeBaseline, context.getBottom() - inset - metrics.descent)
                safeBaseline += clearStatusOverlay(safeBaseline + metrics.ascent,
                    safeBaseline + metrics.descent, size)
                safeBaseline = minOf(safeBaseline, context.getBottom() - inset - metrics.descent)
                text(canvas, ellipsize(state.getDateText(), safeMaxWidth, size, face), safeX,
                    safeBaseline, size, color, align, face)
                return
            }
            val size = maxOf(floor, minOf(
                fitText(lines[0], safeMaxWidth, requested, face),
                fitText(lines[1], safeMaxWidth, requested, face),
            ))
            val lineGap = size * 1.45f
            val metricsPaint = fill(Color.WHITE)
            metricsPaint.typeface = face
            metricsPaint.textSize = size
            val metrics = metricsPaint.fontMetrics
            var safeLowerBaseline = maxOf(raisedBaseline,
                context.getTop() + inset + lineGap - metrics.ascent)
            safeLowerBaseline = minOf(safeLowerBaseline,
                context.getBottom() - inset - metrics.descent)
            safeLowerBaseline += clearStatusOverlay(safeLowerBaseline - lineGap + metrics.ascent,
                safeLowerBaseline + metrics.descent, size)
            safeLowerBaseline = minOf(safeLowerBaseline,
                context.getBottom() - inset - metrics.descent)
            text(canvas, ellipsize(lines[0], safeMaxWidth, size, face), safeX,
                safeLowerBaseline - lineGap, size, color, align, face)
            text(canvas, ellipsize(lines[1], safeMaxWidth, size, face), safeX,
                safeLowerBaseline, size, color, align, face)
        }

        protected fun drawTime(canvas: Canvas, value: String?, x: Float, baseline: Float,
            size: Float, color: Int, align: Paint.Align, face: Typeface?) {
            if (value.isNullOrEmpty() || size <= 0f) return
            val paint = fill(color)
            paint.textSize = size
            paint.textAlign = align
            paint.typeface = face
            drawTimeWithPaint(canvas, value, x, baseline, paint)
        }

        protected fun drawTimeWithPaint(canvas: Canvas, value: String, x: Float,
            baseline: Float, paint: Paint) {
            val pool = PAINT_POOL.get()
            pool.timeTextSizeObserver?.invoke(paint.textSize)
            val previous = pool.digitTracker?.previousFor(value)
            val state = pool.motionState
            val progress = state?.getTimeTransitionProgress() ?: 1f
            val color = paint.color
            if (pool.photoText) {
                photoTextOutline(paint, paint.textSize)
                drawTimeCharacters(canvas, previous, value, x, baseline, paint, progress,
                    state?.getTimeTransition() ?: ClockState.TimeTransition.FADE)
                photoTextFill(paint, color, paint.textSize)
            }
            drawTimeCharacters(canvas, previous, value, x, baseline, paint, progress,
                state?.getTimeTransition() ?: ClockState.TimeTransition.FADE)
        }

        private fun drawTimeCharacters(canvas: Canvas, previous: String?, current: String,
            x: Float, baseline: Float, paint: Paint, progress: Float,
            transition: ClockState.TimeTransition) {
            if (previous == null || progress >= 1f ||
                changedDigitPositions(previous, current).isEmpty()) {
                ClockTimeText.draw(canvas, current, x, baseline, paint)
                return
            }
            val changed = changedDigitPositions(previous, current).toSet()
            val originalAlpha = paint.alpha
            val originalAlign = paint.textAlign
            val characterWidths = current.indices.map { ClockTimeText.slotWidth(current, it, paint) }
            val totalWidth = characterWidths.sum()
            var cursor = when (originalAlign) {
                Paint.Align.CENTER -> x - totalWidth / 2f
                Paint.Align.RIGHT -> x - totalWidth
                else -> x
            }
            paint.textAlign = Paint.Align.CENTER
            val colonOffset = if (':' in current) ClockTimeText.colonBaselineOffset(paint) else 0f
            try {
                current.forEachIndexed { index, character ->
                    val center = cursor + characterWidths[index] / 2f
                    val y = baseline + if (character == ':') colonOffset else 0f
                    if (index !in changed) {
                        paint.alpha = originalAlpha
                        canvas.drawText(character.toString(), center, y, paint)
                    } else {
                        val old = previous[index].toString()
                        val new = character.toString()
                        val pivot = baseline - paint.textSize / 2f
                        fun drawGlyph(value: String, fraction: Float, scaleY: Float = 1f,
                            shiftY: Float = 0f) {
                            if (fraction <= 0f) return
                            paint.alpha = (originalAlpha * fraction).toInt().coerceIn(0, 255)
                            val save = canvas.save()
                            canvas.translate(0f, shiftY)
                            canvas.scale(1f, scaleY, center, pivot)
                            canvas.drawText(value, center, baseline, paint)
                            canvas.restoreToCount(save)
                        }
                        when (transition) {
                            ClockState.TimeTransition.SLIDE_UP,
                            ClockState.TimeTransition.SLIDE_DOWN -> {
                                val direction = if (transition == ClockState.TimeTransition.SLIDE_UP) -1f else 1f
                                val distance = paint.textSize * .24f
                                drawGlyph(old, 1f - progress, shiftY = direction * distance * progress)
                                drawGlyph(new, progress, shiftY = -direction * distance * (1f - progress))
                            }
                            ClockState.TimeTransition.SCALE -> {
                                drawGlyph(old, 1f - progress, scaleY = 1f + .08f * progress)
                                drawGlyph(new, progress, scaleY = .88f + .12f * progress)
                            }
                            ClockState.TimeTransition.FLIP -> {
                                if (progress < .5f) drawGlyph(old, 1f, scaleY = maxOf(.05f, 1f - progress * 2f))
                                else drawGlyph(new, 1f, scaleY = maxOf(.05f, (progress - .5f) * 2f))
                            }
                            ClockState.TimeTransition.FADE -> {
                                drawGlyph(old, 1f - progress)
                                drawGlyph(new, progress)
                            }
                        }
                    }
                    cursor += characterWidths[index]
                }
            } finally {
                paint.alpha = originalAlpha
                paint.textAlign = originalAlign
            }
        }

        protected fun fittedTime(canvas: Canvas, value: String?, x: Float, baseline: Float,
            maxWidth: Float, preferredSize: Float, color: Int, align: Paint.Align,
            face: Typeface?) {
            if (maxWidth <= 0f) return
            drawTime(canvas, value, x, baseline, fitTime(value, maxWidth, preferredSize, face),
                color, align, face)
        }

        protected fun fitTime(value: String?, maxWidth: Float, size: Float, face: Typeface?): Float {
            if (value.isNullOrEmpty()) return size
            val paint = fill(Color.WHITE).apply { typeface = face; textSize = size }
            val measured = ClockTimeText.stableWidth(value, paint)
            return if (measured > maxWidth && measured > 0f) size * maxWidth / measured else size
        }

        protected fun contextText(state: ClockState): String = when {
            state.getWeatherText().isEmpty() -> state.getStatusText()
            state.getStatusText().isEmpty() -> state.getWeatherText()
            else -> state.getWeatherText() + "  ·  " + state.getStatusText()
        }

        protected fun fittedContext(canvas: Canvas, state: ClockState, value: String,
            x: Float, baseline: Float, maxWidth: Float, preferredSize: Float, color: Int,
            align: Paint.Align, face: Typeface?) = fittedText(
            canvas, value, x, baseline, maxWidth, preferredSize, color, align, face,
        )

        protected fun ellipsize(value: String?, maxWidth: Float, size: Float,
            face: Typeface?): String {
            if (value.isNullOrEmpty() || maxWidth <= 0f) return ""
            val paint = fill(Color.WHITE)
            paint.typeface = face
            paint.textSize = size
            if (paint.measureText(value) <= maxWidth) return value
            val suffix = "…"
            val suffixWidth = paint.measureText(suffix)
            var end = value.length
            while (end > 0 && paint.measureText(value, 0, end) + suffixWidth > maxWidth) end--
            return if (end == 0) suffix else value.substring(0, end).trim() + suffix
        }

        protected fun hasCustomBackground(context: ClockRenderContext): Boolean {
            val value: ClockBackground = context.getBackground() ?: return false
            return !value.usesThemeSurface()
        }

        /** Readable ink for text painted on a known surface, honouring the WCAG contrast floor. */
        protected fun inkOn(surface: Int): Int = ClockPalette.foreground(surface)

        protected fun customBackgroundVeil(canvas: Canvas, context: ClockRenderContext,
            color: Int) {
            if (!hasCustomBackground(context)) return
            canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(),
                context.getBottom(), fill(color))
        }

        protected fun lineWidth(context: ClockRenderContext, theme: ClockThemeTokens,
            dp: Float): Float = maxOf(1f, context.getDensity() * dp * theme.getStrokeScale())

        protected fun centeredBaseline(centerY: Float, size: Float, face: Typeface?): Float {
            val paint = fill(Color.WHITE)
            paint.textSize = size
            paint.typeface = face
            val metrics = paint.fontMetrics
            return centerY - (metrics.ascent + metrics.descent) * .5f
        }

        protected fun bottomOverlayShift(context: ClockRenderContext,
            preferredBottomBaseline: Float): Float {
            val safeBaseline = context.getBottom() - context.getBottomInset()
            return minOf(0f, safeBaseline - preferredBottomBaseline)
        }

        protected fun timeText(calendar: Calendar, state: ClockState, seconds: Boolean): String {
            var hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (!state.isUse24Hour()) {
                hour %= 12
                if (hour == 0) hour = 12
            }
            val colon = if (state.isBlinkColon() && calendar.get(Calendar.SECOND) % 2 != 0) " " else ":"
            return if (seconds) String.format(Locale.US, "%02d%s%02d%s%02d", hour, colon,
                calendar.get(Calendar.MINUTE), colon, calendar.get(Calendar.SECOND))
            else String.format(Locale.US, "%02d%s%02d", hour, colon, calendar.get(Calendar.MINUTE))
        }

        protected fun secondValue(calendar: Calendar, state: ClockState): Float =
            secondProgress(calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND),
                state.getSecondHandMotion())

        protected fun hourAngle(calendar: Calendar, state: ClockState): Float =
            (calendar.get(Calendar.HOUR) + calendar.get(Calendar.MINUTE) / 60f +
                secondValue(calendar, state) / 3600f) * 30f - 90f

        protected fun minuteAngle(calendar: Calendar, state: ClockState): Float =
            (calendar.get(Calendar.MINUTE) + secondValue(calendar, state) / 60f) * 6f - 90f

        protected fun secondAngle(calendar: Calendar, state: ClockState): Float =
            secondValue(calendar, state) * 6f - 90f

        protected fun cleanHand(canvas: Canvas, cx: Float, cy: Float, degrees: Float,
            length: Float, width: Float, color: Int, tail: Float, cap: Paint.Cap) {
            val radians = Math.toRadians(degrees.toDouble())
            val dx = Math.cos(radians).toFloat()
            val dy = Math.sin(radians).toFloat()
            val paint = stroke(color, width)
            paint.strokeCap = cap
            canvas.drawLine(cx - dx * tail, cy - dy * tail,
                cx + dx * length, cy + dy * length, paint)
        }

        protected fun arcEnd(canvas: Canvas, cx: Float, cy: Float, radius: Float,
            startDegrees: Float, sweepDegrees: Float, color: Int, width: Float) {
            val radians = Math.toRadians((startDegrees + sweepDegrees).toDouble())
            val x = cx + Math.cos(radians).toFloat() * radius
            val y = cy + Math.sin(radians).toFloat() * radius
            canvas.drawCircle(x, y, width * .52f, fill(color))
        }

        protected fun tick(canvas: Canvas, cx: Float, cy: Float, outer: Float,
            inner: Float, degrees: Float, paint: Paint) {
            val radians = Math.toRadians(degrees.toDouble())
            val dx = Math.cos(radians).toFloat()
            val dy = Math.sin(radians).toFloat()
            canvas.drawLine(cx + dx * inner, cy + dy * inner,
                cx + dx * outer, cy + dy * outer, paint)
        }

        protected fun ring(canvas: Canvas, cx: Float, cy: Float, radius: Float,
            color: Int, width: Float) = canvas.drawCircle(cx, cy, radius, stroke(color, width))
    }

    private class BuiltInStyle(
        private val metadata: ClockStyleMetadata,
        private val tokens: ClockThemeTokens,
        renderer: ClockRenderer,
    ) : ClockStyle {
        private val renderer = IsolatedRenderer(renderer)

        override fun getMetadata() = metadata
        override fun getThemeTokens() = tokens
        override fun getRenderer() = renderer
    }

    private class IsolatedRenderer(private val delegate: ClockRenderer) : ClockRenderer {
        override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState,
            theme: ClockThemeTokens) {
            val marker = RendererBase.beginPaintFrame()
            val paintPool = RendererBase.PAINT_POOL.get()
            val previousPhotoText = paintPool.photoText
            val previousMotionState = paintPool.motionState
            paintPool.photoText = false
            paintPool.motionState = state
            val saveCount = canvas.save()
            try {
                delegate.render(canvas, context, state, theme)
            } finally {
                canvas.restoreToCount(saveCount)
                RendererBase.endPaintFrame(marker)
                paintPool.photoText = previousPhotoText
                paintPool.motionState = previousMotionState
            }
        }
    }

    @JvmStatic
    fun createRegistry(): ClockStyleRegistry = ClockStyleRegistry().also { registry ->
        builtIns().forEach(registry::register)
        registry.setFallback(STYLE_GLASS_ATELIER)
    }

    @JvmStatic
    fun sharedRegistry(): ClockStyleRegistry = sharedRegistryHolder

    /**
     * Localized display name for a built-in style, so the settings gallery follows the app language
     * instead of the English/Chinese literals stored on [ClockStyleMetadata].
     */
    @JvmStatic
    fun styleNameRes(styleId: String?): Int = when (styleId) {
        STYLE_PRO_CLASSIC -> R.string.ultimate_style_pro_classic_name
        STYLE_GLASS_ATELIER -> R.string.ultimate_style_glass_name
        STYLE_NOIR_INSTRUMENT -> R.string.ultimate_style_noir_name
        STYLE_PAPER_STATION -> R.string.ultimate_style_paper_name
        STYLE_ORBIT_NEON -> R.string.ultimate_style_orbit_name
        STYLE_DIGITAL_GRID -> R.string.ultimate_style_grid_name
        STYLE_TYPOGRAPHIC -> R.string.ultimate_style_typographic_name
        STYLE_DUAL_BLOCKS -> R.string.ultimate_style_dual_blocks_name
        STYLE_ORBIT -> R.string.ultimate_style_orbit_migrated_name
        STYLE_BUBBLES -> R.string.ultimate_style_bubbles_name
        STYLE_BLEND -> R.string.ultimate_style_blend_name
        STYLE_RIBBON -> R.string.ultimate_style_ribbon_name
        else -> R.string.ultimate_style_pro_classic_name
    }

    /** Localized one-line summary matching [styleNameRes]. */
    @JvmStatic
    fun styleSummaryRes(styleId: String?): Int = when (styleId) {
        STYLE_PRO_CLASSIC -> R.string.ultimate_style_pro_classic_summary
        STYLE_GLASS_ATELIER -> R.string.ultimate_style_glass_summary
        STYLE_NOIR_INSTRUMENT -> R.string.ultimate_style_noir_summary
        STYLE_PAPER_STATION -> R.string.ultimate_style_paper_summary
        STYLE_ORBIT_NEON -> R.string.ultimate_style_orbit_summary
        STYLE_DIGITAL_GRID -> R.string.ultimate_style_grid_summary
        STYLE_TYPOGRAPHIC -> R.string.ultimate_style_typographic_summary
        STYLE_DUAL_BLOCKS -> R.string.ultimate_style_dual_blocks_summary
        STYLE_ORBIT -> R.string.ultimate_style_orbit_migrated_summary
        STYLE_BUBBLES -> R.string.ultimate_style_bubbles_summary
        STYLE_BLEND -> R.string.ultimate_style_blend_summary
        STYLE_RIBBON -> R.string.ultimate_style_ribbon_summary
        else -> R.string.ultimate_style_pro_classic_summary
    }

    @JvmStatic
    fun builtIns(): List<ClockStyle> {
        val seconds = ClockStyleCapabilities.Capability.SECONDS
        val smoothSeconds = ClockStyleCapabilities.Capability.SMOOTH_SECONDS
        val date = ClockStyleCapabilities.Capability.DATE
        val timeZone = ClockStyleCapabilities.Capability.TIME_ZONE
        val weather = ClockStyleCapabilities.Capability.WEATHER
        val status = ClockStyleCapabilities.Capability.STATUS
        val twentyFourHour = ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR
        val worldClock = ClockStyleCapabilities.Capability.WORLD_CLOCK
        val styles = ArrayList<ClockStyle>(12)
        styles += style(STYLE_PRO_CLASSIC, "Pro Classic",
            "The original ClockMods Pro face with its full set of display customizations.",
            ClockStyleMetadata.Kind.DIGITAL, proClassicTokens(),
            arrayOf(seconds, date, timeZone, weather, status, twentyFourHour),
            ProClassicRenderer())
        styles += style(STYLE_GLASS_ATELIER, "Glass Atelier",
            "A luminous metal and glass watch face inspired by premium industrial design.",
            ClockStyleMetadata.Kind.ANALOG, glassTokens(),
            arrayOf(seconds, smoothSeconds, date, timeZone, weather, status, twentyFourHour),
            GlassAtelierRenderer())
        styles += style(STYLE_NOIR_INSTRUMENT, "Noir Instrument",
            "A calibrated black instrument panel with a secondary seconds gauge.",
            ClockStyleMetadata.Kind.ANALOG, noirTokens(),
            arrayOf(seconds, smoothSeconds, date, timeZone, weather, status, twentyFourHour),
            NoirInstrumentRenderer())
        styles += style(STYLE_PAPER_STATION, "Paper Station",
            "A quiet paper planner face with ink marks, calendar rules, and a red index hand.",
            ClockStyleMetadata.Kind.ANALOG, paperTokens(),
            arrayOf(seconds, smoothSeconds, date, timeZone, weather), PaperStationRenderer())
        styles += style(STYLE_ORBIT_NEON, "Orbit Neon",
            "Concentric orbital progress rings turn time into a living instrument.",
            ClockStyleMetadata.Kind.HYBRID, orbitTokens(),
            arrayOf(seconds, smoothSeconds, date, timeZone, weather, status, twentyFourHour),
            OrbitNeonRenderer())
        styles += style(STYLE_DIGITAL_GRID, "Digital Grid",
            "A modular seven-segment display laid over a precise technical grid.",
            ClockStyleMetadata.Kind.DIGITAL, digitalTokens(),
            arrayOf(seconds, date, timeZone, weather, status, twentyFourHour),
            DigitalGridRenderer())
        styles += style(STYLE_TYPOGRAPHIC, "Typographic",
            "A bold editorial layout where time, date, and context form a measured poster.",
            ClockStyleMetadata.Kind.DIGITAL, typeTokens(),
            arrayOf(seconds, date, timeZone, weather, status, twentyFourHour),
            TypographicRenderer())

        val migrated = arrayOf(seconds, date, timeZone, weather, status, twentyFourHour, worldClock)
        val migratedSmooth = arrayOf(
            seconds, smoothSeconds, date, timeZone, weather, status, twentyFourHour, worldClock,
        )
        styles += style(
            STYLE_DUAL_BLOCKS, "双块", "小时与分钟的双块布局。",
            ClockStyleMetadata.Kind.DIGITAL, dualBlocksTokens(), migrated, DualBlocksRenderer(),
        )
        styles += style(
            STYLE_ORBIT, "轨道", "环形轨道与连续秒点。",
            ClockStyleMetadata.Kind.HYBRID, orbitMigratedTokens(), migratedSmooth, OrbitRenderer(),
        )
        styles += style(
            STYLE_BUBBLES, "气泡", "同心气泡与动态秒点。",
            ClockStyleMetadata.Kind.DIGITAL, bubblesTokens(), migrated, BubblesRenderer(),
        )
        styles += style(
            STYLE_BLEND, "混合", "几何块面与圆泡混合布局。",
            ClockStyleMetadata.Kind.HYBRID, blendTokens(), migratedSmooth, BlendRenderer(),
        )
        styles += style(
            STYLE_RIBBON, "丝带", "横向丝带承载时间信息。",
            ClockStyleMetadata.Kind.DIGITAL, ribbonTokens(), migrated, RibbonRenderer(),
        )
        return styles
    }

    private fun style(id: String, name: String, description: String,
        kind: ClockStyleMetadata.Kind, tokens: ClockThemeTokens,
        capabilities: Array<ClockStyleCapabilities.Capability>, renderer: ClockRenderer): ClockStyle =
        BuiltInStyle(
            ClockStyleMetadata(id, name, description, kind,
                ClockStyleCapabilities.of(*capabilities), 1, 14),
            tokens,
            renderer,
        )

    private fun glassTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFFF0F4F7.toInt(), 0xFFC9D3DC.toInt())
        .surfaceColor(0xF2F8FAFC.toInt()).primaryTextColor(0xFF14202B.toInt())
        .secondaryTextColor(0xFF556573.toInt()).accentColor(0xFFD9423A.toInt())
        .lineColor(0xFF8796A3.toInt()).fonts("sans-serif", "sans-serif")
        .strokeScale(1.05f).build()

    private fun proClassicTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFF000000.toInt(), 0xFF000000.toInt())
        .surfaceColor(0xFF000000.toInt()).primaryTextColor(0xFFFFFFFF.toInt())
        .secondaryTextColor(0xD9FFFFFF.toInt()).accentColor(0xFFFFFFFF.toInt())
        .lineColor(0x66FFFFFF).fonts("sans-serif", "sans-serif").strokeScale(1f).build()

    private fun noirTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFF111417.toInt(), 0xFF050607.toInt())
        .surfaceColor(0xFF15191C.toInt()).primaryTextColor(0xFFF0ECE2.toInt())
        .secondaryTextColor(0xFF8D969B.toInt()).accentColor(0xFFD7A247.toInt())
        .lineColor(0xFF424B50.toInt()).fonts("sans-serif-condensed", "monospace")
        .strokeScale(1.08f).build()

    private fun paperTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFFF4F1E9.toInt(), 0xFFE2D9C9.toInt())
        .surfaceColor(0xFFFAF7F0.toInt()).primaryTextColor(0xFF24211D.toInt())
        .secondaryTextColor(0xFF696159.toInt()).accentColor(0xFFB54740.toInt())
        .lineColor(0xFFB5A88F.toInt()).fonts("serif", "sans-serif").strokeScale(1f).build()

    private fun orbitTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFF10171A.toInt(), 0xFF050809.toInt())
        .surfaceColor(0xFF122126.toInt()).primaryTextColor(0xFFEAFBFF.toInt())
        .secondaryTextColor(0xFF8EABB2.toInt()).accentColor(0xFF2ED9C7.toInt())
        .lineColor(0xFF28535D.toInt()).fonts("sans-serif-light", "monospace")
        .strokeScale(1.1f).build()

    private fun digitalTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFF071110.toInt(), 0xFF020505.toInt())
        .surfaceColor(0xFF0A1C1A.toInt()).primaryTextColor(0xFFA8F7CF.toInt())
        .secondaryTextColor(0xFF69A88E.toInt()).accentColor(0xFFFFC95C.toInt())
        .lineColor(0xFF245246.toInt()).fonts("monospace", "monospace")
        .strokeScale(1.08f).build()

    private fun typeTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFFF1F0EB.toInt(), 0xFFDDE3E4.toInt())
        .surfaceColor(0xFF17191C.toInt()).primaryTextColor(0xFF181A1C.toInt())
        .secondaryTextColor(0xFF62686C.toInt()).accentColor(0xFFDF4B38.toInt())
        .lineColor(0xFFABB3B5.toInt()).fonts("sans-serif", "sans-serif")
        .strokeScale(1.08f).build()

    private fun dualBlocksTokens(): ClockThemeTokens = migratedTokens()

    private fun orbitMigratedTokens(): ClockThemeTokens = migratedTokens()

    private fun bubblesTokens(): ClockThemeTokens = migratedTokens()

    private fun blendTokens(): ClockThemeTokens = migratedTokens()

    private fun ribbonTokens(): ClockThemeTokens = migratedTokens()

    /** Palette sampled from the five reference compositions. */
    private fun migratedTokens(): ClockThemeTokens = ClockThemeTokens.builder()
        .background(0xFF154974.toInt(), 0xFF154974.toInt())
        .surfaceColor(0xFF9ECAFC.toInt()).primaryTextColor(0xFF003256.toInt())
        .secondaryTextColor(0xFFD0E4FF.toInt()).accentColor(0xFF8DBAE2.toInt())
        .lineColor(0xFF23557F.toInt()).fonts("sans-serif", "sans-serif")
        .strokeScale(1f).build()
}
