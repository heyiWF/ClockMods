package com.clockmods.calendar;

import com.tyme.lunar.LunarDay;
import com.tyme.lunar.LunarMonth;
import com.tyme.solar.SolarDay;

import java.util.Calendar;

/**
 * Formats the lunar date shown next to the main clock.
 *
 * <p>The date itself is computed by the offline 6tail tyme4j engine (astronomical, accurate for
 * years 1–9999), while the Chinese rendering keeps this app's own wording (正…冬腊月, 干支[生肖]年,
 * 初/廿 day names) so the on-screen text stays byte-for-byte identical to previous releases. The
 * public {@link #formatMonth}, {@link #formatDay} and {@link #stemZodiacYear} helpers are reused by
 * the Pro calendar so the whole app renders lunar labels consistently.
 */
public final class LunarCalendar {
    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] ZODIAC = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] MONTH_NAMES = {"正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
    private static final String[] DAY_PREFIX = {"初", "十", "廿", "三"};
    private static final String[] DAY_NUMBERS = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    private LunarCalendar() {
    }

    public static String format(Calendar calendar) {
        LunarDate lunarDate = fromSolar(calendar);
        if (lunarDate == null) {
            return "";
        }
        return stemZodiacYear(lunarDate.year, true)
                + formatMonth(lunarDate.month, lunarDate.leap) + formatDay(lunarDate.day);
    }

    public static String formatNatural(Calendar calendar) {
        LunarDate lunarDate = fromSolar(calendar);
        if (lunarDate == null) {
            return "";
        }
        return stemZodiacYear(lunarDate.year, false)
                + formatMonth(lunarDate.month, lunarDate.leap) + formatDay(lunarDate.day);
    }

    public static String formatShort(Calendar calendar) {
        LunarDate lunarDate = fromSolar(calendar);
        if (lunarDate == null) {
            return "";
        }
        if (lunarDate.day == 1) {
            return formatMonth(lunarDate.month, lunarDate.leap);
        }
        return formatDay(lunarDate.day);
    }

    /**
     * Converts a Gregorian date (read in the supplied calendar's own time zone) to its lunar
     * year/month/day via the tyme4j engine. Never returns {@code null} for real calendar dates; the
     * nullable return is kept for source compatibility with callers that guard against it.
     */
    public static LunarDate fromSolar(Calendar solar) {
        int year = solar.get(Calendar.YEAR);
        int month = solar.get(Calendar.MONTH) + 1;
        int day = solar.get(Calendar.DAY_OF_MONTH);

        LunarDay lunarDay = SolarDay.fromYmd(year, month, day).getLunarDay();
        LunarMonth lunarMonth = lunarDay.getLunarMonth();
        return new LunarDate(
                lunarMonth.getLunarYear().getYear(),
                Math.abs(lunarMonth.getMonthWithLeap()),
                lunarDay.getDay(),
                lunarMonth.isLeap());
    }

    /** 干支[生肖]年 (bracketed) or 干支生肖年 (plain) for the given lunar year, e.g. 丙午[马]年 / 丙午马年. */
    public static String stemZodiacYear(int lunarYear, boolean bracketZodiac) {
        int stemBranchIndex = Math.floorMod(lunarYear - 4, 60);
        String stemBranchYear = TIAN_GAN[stemBranchIndex % 10] + DI_ZHI[stemBranchIndex % 12];
        String zodiac = ZODIAC[stemBranchIndex % 12];
        return bracketZodiac
                ? stemBranchYear + "[" + zodiac + "]年"
                : stemBranchYear + zodiac + "年";
    }

    public static String formatMonth(int month, boolean leap) {
        String prefix = leap ? "闰" : "";
        return prefix + MONTH_NAMES[month - 1] + "月";
    }

    public static String formatDay(int day) {
        if (day == 10) {
            return "初十";
        }
        if (day == 20) {
            return "二十";
        }
        if (day == 30) {
            return "三十";
        }
        int prefixIndex = (day - 1) / 10;
        int numberIndex = (day - 1) % 10;
        return DAY_PREFIX[prefixIndex] + DAY_NUMBERS[numberIndex];
    }

    public static final class LunarDate {
        public final int year;
        public final int month;
        public final int day;
        public final boolean leap;

        public LunarDate(int year, int month, int day, boolean leap) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.leap = leap;
        }
    }
}
