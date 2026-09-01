package com.clockmods.background;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import com.clockmods.ui.DateFormatter;
import com.clockmods.weather.WeatherTemperatureFormatter;

import java.util.Calendar;

public class ClockPreferences {
    public static final String MODE_COLOR = "color";
    public static final String MODE_IMAGE = "image";
    public static final String FONT_SYSTEM = "system";
    public static final String FONT_ROBOTO = "roboto";
    public static final String FONT_GOOGLE_SANS_DISPLAY = "google_sans_display";
    // Additional font families bundled with ClockMods Ultimate.
    public static final String FONT_GOOGLE_SANS_TEXT = "google_sans_text";
    public static final String FONT_SF_PRO_DISPLAY = "sf_pro_display";
    public static final String FONT_SF_PRO_ROUNDED = "sf_pro_rounded";
    public static final String FONT_INTER = "inter";
    public static final String FONT_LATO = "lato";
    public static final String FONT_LORA = "lora";
    public static final String FONT_NOTO_SANS = "noto_sans";
    public static final String FONT_BITCOUNT = "bitcount_grid_double";
    public static final String TRANSITION_FADE = "fade";
    public static final String TRANSITION_SLIDE_UP = "slide_up";
    public static final String TRANSITION_SLIDE_DOWN = "slide_down";
    public static final String TRANSITION_SCALE = "scale";
    public static final String TRANSITION_FLIP = "flip";

    private static final String PREFS_NAME = "clock_prefs";
    private static final String KEY_BACKGROUND_MODE = "background_mode";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_DIM_BACKGROUND = "dim_background";
    private static final String KEY_SCHEDULE_DIM_BACKGROUND = "schedule_dim_background";
    private static final String KEY_DIM_START_MINUTES = "dim_start_minutes";
    private static final String KEY_DIM_END_MINUTES = "dim_end_minutes";
    private static final String KEY_TIME_FONT_SCALE = "time_font_scale";
    private static final String KEY_DATE_FONT_SCALE = "date_font_scale";
    private static final String KEY_TIME_COLOR = "time_color";
    private static final String KEY_DATE_COLOR = "date_color";
    private static final String KEY_SHOW_STATUS_ICONS = "show_status_icons";
    private static final String KEY_STATUS_ICON_SCALE = "status_icon_scale";
    private static final String KEY_BLINK_COLON = "blink_colon";
    private static final String KEY_ANIMATE_TIME_CHANGES = "animate_time_changes";
    private static final String KEY_TIME_TRANSITION = "time_transition";
    private static final String KEY_HOURLY_CHIME = "hourly_visual_chime";
    private static final String KEY_HALF_HOUR_CHIME = "half_hour_visual_chime";
    private static final String KEY_HOURLY_CHIME_QUIET = "hourly_chime_quiet";
    private static final String KEY_HOURLY_CHIME_QUIET_START = "hourly_chime_quiet_start";
    private static final String KEY_HOURLY_CHIME_QUIET_END = "hourly_chime_quiet_end";
    private static final String KEY_BOLD_TEXT = "bold_text";
    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String LEGACY_FONT_GOOGLE_SANS = "google_sans";
    private static final String KEY_SHOW_SECONDS = "show_seconds";
    private static final String KEY_SHOW_LUNAR = "show_lunar";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_CALENDAR_WEEK_START = "calendar_week_start";
    private static final String KEY_CALENDAR_HIGHLIGHT_WEEKENDS = "calendar_highlight_weekends";
    private static final String KEY_CALENDAR_THEME = "calendar_theme";
    private static final String KEY_SMALL_SECONDS = "small_seconds";
    private static final String KEY_PORTRAIT_STACKED = "portrait_stacked";
    private static final String KEY_DATE_LUNAR_DUAL_LINE = "date_lunar_dual_line";
    private static final String KEY_USE_24_HOUR = "use_24_hour";
    private static final String KEY_CLOCK_USE_ENGLISH = "clock_use_english";
    // Tri-state interface language ("zh-Hans"/"zh-Hant"/"en"); supersedes the legacy boolean above.
    private static final String KEY_CLOCK_LANGUAGE = "clock_language";
    private static final String KEY_CUSTOM_MESSAGE = "custom_message";
    // Full patterns actually used for rendering the main clock / calendar date lines.
    private static final String KEY_DATE_PATTERN_CN = "date_pattern_cn";
    private static final String KEY_DATE_PATTERN_EN = "date_pattern_en";
    // Pro settings UI restoration state (which dropdown / combo / custom entry was chosen).
    private static final String KEY_DATE_CORE_CN = "date_core_cn";
    private static final String KEY_DATE_CORE_EN = "date_core_en";
    private static final String KEY_DATE_COMBO_CN = "date_combo_cn";
    private static final String KEY_DATE_COMBO_EN = "date_combo_en";
    private static final String KEY_DATE_CUSTOM_ENABLED_CN = "date_custom_enabled_cn";
    private static final String KEY_DATE_CUSTOM_ENABLED_EN = "date_custom_enabled_en";
    private static final String KEY_DATE_CUSTOM_TEXT_CN = "date_custom_text_cn";
    private static final String KEY_DATE_CUSTOM_TEXT_EN = "date_custom_text_en";
    private static final String KEY_FORCE_LANDSCAPE = "force_landscape";
    private static final String KEY_SCREEN_ORIENTATION = "screen_orientation";
    private static final String KEY_USE_NETWORK_TIME = "use_network_time";
    private static final String KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes";
    private static final String KEY_TIME_ZONE_ID = "time_zone_id";
    private static final String KEY_WEATHER_ENABLED = "weather_enabled";
    private static final String KEY_WEATHER_DETAILED = "weather_detailed";
    private static final String KEY_WEATHER_INTERVAL_MINUTES = "weather_interval_minutes";
    private static final String KEY_WEATHER_LOCATION_MODE = "weather_location_mode";
    private static final String KEY_WEATHER_LOCATION_ID = "weather_location_id";
    private static final String KEY_WEATHER_PROVINCE = "weather_province";
    private static final String KEY_WEATHER_CITY = "weather_city";
    private static final String KEY_WEATHER_DISTRICT = "weather_district";
    private static final String KEY_WEATHER_LATITUDE = "weather_latitude";
    private static final String KEY_WEATHER_LONGITUDE = "weather_longitude";
    private static final String KEY_WEATHER_ICON_FILL = "weather_icon_fill";
    private static final String KEY_WEATHER_ICON_DYNAMIC_COLOR = "weather_icon_dynamic_color";
    private static final String KEY_WEATHER_TEMPERATURE_UNIT = "weather_temperature_unit";

    /** Sentinel value meaning "follow the device's own time zone". */
    public static final String TIME_ZONE_FOLLOW_SYSTEM = "";

    /** Fraction of the screen width occupied by the time text by default. */
    public static final float DEFAULT_TIME_FONT_SCALE = 0.88f;
    /** Fraction of the screen width occupied by the date text by default. */
    public static final float DEFAULT_DATE_FONT_SCALE = 0.55f;
    public static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    public static final boolean DEFAULT_DIM_BACKGROUND = false;
    public static final boolean DEFAULT_SCHEDULE_DIM_BACKGROUND = false;
    public static final int DEFAULT_DIM_START_MINUTES = 22 * 60;
    public static final int DEFAULT_DIM_END_MINUTES = 6 * 60;
    /** Status icons (network + battery) are hidden by default. */
    public static final boolean DEFAULT_SHOW_STATUS_ICONS = false;
    public static final float DEFAULT_STATUS_ICON_SCALE = 1.0f;
    public static final float MIN_STATUS_ICON_SCALE = 0.60f;
    public static final float MAX_STATUS_ICON_SCALE = 1.25f;
    public static final boolean DEFAULT_BLINK_COLON = false;
    public static final boolean DEFAULT_ANIMATE_TIME_CHANGES = true;
    public static final String DEFAULT_TIME_TRANSITION = TRANSITION_FADE;
    public static final boolean DEFAULT_HOURLY_CHIME = true;
    public static final boolean DEFAULT_HALF_HOUR_CHIME = false;
    public static final boolean DEFAULT_HOURLY_CHIME_QUIET = true;
    public static final int DEFAULT_HOURLY_CHIME_QUIET_START = 22 * 60;
    public static final int DEFAULT_HOURLY_CHIME_QUIET_END = 7 * 60;
    public static final boolean DEFAULT_BOLD_TEXT = false;
    public static final String DEFAULT_FONT_FAMILY = FONT_SYSTEM;
    public static final boolean DEFAULT_SHOW_SECONDS = true;
    public static final boolean DEFAULT_SHOW_LUNAR = true;
    public static final boolean DEFAULT_AUTO_START = false;
    public static final int CALENDAR_WEEK_START_SUNDAY = Calendar.SUNDAY;
    public static final int CALENDAR_WEEK_START_MONDAY = Calendar.MONDAY;
    public static final int DEFAULT_CALENDAR_WEEK_START = CALENDAR_WEEK_START_SUNDAY;
    public static final boolean DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS = false;
    /** Mirrors {@code CalendarTheme.ID_GRAPHITE}; this module cannot see the ultimate flavour. */
    public static final String DEFAULT_CALENDAR_THEME = "calendar.graphite";
    public static final boolean DEFAULT_SMALL_SECONDS = false;
    public static final boolean DEFAULT_PORTRAIT_STACKED = false;
    /** Landscape only: whether the date and lunar lines stack on two rows (portrait always stacks). */
    public static final boolean DEFAULT_DATE_LUNAR_DUAL_LINE = false;
    public static final boolean DEFAULT_USE_24_HOUR = true;
    public static final boolean DEFAULT_CLOCK_USE_ENGLISH = false;
    /** Interface-language values stored by {@link #getClockLanguage()}. */
    public static final String LANGUAGE_SIMPLIFIED = "zh-Hans";
    public static final String LANGUAGE_TRADITIONAL = "zh-Hant";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String DEFAULT_CLOCK_LANGUAGE = LANGUAGE_SIMPLIFIED;
    /** Optional free-text message shown on the weather line; empty means "no message". */
    public static final String DEFAULT_CUSTOM_MESSAGE = "";
    /** Upper bound on the custom message length, guarding layout and storage. */
    public static final int MAX_CUSTOM_MESSAGE_LENGTH = 200;
    public static final String DEFAULT_DATE_PATTERN_CN = DateFormatter.DEFAULT_PATTERN_CN;
    public static final String DEFAULT_DATE_PATTERN_EN = DateFormatter.DEFAULT_PATTERN_EN;
    /** Screen orientation modes returned by {@link #getScreenOrientation()}. */
    public static final int ORIENTATION_FOLLOW_SYSTEM = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int DEFAULT_SCREEN_ORIENTATION = ORIENTATION_FOLLOW_SYSTEM;
    /** Network time is disabled by default; the device local time is used. */
    public static final boolean DEFAULT_USE_NETWORK_TIME = false;
    /** Default interval, in minutes, between network time synchronizations. */
    public static final int DEFAULT_SYNC_INTERVAL_MINUTES = 60;
    /** Time zone is set to follow the system by default. */
    public static final String DEFAULT_TIME_ZONE_ID = TIME_ZONE_FOLLOW_SYSTEM;
    public static final boolean DEFAULT_WEATHER_ENABLED = false;
    public static final boolean DEFAULT_WEATHER_DETAILED = false;
    public static final int DEFAULT_WEATHER_INTERVAL_MINUTES = 30;
    /** Weather icons default to the solid fill style; disable for the outline (line) style. */
    public static final boolean DEFAULT_WEATHER_ICON_FILL = true;
    /** Weather icons default to white; enable to tint them with the Material 3 accent colour. */
    public static final boolean DEFAULT_WEATHER_ICON_DYNAMIC_COLOR = false;
    /** Weather values are fetched/cached in Celsius and converted only for display. */
    public static final String WEATHER_UNIT_CELSIUS = WeatherTemperatureFormatter.UNIT_CELSIUS;
    public static final String WEATHER_UNIT_FAHRENHEIT = WeatherTemperatureFormatter.UNIT_FAHRENHEIT;
    public static final String DEFAULT_WEATHER_TEMPERATURE_UNIT = WEATHER_UNIT_CELSIUS;
    public static final String WEATHER_LOCATION_AUTOMATIC = "automatic";
    public static final String WEATHER_LOCATION_MANUAL = "manual";
    public static final String DEFAULT_WEATHER_LOCATION_MODE = WEATHER_LOCATION_AUTOMATIC;

    /** Allowed range for the width-based font scale (fraction of screen width). */
    public static final float MIN_FONT_SCALE = 0.20f;
    public static final float MAX_FONT_SCALE = 1.50f;

    /** Default background color: solid black. */
    public static final int DEFAULT_BACKGROUND_COLOR = 0xFF000000;

    private final SharedPreferences preferences;

    public ClockPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getBackgroundMode() {
        return preferences.getString(KEY_BACKGROUND_MODE, MODE_COLOR);
    }

    public void setBackgroundMode(String mode) {
        preferences.edit().putString(KEY_BACKGROUND_MODE, mode).apply();
    }

    public int getBackgroundColor(int defaultColor) {
        return preferences.getInt(KEY_BACKGROUND_COLOR, defaultColor);
    }

    public void setBackgroundColor(int color) {
        preferences.edit().putInt(KEY_BACKGROUND_COLOR, color).apply();
    }

    public boolean isDimBackground() {
        return preferences.getBoolean(KEY_DIM_BACKGROUND, DEFAULT_DIM_BACKGROUND);
    }

    public void setDimBackground(boolean dimBackground) {
        preferences.edit().putBoolean(KEY_DIM_BACKGROUND, dimBackground).apply();
    }

    public boolean isScheduleDimBackground() {
        return preferences.getBoolean(KEY_SCHEDULE_DIM_BACKGROUND, DEFAULT_SCHEDULE_DIM_BACKGROUND);
    }

    public void setScheduleDimBackground(boolean schedule) {
        preferences.edit().putBoolean(KEY_SCHEDULE_DIM_BACKGROUND, schedule).apply();
    }

    public int getDimStartMinutes() {
        return preferences.getInt(KEY_DIM_START_MINUTES, DEFAULT_DIM_START_MINUTES);
    }

    public void setDimStartMinutes(int minutes) {
        preferences.edit().putInt(KEY_DIM_START_MINUTES, minutes).apply();
    }

    public int getDimEndMinutes() {
        return preferences.getInt(KEY_DIM_END_MINUTES, DEFAULT_DIM_END_MINUTES);
    }

    public void setDimEndMinutes(int minutes) {
        preferences.edit().putInt(KEY_DIM_END_MINUTES, minutes).apply();
    }

    public float getTimeFontScale() {
        return clampScale(preferences.getFloat(KEY_TIME_FONT_SCALE, DEFAULT_TIME_FONT_SCALE));
    }

    public void setTimeFontScale(float scale) {
        preferences.edit().putFloat(KEY_TIME_FONT_SCALE, clampScale(scale)).apply();
    }

    public float getDateFontScale() {
        return clampScale(preferences.getFloat(KEY_DATE_FONT_SCALE, DEFAULT_DATE_FONT_SCALE));
    }

    public void setDateFontScale(float scale) {
        preferences.edit().putFloat(KEY_DATE_FONT_SCALE, clampScale(scale)).apply();
    }

    public int getTimeColor() {
        return preferences.getInt(KEY_TIME_COLOR, DEFAULT_TEXT_COLOR);
    }

    public void setTimeColor(int color) {
        preferences.edit().putInt(KEY_TIME_COLOR, color).apply();
    }

    public int getDateColor() {
        return preferences.getInt(KEY_DATE_COLOR, DEFAULT_TEXT_COLOR);
    }

    public void setDateColor(int color) {
        preferences.edit().putInt(KEY_DATE_COLOR, color).apply();
    }

    public boolean isShowStatusIcons() {
        return preferences.getBoolean(KEY_SHOW_STATUS_ICONS, DEFAULT_SHOW_STATUS_ICONS);
    }

    public void setShowStatusIcons(boolean show) {
        preferences.edit().putBoolean(KEY_SHOW_STATUS_ICONS, show).apply();
    }

    public float getStatusIconScale() {
        return normalizeStatusIconScale(
                preferences.getFloat(KEY_STATUS_ICON_SCALE, DEFAULT_STATUS_ICON_SCALE));
    }

    public void setStatusIconScale(float scale) {
        preferences.edit().putFloat(
                KEY_STATUS_ICON_SCALE, normalizeStatusIconScale(scale)).apply();
    }

    public static float normalizeStatusIconScale(float scale) {
        if (Float.isNaN(scale)) return DEFAULT_STATUS_ICON_SCALE;
        return Math.max(MIN_STATUS_ICON_SCALE, Math.min(MAX_STATUS_ICON_SCALE, scale));
    }

    public boolean isBlinkColon() {
        return preferences.getBoolean(KEY_BLINK_COLON, DEFAULT_BLINK_COLON);
    }

    public void setBlinkColon(boolean blinkColon) {
        preferences.edit().putBoolean(KEY_BLINK_COLON, blinkColon).apply();
    }

    public boolean isAnimateTimeChanges() {
        return preferences.getBoolean(KEY_ANIMATE_TIME_CHANGES, DEFAULT_ANIMATE_TIME_CHANGES);
    }

    public void setAnimateTimeChanges(boolean animate) {
        preferences.edit().putBoolean(KEY_ANIMATE_TIME_CHANGES, animate).apply();
    }

    public String getTimeTransition() {
        return normalizeTimeTransition(preferences.getString(
                KEY_TIME_TRANSITION, DEFAULT_TIME_TRANSITION));
    }

    public void setTimeTransition(String transition) {
        preferences.edit().putString(KEY_TIME_TRANSITION,
                normalizeTimeTransition(transition)).apply();
    }

    public boolean isHourlyChimeEnabled() {
        return preferences.getBoolean(KEY_HOURLY_CHIME, DEFAULT_HOURLY_CHIME);
    }

    public void setHourlyChimeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_HOURLY_CHIME, enabled).apply();
    }

    public boolean isHalfHourChimeEnabled() {
        return preferences.getBoolean(KEY_HALF_HOUR_CHIME, DEFAULT_HALF_HOUR_CHIME);
    }

    public void setHalfHourChimeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_HALF_HOUR_CHIME, enabled).apply();
    }

    public boolean isHourlyChimeQuietEnabled() {
        return preferences.getBoolean(KEY_HOURLY_CHIME_QUIET, DEFAULT_HOURLY_CHIME_QUIET);
    }

    public void setHourlyChimeQuietEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_HOURLY_CHIME_QUIET, enabled).apply();
    }

    public int getHourlyChimeQuietStart() {
        return preferences.getInt(KEY_HOURLY_CHIME_QUIET_START,
                DEFAULT_HOURLY_CHIME_QUIET_START);
    }

    public void setHourlyChimeQuietStart(int minutes) {
        preferences.edit().putInt(KEY_HOURLY_CHIME_QUIET_START, minutes).apply();
    }

    public int getHourlyChimeQuietEnd() {
        return preferences.getInt(KEY_HOURLY_CHIME_QUIET_END,
                DEFAULT_HOURLY_CHIME_QUIET_END);
    }

    public void setHourlyChimeQuietEnd(int minutes) {
        preferences.edit().putInt(KEY_HOURLY_CHIME_QUIET_END, minutes).apply();
    }

    public static String normalizeTimeTransition(String transition) {
        if (TRANSITION_SLIDE_UP.equals(transition) || TRANSITION_SLIDE_DOWN.equals(transition)
                || TRANSITION_SCALE.equals(transition) || TRANSITION_FLIP.equals(transition)) {
            return transition;
        }
        return TRANSITION_FADE;
    }

    public boolean isBoldText() {
        return preferences.getBoolean(KEY_BOLD_TEXT, DEFAULT_BOLD_TEXT);
    }

    public void setBoldText(boolean boldText) {
        preferences.edit().putBoolean(KEY_BOLD_TEXT, boldText).apply();
    }

    public String getFontFamily() {
        return normalizeFontFamily(preferences.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY));
    }

    public void setFontFamily(String fontFamily) {
        preferences.edit().putString(KEY_FONT_FAMILY, normalizeFontFamily(fontFamily)).apply();
    }

    public static String normalizeFontFamily(String fontFamily) {
        if (LEGACY_FONT_GOOGLE_SANS.equals(fontFamily)) {
            return FONT_GOOGLE_SANS_DISPLAY;
        }
        if (fontFamily != null && FontCatalog.isAvailable(fontFamily)) {
            return fontFamily;
        }
        return FONT_SYSTEM;
    }

    public boolean isShowSeconds() {
        return preferences.getBoolean(KEY_SHOW_SECONDS, DEFAULT_SHOW_SECONDS);
    }

    public void setShowSeconds(boolean showSeconds) {
        preferences.edit().putBoolean(KEY_SHOW_SECONDS, showSeconds).apply();
    }

    public boolean isShowLunar() {
        return preferences.getBoolean(KEY_SHOW_LUNAR, DEFAULT_SHOW_LUNAR);
    }

    public void setShowLunar(boolean showLunar) {
        preferences.edit().putBoolean(KEY_SHOW_LUNAR, showLunar).apply();
    }

    public boolean isAutoStart() {
        return preferences.getBoolean(KEY_AUTO_START, DEFAULT_AUTO_START);
    }

    public void setAutoStart(boolean autoStart) {
        preferences.edit().putBoolean(KEY_AUTO_START, autoStart).apply();
    }

    public int getCalendarWeekStart() {
        return normalizeCalendarWeekStart(preferences.getInt(
                KEY_CALENDAR_WEEK_START, DEFAULT_CALENDAR_WEEK_START));
    }

    public void setCalendarWeekStart(int firstDayOfWeek) {
        preferences.edit().putInt(KEY_CALENDAR_WEEK_START,
                normalizeCalendarWeekStart(firstDayOfWeek)).apply();
    }

    public boolean isCalendarHighlightWeekends() {
        return preferences.getBoolean(KEY_CALENDAR_HIGHLIGHT_WEEKENDS,
                DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS);
    }

    public void setCalendarHighlightWeekends(boolean highlightWeekends) {
        preferences.edit().putBoolean(KEY_CALENDAR_HIGHLIGHT_WEEKENDS,
                highlightWeekends).apply();
    }

    private static int normalizeCalendarWeekStart(int firstDayOfWeek) {
        return firstDayOfWeek == CALENDAR_WEEK_START_MONDAY
                ? CALENDAR_WEEK_START_MONDAY : CALENDAR_WEEK_START_SUNDAY;
    }

    public String getCalendarTheme() {
        return normalizeCalendarTheme(preferences.getString(
                KEY_CALENDAR_THEME, DEFAULT_CALENDAR_THEME));
    }

    public void setCalendarTheme(String themeId) {
        preferences.edit().putString(KEY_CALENDAR_THEME,
                normalizeCalendarTheme(themeId)).apply();
    }

    /**
     * Only rejects ids that cannot be a preset at all. The catalogue itself lives in the ultimate
     * flavour, so an unknown-but-plausible id is stored as-is and resolved to the default there.
     */
    private static String normalizeCalendarTheme(String themeId) {
        if (themeId == null) return DEFAULT_CALENDAR_THEME;
        String trimmed = themeId.trim();
        return trimmed.length() == 0 || trimmed.length() > 120 ? DEFAULT_CALENDAR_THEME : trimmed;
    }

    public boolean isSmallSeconds() {
        return preferences.getBoolean(KEY_SMALL_SECONDS, DEFAULT_SMALL_SECONDS);
    }

    public void setSmallSeconds(boolean smallSeconds) {
        preferences.edit().putBoolean(KEY_SMALL_SECONDS, smallSeconds).apply();
    }

    public boolean isPortraitStacked() {
        return preferences.getBoolean(KEY_PORTRAIT_STACKED, DEFAULT_PORTRAIT_STACKED);
    }

    public void setPortraitStacked(boolean portraitStacked) {
        preferences.edit().putBoolean(KEY_PORTRAIT_STACKED, portraitStacked).apply();
    }

    /**
     * @return whether the date and lunar lines stack on two rows in landscape. Portrait always
     *         stacks regardless of this value.
     */
    public boolean isDateLunarDualLine() {
        return preferences.getBoolean(KEY_DATE_LUNAR_DUAL_LINE, DEFAULT_DATE_LUNAR_DUAL_LINE);
    }

    public void setDateLunarDualLine(boolean dualLine) {
        preferences.edit().putBoolean(KEY_DATE_LUNAR_DUAL_LINE, dualLine).apply();
    }

    public boolean isUse24Hour() {
        return preferences.getBoolean(KEY_USE_24_HOUR, DEFAULT_USE_24_HOUR);
    }

    public void setUse24Hour(boolean use24Hour) {
        preferences.edit().putBoolean(KEY_USE_24_HOUR, use24Hour).apply();
    }

    /**
     * @return the interface language: {@link #LANGUAGE_SIMPLIFIED}, {@link #LANGUAGE_TRADITIONAL}
     *         or {@link #LANGUAGE_ENGLISH}. Falls back to the legacy boolean on first read so an
     *         existing English preference is preserved after upgrade.
     */
    public String getClockLanguage() {
        String stored = preferences.getString(KEY_CLOCK_LANGUAGE, null);
        if (stored != null) {
            return normalizeClockLanguage(stored);
        }
        return preferences.getBoolean(KEY_CLOCK_USE_ENGLISH, DEFAULT_CLOCK_USE_ENGLISH)
                ? LANGUAGE_ENGLISH : LANGUAGE_SIMPLIFIED;
    }

    public void setClockLanguage(String language) {
        String normalized = normalizeClockLanguage(language);
        preferences.edit()
                .putString(KEY_CLOCK_LANGUAGE, normalized)
                // Keep the legacy boolean in sync for any code path still reading it.
                .putBoolean(KEY_CLOCK_USE_ENGLISH, LANGUAGE_ENGLISH.equals(normalized))
                .apply();
    }

    private static String normalizeClockLanguage(String language) {
        if (LANGUAGE_ENGLISH.equals(language)) {
            return LANGUAGE_ENGLISH;
        }
        if (LANGUAGE_TRADITIONAL.equals(language)) {
            return LANGUAGE_TRADITIONAL;
        }
        return LANGUAGE_SIMPLIFIED;
    }

    public boolean isClockUseEnglish() {
        return LANGUAGE_ENGLISH.equals(getClockLanguage());
    }

    public void setClockUseEnglish(boolean useEnglish) {
        setClockLanguage(useEnglish ? LANGUAGE_ENGLISH : LANGUAGE_SIMPLIFIED);
    }

    /** @return the trimmed custom message, or "" when none is set. */
    public String getCustomMessage() {
        return normalizeCustomMessage(preferences.getString(KEY_CUSTOM_MESSAGE, DEFAULT_CUSTOM_MESSAGE));
    }

    public void setCustomMessage(String message) {
        preferences.edit().putString(KEY_CUSTOM_MESSAGE, normalizeCustomMessage(message)).apply();
    }

    /** Trims whitespace and caps the length so a pasted blob cannot break the layout. */
    public static String normalizeCustomMessage(String message) {
        if (message == null) {
            return DEFAULT_CUSTOM_MESSAGE;
        }
        String trimmed = message.trim();
        if (trimmed.length() > MAX_CUSTOM_MESSAGE_LENGTH) {
            trimmed = trimmed.substring(0, MAX_CUSTOM_MESSAGE_LENGTH);
        }
        return trimmed;
    }

    /** @return the full date pattern applied when the interface is Chinese. */
    public String getDatePatternCn() {
        return normalizeDatePattern(
                preferences.getString(KEY_DATE_PATTERN_CN, DEFAULT_DATE_PATTERN_CN),
                DEFAULT_DATE_PATTERN_CN);
    }

    public void setDatePatternCn(String pattern) {
        preferences.edit().putString(KEY_DATE_PATTERN_CN,
                normalizeDatePattern(pattern, DEFAULT_DATE_PATTERN_CN)).apply();
    }

    /** @return the full date pattern applied when the interface is English. */
    public String getDatePatternEn() {
        return normalizeDatePattern(
                preferences.getString(KEY_DATE_PATTERN_EN, DEFAULT_DATE_PATTERN_EN),
                DEFAULT_DATE_PATTERN_EN);
    }

    public void setDatePatternEn(String pattern) {
        preferences.edit().putString(KEY_DATE_PATTERN_EN,
                normalizeDatePattern(pattern, DEFAULT_DATE_PATTERN_EN)).apply();
    }

    /** Falls back to {@code fallback} when {@code pattern} fails validation, guarding stored data. */
    public static String normalizeDatePattern(String pattern, String fallback) {
        return DateFormatter.isValidPattern(pattern) ? pattern : fallback;
    }

    // ---- Pro settings-UI restoration state (does not affect rendering directly) ----

    public String getDateCore(boolean english) {
        return preferences.getString(english ? KEY_DATE_CORE_EN : KEY_DATE_CORE_CN, "");
    }

    public String getDateCombo(boolean english) {
        return preferences.getString(english ? KEY_DATE_COMBO_EN : KEY_DATE_COMBO_CN, "");
    }

    public boolean isDateCustomEnabled(boolean english) {
        return preferences.getBoolean(
                english ? KEY_DATE_CUSTOM_ENABLED_EN : KEY_DATE_CUSTOM_ENABLED_CN, false);
    }

    public String getDateCustomText(boolean english) {
        return preferences.getString(english ? KEY_DATE_CUSTOM_TEXT_EN : KEY_DATE_CUSTOM_TEXT_CN, "");
    }

    /** Persists the Pro date-format UI state for one language in a single edit. */
    public void setDateFormatState(boolean english, String core, String combo,
            boolean customEnabled, String customText) {
        preferences.edit()
                .putString(english ? KEY_DATE_CORE_EN : KEY_DATE_CORE_CN, core == null ? "" : core)
                .putString(english ? KEY_DATE_COMBO_EN : KEY_DATE_COMBO_CN, combo == null ? "" : combo)
                .putBoolean(english ? KEY_DATE_CUSTOM_ENABLED_EN : KEY_DATE_CUSTOM_ENABLED_CN,
                        customEnabled)
                .putString(english ? KEY_DATE_CUSTOM_TEXT_EN : KEY_DATE_CUSTOM_TEXT_CN,
                        customText == null ? "" : customText)
                .apply();
    }

    public int getScreenOrientation() {
        if (preferences.contains(KEY_SCREEN_ORIENTATION)) {
            return preferences.getInt(KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION);
        }
        // Migrate the legacy boolean "force landscape" flag to the tri-state value.
        if (preferences.getBoolean(KEY_FORCE_LANDSCAPE, false)) {
            return ORIENTATION_LANDSCAPE;
        }
        return DEFAULT_SCREEN_ORIENTATION;
    }

    public void setScreenOrientation(int mode) {
        preferences.edit().putInt(KEY_SCREEN_ORIENTATION, mode).apply();
    }

    /** Maps a screen-orientation mode to the matching {@link ActivityInfo} constant. */
    public static int toActivityInfoOrientation(int mode) {
        switch (mode) {
            case ORIENTATION_PORTRAIT:
                return ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            case ORIENTATION_LANDSCAPE:
                return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            default:
                return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
    }

    public boolean isUseNetworkTime() {
        return preferences.getBoolean(KEY_USE_NETWORK_TIME, DEFAULT_USE_NETWORK_TIME);
    }

    public void setUseNetworkTime(boolean useNetworkTime) {
        preferences.edit().putBoolean(KEY_USE_NETWORK_TIME, useNetworkTime).apply();
    }

    public int getSyncIntervalMinutes() {
        return preferences.getInt(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES);
    }

    public void setSyncIntervalMinutes(int minutes) {
        preferences.edit().putInt(KEY_SYNC_INTERVAL_MINUTES, minutes).apply();
    }

    /** @return the selected time zone id, or {@link #TIME_ZONE_FOLLOW_SYSTEM} to follow the system. */
    public String getTimeZoneId() {
        return preferences.getString(KEY_TIME_ZONE_ID, DEFAULT_TIME_ZONE_ID);
    }

    public void setTimeZoneId(String timeZoneId) {
        preferences.edit().putString(KEY_TIME_ZONE_ID, timeZoneId == null ? TIME_ZONE_FOLLOW_SYSTEM : timeZoneId).apply();
    }

    public boolean isWeatherEnabled() {
        return preferences.getBoolean(KEY_WEATHER_ENABLED, DEFAULT_WEATHER_ENABLED);
    }

    public void setWeatherEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_WEATHER_ENABLED, enabled).apply();
    }

    public boolean isWeatherDetailed() {
        return preferences.getBoolean(KEY_WEATHER_DETAILED, DEFAULT_WEATHER_DETAILED);
    }

    public void setWeatherDetailed(boolean detailed) {
        preferences.edit().putBoolean(KEY_WEATHER_DETAILED, detailed).apply();
    }

    public boolean isWeatherIconFill() {
        return preferences.getBoolean(KEY_WEATHER_ICON_FILL, DEFAULT_WEATHER_ICON_FILL);
    }

    public void setWeatherIconFill(boolean fill) {
        preferences.edit().putBoolean(KEY_WEATHER_ICON_FILL, fill).apply();
    }

    public boolean isWeatherIconDynamicColor() {
        return preferences.getBoolean(KEY_WEATHER_ICON_DYNAMIC_COLOR, DEFAULT_WEATHER_ICON_DYNAMIC_COLOR);
    }

    public void setWeatherIconDynamicColor(boolean dynamicColor) {
        preferences.edit().putBoolean(KEY_WEATHER_ICON_DYNAMIC_COLOR, dynamicColor).apply();
    }

    public String getWeatherTemperatureUnit() {
        return normalizeWeatherTemperatureUnit(
                preferences.getString(KEY_WEATHER_TEMPERATURE_UNIT,
                        DEFAULT_WEATHER_TEMPERATURE_UNIT));
    }

    public void setWeatherTemperatureUnit(String unit) {
        preferences.edit().putString(KEY_WEATHER_TEMPERATURE_UNIT,
                normalizeWeatherTemperatureUnit(unit)).apply();
    }

    public static String normalizeWeatherTemperatureUnit(String unit) {
        return WeatherTemperatureFormatter.normalizeUnit(unit);
    }

    public int getWeatherIntervalMinutes() {
        int value = preferences.getInt(KEY_WEATHER_INTERVAL_MINUTES, DEFAULT_WEATHER_INTERVAL_MINUTES);
        return isValidWeatherInterval(value) ? value : DEFAULT_WEATHER_INTERVAL_MINUTES;
    }

    public void setWeatherIntervalMinutes(int minutes) {
        preferences.edit().putInt(KEY_WEATHER_INTERVAL_MINUTES,
                isValidWeatherInterval(minutes) ? minutes : DEFAULT_WEATHER_INTERVAL_MINUTES).apply();
    }

    public static boolean isValidWeatherInterval(int minutes) {
        return minutes == 10 || minutes == 30 || minutes == 60 || minutes == 180
                || minutes == 360 || minutes == 720;
    }

    public String getWeatherLocationMode() {
        String mode = preferences.getString(KEY_WEATHER_LOCATION_MODE, DEFAULT_WEATHER_LOCATION_MODE);
        return WEATHER_LOCATION_MANUAL.equals(mode) && getWeatherLocationId().length() > 0
                ? WEATHER_LOCATION_MANUAL : WEATHER_LOCATION_AUTOMATIC;
    }

    public void setWeatherLocationMode(String mode) {
        preferences.edit().putString(KEY_WEATHER_LOCATION_MODE,
                WEATHER_LOCATION_MANUAL.equals(mode) ? WEATHER_LOCATION_MANUAL
                        : WEATHER_LOCATION_AUTOMATIC).apply();
    }

    public String getWeatherLocationId() { return preferences.getString(KEY_WEATHER_LOCATION_ID, ""); }
    public String getWeatherProvince() { return preferences.getString(KEY_WEATHER_PROVINCE, ""); }
    public String getWeatherCity() { return preferences.getString(KEY_WEATHER_CITY, ""); }
    public String getWeatherDistrict() { return preferences.getString(KEY_WEATHER_DISTRICT, ""); }

    public double getWeatherLatitude() {
        return Double.longBitsToDouble(preferences.getLong(KEY_WEATHER_LATITUDE,
                Double.doubleToRawLongBits(Double.NaN)));
    }

    public double getWeatherLongitude() {
        return Double.longBitsToDouble(preferences.getLong(KEY_WEATHER_LONGITUDE,
                Double.doubleToRawLongBits(Double.NaN)));
    }

    public void setManualWeatherLocation(String locationId, String province, String city, String district) {
        setManualWeatherLocation(locationId, province, city, district, Double.NaN, Double.NaN);
    }

    public void setManualWeatherLocation(String locationId, String province, String city,
            String district, double latitude, double longitude) {
        preferences.edit()
                .putString(KEY_WEATHER_LOCATION_ID, locationId == null ? "" : locationId)
                .putString(KEY_WEATHER_PROVINCE, province == null ? "" : province)
                .putString(KEY_WEATHER_CITY, city == null ? "" : city)
                .putString(KEY_WEATHER_DISTRICT, district == null ? "" : district)
                .putLong(KEY_WEATHER_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(KEY_WEATHER_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .apply();
    }

    /** Restores all user-configurable settings to their defaults. */
    public void restoreDefaults() {
        preferences.edit()
                .putString(KEY_BACKGROUND_MODE, MODE_COLOR)
                .putInt(KEY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR)
                .putBoolean(KEY_DIM_BACKGROUND, DEFAULT_DIM_BACKGROUND)
                .putBoolean(KEY_SCHEDULE_DIM_BACKGROUND, DEFAULT_SCHEDULE_DIM_BACKGROUND)
                .putInt(KEY_DIM_START_MINUTES, DEFAULT_DIM_START_MINUTES)
                .putInt(KEY_DIM_END_MINUTES, DEFAULT_DIM_END_MINUTES)
                .putFloat(KEY_TIME_FONT_SCALE, DEFAULT_TIME_FONT_SCALE)
                .putFloat(KEY_DATE_FONT_SCALE, DEFAULT_DATE_FONT_SCALE)
                .putInt(KEY_TIME_COLOR, DEFAULT_TEXT_COLOR)
                .putInt(KEY_DATE_COLOR, DEFAULT_TEXT_COLOR)
                .putBoolean(KEY_SHOW_STATUS_ICONS, DEFAULT_SHOW_STATUS_ICONS)
                .putFloat(KEY_STATUS_ICON_SCALE, DEFAULT_STATUS_ICON_SCALE)
                .putBoolean(KEY_BLINK_COLON, DEFAULT_BLINK_COLON)
                .putBoolean(KEY_ANIMATE_TIME_CHANGES, DEFAULT_ANIMATE_TIME_CHANGES)
                .putString(KEY_TIME_TRANSITION, DEFAULT_TIME_TRANSITION)
                .putBoolean(KEY_HOURLY_CHIME, DEFAULT_HOURLY_CHIME)
                .putBoolean(KEY_HALF_HOUR_CHIME, DEFAULT_HALF_HOUR_CHIME)
                .putBoolean(KEY_HOURLY_CHIME_QUIET, DEFAULT_HOURLY_CHIME_QUIET)
                .putInt(KEY_HOURLY_CHIME_QUIET_START, DEFAULT_HOURLY_CHIME_QUIET_START)
                .putInt(KEY_HOURLY_CHIME_QUIET_END, DEFAULT_HOURLY_CHIME_QUIET_END)
                .putBoolean(KEY_BOLD_TEXT, DEFAULT_BOLD_TEXT)
                .putString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY)
                .putBoolean(KEY_SHOW_SECONDS, DEFAULT_SHOW_SECONDS)
                .putBoolean(KEY_SHOW_LUNAR, DEFAULT_SHOW_LUNAR)
                .putBoolean(KEY_AUTO_START, DEFAULT_AUTO_START)
                .putInt(KEY_CALENDAR_WEEK_START, DEFAULT_CALENDAR_WEEK_START)
                .putBoolean(KEY_CALENDAR_HIGHLIGHT_WEEKENDS,
                        DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS)
                .putString(KEY_CALENDAR_THEME, DEFAULT_CALENDAR_THEME)
                .putBoolean(KEY_SMALL_SECONDS, DEFAULT_SMALL_SECONDS)
                .putBoolean(KEY_PORTRAIT_STACKED, DEFAULT_PORTRAIT_STACKED)
                .putBoolean(KEY_DATE_LUNAR_DUAL_LINE, DEFAULT_DATE_LUNAR_DUAL_LINE)
                .putBoolean(KEY_USE_24_HOUR, DEFAULT_USE_24_HOUR)
                .putBoolean(KEY_CLOCK_USE_ENGLISH, DEFAULT_CLOCK_USE_ENGLISH)
                .putString(KEY_CLOCK_LANGUAGE, DEFAULT_CLOCK_LANGUAGE)
                .remove(KEY_CUSTOM_MESSAGE)
                .putString(KEY_DATE_PATTERN_CN, DEFAULT_DATE_PATTERN_CN)
                .putString(KEY_DATE_PATTERN_EN, DEFAULT_DATE_PATTERN_EN)
                .remove(KEY_DATE_CORE_CN)
                .remove(KEY_DATE_CORE_EN)
                .remove(KEY_DATE_COMBO_CN)
                .remove(KEY_DATE_COMBO_EN)
                .remove(KEY_DATE_CUSTOM_ENABLED_CN)
                .remove(KEY_DATE_CUSTOM_ENABLED_EN)
                .remove(KEY_DATE_CUSTOM_TEXT_CN)
                .remove(KEY_DATE_CUSTOM_TEXT_EN)
                .putInt(KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION)
                .remove(KEY_FORCE_LANDSCAPE)
                .putBoolean(KEY_WEATHER_ENABLED, DEFAULT_WEATHER_ENABLED)
                .putBoolean(KEY_WEATHER_DETAILED, DEFAULT_WEATHER_DETAILED)
                .putBoolean(KEY_WEATHER_ICON_FILL, DEFAULT_WEATHER_ICON_FILL)
                .putBoolean(KEY_WEATHER_ICON_DYNAMIC_COLOR, DEFAULT_WEATHER_ICON_DYNAMIC_COLOR)
                .putString(KEY_WEATHER_TEMPERATURE_UNIT, DEFAULT_WEATHER_TEMPERATURE_UNIT)
                .putInt(KEY_WEATHER_INTERVAL_MINUTES, DEFAULT_WEATHER_INTERVAL_MINUTES)
                .putString(KEY_WEATHER_LOCATION_MODE, DEFAULT_WEATHER_LOCATION_MODE)
                .remove(KEY_WEATHER_LOCATION_ID)
                .remove(KEY_WEATHER_PROVINCE)
                .remove(KEY_WEATHER_CITY)
                .remove(KEY_WEATHER_DISTRICT)
                .remove(KEY_WEATHER_LATITUDE)
                .remove(KEY_WEATHER_LONGITUDE)
                .apply();
    }

    private static float clampScale(float scale) {
        return Math.max(MIN_FONT_SCALE, Math.min(MAX_FONT_SCALE, scale));
    }
}
