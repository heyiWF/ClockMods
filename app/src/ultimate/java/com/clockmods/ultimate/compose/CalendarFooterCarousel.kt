package com.clockmods.ultimate.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.pro.CalendarDashboardSizing
import com.clockmods.ui.ClockTypefaceResolver
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Original footer timing: read, scroll to the end, pause, then slide to the next coloured line. */
@Composable
internal fun CalendarFooterCarousel(cell: CalendarCellInfo, date: String, theme: ComposeCalendarTheme,
    typography: CalendarTypography, height: Float, modifier: Modifier) {
    val context = LocalContext.current
    val good = stringResource(R.string.calendar_suitable_prefix)
    val bad = stringResource(R.string.calendar_avoid_prefix)
    val lines = listOf(Triple("", date, theme.text)) +
        (if (cell.suitable.isNotEmpty()) listOf(Triple(good, cell.suitable.joinToString(" "), theme.suitable)) else emptyList()) +
        (if (cell.avoid.isNotEmpty()) listOf(Triple(bad, cell.avoid.joinToString(" "), theme.avoid)) else emptyList())
    val paint = remember(typography) { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.weight) } }
    val bold = remember(typography) { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = ClockTypefaceResolver.resolve(context, typography.family, typography.emphasizedWeight) } }
    var elapsed by remember(cell.day) { mutableLongStateOf(0L) }
    LaunchedEffect(cell.day) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { elapsed = (it - start) / 1_000_000 }
    }
    Canvas(modifier) {
        paint.textSize = CalendarDashboardSizing.monthFooterSize(height * density, density) *
            typography.dateScale / ClockPreferences.DEFAULT_DATE_FONT_SCALE
        bold.textSize = paint.textSize
        val padding = 8 * density
        fun overflow(index: Int): Float {
            val line = lines[index]
            return max(0f, paint.measureText(line.second) - (size.width - padding * 2 - bold.measureText(line.first)))
        }
        fun scrollTime(index: Int) = ceil(overflow(index) / (40 * density) * 1000).toLong()
        fun hold(index: Int) = max(3000L, if (overflow(index) > 0) 2000L + scrollTime(index) else 0L)
        val total = lines.indices.sumOf { hold(it) + 200L }
        var phase = elapsed % total
        var index = 0
        while (phase >= hold(index) + 200) { phase -= hold(index) + 200; index++ }
        val lineHeight = paint.descent() - paint.ascent()
        val travel = if (phase <= hold(index)) 0f else (phase - hold(index)) / 200f * lineHeight
        val canvas = drawContext.canvas.nativeCanvas
        fun drawLine(i: Int, offset: Float, time: Long) {
            val (prefix, body, color) = lines[i]
            paint.color = color; bold.color = color
            val prefixWidth = bold.measureText(prefix)
            val width = paint.measureText(body)
            val excess = overflow(i)
            val baseline = size.height / 2 - (paint.ascent() + paint.descent()) / 2 + offset
            val top = max(0f, (size.height - lineHeight) / 2)
            val bottom = min(size.height, (size.height + lineHeight) / 2)
            val x = if (excess == 0f) (size.width - prefixWidth - width) / 2 else padding
            canvas.save()
            canvas.clipRect(0f, top, size.width, bottom)
            canvas.drawText(prefix, x, baseline, bold)
            canvas.save()
            canvas.clipRect(x + prefixWidth, top, size.width - if (excess > 0) padding else 0f, bottom)
            val scroll = if (excess == 0f) 0f else excess * ((time - 1000f) / scrollTime(i).coerceAtLeast(1)).coerceIn(0f, 1f)
            canvas.drawText(body, x + prefixWidth - scroll, baseline, paint)
            canvas.restore()
            canvas.restore()
        }
        drawLine(index, -travel, phase)
        if (travel > 0) drawLine((index + 1) % lines.size, lineHeight - travel, 0)
    }
}
