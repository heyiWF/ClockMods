package com.clockmods.ultimate.clock;

import java.util.Calendar;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClockDateRowsTest {
    private static Calendar sunday() {
        Calendar value = Calendar.getInstance(); value.clear(); value.set(2026, 9, 25); return value;
    }
    @Test public void japaneseSundayKeepsTheDayUnit() {
        assertArrayEquals(new String[]{"2026年10月25日", "日曜日", "农历年九月"},
                ClockDateRows.orbit(new String[]{"2026年10月25日（日）", "农历年九月"}, sunday(), Locale.JAPAN));
    }
    @Test public void koreanWeekdayGetsItsOwnRow() {
        assertArrayEquals(new String[]{"2026. 10. 25.", "일요일", ""},
                ClockDateRows.orbit(new String[]{"2026. 10. 25. (일)", ""}, sunday(), Locale.KOREA));
    }
    @Test public void chineseAndEnglishKeepCustomDateOrder() {
        assertArrayEquals(new String[]{"2026 年 10 月 25 日", "星期日", ""},
                ClockDateRows.orbit(new String[]{"2026 年 10 月 25 日 星期日", ""}, sunday(), Locale.SIMPLIFIED_CHINESE));
        assertArrayEquals(new String[]{"25 October 2026", "Sunday", ""},
                ClockDateRows.orbit(new String[]{"Sunday, 25 October 2026", ""}, sunday(), Locale.US));
    }
    @Test public void weekdayIsAvailableWhenCustomDateOmitsIt() {
        assertArrayEquals(new String[]{"2026/10/25", "Sunday", ""},
                ClockDateRows.orbit(new String[]{"2026/10/25", ""}, sunday(), Locale.US));
    }
}
