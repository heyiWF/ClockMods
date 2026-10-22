package com.clockmods.ui;

import java.util.Calendar;
import java.util.Locale;

/**
 * Renders the clock's date line in Chinese from a fixed pattern.
 *
 * <p>Supported tokens: {@code yyyy}/{@code yy} (Arabic year), {@code YYY}/{@code YYYY}
 * (Chinese-numeral year), {@code M}/{@code MM} (Arabic month), {@code MMM} (Chinese month),
 * {@code d}/{@code dd} (Arabic day), {@code DD} (Chinese-numeral day) and {@code E}/{@code EEEE}
 * (short/full weekday). Any other character is copied through verbatim, and the result is passed
 * through {@link TextSpacing#pangu} so half-width spacing applies uniformly.
 */
public final class DateFormatter {
    /**
     * Rendering language. {@code CHINESE} and {@code TRADITIONAL} share numerals, month and
     * full-weekday forms; they differ only in the short weekday glyph (周五 vs 週五).
     */
    public enum Lang { CHINESE, TRADITIONAL }

    /** The single date pattern used by the clock and the calendar header. */
    public static final String PATTERN = "yyyy年M月d日 EEEE";

    private static final String[] CN_DIGITS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] CN_UNITS = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] CN_WEEK_FULL = {
            "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    private static final String[] CN_WEEK_SHORT = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
    private static final String[] CN_WEEK_SHORT_TW = {"週日", "週一", "週二", "週三", "週四", "週五", "週六"};

    private DateFormatter() {
    }

    public static String format(Calendar cal, Lang lang) {
        return format(PATTERN, cal, lang);
    }

    /** Renders {@code pattern} for {@code cal}; unrecognized characters degrade to literal text. */
    public static String format(String pattern, Calendar cal, Lang lang) {
        if (pattern == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(pattern.length() + 16);
        appendFormatted(out, pattern, cal, lang);
        return TextSpacing.pangu(out.toString());
    }

    private static void appendFormatted(StringBuilder out, String pattern, Calendar cal, Lang lang) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0 = Sunday
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            dayOfWeek = 0;
        }
        int length = pattern.length();
        int i = 0;
        while (i < length) {
            char c = pattern.charAt(i);
            if (isTokenLetter(c)) {
                int run = 1;
                while (i + run < length && pattern.charAt(i + run) == c) {
                    run++;
                }
                appendToken(out, c, run, year, month, day, dayOfWeek, lang);
                i += run;
                continue;
            }
            int cp = pattern.codePointAt(i);
            out.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
    }

    private static boolean isTokenLetter(char c) {
        return c == 'y' || c == 'Y' || c == 'M' || c == 'd' || c == 'D' || c == 'E';
    }

    private static void appendToken(StringBuilder out, char letter, int run,
            int year, int month, int day, int dayOfWeek, Lang lang) {
        switch (letter) {
            case 'y':
                out.append(run == 2
                        ? String.format(Locale.US, "%02d", Math.floorMod(year, 100))
                        : Integer.toString(year));
                break;
            case 'Y':
                out.append(chineseYear(year, run == 3 ? '〇' : '零'));
                break;
            case 'M':
                if (run == 1) {
                    out.append(Integer.toString(month));
                } else if (run == 2) {
                    out.append(String.format(Locale.US, "%02d", month));
                } else {
                    out.append(chineseCardinal(month));
                }
                break;
            case 'd':
                out.append(run == 1
                        ? Integer.toString(day)
                        : String.format(Locale.US, "%02d", day));
                break;
            case 'D':
                out.append(chineseCardinal(day));
                break;
            case 'E':
                String[] week = lang == Lang.TRADITIONAL ? CN_WEEK_SHORT_TW : CN_WEEK_SHORT;
                out.append(run >= 4 ? CN_WEEK_FULL[dayOfWeek] : week[dayOfWeek]);
                break;
            default:
                break;
        }
    }

    /** Converts a year to per-digit Chinese numerals, using {@code zero} for the digit 0. */
    static String chineseYear(int year, char zero) {
        String digits = Integer.toString(Math.abs(year));
        StringBuilder sb = new StringBuilder(digits.length());
        for (int k = 0; k < digits.length(); k++) {
            char d = digits.charAt(k);
            sb.append(d == '0' ? zero : CN_DIGITS[d - '0'].charAt(0));
        }
        return sb.toString();
    }

    /** Converts a cardinal 1..99 to Chinese numerals (e.g. 7→七, 21→二十一, 30→三十). */
    static String chineseCardinal(int n) {
        if (n <= 0 || n > 99) {
            return Integer.toString(n);
        }
        if (n < 10) {
            return CN_UNITS[n];
        }
        if (n == 10) {
            return "十";
        }
        if (n < 20) {
            return "十" + CN_UNITS[n - 10];
        }
        int tens = n / 10;
        int ones = n % 10;
        return CN_UNITS[tens] + "十" + (ones == 0 ? "" : CN_UNITS[ones]);
    }
}
