package com.clockmods.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public final class CalendarMonth {
    public static final int CELL_COUNT = 42;

    public final int year;
    public final int month;
    public final List<Day> days;

    private CalendarMonth(int year, int month, List<Day> days) {
        this.year = year;
        this.month = month;
        this.days = Collections.unmodifiableList(days);
    }

    public static CalendarMonth create(int year, int month, TimeZone timeZone, long todayMillis) {
        Calendar first = new GregorianCalendar(timeZone);
        first.clear();
        first.set(year, month, 1);
        int leadingDays = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        first.add(Calendar.DAY_OF_MONTH, -leadingDays);

        Calendar today = new GregorianCalendar(timeZone);
        today.setTimeInMillis(todayMillis);
        List<Day> cells = new ArrayList<>(CELL_COUNT);
        for (int index = 0; index < CELL_COUNT; index++) {
            boolean currentMonth = first.get(Calendar.YEAR) == year
                    && first.get(Calendar.MONTH) == month;
            boolean isToday = sameDate(first, today);
            cells.add(new Day(first.get(Calendar.YEAR), first.get(Calendar.MONTH),
                    first.get(Calendar.DAY_OF_MONTH), currentMonth, isToday,
                    LunarCalendar.formatShort(first)));
            first.add(Calendar.DAY_OF_MONTH, 1);
        }
        return new CalendarMonth(year, month, cells);
    }

    private static boolean sameDate(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    public static final class Day {
        public final int year;
        public final int month;
        public final int dayOfMonth;
        public final boolean currentMonth;
        public final boolean today;
        public final String lunarLabel;

        Day(int year, int month, int dayOfMonth, boolean currentMonth, boolean today,
                String lunarLabel) {
            this.year = year;
            this.month = month;
            this.dayOfMonth = dayOfMonth;
            this.currentMonth = currentMonth;
            this.today = today;
            this.lunarLabel = lunarLabel;
        }
    }
}