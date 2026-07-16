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
}