package com.clockmods.ultimate.compose

import androidx.annotation.StringRes
import com.clockmods.R

/**
 * Compose-facing calendar theme data.
 *
 * The legacy calendar renderer has a much larger Java style graph because it paints Android
 * Views.  Compose only needs the palette and a small amount of layout intent, so keeping this
 * catalogue as immutable data makes the setting and the actual calendar use exactly the same
 * values without pulling the old View renderer into the Compose surface.
 */
internal data class ComposeCalendarTheme(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val summaryRes: Int,
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
    /** Poster is intentionally a borderless, flat number grid rather than a card grid. */
    val flatGrid: Boolean = false,
) {
    companion object {
        const val ID_GRAPHITE = "calendar.graphite"
        const val ID_PAPER = "calendar.paper"
        const val ID_POSTER = "calendar.poster"
        const val ID_AGENDA = "calendar.agenda"
        const val ID_CARBON = "calendar.carbon"

        private val PRESETS = listOf(
            ComposeCalendarTheme(
                ID_GRAPHITE,
                R.string.ultimate_calendar_theme_graphite_name,
                R.string.ultimate_calendar_theme_graphite_summary,
                0xFF171918.toInt(), 0xFF171918.toInt(), 0xFF363836.toInt(), 0,
                10f, 0xFFF2F3F2.toInt(), 0xFFD0D2D0.toInt(), 0xFFFFFFFF.toInt(),
                0xFFF2F3F2.toInt(), 0xFF2693FF.toInt(), 0xFF16E13B.toInt(), 0,
                0xFF16E13B.toInt(), 0xFFFFC11A.toInt(), 0xFFFF9B9B.toInt(),
                0xFF16E13B.toInt(), 0xFFFF5A5A.toInt(), 0xFF33404B.toInt(),
                0xFF2693FF.toInt(),
            ),
            ComposeCalendarTheme(
                ID_CARBON,
                R.string.ultimate_calendar_theme_carbon_name,
                R.string.ultimate_calendar_theme_carbon_summary,
                0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF191A19.toInt(), 0,
                10f, 0xFFE8E9E8.toInt(), 0xFF9A9D9A.toInt(), 0xFFF2F3F2.toInt(),
                0xFF9A9D9A.toInt(), 0xFF2693FF.toInt(), 0xFF16E13B.toInt(), 0,
                0xFF16E13B.toInt(), 0xFFFFC11A.toInt(), 0xFFFF9B9B.toInt(),
                0xFF16E13B.toInt(), 0xFFFF5A5A.toInt(), 0xFF262A2E.toInt(),
                0xFF2693FF.toInt(),
            ),
            ComposeCalendarTheme(
                ID_PAPER,
                R.string.ultimate_calendar_theme_paper_name,
                R.string.ultimate_calendar_theme_paper_summary,
                0xFFF5F1E6.toInt(), 0xFFEBE4D3.toInt(), 0xFFFCFAF4.toInt(),
                0xFFDFD7C4.toInt(), 8f, 0xFF262420.toInt(), 0xFF7C7466.toInt(),
                0xFF33302A.toInt(), 0xFF7C7466.toInt(), 0xFF8A6034.toInt(),
                0xFFB5392A.toInt(), 0x22B5392A, 0xFF4A7A4E.toInt(),
                0xFFB07A21.toInt(), 0xFFA8492F.toInt(), 0xFF3F6B45.toInt(),
                0xFFA8342A.toInt(), 0xFFEFE6D2.toInt(), 0xFFB5392A.toInt(),
            ),
            ComposeCalendarTheme(
                ID_POSTER,
                R.string.ultimate_calendar_theme_poster_name,
                R.string.ultimate_calendar_theme_poster_summary,
                0xFFFAFAF8.toInt(), 0xFFF2F2EE.toInt(), 0, 0, 0f,
                0xFF16181A.toInt(), 0xFF9AA0A6.toInt(), 0xFF1F2226.toInt(),
                0xFFB0B5BA.toInt(), 0xFFC8362F.toInt(), 0xFFC8362F.toInt(), 0,
                0xFF4A7A4E.toInt(), 0xFFB07A21.toInt(), 0xFF9BA1A6.toInt(),
                0xFF3F6B45.toInt(), 0xFFA8342A.toInt(), 0, 0xFF16181A.toInt(),
                flatGrid = true,
            ),
            ComposeCalendarTheme(
                ID_AGENDA,
                R.string.ultimate_calendar_theme_agenda_name,
                R.string.ultimate_calendar_theme_agenda_summary,
                0xFFF5F6FB.toInt(), 0xFFE7EAF6.toInt(), 0xFFFFFFFF.toInt(),
                0xFFDCE0F0.toInt(), 18f, 0xFF1B1F3B.toInt(), 0xFF6E748F.toInt(),
                0xFF232845.toInt(), 0xFF8B90A8.toInt(), 0xFF3B4A9E.toInt(),
                0xFF3B4A9E.toInt(), 0x1F3B4A9E, 0xFF2F8F6B.toInt(),
                0xFFC1811F.toInt(), 0xFFC24B57.toInt(), 0xFF2F8F6B.toInt(),
                0xFFC0453F.toInt(), 0xFFE7EAF9.toInt(), 0xFF3B4A9E.toInt(),
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
