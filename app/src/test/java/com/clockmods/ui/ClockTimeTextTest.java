package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

public class ClockTimeTextTest {
    @Test
    public void colonRisesToTheOpticalCentreOfTheDigits() {
        // A digit is drawn from the baseline up to the figure height while the colon only
        // reaches the x-height, so its centre sits below the digits' centre.
        Assert.assertEquals(-10f, ClockTimeText.colonBaselineOffset(-70, 0, -50, 0), 0f);
        Assert.assertEquals(-14f, ClockTimeText.colonBaselineOffset(-96, -4, -68, -4), 0f);
    }

    @Test
    public void colonStaysPutWhenItIsAlreadyCentred() {
        Assert.assertEquals(0f, ClockTimeText.colonBaselineOffset(-70, 0, -55, -15), 0f);
    }

    @Test
    public void glyphsWithoutInkProduceNoShift() {
        Assert.assertEquals(0f, ClockTimeText.colonBaselineOffset(0, 0, -50, 0), 0f);
        Assert.assertEquals(0f, ClockTimeText.colonBaselineOffset(-70, 0, 0, 0), 0f);
    }

    @Test
    public void raisedColonNeverReachesAboveTheDigits() {
        int digitTop = -70;
        int colonTop = -50;
        float offset = ClockTimeText.colonBaselineOffset(digitTop, 0, colonTop, 0);
        Assert.assertTrue(colonTop + offset > digitTop);
    }
}
