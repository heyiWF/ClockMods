package com.clockmods.ui;

import android.content.Context;
import android.graphics.Typeface;

import com.clockmods.background.ClockPreferences;

final class ClockTypefaceResolver {
    private static Typeface roboto;
    private static Typeface robotoBold;
    private static Typeface googleSans;
    private static Typeface googleSansBold;

    private ClockTypefaceResolver() {
    }

    static Typeface resolveTime(Context context, String family, boolean bold) {
        String normalized = ClockPreferences.normalizeFontFamily(family);
        if (ClockPreferences.FONT_ROBOTO.equals(normalized)) {
            return loadRoboto(context, bold);
        }
        if (ClockPreferences.FONT_GOOGLE_SANS.equals(normalized)) {
            return loadGoogleSans(context, bold);
        }
        return bold ? Typeface.create("sans-serif-black", Typeface.NORMAL) : Typeface.DEFAULT;
    }

    static Typeface resolveSupporting(Context context, String family, boolean bold, boolean containsChinese) {
        if (containsChinese) {
            return bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        }
        return resolveTime(context, family, bold);
    }

    /**
     * Resolves the typeface for a single code point of supporting text. Chinese
     * (CJK) characters keep the system typeface so glyphs always render, while
     * every other character (digits, latin letters, punctuation) uses the
     * user selected font so switching the font also affects the non-Chinese
     * parts of the date and weather lines.
     */
    static Typeface resolveSupportingForCodePoint(Context context, String family, boolean bold,
            int codePoint) {
        return resolveSupporting(context, family, bold, isChinese(codePoint));
    }

    static boolean isChinese(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    private static Typeface loadRoboto(Context context, boolean bold) {
        if (roboto == null) {
            roboto = load(context, "fonts/Roboto-Regular.ttf", Typeface.DEFAULT);
            robotoBold = load(context, "fonts/Roboto-Bold.ttf", Typeface.DEFAULT_BOLD);
        }
        return bold ? robotoBold : roboto;
    }

    private static Typeface loadGoogleSans(Context context, boolean bold) {
        if (googleSans == null) {
            googleSans = load(context, "fonts/GoogleSans-Regular.ttf", Typeface.DEFAULT);
            googleSansBold = load(context, "fonts/GoogleSans-Bold.ttf", Typeface.DEFAULT_BOLD);
        }
        return bold ? googleSansBold : googleSans;
    }

    private static Typeface load(Context context, String path, Typeface fallback) {
        try {
            return Typeface.createFromAsset(context.getAssets(), path);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}