package com.clockmods.sdk.style;

/** Applies layout caps only while the user keeps the font at or below 100%. */
public final class FontScalePolicy {
    private FontScalePolicy() {}

    public static boolean allowsOverflow(float configuredScale) {
        return !Float.isNaN(configuredScale) && !Float.isInfinite(configuredScale)
                && configuredScale > 1f;
    }

    public static float cap(float requested, float limit, float configuredScale) {
        return allowsOverflow(configuredScale) ? requested : Math.min(requested, limit);
    }
}
