package com.clockmods.pro;

import com.clockmods.calendar.CalendarMonth;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class LunarAlmanacRangeTest {
    @Test
    public void supportsDatesOutsideLegacyPickerRange() {
        assertMonthUsable(1800, Calendar.JANUARY);
        assertMonthUsable(1900, Calendar.DECEMBER);
        assertMonthUsable(2100, Calendar.JANUARY);
        assertMonthUsable(2200, Calendar.DECEMBER);
    }

    private static void assertMonthUsable(int year, int month0) {
        CalendarMonth month = CalendarMonth.create(year, month0, TimeZone.getTimeZone("UTC"),
                0L, Calendar.MONDAY);
        for (CalendarMonth.Day day : month.days) {
            LunarAlmanac almanac = LunarAlmanac.of(day.year, day.month, day.dayOfMonth);
            Assert.assertFalse(almanac.shortLabel().isEmpty());
            Assert.assertNotNull(almanac.festivals());
        }
        assertFooterUsable(year, month0, 1);
    }

    private static void assertFooterUsable(int year, int month0, int day) {
        LunarAlmanac almanac = LunarAlmanac.of(year, month0, day);
        Assert.assertFalse(almanac.shortLabel().isEmpty());
        Assert.assertFalse(almanac.naturalLabel().isEmpty());
        Assert.assertNotNull(almanac.festivals());
        Assert.assertNotNull(almanac.suitable());
        Assert.assertNotNull(almanac.avoid());
    }
}
