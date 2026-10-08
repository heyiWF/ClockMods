package com.clockmods.ultimate.compose

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import com.clockmods.ui.AlmanacBadge
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.pro.CalendarDashboardSizing
import com.clockmods.ui.ClockTypefaceResolver
import kotlin.math.max
import kotlin.math.min

internal const val ALMANAC_BADGE_BACKGROUND_ALPHA = AlmanacBadge.BACKGROUND_ALPHA

/** Centers the visible ink, including fonts whose Chinese glyphs sit off their line box center. */
internal fun drawCenteredAlmanacGlyph(
    canvas: android.graphics.Canvas, glyph: String, centerX: Float, centerY: Float, paint: Paint,
) {
    val bounds = Rect()
    paint.getTextBounds(glyph, 0, glyph.length, bounds)
    canvas.drawText(glyph, centerX - bounds.exactCenterX(), centerY - bounds.exactCenterY(), paint)
}

/** Original footer timing: read, scroll to the end, pause, then slide to the next coloured line. */
@Composable
internal fun CalendarFooterCarousel(cell: CalendarCellInfo, date: String, theme: ComposeCalendarTheme,
    typography: CalendarTypography, height: Float, modifier: Modifier) {
    val context = LocalContext.current
    val good = stringResource(R.string.calendar_suitable_prefix)
    val bad = stringResource(R.string.calendar_avoid_prefix)
    val lines = listOf(Triple("", date, theme.text)) +
        (if (cell.suitable.isNotEmpty()) listOf(Triple(good, cell.suitable.joinToString(" · "), theme.suitable)) else emptyList()) +
        (if (cell.avoid.isNotEmpty()) listOf(Triple(bad, cell.avoid.joinToString(" · "), theme.avoid)) else emptyList())
    val paint = remember(typography) { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.weight) } }
    val bold = remember(typography) { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.emphasizedWeight) } }
    val badgePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val glyphBounds = remember { Rect() }
    val density = LocalDensity.current.density
    val marquee = LocalCalendarMarquee.current
    val active = LocalCalendarMotionActive.current
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    // Measure once per content/font/viewport change, not on every animation frame.
    val metrics = remember(lines, typography, height, density, viewport, marquee) {
        paint.textSize = min(
            CalendarDashboardSizing.monthFooterSize(height * density, density) *
                typography.dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE,
            viewport.height * .78f,
        )
        paint.textSize *= min(1f,
            viewport.height * .55f / (paint.descent() - paint.ascent()).coerceAtLeast(1f))
        val lineHeight = paint.descent() - paint.ascent()
        val widths = lines.map { paint.measureText(it.second) }
        val overflows = lines.mapIndexed { i, line ->
            max(0f, widths[i] - max(1f, viewport.width - 16f * density -
                if (line.first.isEmpty()) 0f else lineHeight * 1.24f))
        }
        FooterMetrics(paint.textSize, lineHeight, widths, overflows,
            overflows.map { marquee.holdMillis(it, density) })
    }
    var elapsed by remember(metrics) { mutableLongStateOf(0L) }
    LaunchedEffect(metrics, marquee, active) {
        elapsed = 0L
        if (!active || viewport.width == 0) return@LaunchedEffect
        val start = android.os.SystemClock.uptimeMillis()
        while (true) {
            val (index, phase) = footerPhase(elapsed, metrics.holds)
            val remaining = metrics.holds[index] - phase
            val wait = if (remaining > 0L) min(remaining,
                marquee.scrollFrameDelay(phase, metrics.overflows[index], density)) else 16L
            if (wait > 16L) delay(wait) else withFrameNanos { }
            elapsed = android.os.SystemClock.uptimeMillis() - start
        }
    }
    Canvas(modifier.onSizeChanged { viewport = it }
        .semantics { contentDescription = lines.joinToString("，") { it.first + it.second } }) {
        paint.textSize = metrics.textSize
        bold.textSize = paint.textSize
        val padding = 8 * density
        val lineHeight = metrics.lineHeight
        val badgeDiameter = lineHeight * .94f
        val badgeGap = lineHeight * .30f
        fun prefixWidth(prefix: String) = if (prefix.isEmpty()) 0f else badgeDiameter + badgeGap
        val (index, phase) = footerPhase(elapsed, metrics.holds)
        val hold = metrics.holds[index]
        val travel = if (phase <= hold) 0f else (phase - hold) / 200f * lineHeight
        val canvas = drawContext.canvas.nativeCanvas
        fun drawLine(i: Int, offset: Float, time: Long) {
            val (prefix, body, color) = lines[i]
            paint.color = color; bold.color = color
            val prefixWidth = prefixWidth(prefix)
            val width = metrics.widths[i]
            val excess = metrics.overflows[i]
            val baseline = size.height / 2 - (paint.ascent() + paint.descent()) / 2 + offset
            val top = max(0f, (size.height - lineHeight) / 2)
            val bottom = min(size.height, (size.height + lineHeight) / 2)
            val x = if (excess == 0f) (size.width - prefixWidth - width) / 2 else padding
            canvas.save()
            canvas.clipRect(0f, top, size.width, bottom)
            if (prefix.isNotEmpty()) {
                val radius = badgeDiameter / 2f
                val centerY = size.height / 2f + offset
                bold.textSize = paint.textSize * .74f
                AlmanacBadge.draw(canvas, prefix, x + radius, centerY, badgeDiameter,
                    color, bold, badgePaint, glyphBounds)
            }
            canvas.save()
            canvas.clipRect(x + prefixWidth, top, size.width - if (excess > 0) padding else 0f, bottom)
            val scroll = marquee.scrollOffset(time, excess, density)
            canvas.drawText(body, x + prefixWidth - scroll, baseline, paint)
            canvas.restore()
            canvas.restore()
        }
        drawLine(index, -travel, phase)
        if (travel > 0) drawLine((index + 1) % lines.size, lineHeight - travel, 0)
    }
}

private data class FooterMetrics(val textSize: Float, val lineHeight: Float,
    val widths: List<Float>, val overflows: List<Float>, val holds: List<Long>)

internal fun footerPhase(elapsed: Long, holds: List<Long>): Pair<Int, Long> {
    require(holds.isNotEmpty())
    var phase = elapsed.coerceAtLeast(0L) % holds.sumOf { it + 200L }
    var index = 0
    while (phase >= holds[index] + 200L) { phase -= holds[index] + 200L; index++ }
    return index to phase
}
