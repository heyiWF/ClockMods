package com.clockmods.ui;

import android.content.Context;
import android.graphics.Typeface;

public final class ClockTypefaceResolver {
    /** The app ships a single family: Roboto, bundled as an asset (no system-font fallback). */
    private static final String ROBOTO_REGULAR = "fonts/Roboto-Regular.ttf";
    private static final String ROBOTO_BOLD = "fonts/Roboto-Bold.ttf";

    private static Typeface regular;
    private static Typeface bold;

    private ClockTypefaceResolver() {
    }

    public static Typeface resolveTime(Context context, boolean boldWeight) {
        return boldWeight ? bold(context) : regular(context);
    }

    /**
     * Roboto has no CJK glyphs, so Chinese supporting text keeps the system typeface
     * (digits, latin letters and punctuation use Roboto).
     */
    static Typeface resolveSupporting(Context context, boolean boldWeight, boolean containsChinese) {
        if (containsChinese) {
            return boldWeight ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        }
        return resolveTime(context, boldWeight);
    }

    static Typeface resolveSupportingForCodePoint(Context context, boolean boldWeight, int codePoint) {
        return resolveSupporting(context, boldWeight, isChinese(codePoint));
    }

    static boolean isChinese(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    private static Typeface regular(Context context) {
        if (regular == null) {
            regular = load(context, ROBOTO_REGULAR, Typeface.DEFAULT);
        }
        return regular;
    }

    private static Typeface bold(Context context) {
        if (bold == null) {
            bold = load(context, ROBOTO_BOLD, Typeface.DEFAULT_BOLD);
        }
        return bold;
    }

    private static Typeface load(Context context, String path, Typeface fallback) {
        try {
            return Typeface.createFromAsset(context.getAssets(), path);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
