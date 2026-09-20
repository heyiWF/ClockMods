package com.clockmods.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTimeFormatterTest {
    @Test
    fun formatsTimeAccordingToSecondsAndColonSettings() {
        val regular = ClockTimeFormatter.format(8, 5, 7, true, false, false, true, false)
        assertEquals("08:05:07", regular.mainText)
        assertFalse(regular.hasSmallSeconds())

        val hiddenSeconds = ClockTimeFormatter.format(8, 5, 7, false, false, true, true, false)
        assertEquals("08:05", hiddenSeconds.mainText)
        assertFalse(hiddenSeconds.hasSmallSeconds())

        val blinking = ClockTimeFormatter.format(8, 5, 7, true, true, false, true, false)
        assertEquals("08:05:07", blinking.mainText)
        assertFalse(blinking.colonVisible)
    }

    @Test
    fun keepsSmallSecondsSeparate() {
        val displayTime = ClockTimeFormatter.format(8, 5, 7, true, false, true, true, false)
        assertEquals("08:05", displayTime.mainText)
        assertEquals("07", displayTime.secondsText)
        assertTrue(displayTime.hasSmallSeconds())
    }

    @Test
    fun formatsLocalizedTwelveHourPeriods() {
        val midnight = ClockTimeFormatter.format(0, 5, 7, false, false, false, false, false)
        assertEquals("12:05", midnight.mainText)
        assertEquals("\u4e0a\u5348", midnight.periodText)

        val noon = ClockTimeFormatter.format(12, 5, 7, false, false, false, false, true)
        assertEquals("12:05", noon.mainText)
        assertEquals("PM", noon.periodText)
    }

    @Test
    fun formatsHourlyAndHalfHourlyChimes() {
        assertEquals("13:00", ClockTimeFormatter.formatHourlyChime(13, 0, true, false))
        assertEquals("13:30", ClockTimeFormatter.formatHourlyChime(13, 30, true, false))
        assertEquals("\u4e0b\u53481:00", ClockTimeFormatter.formatHourlyChime(13, 0, false, false))
        assertEquals("12:00 PM", ClockTimeFormatter.formatHourlyChime(12, 0, false, true))
    }
}
