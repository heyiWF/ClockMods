package com.clockmods.background;

import org.junit.Assert;
import org.junit.Test;

public class BackgroundRepositoryTest {
    @Test
    public void sampleSizeIsPowerOfTwoAndNotSmallerThanTarget() {
        Assert.assertEquals(4, BackgroundRepository.calculateInSampleSize(4000, 3000, 1000, 750));
        Assert.assertEquals(1, BackgroundRepository.calculateInSampleSize(1200, 800, 1080, 1920));
        Assert.assertEquals(1, BackgroundRepository.calculateInSampleSize(4000, 2000, 1200, 1200));
    }

    @Test
    public void compressedShortSideUsesDisplayLongSideForBothOrientations() {
        Assert.assertEquals(2400,
            BackgroundRepository.calculateCompressedShortSide(8000, 6000, 2400));
        Assert.assertEquals(2400,
            BackgroundRepository.calculateCompressedShortSide(6000, 8000, 2400));
        Assert.assertEquals(1080,
            BackgroundRepository.calculateCompressedShortSide(1920, 1080, 2400));
    }
}