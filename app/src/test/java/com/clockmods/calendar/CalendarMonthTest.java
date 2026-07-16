package com.clockmods.calendar;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class CalendarMonthTest {
    @Test
    public void createsSixWeekGridAndHighlightsToday() {
        TimeZone zone = TimeZone.getTimeZone("Asia/Shanghai");
        Calendar today = new GregorianCalendar(zone);
        today.clear();
        today.set(2026, Calendar.JULY, 16, 12, 0, 0);

        CalendarMonth month = CalendarMonth.create(2026, Calendar.JULY, zone,
                today.getTimeInMillis());

        Assert.assertEquals(42, month.days.size());
        Assert.assertEquals(28, month.days.get(0).dayOfMonth);
        Assert.assertFalse(month.days.get(0).currentMonth);
        CalendarMonth.Day highlighted = month.days.get(18);
        Assert.assertEquals(16, highlighted.dayOfMonth);
        Assert.assertTrue(highlighted.today);
        Assert.assertEquals("初三", highlighted.lunarLabel);
    }
}