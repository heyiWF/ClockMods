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
}