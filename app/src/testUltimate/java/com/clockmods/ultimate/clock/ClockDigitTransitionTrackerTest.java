package com.clockmods.ultimate.clock;

import org.junit.Assert;
import org.junit.Test;

public class ClockDigitTransitionTrackerTest {
    @Test public void oldDigitIsClearedWhenMotionCompletes() {
        ClockDigitTransitionTracker tracker = new ClockDigitTransitionTracker();
        tracker.beginFrame();
        Assert.assertNull(tracker.previousFor("12:34:56", false));
        tracker.beginFrame();
        Assert.assertEquals("12:34:56", tracker.previousFor("12:34:57", true));
        tracker.beginFrame();
        Assert.assertEquals("12:34:56", tracker.previousFor("12:34:57", true));
        tracker.beginFrame();
        Assert.assertNull(tracker.previousFor("12:34:57", false));
        tracker.beginFrame();
        Assert.assertEquals("12:34:57", tracker.previousFor("12:34:58", true));
    }

    @Test public void onlyChangedDigitsAnimate() {
        Assert.assertTrue(ClockDigitTransitionTracker.changedDigit("12:34", "12:35", 4));
        Assert.assertFalse(ClockDigitTransitionTracker.changedDigit("12:34", "12:35", 2));
        Assert.assertFalse(ClockDigitTransitionTracker.changedDigit("12:34", "12:3", 3));
    }
}
