package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateFormatterTest {
    private static Calendar example() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2026, Calendar.AUGUST, 7, 12, 0, 0);
        return cal;
    }

    @Test
    public void rendersTheDefaultChineseDateLine() {
        Assert.assertEquals("2026年8月7日 星期五", DateFormatter.format(example()));
    }

    @Test
    public void rendersLeapDayAndYearBoundary() {
        Calendar calendar = example();
        calendar.set(2024, Calendar.FEBRUARY, 29);
        Assert.assertEquals("2024年2月29日 星期四", DateFormatter.format(calendar));
        calendar.set(2027, Calendar.JANUARY, 1);
        Assert.assertEquals("2027年1月1日 星期五", DateFormatter.format(calendar));
    }

    @Test
    public void followsTheCalendarTimeZone() {
        Calendar calendar = example();
        calendar.set(2026, Calendar.AUGUST, 7, 23, 30, 0);
        long instant = calendar.getTimeInMillis();
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        calendar.setTimeInMillis(instant);
        Assert.assertEquals("2026年8月8日 星期六", DateFormatter.format(calendar));
    }
}
