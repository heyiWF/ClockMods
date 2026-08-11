package com.clockmods.ui;

/**
 * Inserts a half-width space (U+0020) at boundaries between Chinese ideographs and
 * half-width Latin letters or digits, following the common "pangu" spacing convention.
 *
 * <p>Only CJK &harr; ASCII letter/digit transitions are touched, in both directions.
 * Existing spaces are never doubled (a space is neither CJK nor alphanumeric, so a
 * boundary that already has one produces no extra insertion). Full-width punctuation,
 * symbols such as {@code ℃}, and emoji (including surrogate pairs) are copied through
 * unchanged and never trigger a space. Number-to-unit spacing (e.g. {@code 28℃}) is
 * intentionally out of scope here and handled at the concatenation sites instead.
 */
public final class TextSpacing {
    private TextSpacing() {
    }

    /**
     * @return {@code text} with half-width spaces inserted between adjacent Chinese and
     *         Latin-alphanumeric characters. Pure-Chinese and pure-Latin strings, as well
     *         as {@code null} or single-character input, are returned unchanged.
     */
    public static String pangu(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length() + 8);
        int previous = -1;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (previous != -1 && needsPanguSpace(previous, codePoint)) {
                result.append(' ');
            }
            result.appendCodePoint(codePoint);
            previous = codePoint;
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    static boolean needsPanguSpace(int left, int right) {
        return (isCjk(left) && isLatinAlphanumeric(right))
                || (isLatinAlphanumeric(left) && isCjk(right));
    }

    private static boolean isLatinAlphanumeric(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9')
                || (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= 'a' && codePoint <= 'z');
    }

    private static boolean isCjk(int codePoint) {
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF)   // CJK Unified Ideographs
                || (codePoint >= 0x3400 && codePoint <= 0x4DBF)   // Extension A
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)   // Compatibility Ideographs
                || (codePoint >= 0x20000 && codePoint <= 0x2A6DF) // Extension B
                || codePoint == 0x3007;                           // 〇 ideographic number zero
    }
}
