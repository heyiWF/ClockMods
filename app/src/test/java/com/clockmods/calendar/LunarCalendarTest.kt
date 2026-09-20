package com.clockmods.calendar

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.SimpleTimeZone
import java.util.TimeZone
import org.junit.Assert
import org.junit.Test

class LunarCalendarTest {
    @Test
    fun formatsExpectedSampleDate() {
        Assert.assertEquals("丙午[马]年六月初一", LunarCalendar.format(GregorianCalendar(2026, Calendar.JULY, 14)))
    }

    @Test
    fun formatsDatesBelowLegacyLowerBound() {
        Assert.assertEquals("己亥[猪]年冬月廿九", LunarCalendar.format(GregorianCalendar(1899, Calendar.DECEMBER, 31)))
    }

    @Test
    fun formatsShortLabelForCalendarCell() {
        Assert.assertEquals("六月", LunarCalendar.formatShort(GregorianCalendar(2026, Calendar.JULY, 14)))
        Assert.assertEquals("初二", LunarCalendar.formatShort(GregorianCalendar(2026, Calendar.JULY, 15)))
    }

    @Test
    fun usesLocalDateWhenSameInstantFallsOnDifferentDays() {
        val instant = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.JULY, 14, 0, 30, 0)
        }
        val nextDayZone = GregorianCalendar(SimpleTimeZone(14 * 60 * 60 * 1000, "UTC+14")).apply {
            timeInMillis = instant.timeInMillis
        }
        val previousDayZone = GregorianCalendar(SimpleTimeZone(-12 * 60 * 60 * 1000, "UTC-12")).apply {
            timeInMillis = instant.timeInMillis
        }
        Assert.assertEquals("丙午[马]年六月初一", LunarCalendar.format(nextDayZone))
        Assert.assertEquals("丙午[马]年五月廿九", LunarCalendar.format(previousDayZone))
    }
}
