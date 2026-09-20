package com.clockmods.calendar

import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.GregorianCalendar
import java.util.TimeZone

/** A window of consecutive days laid out in a grid. */
class CalendarMonth private constructor(
    @JvmField val year: Int,
    @JvmField val month: Int,
    @JvmField val columns: Int,
    days: List<Day>,
) {
    @JvmField
    val days: List<Day> = Collections.unmodifiableList(days)

    companion object {
        @JvmStatic
        fun create(year: Int, month: Int, timeZone: TimeZone, todayMillis: Long): CalendarMonth =
        create(year, month, timeZone, todayMillis, Calendar.SUNDAY)

    @JvmStatic
    fun create(
        year: Int,
        month: Int,
        timeZone: TimeZone,
        todayMillis: Long,
        firstDayOfWeek: Int,
    ): CalendarMonth {
        val first = dayStart(timeZone, year, month, 1)
        first.add(Calendar.DAY_OF_MONTH, -leadingDays(first, firstDayOfWeek))
        return fill(year, month, DAYS_PER_WEEK, first, CELL_COUNT, timeZone, todayMillis)
    }

    @JvmStatic
    fun createWeek(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        timeZone: TimeZone,
        todayMillis: Long,
        firstDayOfWeek: Int,
    ): CalendarMonth {
        val start = dayStart(timeZone, year, month, dayOfMonth)
        val anchorYear = start.get(Calendar.YEAR)
        val anchorMonth = start.get(Calendar.MONTH)
        start.add(Calendar.DAY_OF_MONTH, -leadingDays(start, firstDayOfWeek))
        return fill(anchorYear, anchorMonth, DAYS_PER_WEEK, start, DAYS_PER_WEEK, timeZone, todayMillis)
    }

    @JvmStatic
    fun createDay(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        timeZone: TimeZone,
        todayMillis: Long,
    ): CalendarMonth {
        val start = dayStart(timeZone, year, month, dayOfMonth)
        return fill(start.get(Calendar.YEAR), start.get(Calendar.MONTH), 1, start, 1, timeZone, todayMillis)
    }

        private fun fill(
            year: Int,
            month: Int,
            columns: Int,
            cursor: Calendar,
            count: Int,
            timeZone: TimeZone,
            todayMillis: Long,
        ): CalendarMonth {
            val today = GregorianCalendar(timeZone).apply { timeInMillis = todayMillis }
            val cells = ArrayList<Day>(count)
            repeat(count) {
                val currentMonth = cursor.get(Calendar.YEAR) == year && cursor.get(Calendar.MONTH) == month
                val isToday = sameDate(cursor, today)
                cells += Day(
                    cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH),
                    cursor.get(Calendar.DAY_OF_MONTH), cursor.get(Calendar.DAY_OF_WEEK),
                    currentMonth, isToday,
                )
                cursor.add(Calendar.DAY_OF_MONTH, 1)
            }
            return CalendarMonth(year, month, columns, cells)
        }

        const val CELL_COUNT: Int = 42
        private const val DAYS_PER_WEEK = 7

        private fun dayStart(timeZone: TimeZone, year: Int, month: Int, dayOfMonth: Int): Calendar =
            GregorianCalendar(timeZone).apply {
                clear()
                set(year, month, dayOfMonth)
            }

        private fun leadingDays(day: Calendar, firstDayOfWeek: Int): Int {
            val normalized = if (firstDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) {
                firstDayOfWeek
            } else Calendar.SUNDAY
            return (day.get(Calendar.DAY_OF_WEEK) - normalized + 7) % 7
        }

        private fun sameDate(first: Calendar, second: Calendar): Boolean =
            first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }

    class Day(
        @JvmField val year: Int,
        @JvmField val month: Int,
        @JvmField val dayOfMonth: Int,
        @JvmField val dayOfWeek: Int,
        @JvmField val currentMonth: Boolean,
        @JvmField val today: Boolean,
    )
}
