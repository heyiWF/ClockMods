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
        assertNull(tracker.previousFor("12", true))
        assertNull(tracker.previousFor("59", true))

        tracker.beginFrame()
        assertEquals("12", tracker.previousFor("13", true))
        assertEquals("59", tracker.previousFor("00", true))

        tracker.beginFrame()
        assertEquals("12", tracker.previousFor("13", true))
        assertEquals("59", tracker.previousFor("00", true))

        tracker.beginFrame()
        assertNull(tracker.previousFor("13", false))
        assertNull(tracker.previousFor("00", false))

        // Seconds start another animation while the main hour:minute line stays unchanged.
        tracker.beginFrame()
        assertNull(tracker.previousFor("13", true))
        assertEquals("00", tracker.previousFor("01", true))

        tracker.beginFrame()
        assertEquals("13", tracker.previousFor("14", true))
        assertEquals("01", tracker.previousFor("02", true))
    }
}
