package com.clockmods.ui

import java.util.Calendar
import org.junit.Assert
import org.junit.Test

class DateFormatterTest {
    private fun example(): Calendar = DateFormatter.exampleDate()

    @Test fun exampleDateIsFriday() {
        Assert.assertEquals(Calendar.FRIDAY, example().get(Calendar.DAY_OF_WEEK))
        Assert.assertEquals(2026, example().get(Calendar.YEAR))
        Assert.assertEquals(Calendar.AUGUST, example().get(Calendar.MONTH))
        Assert.assertEquals(7, example().get(Calendar.DAY_OF_MONTH))
    }

    @Test fun rendersChineseArabicDateWithSpacing() {
        Assert.assertEquals("2026 年 8 月 7 日", DateFormatter.format("yyyy年M月d日", example(), DateFormatter.Lang.CHINESE))
        Assert.assertEquals(
            "2026 年 8 月 7 日 星期五",
            DateFormatter.format(DateFormatter.DEFAULT_PATTERN_CN, example(), DateFormatter.Lang.CHINESE),
        )
        Assert.assertEquals("2026 年 08 月 07 日", DateFormatter.format("yyyy年MM月dd日", example(), DateFormatter.Lang.CHINESE))
    }

    @Test fun rendersChineseNumeralDate() {
        Assert.assertEquals("二〇二六年八月七日", DateFormatter.format("YYY年MMM月DD日", example(), DateFormatter.Lang.CHINESE))
        Assert.assertEquals("二零二六年八月七日", DateFormatter.format("YYYY年MMM月DD日", example(), DateFormatter.Lang.CHINESE))
        Assert.assertEquals("八月七号", DateFormatter.format("MMM月DD号", example(), DateFormatter.Lang.CHINESE))
    }

    @Test fun rendersEnglishDates() {
        Assert.assertEquals("2026/8/7 Friday", DateFormatter.format(DateFormatter.DEFAULT_PATTERN_EN, example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("August 7, 2026", DateFormatter.format("MMMM d, yyyy", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("Aug 7, Fri", DateFormatter.format("MMM d, E", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("07/08/2026", DateFormatter.format("dd/MM/yyyy", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("26", DateFormatter.format("yy", example(), DateFormatter.Lang.ENGLISH))
    }

    @Test fun rendersChineseWeekdayShortAndFull() {
        Assert.assertEquals("星期五", DateFormatter.format("EEEE", example(), DateFormatter.Lang.CHINESE))
        Assert.assertEquals("周五", DateFormatter.format("E", example(), DateFormatter.Lang.CHINESE))
    }

    @Test fun traditionalUsesTraditionalShortWeekdayButSharesEverythingElse() {
        Assert.assertEquals("週五", DateFormatter.format("E", example(), DateFormatter.Lang.TRADITIONAL))
        Assert.assertEquals("星期五", DateFormatter.format("EEEE", example(), DateFormatter.Lang.TRADITIONAL))
        Assert.assertEquals("二〇二六年八月七日", DateFormatter.format("YYY年MMM月DD日", example(), DateFormatter.Lang.TRADITIONAL))
    }

    @Test fun englishDegradesChineseNumeralTokens() {
        Assert.assertEquals("2026", DateFormatter.format("YYY", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("2026", DateFormatter.format("YYYY", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("7", DateFormatter.format("DD", example(), DateFormatter.Lang.ENGLISH))
    }

    @Test fun chineseCardinalCoversDayRange() {
        Assert.assertEquals("七", DateFormatter.chineseCardinal(7))
        Assert.assertEquals("十", DateFormatter.chineseCardinal(10))
        Assert.assertEquals("十一", DateFormatter.chineseCardinal(11))
        Assert.assertEquals("二十", DateFormatter.chineseCardinal(20))
        Assert.assertEquals("二十一", DateFormatter.chineseCardinal(21))
        Assert.assertEquals("三十", DateFormatter.chineseCardinal(30))
        Assert.assertEquals("三十一", DateFormatter.chineseCardinal(31))
    }

    @Test fun chineseYearUsesRequestedZeroGlyph() {
        Assert.assertEquals("二〇二六", DateFormatter.chineseYear(2026, '〇'))
        Assert.assertEquals("二零二六", DateFormatter.chineseYear(2026, '零'))
        Assert.assertEquals("二〇〇〇", DateFormatter.chineseYear(2000, '〇'))
    }

    @Test fun unknownLettersAndSpecifiersAreLiteralAndInjectionSafe() {
        Assert.assertEquals("%s 2026", DateFormatter.format("%s yyyy", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("%7", DateFormatter.format("%d", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("abc 2026", DateFormatter.format("abc yyyy", example(), DateFormatter.Lang.ENGLISH))
    }

    @Test fun supportsQuotedLiterals() {
        Assert.assertEquals("Year: 2026", DateFormatter.format("'Year:' yyyy", example(), DateFormatter.Lang.ENGLISH))
        Assert.assertEquals("'", DateFormatter.format("''", example(), DateFormatter.Lang.ENGLISH))
    }

    @Test fun preservesEmojiInCustomFormats() {
        Assert.assertEquals("📅 2026 年", DateFormatter.format("📅 yyyy年", example(), DateFormatter.Lang.CHINESE))
    }

    @Test fun validatesPatterns() {
        Assert.assertTrue(DateFormatter.isValidPattern("yyyy年M月d日"))
        Assert.assertTrue(DateFormatter.isValidPattern("📅EEEE"))
        Assert.assertFalse(DateFormatter.isValidPattern(null))
        Assert.assertFalse(DateFormatter.isValidPattern(""))
        Assert.assertFalse(DateFormatter.isValidPattern("年月日"))
        Assert.assertFalse(DateFormatter.isValidPattern("'yyyy'"))
        val tooLong = buildString {
            repeat(DateFormatter.MAX_PATTERN_LENGTH + 1) { append('y') }
        }
        Assert.assertFalse(DateFormatter.isValidPattern(tooLong))
    }

    @Test fun everyBuiltInFormatRendersNonEmpty() {
        for (lang in DateFormatter.Lang.values()) {
            for (core in DateFormatter.dateCores(lang)) {
                Assert.assertTrue("core: $core", DateFormatter.preview(core, lang).isNotEmpty())
                Assert.assertTrue("core valid: $core", DateFormatter.isValidPattern(core))
            }
            for (fixed in DateFormatter.fixedFormats(lang)) {
                Assert.assertTrue("fixed: $fixed", DateFormatter.preview(fixed, lang).isNotEmpty())
                Assert.assertTrue("fixed valid: $fixed", DateFormatter.isValidPattern(fixed))
            }
            for (combo in DateFormatter.weekdayCombos(lang)) {
                val full = DateFormatter.composeCombo(combo, "yyyy年M月d日")
                Assert.assertTrue("combo: $combo", DateFormatter.preview(full, lang).isNotEmpty())
                Assert.assertTrue("combo label: $combo", DateFormatter.comboLabel(combo, lang).isNotEmpty())
            }
        }
    }

    @Test fun composeComboSubstitutesDatePlaceholder() {
        Assert.assertEquals(
            "2026 年 8 月 7 日 星期五",
            DateFormatter.format(
                DateFormatter.composeCombo("DATE EEEE", "yyyy年M月d日"),
                example(),
                DateFormatter.Lang.CHINESE,
            ),
        )
        Assert.assertEquals(
            "（星期五）2026 年 8 月 7 日",
            DateFormatter.format(
                DateFormatter.composeCombo("（EEEE）DATE", "yyyy年M月d日"),
                example(),
                DateFormatter.Lang.CHINESE,
            ),
        )
    }
}
