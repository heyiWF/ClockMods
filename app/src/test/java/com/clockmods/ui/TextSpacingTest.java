package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

public class TextSpacingTest {
    @Test
    public void insertsSpaceBetweenChineseAndDigitsBothDirections() {
        Assert.assertEquals("2026 年 8 月 7 日", TextSpacing.pangu("2026年8月7日"));
        Assert.assertEquals("第 5 圈", TextSpacing.pangu("第5圈"));
    }

    @Test
    public void insertsSpaceBetweenChineseAndLatinLetters() {
        Assert.assertEquals("北京 AQI", TextSpacing.pangu("北京AQI"));
        Assert.assertEquals("Pro 版", TextSpacing.pangu("Pro版"));
    }

    @Test
    public void keepsWeekdaySuffixReadable() {
        // The default Chinese pattern renders through this helper.
        Assert.assertEquals("2026 年 8 月 7 日 星期五", TextSpacing.pangu("2026年8月7日 星期五"));
    }

    @Test
    public void doesNotDoubleExistingSpaces() {
        Assert.assertEquals("2026 年 8 月", TextSpacing.pangu("2026 年 8 月"));
        Assert.assertEquals("北京 AQI", TextSpacing.pangu("北京 AQI"));
    }

    @Test
    public void leavesPureChineseUnchanged() {
        Assert.assertEquals("二〇二六年八月七日", TextSpacing.pangu("二〇二六年八月七日"));
        Assert.assertEquals("星期五", TextSpacing.pangu("星期五"));
    }

    @Test
    public void leavesPureLatinAndNumericUnchanged() {
        Assert.assertEquals("2026/8/7 Friday", TextSpacing.pangu("2026/8/7 Friday"));
        Assert.assertEquals("August 7, 2026", TextSpacing.pangu("August 7, 2026"));
    }

    @Test
    public void doesNotSpaceAroundSymbolsOrFullWidthPunctuation() {
        // Degree/percent symbols are not alphanumeric, so pangu leaves them attached.
        Assert.assertEquals("28℃", TextSpacing.pangu("28℃"));
        Assert.assertEquals("95%", TextSpacing.pangu("95%"));
        // Full-width parentheses stay glued to the ideograph.
        Assert.assertEquals("2026 年 8 月 7 日（星期五）", TextSpacing.pangu("2026年8月7日（星期五）"));
    }

    @Test
    public void handlesEmojiAndSurrogatePairsSafely() {
        // Emoji are neither CJK nor Latin-alphanumeric: no space is inserted next to them,
        // and the surrogate pair is preserved intact.
        Assert.assertEquals("📅2026 年", TextSpacing.pangu("📅2026年"));
        Assert.assertEquals("今天📅", TextSpacing.pangu("今天📅"));
    }

    @Test
    public void handlesNullAndShortInput() {
        Assert.assertNull(TextSpacing.pangu(null));
        Assert.assertEquals("", TextSpacing.pangu(""));
        Assert.assertEquals("年", TextSpacing.pangu("年"));
    }
}
