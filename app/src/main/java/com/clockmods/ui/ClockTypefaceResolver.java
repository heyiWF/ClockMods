package com.clockmods.ui;

import android.content.Context;
import android.graphics.Typeface;

import com.clockmods.background.ClockPreferences;
import com.clockmods.background.FontCatalog;

import java.util.HashMap;
import java.util.Map;

final class ClockTypefaceResolver {
    /** Cache keyed by "<fontId>#<bold>" so each asset is only decoded once. */
    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private ClockTypefaceResolver() {
    }

    static Typeface resolveTime(Context context, String family, boolean bold) {
        String normalized = ClockPreferences.normalizeFontFamily(family);
        FontCatalog.FontOption option = FontCatalog.optionFor(normalized);
        if (option.isSystem()) {
            return bold ? Typeface.create("sans-serif-black", Typeface.NORMAL) : Typeface.DEFAULT;
        }
        return loadOption(context, option, bold);
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

    private static Typeface loadOption(Context context, FontCatalog.FontOption option, boolean bold) {
        String cacheKey = option.id + '#' + bold;
        Typeface cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String assetPath = bold && option.boldAsset != null ? option.boldAsset : option.regularAsset;
        Typeface fallback = bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        Typeface loaded = load(context, assetPath, fallback);
        // When a family ships no dedicated bold asset, synthesize bold from the
        // regular face so the "bold text" option still visibly thickens glyphs.
        if (bold && option.boldAsset == null && loaded != null) {
            loaded = Typeface.create(loaded, Typeface.BOLD);
        }
        CACHE.put(cacheKey, loaded);
        return loaded;
    }

    private static Typeface load(Context context, String path, Typeface fallback) {
        try {
            return Typeface.createFromAsset(context.getAssets(), path);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}