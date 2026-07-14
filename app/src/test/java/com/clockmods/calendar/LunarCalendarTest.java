package com.clockmods.calendar;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

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
}