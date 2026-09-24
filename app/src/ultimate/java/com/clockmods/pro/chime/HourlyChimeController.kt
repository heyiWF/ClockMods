package com.clockmods.pro.chime

import java.util.Calendar
import java.util.TimeZone

/** Pure scheduling rules used by the Compose chime indicator and tests. */
object HourlyChimeController {
    const val START_BEFORE_MILLIS = 2_000L
    const val DURATION_MILLIS = 5_000L

    /** The cue is anchored to the clock boundary, even if a frame is delivered late. */
    @JvmStatic
    fun progressAtMillis(nowMillis: Long, chimeAtMillis: Long): Float? {
        if (chimeAtMillis == Long.MIN_VALUE) return null
        val elapsed = nowMillis - (chimeAtMillis - START_BEFORE_MILLIS)
        return if (elapsed in 0 until DURATION_MILLIS) elapsed.toFloat() / DURATION_MILLIS else null
    }

    @JvmStatic
    fun upcomingChimeAtMillis(
        now: Calendar,
        hourlyEnabled: Boolean,
        halfHourEnabled: Boolean,
    ): Long {
        val minute = now.get(Calendar.MINUTE)
        val approachingHour = hourlyEnabled && minute == 59
        val approachingHalfHour = halfHourEnabled && minute == 29
        if (now.get(Calendar.SECOND) < 58 || !approachingHour && !approachingHalfHour) {
            return Long.MIN_VALUE
        }
        return (now.clone() as Calendar).apply {
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @JvmStatic
    fun isQuietAtMinute(current: Int, start: Int, end: Int): Boolean {
        val normalizedCurrent = normalizeMinute(current)
        val normalizedStart = normalizeMinute(start)
        val normalizedEnd = normalizeMinute(end)
        if (normalizedStart == normalizedEnd) return true
        return if (normalizedStart < normalizedEnd) {
            normalizedCurrent >= normalizedStart && normalizedCurrent < normalizedEnd
        } else {
            normalizedCurrent >= normalizedStart || normalizedCurrent < normalizedEnd
        }
    }

    @JvmStatic
    fun resolveTimeZone(zoneId: String?): TimeZone =
        if (zoneId.isNullOrEmpty()) TimeZone.getDefault() else TimeZone.getTimeZone(zoneId)

    private fun normalizeMinute(value: Int): Int {
        val normalized = value % MINUTES_PER_DAY
        return if (normalized < 0) normalized + MINUTES_PER_DAY else normalized
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
