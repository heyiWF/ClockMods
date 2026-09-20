package com.clockmods.ultimate.clock

import com.clockmods.sdk.clock.ClockThemeTokens

/** Three editable surfaces, with foregrounds derived from the surface they actually sit on. */
class ClockPalette private constructor(
    background: Int,
    panel: Int,
    accent: Int,
    @JvmField val gaussianBlur: Boolean,
    blurStrength: Int,
    blurBrightness: Int,
    glassForegrounds: Boolean,
    glassTint: Int,
    @JvmField val cardShadow: Boolean,
) {
    @JvmField val blurStrength: Int = blurStrength.coerceIn(0, 100)
    @JvmField val blurBrightness: Int = blurBrightness.coerceIn(0, 100)
    @JvmField val background: Int = background or 0xFF000000.toInt()
    @JvmField val panel: Int = panel or 0xFF000000.toInt()
    @JvmField val accent: Int = accent or 0xFF000000.toInt()
    @JvmField val panelAlt: Int = mix(this.panel, this.accent, .12f)
    @JvmField val badge: Int = mix(this.panel, this.accent, .65f)
    @JvmField val onBackground: Int = foreground(this.background)
    private val glassText: Int = if (glassForegrounds) glassTextColor(this.blurBrightness, glassTint) else 0
    @JvmField val onPanel: Int = if (glassForegrounds) glassText else foreground(this.panel)
    @JvmField val onAccent: Int = if (glassForegrounds) glassText else foreground(this.accent)
    @JvmField val onPanelAlt: Int = if (glassForegrounds) glassText else foreground(panelAlt)
    @JvmField val onBadge: Int = if (glassForegrounds) glassText else foreground(badge)
    @JvmField val mutedBackground: Int = muted(this.background)
    @JvmField val mutedPanel: Int = if (glassForegrounds) glassText else muted(this.panel)
    @JvmField val mutedAccent: Int = if (glassForegrounds) glassText else muted(this.accent)

    constructor(background: Int, panel: Int, accent: Int) : this(
        background, panel, accent, false,
        ClockThemeTokens.DEFAULT_BLUR_STRENGTH,
        ClockThemeTokens.DEFAULT_BLUR_BRIGHTNESS,
        false, 0, true,
    )

    constructor(background: Int, panel: Int, accent: Int, gaussianBlur: Boolean) : this(
        background, panel, accent, gaussianBlur,
        ClockThemeTokens.DEFAULT_BLUR_STRENGTH,
        ClockThemeTokens.DEFAULT_BLUR_BRIGHTNESS,
        false, 0, true,
    )

    constructor(
        background: Int,
        panel: Int,
        accent: Int,
        gaussianBlur: Boolean,
        blurStrength: Int,
        blurBrightness: Int,
    ) : this(background, panel, accent, gaussianBlur, blurStrength, blurBrightness, false, 0, true)

    fun color(role: Int): Int = when (role) {
        0 -> background
        1 -> panel
        else -> accent
    }

    fun withColor(role: Int, color: Int): ClockPalette = ClockPalette(
        if (role == 0) color else background,
        if (role == 1) color else panel,
        if (role == 2) color else accent,
        gaussianBlur, blurStrength, blurBrightness, false, 0, cardShadow,
    )

    fun withGaussianBlur(enabled: Boolean): ClockPalette = ClockPalette(
        background, panel, accent, enabled, blurStrength, blurBrightness, false, 0, cardShadow,
    )

    fun withBlurStrength(percent: Int): ClockPalette = ClockPalette(
        background, panel, accent, gaussianBlur, percent, blurBrightness, false, 0, cardShadow,
    )

    fun withBlurBrightness(percent: Int): ClockPalette = ClockPalette(
        background, panel, accent, gaussianBlur, blurStrength, percent, false, 0, cardShadow,
    )

    /** Turns the cards' soft elevation shadow on or off without touching their colourway. */
    fun withCardShadow(enabled: Boolean): ClockPalette = ClockPalette(
        background, panel, accent, gaussianBlur, blurStrength, blurBrightness, false, 0, enabled,
    )

    fun applyTo(source: ClockThemeTokens): ClockThemeTokens = source.toBuilder()
        .background(background, background)
        .lineColor(panel)
        .surfaceColor(accent)
        .primaryTextColor(onAccent)
        .secondaryTextColor(onBackground)
        .accentColor(badge)
        .gaussianBlur(gaussianBlur)
        .blurStrength(blurStrength)
        .blurBrightness(blurBrightness)
        .cardShadow(cardShadow)
        .build()

    fun ring(strength: Float): Int = mix(background, onBackground, strength)

    fun hand(): Int = if (contrast(accent, panelAlt) >= 3) accent else onPanelAlt

    companion object {
        @JvmField
        val DEFAULT = ClockPalette(0xFF154974.toInt(), 0xFF23557F.toInt(), 0xFF9ECAFC.toInt(), false,
            ClockThemeTokens.DEFAULT_BLUR_STRENGTH, ClockThemeTokens.DEFAULT_BLUR_BRIGHTNESS, false, 0, true)

        // Subtle cool material tints; GaussianGlass separately controls image brightness and contrast.
        @JvmField
        val GLASS = ClockPalette(0xFF454545.toInt(), 0xFF58616D.toInt(), 0xFF636C77.toInt(), false,
            ClockThemeTokens.DEFAULT_BLUR_STRENGTH, ClockThemeTokens.DEFAULT_BLUR_BRIGHTNESS, false, 0, true)

        @JvmStatic
        fun glass(brightness: Int, tint: Int): ClockPalette = ClockPalette(
            GLASS.background, GLASS.panel, GLASS.accent, true,
            ClockThemeTokens.DEFAULT_BLUR_STRENGTH, brightness, true, tint, true,
        )

        /** Choose readable foregrounds from the sampled image after its brightness overlay. */
        @JvmStatic
        fun glassTextColor(brightness: Int, tint: Int): Int = foreground(
            GaussianGlass.applyBrightnessOverlay(tint, brightness)
        )

        @JvmStatic
        fun supports(id: String?): Boolean = id == UltimateClockStyles.STYLE_DUAL_BLOCKS ||
            id == UltimateClockStyles.STYLE_ORBIT ||
            id == UltimateClockStyles.STYLE_BUBBLES ||
            id == UltimateClockStyles.STYLE_BLEND ||
            id == UltimateClockStyles.STYLE_RIBBON

        @JvmStatic
        fun fromTokens(theme: ClockThemeTokens): ClockPalette = ClockPalette(
            theme.getBackgroundStartColor(), theme.getLineColor(), theme.getSurfaceColor(),
            theme.isGaussianBlur(), theme.getBlurStrength(), theme.getBlurBrightness(), false, 0,
            theme.isCardShadow(),
        )

        @JvmStatic
        fun foreground(surface: Int): Int {
            val light = mix(surface, 0xFFF6F8FC.toInt(), .82f)
            val dark = mix(surface, 0xFF111B2C.toInt(), .90f)
            val useLight = contrast(0xFFFEFEFF.toInt(), surface) >= contrast(0xFF010102.toInt(), surface)
            val preferred = if (useLight) light else dark
            val extreme = if (useLight) 0xFFFEFEFF.toInt() else 0xFF010102.toInt()
            for (step in 0..20) {
                val candidate = mix(preferred, extreme, step / 20f)
                if (contrast(candidate, surface) >= 4.5) return candidate
            }
            return extreme
        }

        private fun muted(surface: Int): Int {
            val text = foreground(surface)
            val softer = mix(surface, text, .78f)
            return if (contrast(softer, surface) >= 4.5) softer else text
        }

        @JvmStatic
        fun mix(from: Int, to: Int, amount: Float): Int {
            var result = 0xFF000000.toInt()
            var shift = 0
            while (shift <= 16) {
                val a = (from ushr shift) and 255
                val b = (to ushr shift) and 255
                result = result or (Math.round(a + (b - a) * amount) shl shift)
                shift += 8
            }
            return result
        }

        @JvmStatic
        fun contrast(a: Int, b: Int): Double {
            val l1 = luminance(a)
            val l2 = luminance(b)
            return (maxOf(l1, l2) + .05) / (minOf(l1, l2) + .05)
        }

        private fun luminance(color: Int): Double =
            .2126 * linear((color ushr 16) and 255) +
                .7152 * linear((color ushr 8) and 255) +
                .0722 * linear(color and 255)

        private fun linear(channel: Int): Double {
            val value = channel / 255.0
            return if (value <= .04045) value / 12.92 else Math.pow((value + .055) / 1.055, 2.4)
        }
    }
}
