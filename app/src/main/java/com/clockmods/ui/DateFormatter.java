package com.clockmods.ui;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Renders dates from a compact, injection-safe pattern language shared by all flavors.
 *
 * <p>A pattern is a sequence of field tokens (runs of a single reserved letter) and literal
 * text. Recognized tokens, interpreted per {@link Lang}:
 * <table>
 *   <tr><td>{@code yyyy}/{@code yy}</td><td>2026 / 26 (Arabic year)</td></tr>
 *   <tr><td>{@code YYY}/{@code YYYY}</td><td>二〇二六 / 二零二六 (Chinese-numeral year)</td></tr>
 *   <tr><td>{@code M}/{@code MM}</td><td>8 / 08 (Arabic month)</td></tr>
 *   <tr><td>{@code MMM}/{@code MMMM}</td><td>Aug / August (English) &middot; 八 (Chinese)</td></tr>
 *   <tr><td>{@code d}/{@code dd}</td><td>7 / 07 (Arabic day)</td></tr>
 *   <tr><td>{@code DD}</td><td>七 (Chinese-numeral day)</td></tr>
 *   <tr><td>{@code E}/{@code EEEE}</td><td>Fri / Friday (English) &middot; 周五 / 星期五 (Chinese) &middot; 週五 (Traditional short)</td></tr>
 * </table>
 *
 * <p>Any other character &mdash; separators, CJK, punctuation, emoji (surrogate pairs) &mdash; is
 * copied through verbatim as a literal. Text wrapped in single quotes is literal too ({@code ''}
 * yields a literal apostrophe), giving custom patterns an escape hatch for reserved letters.
 * The pattern is never handed to {@link String#format} or any interpreter, so there is no format
 * injection surface. The rendered result is passed through {@link TextSpacing#pangu} so the
 * half-width spacing convention applies uniformly to every format (e.g. {@code 2026 年 8 月 7 日}).
 */
public final class DateFormatter {
    /**
     * Rendering language. {@code CHINESE} and {@code TRADITIONAL} share identical numerals, month
     * and full-weekday forms; they differ only in the short weekday glyph (周五 vs 週五).
     */
    public enum Lang { CHINESE, TRADITIONAL, ENGLISH }

    /** Upper bound on a user-supplied pattern, guarding against pathological input. */
    public static final int MAX_PATTERN_LENGTH = 200;

    private static final String[] CN_DIGITS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] CN_UNITS = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] CN_WEEK_FULL = {
            "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    private static final String[] CN_WEEK_SHORT = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
    private static final String[] CN_WEEK_SHORT_TW = {"週日", "週一", "週二", "週三", "週四", "週五", "週六"};
    private static final String[] EN_WEEK_FULL = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] EN_WEEK_SHORT = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] EN_MONTH_FULL = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};
    private static final String[] EN_MONTH_SHORT = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    // ---- Default rendering patterns (equivalent to the previous hard-coded behavior) ----
    /** Chinese default: renders "2026 年 8 月 7 日 星期五". */
    public static final String DEFAULT_PATTERN_CN = "yyyy年M月d日 EEEE";
    /** English default: renders "2026/8/7 Friday". */
    public static final String DEFAULT_PATTERN_EN = "yyyy/M/d EEEE";

    // ---- Chinese date cores (documentation table 二) ----
    private static final String[] CN_DATE_CORES = {
            "yyyy年MM月dd日", "yyyy年M月d日", "yy年M月d日", "yy年MM月dd日",
            "yyyy年M月d号", "yyyy年MM月dd号", "MM月dd日", "M月d日",
            "yyyy/MM/dd", "yyyy/M/d", "yy/M/d", "yyyy-MM-dd", "yyyy-M-d",
            "yyyy.MM.dd", "yyyy.M.d", "MM/dd", "M/d", "dd/MM", "d/M",
            "MM-dd", "dd-MM",
            "YYY年MMM月DD日", "YYYY年MMM月DD日", "YYY年MMM月DD号", "YYYY年MMM月DD号",
            "MMM月DD日", "MMM月DD号"};

    // ---- English date cores (documentation table 一) ----
    private static final String[] EN_DATE_CORES = {
            "MM/dd/yyyy", "M/d/yyyy", "MM/dd/yy", "M/d/yy",
            "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "d/M/yy",
            "yyyy/MM/dd", "yyyy/M/d", "yy/MM/dd", "yyyy-MM-dd", "yyyy-M-d",
            "MM-dd-yyyy", "M-d-yyyy", "dd-MM-yyyy", "d-M-yyyy",
            "yyyy.MM.dd", "yyyy.M.d", "dd.MM.yyyy",
            "MMMM d, yyyy", "MMMM dd, yyyy", "MMM d, yyyy", "MMM dd, yyyy",
            "d MMMM yyyy", "dd MMMM yyyy", "d MMM yyyy", "dd MMM yyyy",
            "MM/dd", "M/d", "dd/MM", "d/M", "MM-dd", "M-d", "dd-MM", "d-M",
            "MM.dd", "M.d", "dd.MM", "d.M",
            "MMMM d", "MMM d", "MMMM dd", "MMM dd", "d MMMM", "d MMM",
            "dd MMMM", "dd MMM", "MMMM-d", "MMM-d", "d-MMMM", "d-MMM"};

    // ---- Weekday combos (documentation table 三). "DATE" is the literal date-core placeholder. ----
    // Chinese uses full-width comma and parentheses; the first entry omits the weekday entirely.
    private static final String[] CN_WEEKDAY_COMBOS = {
            "DATE",
            "DATE EEEE", "DATE E",
            "DATE，EEEE", "DATE，E",
            "DATE（EEEE）", "DATE（E）",
            "（EEEE）DATE", "（E）DATE",
            "DATE [EEEE]", "DATE [E]",
            "[EEEE] DATE", "[E] DATE",
            "DATE - EEEE", "DATE - E",
            "DATE · EEEE", "DATE · E",
            "EEEE DATE", "E DATE",
            "EEEE，DATE", "E，DATE",
            "DATE | EEEE", "DATE | E",
            "EEEE | DATE", "E | DATE"};
    private static final String[] EN_WEEKDAY_COMBOS = {
            "DATE",
            "DATE EEEE", "DATE E",
            "DATE, EEEE", "DATE, E",
            "DATE (EEEE)", "DATE (E)",
            "(EEEE) DATE", "(E) DATE",
            "DATE [EEEE]", "DATE [E]",
            "[EEEE] DATE", "[E] DATE",
            "DATE - EEEE", "DATE - E",
            "DATE · EEEE", "DATE · E",
            "EEEE DATE", "E DATE",
            "EEEE, DATE", "E, DATE",
            "DATE | EEEE", "DATE | E",
            "EEEE | DATE", "E | DATE"};

    // ---- Fixed, non-customizable format lists for Compat/Modern ----
    private static final String[] CN_FIXED_FORMATS = {
            "yyyy/MM/dd EEEE", "yyyy/M/d EEEE",
            "yyyy年MM月dd日 EEEE", "yyyy年M月d日 EEEE",
            "yyyy.MM.dd EEEE", "yyyy.M.d EEEE",
            "yyyy.MM.dd E", "yyyy.M.d E",
            "yyyy年MM月dd日 E", "yyyy年M月d日 E",
            // Year-less variants of each of the above.
            "MM/dd EEEE", "M/d EEEE",
            "MM月dd日 EEEE", "M月d日 EEEE",
            "MM.dd EEEE", "M.d EEEE",
            "MM.dd E", "M.d E",
            "MM月dd日 E", "M月d日 E"};
    private static final String[] EN_FIXED_FORMATS = {
            "yyyy/MM/dd EEEE", "yyyy/M/d EEEE",
            "yyyy-MM-dd EEEE", "yyyy-M-d EEEE",
            "yyyy.MM.dd EEEE", "yyyy.M.d EEEE",
            "EEEE, MMMM d, yyyy", "MMM d, E", "MMM d, yyyy",
            // Year-less variants (entries already without a year are not duplicated).
            "MM/dd EEEE", "M/d EEEE",
            "MM-dd EEEE", "M-d EEEE",
            "MM.dd EEEE", "M.d EEEE",
            "EEEE, MMMM d", "MMM d"};

    private DateFormatter() {
    }

    /**
     * Renders {@code pattern} for {@code cal} under {@code lang}. Never throws; unrecognized input
     * degrades to literal text. Returns "" for a {@code null} pattern.
     */
    public static String format(String pattern, Calendar cal, Lang lang) {
        if (pattern == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(pattern.length() + 16);
        appendFormatted(out, pattern, cal, lang);
        return TextSpacing.pangu(out.toString());
    }

    /**
     * @return whether {@code pattern} is safe to apply: non-empty, within {@link #MAX_PATTERN_LENGTH},
     *         and containing at least one real date/weekday field token (so it renders something
     *         meaningful rather than pure literal text).
     */
    public static boolean isValidPattern(String pattern) {
        if (pattern == null || pattern.length() == 0 || pattern.length() > MAX_PATTERN_LENGTH) {
            return false;
        }
        StringBuilder scratch = new StringBuilder(pattern.length() + 16);
        return appendFormatted(scratch, pattern, exampleDate(), Lang.CHINESE) > 0;
    }

    /** The fixed sample date used by every settings preview: 2026-08-07 (a Friday). */
    public static Calendar exampleDate() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2026, Calendar.AUGUST, 7, 12, 0, 0);
        return cal;
    }

    /** Renders {@code pattern} with the fixed {@link #exampleDate()} (for preview labels). */
    public static String preview(String pattern, Lang lang) {
        return format(pattern, exampleDate(), lang);
    }

    // ---- Accessors for the settings UI ----

    public static String[] dateCores(Lang lang) {
        return (lang == Lang.ENGLISH ? EN_DATE_CORES : CN_DATE_CORES).clone();
    }

    public static String[] weekdayCombos(Lang lang) {
        return (lang == Lang.ENGLISH ? EN_WEEKDAY_COMBOS : CN_WEEKDAY_COMBOS).clone();
    }

    public static String[] fixedFormats(Lang lang) {
        return (lang == Lang.ENGLISH ? EN_FIXED_FORMATS : CN_FIXED_FORMATS).clone();
    }

    /** Substitutes a chosen date core into a weekday-combo template, yielding a full pattern. */
    public static String composeCombo(String comboTemplate, String core) {
        if (comboTemplate == null) {
            return core == null ? "" : core;
        }
        return comboTemplate.replace("DATE", core == null ? "" : core);
    }

    /** A human-facing label for a combo template, e.g. "… 星期五" / "(Friday) …". */
    public static String comboLabel(String comboTemplate, Lang lang) {
        return preview(composeCombo(comboTemplate, "…"), lang);
    }

    // ---- Core scanner ----

    /** Appends the rendered pattern to {@code out}; returns the number of field tokens seen. */
    private static int appendFormatted(StringBuilder out, String pattern, Calendar cal, Lang lang) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0 = Sunday
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            dayOfWeek = 0;
        }

        int tokenCount = 0;
        int length = pattern.length();
        int i = 0;
        while (i < length) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                // Quoted literal: copy verbatim until the closing quote; "''" is a literal quote.
                i++;
                if (i < length && pattern.charAt(i) == '\'') {
                    out.append('\'');
                    i++;
                    continue;
                }
                while (i < length && pattern.charAt(i) != '\'') {
                    int cp = pattern.codePointAt(i);
                    out.appendCodePoint(cp);
                    i += Character.charCount(cp);
                }
                if (i < length) {
                    i++; // consume closing quote
                }
                continue;
            }
            if (isTokenLetter(c)) {
                int run = 1;
                while (i + run < length && pattern.charAt(i + run) == c) {
                    run++;
                }
                appendToken(out, c, run, year, month, day, dayOfWeek, lang);
                tokenCount++;
                i += run;
                continue;
            }
            int cp = pattern.codePointAt(i);
            out.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return tokenCount;
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
                // Chinese-numeral year token; degrades to an Arabic year in English so a mistyped
                // token never injects Chinese numerals into an otherwise-English date.
                if (lang == Lang.ENGLISH) {
                    out.append(Integer.toString(year));
                } else {
                    out.append(chineseYear(year, run == 3 ? '〇' : '零'));
                }
                break;
            case 'M':
                if (lang == Lang.ENGLISH) {
                    if (run == 1) {
                        out.append(Integer.toString(month));
                    } else if (run == 2) {
                        out.append(String.format(Locale.US, "%02d", month));
                    } else if (run == 3) {
                        out.append(EN_MONTH_SHORT[month - 1]);
                    } else {
                        out.append(EN_MONTH_FULL[month - 1]);
                    }
                } else {
                    if (run == 1) {
                        out.append(Integer.toString(month));
                    } else if (run == 2) {
                        out.append(String.format(Locale.US, "%02d", month));
                    } else {
                        out.append(chineseCardinal(month));
                    }
                }
                break;
            case 'd':
                out.append(run == 1
                        ? Integer.toString(day)
                        : String.format(Locale.US, "%02d", day));
                break;
            case 'D':
                // Chinese-numeral day token; degrades to an Arabic day in English for the same reason.
                if (lang == Lang.ENGLISH) {
                    out.append(Integer.toString(day));
                } else {
                    out.append(chineseCardinal(day));
                }
                break;
            case 'E':
                if (lang == Lang.ENGLISH) {
                    out.append(run >= 4 ? EN_WEEK_FULL[dayOfWeek] : EN_WEEK_SHORT[dayOfWeek]);
                } else if (lang == Lang.TRADITIONAL) {
                    out.append(run >= 4 ? CN_WEEK_FULL[dayOfWeek] : CN_WEEK_SHORT_TW[dayOfWeek]);
                } else {
                    out.append(run >= 4 ? CN_WEEK_FULL[dayOfWeek] : CN_WEEK_SHORT[dayOfWeek]);
                }
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
