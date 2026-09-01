package com.clockmods.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * A window of consecutive days laid out in a grid: the six-week month page, a single week, or a
 * single day. {@link #columns} is how wide a row is, so a caller can fill a grid without knowing
 * which factory produced the window.
 */
public final class CalendarMonth {
    /** Cells in a {@link #create} month page: six weeks of seven days. */
    public static final int CELL_COUNT = 42;
    private static final int DAYS_PER_WEEK = 7;

    public final int year;
    public final int month;
    /** Cells per row: 7 for a month or a week, 1 for a single day. */
    public final int columns;
    public final List<Day> days;

    private CalendarMonth(int year, int month, int columns, List<Day> days) {
        this.year = year;
        this.month = month;
        this.columns = columns;
        this.days = Collections.unmodifiableList(days);
    }

    public static CalendarMonth create(int year, int month, TimeZone timeZone, long todayMillis) {
        return create(year, month, timeZone, todayMillis, Calendar.SUNDAY);
    }

    public static CalendarMonth create(int year, int month, TimeZone timeZone, long todayMillis,
            int firstDayOfWeek) {
        Calendar first = dayStart(timeZone, year, month, 1);
        first.add(Calendar.DAY_OF_MONTH, -leadingDays(first, firstDayOfWeek));
        return fill(year, month, DAYS_PER_WEEK, first, CELL_COUNT, timeZone, todayMillis);
    }

    /**
     * The seven days of the week containing the given date. {@link Day#currentMonth} stays relative
     * to the anchor date's month, so a week straddling a month boundary can still be told apart.
     */
    public static CalendarMonth createWeek(int year, int month, int dayOfMonth, TimeZone timeZone,
            long todayMillis, int firstDayOfWeek) {
        Calendar start = dayStart(timeZone, year, month, dayOfMonth);
        int anchorYear = start.get(Calendar.YEAR);
        int anchorMonth = start.get(Calendar.MONTH);
        start.add(Calendar.DAY_OF_MONTH, -leadingDays(start, firstDayOfWeek));
        return fill(anchorYear, anchorMonth, DAYS_PER_WEEK, start, DAYS_PER_WEEK, timeZone,
                todayMillis);
    }

    /** A single day. Its one cell is always {@link Day#currentMonth}. */
    public static CalendarMonth createDay(int year, int month, int dayOfMonth, TimeZone timeZone,
            long todayMillis) {
        Calendar start = dayStart(timeZone, year, month, dayOfMonth);
        return fill(start.get(Calendar.YEAR), start.get(Calendar.MONTH), 1, start, 1, timeZone,
                todayMillis);
    }

    /**
     * @param cursor advanced one day per cell, so DST transitions are crossed by field arithmetic
     *               rather than by adding a fixed number of milliseconds.
     */
    private static CalendarMonth fill(int year, int month, int columns, Calendar cursor, int count,
            TimeZone timeZone, long todayMillis) {
        Calendar today = new GregorianCalendar(timeZone);
        today.setTimeInMillis(todayMillis);
        List<Day> cells = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            boolean currentMonth = cursor.get(Calendar.YEAR) == year
                    && cursor.get(Calendar.MONTH) == month;
            boolean isToday = sameDate(cursor, today);
            cells.add(new Day(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH),
                    cursor.get(Calendar.DAY_OF_MONTH), cursor.get(Calendar.DAY_OF_WEEK),
                    currentMonth, isToday));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return new CalendarMonth(year, month, columns, cells);
    }

    private static Calendar dayStart(TimeZone timeZone, int year, int month, int dayOfMonth) {
        Calendar day = new GregorianCalendar(timeZone);
        day.clear();
        day.set(year, month, dayOfMonth);
        return day;
    }

    /** Days to step back from {@code day} to reach the start of its week. */
    private static int leadingDays(Calendar day, int firstDayOfWeek) {
        int normalizedFirstDay = firstDayOfWeek >= Calendar.SUNDAY
                && firstDayOfWeek <= Calendar.SATURDAY ? firstDayOfWeek : Calendar.SUNDAY;
        return (day.get(Calendar.DAY_OF_WEEK) - normalizedFirstDay + 7) % 7;
    }

    private static boolean sameDate(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    public static final class Day {
        public final int year;
        public final int month;
        public final int dayOfMonth;
        public final int dayOfWeek;
        public final boolean currentMonth;
        public final boolean today;

        Day(int year, int month, int dayOfMonth, int dayOfWeek, boolean currentMonth,
                boolean today) {
            this.year = year;
            this.month = month;
            this.dayOfMonth = dayOfMonth;
            this.dayOfWeek = dayOfWeek;
            this.currentMonth = currentMonth;
            this.today = today;
        }
    }
}
