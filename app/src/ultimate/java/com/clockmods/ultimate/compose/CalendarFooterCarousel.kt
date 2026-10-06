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
import com.clockmods.ui.CalendarMarqueeTiming
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
    var elapsed by remember(cell.day) { mutableLongStateOf(0L) }
    LaunchedEffect(cell.day) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { elapsed = (it - start) / 1_000_000 }
    }
    Canvas(modifier.semantics { contentDescription = lines.joinToString("，") { it.first + it.second } }) {
        paint.textSize = min(
            CalendarDashboardSizing.monthFooterSize(height * density, density) *
                typography.dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE,
            size.height * .78f,
        )
        paint.textSize *= min(1f,
            size.height * .55f / (paint.descent() - paint.ascent()).coerceAtLeast(1f))
        bold.textSize = paint.textSize
        val padding = 8 * density
        val lineHeight = paint.descent() - paint.ascent()
        val badgeDiameter = lineHeight * .94f
        val badgeGap = lineHeight * .30f
        fun prefixWidth(prefix: String) = if (prefix.isEmpty()) 0f else badgeDiameter + badgeGap
        fun overflow(index: Int): Float {
            val line = lines[index]
            return max(0f, paint.measureText(line.second) - (size.width - padding * 2 - prefixWidth(line.first)))
        }
        fun hold(index: Int) = CalendarMarqueeTiming.holdMillis(overflow(index), density)
        val total = lines.indices.sumOf { hold(it) + 200L }
        var phase = elapsed % total
        var index = 0
        while (phase >= hold(index) + 200) { phase -= hold(index) + 200; index++ }
        val travel = if (phase <= hold(index)) 0f else (phase - hold(index)) / 200f * lineHeight
        val canvas = drawContext.canvas.nativeCanvas
        fun drawLine(i: Int, offset: Float, time: Long) {
            val (prefix, body, color) = lines[i]
            paint.color = color; bold.color = color
            val prefixWidth = prefixWidth(prefix)
            val width = paint.measureText(body)
            val excess = overflow(i)
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
            val scroll = CalendarMarqueeTiming.scrollOffset(time, excess, density)
            canvas.drawText(body, x + prefixWidth - scroll, baseline, paint)
            canvas.restore()
            canvas.restore()
        }
        drawLine(index, -travel, phase)
        if (travel > 0) drawLine((index + 1) % lines.size, lineHeight - travel, 0)
    }
}
