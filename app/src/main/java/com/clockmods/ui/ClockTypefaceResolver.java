package com.clockmods.ui;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Loads the bundled Roboto faces for every text surface in the app.
 *
 * <p>Roboto has no CJK glyphs, so Chinese characters fall back to the platform font glyph by
 * glyph while Latin letters, digits and punctuation stay in Roboto.
 */
public final class ClockTypefaceResolver {
    private static final String ROBOTO_REGULAR = "fonts/Roboto-Regular.ttf";
    private static final String ROBOTO_BOLD = "fonts/Roboto-Bold.ttf";

    private static Typeface regular;
    private static Typeface bold;

    private ClockTypefaceResolver() {
    }

    public static Typeface resolve(Context context, boolean boldWeight) {
        return boldWeight ? bold(context) : regular(context);
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
