package com.clockmods.ultimate.clock

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.clockmods.sdk.clock.ClockBackground
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockThemeTokens
import com.clockmods.ui.ClockTimeText
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The five reference compositions retained from the original Ultimate gallery. */
internal abstract class MigratedRenderer : UltimateClockStyles.RendererBase() {
    private var glass: GaussianGlass? = null
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var density = 1f
    private var cardShadow = false

    protected abstract val mode: Int

    private fun panel(canvas: Canvas, bounds: RectF, rx: Float, ry: Float, color: Int) {
        val currentGlass = glass
        if (currentGlass != null) {
            currentGlass.roundRect(canvas, bounds, rx, ry, color, 0f, 0f, 0f)
            return
        }
        if (cardShadow) canvas.drawRoundRect(bounds, rx, ry, shadowPaint)
        canvas.drawRoundRect(bounds, rx, ry, fill(color))
    }

    private fun bubble(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        val currentGlass = glass
        if (currentGlass != null) {
            currentGlass.circle(canvas, x, y, radius, color)
            return
        }
        if (cardShadow) canvas.drawCircle(x, y, radius, shadowPaint)
        canvas.drawCircle(x, y, radius, fill(color))
    }

    private fun armShadow() {
        shadowPaint.setShadowLayer(density * 5f, 0f, density * 2f, 0x36000000)
        shadowPaint.color = 0xFF000000.toInt()
    }

    final override fun render(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        theme: ClockThemeTokens,
    ) {
        glass = GaussianGlass.create(context, theme)
        density = context.getDensity()
        cardShadow = theme.isCardShadow()
        armShadow()
        background(canvas, context, theme, glass == null)
        var colors = glass?.palette() ?: ClockPalette.fromTokens(theme)
        val hostBackground = context.getBackground()
        if (hostBackground?.getMode() == ClockBackground.Mode.COLOR) {
            colors = colors.withColor(0, hostBackground.getColor())
        }
        if (hostBackground?.isDimmed() == true && !hostBackground.hasImage()) {
            colors = colors.withColor(
                0,
                ClockPalette.mix(colors.background, 0xFF000000.toInt(), .4f),
            )
        }

        val display = displayTypeface(theme, Typeface.BOLD)
        val supporting = supportingTypeface(theme, Typeface.BOLD)
        val calendar = state.newCalendar()
        val time = timeText(calendar, state, false)
        var faceContext = context
        if (state.getWorldClocks().isNotEmpty()) {
            val strip = UltimateClockStyles.worldClockStripBounds(
                context.getLeft(), context.getTop(), context.getRight(), context.getBottom(),
                context.getDensity(), context.getBottomInset(),
            )
            val gap = min(context.getDensity() * 8f, context.getHeight() * .025f)
            val contentBottom = max(context.getTop(), strip.top - gap)
            faceContext = ClockRenderContext(
                context.getLeft(), context.getTop(), context.getRight(), contentBottom,
                context.getDensity(), context.getScaledDensity(), context.getFrameTimeMillis(),
                context.getBackground(), 0f, context.getWorldClockScroll(), false,
                context.getStatusOverlay(),
            )
        }

        val saveCount = canvas.save()
        when (mode) {
            MODE_DUAL -> drawDual(canvas, faceContext, state, calendar, display, supporting, colors)
            MODE_ORBIT -> drawOrbit(canvas, faceContext, state, calendar, time, display, supporting, colors)
            MODE_BUBBLES -> drawBubbles(canvas, faceContext, state, calendar, display, supporting, colors)
            MODE_BLEND -> drawBlend(canvas, faceContext, state, calendar, time, display, supporting, colors)
            else -> drawRibbon(canvas, faceContext, state, calendar, time, display, supporting, colors)
        }
        canvas.restoreToCount(saveCount)
        if (!context.isWorldClockStripHosted()) {
            drawWorldStrip(canvas, context, theme, state, supporting)
        }
    }

    private fun drawDual(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        calendar: Calendar,
        face: Typeface?,
        supporting: Typeface?,
        colors: ClockPalette,
    ) {
        val width = context.getWidth()
        val height = context.getHeight()
        val landscape = width >= height
        val marginX = width * if (landscape) .029f else .055f
        val marginY = height * .037f
        val gap = width * if (landscape) .022f else .035f
        val first: RectF
        val second: RectF
        if (landscape) {
            first = RectF(
                context.getLeft() + marginX, context.getTop() + marginY,
                context.getCenterX() - gap * .5f, context.getBottom() - marginY,
            )
            second = RectF(
                context.getCenterX() + gap * .5f, context.getTop() + marginY,
                context.getRight() - marginX, context.getBottom() - marginY,
            )
        } else {
            first = RectF(
                context.getLeft() + marginX, context.getTop() + marginY,
                context.getRight() - marginX, context.getCenterY() - gap * .5f,
            )
            second = RectF(
                context.getLeft() + marginX, context.getCenterY() + gap * .5f,
                context.getRight() - marginX, context.getBottom() - marginY,
            )
        }
        val radius = min(first.width(), first.height()) * .075f
        panel(canvas, first, radius, radius, colors.panel)
        panel(canvas, second, radius, radius, colors.accent)

        var hourValue = calendar.get(Calendar.HOUR_OF_DAY)
        if (!state.isUse24Hour()) {
            hourValue %= 12
            if (hourValue == 0) hourValue = 12
        }
        val hours = String.format(Locale.US, "%02d", hourValue)
        val minutes = String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE))
        var size = min(first.height() * .40f, first.width() * .59f) * state.getTimeScale()
        size = fitText(hours, first.width() * .76f, size, face)
        drawTime(
            canvas, hours, first.centerX(), centeredBaseline(first.centerY(), size, face),
            size, colors.onPanel, Paint.Align.CENTER, face,
        )
        size = fitText(minutes, second.width() * .76f, size, face)
        drawTime(
            canvas, minutes, second.centerX(), centeredBaseline(second.centerY(), size, face),
            size, colors.onAccent, Paint.Align.CENTER, face,
        )

        val unit = min(width, height)
        val labelSize = readableSize(context, unit * .027f * state.getSupportingScale(), 10f)
        text(
            canvas, if (isChinese(state)) "小时" else "HOUR",
            first.left + first.width() * .05f, first.bottom - first.height() * .052f,
            labelSize, colors.mutedPanel, Paint.Align.LEFT, supporting,
        )
        text(
            canvas, if (isChinese(state)) "分钟" else "MINUTE",
            second.right - second.width() * .05f, second.bottom - second.height() * .052f,
            labelSize, colors.onAccent, Paint.Align.RIGHT, supporting,
        )

        var dateBaseline = first.top + first.height() * .078f
        var dateSize = readableSize(context, unit * .034f * state.getDateScale(), 12f)
        val datePaint = fill(colors.onPanel).apply {
            typeface = supporting
            textSize = dateSize
        }
        val dateRows = UltimateClockStyles.dateLines(
            state.getDateText(), datePaint, first.width() * .74f, !landscape, state.getLocale(),
        )
        dateSize = max(
            readableSize(context, 0f, 12f),
            min(
                fitText(dateRows[0], first.width() * .74f, dateSize, supporting),
                if (dateRows[1].isEmpty()) dateSize
                else fitText(dateRows[1], first.width() * .74f, dateSize, supporting),
            ),
        )
        datePaint.textSize = dateSize
        val secondLine = if (!landscape &&
            UltimateClockStyles.splitDateAndLunar(state.getDateText())[1].isNotEmpty()
        ) dateSize * 1.45f else 0f
        val paddedBaseline = first.top + min(
            context.getDensity() * 20f,
            first.height() * .05f,
        ) - datePaint.fontMetrics.ascent + secondLine
        dateBaseline = if (landscape) max(dateBaseline, paddedBaseline) else paddedBaseline
        drawDate(
            canvas, context, state, first.left + first.width() * .05f, dateBaseline,
            first.width() * .74f, Paint.Align.LEFT, colors.onPanel, supporting, unit * .034f,
        )
        drawContext(
            canvas, context, state, second.right - second.width() * .05f,
            second.top + second.height() * .078f, second.width() * .62f,
            Paint.Align.RIGHT, colors.onAccent, supporting, unit * .028f,
        )
        if (secondsVisible(state)) {
            val markerSize = readableSize(context, unit * .034f * state.getSupportingScale(), 16f)
            val markerRadius = max(unit * .038f, markerSize * .88f)
            val markerX = second.left + second.width() * .10f
            val markerY = second.bottom - max(second.height() * .079f, markerRadius * 1.25f)
            drawScallopedCircle(canvas, markerX, markerY, markerRadius, colors.badge)
            text(
                canvas, String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND)),
                markerX, centeredBaseline(markerY, markerSize, supporting), markerSize,
                colors.onBadge, Paint.Align.CENTER, supporting,
            )
        }
    }

    private fun drawOrbit(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        calendar: Calendar,
        time: String,
        face: Typeface?,
        supporting: Typeface?,
        colors: ClockPalette,
    ) {
        val centerX = context.getCenterX()
        val height = context.getHeight()
        val width = context.getWidth()
        val centerY = context.getTop() + height * .522f
        val outer = min(height * .382f, width * .32f)
        ring(canvas, centerX, centerY, outer, colors.ring(.20f), max(2f, height * .0055f))
        ring(canvas, centerX, centerY, outer * .755f, colors.ring(.15f), max(2f, height * .004f))
        ring(canvas, centerX, centerY, outer * .515f, colors.ring(.10f), max(1f, height * .0026f))
        if (secondsVisible(state)) {
            val radians = Math.toRadians(secondAngle(calendar, state).toDouble())
            bubble(
                canvas,
                centerX + cos(radians).toFloat() * outer,
                centerY + sin(radians).toFloat() * outer,
                max(context.getDensity() * 4f, height * .0105f),
                colors.accent,
            )
        }
        val dialSize = fitText(time, outer * 1.90f, height * .278f * state.getTimeScale(), face)
        withPhotoText(glass != null) {
            drawTime(
                canvas, time, centerX, centeredBaseline(centerY, dialSize, face), dialSize,
                colors.onBackground, Paint.Align.CENTER, face,
            )
        }
        drawDate(
            canvas, context, state, context.getRight() - width * .029f,
            context.getTop() + height * .072f, width * .46f, Paint.Align.RIGHT,
            colors.onBackground, supporting, min(width, height) * .032f,
        )
        drawContext(
            canvas, context, state, context.getLeft() + width * .029f,
            context.getTop() + height * .072f, width * .40f, Paint.Align.LEFT,
            colors.mutedBackground, supporting, min(width, height) * .027f,
        )
        if (secondsVisible(state)) {
            val bubbleRadius = height * .040f
            val y = centerY + outer
            drawScallopedCircle(canvas, centerX, y, bubbleRadius, colors.accent)
            val bubbleSize = min(
                bubbleRadius * 1.15f,
                readableSize(context, bubbleRadius * .95f, 16f),
            )
            val seconds = String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND))
            val secondsPaint = fill(colors.onAccent).apply {
                typeface = supporting
                textSize = bubbleSize
                textAlign = Paint.Align.CENTER
            }
            val glyphBounds = Rect()
            secondsPaint.getTextBounds(seconds, 0, seconds.length, glyphBounds)
            canvas.drawText(
                seconds, centerX,
                y - (glyphBounds.top + glyphBounds.bottom) * .5f,
                secondsPaint,
            )
        }
    }

    private fun drawBubbles(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        calendar: Calendar,
        face: Typeface?,
        supporting: Typeface?,
        colors: ClockPalette,
    ) {
        val width = context.getWidth()
        val height = context.getHeight()
        val landscape = width >= height
        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (!state.isUse24Hour()) {
            hour %= 12
            if (hour == 0) hour = 12
        }
        val hours = String.format(Locale.US, "%02d", hour)
        val minutes = String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE))
        if (!landscape) {
            drawBubblesPortrait(
                canvas, context, state, calendar, face, supporting, hours, minutes, colors,
            )
            return
        }

        val hourPanel = RectF(
            context.getLeft() + width * .029f, context.getTop() + height * .218f,
            context.getLeft() + width * .374f, context.getTop() + height * .829f,
        )
        panel(canvas, hourPanel, height * .050f, height * .050f, colors.panel)
        val minuteX = context.getLeft() + width * .585f
        val minuteY = context.getTop() + height * .516f
        val minuteRadius = height * .315f
        drawScallopedCircle(canvas, minuteX, minuteY, minuteRadius, colors.accent)
        val secondX = context.getLeft() + width * .884f
        val secondY = context.getTop() + height * .522f
        val secondRadius = height * .143f
        bubble(canvas, secondX, secondY, secondRadius, colors.panelAlt)

        val preferred = height * .235f * state.getTimeScale()
        val hourSize = fitText(hours, hourPanel.width() * .68f, preferred, face)
        val minuteSize = fitText(minutes, minuteRadius * 1.25f, preferred, face)
        drawTime(
            canvas, hours, hourPanel.centerX(), centeredBaseline(hourPanel.centerY(), hourSize, face),
            hourSize, colors.onPanel, Paint.Align.CENTER, face,
        )
        drawTime(
            canvas, minutes, minuteX, centeredBaseline(minuteY, minuteSize, face),
            minuteSize, colors.onAccent, Paint.Align.CENTER, face,
        )
        if (secondsVisible(state)) {
            val secondSize = fitText(
                "00", secondRadius * 1.15f,
                height * .075f * state.getSupportingScale(), supporting,
            )
            text(
                canvas, String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND)),
                secondX, centeredBaseline(secondY, secondSize, supporting), secondSize,
                colors.onPanelAlt, Paint.Align.CENTER, supporting,
            )
        }

        val unit = min(width, height)
        val labelSize = readableSize(context, unit * .027f * state.getSupportingScale(), 10f)
        text(
            canvas, if (isChinese(state)) "小时" else "HOUR",
            hourPanel.left + hourPanel.width() * .045f,
            hourPanel.bottom - hourPanel.height() * .06f,
            labelSize, colors.mutedPanel, Paint.Align.LEFT, supporting,
        )
        text(
            canvas, if (isChinese(state)) "分钟" else "MINUTE", minuteX,
            minuteY + minuteRadius * .82f, labelSize, colors.onAccent,
            Paint.Align.CENTER, supporting,
        )
        drawDate(
            canvas, context, state, context.getLeft() + width * .029f,
            context.getTop() + height * .072f, width * .55f, Paint.Align.LEFT,
            colors.mutedBackground, supporting, unit * .032f,
        )
        drawContext(
            canvas, context, state, context.getRight() - width * .029f,
            context.getTop() + height * .072f, width * .35f, Paint.Align.RIGHT,
            colors.mutedBackground, supporting, unit * .027f,
        )
    }

    private fun drawBubblesPortrait(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        calendar: Calendar,
        face: Typeface?,
        supporting: Typeface?,
        hours: String,
        minutes: String,
        colors: ClockPalette,
    ) {
        val width = context.getWidth()
        val height = context.getHeight()
        drawDate(
            canvas, context, state, context.getLeft() + width * .06f,
            context.getTop() + height * .055f, width * .76f, Paint.Align.LEFT,
            colors.mutedBackground, supporting, width * .040f,
        )
        val geometry = UltimateClockStyles.bubblesPortraitGeometry(
            width, height, context.getDensity(),
        )
        val hourPanel = RectF(
            context.getLeft() + width * .09f, context.getTop() + geometry[0],
            context.getRight() - width * .09f, context.getTop() + geometry[1],
        )
        val hourCorner = min(hourPanel.width(), hourPanel.height()) * .11f
        panel(canvas, hourPanel, hourCorner, hourCorner, colors.panel)

        val minuteX = context.getLeft() + width * .42f
        val minuteY = context.getTop() + geometry[2]
        val minuteRadius = geometry[3]
        drawScallopedCircle(canvas, minuteX, minuteY, minuteRadius, colors.accent)

        val hourPreferred = min(hourPanel.height() * .44f, width * .24f) * state.getTimeScale()
        val hourSize = fitText(hours, hourPanel.width() * .72f, hourPreferred, face)
        val minutePreferred = min(minuteRadius * .72f, width * .24f) * state.getTimeScale()
        val minuteSize = fitText(minutes, minuteRadius * 1.30f, minutePreferred, face)
        drawTime(
            canvas, hours, hourPanel.centerX(), centeredBaseline(hourPanel.centerY(), hourSize, face),
            hourSize, colors.onPanel, Paint.Align.CENTER, face,
        )
        drawTime(
            canvas, minutes, minuteX, centeredBaseline(minuteY, minuteSize, face),
            minuteSize, colors.onAccent, Paint.Align.CENTER, face,
        )

        val labelSize = readableSize(
            context, min(width, height) * .027f * state.getSupportingScale(), 10f,
        )
        text(
            canvas, if (isChinese(state)) "小时" else "HOUR",
            hourPanel.left + hourPanel.width() * .06f,
            hourPanel.bottom - hourPanel.height() * .055f, labelSize,
            colors.mutedPanel, Paint.Align.LEFT, supporting,
        )
        text(
            canvas, if (isChinese(state)) "分钟" else "MINUTE", minuteX,
            minuteY + minuteRadius * .79f, labelSize, colors.onAccent,
            Paint.Align.CENTER, supporting,
        )
        if (secondsVisible(state)) {
            val secondRadius = min(width * .09f, minuteRadius * .27f)
            val desiredX = minuteX + minuteRadius + secondRadius + geometry[4] * .40f
            val secondX = min(context.getRight() - width * .03f - secondRadius, desiredX)
            val secondY = minuteY + minuteRadius * .32f
            bubble(canvas, secondX, secondY, secondRadius, colors.panelAlt)
            val secondSize = fitText("00", secondRadius * 1.28f, secondRadius * .76f, supporting)
            text(
                canvas, String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND)),
                secondX, centeredBaseline(secondY, secondSize, supporting), secondSize,
                colors.onPanelAlt, Paint.Align.CENTER, supporting,
            )
        }
        drawContext(
            canvas, context, state, context.getLeft() + width * .06f,
            context.getTop() + height * .965f, width * .88f, Paint.Align.LEFT,
            colors.mutedBackground, supporting, width * .034f,
        )
    }

    private fun drawBlend(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        calendar: Calendar,
        time: String,
        face: Typeface?,
        supporting: Typeface?,
        colors: ClockPalette,
    ) {
        val width = context.getWidth()
        val height = context.getHeight()
        val landscape = width >= height
        val analog: RectF
        val digital: RectF
        if (landscape) {
            analog = RectF(
                context.getLeft() + width * .030f, context.getTop() + height * .046f,
                context.getLeft() + width * .527f, context.getBottom() - height * .034f,
            )
            digital = RectF(
                context.getLeft() + width * .550f, context.getTop() + height * .046f,
                context.getRight() - width * .027f, context.getBottom() - height * .034f,
            )
        } else {
            analog = RectF(
                context.getLeft() + width * .05f, context.getTop() + height * .035f,
                context.getRight() - width * .05f, context.getTop() + height * .55f,
            )
            digital = RectF(
                context.getLeft() + width * .05f, context.getTop() + height * .575f,
                context.getRight() - width * .05f, context.getBottom() - height * .035f,
            )
        }
        val radius = min(analog.width(), analog.height()) * .06f
        panel(canvas, analog, radius, radius, colors.panel)
        panel(canvas, digital, radius, radius, colors.accent)
        drawAnalog(canvas, context, state, calendar, analog, colors)

        val timeSize = fitText(
            time, digital.width() * .88f,
            min(digital.height() * .30f, digital.width() * .29f) * state.getTimeScale(), face,
        )
        drawTime(
            canvas, time, digital.centerX(),
            centeredBaseline(digital.centerY() - digital.height() * .03f, timeSize, face),
            timeSize, colors.onAccent, Paint.Align.CENTER, face,
        )
        val unit = min(width, height)
        drawDateAndLunar(
            canvas, context, state, digital.left + digital.width() * .045f,
            digital.bottom - digital.height() * .050f, digital.width() * .72f,
            Paint.Align.LEFT, colors.mutedAccent, supporting, unit * .032f,
        )
        drawContext(
            canvas, context, state, digital.right - digital.width() * .045f,
            digital.top + digital.height() * .067f, digital.width() * .48f,
            Paint.Align.RIGHT, colors.mutedAccent, supporting, unit * .027f,
        )
        if (secondsVisible(state)) {
            val secondSize = readableSize(
                context, unit * .034f * state.getSupportingScale(), 16f,
            )
            val secondRadius = max(unit * .038f, secondSize * .88f)
            val secondX = digital.right - digital.width() * .09f
            val secondY = digital.bottom - digital.height() * .050f - secondRadius
            drawScallopedCircle(canvas, secondX, secondY, secondRadius, colors.badge)
            text(
                canvas, String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND)),
                secondX, centeredBaseline(secondY, secondSize, supporting), secondSize,
                colors.onBadge, Paint.Align.CENTER, supporting,
            )
        }
    }

    private fun drawAnalog(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        calendar: Calendar,
        panel: RectF,
        colors: ClockPalette,
    ) {
        val radius = min(panel.width() * .46f, panel.height() * .44f)
        val centerX = panel.centerX()
        val centerY = panel.centerY()
        bubble(canvas, centerX, centerY, radius, colors.panelAlt)
        for (index in 0 until 60) {
            val major = index % 5 == 0
            val angle = Math.toRadians(index * 6.0 - 90.0)
            val outside = radius * .93f
            val inside = radius * if (major) .80f else .86f
            val tickPaint = stroke(
                colors.onPanelAlt,
                max(
                    context.getDensity() * if (major) 3f else 1f,
                    radius * if (major) .012f else .005f,
                ),
            ).apply { strokeCap = Paint.Cap.ROUND }
            canvas.drawLine(
                centerX + cos(angle).toFloat() * inside,
                centerY + sin(angle).toFloat() * inside,
                centerX + cos(angle).toFloat() * outside,
                centerY + sin(angle).toFloat() * outside,
                tickPaint,
            )
        }
        cleanHand(
            canvas, centerX, centerY, hourAngle(calendar, state), radius * .50f,
            radius * .055f, colors.onPanelAlt, radius * .03f, Paint.Cap.ROUND,
        )
        cleanHand(
            canvas, centerX, centerY, minuteAngle(calendar, state), radius * .69f,
            radius * .039f, colors.onPanelAlt, radius * .04f, Paint.Cap.ROUND,
        )
        if (secondsVisible(state)) {
            cleanHand(
                canvas, centerX, centerY, secondAngle(calendar, state), radius * .77f,
                max(context.getDensity() * 1.5f, radius * .010f), colors.hand(),
                radius * .04f, Paint.Cap.ROUND,
            )
        }
        canvas.drawCircle(centerX, centerY, radius * .070f, fill(colors.hand()))
    }

    private fun drawRibbon(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        calendar: Calendar,
        time: String,
        face: Typeface?,
        supporting: Typeface?,
        colors: ClockPalette,
    ) {
        val width = context.getWidth()
        val height = context.getHeight()
        val portrait = height > width
        val outer: RectF
        val ribbon: RectF
        if (portrait) {
            val geometry = UltimateClockStyles.ribbonPortraitGeometry(width, height)
            val centerY = context.getTop() + geometry[0]
            outer = RectF(
                context.getLeft() + width * .050f, centerY - geometry[1] * .5f,
                context.getRight() - width * .040f, centerY + geometry[1] * .5f,
            )
            ribbon = RectF(
                context.getLeft() + width * .078f, centerY - geometry[2] * .5f,
                context.getRight() - width * .069f, centerY + geometry[2] * .5f,
            )
        } else {
            outer = RectF(
                context.getLeft() + width * .050f, context.getTop() + height * .205f,
                context.getRight() - width * .040f, context.getTop() + height * .850f,
            )
            ribbon = RectF(
                context.getLeft() + width * .078f, context.getTop() + height * .265f,
                context.getRight() - width * .069f, context.getTop() + height * .800f,
            )
        }

        val saveCount = canvas.save()
        canvas.rotate(-2f, outer.centerX(), outer.centerY())
        val currentGlass = glass
        if (currentGlass == null) {
            panel(canvas, outer, outer.height() * .085f, outer.height() * .085f, colors.panel)
        } else {
            currentGlass.roundRect(
                canvas, outer, outer.height() * .085f, outer.height() * .085f,
                colors.panel, 0f, 0f, -2f,
            )
        }
        canvas.restoreToCount(saveCount)
        panel(canvas, ribbon, ribbon.height() * .5f, ribbon.height() * .5f, colors.accent)

        val timeX = ribbon.left + ribbon.width() * .055f
        var timeSize = fitText(
            time, ribbon.width() * if (secondsVisible(state)) .58f else .86f,
            min(ribbon.height() * .56f, width * .30f) * state.getTimeScale(), face,
        )
        var secondsSize = UltimateClockStyles.ribbonSecondsTextSize(timeSize)
        var secondsRadius = max(secondsSize * .76f, min(width, height) * .042f)
        var secondsX = ribbon.right - ribbon.width() * .055f - secondsRadius
        if (secondsVisible(state)) {
            val timeMaxWidth = max(
                1f,
                secondsX - secondsRadius - ribbon.width() * .035f - timeX,
            )
            timeSize = fitText(time, timeMaxWidth, timeSize, face)
            secondsSize = UltimateClockStyles.ribbonSecondsTextSize(timeSize)
            secondsRadius = max(secondsSize * .76f, min(width, height) * .042f)
            secondsX = ribbon.right - ribbon.width() * .055f - secondsRadius
        }
        drawTime(
            canvas, time, timeX, centeredBaseline(ribbon.centerY(), timeSize, face),
            timeSize, colors.onAccent, Paint.Align.LEFT, face,
        )
        if (secondsVisible(state)) {
            val secondY = ribbon.centerY()
            drawScallopedCircle(canvas, secondsX, secondY, secondsRadius, colors.badge)
            text(
                canvas, String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND)),
                secondsX, centeredBaseline(secondY, secondsSize, supporting), secondsSize,
                colors.onBadge, Paint.Align.CENTER, supporting,
            )
        }

        val topRowY = context.getTop() + height * if (portrait) .055f else .079f
        drawDate(
            canvas, context, state, context.getLeft() + width * .038f, topRowY,
            width * .55f, Paint.Align.LEFT, colors.onBackground, supporting,
            min(width, height) * .032f,
        )
        drawContext(
            canvas, context, state, context.getRight() - width * .029f, topRowY,
            width * .35f, Paint.Align.RIGHT, colors.mutedBackground, supporting,
            min(width, height) * .027f,
        )
    }

    private fun drawDate(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        x: Float,
        baseline: Float,
        maxWidth: Float,
        align: Paint.Align,
        color: Int,
        face: Typeface?,
        size: Float,
    ) {
        withPhotoText(glass != null && mode in PHOTO_TEXT_MODES) {
            readableDate(
                canvas, context, state, x,
                clearOfStatusOverlay(
                    context, x, align, baseline, maxWidth,
                    size * max(1f, state.getDateScale()),
                ),
                maxWidth, size, color, align, face,
                context.getHeight() > context.getWidth(),
            )
        }
    }

    private fun drawDateAndLunar(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        x: Float,
        lowerBaseline: Float,
        maxWidth: Float,
        align: Paint.Align,
        color: Int,
        face: Typeface?,
        size: Float,
    ) {
        readableDate(
            canvas, context, state, x, lowerBaseline, maxWidth, size,
            color, align, face, true,
        )
    }

    private fun drawContext(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        x: Float,
        baseline: Float,
        maxWidth: Float,
        align: Paint.Align,
        color: Int,
        face: Typeface?,
        size: Float,
    ) {
        val value = contextText(state)
        if (value.isEmpty()) return
        withPhotoText(glass != null && mode in PHOTO_TEXT_MODES) {
            readableText(
                canvas, context, value, x,
                clearOfStatusOverlay(
                    context, x, align, baseline, maxWidth,
                    size * max(1f, state.getSupportingScale()),
                ),
                maxWidth, size * state.getSupportingScale(), 12f,
                color, align, face,
            )
        }
    }

    private inline fun withPhotoText(enabled: Boolean, block: () -> Unit) {
        val paintPool = PAINT_POOL.get()
        val previous = paintPool.photoText
        paintPool.photoText = enabled
        try {
            block()
        } finally {
            paintPool.photoText = previous
        }
    }

    private fun drawScallopedCircle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        color: Int,
    ) {
        val path = Path()
        val points = 96
        for (index in 0..points) {
            val angle = Math.PI * 2.0 * index / points - Math.PI / 2.0
            val wave = 1f + .055f * cos(angle * 12.0).toFloat()
            val x = centerX + cos(angle).toFloat() * radius * wave
            val y = centerY + sin(angle).toFloat() * radius * wave
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val currentGlass = glass
        if (currentGlass == null) canvas.drawPath(path, fill(color))
        else currentGlass.path(canvas, path, color)
    }

    private fun drawWorldStrip(
        canvas: Canvas,
        context: ClockRenderContext,
        theme: ClockThemeTokens,
        state: ClockState,
        supporting: Typeface?,
    ) {
        val entries = state.getWorldClocks()
        if (entries.isEmpty()) return
        val width = context.getWidth()
        val height = context.getHeight()
        val strip = UltimateClockStyles.worldClockStripBounds(
            context.getLeft(), context.getTop(), context.getRight(), context.getBottom(),
            context.getDensity(), context.getBottomInset(),
        )
        val styleIds = arrayOf(
            UltimateClockStyles.STYLE_DUAL_BLOCKS,
            UltimateClockStyles.STYLE_ORBIT,
            UltimateClockStyles.STYLE_BUBBLES,
            UltimateClockStyles.STYLE_BLEND,
            UltimateClockStyles.STYLE_RIBBON,
        )
        val faceHeight = strip.top - context.getTop() -
            min(context.getDensity() * 8f, height * .025f)
        val inset = UltimateClockStyles.worldClockContentInset(
            styleIds[mode], width, faceHeight, context.getDensity(),
        )
        val total = UltimateClockStyles.worldClockContentWidth(
            entries.size, width, height, context.getDensity(),
        ) + inset * 2f
        val scroll = min(context.getWorldClockScroll(), max(0f, total - strip.width()))
        val saveCount = canvas.save()
        canvas.clipRect(strip.left, strip.top, strip.right, strip.bottom)
        canvas.translate(strip.left + inset - scroll, strip.top)
        drawWorldCards(
            canvas, context, theme, state, supporting, strip.height(),
            strip.left + inset - scroll, strip.top,
        )
        canvas.restoreToCount(saveCount)
    }

    internal fun drawWorldCards(
        canvas: Canvas,
        context: ClockRenderContext,
        theme: ClockThemeTokens,
        state: ClockState,
        supporting: Typeface?,
        height: Float,
        originX: Float,
        originY: Float,
    ) {
        val worldGlass = GaussianGlass.create(context, theme)
        var colors = worldGlass?.palette() ?: ClockPalette.fromTokens(theme)
        val unit = min(context.getWidth(), context.getHeight())
        val cardWidth = UltimateClockStyles.worldClockCardWidth(
            context.getWidth(), context.getHeight(), context.getDensity(),
        )
        val gap = UltimateClockStyles.worldClockCardGap(
            context.getWidth(), context.getHeight(), context.getDensity(),
        )
        val saveCount = canvas.save()
        val timeFace = displayTypeface(theme, Typeface.BOLD)
        state.getWorldClocks().forEachIndexed { index, entry ->
            val cardOriginX = originX + index * (cardWidth + gap)
            val card = RectF(0f, 0f, cardWidth, height)
            if (canvas.quickReject(card)) {
                canvas.translate(cardWidth + gap, 0f)
                return@forEachIndexed
            }
            val radius = min(context.getDensity() * 20f, card.height() * .18f)
            if (worldGlass != null) {
                colors = worldGlass.paletteAt(cardOriginX, originY, cardWidth, height)
                worldGlass.roundRect(
                    canvas, card, radius, radius, colors.panel,
                    cardOriginX, originY, 0f,
                )
            } else {
                canvas.drawRoundRect(card, radius, radius, fill(colors.panel))
            }

            val padding = cardWidth * .09f
            val contentWidth = cardWidth - padding * 2f
            val supportScale = state.getSupportingScale()
            var citySize = readableSize(context, unit * .030f * supportScale, 12f)
            citySize = min(citySize, card.height() * .18f)
            val headerY = card.top + card.height() * .23f
            val flagWidth = citySize * 1.7f
            text(
                canvas, entry.getFlagEmoji(), padding,
                centeredBaseline(headerY, citySize, supporting), citySize,
                colors.onPanel, Paint.Align.LEFT, supporting,
            )
            text(
                canvas,
                ellipsize(
                    WorldClockCatalog.displayCity(
                        entry, WorldClockCatalog.languageOf(state.getLocale()),
                    ),
                    contentWidth - flagWidth, citySize, supporting,
                ),
                padding + flagWidth, centeredBaseline(headerY, citySize, supporting), citySize,
                colors.onPanel, Paint.Align.LEFT, supporting,
            )

            val local = Calendar.getInstance(
                TimeZone.getTimeZone(entry.getZoneId()), state.getLocale(),
            ).apply { timeInMillis = state.getTimeMillis() }
            val localTime = timeText(local, state, false)
            val timeSize = fitText(
                localTime, contentWidth,
                min(
                    card.height() * .30f,
                    readableSize(context, unit * .062f * supportScale, 22f),
                ),
                timeFace,
            )
            val timePaint = fill(colors.onPanel).apply {
                typeface = timeFace
                textSize = timeSize
                textAlign = Paint.Align.LEFT
            }
            ClockTimeText.draw(
                canvas, localTime, padding,
                centeredBaseline(card.top + card.height() * .54f, timeSize, timeFace),
                timePaint,
            )
            val zoneSize = min(
                card.height() * .14f,
                readableSize(context, unit * .020f * supportScale, 10f),
            )
            text(
                canvas, ellipsize(entry.getZoneId(), contentWidth, zoneSize, supporting),
                padding, centeredBaseline(card.top + card.height() * .81f, zoneSize, supporting),
                zoneSize, colors.mutedPanel, Paint.Align.LEFT, supporting,
            )
            canvas.translate(cardWidth + gap, 0f)
        }
        canvas.restoreToCount(saveCount)
    }

    companion object {
        private const val MODE_DUAL = 0
        private const val MODE_ORBIT = 1
        private const val MODE_BUBBLES = 2
        private const val MODE_BLEND = 3
        private const val MODE_RIBBON = 4
        private val PHOTO_TEXT_MODES = setOf(MODE_ORBIT, MODE_BUBBLES, MODE_RIBBON)
        private val worldCardsRenderer = object : MigratedRenderer() {
            override val mode = MODE_DUAL
        }

        fun drawWorldClockCards(
            canvas: Canvas,
            context: ClockRenderContext,
            state: ClockState,
            theme: ClockThemeTokens,
            height: Float,
            originX: Float,
            originY: Float,
        ) {
            val marker = beginPaintFrame()
            try {
                worldCardsRenderer.drawWorldCards(
                    canvas, context, theme, state,
                    supportingTypefaceFor(theme, Typeface.BOLD),
                    height, originX, originY,
                )
            } finally {
                endPaintFrame(marker)
            }
        }

        private fun clearOfStatusOverlay(
            context: ClockRenderContext,
            anchorX: Float,
            align: Paint.Align,
            baseline: Float,
            maxWidth: Float,
            drawnSize: Float,
        ): Float {
            val overlay = context.getStatusOverlay() ?: return baseline
            val size = max(0f, drawnSize)
            val span = max(0f, maxWidth)
            val rowLeft = when (align) {
                Paint.Align.RIGHT -> anchorX - span
                Paint.Align.CENTER -> anchorX - span * .5f
                else -> anchorX
            }
            val ascent = size * 1.15f
            if (!overlay.spansHorizontally(rowLeft, rowLeft + span) ||
                !overlay.spansVertically(baseline - ascent, baseline + size * .25f)
            ) return baseline
            return overlay.getBottom() + max(context.getDensity() * 4f, size * .25f) + ascent
        }

        private fun secondsVisible(state: ClockState): Boolean =
            state.isShowSeconds() && state.getSecondHandMotion() != ClockState.SecondHandMotion.OFF

        private fun isChinese(state: ClockState): Boolean =
            state.getLocale().language == Locale.CHINESE.language
    }
}

internal class DualBlocksRenderer : MigratedRenderer() {
    override val mode = 0
}

internal class OrbitRenderer : MigratedRenderer() {
    override val mode = 1
}

internal class BubblesRenderer : MigratedRenderer() {
    override val mode = 2
}

internal class BlendRenderer : MigratedRenderer() {
    override val mode = 3
}

internal class RibbonRenderer : MigratedRenderer() {
    override val mode = 4
}
