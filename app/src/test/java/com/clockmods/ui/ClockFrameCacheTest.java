package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Pins the reuse rules of {@link ClockFrameCache}: a frame may only skip measuring when nothing
 * that can move a baseline changed.
 */
public class ClockFrameCacheTest {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 480;
    private static final int GENERATION = 3;
    private static final int FLAGS = 1;

    private ClockFrameCache cache;

    @Before
    public void setUp() {
        cache = new ClockFrameCache();
        cache.storeKey(WIDTH, HEIGHT, GENERATION, 0.88f, 0.55f, FLAGS, 8, 2, 0,
                "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", "");
    }

    private boolean matches(int mainLength, String dateText) {
        return cache.matches(WIDTH, HEIGHT, GENERATION, 0.88f, 0.55f, FLAGS, mainLength, 2, 0,
                dateText, "丙午[马]年十月初九", "");
    }

    @Test
    public void identicalFrameReusesTheMeasurement() {
        Assert.assertTrue(matches(8, "2026 年 11 月 18 日 星期三"));
    }

    @Test
    public void differentDigitsWithTheSameLengthReuseIt() {
        // "17:48:51" and "08:00:00" are equally wide because every digit is measured at the
        // widest digit width; only the character count can change the solved size. The digit
        // values themselves are deliberately not part of the key, so a tick that only replaces
        // digits still hits the cache.
        Assert.assertTrue(cache.matches(WIDTH, HEIGHT, GENERATION, 0.88f, 0.55f, FLAGS, 8, 2, 0,
                "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
    }

    @Test
    public void anotherCharacterCountIsMeasuredAgain() {
        // 9:59 -> 10:00 adds a digit, which changes the width the size solver sees.
        Assert.assertFalse(matches(7, "2026 年 11 月 18 日 星期三"));
    }

    @Test
    public void dateTextChangeIsMeasuredAgain() {
        Assert.assertFalse(matches(8, "2026 年 11 月 19 日 星期四"));
    }

    @Test
    public void anyKeyFieldChangeIsMeasuredAgain() {
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT + 1, GENERATION, 0.88f, 0.55f, FLAGS, 8, 2,
                0, "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT, GENERATION + 1, 0.88f, 0.55f, FLAGS, 8, 2,
                0, "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT, GENERATION, 0.9f, 0.55f, FLAGS, 8, 2, 0,
                "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT, GENERATION, 0.88f, 0.6f, FLAGS, 8, 2, 0,
                "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT, GENERATION, 0.88f, 0.55f, FLAGS + 1, 8, 2,
                0, "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT, GENERATION, 0.88f, 0.55f, FLAGS, 8, 3, 0,
                "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT, GENERATION, 0.88f, 0.55f, FLAGS, 8, 2, 1,
                "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", ""));
        Assert.assertFalse(cache.matches(WIDTH, HEIGHT, GENERATION, 0.88f, 0.55f, FLAGS, 8, 2, 0,
                "2026 年 11 月 18 日 星期三", "丙午[马]年十月初九", "hello"));
    }

    @Test
    public void invalidatedCacheIsNeverReused() {
        cache.invalidate();
        Assert.assertFalse(matches(8, "2026 年 11 月 18 日 星期三"));
    }
}
