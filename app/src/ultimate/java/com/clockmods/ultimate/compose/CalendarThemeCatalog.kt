package com.clockmods.ultimate.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.R

/**
 * Layout intent for a calendar theme.
 *
 * The colour palette alone made every theme read as "the same theme"; a layout discriminator
 * gives each preset a genuinely different structure so the selector thumbnails and the live
 * calendar both reflect the reference designs.
 */
internal enum class CalendarLayout {
    /** Grid on the left, rich detail card on the right (landscape) or below (portrait). */
    DASHBOARD,

    /** Cream paper card grid with a red "today" highlight and 班/休 badges. */
    CARD_GRID,

    /** Oversized month word-mark hero with a borderless number grid. */
    HERO_MONTH,

    /** Horizontal week strip with a day-detail panel featuring weather + 宜/忌. */
    WEEK_AGENDA,

    /** Immersive split panel: month grid + weather strip + a 宜/忌 marquee footer. */
    SPLIT_PANEL,
}

/**
 * Compose-facing calendar theme data.
 *
 * The legacy calendar renderer has a much larger Java style graph because it paints Android
 * Views.  Compose only needs the palette, a layout discriminator and a little typography
 * intent, so keeping this catalogue as immutable data makes the setting and the actual calendar
 * use exactly the same values without pulling the old View renderer into the Compose surface.
 */
internal data class ComposeCalendarTheme(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val summaryRes: Int,
    val layout: CalendarLayout,
    val backgroundStart: Int,
    val backgroundEnd: Int,
    val panel: Int,
    val panelStroke: Int,
    val cornerRadiusDp: Float,
    val text: Int,
    val secondary: Int,
    val day: Int,
    val weekday: Int,
    val accent: Int,
    val today: Int,
    val todayFill: Int,
    val restBadge: Int,
    val workBadge: Int,
    val weekend: Int,
    val suitable: Int,
    val avoid: Int,
    val selectionFill: Int,
    val selectionStroke: Int,
    /** True for the borderless, flat number grids (Hero month). */
    val flatGrid: Boolean = false,
    /** Show a weather strip inside the detail surface. */
    val showWeather: Boolean = false,
    /** Scroll 宜/忌 on a marquee footer instead of showing them in the detail card. */
    val marqueeAlmanac: Boolean = false,
) {
    companion object {
        const val ID_GRAPHITE = "calendar.graphite"
        const val ID_PAPER = "calendar.paper"
        const val ID_POSTER = "calendar.poster"
        const val ID_AGENDA = "calendar.agenda"
        const val ID_CARBON = "calendar.carbon"

        private val PRESETS = listOf(
            // Graphite — default dark dashboard: card grid + rich detail panel.
            ComposeCalendarTheme(
                ID_GRAPHITE,
                R.string.ultimate_calendar_theme_graphite_name,
                R.string.ultimate_calendar_theme_graphite_summary,
                CalendarLayout.DASHBOARD,
                0xFF171918.toInt(), 0xFF171918.toInt(), 0xFF363836.toInt(), 0,
                10f, 0xFFF2F3F2.toInt(), 0xFFD0D2D0.toInt(), 0xFFFFFFFF.toInt(),
                0xFFF2F3F2.toInt(), 0xFF2693FF.toInt(), 0xFF2693FF.toInt(), 0,
                0xFF16E13B.toInt(), 0xFFFFC11A.toInt(), 0xFFFF9B9B.toInt(),
                0xFF16E13B.toInt(), 0xFFFF5A5A.toInt(), 0xFF33404B.toInt(),
                0xFF2693FF.toInt(),
                showWeather = true,
            ),
            // Carbon — OLED split panel: month grid + weather + 宜/忌 marquee footer.
            ComposeCalendarTheme(
                ID_CARBON,
                R.string.ultimate_calendar_theme_carbon_name,
                R.string.ultimate_calendar_theme_carbon_summary,
                CalendarLayout.SPLIT_PANEL,
                0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF191A19.toInt(), 0,
                10f, 0xFFE8E9E8.toInt(), 0xFF9A9D9A.toInt(), 0xFFF2F3F2.toInt(),
                0xFF9A9D9A.toInt(), 0xFF2693FF.toInt(), 0xFF2693FF.toInt(), 0,
                0xFF16E13B.toInt(), 0xFFFFC11A.toInt(), 0xFFFF9B9B.toInt(),
                0xFF16E13B.toInt(), 0xFFFF5A5A.toInt(), 0xFF262A2E.toInt(),
                0xFF2693FF.toInt(),
                showWeather = true,
                marqueeAlmanac = true,
            ),
            // Paper — cream card grid with a red "today" highlight and 班/休 badges.
            ComposeCalendarTheme(
                ID_PAPER,
                R.string.ultimate_calendar_theme_paper_name,
                R.string.ultimate_calendar_theme_paper_summary,
                CalendarLayout.CARD_GRID,
                0xFFF5F1E6.toInt(), 0xFFEBE4D3.toInt(), 0xFFFCFAF4.toInt(),
                0xFFDFD7C4.toInt(), 8f, 0xFF262420.toInt(), 0xFF7C7466.toInt(),
                0xFF33302A.toInt(), 0xFF7C7466.toInt(), 0xFF8A6034.toInt(),
                0xFFB5392A.toInt(), 0x22B5392A, 0xFF4A7A4E.toInt(),
                0xFFB07A21.toInt(), 0xFFA8492F.toInt(), 0xFF3F6B45.toInt(),
                0xFFA8342A.toInt(), 0xFFEFE6D2.toInt(), 0xFFB5392A.toInt(),
            ),
            // Poster — oversized month word-mark with a borderless number grid.
            ComposeCalendarTheme(
                ID_POSTER,
                R.string.ultimate_calendar_theme_poster_name,
                R.string.ultimate_calendar_theme_poster_summary,
                CalendarLayout.HERO_MONTH,
                0xFFFAFAF8.toInt(), 0xFFF2F2EE.toInt(), 0, 0, 0f,
                0xFF16181A.toInt(), 0xFF9AA0A6.toInt(), 0xFF1F2226.toInt(),
                0xFFB0B5BA.toInt(), 0xFFC8362F.toInt(), 0xFFC8362F.toInt(), 0,
                0xFF4A7A4E.toInt(), 0xFFB07A21.toInt(), 0xFF9BA1A6.toInt(),
                0xFF3F6B45.toInt(), 0xFFA8342A.toInt(), 0, 0xFF16181A.toInt(),
                flatGrid = true,
            ),
            // Agenda — indigo week strip with a day-detail panel carrying weather and 宜/忌.
            ComposeCalendarTheme(
                ID_AGENDA,
                R.string.ultimate_calendar_theme_agenda_name,
                R.string.ultimate_calendar_theme_agenda_summary,
                CalendarLayout.WEEK_AGENDA,
                0xFFF5F6FB.toInt(), 0xFFE7EAF6.toInt(), 0xFFFFFFFF.toInt(),
                0xFFDCE0F0.toInt(), 18f, 0xFF1B1F3B.toInt(), 0xFF6E748F.toInt(),
                0xFF232845.toInt(), 0xFF8B90A8.toInt(), 0xFF3B4A9E.toInt(),
                0xFF3B4A9E.toInt(), 0x1F3B4A9E, 0xFF2F8F6B.toInt(),
                0xFFC1811F.toInt(), 0xFFC24B57.toInt(), 0xFF2F8F6B.toInt(),
                0xFFC0453F.toInt(), 0xFFE7EAF9.toInt(), 0xFF3B4A9E.toInt(),
                showWeather = true,
            ),
        )

        fun presets(): List<ComposeCalendarTheme> = PRESETS

        fun resolve(id: String?): ComposeCalendarTheme =
            PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()
    }
}

internal object CalendarThemeCatalog {
    fun presets(): List<ComposeCalendarTheme> = ComposeCalendarTheme.presets()

    fun resolve(id: String?): ComposeCalendarTheme = ComposeCalendarTheme.resolve(id)
}

/**
 * A miniature rendition of a calendar theme, drawn purely from the theme's own colours so each
 * gallery/selector tile advertises the real look (dark dashboard vs. cream card grid vs. flat
 * month hero) rather than a single flat swatch. The layout branch mirrors the live calendar's
 * `CalendarLayout` dispatch so the thumbnail is an honest preview.
 */
@Composable
internal fun CalendarThemeThumbnail(
    theme: ComposeCalendarTheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(theme.backgroundStart), Color(theme.backgroundEnd)),
                ),
            )
            .padding(6.dp),
    ) {
        when (theme.layout) {
            CalendarLayout.HERO_MONTH -> PosterThumbnail(theme)
            CalendarLayout.WEEK_AGENDA -> AgendaThumbnail(theme)
            CalendarLayout.SPLIT_PANEL -> SplitThumbnail(theme)
            else -> GridThumbnail(theme)
        }
    }
}

/** Big month word-mark on the left, flat number grid on the right. */
@Composable
private fun PosterThumbnail(theme: ComposeCalendarTheme) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(0.34f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "九月",
                color = Color(theme.text),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text("2026", color = Color(theme.secondary), fontSize = 8.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(22.dp)
                    .height(2.dp)
                    .background(Color(theme.accent)),
            )
        }
        Spacer(Modifier.width(6.dp))
        MiniGrid(theme, columns = 7, rows = 4, modifier = Modifier.weight(0.66f), flat = true)
    }
}

/** Week strip on the left, day-detail card on the right. */
@Composable
private fun AgendaThumbnail(theme: ComposeCalendarTheme) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { row ->
                val selected = row == 1
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) Color(theme.selectionFill) else Color(theme.panel))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        (row + 20).toString(),
                        color = if (selected) Color(theme.text) else Color(theme.secondary),
                        fontSize = 7.sp,
                    )
                }
            }
        }
        Column(
            Modifier
                .weight(0.58f)
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(theme.panel))
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("9月22日", color = Color(theme.text), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Text("晴 26°", color = Color(theme.secondary), fontSize = 7.sp)
            AlmanacChips(theme)
        }
    }
}

/** Month grid on the left, immersive detail panel with a bottom marquee bar on the right. */
@Composable
private fun SplitThumbnail(theme: ComposeCalendarTheme) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        MiniGrid(theme, columns = 7, rows = 5, modifier = Modifier.fillMaxWidth().weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(theme.suitable).copy(alpha = 0.22f))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "宜 出行 · 会友",
                color = Color(theme.suitable),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/** The classic card grid used by the dashboard and paper themes. */
@Composable
private fun GridThumbnail(theme: ComposeCalendarTheme) {
    MiniGrid(theme, columns = 7, rows = 5, modifier = Modifier.fillMaxSize())
}

/** A tiny month grid; today is drawn with the theme's highlight so the tile reads instantly. */
@Composable
private fun MiniGrid(
    theme: ComposeCalendarTheme,
    columns: Int,
    rows: Int,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
) {
    val todayFill = theme.todayFill.takeIf { it != 0 }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(columns) { col ->
                Text(
                    listOf("日", "一", "二", "三", "四", "五", "六")[col],
                    color = Color(theme.weekday),
                    fontSize = 5.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        repeat(rows) { row ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(columns) { col ->
                    val isToday = row == 1 && col == 3
                    val isRest = (row == 2 && col == 5) || (row == 3 && col == 6)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(if (flat) 0.dp else 3.dp))
                            .background(
                                when {
                                    isToday -> todayFill?.let(::Color) ?: Color(theme.panel)
                                    isRest -> Color(theme.restBadge).copy(alpha = 0.28f)
                                    flat -> Color.Transparent
                                    theme.panel != 0 -> Color(theme.panel)
                                    else -> Color.Transparent
                                },
                            ),
                    )
                }
            }
        }
    }
}

/** Compact 宜/忌 pill preview used inside the day-detail thumbnail. */
@Composable
private fun AlmanacChips(theme: ComposeCalendarTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        AlmanacChip(
            label = androidx.compose.ui.res.stringResource(R.string.ultimate_calendar_almanac_suitable),
            color = Color(theme.suitable),
        )
        AlmanacChip(
            label = androidx.compose.ui.res.stringResource(R.string.ultimate_calendar_almanac_avoid),
            color = Color(theme.avoid),
        )
    }
}

@Composable
private fun AlmanacChip(label: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 3.dp, vertical = 1.dp),
    ) {
        Text(label, color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}
