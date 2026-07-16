package com.clockmods.calendar;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class LunarCalendarTest {
    @Test
    public void formatsExpectedSampleDate() {
        Calendar calendar = new GregorianCalendar(2026, Calendar.JULY, 14);
        Assert.assertEquals("丙午[马]年六月初一", LunarCalendar.format(calendar));
    }

    @Test
    public void returnsEmptyForOutOfRangeDate() {
        Calendar calendar = new GregorianCalendar(1899, Calendar.DECEMBER, 31);
        Assert.assertEquals("", LunarCalendar.format(calendar));
    }

    @Test
    public void usesLocalDateWhenSameInstantFallsOnDifferentDays() {
        Calendar instant = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        instant.clear();
        instant.set(2026, Calendar.JULY, 14, 0, 30, 0);

        Calendar nextDayZone = new GregorianCalendar(new SimpleTimeZone(14 * 60 * 60 * 1000, "UTC+14"));
        Calendar previousDayZone = new GregorianCalendar(new SimpleTimeZone(-12 * 60 * 60 * 1000, "UTC-12"));
        nextDayZone.setTimeInMillis(instant.getTimeInMillis());
        previousDayZone.setTimeInMillis(instant.getTimeInMillis());

        Assert.assertEquals("丙午[马]年六月初一", LunarCalendar.format(nextDayZone));
        Assert.assertEquals("丙午[马]年五月廿九", LunarCalendar.format(previousDayZone));
    }
}
