package com.clockmods.calendar

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert
import org.junit.Test

class CalendarMonthTest {
    @Test
    fun createsSixWeekGridAndHighlightsToday() {
        val zone = TimeZone.getTimeZone("Asia/Shanghai")
        val today = GregorianCalendar(zone).apply {
            clear()
            set(2026, Calendar.JULY, 16, 12, 0, 0)
        }
        val month = CalendarMonth.create(2026, Calendar.JULY, zone, today.timeInMillis)
        Assert.assertEquals(42, month.days.size)
        Assert.assertEquals(28, month.days[0].dayOfMonth)
        Assert.assertEquals(Calendar.SUNDAY, month.days[0].dayOfWeek)
        Assert.assertFalse(month.days[0].currentMonth)
        val highlighted = month.days[18]
        Assert.assertEquals(16, highlighted.dayOfMonth)
        Assert.assertTrue(highlighted.today)
    }

    @Test
    fun createsMondayFirstGridWhenRequested() {
        val zone = TimeZone.getTimeZone("Asia/Shanghai")
        val today = GregorianCalendar(zone).apply {
            clear()
            set(2026, Calendar.JULY, 16, 12, 0, 0)
        }
        val month = CalendarMonth.create(2026, Calendar.JULY, zone, today.timeInMillis, Calendar.MONDAY)
        Assert.assertEquals(42, month.days.size)
        Assert.assertEquals(29, month.days[0].dayOfMonth)
        Assert.assertEquals(Calendar.MONDAY, month.days[0].dayOfWeek)
        Assert.assertFalse(month.days[0].currentMonth)
        val highlighted = month.days[17]
        Assert.assertEquals(16, highlighted.dayOfMonth)
        Assert.assertTrue(highlighted.today)
    }
}
