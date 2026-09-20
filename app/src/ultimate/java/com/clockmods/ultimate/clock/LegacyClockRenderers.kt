package com.clockmods.ultimate.clock

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockThemeTokens
import com.clockmods.ui.ClockTimeText
import com.clockmods.ui.ClockTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Kotlin renderers for the seven original gallery compositions. */
internal class ProClassicRenderer : UltimateClockStyles.RendererBase() {
    override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState, theme: ClockThemeTokens) {
        background(canvas, context, theme)
        val width = context.getWidth()
        val height = context.getHeight()
        val unit = min(width, height)
        val centerX = context.getCenterX()
        val centerY = context.getCenterY()
        val display = displayTypeface(theme, Typeface.NORMAL)
        val supporting = supportingTypeface(theme, Typeface.NORMAL)
        val calendar = state.newCalendar()
        if (height > width && state.isPortraitStacked()) {
            renderPortraitStacked(canvas, context, state, theme, calendar, display, supporting)
            return
        }
        val formatted = ClockTimeFormatter.format(
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND), state.isShowSeconds(), state.isBlinkColon(),
            state.isSmallSeconds(), state.isUse24Hour(), state.getLocale().language == "en",
        )
        val time = if (formatted.colonVisible) formatted.mainText else formatted.mainText.replace(':', ' ')
        val accessorySpace = when {
            formatted.hasSmallSeconds() && formatted.hasPeriod() -> unit * .25f
            formatted.hasSmallSeconds() || formatted.hasPeriod() -> unit * .14f
            else -> 0f
        }
        val timeSize = fitText(
            time,
            width * .94f - accessorySpace,
            unit * .42f * state.getTimeScale(),
            display,
        )
        val animatedTimeColor = transitionColor(theme.getPrimaryTextColor(), state)
        val timePaint = fill(animatedTimeColor).apply {
            typeface = display
            textSize = timeSize
            textAlign = Paint.Align.CENTER
            setShadowLayer(max(2f, unit * .035f), 0f, max(1f, unit * .012f), 0x66000000)
        }
        val accessorySize = timeSize * .42f
        val accessoryPaint = fill(animatedTimeColor).apply {
            typeface = supporting
            textSize = accessorySize
        }
        val mainWidth = timePaint.measureText(time)
        val gap = max(context.getDensity() * 6f, timeSize * .06f)
        val leftWidth = formatted.periodText.takeIf(String::isNotEmpty)
            ?.let(accessoryPaint::measureText) ?: 0f
        val rightWidth = formatted.secondsText.takeIf(String::isNotEmpty)
            ?.let(accessoryPaint::measureText) ?: 0f
        val totalWidth = mainWidth +
            (if (leftWidth > 0f) leftWidth + gap else 0f) +
            (if (rightWidth > 0f) rightWidth + gap else 0f)
        val mainLeft = centerX - totalWidth * .5f +
            (if (leftWidth > 0f) leftWidth + gap else 0f)
        val mainCenter = mainLeft + mainWidth * .5f
        val metrics = timePaint.fontMetrics
        val baseline = centerY - (metrics.ascent + metrics.descent) * .5f
        val transitionSave = beginTimeTransition(canvas, context, state, centerX, centerY)
        ClockTimeText.draw(canvas, time, mainCenter, baseline, timePaint)
        val accessoryBaseline = baseline + metrics.descent - accessoryPaint.fontMetrics.descent
        if (leftWidth > 0f) text(canvas, formatted.periodText, mainLeft - gap, accessoryBaseline,
            accessorySize, animatedTimeColor, Paint.Align.RIGHT, supporting)
        if (rightWidth > 0f) text(canvas, formatted.secondsText, mainLeft + mainWidth + gap,
            accessoryBaseline, accessorySize, animatedTimeColor, Paint.Align.LEFT, supporting)
        if (transitionSave >= 0) canvas.restoreToCount(transitionSave)

        val dateSize = readableSize(context, unit * .045f * state.getDateScale(), 12f)
        val supportingSize = readableSize(context, unit * .040f * state.getSupportingScale(), 12f)
        val supportingGap = max(context.getDensity() * 7f, max(dateSize, supportingSize) * .55f)
        readableDate(
            canvas, context, state, centerX, baseline + metrics.ascent - supportingGap,
            width * .88f, unit * .045f, theme.getSecondaryTextColor(), Paint.Align.CENTER,
            supporting, height > width,
        )
        readableText(
            canvas, context, contextText(state), centerX,
            baseline + metrics.descent + supportingGap + supportingSize,
            width * .88f, unit * .040f * state.getSupportingScale(), 12f,
            theme.getSecondaryTextColor(), Paint.Align.CENTER, supporting,
        )
    }

    private fun renderPortraitStacked(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        theme: ClockThemeTokens,
        calendar: Calendar,
        display: Typeface?,
        supporting: Typeface?,
    ) {
        val width = context.getWidth()
        val height = context.getHeight()
        val unit = min(width, height)
        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (!state.isUse24Hour()) {
            hour %= 12
            if (hour == 0) hour = 12
        }
        val lines = buildList {
            add(String.format(Locale.US, "%02d", hour))
            add(String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE)))
            if (state.isShowSeconds()) add(String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND)))
        }
        val top = context.getTop() + height * .20f
        val bottom = context.getTop() + height * .78f
        val step = if (lines.size == 1) 0f else (bottom - top) / (lines.size - 1)
        val preferredSize = min(unit * .34f * state.getTimeScale(), height * .20f)
        val timeSize = fitText("00", width * .72f, preferredSize, display)
        val animatedTimeColor = transitionColor(theme.getPrimaryTextColor(), state)
        val transitionSave = beginTimeTransition(
            canvas, context, state, context.getCenterX(), (top + bottom) * .5f,
        )
        lines.forEachIndexed { index, value ->
            drawTime(
                canvas, value, context.getCenterX(),
                centeredBaseline(top + step * index, timeSize, display),
                timeSize, animatedTimeColor, Paint.Align.CENTER, display,
            )
        }
        if (!state.isUse24Hour()) {
            val period = ClockTimeFormatter.periodText(
                calendar.get(Calendar.HOUR_OF_DAY), state.getLocale().language == "en",
            )
            text(canvas, period, context.getRight() - width * .10f,
                context.getTop() + height * .13f, unit * .055f,
                animatedTimeColor, Paint.Align.RIGHT, supporting)
        }
        if (transitionSave >= 0) canvas.restoreToCount(transitionSave)
        readableDate(
            canvas, context, state, context.getCenterX(), context.getTop() + height * .10f,
            width * .86f, unit * .042f, theme.getSecondaryTextColor(), Paint.Align.CENTER,
            supporting, true,
        )
        readableText(
            canvas, context, contextText(state), context.getCenterX(),
            context.getBottom() - height * .06f, width * .86f,
            unit * .038f * state.getSupportingScale(), 11f,
            theme.getSecondaryTextColor(), Paint.Align.CENTER, supporting,
        )
    }

    private fun transitionColor(color: Int, state: ClockState): Int {
        if (state.getTimeTransition() != ClockState.TimeTransition.FADE) return color
        val progress = state.getTimeTransitionProgress()
        if (progress >= 1f) return color
        return alpha(color, (255f * (.88f + .12f * progress)).toInt())
    }

    private fun beginTimeTransition(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        pivotX: Float,
        pivotY: Float,
    ): Int {
        val progress = state.getTimeTransitionProgress()
        if (progress >= 1f || state.getTimeTransition() == ClockState.TimeTransition.FADE) return -1
        val saveCount = canvas.save()
        val remaining = 1f - progress
        when (state.getTimeTransition()) {
            ClockState.TimeTransition.SLIDE_UP ->
                canvas.translate(0f, context.getHeight() * .025f * remaining)
            ClockState.TimeTransition.SLIDE_DOWN ->
                canvas.translate(0f, -context.getHeight() * .025f * remaining)
            ClockState.TimeTransition.SCALE -> {
                val scale = .94f + .06f * progress
                canvas.scale(scale, scale, pivotX, pivotY)
            }
            ClockState.TimeTransition.FLIP ->
                canvas.scale(1f, .05f + .95f * progress, pivotX, pivotY)
            ClockState.TimeTransition.FADE -> Unit
        }
        return saveCount
    }
}

internal class GlassAtelierRenderer : UltimateClockStyles.RendererBase() {
    override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState, theme: ClockThemeTokens) {
        background(canvas, context, theme)
        customBackgroundVeil(canvas, context, -1461129486)
        val w = context.getWidth()
        val h = context.getHeight()
        val unit = min(w, h)
        val landscape = w >= h * 1.12f
        val sans = supportingTypeface(theme, Typeface.NORMAL)
        val bold = displayTypeface(theme, Typeface.BOLD)
        val infoLeft = context.getLeft() + w * if (landscape) .075f else .10f
        val infoRight = context.getLeft() + w * if (landscape) .405f else .90f
        val titleY = context.getTop() + h * if (landscape) .15f else .075f
        var dateY = context.getTop() + h * if (landscape) .29f else .65f
        var timeY = context.getTop() + h * if (landscape) .47f else .76f
        var contextY = context.getTop() + h * if (landscape) .60f else .84f
        var zoneY = context.getTop() + h * if (landscape) .69f else .895f
        val small = readableSize(context, unit * .021f, 10f)
        val metadataShift = bottomOverlayShift(context, zoneY)
        dateY += metadataShift
        timeY += metadataShift
        contextY += metadataShift
        zoneY += metadataShift

        fittedText(canvas, "ATELIER / 01", infoLeft, titleY, infoRight - infoLeft, small,
            theme.getAccentColor(), Paint.Align.LEFT, bold)
        canvas.drawLine(
            infoLeft, titleY + unit * .035f,
            infoLeft + min(infoRight - infoLeft, unit * .16f), titleY + unit * .035f,
            stroke(alpha(theme.getAccentColor(), 190), lineWidth(context, theme, 1.2f)),
        )
        readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft,
            unit * .034f, theme.getSecondaryTextColor(), Paint.Align.LEFT, sans, !landscape)
        fittedTime(canvas, timeText(state.newCalendar(), state, false), infoLeft, timeY,
            infoRight - infoLeft, unit * if (landscape) .145f else .12f,
            theme.getPrimaryTextColor(), Paint.Align.LEFT, bold)
        readableText(canvas, context, contextText(state), infoLeft, contextY, infoRight - infoLeft,
            unit * .030f * state.getSupportingScale(), 12f,
            theme.getPrimaryTextColor(), Paint.Align.LEFT, sans)

        val cx = if (landscape) context.getLeft() + w * .69f else context.getCenterX()
        val cy = context.getTop() + h * if (landscape) .49f else .35f
        val radius = if (landscape) min(h * .33f, w * .225f) else min(w * .36f, h * .225f)
        canvas.drawCircle(cx, cy + radius * .045f, radius * 1.10f, fill(0x2A162531))
        val bezel = fill(theme.getLineColor()).apply {
            shader = LinearGradient(
                cx - radius, cy - radius, cx + radius, cy + radius,
                0xFFFDFEFF.toInt(), 0xFF9EABB6.toInt(), Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius * 1.085f, bezel)
        ring(canvas, cx, cy, radius * 1.085f, alpha(theme.getPrimaryTextColor(), 100), lineWidth(context, theme, .75f))
        val face = fill(Color.WHITE).apply {
            shader = RadialGradient(
                cx - radius * .28f, cy - radius * .32f, radius * 1.32f,
                Color.WHITE, 0xFFDDE5EA.toInt(), Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius, face)
        ring(canvas, cx, cy, radius * .94f, alpha(theme.getLineColor(), 120), lineWidth(context, theme, .7f))

        val tickPaint = stroke(theme.getLineColor(), lineWidth(context, theme, .8f))
        repeat(60) { i ->
            val major = i % 5 == 0
            tickPaint.color = if (major) theme.getPrimaryTextColor() else alpha(theme.getLineColor(), 180)
            tickPaint.strokeWidth = if (major) radius * .016f else radius * .006f
            tick(canvas, cx, cy, radius * .88f, radius * if (major) .77f else .83f, i * 6f - 90f, tickPaint)
        }
        val numerals = intArrayOf(12, 3, 6, 9)
        val numeralSize = radius * .13f
        numerals.forEach { value ->
            val angle = Math.toRadians(value * 30.0 - 90.0)
            val x = cx + cos(angle).toFloat() * radius * .68f
            val y = cy + sin(angle).toFloat() * radius * .68f
            text(canvas, value.toString(), x, centeredBaseline(y, numeralSize, bold), numeralSize,
                theme.getPrimaryTextColor(), Paint.Align.CENTER, bold)
        }
        val calendar = state.newCalendar()
        cleanHand(canvas, cx, cy, hourAngle(calendar, state), radius * .50f, radius * .052f,
            theme.getPrimaryTextColor(), radius * .07f, Paint.Cap.ROUND)
        cleanHand(canvas, cx, cy, minuteAngle(calendar, state), radius * .72f, radius * .028f,
            theme.getPrimaryTextColor(), radius * .10f, Paint.Cap.ROUND)
        if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
            cleanHand(canvas, cx, cy, secondAngle(calendar, state), radius * .82f,
                max(lineWidth(context, theme, 1f), radius * .009f), theme.getAccentColor(), radius * .17f,
                Paint.Cap.ROUND)
        }
        canvas.drawCircle(cx, cy, radius * .050f, fill(theme.getPrimaryTextColor()))
        canvas.drawCircle(cx, cy, radius * .026f, fill(theme.getAccentColor()))
    }
}

internal class NoirInstrumentRenderer : UltimateClockStyles.RendererBase() {
    override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState, theme: ClockThemeTokens) {
        background(canvas, context, theme)
        customBackgroundVeil(canvas, context, -838399734)
        val w = context.getWidth()
        val h = context.getHeight()
        val unit = min(w, h)
        val landscape = w >= h * 1.15f
        val split = if (landscape) context.getLeft() + w * .56f else context.getTop() + h * .56f
        val datum = stroke(alpha(theme.getLineColor(), 155), lineWidth(context, theme, .7f))
        if (landscape) {
            canvas.drawLine(split, context.getTop() + h * .10f, split, context.getTop() + h * .86f, datum)
        } else {
            canvas.drawLine(context.getLeft() + w * .09f, split, context.getRight() - w * .09f, split, datum)
        }
        repeat(9) { i ->
            val x = context.getLeft() + w * (.08f + i * .105f)
            canvas.drawLine(x, context.getTop() + h * .075f, x, context.getTop() + h * .09f,
                stroke(alpha(theme.getLineColor(), 115), lineWidth(context, theme, .65f)))
        }
        val cx = if (landscape) context.getLeft() + w * .30f else context.getCenterX()
        val cy = context.getTop() + h * if (landscape) .48f else .31f
        val radius = if (landscape) min(h * .34f, w * .225f) else min(w * .35f, h * .225f)
        canvas.drawCircle(cx, cy + radius * .035f, radius * 1.08f, fill(0x52000000))
        canvas.drawCircle(cx, cy, radius * 1.075f, fill(0xFF090B0C.toInt()))
        ring(canvas, cx, cy, radius * 1.075f, alpha(theme.getAccentColor(), 145), radius * .018f)
        canvas.drawCircle(cx, cy, radius, fill(theme.getSurfaceColor()))
        ring(canvas, cx, cy, radius, alpha(theme.getPrimaryTextColor(), 120), lineWidth(context, theme, 1.1f))
        ring(canvas, cx, cy, radius * .92f, alpha(theme.getLineColor(), 175), lineWidth(context, theme, .7f))
        val tickPaint = stroke(theme.getLineColor(), lineWidth(context, theme, .8f))
        repeat(60) { i ->
            val major = i % 5 == 0
            tickPaint.color = if (major) theme.getPrimaryTextColor() else alpha(theme.getLineColor(), 190)
            tickPaint.strokeWidth = if (major) radius * .014f else radius * .005f
            tick(canvas, cx, cy, radius * .86f, radius * if (major) .76f else .81f, i * 6f - 90f, tickPaint)
        }
        val mono = supportingTypeface(theme, Typeface.NORMAL)
        val condensed = displayTypeface(theme, Typeface.BOLD)
        intArrayOf(12, 3, 6, 9).forEach { value ->
            val angle = Math.toRadians(value * 30.0 - 90.0)
            val x = cx + cos(angle).toFloat() * radius * .66f
            val y = cy + sin(angle).toFloat() * radius * .66f
            text(canvas, value.toString(), x, centeredBaseline(y, radius * .115f, condensed), radius * .115f,
                theme.getPrimaryTextColor(), Paint.Align.CENTER, condensed)
        }
        val c = state.newCalendar()
        cleanHand(canvas, cx, cy, hourAngle(c, state), radius * .48f, radius * .042f,
            theme.getPrimaryTextColor(), radius * .07f, Paint.Cap.SQUARE)
        cleanHand(canvas, cx, cy, minuteAngle(c, state), radius * .70f, radius * .022f,
            theme.getPrimaryTextColor(), radius * .10f, Paint.Cap.SQUARE)
        if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
            cleanHand(canvas, cx, cy, secondAngle(c, state), radius * .80f,
                max(lineWidth(context, theme, .8f), radius * .007f), theme.getAccentColor(), radius * .16f,
                Paint.Cap.SQUARE)
        }
        canvas.drawCircle(cx, cy, radius * .038f, fill(theme.getAccentColor()))
        canvas.drawCircle(cx, cy, radius * .016f, fill(0xFF0A0C0D.toInt()))

        val infoLeft = context.getLeft() + w * if (landscape) .62f else .10f
        val infoRight = context.getRight() - w * if (landscape) .075f else .10f
        val titleY = context.getTop() + h * if (landscape) .17f else .62f
        var timeY = context.getTop() + h * if (landscape) .38f else .71f
        var dateY = context.getTop() + h * if (landscape) .49f else .78f
        var contextY = context.getTop() + h * if (landscape) .62f else .85f
        var zoneY = context.getTop() + h * if (landscape) .70f else .90f
        val small = readableSize(context, unit * .020f, 10f)
        val metadataShift = bottomOverlayShift(context, zoneY)
        timeY += metadataShift; dateY += metadataShift; contextY += metadataShift; zoneY += metadataShift
        fittedText(canvas, "INSTRUMENT / 24", infoLeft, titleY, infoRight - infoLeft, small,
            theme.getAccentColor(), Paint.Align.LEFT, mono)
        fittedTime(canvas, timeText(c, state, false), infoLeft, timeY, infoRight - infoLeft,
            unit * if (landscape) .14f else .12f, theme.getPrimaryTextColor(), Paint.Align.LEFT, condensed)
        readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft, unit * .030f,
            theme.getSecondaryTextColor(), Paint.Align.LEFT, mono, !landscape)
        readableText(canvas, context, contextText(state), infoLeft, contextY, infoRight - infoLeft,
            unit * .027f * state.getSupportingScale(), 12f, theme.getPrimaryTextColor(), Paint.Align.LEFT, mono)
        if (landscape) {
            val subX = context.getLeft() + w * .86f
            val subY = context.getTop() + h * .77f
            val subR = unit * .060f
            ring(canvas, subX, subY, subR, alpha(theme.getLineColor(), 210), lineWidth(context, theme, .8f))
            repeat(12) { i ->
                tick(canvas, subX, subY, subR * .87f, subR * .76f, i * 30f - 90f,
                    stroke(if (i % 3 == 0) theme.getAccentColor() else theme.getLineColor(),
                        lineWidth(context, theme, if (i % 3 == 0) 1f else .6f)))
            }
            if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                cleanHand(canvas, subX, subY, secondAngle(c, state), subR * .65f,
                    lineWidth(context, theme, .8f), theme.getAccentColor(), subR * .08f, Paint.Cap.SQUARE)
            }
            val seconds = if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
                String.format(Locale.US, "%02d", c.get(Calendar.SECOND))
            } else "--"
            val subSecondsSize = min(subR * .55f, readableSize(context, subR * .31f, 10f))
            text(canvas, seconds, subX, centeredBaseline(subY, subSecondsSize, mono), subSecondsSize,
                theme.getPrimaryTextColor(), Paint.Align.CENTER, mono)
        }
    }
}

internal class PaperStationRenderer : UltimateClockStyles.RendererBase() {
    override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState, theme: ClockThemeTokens) {
        background(canvas, context, theme)
        customBackgroundVeil(canvas, context, 0xE8F3EEE4.toInt())
        val w = context.getWidth()
        val h = context.getHeight()
        val unit = min(w, h)
        val landscape = w >= h * 1.12f
        val rule = stroke(alpha(theme.getLineColor(), 58), lineWidth(context, theme, .55f))
        val spacing = max(context.getDensity() * 18f, unit * .075f)
        var y = context.getTop() + spacing * .72f
        while (y < context.getBottom()) {
            canvas.drawLine(context.getLeft(), y, context.getRight(), y, rule)
            y += spacing
        }
        val marginX = context.getLeft() + w * .085f
        canvas.drawLine(marginX, context.getTop(), marginX, context.getBottom(),
            stroke(alpha(theme.getAccentColor(), 135), lineWidth(context, theme, .9f)))
        val cx = if (landscape) context.getLeft() + w * .31f else context.getCenterX()
        val cy = context.getTop() + h * if (landscape) .49f else .32f
        val radius = if (landscape) min(h * .285f, w * .22f) else min(w * .34f, h * .215f)
        canvas.drawCircle(cx, cy + radius * .025f, radius * 1.075f, fill(0x18000000))
        canvas.drawCircle(cx, cy, radius * 1.055f, fill(theme.getSurfaceColor()))
        ring(canvas, cx, cy, radius * 1.055f, alpha(theme.getLineColor(), 175), lineWidth(context, theme, .8f))
        ring(canvas, cx, cy, radius * .92f, alpha(theme.getLineColor(), 120), lineWidth(context, theme, .55f))
        val tickPaint = stroke(theme.getLineColor(), lineWidth(context, theme, .7f))
        repeat(60) { i ->
            val major = i % 5 == 0
            tickPaint.color = if (major) theme.getPrimaryTextColor() else alpha(theme.getLineColor(), 150)
            tickPaint.strokeWidth = if (major) radius * .010f else radius * .004f
            tick(canvas, cx, cy, radius * .85f, radius * if (major) .77f else .82f, i * 6f - 90f, tickPaint)
        }
        val serif = displayTypeface(theme, Typeface.NORMAL)
        val sans = supportingTypeface(theme, Typeface.NORMAL)
        val sansBold = supportingTypeface(theme, Typeface.BOLD)
        val numeralSize = radius * .105f
        intArrayOf(12, 3, 6, 9).forEach { value ->
            val angle = Math.toRadians(value * 30.0 - 90.0)
            val x = cx + cos(angle).toFloat() * radius * .66f
            val numeralY = cy + sin(angle).toFloat() * radius * .66f
            text(canvas, value.toString(), x, centeredBaseline(numeralY, numeralSize, serif),
                numeralSize, theme.getPrimaryTextColor(), Paint.Align.CENTER, serif)
        }
        val calendar = state.newCalendar()
        cleanHand(canvas, cx, cy, hourAngle(calendar, state), radius * .48f, radius * .032f,
            theme.getPrimaryTextColor(), radius * .06f, Paint.Cap.SQUARE)
        cleanHand(canvas, cx, cy, minuteAngle(calendar, state), radius * .70f, radius * .018f,
            theme.getPrimaryTextColor(), radius * .08f, Paint.Cap.SQUARE)
        if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
            cleanHand(canvas, cx, cy, secondAngle(calendar, state), radius * .80f,
                max(lineWidth(context, theme, .75f), radius * .006f), theme.getAccentColor(),
                radius * .15f, Paint.Cap.SQUARE)
        }
        canvas.drawCircle(cx, cy, radius * .037f, fill(theme.getAccentColor()))
        canvas.drawCircle(cx, cy, radius * .014f, fill(theme.getSurfaceColor()))

        val infoLeft = context.getLeft() + w * if (landscape) .59f else .13f
        val infoRight = context.getRight() - w * if (landscape) .09f else .13f
        val titleY = context.getTop() + h * if (landscape) .17f else .61f
        var dateY = context.getTop() + h * if (landscape) .29f else .68f
        var dividerY = context.getTop() + h * if (landscape) .35f else .72f
        var timeY = context.getTop() + h * if (landscape) .52f else .80f
        var contextY = context.getTop() + h * if (landscape) .64f else .86f
        var zoneY = context.getTop() + h * if (landscape) .72f else .905f
        val index = String.format(Locale.US, "PAPER / %02d.%02d",
            calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        val small = readableSize(context, unit * .020f, 10f)
        val metadataShift = bottomOverlayShift(context, zoneY)
        dateY += metadataShift; dividerY += metadataShift; timeY += metadataShift
        contextY += metadataShift; zoneY += metadataShift
        fittedText(canvas, index, infoLeft, titleY, infoRight - infoLeft, small,
            theme.getAccentColor(), Paint.Align.LEFT, sansBold)
        readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft,
            unit * .032f, theme.getSecondaryTextColor(), Paint.Align.LEFT, serif, !landscape)
        canvas.drawLine(infoLeft, dividerY, infoRight, dividerY,
            stroke(theme.getAccentColor(), lineWidth(context, theme, 1f)))
        fittedTime(canvas, timeText(calendar, state, false), infoLeft, timeY, infoRight - infoLeft,
            unit * if (landscape) .14f else .12f,
            theme.getPrimaryTextColor(), Paint.Align.LEFT, sansBold)
        readableText(canvas, context, contextText(state), infoLeft, contextY, infoRight - infoLeft,
            unit * .027f * state.getSupportingScale(), 12f,
            theme.getPrimaryTextColor(), Paint.Align.LEFT, sans)
    }
}

internal class OrbitNeonRenderer : UltimateClockStyles.RendererBase() {
    override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState, theme: ClockThemeTokens) {
        background(canvas, context, theme)
        customBackgroundVeil(canvas, context, 0xC70A1012.toInt())
        val w = context.getWidth()
        val h = context.getHeight()
        val unit = min(w, h)
        val landscape = w >= h * 1.12f
        val cx = if (landscape) context.getLeft() + w * .35f else context.getCenterX()
        val cy = context.getTop() + h * if (landscape) .49f else .36f
        val outer = if (landscape) min(h * .335f, w * .24f) else min(w * .34f, h * .235f)
        val middle = outer * .72f
        val inner = outer * .47f
        val outerWidth = max(lineWidth(context, theme, 2f), unit * .024f)
        val middleWidth = max(lineWidth(context, theme, 1.6f), unit * .018f)
        val innerWidth = max(lineWidth(context, theme, 1.3f), unit * .014f)
        canvas.drawCircle(cx, cy, outer, stroke(alpha(theme.getLineColor(), 115), outerWidth))
        canvas.drawCircle(cx, cy, middle, stroke(alpha(theme.getLineColor(), 100), middleWidth))
        canvas.drawCircle(cx, cy, inner, stroke(alpha(theme.getLineColor(), 85), innerWidth))
        val c = state.newCalendar()
        val hourProgress = (c.get(Calendar.HOUR) + c.get(Calendar.MINUTE) / 60f) / 12f
        val minuteProgress = (c.get(Calendar.MINUTE) + secondValue(c, state) / 60f) / 60f
        val secondProgress = secondValue(c, state) / 60f
        val outerRect = RectF(cx - outer, cy - outer, cx + outer, cy + outer)
        val midRect = RectF(cx - middle, cy - middle, cx + middle, cy + middle)
        val innerRect = RectF(cx - inner, cy - inner, cx + inner, cy + inner)
        canvas.drawArc(outerRect, -90f, hourProgress * 360f, false, stroke(theme.getAccentColor(), outerWidth))
        arcEnd(canvas, cx, cy, outer, -90f, hourProgress * 360f, theme.getAccentColor(), outerWidth)
        canvas.drawArc(midRect, -90f, minuteProgress * 360f, false, stroke(0xFF62AEFF.toInt(), middleWidth))
        arcEnd(canvas, cx, cy, middle, -90f, minuteProgress * 360f, 0xFF62AEFF.toInt(), middleWidth)
        if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
            canvas.drawArc(innerRect, -90f, secondProgress * 360f, false, stroke(0xFFFF725E.toInt(), innerWidth))
            arcEnd(canvas, cx, cy, inner, -90f, secondProgress * 360f, 0xFFFF725E.toInt(), innerWidth)
        }
        repeat(12) { i ->
            val a = Math.toRadians(i * 30.0 - 90.0)
            val px = cx + cos(a).toFloat() * outer * .86f
            val py = cy + sin(a).toFloat() * outer * .86f
            canvas.drawCircle(px, py, if (i % 3 == 0) unit * .006f else unit * .0035f,
                fill(if (i % 3 == 0) alpha(theme.getPrimaryTextColor(), 190) else alpha(theme.getLineColor(), 170)))
        }
        val time = timeText(c, state, false)
        val display = displayTypeface(theme, Typeface.NORMAL)
        val mono = supportingTypeface(theme, Typeface.NORMAL)
        val timeSize = fitText(time, inner * 1.62f, unit * .105f, display)
        drawTime(canvas, time, cx, centeredBaseline(cy, timeSize, display), timeSize,
            theme.getPrimaryTextColor(), Paint.Align.CENTER, display)
        val seconds = if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
            String.format(Locale.US, "%02d", c.get(Calendar.SECOND))
        } else "--"
        text(canvas, seconds, cx, cy + inner * .46f, readableSize(context, unit * .023f, 12f),
            0xFFFF725E.toInt(), Paint.Align.CENTER, mono)

        val infoLeft = context.getLeft() + w * if (landscape) .65f else .10f
        val infoRight = context.getRight() - w * if (landscape) .08f else .10f
        val titleY = context.getTop() + h * if (landscape) .17f else .075f
        var dateY = context.getTop() + h * if (landscape) .30f else .67f
        var contextY = context.getTop() + h * if (landscape) .40f else .745f
        var zoneY = context.getTop() + h * if (landscape) .48f else .80f
        val small = readableSize(context, unit * .020f, 10f)
        val valuesBottomY = context.getTop() + h * if (landscape) .81f else .89f
        val metadataShift = bottomOverlayShift(context, valuesBottomY)
        dateY += metadataShift; contextY += metadataShift; zoneY += metadataShift
        fittedText(canvas, "ORBIT / LIVE", infoLeft, titleY, infoRight - infoLeft, small,
            theme.getAccentColor(), Paint.Align.LEFT, mono)
        readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft, unit * .031f,
            theme.getPrimaryTextColor(), Paint.Align.LEFT, display, !landscape)
        readableText(canvas, context, contextText(state), infoLeft, contextY, infoRight - infoLeft,
            unit * .026f * state.getSupportingScale(), 12f, theme.getSecondaryTextColor(), Paint.Align.LEFT, mono)
        var hourValue = c.get(Calendar.HOUR)
        if (hourValue == 0) hourValue = 12
        if (landscape) {
            val values = arrayOf(
                String.format(Locale.US, "H  %02d / 12", hourValue),
                String.format(Locale.US, "M  %02d / 60", c.get(Calendar.MINUTE)),
                "S  $seconds / 60",
            )
            val colors = intArrayOf(theme.getAccentColor(), 0xFF62AEFF.toInt(), 0xFFFF725E.toInt())
            values.forEachIndexed { i, value ->
                val lineY = context.getTop() + h * (.62f + i * .095f) + metadataShift
                canvas.drawLine(infoLeft, lineY - small * .32f, infoLeft + unit * .045f,
                    lineY - small * .32f, stroke(colors[i], lineWidth(context, theme, 1.6f)))
                val valueX = infoLeft + unit * .072f
                fittedText(canvas, value, valueX, lineY, infoRight - valueX, small,
                    theme.getPrimaryTextColor(), Paint.Align.LEFT, mono)
            }
        } else {
            val values = arrayOf(
                String.format(Locale.US, "H %02d", hourValue),
                String.format(Locale.US, "M %02d", c.get(Calendar.MINUTE)),
                "S $seconds",
            )
            val colors = intArrayOf(theme.getAccentColor(), 0xFF62AEFF.toInt(), 0xFFFF725E.toInt())
            values.forEachIndexed { i, value ->
                val x = context.getLeft() + w * (.20f + i * .30f)
                fittedText(canvas, value, x, context.getTop() + h * .89f + metadataShift,
                    w * .24f, small, colors[i], Paint.Align.CENTER, mono)
            }
        }
    }
}

internal class DigitalGridRenderer : UltimateClockStyles.RendererBase() {
    private val digits = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 0), intArrayOf(0, 1, 1, 0, 0, 0, 0),
        intArrayOf(1, 1, 0, 1, 1, 0, 1), intArrayOf(1, 1, 1, 1, 0, 0, 1),
        intArrayOf(0, 1, 1, 0, 0, 1, 1), intArrayOf(1, 0, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1), intArrayOf(1, 1, 1, 0, 0, 0, 0),
        intArrayOf(1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 1, 1, 1, 0, 1, 1),
    )

    override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState, theme: ClockThemeTokens) {
        background(canvas, context, theme)
        customBackgroundVeil(canvas, context, 0xD407100F.toInt())
        val w = context.getWidth()
        val h = context.getHeight()
        val unit = min(w, h)
        val grid = max(context.getDensity() * 18f, unit * .075f)
        val gridPaint = stroke(alpha(theme.getLineColor(), 60), lineWidth(context, theme, .45f))
        var x = context.getLeft()
        while (x <= context.getRight()) {
            canvas.drawLine(x, context.getTop(), x, context.getBottom(), gridPaint)
            x += grid
        }
        var y = context.getTop()
        while (y <= context.getBottom()) {
            canvas.drawLine(context.getLeft(), y, context.getRight(), y, gridPaint)
            y += grid
        }
        val mono = displayTypeface(theme, Typeface.NORMAL)
        text(canvas, "DIGITAL GRID", context.getLeft() + w * .07f, context.getTop() + h * .10f,
            readableSize(context, unit * .028f, 10f), theme.getAccentColor(), Paint.Align.LEFT, mono)
        val c = state.newCalendar()
        var hour = c.get(Calendar.HOUR_OF_DAY)
        if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12 }
        val timeDigits = String.format(Locale.US, "%02d%02d", hour, c.get(Calendar.MINUTE))
        val landscape = w >= h * 1.2f
        val blockTop = context.getTop() + h * if (landscape) .23f else .19f
        val blockHeight = h * if (landscape) .39f else .29f
        val left = context.getLeft() + w * .075f
        val right = context.getLeft() + w * if (landscape) .79f else .925f
        val digitGap = max(context.getDensity() * 4f, w * .012f)
        val colonGap = max(context.getDensity() * 12f, w * if (landscape) .050f else .045f)
        val digitWidth = (right - left - digitGap * 2f - colonGap) / 4f
        val positions = floatArrayOf(
            left,
            left + digitWidth + digitGap,
            left + digitWidth * 2f + digitGap + colonGap,
            left + digitWidth * 3f + digitGap * 2f + colonGap,
        )
        val panelPad = unit * .012f
        val radius = unit * .010f
        val hoursPanel = RectF(positions[0] - panelPad, blockTop - panelPad,
            positions[1] + digitWidth + panelPad, blockTop + blockHeight + panelPad)
        val minutesPanel = RectF(positions[2] - panelPad, blockTop - panelPad,
            positions[3] + digitWidth + panelPad, blockTop + blockHeight + panelPad)
        listOf(hoursPanel, minutesPanel).forEach { panel ->
            canvas.drawRoundRect(panel, radius, radius, fill(alpha(theme.getSurfaceColor(), 225)))
            canvas.drawRoundRect(panel, radius, radius, stroke(alpha(theme.getLineColor(), 185), lineWidth(context, theme, .7f)))
        }
        repeat(4) { i ->
            val box = RectF(positions[i], blockTop, positions[i] + digitWidth, blockTop + blockHeight)
            drawDigit(canvas, timeDigits[i] - '0', box, theme)
        }
        val colonX = (positions[1] + digitWidth + positions[2]) * .5f
        val colonRadius = unit * .010f
        canvas.drawCircle(colonX, blockTop + blockHeight * .375f, colonRadius, fill(theme.getAccentColor()))
        canvas.drawCircle(colonX, blockTop + blockHeight * .625f, colonRadius, fill(theme.getAccentColor()))

        val secPanel = if (landscape) {
            RectF(context.getLeft() + w * .835f, blockTop - panelPad,
                context.getLeft() + w * .93f, blockTop + blockHeight + panelPad)
        } else {
            RectF(context.getLeft() + w * .34f, context.getTop() + h * .54f,
                context.getLeft() + w * .66f, context.getTop() + h * .635f)
        }
        canvas.drawRoundRect(secPanel, radius, radius, fill(alpha(theme.getSurfaceColor(), 225)))
        canvas.drawRoundRect(secPanel, radius, radius, stroke(alpha(theme.getAccentColor(), 190), lineWidth(context, theme, .8f)))
        val secondsVisible = state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF
        if (secondsVisible) {
            val seconds = String.format(Locale.US, "%02d", c.get(Calendar.SECOND))
            val innerGap = secPanel.width() * .06f
            val secDigitWidth = (secPanel.width() - innerGap * 3f) * .5f
            val secTop = secPanel.top + secPanel.height() * .13f
            val secBottom = secPanel.top + secPanel.height() * .68f
            val tens = RectF(secPanel.left + innerGap, secTop, secPanel.left + innerGap + secDigitWidth, secBottom)
            val ones = RectF(tens.right + innerGap, secTop, tens.right + innerGap + secDigitWidth, secBottom)
            drawDigit(canvas, seconds[0] - '0', tens, theme, theme.getAccentColor())
            drawDigit(canvas, seconds[1] - '0', ones, theme, theme.getAccentColor())
        } else {
            text(canvas, "--", secPanel.centerX(), centeredBaseline(secPanel.centerY(), secPanel.height() * .31f, mono),
                secPanel.height() * .31f, theme.getAccentColor(), Paint.Align.CENTER, mono)
        }
        text(canvas, "SEC", secPanel.centerX(), secPanel.top + secPanel.height() * .88f,
            readableSize(context, unit * .018f, 10f), theme.getSecondaryTextColor(), Paint.Align.CENTER, mono)

        var dateY = context.getTop() + h * if (landscape) .75f else .71f
        var contextY = context.getTop() + h * if (landscape) .83f else .79f
        var zoneY = context.getTop() + h * if (landscape) .89f else .845f
        val textLeft = context.getLeft() + w * .075f
        val textRight = context.getRight() - w * .075f
        val small = readableSize(context, unit * .018f, 10f)
        var metadataShift = bottomOverlayShift(context, zoneY)
        val minimumDateY = secPanel.bottom + max(small, unit * .012f)
        metadataShift = max(metadataShift, min(0f, minimumDateY - dateY))
        dateY += metadataShift; contextY += metadataShift; zoneY += metadataShift
        readableDate(canvas, context, state, textLeft, dateY, textRight - textLeft, unit * .030f,
            theme.getPrimaryTextColor(), Paint.Align.LEFT, mono, !landscape)
        readableText(canvas, context, contextText(state), textLeft, contextY, textRight - textLeft,
            unit * .025f * state.getSupportingScale(), 12f, theme.getAccentColor(), Paint.Align.LEFT, mono)
    }

    private fun drawDigit(canvas: Canvas, digit: Int, box: RectF, theme: ClockThemeTokens,
        activeColor: Int = theme.getPrimaryTextColor()) {
        val x = box.left; val y = box.top; val w = box.width(); val h = box.height()
        val pad = min(w, h) * .18f
        val left = x + pad; val right = x + w - pad; val top = y + pad
        val middle = y + h * .5f; val bottom = y + h - pad
        val width = max(2f, min(w, h) * .07f)
        val active = digits[digit.coerceIn(0, 9)]
        val off = alpha(theme.getLineColor(), 32)
        segment(canvas, left + width, top, right - width, top, active[0] != 0, activeColor, off, width)
        segment(canvas, right, top + width, right, middle - width, active[1] != 0, activeColor, off, width)
        segment(canvas, right, middle + width, right, bottom - width, active[2] != 0, activeColor, off, width)
        segment(canvas, left + width, bottom, right - width, bottom, active[3] != 0, activeColor, off, width)
        segment(canvas, left, middle + width, left, bottom - width, active[4] != 0, activeColor, off, width)
        segment(canvas, left, top + width, left, middle - width, active[5] != 0, activeColor, off, width)
        segment(canvas, left + width, middle, right - width, middle, active[6] != 0, activeColor, off, width)
    }

    private fun segment(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float,
        active: Boolean, on: Int, off: Int, width: Float) {
        val paint = stroke(if (active) on else off, width).apply { strokeCap = Paint.Cap.SQUARE }
        canvas.drawLine(x1, y1, x2, y2, paint)
    }
}

internal class TypographicRenderer : UltimateClockStyles.RendererBase() {
    override fun render(canvas: Canvas, context: ClockRenderContext, state: ClockState, theme: ClockThemeTokens) {
        background(canvas, context, theme)
        customBackgroundVeil(canvas, context, 0xE4EEF0EC.toInt())
        val w = context.getWidth()
        val h = context.getHeight()
        val unit = min(w, h)
        val display = displayTypeface(theme, Typeface.BOLD)
        val bold = supportingTypeface(theme, Typeface.BOLD)
        val regular = supportingTypeface(theme, Typeface.NORMAL)
        val c = state.newCalendar()
        var hour = c.get(Calendar.HOUR_OF_DAY)
        if (!state.isUse24Hour()) { hour %= 12; if (hour == 0) hour = 12 }
        val hours = String.format(Locale.US, "%02d", hour)
        val minutes = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE))
        val landscape = w >= h * 1.12f
        val small = readableSize(context, unit * .020f, 10f)
        val divider: Float
        if (landscape) {
            divider = context.getLeft() + w * .36f
            canvas.drawRect(context.getLeft(), context.getTop(), divider, context.getBottom(), fill(theme.getSurfaceColor()))
            canvas.drawRect(divider, context.getTop(), divider + unit * .008f, context.getBottom(), fill(theme.getAccentColor()))
            val hourCenterX = (context.getLeft() + divider) * .5f
            val minuteCenterX = divider + (context.getRight() - divider) * .50f
            val centerY = context.getTop() + h * .47f
            val hourSize = fitText(hours, (divider - context.getLeft()) * .78f, unit * .34f, display)
            val minuteSize = fitText(minutes, (context.getRight() - divider) * .72f, unit * .34f, display)
            text(canvas, hours, hourCenterX, centeredBaseline(centerY, hourSize, display), hourSize,
                0xFFF5F2EA.toInt(), Paint.Align.CENTER, display)
            text(canvas, minutes, minuteCenterX, centeredBaseline(centerY, minuteSize, display), minuteSize,
                theme.getPrimaryTextColor(), Paint.Align.CENTER, display)
        } else {
            divider = context.getTop() + h * .48f
            canvas.drawRect(context.getLeft(), context.getTop(), context.getRight(), divider, fill(theme.getSurfaceColor()))
            canvas.drawRect(context.getLeft(), divider, context.getRight(), divider + unit * .008f, fill(theme.getAccentColor()))
            val hourSize = fitText(hours, w * .76f, unit * .34f, display)
            val minuteSize = fitText(minutes, w * .76f, unit * .34f, display)
            val hourCenterY = context.getTop() + h * .27f
            val minuteCenterY = context.getTop() + h * .64f
            text(canvas, hours, context.getCenterX(), centeredBaseline(hourCenterY, hourSize, display), hourSize,
                0xFFF5F2EA.toInt(), Paint.Align.CENTER, display)
            text(canvas, minutes, context.getCenterX(), centeredBaseline(minuteCenterY, minuteSize, display), minuteSize,
                theme.getPrimaryTextColor(), Paint.Align.CENTER, display)
        }
        val titleX = context.getLeft() + w * .07f
        val titleY = context.getTop() + h * .105f
        text(canvas, "TYPE / 06", titleX, titleY, small, 0xCCF5F2EA.toInt(), Paint.Align.LEFT, bold)
        val seconds = if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
            String.format(Locale.US, "%02d", c.get(Calendar.SECOND))
        } else "--"
        val secondsX = context.getRight() - w * .07f
        val secondsY = context.getTop() + h * .16f
        text(canvas, seconds, secondsX, secondsY, unit * .065f, theme.getAccentColor(), Paint.Align.RIGHT, bold)
        val secondsLabelY = secondsY + max(small * 1.15f, unit * .045f)
        text(canvas, "SEC", secondsX, secondsLabelY, small,
            if (landscape) theme.getSecondaryTextColor() else 0xA8F5F2EA.toInt(), Paint.Align.RIGHT, regular)

        val dotX = if (landscape) divider else context.getRight() - w * .13f
        val dotCenterY = if (landscape) context.getTop() + h * .47f else divider
        val dotRadius = unit * .013f
        canvas.drawCircle(dotX, dotCenterY - unit * .050f, dotRadius, fill(theme.getAccentColor()))
        canvas.drawCircle(dotX, dotCenterY + unit * .050f, dotRadius, fill(theme.getAccentColor()))
        val barLeft = context.getLeft() + w * if (landscape) .43f else .10f
        val barRight = context.getRight() - w * .075f
        val barY = context.getTop() + h * if (landscape) .64f else .755f
        val secProgress = if (state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF) {
            secondValue(c, state) / 60f
        } else 0f
        canvas.drawLine(barLeft, barY, barRight, barY,
            stroke(alpha(theme.getLineColor(), 150), max(lineWidth(context, theme, 1f), unit * .004f)))
        canvas.drawLine(barLeft, barY, barLeft + (barRight - barLeft) * secProgress, barY,
            stroke(theme.getAccentColor(), max(lineWidth(context, theme, 1.4f), unit * .007f)))

        val infoLeft = context.getLeft() + w * if (landscape) .43f else .10f
        val infoRight = context.getRight() - w * if (landscape) .075f else .10f
        var dateY = context.getTop() + h * if (landscape) .75f else .825f
        var contextY = context.getTop() + h * if (landscape) .83f else .885f
        val metadataShift = bottomOverlayShift(context, contextY)
        dateY += metadataShift; contextY += metadataShift
        readableDate(canvas, context, state, infoLeft, dateY, infoRight - infoLeft, unit * .031f,
            theme.getPrimaryTextColor(), Paint.Align.LEFT, bold, !landscape)
        readableText(canvas, context, contextText(state), infoLeft, contextY, infoRight - infoLeft,
            unit * .024f * state.getSupportingScale(), 12f,
            theme.getSecondaryTextColor(), Paint.Align.LEFT, regular)
    }
}
