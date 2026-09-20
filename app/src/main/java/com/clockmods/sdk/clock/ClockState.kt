package com.clockmods.sdk.clock

import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.TimeZone

/** Immutable clock data. It contains no layout, paint, or theme decisions. */
class ClockState private constructor(
    timeMillis: Long,
    timeZone: TimeZone,
    locale: Locale,
    use24Hour: Boolean,
    showSeconds: Boolean,
    blinkColon: Boolean,
    smallSeconds: Boolean,
    portraitStacked: Boolean,
    dateLunarDualLine: Boolean,
    secondHandMotion: SecondHandMotion,
    dateText: String?,
    timeZoneText: String?,
    weatherText: String?,
    statusText: String?,
    worldClocks: List<WorldClockEntry>,
    timeScale: Float,
    dateScale: Float,
    supportingScale: Float,
    timeTransition: TimeTransition,
    timeTransitionProgress: Float,
) {
    enum class SecondHandMotion { OFF, TICK, SWEEP }
    enum class TimeTransition { FADE, SLIDE_UP, SLIDE_DOWN, SCALE, FLIP }

    private val timeMillis = timeMillis
    private val timeZone = timeZone.clone() as TimeZone
    private val locale = locale
    private val use24Hour = use24Hour
    private val showSeconds = showSeconds
    private val blinkColon = blinkColon
    private val smallSeconds = smallSeconds
    private val portraitStacked = portraitStacked
    private val dateLunarDualLine = dateLunarDualLine
    private val secondHandMotion = secondHandMotion
    private val dateText = clean(dateText)
    private val timeZoneText = clean(timeZoneText)
    private val weatherText = clean(weatherText)
    private val statusText = clean(statusText)
    private val worldClocks = Collections.unmodifiableList(worldClocks.toList())
    private val timeScale = positiveScale(timeScale)
    private val dateScale = positiveScale(dateScale)
    private val supportingScale = positiveScale(supportingScale)
    private val timeTransition = timeTransition
    private val timeTransitionProgress = transitionProgress(timeTransitionProgress)

    fun getTimeMillis() = timeMillis
    fun getTimeZone() = timeZone.clone() as TimeZone
    fun getLocale() = locale
    fun isUse24Hour() = use24Hour
    fun isShowSeconds() = showSeconds
    fun isBlinkColon() = blinkColon
    fun isSmallSeconds() = smallSeconds
    fun isPortraitStacked() = portraitStacked
    fun isDateLunarDualLine() = dateLunarDualLine
    fun getSecondHandMotion() = secondHandMotion
    fun getDateText() = dateText
    fun getTimeZoneText() = timeZoneText
    fun getWeatherText() = weatherText
    fun getStatusText() = statusText
    fun getWorldClocks() = worldClocks
    fun getTimeScale() = timeScale
    fun getDateScale() = dateScale
    fun getSupportingScale() = supportingScale
    fun getTimeTransition() = timeTransition
    fun getTimeTransitionProgress() = timeTransitionProgress

    fun newCalendar(): Calendar = Calendar.getInstance(timeZone, locale).also {
        it.timeInMillis = timeMillis
    }

    class Builder private constructor(private val timeMillis: Long) {
        private var timeZone: TimeZone = TimeZone.getDefault()
        private var locale: Locale = Locale.getDefault()
        private var use24Hour = true
        private var showSeconds = true
        private var blinkColon = false
        private var smallSeconds = false
        private var portraitStacked = false
        private var dateLunarDualLine = false
        private var secondHandMotion = SecondHandMotion.TICK
        private var dateText = ""
        private var timeZoneText = ""
        private var weatherText = ""
        private var statusText = ""
        private var worldClocks: List<WorldClockEntry> = emptyList()
        private var timeScale = 1f
        private var dateScale = 1f
        private var supportingScale = 1f
        private var timeTransition = TimeTransition.FADE
        private var timeTransitionProgress = 1f

        fun timeZone(value: TimeZone?): Builder = apply {
            requireNotNull(value) { "timeZone must not be null" }
            timeZone = value.clone() as TimeZone
        }

        fun locale(value: Locale?): Builder = apply {
            locale = requireNotNull(value) { "locale must not be null" }
        }

        fun use24Hour(value: Boolean) = apply { use24Hour = value }
        fun showSeconds(value: Boolean) = apply { showSeconds = value }
        fun blinkColon(value: Boolean) = apply { blinkColon = value }
        fun smallSeconds(value: Boolean) = apply { smallSeconds = value }
        fun portraitStacked(value: Boolean) = apply { portraitStacked = value }
        fun dateLunarDualLine(value: Boolean) = apply { dateLunarDualLine = value }

        fun secondHandMotion(value: SecondHandMotion?): Builder = apply {
            secondHandMotion = requireNotNull(value) { "secondHandMotion must not be null" }
        }

        fun dateText(value: String?) = apply { dateText = value.orEmpty() }
        fun timeZoneText(value: String?) = apply { timeZoneText = value.orEmpty() }
        fun weatherText(value: String?) = apply { weatherText = value.orEmpty() }
        fun statusText(value: String?) = apply { statusText = value.orEmpty() }

        fun worldClocks(value: List<WorldClockEntry>?): Builder = apply {
            worldClocks = value?.toList() ?: emptyList()
        }

        fun timeScale(value: Float) = apply { timeScale = value }
        fun dateScale(value: Float) = apply { dateScale = value }
        fun supportingScale(value: Float) = apply { supportingScale = value }

        fun timeTransition(value: TimeTransition?) = apply {
            timeTransition = requireNotNull(value) { "timeTransition must not be null" }
        }

        fun timeTransitionProgress(value: Float) = apply { timeTransitionProgress = value }

        fun build() = ClockState(
            timeMillis,
            timeZone,
            locale,
            use24Hour,
            showSeconds,
            blinkColon,
            smallSeconds,
            portraitStacked,
            dateLunarDualLine,
            secondHandMotion,
            dateText,
            timeZoneText,
            weatherText,
            statusText,
            worldClocks,
            timeScale,
            dateScale,
            supportingScale,
            timeTransition,
            timeTransitionProgress,
        )

        internal companion object {
            @JvmSynthetic
            fun create(timeMillis: Long) = Builder(timeMillis)
        }
    }

    companion object {
        @JvmStatic
        fun builder(timeMillis: Long) = Builder.create(timeMillis)

        private fun clean(value: String?) = value?.trim().orEmpty()
        private fun positiveScale(value: Float) =
            if (value.isNaN() || value.isInfinite() || value <= 0f) 1f else value
        private fun transitionProgress(value: Float) =
            if (value.isNaN()) 1f else value.coerceIn(0f, 1f)
    }
}
