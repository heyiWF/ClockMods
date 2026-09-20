package com.clockmods.ui

import java.util.Locale

object ClockTimeFormatter {
    @JvmStatic
    fun format(
        hour: Int,
        minute: Int,
        second: Int,
        showSeconds: Boolean,
        blinkColon: Boolean,
        smallSeconds: Boolean,
        use24Hour: Boolean,
        useEnglish: Boolean,
    ): DisplayTime {
        val showColon = !blinkColon || second % 2 == 0
        var displayHour = if (use24Hour) hour else hour % 12
        if (!use24Hour && displayHour == 0) displayHour = 12
        val periodText = if (use24Hour) "" else periodText(hour, useEnglish)
        val hoursAndMinutes = twoDigits(displayHour) + ":" + twoDigits(minute)
        return when {
            !showSeconds -> DisplayTime(hoursAndMinutes, "", periodText, showColon)
            smallSeconds -> DisplayTime(hoursAndMinutes, twoDigits(second), periodText, showColon)
            else -> DisplayTime(hoursAndMinutes + ":" + twoDigits(second), "", periodText, showColon)
        }
    }

    @JvmStatic
    fun formatHourlyChime(hour: Int, minute: Int, use24Hour: Boolean, useEnglish: Boolean): String {
        if (use24Hour) return twoDigits(hour) + ":" + twoDigits(minute)
        var displayHour = hour % 12
        if (displayHour == 0) displayHour = 12
        val time = displayHour.toString() + ":" + twoDigits(minute)
        val period = periodText(hour, useEnglish)
        return if (useEnglish) "$time $period" else period + time
    }

    @JvmStatic
    fun periodText(hour: Int, useEnglish: Boolean): String = if (useEnglish) {
        if (hour < 12) "AM" else "PM"
    } else {
        if (hour < 12) "上午" else "下午"
    }

    private fun twoDigits(value: Int): String = String.format(Locale.CHINA, "%02d", value)

    class DisplayTime constructor(
        @JvmField val mainText: String,
        @JvmField val secondsText: String,
        @JvmField val periodText: String,
        @JvmField val colonVisible: Boolean,
    ) {
        fun hasSmallSeconds(): Boolean = secondsText.isNotEmpty()
        fun hasPeriod(): Boolean = periodText.isNotEmpty()
    }
}
