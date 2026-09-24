package com.clockmods.ultimate.compose

import com.clockmods.calendar.CalendarMonth
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CalendarCellCacheTest {
    private val zone = TimeZone.getTimeZone("UTC")
    private val now = Calendar.getInstance(zone).apply {
        clear(); set(2026, Calendar.SEPTEMBER, 24)
    }.timeInMillis

    private fun info(day: CalendarMonth.Day) = CalendarCellInfo(
        day, "label", listOf("festival"), null, listOf("good"), listOf("bad"), true,
    )

    @Test fun overlappingPagesReuseDetailsButKeepTheirOwnMonthAndTodayFlags() {
        var loads = 0
        val cache = CalendarCellCache { day -> loads++; info(day) }
        val september = CalendarMonth.create(2026, Calendar.SEPTEMBER, zone, now)
        val october = CalendarMonth.create(2026, Calendar.OCTOBER, zone, now)
        val first = cache.cells(september)
        val second = cache.cells(october)
        val uniqueDates = (september.days + october.days).map { Triple(it.year, it.month, it.dayOfMonth) }.toSet()
        assertEquals(uniqueDates.size, loads)
        assertFalse(first.first { it.day.month == Calendar.OCTOBER }.day.currentMonth)
        assertTrue(second.first { it.day.month == Calendar.OCTOBER }.day.currentMonth)
        assertTrue(second.all { it.hasSchedule })
        val tomorrow = CalendarMonth.create(2026, Calendar.SEPTEMBER, zone, now + 86_400_000)
        val refreshed = cache.cells(tomorrow)
        assertEquals(25, refreshed.single { it.day.today }.day.dayOfMonth)
        assertEquals(uniqueDates.size, loads)
    }

    @Test fun cacheEvictsLeastRecentlyUsedDates() {
        var loads = 0
        val cache = CalendarCellCache(capacity = 2) { day -> loads++; info(day) }
        fun visit(day: Int) = cache.cells(CalendarMonth.createDay(2026, 8, day, zone, now))
        visit(1); visit(2); visit(1); visit(3)
        assertEquals(3, loads)
        visit(1)
        assertEquals(3, loads)
        visit(2)
        assertEquals(4, loads)
    }

    @Test fun newScreenRevisionReloadsScheduleMarkers() {
        var scheduled = true
        fun cache() = CalendarCellCache { day -> info(day).copy(hasSchedule = scheduled) }
        val page = CalendarMonth.createDay(2026, 8, 24, zone, now)
        assertTrue(cache().cells(page).single().hasSchedule)
        scheduled = false
        assertFalse(cache().cells(page).single().hasSchedule)
    }
}
