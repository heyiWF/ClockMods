package com.clockmods.ultimate.clock;

import com.clockmods.sdk.clock.ClockThemeTokens;

/** Three editable surfaces, with foregrounds derived from the surface they actually sit on. */
public final class ClockPalette {
    public static final ClockPalette DEFAULT = new ClockPalette(0xFF154974, 0xFF23557F, 0xFF9ECAFC);
    // Subtle cool material tints; GaussianGlass separately controls image brightness and contrast.
    static final ClockPalette GLASS = new ClockPalette(0xFF454545, 0xFF58616D, 0xFF636C77);
    public final boolean gaussianBlur;
    public final int blurStrength, blurBrightness;
    public final int background, panel, accent, panelAlt, badge;
    public final int onBackground, onPanel, onAccent, onPanelAlt, onBadge;
    public final int mutedBackground, mutedPanel, mutedAccent;

    public ClockPalette(int background, int panel, int accent) {
        this(background, panel, accent, false);
    }

    public ClockPalette(int background, int panel, int accent, boolean gaussianBlur) {
        this(background, panel, accent, gaussianBlur, ClockThemeTokens.DEFAULT_BLUR_STRENGTH,
                ClockThemeTokens.DEFAULT_BLUR_BRIGHTNESS);
    }

    public ClockPalette(int background, int panel, int accent, boolean gaussianBlur,
            int blurStrength, int blurBrightness) {
        this(background, panel, accent, gaussianBlur, blurStrength, blurBrightness, false, 0);
    }

    private ClockPalette(int background, int panel, int accent, boolean gaussianBlur,
            int blurStrength, int blurBrightness, boolean glassForegrounds, int glassTint) {
        this.gaussianBlur = gaussianBlur;
        this.blurStrength = Math.max(0, Math.min(100, blurStrength));
        this.blurBrightness = Math.max(0, Math.min(100, blurBrightness));
        this.background = background | 0xFF000000;
        this.panel = panel | 0xFF000000;
        this.accent = accent | 0xFF000000;
        panelAlt = mix(this.panel, this.accent, .12f);
        badge = mix(this.panel, this.accent, .65f);
        onBackground = foreground(this.background);
        int glassText = glassForegrounds ? glassTextColor(this.blurBrightness, glassTint) : 0;
        onPanel = glassForegrounds ? glassText : foreground(this.panel);
        onAccent = glassForegrounds ? glassText : foreground(this.accent);
        onPanelAlt = glassForegrounds ? glassText : foreground(panelAlt);
        onBadge = glassForegrounds ? glassText : foreground(badge);
        mutedBackground = muted(this.background);
        mutedPanel = glassForegrounds ? glassText : muted(this.panel);
        mutedAccent = glassForegrounds ? glassText : muted(this.accent);
    }

    static ClockPalette glass(int brightness, int tint) {
        return new ClockPalette(GLASS.background, GLASS.panel, GLASS.accent, true,
                ClockThemeTokens.DEFAULT_BLUR_STRENGTH, brightness, true, tint);
    }

    static int glassCenter(int brightness) {
        int value = Math.max(0, Math.min(100, brightness));
        return Math.round(value <= 50 ? 117f * value / 50f : 117f + 138f * (value - 50) / 50f);
    }

    /** Preserve the sampled card hue, moving toward a tinted extreme only as contrast requires. */
    static int glassTextColor(int brightness, int tint) {
        boolean light = brightness <= 50;
        int preferred = mix(tint, light ? 0xFFF6F8FC : 0xFF111B2C, light ? .82f : .90f);
        int extreme = light ? 0xFFFEFEFF : 0xFF010102;
        int center = glassCenter(brightness);
        int surface = 0xFF000000 | center << 16 | center << 8 | center;
        for (int step = 0; step <= 20; step++) {
            int candidate = mix(preferred, extreme, step / 20f);
            if (contrast(candidate, surface) >= 4.5) return candidate;
        }
        return extreme;
    }

    public static boolean supports(String id) {
        return UltimateClockStyles.STYLE_DUAL_BLOCKS.equals(id)
                || UltimateClockStyles.STYLE_ORBIT.equals(id)
                || UltimateClockStyles.STYLE_BUBBLES.equals(id)
                || UltimateClockStyles.STYLE_BLEND.equals(id)
                || UltimateClockStyles.STYLE_RIBBON.equals(id);
    }

    public int color(int role) { return role == 0 ? background : role == 1 ? panel : accent; }

    public ClockPalette withColor(int role, int color) {
        return new ClockPalette(role == 0 ? color : background,
                role == 1 ? color : panel, role == 2 ? color : accent, gaussianBlur,
                blurStrength, blurBrightness);
    }

    public ClockPalette withGaussianBlur(boolean enabled) {
        return new ClockPalette(background, panel, accent, enabled, blurStrength, blurBrightness);
    }

    public ClockPalette withBlurStrength(int percent) {
        return new ClockPalette(background, panel, accent, gaussianBlur, percent, blurBrightness);
    }

    public ClockPalette withBlurBrightness(int percent) {
        return new ClockPalette(background, panel, accent, gaussianBlur, blurStrength, percent);
    }

    public ClockThemeTokens applyTo(ClockThemeTokens source) {
        return source.toBuilder().background(background, background).lineColor(panel)
                .surfaceColor(accent).primaryTextColor(onAccent)
                .secondaryTextColor(onBackground).accentColor(badge)
                .gaussianBlur(gaussianBlur).blurStrength(blurStrength).blurBrightness(blurBrightness).build();
    }

    public static ClockPalette fromTokens(ClockThemeTokens theme) {
        return new ClockPalette(theme.getBackgroundStartColor(), theme.getLineColor(),
                theme.getSurfaceColor(), theme.isGaussianBlur(), theme.getBlurStrength(), theme.getBlurBrightness());
    }

    public int ring(float strength) { return mix(background, onBackground, strength); }

    public int hand() { return contrast(accent, panelAlt) >= 3 ? accent : onPanelAlt; }

    static int foreground(int surface) {
        int light = mix(surface, 0xFFF6F8FC, .82f);
        int dark = mix(surface, 0xFF111B2C, .90f);
        boolean useLight = contrast(0xFFFEFEFF, surface) >= contrast(0xFF010102, surface);
        int preferred = useLight ? light : dark;
        int extreme = useLight ? 0xFFFEFEFF : 0xFF010102;
        for (int step = 0; step <= 20; step++) {
            int candidate = mix(preferred, extreme, step / 20f);
            if (contrast(candidate, surface) >= 4.5) return candidate;
        }
        return extreme;
    }

    private static int muted(int surface) {
        int text = foreground(surface);
        int softer = mix(surface, text, .78f);
        return contrast(softer, surface) >= 4.5 ? softer : text;
    }

    static int mix(int from, int to, float amount) {
        int result = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = (from >>> shift) & 255;
            int b = (to >>> shift) & 255;
            result |= Math.round(a + (b - a) * amount) << shift;
        }
        return result;
    }

    static double contrast(int a, int b) {
        double l1 = luminance(a), l2 = luminance(b);
        return (Math.max(l1, l2) + .05) / (Math.min(l1, l2) + .05);
    }

    private static double luminance(int color) {
        return .2126 * linear((color >>> 16) & 255)
                + .7152 * linear((color >>> 8) & 255) + .0722 * linear(color & 255);
    }

    private static double linear(int channel) {
        double value = channel / 255d;
        return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
    }
}
