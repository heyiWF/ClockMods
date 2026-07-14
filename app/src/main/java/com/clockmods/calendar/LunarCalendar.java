package com.clockmods.calendar;

import java.util.Calendar;
import java.util.GregorianCalendar;

public final class LunarCalendar {
    private static final long[] LUNAR_INFO = new long[] {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
            0x14b63
    };

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
        int stemBranchIndex = (lunarDate.year - 4) % 60;
        String stemBranchYear = TIAN_GAN[stemBranchIndex % 10] + DI_ZHI[stemBranchIndex % 12];
        return stemBranchYear + "[" + ZODIAC[stemBranchIndex % 12] + "]年" + formatMonth(lunarDate.month, lunarDate.leap) + formatDay(lunarDate.day);
    }

    public static LunarDate fromSolar(Calendar solar) {
        int year = solar.get(Calendar.YEAR);
        if (year < 1900 || year > 2050) {
            return null;
        }

        Calendar baseDate = new GregorianCalendar(1900, Calendar.JANUARY, 31);
        long offset = (solar.getTimeInMillis() - baseDate.getTimeInMillis()) / 86400000L;

        int lunarYear;
        for (lunarYear = 1900; lunarYear < 2051 && offset > 0; lunarYear++) {
            int yearDays = yearDays(lunarYear);
            if (offset < yearDays) {
                break;
            }
            offset -= yearDays;
        }

        int leapMonth = leapMonth(lunarYear);
        boolean leap = false;
        int lunarMonth;
        for (lunarMonth = 1; lunarMonth <= 12 && offset >= 0; lunarMonth++) {
            int monthDays;
            if (leapMonth > 0 && lunarMonth == leapMonth + 1 && !leap) {
                lunarMonth--;
                leap = true;
                monthDays = leapDays(lunarYear);
            } else {
                monthDays = monthDays(lunarYear, lunarMonth);
            }

            if (offset < monthDays) {
                break;
            }
            offset -= monthDays;

            if (leap && lunarMonth == leapMonth) {
                leap = false;
            }
        }

        return new LunarDate(lunarYear, lunarMonth, (int) offset + 1, leap);
    }

    private static int yearDays(int year) {
        int sum = 348;
        long info = LUNAR_INFO[year - 1900];
        for (int bit = 0x8000; bit > 0x8; bit >>= 1) {
            if ((info & bit) != 0) {
                sum++;
            }
        }
        return sum + leapDays(year);
    }

    private static int leapDays(int year) {
        if (leapMonth(year) != 0) {
            return (LUNAR_INFO[year - 1900] & 0x10000) != 0 ? 30 : 29;
        }
        return 0;
    }

    private static int leapMonth(int year) {
        return (int) (LUNAR_INFO[year - 1900] & 0xf);
    }

    private static int monthDays(int year, int month) {
        return (LUNAR_INFO[year - 1900] & (0x10000 >> month)) != 0 ? 30 : 29;
    }

    private static String formatMonth(int month, boolean leap) {
        String prefix = leap ? "闰" : "";
        return prefix + MONTH_NAMES[month - 1] + "月";
    }

    private static String formatDay(int day) {
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