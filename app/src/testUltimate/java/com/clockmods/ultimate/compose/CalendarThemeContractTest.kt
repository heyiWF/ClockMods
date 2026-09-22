package com.clockmods.ultimate.compose

import com.clockmods.background.ClockPreferences
import org.junit.Assert.*
import org.junit.Test

/** The shipped ultimate catalogue at 1abc6b8 is the migration contract. */
class CalendarThemeContractTest {
    @Test fun originalCatalogueAndFallback() {
        val themes = CalendarThemeCatalog.presets()
        assertEquals(listOf("calendar.graphite", "calendar.carbon", "calendar.paper", "calendar.poster", "calendar.agenda"), themes.map { it.id })
        assertEquals(listOf(CalendarLayout.DASHBOARD, CalendarLayout.DASHBOARD, CalendarLayout.WALL, CalendarLayout.POSTER, CalendarLayout.AGENDA), themes.map { it.layout })
        assertEquals(ClockPreferences.DEFAULT_CALENDAR_THEME, CalendarThemeCatalog.resolve("removed-theme").id)
        assertEquals(themes.first(), CalendarThemeCatalog.resolve(null))
    }

    @Test fun originalSurfaceAndTodayColors() {
        val expected = listOf(
            listOf(0xFF171918, 0xFF171918, 0xFF363836, 0xFF16E13B, 0x00000000),
            listOf(0xFF000000, 0xFF000000, 0xFF191A19, 0xFF16E13B, 0x00000000),
            listOf(0xFFF5F1E6, 0xFFEBE4D3, 0xFFFCFAF4, 0xFFB5392A, 0x22B5392A),
            listOf(0xFFFAFAF8, 0xFFF2F2EE, 0x00000000, 0xFFC8362F, 0x00000000),
            listOf(0xFFF5F6FB, 0xFFE7EAF6, 0xFFFFFFFF, 0xFF3B4A9E, 0x1F3B4A9E),
        ).map { row -> row.map(Long::toInt) }
        assertEquals(expected, CalendarThemeCatalog.presets().map {
            listOf(it.backgroundStart, it.backgroundEnd, it.panel, it.today, it.todayFill)
        })
    }

    @Test fun capabilitiesFollowOriginalCompositions() {
        val themes = CalendarThemeCatalog.presets()
        assertEquals(listOf(true, true, false, false, true), themes.map { it.showWeather })
        assertEquals(listOf(false, false, false, true, false), themes.map { it.flatGrid })
        assertEquals(listOf(true, true, false, false, false), themes.map { it.followsUserTimeColor })
        assertEquals(listOf(10f, 10f, 8f, 0f, 18f), themes.map { it.cornerRadiusDp })
    }
}
