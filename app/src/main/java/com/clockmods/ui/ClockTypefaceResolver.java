package com.clockmods.ui;

import android.content.Context;
import android.graphics.Typeface;

import com.clockmods.background.ClockPreferences;
import com.clockmods.background.FontCatalog;

import java.util.HashMap;
import java.util.Map;

public final class ClockTypefaceResolver {
    /** Cache keyed by "<fontId>#<weight>" so each asset is only decoded once. */
    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private ClockTypefaceResolver() {
    }

    /**
     * Resolves {@code family} at {@code weight}. The weight is snapped to what the family can
     * actually render (see {@link FontCatalog.FontOption#nearestWeight(int)}), so asking a
     * three-weight family for 250 yields its Regular rather than a synthesized approximation.
     */
    public static Typeface resolve(Context context, String family, int weight) {
        String normalized = ClockPreferences.normalizeFontFamily(family);
        FontCatalog.FontOption option = FontCatalog.optionFor(normalized);
        int resolvedWeight = option.nearestWeight(weight);
        if (option.isSystem()) {
            return systemTypeface(resolvedWeight);
        }
        return loadOption(context, option, resolvedWeight);
    }

    public static Typeface resolveTime(Context context, String family, boolean bold) {
        return resolve(context, family, bold ? 700 : FontCatalog.DEFAULT_WEIGHT);
    }

    /**
     * Resolves the font a calendar theme has chosen for {@code scopeId} at the given hierarchy
     * tier. {@code emphasized} is the masthead tier: it takes the next-heavier stop the family can
     * render, so a Medium body still gets a visibly bolder title rather than a synthetic one.
     */
    public static Typeface resolveForTheme(Context context, ClockPreferences preferences,
            String scopeId, boolean emphasized) {
        String family = preferences.getFontFamily(scopeId);
        int weight = preferences.getFontWeight(scopeId);
        if (emphasized) {
            weight = FontCatalog.optionFor(family).emphasizedWeight(weight);
        }
        return resolve(context, family, weight);
    }

    static Typeface resolveSupporting(Context context, String family, int weight,
            boolean containsChinese) {
        if (containsChinese) {
            // The bundled families carry no CJK glyphs, so Chinese keeps the system typeface and
            // only borrows the weight.
            return systemTypeface(weight);
        }
        return resolve(context, family, weight);
    }

    static Typeface resolveSupporting(Context context, String family, boolean bold,
            boolean containsChinese) {
        return resolveSupporting(context, family,
                bold ? 700 : FontCatalog.DEFAULT_WEIGHT, containsChinese);
    }

    /**
     * Resolves the typeface for a single code point of supporting text. Chinese
     * (CJK) characters keep the system typeface so glyphs always render, while
     * every other character (digits, latin letters, punctuation) uses the
     * user selected font so switching the font also affects the non-Chinese
     * parts of the date and weather lines.
     */
    static Typeface resolveSupportingForCodePoint(Context context, String family, int weight,
            int codePoint) {
        return resolveSupporting(context, family, weight, isChinese(codePoint));
    }

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

    /**
     * The platform exposes its sans-serif weights as separate family names rather than an axis,
     * so the system font's stops map onto the five names Android actually ships.
     */
    private static Typeface systemTypeface(int weight) {
        String family;
        if (weight <= 100) {
            family = "sans-serif-thin";
        } else if (weight <= 300) {
            family = "sans-serif-light";
        } else if (weight <= 400) {
            family = "sans-serif";
        } else if (weight <= 500) {
            family = "sans-serif-medium";
        } else {
            family = "sans-serif-black";
        }
        return Typeface.create(family, Typeface.NORMAL);
    }

    private static Typeface loadOption(Context context, FontCatalog.FontOption option, int weight) {
        String cacheKey = option.id + '#' + weight;
        Typeface cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Typeface loaded = option.isVariable()
                ? loadVariable(context, option, weight)
                : load(context, option.staticAssetFor(weight), systemTypeface(weight));
        CACHE.put(cacheKey, loaded);
        return loaded;
    }

    /**
     * Instantiates one named position on the family's {@code wght} axis. Any non-weight axis the
     * family pins (SF Pro's optical size) travels alongside it, since dropping it here would let
     * the axis fall back to its default and quietly change the cut.
     *
     * <p>The declared weight is pinned to regular, independent of the axis position. The bundled
     * families carry no CJK glyphs, so a 宜/忌 or temperature run that hits those glyphs falls
     * through to the system font — and the system resolves <em>which</em> CJK cut to use from the
     * typeface's reported weight. Without this, a Black clock would render 多云/湿度/宜忌 in a
     * bold CJK face; declaring regular keeps Chinese exactly as the user last saw it while the
     * Latin and digits still draw at the weight the font file actually has.
     */
    private static Typeface loadVariable(Context context, FontCatalog.FontOption option,
            int weight) {
        String settings = "'wght' " + weight
                + (option.variableAxes == null ? "" : ", " + option.variableAxes);
        try {
            return new Typeface.Builder(context.getAssets(), option.variableAsset)
                    .setFontVariationSettings(settings)
                    .setWeight(FontCatalog.DEFAULT_WEIGHT)
                    .build();
        } catch (RuntimeException ignored) {
            return load(context, option.variableAsset, systemTypeface(weight));
        }
    }

    private static Typeface load(Context context, String path, Typeface fallback) {
        if (path == null) {
            return fallback;
        }
        try {
            return new Typeface.Builder(context.getAssets(), path)
                    .setWeight(FontCatalog.DEFAULT_WEIGHT)
                    .build();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
