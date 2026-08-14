package com.clockmods.ui;

import android.view.Gravity;

import org.junit.Assert;
import org.junit.Test;

public class ClockTextLayoutTest {
    @Test
    public void textHeightUsesFontMetricsInsteadOfRenderedDigits() {
        Assert.assertEquals(92, ClockTextLayout.stableTextHeight(
                -76, -68, 18, 24, false, 3, 3, 0));
        Assert.assertEquals(106, ClockTextLayout.stableTextHeight(
                -76, -68, 18, 24, true, 3, 3, 0));
        Assert.assertEquals(120, ClockTextLayout.stableTextHeight(
                -76, -68, 18, 24, false, 3, 3, 120));
    }

    @Test
    public void baselineRemainsFixedForEachVerticalGravity() {
        Assert.assertEquals(71, ClockTextLayout.stableBaseline(
                100, -68, 18, 3, 3, Gravity.TOP));
        Assert.assertEquals(75, ClockTextLayout.stableBaseline(
                100, -68, 18, 3, 3, Gravity.CENTER_VERTICAL));
        Assert.assertEquals(79, ClockTextLayout.stableBaseline(
                100, -68, 18, 3, 3, Gravity.BOTTOM));
    }
}
