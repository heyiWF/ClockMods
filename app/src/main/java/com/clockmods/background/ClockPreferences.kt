package com.clockmods.background

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import com.clockmods.ui.DateFormatter
import com.clockmods.weather.WeatherTemperatureFormatter
import java.util.Calendar

/** SharedPreferences facade. Key names and defaults are part of the upgrade contract. */
open class ClockPreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBackgroundMode(): String = preferences.getString(KEY_BACKGROUND_MODE, MODE_COLOR) ?: MODE_COLOR
    fun setBackgroundMode(mode: String?) { preferences.edit().putString(KEY_BACKGROUND_MODE, mode).apply() }
    fun getBackgroundColor(defaultColor: Int): Int = preferences.getInt(KEY_BACKGROUND_COLOR, defaultColor)
    fun setBackgroundColor(color: Int) { preferences.edit().putInt(KEY_BACKGROUND_COLOR, color).apply() }
    fun isDimBackground(): Boolean = preferences.getBoolean(KEY_DIM_BACKGROUND, DEFAULT_DIM_BACKGROUND)
    fun setDimBackground(value: Boolean) { preferences.edit().putBoolean(KEY_DIM_BACKGROUND, value).apply() }
    fun isScheduleDimBackground(): Boolean = preferences.getBoolean(KEY_SCHEDULE_DIM_BACKGROUND, DEFAULT_SCHEDULE_DIM_BACKGROUND)
    fun setScheduleDimBackground(value: Boolean) { preferences.edit().putBoolean(KEY_SCHEDULE_DIM_BACKGROUND, value).apply() }
    fun getDimStartMinutes(): Int = preferences.getInt(KEY_DIM_START_MINUTES, DEFAULT_DIM_START_MINUTES)
    fun setDimStartMinutes(value: Int) { preferences.edit().putInt(KEY_DIM_START_MINUTES, value).apply() }
    fun getDimEndMinutes(): Int = preferences.getInt(KEY_DIM_END_MINUTES, DEFAULT_DIM_END_MINUTES)
    fun setDimEndMinutes(value: Int) { preferences.edit().putInt(KEY_DIM_END_MINUTES, value).apply() }

    fun getTimeFontScale(): Float = clampScale(preferences.getFloat(KEY_TIME_FONT_SCALE, DEFAULT_TIME_FONT_SCALE))
    fun setTimeFontScale(value: Float) { preferences.edit().putFloat(KEY_TIME_FONT_SCALE, clampScale(value)).apply() }
    fun getDateFontScale(): Float = clampScale(preferences.getFloat(KEY_DATE_FONT_SCALE, DEFAULT_DATE_FONT_SCALE))
    fun setDateFontScale(value: Float) { preferences.edit().putFloat(KEY_DATE_FONT_SCALE, clampScale(value)).apply() }
    fun getTimeColor(): Int = preferences.getInt(KEY_TIME_COLOR, DEFAULT_TEXT_COLOR)
    fun setTimeColor(value: Int) { preferences.edit().putInt(KEY_TIME_COLOR, value).apply() }
    fun getDateColor(): Int = preferences.getInt(KEY_DATE_COLOR, DEFAULT_TEXT_COLOR)
    fun setDateColor(value: Int) { preferences.edit().putInt(KEY_DATE_COLOR, value).apply() }
    fun isShowStatusIcons(): Boolean = preferences.getBoolean(KEY_SHOW_STATUS_ICONS, DEFAULT_SHOW_STATUS_ICONS)
    fun setShowStatusIcons(value: Boolean) { preferences.edit().putBoolean(KEY_SHOW_STATUS_ICONS, value).apply() }
    fun getStatusIconScale(): Float = normalizeStatusIconScale(preferences.getFloat(KEY_STATUS_ICON_SCALE, DEFAULT_STATUS_ICON_SCALE))
    fun setStatusIconScale(value: Float) { preferences.edit().putFloat(KEY_STATUS_ICON_SCALE, normalizeStatusIconScale(value)).apply() }
    fun isBlinkColon(): Boolean = preferences.getBoolean(KEY_BLINK_COLON, DEFAULT_BLINK_COLON)
    fun setBlinkColon(value: Boolean) { preferences.edit().putBoolean(KEY_BLINK_COLON, value).apply() }
    fun isAnimateTimeChanges(): Boolean = preferences.getBoolean(KEY_ANIMATE_TIME_CHANGES, DEFAULT_ANIMATE_TIME_CHANGES)
    fun setAnimateTimeChanges(value: Boolean) { preferences.edit().putBoolean(KEY_ANIMATE_TIME_CHANGES, value).apply() }
    fun getTimeTransition(): String = normalizeTimeTransition(preferences.getString(KEY_TIME_TRANSITION, DEFAULT_TIME_TRANSITION))
    fun setTimeTransition(value: String?) { preferences.edit().putString(KEY_TIME_TRANSITION, normalizeTimeTransition(value)).apply() }
    fun isHourlyChimeEnabled(): Boolean = preferences.getBoolean(KEY_HOURLY_CHIME, DEFAULT_HOURLY_CHIME)
    fun setHourlyChimeEnabled(value: Boolean) { preferences.edit().putBoolean(KEY_HOURLY_CHIME, value).apply() }
    fun isHalfHourChimeEnabled(): Boolean = preferences.getBoolean(KEY_HALF_HOUR_CHIME, DEFAULT_HALF_HOUR_CHIME)
    fun setHalfHourChimeEnabled(value: Boolean) { preferences.edit().putBoolean(KEY_HALF_HOUR_CHIME, value).apply() }
    fun isHourlyChimeQuietEnabled(): Boolean = preferences.getBoolean(KEY_HOURLY_CHIME_QUIET, DEFAULT_HOURLY_CHIME_QUIET)
    fun setHourlyChimeQuietEnabled(value: Boolean) { preferences.edit().putBoolean(KEY_HOURLY_CHIME_QUIET, value).apply() }
    fun getHourlyChimeQuietStart(): Int = preferences.getInt(KEY_HOURLY_CHIME_QUIET_START, DEFAULT_HOURLY_CHIME_QUIET_START)
    fun setHourlyChimeQuietStart(value: Int) { preferences.edit().putInt(KEY_HOURLY_CHIME_QUIET_START, value).apply() }
    fun getHourlyChimeQuietEnd(): Int = preferences.getInt(KEY_HOURLY_CHIME_QUIET_END, DEFAULT_HOURLY_CHIME_QUIET_END)
    fun setHourlyChimeQuietEnd(value: Int) { preferences.edit().putInt(KEY_HOURLY_CHIME_QUIET_END, value).apply() }

    fun isBoldText(): Boolean = preferences.getBoolean(KEY_BOLD_TEXT, DEFAULT_BOLD_TEXT)
    fun setBoldText(value: Boolean) { preferences.edit().putBoolean(KEY_BOLD_TEXT, value).apply() }
    fun getFontFamily(): String = normalizeFontFamily(preferences.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY))
    fun setFontFamily(value: String?) { preferences.edit().putString(KEY_FONT_FAMILY, normalizeFontFamily(value)).apply() }
    fun getFontFamily(scopeId: String?): String {
        val scoped = preferences.getString(KEY_FONT_FAMILY_PREFIX + normalizeScope(scopeId), null)
        return if (scoped != null) normalizeFontFamily(scoped) else getFontFamily()
    }
    fun setFontFamily(scopeId: String?, value: String?) {
        preferences.edit().putString(KEY_FONT_FAMILY_PREFIX + normalizeScope(scopeId), normalizeFontFamily(value)).apply()
    }
    fun getFontWeight(scopeId: String?): Int {
        val scoped = preferences.getInt(KEY_FONT_WEIGHT_PREFIX + normalizeScope(scopeId), 0)
        return if (scoped > 0) clampWeight(scoped) else if (isBoldText() || usesBoldReferenceWeight(scopeId)) BOLD_WEIGHT else FontCatalog.DEFAULT_WEIGHT
    }
    fun setFontWeight(scopeId: String?, weight: Int) { preferences.edit().putInt(KEY_FONT_WEIGHT_PREFIX + normalizeScope(scopeId), clampWeight(weight)).apply() }
    fun getTimeFontScale(scopeId: String?): Float = clampScale(preferences.getFloat(KEY_TIME_FONT_SCALE_PREFIX + normalizeScope(scopeId), getTimeFontScale()))
    fun setTimeFontScale(scopeId: String?, value: Float) { preferences.edit().putFloat(KEY_TIME_FONT_SCALE_PREFIX + normalizeScope(scopeId), clampScale(value)).apply() }
    fun getDateFontScale(scopeId: String?): Float = clampScale(preferences.getFloat(KEY_DATE_FONT_SCALE_PREFIX + normalizeScope(scopeId), getDateFontScale()))
    fun setDateFontScale(scopeId: String?, value: Float) { preferences.edit().putFloat(KEY_DATE_FONT_SCALE_PREFIX + normalizeScope(scopeId), clampScale(value)).apply() }
    fun getSupportingFontScale(scopeId: String?): Float = normalizeSupportingScale(preferences.getFloat(KEY_SUPPORTING_FONT_SCALE_PREFIX + normalizeScope(scopeId), DEFAULT_SUPPORTING_FONT_SCALE))
    fun setSupportingFontScale(scopeId: String?, value: Float) { preferences.edit().putFloat(KEY_SUPPORTING_FONT_SCALE_PREFIX + normalizeScope(scopeId), normalizeSupportingScale(value)).apply() }

    fun isShowSeconds(): Boolean = preferences.getBoolean(KEY_SHOW_SECONDS, DEFAULT_SHOW_SECONDS)
    fun setShowSeconds(value: Boolean) { preferences.edit().putBoolean(KEY_SHOW_SECONDS, value).apply() }
    fun isShowLunar(): Boolean = preferences.getBoolean(KEY_SHOW_LUNAR, DEFAULT_SHOW_LUNAR)
    fun setShowLunar(value: Boolean) { preferences.edit().putBoolean(KEY_SHOW_LUNAR, value).apply() }
    fun isAutoStart(): Boolean = preferences.getBoolean(KEY_AUTO_START, DEFAULT_AUTO_START)
    fun setAutoStart(value: Boolean) { preferences.edit().putBoolean(KEY_AUTO_START, value).apply() }
    fun getCalendarWeekStart(): Int = normalizeCalendarWeekStart(preferences.getInt(KEY_CALENDAR_WEEK_START, DEFAULT_CALENDAR_WEEK_START))
    fun setCalendarWeekStart(value: Int) { preferences.edit().putInt(KEY_CALENDAR_WEEK_START, normalizeCalendarWeekStart(value)).apply() }
    fun isCalendarHighlightWeekends(): Boolean = preferences.getBoolean(KEY_CALENDAR_HIGHLIGHT_WEEKENDS, DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS)
    fun setCalendarHighlightWeekends(value: Boolean) { preferences.edit().putBoolean(KEY_CALENDAR_HIGHLIGHT_WEEKENDS, value).apply() }
    fun getCalendarTheme(): String = normalizeCalendarTheme(preferences.getString(KEY_CALENDAR_THEME, DEFAULT_CALENDAR_THEME))
    fun setCalendarTheme(value: String?) { preferences.edit().putString(KEY_CALENDAR_THEME, normalizeCalendarTheme(value)).apply() }
    fun isSmallSeconds(): Boolean = preferences.getBoolean(KEY_SMALL_SECONDS, DEFAULT_SMALL_SECONDS)
    fun setSmallSeconds(value: Boolean) { preferences.edit().putBoolean(KEY_SMALL_SECONDS, value).apply() }
    fun isPortraitStacked(): Boolean = preferences.getBoolean(KEY_PORTRAIT_STACKED, DEFAULT_PORTRAIT_STACKED)
    fun setPortraitStacked(value: Boolean) { preferences.edit().putBoolean(KEY_PORTRAIT_STACKED, value).apply() }
    fun isDateLunarDualLine(): Boolean = preferences.getBoolean(KEY_DATE_LUNAR_DUAL_LINE, DEFAULT_DATE_LUNAR_DUAL_LINE)
    fun setDateLunarDualLine(value: Boolean) { preferences.edit().putBoolean(KEY_DATE_LUNAR_DUAL_LINE, value).apply() }
    fun isUse24Hour(): Boolean = preferences.getBoolean(KEY_USE_24_HOUR, DEFAULT_USE_24_HOUR)
    fun setUse24Hour(value: Boolean) { preferences.edit().putBoolean(KEY_USE_24_HOUR, value).apply() }

    fun getClockLanguage(): String {
        val stored = preferences.getString(KEY_CLOCK_LANGUAGE, null)
        return if (stored != null) normalizeClockLanguage(stored) else if (preferences.getBoolean(KEY_CLOCK_USE_ENGLISH, DEFAULT_CLOCK_USE_ENGLISH)) LANGUAGE_ENGLISH else LANGUAGE_SIMPLIFIED
    }
    fun setClockLanguage(value: String?) {
        val normalized = normalizeClockLanguage(value)
        preferences.edit().putString(KEY_CLOCK_LANGUAGE, normalized).putBoolean(KEY_CLOCK_USE_ENGLISH, normalized == LANGUAGE_ENGLISH).apply()
    }
    fun isClockUseEnglish(): Boolean = getClockLanguage() == LANGUAGE_ENGLISH
    fun setClockUseEnglish(value: Boolean) { setClockLanguage(if (value) LANGUAGE_ENGLISH else LANGUAGE_SIMPLIFIED) }
    fun getCustomMessage(): String = normalizeCustomMessage(preferences.getString(KEY_CUSTOM_MESSAGE, DEFAULT_CUSTOM_MESSAGE))
    fun setCustomMessage(value: String?) { preferences.edit().putString(KEY_CUSTOM_MESSAGE, normalizeCustomMessage(value)).apply() }
    fun getDatePatternCn(): String = normalizeDatePattern(preferences.getString(KEY_DATE_PATTERN_CN, DEFAULT_DATE_PATTERN_CN), DEFAULT_DATE_PATTERN_CN)
    fun setDatePatternCn(value: String?) { preferences.edit().putString(KEY_DATE_PATTERN_CN, normalizeDatePattern(value, DEFAULT_DATE_PATTERN_CN)).apply() }
    fun getDatePatternEn(): String = normalizeDatePattern(preferences.getString(KEY_DATE_PATTERN_EN, DEFAULT_DATE_PATTERN_EN), DEFAULT_DATE_PATTERN_EN)
    fun setDatePatternEn(value: String?) { preferences.edit().putString(KEY_DATE_PATTERN_EN, normalizeDatePattern(value, DEFAULT_DATE_PATTERN_EN)).apply() }

    fun getDateCore(english: Boolean): String = preferences.getString(if (english) KEY_DATE_CORE_EN else KEY_DATE_CORE_CN, "") ?: ""
    fun getDateCombo(english: Boolean): String = preferences.getString(if (english) KEY_DATE_COMBO_EN else KEY_DATE_COMBO_CN, "") ?: ""
    fun isDateCustomEnabled(english: Boolean): Boolean = preferences.getBoolean(if (english) KEY_DATE_CUSTOM_ENABLED_EN else KEY_DATE_CUSTOM_ENABLED_CN, false)
    fun getDateCustomText(english: Boolean): String = preferences.getString(if (english) KEY_DATE_CUSTOM_TEXT_EN else KEY_DATE_CUSTOM_TEXT_CN, "") ?: ""
    fun setDateFormatState(english: Boolean, core: String?, combo: String?, customEnabled: Boolean, customText: String?) {
        preferences.edit()
            .putString(if (english) KEY_DATE_CORE_EN else KEY_DATE_CORE_CN, core ?: "")
            .putString(if (english) KEY_DATE_COMBO_EN else KEY_DATE_COMBO_CN, combo ?: "")
            .putBoolean(if (english) KEY_DATE_CUSTOM_ENABLED_EN else KEY_DATE_CUSTOM_ENABLED_CN, customEnabled)
            .putString(if (english) KEY_DATE_CUSTOM_TEXT_EN else KEY_DATE_CUSTOM_TEXT_CN, customText ?: "")
            .apply()
    }

    fun getScreenOrientation(): Int = when {
        preferences.contains(KEY_SCREEN_ORIENTATION) -> preferences.getInt(KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION)
        preferences.getBoolean(KEY_FORCE_LANDSCAPE, false) -> ORIENTATION_LANDSCAPE
        else -> DEFAULT_SCREEN_ORIENTATION
    }
    fun setScreenOrientation(mode: Int) { preferences.edit().putInt(KEY_SCREEN_ORIENTATION, mode).apply() }
    fun isUseNetworkTime(): Boolean = preferences.getBoolean(KEY_USE_NETWORK_TIME, DEFAULT_USE_NETWORK_TIME)
    fun setUseNetworkTime(value: Boolean) { preferences.edit().putBoolean(KEY_USE_NETWORK_TIME, value).apply() }
    fun getSyncIntervalMinutes(): Int = preferences.getInt(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES)
    fun setSyncIntervalMinutes(value: Int) { preferences.edit().putInt(KEY_SYNC_INTERVAL_MINUTES, value).apply() }
    fun getTimeZoneId(): String = preferences.getString(KEY_TIME_ZONE_ID, DEFAULT_TIME_ZONE_ID) ?: DEFAULT_TIME_ZONE_ID
    fun setTimeZoneId(value: String?) { preferences.edit().putString(KEY_TIME_ZONE_ID, value ?: TIME_ZONE_FOLLOW_SYSTEM).apply() }

    fun isWeatherEnabled(): Boolean = preferences.getBoolean(KEY_WEATHER_ENABLED, DEFAULT_WEATHER_ENABLED)
    fun setWeatherEnabled(value: Boolean) { preferences.edit().putBoolean(KEY_WEATHER_ENABLED, value).apply() }
    fun isWeatherDetailed(): Boolean = preferences.getBoolean(KEY_WEATHER_DETAILED, DEFAULT_WEATHER_DETAILED)
    fun setWeatherDetailed(value: Boolean) { preferences.edit().putBoolean(KEY_WEATHER_DETAILED, value).apply() }
    fun isWeatherIconFill(): Boolean = preferences.getBoolean(KEY_WEATHER_ICON_FILL, DEFAULT_WEATHER_ICON_FILL)
    fun setWeatherIconFill(value: Boolean) { preferences.edit().putBoolean(KEY_WEATHER_ICON_FILL, value).apply() }
    fun isWeatherIconDynamicColor(): Boolean = preferences.getBoolean(KEY_WEATHER_ICON_DYNAMIC_COLOR, DEFAULT_WEATHER_ICON_DYNAMIC_COLOR)
    fun setWeatherIconDynamicColor(value: Boolean) { preferences.edit().putBoolean(KEY_WEATHER_ICON_DYNAMIC_COLOR, value).apply() }
    fun getWeatherTemperatureUnit(): String = normalizeWeatherTemperatureUnit(preferences.getString(KEY_WEATHER_TEMPERATURE_UNIT, DEFAULT_WEATHER_TEMPERATURE_UNIT))
    fun setWeatherTemperatureUnit(value: String?) { preferences.edit().putString(KEY_WEATHER_TEMPERATURE_UNIT, normalizeWeatherTemperatureUnit(value)).apply() }
    fun getWeatherIntervalMinutes(): Int { val value = preferences.getInt(KEY_WEATHER_INTERVAL_MINUTES, DEFAULT_WEATHER_INTERVAL_MINUTES); return if (isValidWeatherInterval(value)) value else DEFAULT_WEATHER_INTERVAL_MINUTES }
    fun setWeatherIntervalMinutes(value: Int) { preferences.edit().putInt(KEY_WEATHER_INTERVAL_MINUTES, if (isValidWeatherInterval(value)) value else DEFAULT_WEATHER_INTERVAL_MINUTES).apply() }
    fun getWeatherLocationMode(): String { val mode = preferences.getString(KEY_WEATHER_LOCATION_MODE, DEFAULT_WEATHER_LOCATION_MODE); return if (mode == WEATHER_LOCATION_MANUAL && getWeatherLocationId().isNotEmpty()) WEATHER_LOCATION_MANUAL else WEATHER_LOCATION_AUTOMATIC }
    fun setWeatherLocationMode(value: String?) { preferences.edit().putString(KEY_WEATHER_LOCATION_MODE, if (value == WEATHER_LOCATION_MANUAL) WEATHER_LOCATION_MANUAL else WEATHER_LOCATION_AUTOMATIC).apply() }
    fun getWeatherLocationId(): String = preferences.getString(KEY_WEATHER_LOCATION_ID, "") ?: ""
    fun getWeatherProvince(): String = preferences.getString(KEY_WEATHER_PROVINCE, "") ?: ""
    fun getWeatherCity(): String = preferences.getString(KEY_WEATHER_CITY, "") ?: ""
    fun getWeatherDistrict(): String = preferences.getString(KEY_WEATHER_DISTRICT, "") ?: ""
    fun getWeatherLatitude(): Double = java.lang.Double.longBitsToDouble(preferences.getLong(KEY_WEATHER_LATITUDE, java.lang.Double.doubleToRawLongBits(Double.NaN)))
    fun getWeatherLongitude(): Double = java.lang.Double.longBitsToDouble(preferences.getLong(KEY_WEATHER_LONGITUDE, java.lang.Double.doubleToRawLongBits(Double.NaN)))
    fun setManualWeatherLocation(locationId: String?, province: String?, city: String?, district: String?) = setManualWeatherLocation(locationId, province, city, district, Double.NaN, Double.NaN)
    fun setManualWeatherLocation(locationId: String?, province: String?, city: String?, district: String?, latitude: Double, longitude: Double) {
        preferences.edit().putString(KEY_WEATHER_LOCATION_ID, locationId ?: "").putString(KEY_WEATHER_PROVINCE, province ?: "").putString(KEY_WEATHER_CITY, city ?: "").putString(KEY_WEATHER_DISTRICT, district ?: "").putLong(KEY_WEATHER_LATITUDE, java.lang.Double.doubleToRawLongBits(latitude)).putLong(KEY_WEATHER_LONGITUDE, java.lang.Double.doubleToRawLongBits(longitude)).apply()
    }

    fun restoreDefaults() {
        val editor = preferences.edit()
        clearPerThemeTypography(editor)
        editor.putString(KEY_BACKGROUND_MODE, MODE_COLOR).putInt(KEY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR)
            .putBoolean(KEY_DIM_BACKGROUND, DEFAULT_DIM_BACKGROUND).putBoolean(KEY_SCHEDULE_DIM_BACKGROUND, DEFAULT_SCHEDULE_DIM_BACKGROUND)
            .putInt(KEY_DIM_START_MINUTES, DEFAULT_DIM_START_MINUTES).putInt(KEY_DIM_END_MINUTES, DEFAULT_DIM_END_MINUTES)
            .putFloat(KEY_TIME_FONT_SCALE, DEFAULT_TIME_FONT_SCALE).putFloat(KEY_DATE_FONT_SCALE, DEFAULT_DATE_FONT_SCALE)
            .putInt(KEY_TIME_COLOR, DEFAULT_TEXT_COLOR).putInt(KEY_DATE_COLOR, DEFAULT_TEXT_COLOR)
            .putBoolean(KEY_SHOW_STATUS_ICONS, DEFAULT_SHOW_STATUS_ICONS).putFloat(KEY_STATUS_ICON_SCALE, DEFAULT_STATUS_ICON_SCALE)
            .putBoolean(KEY_BLINK_COLON, DEFAULT_BLINK_COLON).putBoolean(KEY_ANIMATE_TIME_CHANGES, DEFAULT_ANIMATE_TIME_CHANGES)
            .putString(KEY_TIME_TRANSITION, DEFAULT_TIME_TRANSITION).putBoolean(KEY_HOURLY_CHIME, DEFAULT_HOURLY_CHIME)
            .putBoolean(KEY_HALF_HOUR_CHIME, DEFAULT_HALF_HOUR_CHIME).putBoolean(KEY_HOURLY_CHIME_QUIET, DEFAULT_HOURLY_CHIME_QUIET)
            .putInt(KEY_HOURLY_CHIME_QUIET_START, DEFAULT_HOURLY_CHIME_QUIET_START).putInt(KEY_HOURLY_CHIME_QUIET_END, DEFAULT_HOURLY_CHIME_QUIET_END)
            .putBoolean(KEY_BOLD_TEXT, DEFAULT_BOLD_TEXT).putString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY)
            .putBoolean(KEY_SHOW_SECONDS, DEFAULT_SHOW_SECONDS).putBoolean(KEY_SHOW_LUNAR, DEFAULT_SHOW_LUNAR).putBoolean(KEY_AUTO_START, DEFAULT_AUTO_START)
            .putInt(KEY_CALENDAR_WEEK_START, DEFAULT_CALENDAR_WEEK_START).putBoolean(KEY_CALENDAR_HIGHLIGHT_WEEKENDS, DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS)
            .putString(KEY_CALENDAR_THEME, DEFAULT_CALENDAR_THEME).putBoolean(KEY_SMALL_SECONDS, DEFAULT_SMALL_SECONDS)
            .putBoolean(KEY_PORTRAIT_STACKED, DEFAULT_PORTRAIT_STACKED).putBoolean(KEY_DATE_LUNAR_DUAL_LINE, DEFAULT_DATE_LUNAR_DUAL_LINE)
            .putBoolean(KEY_USE_24_HOUR, DEFAULT_USE_24_HOUR).putBoolean(KEY_CLOCK_USE_ENGLISH, DEFAULT_CLOCK_USE_ENGLISH)
            .putString(KEY_CLOCK_LANGUAGE, DEFAULT_CLOCK_LANGUAGE).remove(KEY_CUSTOM_MESSAGE)
            .putString(KEY_DATE_PATTERN_CN, DEFAULT_DATE_PATTERN_CN).putString(KEY_DATE_PATTERN_EN, DEFAULT_DATE_PATTERN_EN)
            .remove(KEY_DATE_CORE_CN).remove(KEY_DATE_CORE_EN).remove(KEY_DATE_COMBO_CN).remove(KEY_DATE_COMBO_EN)
            .remove(KEY_DATE_CUSTOM_ENABLED_CN).remove(KEY_DATE_CUSTOM_ENABLED_EN).remove(KEY_DATE_CUSTOM_TEXT_CN).remove(KEY_DATE_CUSTOM_TEXT_EN)
            .putInt(KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION).remove(KEY_FORCE_LANDSCAPE)
            .putBoolean(KEY_USE_NETWORK_TIME, DEFAULT_USE_NETWORK_TIME)
            .putInt(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES)
            .putString(KEY_TIME_ZONE_ID, DEFAULT_TIME_ZONE_ID)
            .putBoolean(KEY_WEATHER_ENABLED, DEFAULT_WEATHER_ENABLED).putBoolean(KEY_WEATHER_DETAILED, DEFAULT_WEATHER_DETAILED)
            .putBoolean(KEY_WEATHER_ICON_FILL, DEFAULT_WEATHER_ICON_FILL).putBoolean(KEY_WEATHER_ICON_DYNAMIC_COLOR, DEFAULT_WEATHER_ICON_DYNAMIC_COLOR)
            .putString(KEY_WEATHER_TEMPERATURE_UNIT, DEFAULT_WEATHER_TEMPERATURE_UNIT).putInt(KEY_WEATHER_INTERVAL_MINUTES, DEFAULT_WEATHER_INTERVAL_MINUTES)
            .putString(KEY_WEATHER_LOCATION_MODE, DEFAULT_WEATHER_LOCATION_MODE).remove(KEY_WEATHER_LOCATION_ID).remove(KEY_WEATHER_PROVINCE)
            .remove(KEY_WEATHER_CITY).remove(KEY_WEATHER_DISTRICT).remove(KEY_WEATHER_LATITUDE).remove(KEY_WEATHER_LONGITUDE).apply()
    }

    companion object {
        const val MODE_COLOR = "color"; const val MODE_IMAGE = "image"
        const val FONT_SYSTEM = "system"; const val FONT_ROBOTO = "roboto"; const val FONT_GOOGLE_SANS_DISPLAY = "google_sans_display"
        const val FONT_GOOGLE_SANS_TEXT = "google_sans_text"; const val FONT_SF_PRO_DISPLAY = "sf_pro_display"; const val FONT_SF_PRO_ROUNDED = "sf_pro_rounded"
        const val FONT_INTER = "inter"; const val FONT_LATO = "lato"; const val FONT_LORA = "lora"; const val FONT_NOTO_SANS = "noto_sans"; const val FONT_BITCOUNT = "bitcount_grid_double"
        const val TRANSITION_FADE = "fade"; const val TRANSITION_SLIDE_UP = "slide_up"; const val TRANSITION_SLIDE_DOWN = "slide_down"; const val TRANSITION_SCALE = "scale"; const val TRANSITION_FLIP = "flip"
        private const val PREFS_NAME = "clock_prefs"; private const val KEY_BACKGROUND_MODE = "background_mode"; private const val KEY_BACKGROUND_COLOR = "background_color"; private const val KEY_DIM_BACKGROUND = "dim_background"; private const val KEY_SCHEDULE_DIM_BACKGROUND = "schedule_dim_background"; private const val KEY_DIM_START_MINUTES = "dim_start_minutes"; private const val KEY_DIM_END_MINUTES = "dim_end_minutes"; private const val KEY_TIME_FONT_SCALE = "time_font_scale"; private const val KEY_DATE_FONT_SCALE = "date_font_scale"; private const val KEY_TIME_COLOR = "time_color"; private const val KEY_DATE_COLOR = "date_color"; private const val KEY_SHOW_STATUS_ICONS = "show_status_icons"; private const val KEY_STATUS_ICON_SCALE = "status_icon_scale"; private const val KEY_BLINK_COLON = "blink_colon"; private const val KEY_ANIMATE_TIME_CHANGES = "animate_time_changes"; private const val KEY_TIME_TRANSITION = "time_transition"; private const val KEY_HOURLY_CHIME = "hourly_visual_chime"; private const val KEY_HALF_HOUR_CHIME = "half_hour_visual_chime"; private const val KEY_HOURLY_CHIME_QUIET = "hourly_chime_quiet"; private const val KEY_HOURLY_CHIME_QUIET_START = "hourly_chime_quiet_start"; private const val KEY_HOURLY_CHIME_QUIET_END = "hourly_chime_quiet_end"; private const val KEY_BOLD_TEXT = "bold_text"; private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_FAMILY_PREFIX = "font_family__"; private const val KEY_FONT_WEIGHT_PREFIX = "font_weight__"; private const val KEY_TIME_FONT_SCALE_PREFIX = "time_font_scale__"; private const val KEY_DATE_FONT_SCALE_PREFIX = "date_font_scale__"; private const val KEY_SUPPORTING_FONT_SCALE_PREFIX = "supporting_font_scale__"; private const val CALENDAR_SCOPE_PREFIX = "calendar:"; const val BOLD_WEIGHT = 700; private const val LEGACY_FONT_GOOGLE_SANS = "google_sans"; private const val KEY_SHOW_SECONDS = "show_seconds"; private const val KEY_SHOW_LUNAR = "show_lunar"; private const val KEY_AUTO_START = "auto_start"; private const val KEY_CALENDAR_WEEK_START = "calendar_week_start"; private const val KEY_CALENDAR_HIGHLIGHT_WEEKENDS = "calendar_highlight_weekends"; private const val KEY_CALENDAR_THEME = "calendar_theme"; private const val KEY_SMALL_SECONDS = "small_seconds"; private const val KEY_PORTRAIT_STACKED = "portrait_stacked"; private const val KEY_DATE_LUNAR_DUAL_LINE = "date_lunar_dual_line"; private const val KEY_USE_24_HOUR = "use_24_hour"; private const val KEY_CLOCK_USE_ENGLISH = "clock_use_english"; private const val KEY_CLOCK_LANGUAGE = "clock_language"; private const val KEY_CUSTOM_MESSAGE = "custom_message"; private const val KEY_DATE_PATTERN_CN = "date_pattern_cn"; private const val KEY_DATE_PATTERN_EN = "date_pattern_en"; private const val KEY_DATE_CORE_CN = "date_core_cn"; private const val KEY_DATE_CORE_EN = "date_core_en"; private const val KEY_DATE_COMBO_CN = "date_combo_cn"; private const val KEY_DATE_COMBO_EN = "date_combo_en"; private const val KEY_DATE_CUSTOM_ENABLED_CN = "date_custom_enabled_cn"; private const val KEY_DATE_CUSTOM_ENABLED_EN = "date_custom_enabled_en"; private const val KEY_DATE_CUSTOM_TEXT_CN = "date_custom_text_cn"; private const val KEY_DATE_CUSTOM_TEXT_EN = "date_custom_text_en"; private const val KEY_FORCE_LANDSCAPE = "force_landscape"; private const val KEY_SCREEN_ORIENTATION = "screen_orientation"; private const val KEY_USE_NETWORK_TIME = "use_network_time"; private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"; private const val KEY_TIME_ZONE_ID = "time_zone_id"; private const val KEY_WEATHER_ENABLED = "weather_enabled"; private const val KEY_WEATHER_DETAILED = "weather_detailed"; private const val KEY_WEATHER_INTERVAL_MINUTES = "weather_interval_minutes"; private const val KEY_WEATHER_LOCATION_MODE = "weather_location_mode"; private const val KEY_WEATHER_LOCATION_ID = "weather_location_id"; private const val KEY_WEATHER_PROVINCE = "weather_province"; private const val KEY_WEATHER_CITY = "weather_city"; private const val KEY_WEATHER_DISTRICT = "weather_district"; private const val KEY_WEATHER_LATITUDE = "weather_latitude"; private const val KEY_WEATHER_LONGITUDE = "weather_longitude"; private const val KEY_WEATHER_ICON_FILL = "weather_icon_fill"; private const val KEY_WEATHER_ICON_DYNAMIC_COLOR = "weather_icon_dynamic_color"; private const val KEY_WEATHER_TEMPERATURE_UNIT = "weather_temperature_unit"
        const val TIME_ZONE_FOLLOW_SYSTEM = ""; const val DEFAULT_TIME_FONT_SCALE = 0.88f; const val DEFAULT_DATE_FONT_SCALE = 0.55f; const val DEFAULT_SUPPORTING_FONT_SCALE = 1.0f; const val MIN_FONT_SCALE = 0.20f; const val MAX_FONT_SCALE = 1.50f; const val MIN_SUPPORTING_FONT_SCALE = MIN_FONT_SCALE; const val MAX_SUPPORTING_FONT_SCALE = MAX_FONT_SCALE; const val DEFAULT_TEXT_COLOR = -1; const val DEFAULT_DIM_BACKGROUND = false; const val DEFAULT_SCHEDULE_DIM_BACKGROUND = false; const val DEFAULT_DIM_START_MINUTES = 22 * 60; const val DEFAULT_DIM_END_MINUTES = 6 * 60; const val DEFAULT_SHOW_STATUS_ICONS = false; const val DEFAULT_STATUS_ICON_SCALE = 1.0f; const val MIN_STATUS_ICON_SCALE = 0.60f; const val MAX_STATUS_ICON_SCALE = 1.25f; const val DEFAULT_BLINK_COLON = false; const val DEFAULT_ANIMATE_TIME_CHANGES = true; const val DEFAULT_TIME_TRANSITION = TRANSITION_FADE; const val DEFAULT_HOURLY_CHIME = true; const val DEFAULT_HALF_HOUR_CHIME = false; const val DEFAULT_HOURLY_CHIME_QUIET = true; const val DEFAULT_HOURLY_CHIME_QUIET_START = 22 * 60; const val DEFAULT_HOURLY_CHIME_QUIET_END = 7 * 60; const val DEFAULT_BOLD_TEXT = false; const val DEFAULT_FONT_FAMILY = FONT_SYSTEM; const val DEFAULT_SHOW_SECONDS = true; const val DEFAULT_SHOW_LUNAR = true; const val DEFAULT_AUTO_START = false; const val CALENDAR_WEEK_START_SUNDAY = Calendar.SUNDAY; const val CALENDAR_WEEK_START_MONDAY = Calendar.MONDAY; const val DEFAULT_CALENDAR_WEEK_START = CALENDAR_WEEK_START_SUNDAY; const val DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS = false; const val DEFAULT_CALENDAR_THEME = "calendar.graphite"; const val DEFAULT_SMALL_SECONDS = false; const val DEFAULT_PORTRAIT_STACKED = false; const val DEFAULT_DATE_LUNAR_DUAL_LINE = false; const val DEFAULT_USE_24_HOUR = true; const val DEFAULT_CLOCK_USE_ENGLISH = false; const val LANGUAGE_SIMPLIFIED = "zh-Hans"; const val LANGUAGE_TRADITIONAL = "zh-Hant"; const val LANGUAGE_ENGLISH = "en"; const val DEFAULT_CLOCK_LANGUAGE = LANGUAGE_SIMPLIFIED; const val DEFAULT_CUSTOM_MESSAGE = ""; const val MAX_CUSTOM_MESSAGE_LENGTH = 200; const val DEFAULT_DATE_PATTERN_CN = DateFormatter.DEFAULT_PATTERN_CN; const val DEFAULT_DATE_PATTERN_EN = DateFormatter.DEFAULT_PATTERN_EN; const val ORIENTATION_FOLLOW_SYSTEM = 0; const val ORIENTATION_PORTRAIT = 1; const val ORIENTATION_LANDSCAPE = 2; const val DEFAULT_SCREEN_ORIENTATION = ORIENTATION_FOLLOW_SYSTEM; const val DEFAULT_USE_NETWORK_TIME = false; const val DEFAULT_SYNC_INTERVAL_MINUTES = 60; const val DEFAULT_TIME_ZONE_ID = TIME_ZONE_FOLLOW_SYSTEM; const val DEFAULT_WEATHER_ENABLED = false; const val DEFAULT_WEATHER_DETAILED = false; const val DEFAULT_WEATHER_INTERVAL_MINUTES = 30; const val DEFAULT_WEATHER_ICON_FILL = true; const val DEFAULT_WEATHER_ICON_DYNAMIC_COLOR = false; const val WEATHER_UNIT_CELSIUS = WeatherTemperatureFormatter.UNIT_CELSIUS; const val WEATHER_UNIT_FAHRENHEIT = WeatherTemperatureFormatter.UNIT_FAHRENHEIT; const val DEFAULT_WEATHER_TEMPERATURE_UNIT = WEATHER_UNIT_CELSIUS; const val WEATHER_LOCATION_AUTOMATIC = "automatic"; const val WEATHER_LOCATION_MANUAL = "manual"; const val DEFAULT_WEATHER_LOCATION_MODE = WEATHER_LOCATION_AUTOMATIC; const val DEFAULT_BACKGROUND_COLOR = -16777216

        @JvmStatic fun normalizeStatusIconScale(value: Float): Float = if (value.isNaN()) DEFAULT_STATUS_ICON_SCALE else value.coerceIn(MIN_STATUS_ICON_SCALE, MAX_STATUS_ICON_SCALE)
        @JvmStatic fun normalizeTimeTransition(value: String?): String = if (value == TRANSITION_SLIDE_UP || value == TRANSITION_SLIDE_DOWN || value == TRANSITION_SCALE || value == TRANSITION_FLIP) value else TRANSITION_FADE
        @JvmStatic fun calendarScope(themeId: String?): String = CALENDAR_SCOPE_PREFIX + (themeId?.trim() ?: "")
        @JvmStatic fun normalizeSupportingScale(value: Float): Float = if (value.isNaN() || value.isInfinite()) DEFAULT_SUPPORTING_FONT_SCALE else value.coerceIn(MIN_SUPPORTING_FONT_SCALE, MAX_SUPPORTING_FONT_SCALE)
        @JvmStatic fun normalizeFontFamily(value: String?): String = when { value == LEGACY_FONT_GOOGLE_SANS -> FONT_GOOGLE_SANS_DISPLAY; value != null && FontCatalog.isAvailable(value) -> value; else -> FONT_SYSTEM }
        @JvmStatic fun normalizeCustomMessage(value: String?): String = (value?.trim()?.take(MAX_CUSTOM_MESSAGE_LENGTH)) ?: DEFAULT_CUSTOM_MESSAGE
        @JvmStatic fun normalizeDatePattern(value: String?, fallback: String): String = if (DateFormatter.isValidPattern(value)) value!! else fallback
        @JvmStatic fun normalizeWeatherTemperatureUnit(value: String?): String = WeatherTemperatureFormatter.normalizeUnit(value)
        @JvmStatic fun isValidWeatherInterval(value: Int): Boolean = value == 10 || value == 30 || value == 60 || value == 180 || value == 360 || value == 720
        @JvmStatic fun toActivityInfoOrientation(mode: Int): Int = when (mode) { ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT; ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        private fun normalizeClockLanguage(value: String?): String = when (value) { LANGUAGE_ENGLISH -> LANGUAGE_ENGLISH; LANGUAGE_TRADITIONAL -> LANGUAGE_TRADITIONAL; else -> LANGUAGE_SIMPLIFIED }
        private fun normalizeCalendarWeekStart(value: Int): Int = if (value == CALENDAR_WEEK_START_MONDAY) CALENDAR_WEEK_START_MONDAY else CALENDAR_WEEK_START_SUNDAY
        private fun normalizeCalendarTheme(value: String?): String { val trimmed = value?.trim() ?: return DEFAULT_CALENDAR_THEME; return if (trimmed.isEmpty() || trimmed.length > 120) DEFAULT_CALENDAR_THEME else trimmed }
        private fun normalizeScope(value: String?): String = value?.trim()?.take(160) ?: ""
        private fun clampScale(value: Float): Float = if (value.isNaN() || value.isInfinite()) DEFAULT_TIME_FONT_SCALE else value.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        private fun clampWeight(value: Int): Int = value.coerceIn(FontCatalog.WEIGHT_STOPS.first(), FontCatalog.WEIGHT_STOPS.last())
        private fun usesBoldReferenceWeight(scopeId: String?): Boolean = scopeId == "ultimate.dual_blocks" || scopeId == "ultimate.orbit" || scopeId == "ultimate.bubbles" || scopeId == "ultimate.blend" || scopeId == "ultimate.ribbon"
    }

    private fun clearPerThemeTypography(editor: SharedPreferences.Editor) {
        preferences.all.keys.filter { it.startsWith(KEY_FONT_FAMILY_PREFIX) || it.startsWith(KEY_FONT_WEIGHT_PREFIX) || it.startsWith(KEY_TIME_FONT_SCALE_PREFIX) || it.startsWith(KEY_DATE_FONT_SCALE_PREFIX) || it.startsWith(KEY_SUPPORTING_FONT_SCALE_PREFIX) }.forEach(editor::remove)
    }
}
