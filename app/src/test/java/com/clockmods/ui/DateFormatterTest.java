package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateFormatterTest {
    private static Calendar example() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2026, Calendar.AUGUST, 7, 12, 0, 0);
        return cal;
    }

    @Test
    public void rendersTheDefaultChineseDateLine() {
        Assert.assertEquals("2026年8月7日 星期五", DateFormatter.format(example()));
    }

    @Test
    public void rendersArabicTokensWithoutExtraSpacing() {
        Assert.assertEquals("2026年8月7日",
                DateFormatter.format("yyyy年M月d日", example()));
        Assert.assertEquals("2026年08月07日",
                DateFormatter.format("yyyy年MM月dd日", example()));
        Assert.assertEquals("26", DateFormatter.format("yy", example()));
    }

    @Test
    public void rendersChineseNumeralTokens() {
        Assert.assertEquals("二〇二六年八月七日",
                DateFormatter.format("YYY年MMM月DD日", example()));
        Assert.assertEquals("二零二六年八月七日",
                DateFormatter.format("YYYY年MMM月DD日", example()));
    }

    @Test
    public void keepsUnknownCharactersLiteral() {
        Assert.assertEquals("abc 2026", DateFormatter.format("abc yyyy", example()));
        Assert.assertEquals("2026年", DateFormatter.format("yyyy年", example()));
    }

    @Test
    public void fallsBackToEmptyOrLiteralInput() {
        Assert.assertEquals("", DateFormatter.format(null, example()));
        Assert.assertEquals("年月日", DateFormatter.format("年月日", example()));
    }
}
