package com.clockmods.pro

import com.clockmods.calendar.CalendarMonth
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class LunarAlmanacRangeTest {
    @Test
    fun supportsDatesOutsideLegacyPickerRange() {
        assertMonthUsable(1800, Calendar.JANUARY)
        assertMonthUsable(1900, Calendar.DECEMBER)
        assertMonthUsable(2100, Calendar.JANUARY)
        assertMonthUsable(2200, Calendar.DECEMBER)
    }

    private fun assertMonthUsable(year: Int, month0: Int) {
        val month = CalendarMonth.create(
            year,
            month0,
            TimeZone.getTimeZone("UTC"),
            0L,
            Calendar.MONDAY,
        )
        for (day in month.days) {
            val almanac = LunarAlmanac.of(day.year, day.month, day.dayOfMonth)
            assertFalse(almanac.shortLabel().isEmpty())
            assertNotNull(almanac.festivals())
        }
        assertFooterUsable(year, month0, 1)
    }

    private fun assertFooterUsable(year: Int, month0: Int, day: Int) {
        val almanac = LunarAlmanac.of(year, month0, day)
        assertFalse(almanac.shortLabel().isEmpty())
        assertFalse(almanac.naturalLabel().isEmpty())
        assertNotNull(almanac.festivals())
        assertNotNull(almanac.suitable())
        assertNotNull(almanac.avoid())
    }
}
