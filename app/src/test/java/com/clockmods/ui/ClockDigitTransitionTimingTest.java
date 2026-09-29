package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

public class ClockDigitTransitionTimingTest {
    private static final float FONT_SIZE = 46f;

    @Test public void supportingSlideTravelsFurtherThanAFontRelativeDigitSweep() {
        // "深圳宝安 28°C 晴" at 46 px measures roughly 357 px, so a plain font-relative travel
        // (0.27 em) would shift it by only 12 px — invisible on a line a dozen glyphs wide.
        float travel = ClockDigitTransitionTiming.supportingSlideDistance(357f, FONT_SIZE);
        Assert.assertTrue(travel
                > FONT_SIZE * ClockDigitTransitionTiming.SLIDE_DISTANCE_FRACTION * 4f);
    }

    @Test public void shortSupportingLineStillMovesAtLeastThreeQuartersOfAnEm() {
        Assert.assertEquals(FONT_SIZE * ClockDigitTransitionTiming.SUPPORTING_SLIDE_MIN_EM,
                ClockDigitTransitionTiming.supportingSlideDistance(FONT_SIZE, FONT_SIZE), 1e-3f);
        Assert.assertEquals(FONT_SIZE * ClockDigitTransitionTiming.SUPPORTING_SLIDE_MIN_EM,
                ClockDigitTransitionTiming.supportingSlideDistance(0f, FONT_SIZE), 1e-3f);
    }

    @Test public void longSupportingLineNeverSweepsMoreThanTwoAndAHalfEm() {
        float limit = FONT_SIZE * ClockDigitTransitionTiming.SUPPORTING_SLIDE_MAX_EM;
        Assert.assertEquals(limit,
                ClockDigitTransitionTiming.supportingSlideDistance(4000f, FONT_SIZE), 1e-3f);
        Assert.assertEquals(357f * ClockDigitTransitionTiming.SUPPORTING_SLIDE_SHARE,
                ClockDigitTransitionTiming.supportingSlideDistance(357f, FONT_SIZE), 1e-3f);
    }

    @Test public void emptyLineOrFontHasNoTravel() {
        Assert.assertEquals(0f,
                ClockDigitTransitionTiming.supportingSlideDistance(357f, 0f), 0f);
        Assert.assertEquals(0f,
                ClockDigitTransitionTiming.supportingSlideDistance(357f, -1f), 0f);
    }

    @Test public void sweepingCarriesTheLineHalfAgainAsFarAsAFontRelativeSweepWould() {
        // The travel was lengthened by half; a regression here silently undoes the effect.
        Assert.assertEquals(.27f, ClockDigitTransitionTiming.SLIDE_DISTANCE_FRACTION, 0f);
        Assert.assertEquals(.18f, ClockDigitTransitionTiming.SUPPORTING_SLIDE_SHARE, 0f);
        Assert.assertEquals(.75f, ClockDigitTransitionTiming.SUPPORTING_SLIDE_MIN_EM, 0f);
        Assert.assertEquals(2.4f, ClockDigitTransitionTiming.SUPPORTING_SLIDE_MAX_EM, 0f);
    }

    @Test public void sweepingIsSlowerThanTheInPlaceTransitions() {
        Assert.assertTrue(ClockDigitTransitionTiming.SLIDE_DURATION_MILLIS
                > ClockDigitTransitionTiming.DURATION_MILLIS);
        Assert.assertTrue(ClockDigitTransitionTiming.SUPPORTING_DURATION_MILLIS
                > ClockDigitTransitionTiming.SLIDE_DURATION_MILLIS);
    }

    @Test public void sweepFadeSpansTheWholeTravel() {
        // Both copies stay visible for as long as they are moving; a ramp that finished early
        // would hide the travelled distance behind two already transparent copies.
        Assert.assertEquals(1f, ClockDigitTransitionTiming.sweepAlpha(0f, true), 1e-4f);
        Assert.assertEquals(0f, ClockDigitTransitionTiming.sweepAlpha(0f, false), 1e-4f);
        Assert.assertEquals(.5f, ClockDigitTransitionTiming.sweepAlpha(.5f, true), 1e-4f);
        Assert.assertEquals(.5f, ClockDigitTransitionTiming.sweepAlpha(.5f, false), 1e-4f);
        Assert.assertEquals(0f, ClockDigitTransitionTiming.sweepAlpha(1f, true), 1e-4f);
        Assert.assertEquals(1f, ClockDigitTransitionTiming.sweepAlpha(1f, false), 1e-4f);
    }

    @Test public void sweepFadeStaysVisiblePastTheHalfwayMarkOfAFastEase() {
        // Ease-out front-loads the travel: the copies are already 79 % done a third of the way
        // into the run, so the fade must not be finished by then as well.
        float travel = ClockDigitTransitionTiming.easeOutCubic(.35f);
        Assert.assertTrue(travel > .7f);
        Assert.assertTrue(ClockDigitTransitionTiming.sweepAlpha(travel, true) > .2f);
        Assert.assertTrue(ClockDigitTransitionTiming.sweepAlpha(travel, false) > .7f);
    }

    @Test public void sweepFadeClampsTravelOutsideItsRange() {
        Assert.assertEquals(1f, ClockDigitTransitionTiming.sweepAlpha(-2f, true), 1e-4f);
        Assert.assertEquals(0f, ClockDigitTransitionTiming.sweepAlpha(3f, true), 1e-4f);
        Assert.assertEquals(1f, ClockDigitTransitionTiming.sweepAlpha(3f, false), 1e-4f);
    }

    @Test public void sweepNeverPushesInkPastTheCanvasEdge() {
        // The weather line on a 2560 px canvas sits 640 px wide at x = 1840, i.e. only 80 px of
        // room; a 110 px outward travel would have taken its end off the screen, which reads as a
        // clipped line.
        float canvasWidth = 2560f;
        float inkLeft = 1840f;
        float inkWidth = 640f;
        float distance = 110f;
        float outward = ClockDigitTransitionTiming.outwardSweepTravel(distance, inkLeft, inkWidth,
                canvasWidth);
        Assert.assertEquals(80f, outward, 1e-3f);
        Assert.assertEquals(canvasWidth, inkLeft + inkWidth + outward, 1e-3f);
    }

    @Test public void sweepKeepsItsFullTravelWhenTheCanvasHasRoom() {
        Assert.assertEquals(110f, ClockDigitTransitionTiming.outwardSweepTravel(110f, 200f, 640f,
                2560f), 1e-3f);
        Assert.assertEquals(110f, ClockDigitTransitionTiming.inwardSweepTravel(110f, 200f), 1e-3f);
    }

    @Test public void inwardTravelDoesNotDependOnTheRoomToTheRight() {
        // A line hugging the right edge can still sweep in from the left at full distance.
        Assert.assertEquals(110f, ClockDigitTransitionTiming.inwardSweepTravel(110f, 1840f), 1e-3f);
    }

    @Test public void sweepTravelIsNeverNegative() {
        Assert.assertEquals(0f,
                ClockDigitTransitionTiming.outwardSweepTravel(110f, 2400f, 640f, 2560f), 1e-3f);
        Assert.assertEquals(0f, ClockDigitTransitionTiming.inwardSweepTravel(110f, 0f), 1e-3f);
        Assert.assertEquals(0f, ClockDigitTransitionTiming.inwardSweepTravel(110f, -30f), 1e-3f);
    }

    @Test public void easeOutCubicHitsBothEnds() {
        Assert.assertEquals(1f, ClockDigitTransitionTiming.easeOutCubic(1f), 0f);
        Assert.assertEquals(0f, ClockDigitTransitionTiming.easeOutCubic(0f), 0f);
    }
}
