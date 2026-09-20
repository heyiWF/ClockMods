package com.clockmods.ui

import org.junit.Assert
import org.junit.Test

class TextSpacingTest {
    @Test fun insertsSpaceBetweenChineseAndDigitsBothDirections() {
        Assert.assertEquals("2026 年 8 月 7 日", TextSpacing.pangu("2026年8月7日"))
        Assert.assertEquals("第 5 圈", TextSpacing.pangu("第5圈"))
    }

    @Test fun insertsSpaceBetweenChineseAndLatinLetters() {
        Assert.assertEquals("北京 AQI", TextSpacing.pangu("北京AQI"))
        Assert.assertEquals("Pro 版", TextSpacing.pangu("Pro版"))
    }

    @Test fun keepsWeekdaySuffixReadable() {
        Assert.assertEquals("2026 年 8 月 7 日 星期五", TextSpacing.pangu("2026年8月7日 星期五"))
    }

    @Test fun doesNotDoubleExistingSpaces() {
        Assert.assertEquals("2026 年 8 月", TextSpacing.pangu("2026 年 8 月"))
        Assert.assertEquals("北京 AQI", TextSpacing.pangu("北京 AQI"))
    }

    @Test fun leavesPureChineseUnchanged() {
        Assert.assertEquals("二〇二六年八月七日", TextSpacing.pangu("二〇二六年八月七日"))
        Assert.assertEquals("星期五", TextSpacing.pangu("星期五"))
    }

    @Test fun leavesPureLatinAndNumericUnchanged() {
        Assert.assertEquals("2026/8/7 Friday", TextSpacing.pangu("2026/8/7 Friday"))
        Assert.assertEquals("August 7, 2026", TextSpacing.pangu("August 7, 2026"))
    }

    @Test fun doesNotSpaceAroundSymbolsOrFullWidthPunctuation() {
        Assert.assertEquals("28℃", TextSpacing.pangu("28℃"))
        Assert.assertEquals("95%", TextSpacing.pangu("95%"))
        Assert.assertEquals("2026 年 8 月 7 日（星期五）", TextSpacing.pangu("2026年8月7日（星期五）"))
    }

    @Test fun handlesEmojiAndSurrogatePairsSafely() {
        Assert.assertEquals("📅2026 年", TextSpacing.pangu("📅2026年"))
        Assert.assertEquals("今天📅", TextSpacing.pangu("今天📅"))
    }

    @Test fun handlesNullAndShortInput() {
        Assert.assertNull(TextSpacing.pangu(null))
        Assert.assertEquals("", TextSpacing.pangu(""))
        Assert.assertEquals("年", TextSpacing.pangu("年"))
    }
}
