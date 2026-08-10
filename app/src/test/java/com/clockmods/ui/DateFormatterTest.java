package com.clockmods.ui;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class DateFormatterTest {

    private static Calendar example() {
        return DateFormatter.exampleDate();
    }

    @Test
    public void exampleDateIsFriday() {
        Assert.assertEquals(Calendar.FRIDAY, example().get(Calendar.DAY_OF_WEEK));
        Assert.assertEquals(2026, example().get(Calendar.YEAR));
        Assert.assertEquals(Calendar.AUGUST, example().get(Calendar.MONTH));
        Assert.assertEquals(7, example().get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void rendersChineseArabicDateWithSpacing() {
        // Spacing convention is applied to the table output too.
        Assert.assertEquals("2026 年 8 月 7 日",
                DateFormatter.format("yyyy年M月d日", example(), DateFormatter.Lang.CHINESE));
        Assert.assertEquals("2026 年 8 月 7 日 星期五",
                DateFormatter.format(DateFormatter.DEFAULT_PATTERN_CN, example(), DateFormatter.Lang.CHINESE));
        Assert.assertEquals("2026 年 08 月 07 日",
                DateFormatter.format("yyyy年MM月dd日", example(), DateFormatter.Lang.CHINESE));
    }

    @Test
    public void rendersChineseNumeralDate() {
        Assert.assertEquals("二〇二六年八月七日",
                DateFormatter.format("YYY年MMM月DD日", example(), DateFormatter.Lang.CHINESE));
        Assert.assertEquals("二零二六年八月七日",
                DateFormatter.format("YYYY年MMM月DD日", example(), DateFormatter.Lang.CHINESE));
        Assert.assertEquals("八月七号",
                DateFormatter.format("MMM月DD号", example(), DateFormatter.Lang.CHINESE));
    }

    @Test
    public void rendersEnglishDates() {
        Assert.assertEquals("2026/8/7 Friday",
                DateFormatter.format(DateFormatter.DEFAULT_PATTERN_EN, example(), DateFormatter.Lang.ENGLISH));
        Assert.assertEquals("August 7, 2026",
                DateFormatter.format("MMMM d, yyyy", example(), DateFormatter.Lang.ENGLISH));
        Assert.assertEquals("Aug 7, Fri",
                DateFormatter.format("MMM d, E", example(), DateFormatter.Lang.ENGLISH));
        Assert.assertEquals("07/08/2026",
                DateFormatter.format("dd/MM/yyyy", example(), DateFormatter.Lang.ENGLISH));
        Assert.assertEquals("26",
                DateFormatter.format("yy", example(), DateFormatter.Lang.ENGLISH));
    }

    @Test
    public void rendersChineseWeekdayShortAndFull() {
        Assert.assertEquals("星期五",
                DateFormatter.format("EEEE", example(), DateFormatter.Lang.CHINESE));
        Assert.assertEquals("周五",
                DateFormatter.format("E", example(), DateFormatter.Lang.CHINESE));
    }

    @Test
    public void chineseCardinalCoversDayRange() {
        Assert.assertEquals("七", DateFormatter.chineseCardinal(7));
        Assert.assertEquals("十", DateFormatter.chineseCardinal(10));
        Assert.assertEquals("十一", DateFormatter.chineseCardinal(11));
        Assert.assertEquals("二十", DateFormatter.chineseCardinal(20));
        Assert.assertEquals("二十一", DateFormatter.chineseCardinal(21));
        Assert.assertEquals("三十", DateFormatter.chineseCardinal(30));
        Assert.assertEquals("三十一", DateFormatter.chineseCardinal(31));
    }

    @Test
    public void chineseYearUsesRequestedZeroGlyph() {
        Assert.assertEquals("二〇二六", DateFormatter.chineseYear(2026, '〇'));
        Assert.assertEquals("二零二六", DateFormatter.chineseYear(2026, '零'));
        Assert.assertEquals("二〇〇〇", DateFormatter.chineseYear(2000, '〇'));
    }

    @Test
    public void unknownLettersAndSpecifiersAreLiteralAndInjectionSafe() {
        // "%" and non-token letters ('s') are copied verbatim: no printf-style interpretation,
        // so no format-injection surface. (No String.format consumes these.)
        Assert.assertEquals("%s 2026",
                DateFormatter.format("%s yyyy", example(), DateFormatter.Lang.ENGLISH));
        // "%d" is NOT a printf specifier here: 'd' is the day token, so it renders the day (7)
        // rather than consuming an argument. This is precisely why the pattern is injection-safe.
        Assert.assertEquals("%7",
                DateFormatter.format("%d", example(), DateFormatter.Lang.ENGLISH));
        // Other non-token letters pass through verbatim.
        Assert.assertEquals("abc 2026",
                DateFormatter.format("abc yyyy", example(), DateFormatter.Lang.ENGLISH));
    }

    @Test
    public void supportsQuotedLiterals() {
        Assert.assertEquals("Year: 2026",
                DateFormatter.format("'Year:' yyyy", example(), DateFormatter.Lang.ENGLISH));
        Assert.assertEquals("'", DateFormatter.format("''", example(), DateFormatter.Lang.ENGLISH));
    }

    @Test
    public void preservesEmojiInCustomFormats() {
        Assert.assertEquals("📅 2026 年",
                DateFormatter.format("📅 yyyy年", example(), DateFormatter.Lang.CHINESE));
    }

    @Test
    public void validatesPatterns() {
        Assert.assertTrue(DateFormatter.isValidPattern("yyyy年M月d日"));
        Assert.assertTrue(DateFormatter.isValidPattern("📅EEEE"));
        Assert.assertFalse(DateFormatter.isValidPattern(null));
        Assert.assertFalse(DateFormatter.isValidPattern(""));
        // No field token: literal-only text is not a usable date format.
        Assert.assertFalse(DateFormatter.isValidPattern("年月日"));
        Assert.assertFalse(DateFormatter.isValidPattern("'yyyy'")); // quoted, so no live token
        // Over-length input is rejected.
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i <= DateFormatter.MAX_PATTERN_LENGTH; i++) {
            tooLong.append('y');
        }
        Assert.assertFalse(DateFormatter.isValidPattern(tooLong.toString()));
    }

    @Test
    public void everyBuiltInFormatRendersNonEmpty() {
        for (DateFormatter.Lang lang : DateFormatter.Lang.values()) {
            for (String core : DateFormatter.dateCores(lang)) {
                Assert.assertTrue("core: " + core, DateFormatter.preview(core, lang).length() > 0);
                Assert.assertTrue("core valid: " + core, DateFormatter.isValidPattern(core));
            }
            for (String fixed : DateFormatter.fixedFormats(lang)) {
                Assert.assertTrue("fixed: " + fixed, DateFormatter.preview(fixed, lang).length() > 0);
                Assert.assertTrue("fixed valid: " + fixed, DateFormatter.isValidPattern(fixed));
            }
            for (String combo : DateFormatter.weekdayCombos(lang)) {
                String full = DateFormatter.composeCombo(combo, "yyyy年M月d日");
                Assert.assertTrue("combo: " + combo, DateFormatter.preview(full, lang).length() > 0);
                Assert.assertTrue("combo label: " + combo,
                        DateFormatter.comboLabel(combo, lang).length() > 0);
            }
        }
    }

    @Test
    public void composeComboSubstitutesDatePlaceholder() {
        Assert.assertEquals("2026 年 8 月 7 日 星期五",
                DateFormatter.format(DateFormatter.composeCombo("DATE EEEE", "yyyy年M月d日"),
                        example(), DateFormatter.Lang.CHINESE));
        Assert.assertEquals("（星期五）2026 年 8 月 7 日",
                DateFormatter.format(DateFormatter.composeCombo("（EEEE）DATE", "yyyy年M月d日"),
                        example(), DateFormatter.Lang.CHINESE));
    }
}
