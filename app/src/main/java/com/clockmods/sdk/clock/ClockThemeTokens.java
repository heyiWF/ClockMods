package com.clockmods.sdk.clock;

/**
 * Portable visual tokens shared by previews and renderers.
 *
 * <p>Structure and geometry intentionally remain in {@link ClockRenderer}; changing these tokens
 * alone is not considered a new clock style.</p>
 */
public final class ClockThemeTokens {
    private final int backgroundStartColor;
    private final int backgroundEndColor;
    private final int surfaceColor;
    private final int primaryTextColor;
    private final int secondaryTextColor;
    private final int accentColor;
    private final int lineColor;
    private final String displayFontFamily;
    private final String supportingFontFamily;
    private final float strokeScale;

    private ClockThemeTokens(Builder builder) {
        backgroundStartColor = builder.backgroundStartColor;
        backgroundEndColor = builder.backgroundEndColor;
        surfaceColor = builder.surfaceColor;
        primaryTextColor = builder.primaryTextColor;
        secondaryTextColor = builder.secondaryTextColor;
        accentColor = builder.accentColor;
        lineColor = builder.lineColor;
        displayFontFamily = builder.displayFontFamily;
        supportingFontFamily = builder.supportingFontFamily;
        strokeScale = builder.strokeScale;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getBackgroundStartColor() { return backgroundStartColor; }
    public int getBackgroundEndColor() { return backgroundEndColor; }
    public int getSurfaceColor() { return surfaceColor; }
    public int getPrimaryTextColor() { return primaryTextColor; }
    public int getSecondaryTextColor() { return secondaryTextColor; }
    public int getAccentColor() { return accentColor; }
    public int getLineColor() { return lineColor; }
    public String getDisplayFontFamily() { return displayFontFamily; }
    public String getSupportingFontFamily() { return supportingFontFamily; }
    public float getStrokeScale() { return strokeScale; }

    public static final class Builder {
        private int backgroundStartColor = 0xFF101418;
        private int backgroundEndColor = 0xFF080A0D;
        private int surfaceColor = 0xFF20262D;
        private int primaryTextColor = 0xFFFFFFFF;
        private int secondaryTextColor = 0xFFADB5BD;
        private int accentColor = 0xFFFF5A52;
        private int lineColor = 0xFF6B747E;
        private String displayFontFamily = "sans-serif";
        private String supportingFontFamily = "sans-serif";
        private float strokeScale = 1f;

        public Builder background(int startColor, int endColor) {
            backgroundStartColor = startColor;
            backgroundEndColor = endColor;
            return this;
        }

        public Builder surfaceColor(int color) { surfaceColor = color; return this; }
        public Builder primaryTextColor(int color) { primaryTextColor = color; return this; }
        public Builder secondaryTextColor(int color) { secondaryTextColor = color; return this; }
        public Builder accentColor(int color) { accentColor = color; return this; }
        public Builder lineColor(int color) { lineColor = color; return this; }

        public Builder fonts(String displayFamily, String supportingFamily) {
            displayFontFamily = requireFamily(displayFamily);
            supportingFontFamily = requireFamily(supportingFamily);
            return this;
        }

        public Builder strokeScale(float scale) {
            if (Float.isNaN(scale) || Float.isInfinite(scale) || scale <= 0f) {
                throw new IllegalArgumentException("strokeScale must be finite and positive");
            }
            strokeScale = scale;
            return this;
        }

        public ClockThemeTokens build() {
            return new ClockThemeTokens(this);
        }

        private static String requireFamily(String family) {
            if (family == null || family.trim().length() == 0) {
                throw new IllegalArgumentException("font family must not be blank");
            }
            return family.trim();
        }
    }
}
