package com.clockmods.sdk.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockOverlayBoundsTest {
    @Test
    fun preservesOrderedCoordinates() {
        val bounds = ClockOverlayBounds(1f, 2f, 11f, 22f)

        assertBounds(bounds, 1f, 2f, 11f, 22f)
    }

    @Test
    fun normalizesReversedCoordinatesOnBothAxes() {
        val bounds = ClockOverlayBounds(11f, 22f, 1f, 2f)

        assertBounds(bounds, 1f, 2f, 11f, 22f)
        assertEquals(10f, bounds.getWidth(), 0f)
        assertEquals(20f, bounds.getHeight(), 0f)
    }

    @Test
    fun normalizesEachAxisIndependently() {
        assertBounds(ClockOverlayBounds(11f, 2f, 1f, 22f), 1f, 2f, 11f, 22f)
        assertBounds(ClockOverlayBounds(1f, 22f, 11f, 2f), 1f, 2f, 11f, 22f)
    }

    @Test
    fun overlapUsesOpenEdgesForDegenerateBounds() {
        val bounds = ClockOverlayBounds(4f, 7f, 4f, 7f)

        assertEquals(0f, bounds.getWidth(), 0f)
        assertEquals(0f, bounds.getHeight(), 0f)
        assertFalse(bounds.spansHorizontally(4f, 4f))
        assertFalse(bounds.spansVertically(7f, 7f))
        assertTrue(bounds.spansHorizontally(3f, 5f))
        assertTrue(bounds.spansVertically(6f, 8f))
    }

    private fun assertBounds(
        bounds: ClockOverlayBounds,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        assertEquals(left, bounds.getLeft(), 0f)
        assertEquals(top, bounds.getTop(), 0f)
        assertEquals(right, bounds.getRight(), 0f)
        assertEquals(bottom, bounds.getBottom(), 0f)
    }
}
