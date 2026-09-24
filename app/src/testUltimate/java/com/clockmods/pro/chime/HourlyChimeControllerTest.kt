package com.clockmods.pro.chime

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlyChimeControllerTest {
    @Test
    fun startsDuringFinalTwoSecondsBeforeHour() {
        val now = timeAt(13, 59, 58, 250)
        val chimeAt = Calendar.getInstance(TIME_ZONE).apply {
            timeInMillis = HourlyChimeController.upcomingChimeAtMillis(now, true, false)
        }
        assertEquals(14, chimeAt.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, chimeAt.get(Calendar.MINUTE))
        assertEquals(0, chimeAt.get(Calendar.SECOND))
        assertEquals(0, chimeAt.get(Calendar.MILLISECOND))
    }

    @Test
    fun startsDuringFinalTwoSecondsBeforeHalfHourWhenEnabled() {
        val now = timeAt(13, 29, 58, 250)
        val chimeAt = Calendar.getInstance(TIME_ZONE).apply {
            timeInMillis = HourlyChimeController.upcomingChimeAtMillis(now, true, true)
        }
        assertEquals(13, chimeAt.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, chimeAt.get(Calendar.MINUTE))
        assertEquals(0, chimeAt.get(Calendar.SECOND))
        assertEquals(0, chimeAt.get(Calendar.MILLISECOND))
    }

    @Test
    fun halfHourDoesNotStartWhenItsOptionIsDisabled() {
        assertEquals(
            Long.MIN_VALUE,
            HourlyChimeController.upcomingChimeAtMillis(timeAt(13, 29, 59, 0), true, false),
        )
    }

    @Test
    fun halfHourCanRunIndependentlyWhenHourlyIsDisabled() {
        assertEquals(
            13 * 60 + 30,
            minuteOf(HourlyChimeController.upcomingChimeAtMillis(timeAt(13, 29, 58, 250), false, true)),
        )
    }

    @Test
    fun noChimeIsScheduledWhenBothOptionsAreDisabled() {
        assertEquals(
            Long.MIN_VALUE,
            HourlyChimeController.upcomingChimeAtMillis(timeAt(13, 59, 59, 0), false, false),
        )
    }

    @Test
    fun doesNotStartBeforeFinalTwoSecondsOrAfterHour() {
        assertEquals(
            Long.MIN_VALUE,
            HourlyChimeController.upcomingChimeAtMillis(timeAt(13, 59, 57, 999), true, true),
        )
        assertEquals(
            Long.MIN_VALUE,
            HourlyChimeController.upcomingChimeAtMillis(timeAt(14, 0, 0, 0), true, true),
        )
    }

    @Test
    fun hourlyCueRunsFromMinute58ToMinute03Exactly() {
        val target = timeAt(14, 0, 0, 0).timeInMillis
        assertEquals(null, HourlyChimeController.progressAtMillis(target - 2_001, target))
        assertEquals(0f, HourlyChimeController.progressAtMillis(target - 2_000, target))
        assertEquals(.4f, HourlyChimeController.progressAtMillis(target, target))
        assertTrue(HourlyChimeController.progressAtMillis(target + 2_999, target)!! < 1f)
        assertEquals(null, HourlyChimeController.progressAtMillis(target + 3_000, target))
    }

    @Test
    fun halfHourCueHasTheSameWallClockWindow() {
        val target = timeAt(13, 30, 0, 0).timeInMillis
        assertEquals(0f, HourlyChimeController.progressAtMillis(target - 2_000, target))
        assertEquals(.4f, HourlyChimeController.progressAtMillis(target, target))
        assertEquals(null, HourlyChimeController.progressAtMillis(target + 3_000, target))
    }

    @Test
    fun quietHoursHandleNormalAndOvernightBoundaries() {
        assertTrue(HourlyChimeController.isQuietAtMinute(22 * 60, 22 * 60, 7 * 60))
        assertTrue(HourlyChimeController.isQuietAtMinute(6 * 60 + 59, 22 * 60, 7 * 60))
        assertFalse(HourlyChimeController.isQuietAtMinute(7 * 60, 22 * 60, 7 * 60))
        assertTrue(HourlyChimeController.isQuietAtMinute(12 * 60, 8 * 60, 18 * 60))
        assertFalse(HourlyChimeController.isQuietAtMinute(18 * 60, 8 * 60, 18 * 60))
        assertTrue(HourlyChimeController.isQuietAtMinute(12 * 60, 0, 0))
    }

    @Test
    fun configuredTimeZoneIsUsedWhenPresent() {
        assertEquals("Asia/Shanghai", HourlyChimeController.resolveTimeZone("Asia/Shanghai").id)
    }

    private fun minuteOf(millis: Long): Int = Calendar.getInstance(TIME_ZONE).run {
        timeInMillis = millis
        get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
    }

    private fun timeAt(hour: Int, minute: Int, second: Int, millis: Int): Calendar =
        Calendar.getInstance(TIME_ZONE).apply {
            set(2026, Calendar.JULY, 24, hour, minute, second)
            set(Calendar.MILLISECOND, millis)
        }

    private companion object {
        val TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }
}
