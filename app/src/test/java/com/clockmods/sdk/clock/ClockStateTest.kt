package com.clockmods.sdk.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockStateTest {
    @Test
    fun transitionProgressIsNormalizedToUnitRange() {
        val cases = listOf(
            Float.NEGATIVE_INFINITY to 0f,
            -0.25f to 0f,
            0f to 0f,
            0.42f to 0.42f,
            1f to 1f,
            1.25f to 1f,
            Float.POSITIVE_INFINITY to 1f,
            Float.NaN to 1f,
        )

        cases.forEach { (input, expected) ->
            val state = ClockState.builder(0L)
                .timeTransitionProgress(input)
                .build()

            assertEquals(input.toString(), expected, state.getTimeTransitionProgress(), 0f)
        }
    }
}
