package com.clockmods.background;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;


import java.util.Calendar;

public class ClockPreferences {
    public static final String MODE_COLOR = "color";
    public static final String MODE_IMAGE = "image";
    public static final String TRANSITION_FADE = "fade";
    public static final String TRANSITION_SLIDE_UP = "slide_up";
    public static final String TRANSITION_SLIDE_DOWN = "slide_down";
    public static final String TRANSITION_SCALE = "scale";
    public static final String TRANSITION_FLIP = "flip";

    private static final String PREFS_NAME = "clock_prefs";
    private static final String KEY_BACKGROUND_MODE = "background_mode";
    private static final String KEY_DIM_BACKGROUND = "dim_background";
    private static final String KEY_TIME_FONT_SCALE = "time_font_scale";
    private static final String KEY_DATE_FONT_SCALE = "date_font_scale";
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
    private static final String KEY_SHOW_SECONDS = "show_seconds";
    private static final String KEY_SHOW_LUNAR = "show_lunar";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_CALENDAR_WEEK_START = "calendar_week_start";
    private static final String KEY_CALENDAR_HIGHLIGHT_WEEKENDS = "calendar_highlight_weekends";
    private static final String KEY_SMALL_SECONDS = "small_seconds";
    private static final String KEY_PORTRAIT_STACKED = "portrait_stacked";
    private static final String KEY_DATE_LUNAR_DUAL_LINE = "date_lunar_dual_line";
    private static final String KEY_USE_24_HOUR = "use_24_hour";
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

    /** Sentinel value meaning "follow the device's own time zone". */
    public static final String TIME_ZONE_FOLLOW_SYSTEM = "";

    /** Fraction of the screen width occupied by the time text by default. */
    public static final float DEFAULT_TIME_FONT_SCALE = 0.88f;
    /** Fraction of the screen width occupied by the date text by default. */
    public static final float DEFAULT_DATE_FONT_SCALE = 0.55f;
    /** Fixed colour of every text element; the app no longer offers a colour setting. */
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final boolean DEFAULT_DIM_BACKGROUND = false;
    /** The network/battery status text is shown by default. */
    public static final boolean DEFAULT_SHOW_STATUS_ICONS = true;
    public static final float DEFAULT_STATUS_ICON_SCALE = 1.0f;
    public static final float MIN_STATUS_ICON_SCALE = 0.60f;
    public static final float MAX_STATUS_ICON_SCALE = 2.00f;
    public static final boolean DEFAULT_BLINK_COLON = false;
    public static final boolean DEFAULT_ANIMATE_TIME_CHANGES = false;
    public static final String DEFAULT_TIME_TRANSITION = TRANSITION_FADE;
    public static final boolean DEFAULT_HOURLY_CHIME = true;
    public static final boolean DEFAULT_HALF_HOUR_CHIME = false;
    public static final boolean DEFAULT_HOURLY_CHIME_QUIET = true;
    public static final int DEFAULT_HOURLY_CHIME_QUIET_START = 22 * 60;
    public static final int DEFAULT_HOURLY_CHIME_QUIET_END = 7 * 60;
    public static final boolean DEFAULT_BOLD_TEXT = true;
    public static final boolean DEFAULT_SHOW_SECONDS = true;
    public static final boolean DEFAULT_SHOW_LUNAR = true;
    public static final boolean DEFAULT_AUTO_START = false;
    public static final int CALENDAR_WEEK_START_SUNDAY = Calendar.SUNDAY;
    public static final int CALENDAR_WEEK_START_MONDAY = Calendar.MONDAY;
    public static final int DEFAULT_CALENDAR_WEEK_START = CALENDAR_WEEK_START_SUNDAY;
    public static final boolean DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS = false;
    public static final boolean DEFAULT_SMALL_SECONDS = false;
    public static final boolean DEFAULT_PORTRAIT_STACKED = false;
    /** Landscape only: whether the date and lunar lines stack on two rows (portrait always stacks). */
    public static final boolean DEFAULT_DATE_LUNAR_DUAL_LINE = true;
    public static final boolean DEFAULT_USE_24_HOUR = true;
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
    /** Weather icons default to white; enable to tint them with the Material 3 accent colour. */
    public static final String WEATHER_LOCATION_AUTOMATIC = "automatic";
    public static final String WEATHER_LOCATION_MANUAL = "manual";
    public static final String DEFAULT_WEATHER_LOCATION_MODE = WEATHER_LOCATION_AUTOMATIC;

    /** Allowed range for the width-based font scale (fraction of screen width). */
    public static final float MIN_FONT_SCALE = 0.20f;
    /** TEMP (compat verification build): upper limit raised to 200% of screen width. */
    public static final float MAX_FONT_SCALE = 2.00f;

    /** Background colour for the solid mode: always black. */
    public static final int BACKGROUND_COLOR = 0xFF000000;

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

    public boolean isDimBackground() {
        return preferences.getBoolean(KEY_DIM_BACKGROUND, DEFAULT_DIM_BACKGROUND);
    }

    public void setDimBackground(boolean dimBackground) {
        preferences.edit().putBoolean(KEY_DIM_BACKGROUND, dimBackground).apply();
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

    private static String calendarMotionKey(String themeId) {
        String scope = themeId == null ? "" : themeId.trim();
        if (scope.length() > 160) scope = scope.substring(0, 160);
        return "calendar_marquee__calendar:" + scope;
    }

    public com.clockmods.sdk.style.MarqueeSpec getCalendarMarquee(String themeId) {
        return com.clockmods.sdk.style.MarqueeSpec.DEFAULT;
    }

    public void setCalendarMarquee(String themeId, com.clockmods.sdk.style.MarqueeSpec value) {
        if (value == null) value = com.clockmods.sdk.style.MarqueeSpec.DEFAULT;
        String key = calendarMotionKey(themeId);
        preferences.edit().putFloat(key + "_speed", value.speedDpPerSecond)
                .putLong(key + "_pause", value.pauseMillis).putFloat(key + "_gap", value.gapDp).apply();
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
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith("calendar_marquee__")) editor.remove(key);
        }
        editor
                .putString(KEY_BACKGROUND_MODE, MODE_COLOR)
                .putBoolean(KEY_DIM_BACKGROUND, DEFAULT_DIM_BACKGROUND)
                .putFloat(KEY_TIME_FONT_SCALE, DEFAULT_TIME_FONT_SCALE)
                .putFloat(KEY_DATE_FONT_SCALE, DEFAULT_DATE_FONT_SCALE)
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
                .putBoolean(KEY_SHOW_SECONDS, DEFAULT_SHOW_SECONDS)
                .putBoolean(KEY_SHOW_LUNAR, DEFAULT_SHOW_LUNAR)
                .putBoolean(KEY_AUTO_START, DEFAULT_AUTO_START)
                .putInt(KEY_CALENDAR_WEEK_START, DEFAULT_CALENDAR_WEEK_START)
                .putBoolean(KEY_CALENDAR_HIGHLIGHT_WEEKENDS,
                        DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS)
                .putBoolean(KEY_SMALL_SECONDS, DEFAULT_SMALL_SECONDS)
                .putBoolean(KEY_PORTRAIT_STACKED, DEFAULT_PORTRAIT_STACKED)
                .putBoolean(KEY_DATE_LUNAR_DUAL_LINE, DEFAULT_DATE_LUNAR_DUAL_LINE)
                .putBoolean(KEY_USE_24_HOUR, DEFAULT_USE_24_HOUR)
                .putInt(KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION)
                .remove(KEY_FORCE_LANDSCAPE)
                .putBoolean(KEY_WEATHER_ENABLED, DEFAULT_WEATHER_ENABLED)
                .putBoolean(KEY_WEATHER_DETAILED, DEFAULT_WEATHER_DETAILED)
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
