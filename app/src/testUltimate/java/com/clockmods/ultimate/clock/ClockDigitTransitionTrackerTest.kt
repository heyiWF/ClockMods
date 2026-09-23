package com.clockmods.ultimate.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockDigitTransitionTrackerTest {
    @Test
    fun onlyChangedDigitsAreSelected() {
        assertEquals(listOf(1, 3, 4, 6, 7), changedDigitPositions("12:59:09", "13:00:10"))
        assertEquals(emptyList<Int>(), changedDigitPositions("12:59", "12:59"))
        assertEquals(emptyList<Int>(), changedDigitPositions("9:59", "10:00"))
    }

    @Test
    fun separateClockLinesKeepTheirOwnPreviousValues() {
        val tracker = ClockDigitTransitionTracker()
        tracker.beginFrame()
        assertNull(tracker.previousFor("12"))
        assertNull(tracker.previousFor("59"))

        tracker.beginFrame()
        assertEquals("12", tracker.previousFor("13"))
        assertEquals("59", tracker.previousFor("00"))

        tracker.beginFrame()
        assertEquals("12", tracker.previousFor("13"))
        assertEquals("59", tracker.previousFor("00"))
    }
}
