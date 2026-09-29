package com.clockmods.ui

import org.junit.Assert
import org.junit.Test

class ClockTransitionTimingTest {
    private val fontSize = 46f

    @Test
    fun supportingSlideTravelsFurtherThanAFontRelativeDigitSweep() {
        // "深圳宝安 28°C 晴" at 46 px measures roughly 357 px, so a plain font-relative travel
        // (0.18 em) would shift it by only 8 px — invisible on a line a dozen glyphs wide.
        val travel = ClockTransitionTiming.supportingSlideDistance(357f, fontSize)
        Assert.assertTrue(travel > fontSize * ClockTransitionTiming.SLIDE_DISTANCE_FRACTION * 4f)
    }

    @Test
    fun shortSupportingLineStillMovesAtLeastHalfAnEm() {
        Assert.assertEquals(fontSize * ClockTransitionTiming.SUPPORTING_SLIDE_MIN_EM,
            ClockTransitionTiming.supportingSlideDistance(fontSize, fontSize), 1e-3f)
        Assert.assertEquals(fontSize * ClockTransitionTiming.SUPPORTING_SLIDE_MIN_EM,
            ClockTransitionTiming.supportingSlideDistance(0f, fontSize), 1e-3f)
    }

    @Test
    fun longSupportingLineNeverSweepsMoreThanOneAndAHalfEm() {
        val limit = fontSize * ClockTransitionTiming.SUPPORTING_SLIDE_MAX_EM
        Assert.assertEquals(limit, ClockTransitionTiming.supportingSlideDistance(4000f, fontSize), 1e-3f)
        Assert.assertEquals(357f * ClockTransitionTiming.SUPPORTING_SLIDE_SHARE,
            ClockTransitionTiming.supportingSlideDistance(357f, fontSize), 1e-3f)
    }

    @Test
    fun emptyLineOrFontHasNoTravel() {
        Assert.assertEquals(0f, ClockTransitionTiming.supportingSlideDistance(357f, 0f), 0f)
        Assert.assertEquals(0f, ClockTransitionTiming.supportingSlideDistance(357f, -1f), 0f)
    }

    @Test
    fun digitSweepKeepsItsOwnFraction() {
        Assert.assertEquals(.18f, ClockTransitionTiming.SLIDE_DISTANCE_FRACTION, 0f)
        Assert.assertEquals(1f, ClockTransitionTiming.easeOutCubic(1f), 0f)
        Assert.assertEquals(0f, ClockTransitionTiming.easeOutCubic(0f), 0f)
    }
}
