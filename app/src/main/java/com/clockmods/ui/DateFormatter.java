package com.clockmods.ui;

import java.util.Calendar;

/** The fixed Chinese date line used by the Lite clock. */
public final class DateFormatter {
    private static final String[] WEEKDAYS = {
            "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    private DateFormatter() { }

    public static String format(Calendar calendar) {
        return calendar.get(Calendar.YEAR) + " 年 " + (calendar.get(Calendar.MONTH) + 1)
                + " 月 " + calendar.get(Calendar.DAY_OF_MONTH) + " 日 "
                + WEEKDAYS[calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY];
    }
}
