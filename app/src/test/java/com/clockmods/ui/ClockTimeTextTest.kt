package com.clockmods.ui

import org.junit.Assert
import org.junit.Test

class ClockTimeTextTest {
    @Test
    fun colonRisesToTheOpticalCentreOfTheDigits() {
        Assert.assertEquals(-10f, ClockTimeText.colonBaselineOffset(-70, 0, -50, 0), 0f)
        Assert.assertEquals(-14f, ClockTimeText.colonBaselineOffset(-96, -4, -68, -4), 0f)
    }

    @Test
    fun colonStaysPutWhenItIsAlreadyCentred() {
        Assert.assertEquals(0f, ClockTimeText.colonBaselineOffset(-70, 0, -55, -15), 0f)
    }

    @Test
    fun glyphsWithoutInkProduceNoShift() {
        Assert.assertEquals(0f, ClockTimeText.colonBaselineOffset(0, 0, -50, 0), 0f)
        Assert.assertEquals(0f, ClockTimeText.colonBaselineOffset(-70, 0, 0, 0), 0f)
    }

    @Test
    fun raisedColonNeverReachesAboveTheDigits() {
        val digitTop = -70
        val colonTop = -50
        val offset = ClockTimeText.colonBaselineOffset(digitTop, 0, colonTop, 0)
        Assert.assertTrue(colonTop + offset > digitTop)
    }
}
