package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

public class ClockLayoutCalculatorTest {
    @Test
    public void formatsTimeAccordingToSecondAndColonSettings() {
    ClockTimeFormatter.DisplayTime regular = ClockTimeFormatter.format(
        8, 5, 7, true, false, false, true, false);
    Assert.assertEquals("08:05:07", regular.mainText);
    Assert.assertFalse(regular.hasSmallSeconds());

    ClockTimeFormatter.DisplayTime hiddenSeconds = ClockTimeFormatter.format(
        8, 5, 7, false, false, true, true, false);
    Assert.assertEquals("08:05", hiddenSeconds.mainText);
    Assert.assertFalse(hiddenSeconds.hasSmallSeconds());
    }

    @Test
    public void blinkingColonAlternatesOnSecondBoundary() {
    Assert.assertEquals("08:05:06", ClockTimeFormatter.format(
        8, 5, 6, true, true, false, true, false).mainText);
    ClockTimeFormatter.DisplayTime hiddenColons = ClockTimeFormatter.format(
        8, 5, 7, true, true, false, true, false);
    Assert.assertEquals("08:05:07", hiddenColons.mainText);
    Assert.assertFalse(hiddenColons.colonVisible);
    }

    @Test
    public void smallSecondsAreSeparatedWithoutColon() {
    ClockTimeFormatter.DisplayTime displayTime = ClockTimeFormatter.format(
        8, 5, 7, true, false, true, true, false);

    Assert.assertEquals("08:05", displayTime.mainText);
    Assert.assertEquals("07", displayTime.secondsText);
    Assert.assertTrue(displayTime.hasSmallSeconds());
    }

        @Test
        public void formatsLocalizedTwelveHourPeriods() {
        ClockTimeFormatter.DisplayTime midnight = ClockTimeFormatter.format(
            0, 5, 7, false, false, false, false, false);
        Assert.assertEquals("12:05", midnight.mainText);
        Assert.assertEquals("上午", midnight.periodText);

        ClockTimeFormatter.DisplayTime noon = ClockTimeFormatter.format(
            12, 5, 7, false, false, false, false, true);
        Assert.assertEquals("12:05", noon.mainText);
        Assert.assertEquals("PM", noon.periodText);

        ClockTimeFormatter.DisplayTime afternoon = ClockTimeFormatter.format(
            15, 5, 7, false, false, false, false, false);
        Assert.assertEquals("03:05", afternoon.mainText);
        Assert.assertEquals("下午", afternoon.periodText);
        }

    @Test
    public void usesSingleDateLineOnlyWhenLandscapeAndTextFits() {
        Assert.assertTrue(ClockLayoutCalculator.shouldUseSingleDateLine(true, 800f, 1000));
        Assert.assertFalse(ClockLayoutCalculator.shouldUseSingleDateLine(false, 800f, 1000));
        Assert.assertFalse(ClockLayoutCalculator.shouldUseSingleDateLine(true, 950f, 1000));
    }

    @Test
    public void centerCropScaleFillsBothAxes() {
        Assert.assertEquals(2f, ClockLayoutCalculator.centerCropScale(1000, 500, 1000, 1000), 0.001f);
        Assert.assertEquals(2f, ClockLayoutCalculator.centerCropScale(500, 1000, 1000, 1000), 0.001f);
    }
}