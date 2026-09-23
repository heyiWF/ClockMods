package com.clockmods.ultimate.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas

/** Landscape miniatures follow the same pane order, calendar marks and surfaces as the page. */
@Composable
internal fun CalendarThemeThumbnail(theme: ComposeCalendarTheme, modifier: Modifier = Modifier) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(theme.backgroundStart), Color(theme.backgroundEnd))))
        val u = size.width / 180f
        val h = size.height
        fun panel(x: Float, y: Float, w: Float, height: Float) = drawRoundRect(Color(theme.panel),
            Offset(x, y), Size(w, height), CornerRadius(theme.cornerRadiusDp * u / 3))
        fun label(text: String, x: Float, y: Float, font: Float, color: Int) {
            paint.color = color; paint.textSize = font * u
            drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
        }
        fun almanacLabel(prefix: String, body: String, x: Float, baseline: Float, color: Int) {
            val radius = 4.5f * u
            drawCircle(Color(color).copy(alpha = ALMANAC_BADGE_BACKGROUND_ALPHA), radius,
                Offset(x + radius, baseline - 2f * u))
            label(prefix, x + 1.5f * u, baseline, 6f, color)
            label(body, x + radius * 2 + 3f * u, baseline, 6f, color)
        }
        fun grid(left: Float, top: Float, width: Float, height: Float, poster: Boolean = false) {
            if (!poster) { panel(left, top, width, height); label("‹     2026年9月      ›", left + 6*u, top + 10*u, 6f, theme.text) }
            val start = top + if (poster) 3*u else 17*u
            val rowH = (height - (start - top)) / 7
            for (row in 0..6) for (col in 0..6) {
                val x = left + (col + .35f) * width / 7
                val y = start + (row + .8f) * rowH
                val day = (row - 1) * 7 + col + 1
                label(if (row == 0) listOf("日", "一", "二", "三", "四", "五", "六")[col] else day.toString(),
                    x, y, if (row == 0) 4f else 5.5f, if (day == 21) theme.today else if (row == 0) theme.weekday else theme.day)
                if (poster && day == 21) drawCircle(Color(theme.today), .8f*u, Offset(x+2*u, y+3*u))
            }
        }
        when (theme.layout) {
            CalendarLayout.DASHBOARD -> {
                val left = size.width * .48f
                panel(4*u, 4*u, left-6*u, h*.30f)
                label("12:45", 12*u, h*.25f, 22f, theme.text)
                panel(4*u, h*.36f, left-6*u, h*.25f)
                label("☀  26℃", 15*u, h*.55f, 12f, theme.text)
                panel(4*u, h*.65f, left-6*u, h*.29f)
                label("今天    明天    后天", 9*u, h*.78f, 5f, theme.text)
                label("☀      ☁      ☀", 12*u, h*.90f, 9f, theme.accent)
                grid(left+2*u, 4*u, size.width-left-6*u, h-8*u)
            }
            CalendarLayout.WALL -> grid(4*u, 4*u, size.width-8*u, h-8*u)
            CalendarLayout.POSTER -> {
                label("九月", 6*u, h*.48f, 24f, theme.text)
                label("2026", 6*u, h*.66f, 9f, theme.secondary)
                drawLine(Color(theme.secondary).copy(alpha = .32f), Offset(size.width*.36f, 5*u), Offset(size.width*.36f, h-5*u), u)
                grid(size.width*.40f, 6*u, size.width*.56f, h-12*u, true)
            }
            CalendarLayout.AGENDA -> {
                label("2026年9月", 5*u, 10*u, 8f, theme.text)
                for (row in 0..6) {
                    val y = 14*u + row*(h-18*u)/7
                    if (row == 1) panel(4*u, y, size.width*.32f, (h-18*u)/7)
                    label("${row+20}    初${row+1}", 7*u, y+6*u, 5f, if (row == 1) theme.today else theme.day)
                }
                panel(size.width*.39f, 5*u, size.width*.57f, h-10*u)
                label("9月21日 星期一", size.width*.43f, 17*u, 7f, theme.text)
                almanacLabel("宜", "出行 · 会友", size.width*.43f, h*.5f, theme.suitable)
                almanacLabel("忌", "动土", size.width*.43f, h*.66f, theme.avoid)
            }
        }
    }
}
