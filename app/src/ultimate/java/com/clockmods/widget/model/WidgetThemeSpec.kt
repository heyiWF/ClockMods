package com.clockmods.widget.model

class WidgetThemeSpec(
    @JvmField val id: String,
    @JvmField val displayNameRes: Int,
    @JvmField val backgroundDrawableRes: Int,
    @JvmField val primaryTextColor: Int,
    @JvmField val secondaryTextColor: Int,
    @JvmField val accentColor: Int,
    @JvmField val fontLayoutVariant: Font,
    @JvmField val defaultBackgroundAlpha: Int,
) {
    enum class Font { SANS, SERIF, MONOSPACE }

    @JvmField val iconColor: Int = accentColor
    @JvmField val supportsAnalog: Boolean = true
    @JvmField val supportsDigital: Boolean = true
    @JvmField val supportsWeather: Boolean = true
    @JvmField val supportsCalendar: Boolean = true
}
