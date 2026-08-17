package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

public class ClockViewTest {
    @Test
    public void detectsChineseTextForSystemFontFallback() {
        Assert.assertTrue(ClockView.containsChinese("2026年7月16日 星期四"));
        Assert.assertTrue(ClockView.containsChinese("Beijing 北京 28℃"));
        Assert.assertFalse(ClockView.containsChinese("2026/7/16 Thursday"));
        Assert.assertFalse(ClockView.containsChinese("Roboto 12:34"));
    }

    @Test
    public void keepsOnlyPanguSpacesFreeOfExtraTracking() {
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("中 A", 1));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("中 A", 2));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("A 中", 1));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("A 中", 2));
        Assert.assertTrue(ClockView.hasSupportingTrackingAt("A B", 1));
        Assert.assertTrue(ClockView.hasSupportingTrackingAt("A B", 2));
        Assert.assertTrue(ClockView.hasSupportingTrackingAt("AB", 1));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("AB", 0));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("AB", 2));

        String extensionB = "\uD840\uDC00";
        Assert.assertFalse(ClockView.hasSupportingTrackingAt(extensionB + " A", 2));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt(extensionB + " A", 3));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("A " + extensionB, 1));
        Assert.assertFalse(ClockView.hasSupportingTrackingAt("A " + extensionB, 2));

        String calendarEmoji = "\uD83D\uDCC5";
        Assert.assertTrue(ClockView.hasSupportingTrackingAt(calendarEmoji + " A", 2));
        Assert.assertTrue(ClockView.hasSupportingTrackingAt(calendarEmoji + " A", 3));
    }

    @Test
    public void loopingMarqueeAlwaysMovesForwardAndWrapsAtCycleBoundary() {
        Assert.assertEquals(0f, ClockView.loopingMarqueeOffset(0L, 300f, 40f), 0f);
        Assert.assertEquals(40f, ClockView.loopingMarqueeOffset(1000L, 300f, 40f), 0f);
        Assert.assertEquals(20f, ClockView.loopingMarqueeOffset(8000L, 300f, 40f), 0f);
        Assert.assertEquals(0f, ClockView.loopingMarqueeOffset(1000L, 0f, 40f), 0f);
    }

    @Test
    public void oneShotMarqueeReservesBothFadedEdges() {
        Assert.assertEquals(248f,
                ClockView.oneShotMarqueeDistance(500f, 300f, 24f), 0f);
        Assert.assertEquals(0f,
                ClockView.oneShotMarqueeDistance(200f, 300f, 24f), 0f);
    }

    @Test
    public void oneShotMarqueePausesThenStopsAtReadableEnd() {
        Assert.assertEquals(0f,
                ClockView.oneShotMarqueeOffset(500L, 1000L, 248f, 40f), 0f);
        Assert.assertEquals(40f,
                ClockView.oneShotMarqueeOffset(2000L, 1000L, 248f, 40f), 0f);
        Assert.assertEquals(248f,
                ClockView.oneShotMarqueeOffset(10000L, 1000L, 248f, 40f), 0f);
    }
}
