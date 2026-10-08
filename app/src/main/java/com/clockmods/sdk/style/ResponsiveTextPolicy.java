package com.clockmods.sdk.style;

/** Geometry-only policy. Primary content owns its slot before secondary type may grow. */
public final class ResponsiveTextPolicy {
    private ResponsiveTextPolicy() {}

    public static boolean needsCompactLayout(float width, float height, float density) {
        if (width <= 0f || height <= 0f) return true;
        float aspect = width / height;
        return aspect < .4f || aspect > 2.8f || Math.min(width, height) / Math.max(.01f, density) < 240f;
    }

    /** Secondary growth is available only on roomy surfaces, and yields to enlarged primary type. */
    public static float secondaryLimit(float width, float height, float density, float primaryScale) {
        float shortSide = Math.max(0f, Math.min(width, height));
        float room = Math.max(0f, Math.min(1f,
                (shortSide / Math.max(.01f, density) - 240f) / 360f));
        float spare = Math.max(0f, room - Math.max(0f, primaryScale - 1f) * .5f);
        return shortSide * (.045f + .045f * spare);
    }

    /** Font size cap for a reserved line box; never lets an accessibility floor exceed its slot. */
    public static float fitLine(float desired, float measuredWidthAtOne, float width,
            float height, float lineHeightAtOne) {
        return Math.max(0f, Math.min(desired, Math.min(Math.max(0f, width) / Math.max(.01f, measuredWidthAtOne),
                Math.max(0f, height) / Math.max(.01f, lineHeightAtOne))));
    }
}
