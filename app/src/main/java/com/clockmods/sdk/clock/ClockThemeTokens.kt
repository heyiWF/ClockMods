package com.clockmods.sdk.clock

import android.graphics.Typeface

/** Portable visual tokens shared by previews and renderers. */
class ClockThemeTokens private constructor(
    backgroundStartColor: Int,
    backgroundEndColor: Int,
    surfaceColor: Int,
    primaryTextColor: Int,
    secondaryTextColor: Int,
    accentColor: Int,
    lineColor: Int,
    displayFontFamily: String,
    supportingFontFamily: String,
    displayTypeface: Typeface?,
    supportingTypeface: Typeface?,
    strokeScale: Float,
    gaussianBlur: Boolean,
    blurStrength: Int,
    blurBrightness: Int,
    cardShadow: Boolean,
) {
    companion object {
        const val DEFAULT_BLUR_STRENGTH = 50
        const val DEFAULT_BLUR_BRIGHTNESS = 25

        @JvmStatic
        fun builder() = Builder()
    }

    private val backgroundStartColor = backgroundStartColor
    private val backgroundEndColor = backgroundEndColor
    private val surfaceColor = surfaceColor
    private val primaryTextColor = primaryTextColor
    private val secondaryTextColor = secondaryTextColor
    private val accentColor = accentColor
    private val lineColor = lineColor
    private val displayFontFamily = displayFontFamily
    private val supportingFontFamily = supportingFontFamily
    private val displayTypeface = displayTypeface
    private val supportingTypeface = supportingTypeface
    private val strokeScale = strokeScale
    private val gaussianBlur = gaussianBlur
    private val blurStrength = blurStrength
    private val blurBrightness = blurBrightness
    private val cardShadow = cardShadow

    fun toBuilder() = Builder()
        .background(backgroundStartColor, backgroundEndColor)
        .surfaceColor(surfaceColor)
        .primaryTextColor(primaryTextColor)
        .secondaryTextColor(secondaryTextColor)
        .accentColor(accentColor)
        .lineColor(lineColor)
        .fonts(displayFontFamily, supportingFontFamily)
        .typefaces(displayTypeface, supportingTypeface)
        .strokeScale(strokeScale)
        .gaussianBlur(gaussianBlur)
        .blurStrength(blurStrength)
        .blurBrightness(blurBrightness)
        .cardShadow(cardShadow)

    fun getBackgroundStartColor() = backgroundStartColor
    fun getBackgroundEndColor() = backgroundEndColor
    fun getSurfaceColor() = surfaceColor
    fun getPrimaryTextColor() = primaryTextColor
    fun getSecondaryTextColor() = secondaryTextColor
    fun getAccentColor() = accentColor
    fun getLineColor() = lineColor
    fun getDisplayFontFamily() = displayFontFamily
    fun getSupportingFontFamily() = supportingFontFamily
    fun getDisplayTypeface() = displayTypeface
    fun getSupportingTypeface() = supportingTypeface
    fun getStrokeScale() = strokeScale
    fun isGaussianBlur() = gaussianBlur
    fun getBlurStrength() = blurStrength
    fun getBlurBrightness() = blurBrightness
    fun isCardShadow() = cardShadow

    class Builder {
        private var backgroundStartColor = 0xFF101418.toInt()
        private var backgroundEndColor = 0xFF080A0D.toInt()
        private var surfaceColor = 0xFF20262D.toInt()
        private var primaryTextColor = 0xFFFFFFFF.toInt()
        private var secondaryTextColor = 0xFFADB5BD.toInt()
        private var accentColor = 0xFFFF5A52.toInt()
        private var lineColor = 0xFF6B747E.toInt()
        private var displayFontFamily = "sans-serif"
        private var supportingFontFamily = "sans-serif"
        private var displayTypeface: Typeface? = null
        private var supportingTypeface: Typeface? = null
        private var strokeScale = 1f
        private var gaussianBlur = false
        private var blurStrength = DEFAULT_BLUR_STRENGTH
        private var blurBrightness = DEFAULT_BLUR_BRIGHTNESS
        private var cardShadow = false

        fun gaussianBlur(enabled: Boolean) = apply { gaussianBlur = enabled }
        fun cardShadow(enabled: Boolean) = apply { cardShadow = enabled }
        fun blurStrength(percent: Int) = apply { blurStrength = percent.coerceIn(0, 100) }
        fun blurBrightness(percent: Int) = apply { blurBrightness = percent.coerceIn(0, 100) }
        fun background(startColor: Int, endColor: Int) = apply {
            backgroundStartColor = startColor
            backgroundEndColor = endColor
        }
        fun surfaceColor(color: Int) = apply { surfaceColor = color }
        fun primaryTextColor(color: Int) = apply { primaryTextColor = color }
        fun secondaryTextColor(color: Int) = apply { secondaryTextColor = color }
        fun accentColor(color: Int) = apply { accentColor = color }
        fun lineColor(color: Int) = apply { lineColor = color }
        fun fonts(displayFamily: String?, supportingFamily: String?) = apply {
            displayFontFamily = requireFamily(displayFamily)
            supportingFontFamily = requireFamily(supportingFamily)
        }
        fun typefaces(display: Typeface?, supporting: Typeface?) = apply {
            displayTypeface = display
            supportingTypeface = supporting
        }
        fun strokeScale(scale: Float) = apply {
            require(scale.isFinite() && scale > 0f) { "strokeScale must be finite and positive" }
            strokeScale = scale
        }
        fun build() = ClockThemeTokens(
            backgroundStartColor,
            backgroundEndColor,
            surfaceColor,
            primaryTextColor,
            secondaryTextColor,
            accentColor,
            lineColor,
            displayFontFamily,
            supportingFontFamily,
            displayTypeface,
            supportingTypeface,
            strokeScale,
            gaussianBlur,
            blurStrength,
            blurBrightness,
            cardShadow,
        )

        private fun requireFamily(family: String?) = family?.trim().orEmpty().also {
            require(it.isNotEmpty()) { "font family must not be blank" }
        }
    }
}
