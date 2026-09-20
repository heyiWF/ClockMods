package com.clockmods.pro.schedule

import java.util.Locale

/** One entry on a day's schedule. */
class ScheduleItem(
    id: String?,
    title: String?,
    @JvmField val hour: Int,
    @JvmField val minute: Int,
) {
    @JvmField
    val id: String = id.orEmpty()

    @JvmField
    val title: String = title.orEmpty()

    fun hasTime(): Boolean = hour != TIME_NONE && minute != TIME_NONE

    fun timeLabel(): String =
        if (hasTime()) String.format(Locale.US, "%02d:%02d", hour, minute) else ""

    fun sortKey(): Int = if (hasTime()) hour * 100 + minute else -1

    override fun equals(other: Any?): Boolean =
        this === other || other is ScheduleItem &&
            id == other.id && title == other.title &&
            hour == other.hour && minute == other.minute

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + hour
        return 31 * result + minute
    }

    companion object {
        const val TIME_NONE = -1
    }
}
