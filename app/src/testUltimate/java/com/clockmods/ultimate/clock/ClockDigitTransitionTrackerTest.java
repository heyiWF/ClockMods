package com.clockmods.ultimate.clock;

import com.clockmods.ui.ClockDigitTransitionTiming;

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

    @Test public void aSingleChangedDigitMovesItsWholePair() {
        Assert.assertTrue(ClockDigitTransitionTiming.changedDigitPair("12:34:56", "12:35:56", 3));
        Assert.assertTrue(ClockDigitTransitionTiming.changedDigitPair("12:34:56", "12:35:56", 4));
        Assert.assertFalse(ClockDigitTransitionTiming.changedDigitPair("12:34:56", "12:35:56", 0));
        Assert.assertFalse(ClockDigitTransitionTiming.changedDigitPair("12:34:56", "12:35:56", 6));
    }

    @Test public void scanSweepsOutBeforeSweepingIn() {
        Assert.assertEquals(0f, ClockDigitTransitionTiming.scanPhase(0f), .0001f);
        Assert.assertTrue(ClockDigitTransitionTiming.scanPhase(.499f) > .99f);
        Assert.assertEquals(0f, ClockDigitTransitionTiming.scanPhase(.5f), .0001f);
        Assert.assertTrue(ClockDigitTransitionTiming.scanPhase(.25f) > .5f);
        Assert.assertTrue(ClockDigitTransitionTiming.scanPhase(.75f) > .5f);
        Assert.assertEquals(1f, ClockDigitTransitionTiming.scanPhase(1f), .0001f);
        Assert.assertEquals(80f, ClockDigitTransitionTiming.scanEdge(100f, 200f, 20f, 0f), .0001f);
        Assert.assertEquals(100f, ClockDigitTransitionTiming.scanEdge(100f, 200f, 20f, .5f), .0001f);
        Assert.assertEquals(320f, ClockDigitTransitionTiming.scanEdge(100f, 200f, 20f, 1f), .0001f);
    }
}
