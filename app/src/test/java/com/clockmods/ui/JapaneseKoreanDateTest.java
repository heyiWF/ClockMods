package com.clockmods.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class JapaneseKoreanDateTest {
    @Test public void japaneseUsesLocalPunctuationAndNoChineseSpacing() {
        assertEquals("2026年8月7日（金）", DateFormatter.preview(
                DateFormatter.defaultPattern(DateFormatter.Lang.JAPANESE), DateFormatter.Lang.JAPANESE));
        assertEquals("2026/08/07 金曜日", DateFormatter.preview("yyyy/MM/dd EEEE", DateFormatter.Lang.JAPANESE));
    }
    @Test public void koreanUsesLocalYearMonthDayAndWeekday() {
        assertEquals("2026년 8월 7일 금요일", DateFormatter.preview(
                DateFormatter.defaultPattern(DateFormatter.Lang.KOREAN), DateFormatter.Lang.KOREAN));
        assertEquals("2026. 8. 7. (금)", DateFormatter.preview("yyyy. M. d. (E)", DateFormatter.Lang.KOREAN));
        assertEquals("2026 7", DateFormatter.preview("YYYY DD", DateFormatter.Lang.KOREAN));
    }
    @Test public void localizedMonthsDoNotAcquireDuplicateUnits() {
        assertEquals("8月", DateFormatter.preview("MMMM", DateFormatter.Lang.JAPANESE));
        assertEquals("8월", DateFormatter.preview("MMMM", DateFormatter.Lang.KOREAN));
        for (DateFormatter.Lang lang : new DateFormatter.Lang[]{DateFormatter.Lang.JAPANESE,DateFormatter.Lang.KOREAN}) {
            for (String core : DateFormatter.dateCores(lang)) {
                String value = DateFormatter.preview(core, lang);
                assertFalse(value.contains("月月")); assertFalse(value.contains("월월"));
                assertFalse(value.contains("号")); assertFalse(value.contains("星期"));
            }
        }
    }
    @Test public void literalTextAndWeekdayCompositionRemainAvailable() {
        assertEquals("2026/08/07 (금)", DateFormatter.preview(
                DateFormatter.composeCombo("DATE (E)", "yyyy/MM/dd"), DateFormatter.Lang.KOREAN));
        assertEquals("2026年 E 金曜日", DateFormatter.preview("yyyy年 'E' EEEE", DateFormatter.Lang.JAPANESE));
    }
}
