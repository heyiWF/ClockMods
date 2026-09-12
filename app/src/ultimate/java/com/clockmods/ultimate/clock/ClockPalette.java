package com.clockmods.ultimate.clock;

import com.clockmods.sdk.clock.ClockThemeTokens;

/** Three editable surfaces, with foregrounds derived from the surface they actually sit on. */
public final class ClockPalette {
    public static final ClockPalette DEFAULT = new ClockPalette(0xFF154974, 0xFF23557F, 0xFF9ECAFC);
    public final int background, panel, accent, panelAlt, badge;
    public final int onBackground, onPanel, onAccent, onPanelAlt, onBadge;
    public final int mutedBackground, mutedPanel, mutedAccent;

    public ClockPalette(int background, int panel, int accent) {
        this.background = background | 0xFF000000;
        this.panel = panel | 0xFF000000;
        this.accent = accent | 0xFF000000;
        panelAlt = mix(this.panel, this.accent, .12f);
        badge = mix(this.panel, this.accent, .65f);
        onBackground = foreground(this.background);
        onPanel = foreground(this.panel);
        onAccent = foreground(this.accent);
        onPanelAlt = foreground(panelAlt);
        onBadge = foreground(badge);
        mutedBackground = muted(this.background);
        mutedPanel = muted(this.panel);
        mutedAccent = muted(this.accent);
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
                role == 1 ? color : panel, role == 2 ? color : accent);
    }

    public ClockThemeTokens applyTo(ClockThemeTokens source) {
        return source.toBuilder().background(background, background).lineColor(panel)
                .surfaceColor(accent).primaryTextColor(onAccent)
                .secondaryTextColor(onBackground).accentColor(badge).build();
    }

    public static ClockPalette fromTokens(ClockThemeTokens theme) {
        return new ClockPalette(theme.getBackgroundStartColor(), theme.getLineColor(),
                theme.getSurfaceColor());
    }

    public int ring(float strength) { return mix(background, onBackground, strength); }

    public int hand() { return contrast(accent, panelAlt) >= 3 ? accent : onPanelAlt; }

    static int foreground(int surface) {
        int light = 0xFFF4F7FC;
        int dark = 0xFF101923;
        int best = contrast(light, surface) >= contrast(dark, surface) ? light : dark;
        if (contrast(best, surface) >= 4.5) return best;
        return contrast(0xFFFFFFFF, surface) >= contrast(0xFF000000, surface)
                ? 0xFFFFFFFF : 0xFF000000;
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
