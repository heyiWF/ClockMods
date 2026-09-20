package com.clockmods.widget.model

import com.clockmods.background.FontCatalog
import java.util.TimeZone

/** Immutable, normalized per-instance contract. Persisted IDs must remain stable. */
class WidgetConfig private constructor(builder: Builder) {
    @JvmField val schemaVersion: Int = 1
    @JvmField val appWidgetId: Int = builder.appWidgetId
    @JvmField val kind: WidgetKind = builder.kind ?: WidgetKind.DIGITAL
    @JvmField val themeId: String = builder.themeId.takeIf(THEME_ID_SET::contains) ?: THEME_IDS[0]

    private val validZone = ZONES.contains(builder.timeZoneId)
    @JvmField val timeZoneId: String = if (validZone) builder.timeZoneId else TimeZone.getDefault().id
    @JvmField val useSystemTimeZone: Boolean = builder.useSystemTimeZone || !validZone
    @JvmField val useSystemTimeFormat: Boolean = builder.useSystemTimeFormat
    @JvmField val use24Hour: Boolean = builder.use24Hour
    @JvmField val showSeconds: Boolean = builder.showSeconds &&
        (kind == WidgetKind.DIGITAL || kind == WidgetKind.WEATHER)
    @JvmField val showDate: Boolean = builder.showDate
    @JvmField val showWeekday: Boolean = builder.showWeekday
    @JvmField val showLunar: Boolean = builder.showLunar
    @JvmField val showWeatherDescription: Boolean = builder.showWeatherDescription
    @JvmField val showLocation: Boolean = builder.showLocation
    @JvmField val backgroundAlpha: Int = builder.backgroundAlpha.coerceIn(0, 255)
    @JvmField val textScale: Float = if (builder.textScale.isNaN()) 1f else builder.textScale.coerceIn(.85f, 1.2f)
    @JvmField val tapAction: String = builder.tapAction.takeIf(TAP_ACTIONS::contains) ?: "open_clock"
    @JvmField val updatedAt: Long = builder.updatedAt.coerceAtLeast(0)
    @JvmField val darkText: Boolean = builder.darkText
    @JvmField val fontId: String = builder.fontId.takeIf(FONTS::contains) ?: FONT_THEME

    fun zone(): TimeZone = if (useSystemTimeZone) TimeZone.getDefault() else TimeZone.getTimeZone(timeZoneId)

    fun toBuilder(): Builder = Builder(this)

    class Builder private constructor() {
        internal var appWidgetId: Int = 0
        internal var kind: WidgetKind? = null
        internal var themeId: String = "system.dynamic"
        internal var timeZoneId: String = TimeZone.getDefault().id
        internal var useSystemTimeZone: Boolean = true
        internal var useSystemTimeFormat: Boolean = true
        internal var use24Hour: Boolean = true
        internal var showSeconds: Boolean = false
        internal var showDate: Boolean = true
        internal var showWeekday: Boolean = true
        internal var showLunar: Boolean = true
        internal var showWeatherDescription: Boolean = true
        internal var showLocation: Boolean = true
        internal var backgroundAlpha: Int = 255
        internal var textScale: Float = 1f
        internal var tapAction: String = "open_clock"
        internal var updatedAt: Long = System.currentTimeMillis()
        internal var darkText: Boolean = false
        internal var fontId: String = FONT_THEME

        internal constructor(id: Int, kind: WidgetKind?) : this() {
            appWidgetId = id
            this.kind = kind
        }

        internal constructor(config: WidgetConfig) : this() {
            appWidgetId = config.appWidgetId
            kind = config.kind
            themeId = config.themeId
            timeZoneId = config.timeZoneId
            useSystemTimeZone = config.useSystemTimeZone
            useSystemTimeFormat = config.useSystemTimeFormat
            use24Hour = config.use24Hour
            showSeconds = config.showSeconds
            showDate = config.showDate
            showWeekday = config.showWeekday
            showLunar = config.showLunar
            showWeatherDescription = config.showWeatherDescription
            showLocation = config.showLocation
            backgroundAlpha = config.backgroundAlpha
            textScale = config.textScale
            tapAction = config.tapAction
            updatedAt = config.updatedAt
            darkText = config.darkText
            fontId = config.fontId
        }

        fun appWidgetId(value: Int) = apply { appWidgetId = value }
        fun kind(value: WidgetKind?) = apply { kind = value }
        fun themeId(value: String?) = apply { themeId = value.orEmpty() }
        fun timeZoneId(value: String?) = apply { timeZoneId = value.orEmpty() }
        fun useSystemTimeZone(value: Boolean) = apply { useSystemTimeZone = value }
        fun useSystemTimeFormat(value: Boolean) = apply { useSystemTimeFormat = value }
        fun use24Hour(value: Boolean) = apply { use24Hour = value }
        fun showSeconds(value: Boolean) = apply { showSeconds = value }
        fun showDate(value: Boolean) = apply { showDate = value }
        fun showWeekday(value: Boolean) = apply { showWeekday = value }
        fun showLunar(value: Boolean) = apply { showLunar = value }
        fun showWeatherDescription(value: Boolean) = apply { showWeatherDescription = value }
        fun showLocation(value: Boolean) = apply { showLocation = value }
        fun backgroundAlpha(value: Int) = apply { backgroundAlpha = value }
        fun textScale(value: Float) = apply { textScale = value }
        fun tapAction(value: String?) = apply { tapAction = value.orEmpty() }
        fun updatedAt(value: Long) = apply { updatedAt = value }
        fun darkText(value: Boolean) = apply { darkText = value }
        fun fontId(value: String?) = apply { fontId = value.orEmpty() }
        fun build(): WidgetConfig = WidgetConfig(this)
    }

    companion object {
        @JvmField val THEME_IDS: Array<String> = arrayOf(
            "system.dynamic", "glass.light", "instrument.dark", "paper.warm", "neon.night", "transparent.clean",
        )

        /** Follow the selected theme's font structure - the default. */
        const val FONT_THEME: String = "theme"

        /** Stable persisted font ids, kept in the app catalog's order. */
        @JvmField val FONT_IDS: Array<String> = buildFontIds()

        private val THEME_ID_SET = THEME_IDS.toHashSet()
        private val ZONES = TimeZone.getAvailableIDs().toHashSet()
        private val FONTS = FONT_IDS.toHashSet()
        private val TAP_ACTIONS = setOf("open_clock", "open_calendar", "open_weather", "open_config")

        private fun buildFontIds(): Array<String> {
            val families = FontCatalog.options()
            return Array(families.size + 1) { index ->
                if (index == 0) FONT_THEME else families[index - 1].id
            }
        }

        @JvmStatic fun builder(id: Int, kind: WidgetKind?): Builder = Builder(id, kind)
    }
}
