package com.clockmods.ui

import org.junit.Assert
import org.junit.Test

class ClockTransitionTimingTest {
    private val fontSize = 46f

    @Test
    fun supportingSlideTravelsFurtherThanAFontRelativeDigitSweep() {
        // "深圳宝安 28°C 晴" at 46 px measures roughly 357 px, so a plain font-relative travel
        // (0.27 em) would shift it by only 12 px — invisible on a line a dozen glyphs wide.
        val travel = ClockTransitionTiming.supportingSlideDistance(357f, fontSize)
        Assert.assertTrue(travel > fontSize * ClockTransitionTiming.SLIDE_DISTANCE_FRACTION * 4f)
    }

    @Test
    fun shortSupportingLineStillMovesAtLeastThreeQuartersOfAnEm() {
        Assert.assertEquals(fontSize * ClockTransitionTiming.SUPPORTING_SLIDE_MIN_EM,
            ClockTransitionTiming.supportingSlideDistance(fontSize, fontSize), 1e-3f)
        Assert.assertEquals(fontSize * ClockTransitionTiming.SUPPORTING_SLIDE_MIN_EM,
            ClockTransitionTiming.supportingSlideDistance(0f, fontSize), 1e-3f)
    }

    @Test
    fun longSupportingLineNeverSweepsMoreThanTwoAnAHalfEm() {
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
    fun sweepingCarriesTheLineHalfAgainAsFarAsAFontRelativeSweepWould() {
        // The travel was lengthened by half; a regression here silently undoes the effect.
        Assert.assertEquals(.27f, ClockTransitionTiming.SLIDE_DISTANCE_FRACTION, 0f)
        Assert.assertEquals(.18f, ClockTransitionTiming.SUPPORTING_SLIDE_SHARE, 0f)
        Assert.assertEquals(.75f, ClockTransitionTiming.SUPPORTING_SLIDE_MIN_EM, 0f)
        Assert.assertEquals(2.4f, ClockTransitionTiming.SUPPORTING_SLIDE_MAX_EM, 0f)
    }

    @Test
    fun sweepingIsSlowerThanTheInPlaceTransitions() {
        Assert.assertTrue(ClockTransitionTiming.SLIDE_DURATION_MILLIS >
            ClockTransitionTiming.DURATION_MILLIS)
        Assert.assertTrue(ClockTransitionTiming.SUPPORTING_DURATION_MILLIS >
            ClockTransitionTiming.SLIDE_DURATION_MILLIS)
    }

    @Test
    fun sweepFadeSpansTheWholeTravel() {
        // Both copies stay visible for as long as they are moving; a ramp that finished early
        // would hide the travelled distance behind two already transparent copies.
        Assert.assertEquals(1f, ClockTransitionTiming.sweepAlpha(0f, true), 1e-4f)
        Assert.assertEquals(0f, ClockTransitionTiming.sweepAlpha(0f, false), 1e-4f)
        Assert.assertEquals(.5f, ClockTransitionTiming.sweepAlpha(.5f, true), 1e-4f)
        Assert.assertEquals(.5f, ClockTransitionTiming.sweepAlpha(.5f, false), 1e-4f)
        Assert.assertEquals(0f, ClockTransitionTiming.sweepAlpha(1f, true), 1e-4f)
        Assert.assertEquals(1f, ClockTransitionTiming.sweepAlpha(1f, false), 1e-4f)
    }

    @Test
    fun sweepFadeStaysOpaquePastTheHalfwayMarkOfAFastEase() {
        // Ease-out front-loads the travel: the copies are already 79 % done a third of the way
        // into the run, so the fade must not be finished by then as well.
        val travel = ClockTransitionTiming.easeOutCubic(.35f)
        Assert.assertTrue(travel > .7f)
        Assert.assertTrue(ClockTransitionTiming.sweepAlpha(travel, true) > .2f)
        Assert.assertTrue(ClockTransitionTiming.sweepAlpha(travel, false) > .7f)
    }

    @Test
    fun sweepFadeClampsTravelOutsideItsRange() {
        Assert.assertEquals(1f, ClockTransitionTiming.sweepAlpha(-2f, true), 1e-4f)
        Assert.assertEquals(0f, ClockTransitionTiming.sweepAlpha(3f, true), 1e-4f)
        Assert.assertEquals(1f, ClockTransitionTiming.sweepAlpha(3f, false), 1e-4f)
    }

    @Test
    fun sweepNeverPushesInkPastTheCanvasEdge() {
        // The weather line on a 2560 px canvas sits 640 px wide at x = 1840, i.e. only 80 px of
        // room; a 110 px outward travel would have taken its end off the screen, which reads as a
        // clipped line.
        val canvasWidth = 2560f
        val inkLeft = 1840f
        val inkWidth = 640f
        val distance = 110f
        val outward = ClockTransitionTiming.outwardSweepTravel(distance, inkLeft, inkWidth,
            canvasWidth)
        Assert.assertEquals(80f, outward, 1e-3f)
        Assert.assertEquals(canvasWidth, inkLeft + inkWidth + outward, 1e-3f)
    }

    @Test
    fun sweepKeepsItsFullTravelWhenTheCanvasHasRoom() {
        Assert.assertEquals(110f, ClockTransitionTiming.outwardSweepTravel(110f, 200f, 640f,
            2560f), 1e-3f)
        Assert.assertEquals(110f, ClockTransitionTiming.inwardSweepTravel(110f, 200f), 1e-3f)
    }

    @Test
    fun inwardTravelDoesNotDependOnTheRoomToTheRight() {
        // A line hugging the right edge can still sweep in from the left at full distance.
        Assert.assertEquals(110f, ClockTransitionTiming.inwardSweepTravel(110f, 1840f), 1e-3f)
    }

    @Test
    fun sweepTravelIsNeverNegative() {
        Assert.assertEquals(0f,
            ClockTransitionTiming.outwardSweepTravel(110f, 2400f, 640f, 2560f), 1e-3f)
        Assert.assertEquals(0f, ClockTransitionTiming.inwardSweepTravel(110f, 0f), 1e-3f)
        Assert.assertEquals(0f, ClockTransitionTiming.inwardSweepTravel(110f, -30f), 1e-3f)
    }

    @Test
    fun easeOutCubicHitsBothEnds() {
        Assert.assertEquals(1f, ClockTransitionTiming.easeOutCubic(1f), 0f)
        Assert.assertEquals(0f, ClockTransitionTiming.easeOutCubic(0f), 0f)
    }
}
