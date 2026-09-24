package com.clockmods.ultimate.compose

import com.clockmods.calendar.CalendarMonth

/** Screen-owned cache of expensive civil-date data, shared by current and adjacent pages. */
internal class CalendarCellCache(
    private val capacity: Int = 192,
    private val load: (CalendarMonth.Day) -> CalendarCellInfo,
) {
    private data class DateKey(val year: Int, val month: Int, val day: Int)
    private val entries = object : LinkedHashMap<DateKey, CalendarCellInfo>(capacity, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<DateKey, CalendarCellInfo>?): Boolean =
            size > capacity
    }

    fun cells(month: CalendarMonth): List<CalendarCellInfo> = month.days.map(::get)

    private fun get(day: CalendarMonth.Day): CalendarCellInfo {
        val key = DateKey(day.year, day.month, day.dayOfMonth)
        val cached = synchronized(entries) { entries[key] }
        val value = cached ?: load(day).let { loaded ->
            // Never hold the lock while computing almanac data: background prefetch must
            // not block a foreground cache hit. Duplicate misses are safe and uncommon.
            synchronized(entries) { entries[key] ?: loaded.also { entries[key] = it } }
        }
        // currentMonth/today belong to the viewport, not the cached civil date.
        return value.copy(day = day)
    }
}
