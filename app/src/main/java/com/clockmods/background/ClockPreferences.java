package com.clockmods.background;

import android.content.Context;
import android.content.SharedPreferences;

public class ClockPreferences {
    public static final String MODE_COLOR = "color";
    public static final String MODE_IMAGE = "image";
    public static final String FONT_SYSTEM = "system";
    public static final String FONT_ROBOTO = "roboto";
    public static final String FONT_GOOGLE_SANS = "google_sans";

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
    private static final String KEY_BLINK_COLON = "blink_colon";
    private static final String KEY_ANIMATE_TIME_CHANGES = "animate_time_changes";
    private static final String KEY_BOLD_TEXT = "bold_text";
    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_SHOW_SECONDS = "show_seconds";
    private static final String KEY_SHOW_LUNAR = "show_lunar";
    private static final String KEY_SMALL_SECONDS = "small_seconds";
    private static final String KEY_USE_24_HOUR = "use_24_hour";
    private static final String KEY_CLOCK_USE_ENGLISH = "clock_use_english";
    private static final String KEY_USE_NETWORK_TIME = "use_network_time";
    private static final String KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes";
    private static final String KEY_TIME_ZONE_ID = "time_zone_id";
    private static final String KEY_WEATHER_ENABLED = "weather_enabled";
    private static final String KEY_WEATHER_INTERVAL_MINUTES = "weather_interval_minutes";
    private static final String KEY_WEATHER_LOCATION_MODE = "weather_location_mode";
    private static final String KEY_WEATHER_LOCATION_ID = "weather_location_id";
    private static final String KEY_WEATHER_PROVINCE = "weather_province";
    private static final String KEY_WEATHER_CITY = "weather_city";
    private static final String KEY_WEATHER_DISTRICT = "weather_district";

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
    public static final boolean DEFAULT_BLINK_COLON = false;
    public static final boolean DEFAULT_ANIMATE_TIME_CHANGES = true;
    public static final boolean DEFAULT_BOLD_TEXT = false;
    public static final String DEFAULT_FONT_FAMILY = FONT_SYSTEM;
    public static final boolean DEFAULT_SHOW_SECONDS = true;
    public static final boolean DEFAULT_SHOW_LUNAR = true;
    public static final boolean DEFAULT_SMALL_SECONDS = false;
    public static final boolean DEFAULT_USE_24_HOUR = true;
    public static final boolean DEFAULT_CLOCK_USE_ENGLISH = false;
    /** Network time is disabled by default; the device local time is used. */
    public static final boolean DEFAULT_USE_NETWORK_TIME = false;
    /** Default interval, in minutes, between network time synchronizations. */
    public static final int DEFAULT_SYNC_INTERVAL_MINUTES = 60;
    /** Time zone is set to follow the system by default. */
    public static final String DEFAULT_TIME_ZONE_ID = TIME_ZONE_FOLLOW_SYSTEM;
    public static final boolean DEFAULT_WEATHER_ENABLED = false;
    public static final int DEFAULT_WEATHER_INTERVAL_MINUTES = 30;
    public static final String WEATHER_LOCATION_AUTOMATIC = "automatic";
    public static final String WEATHER_LOCATION_MANUAL = "manual";
    public static final String DEFAULT_WEATHER_LOCATION_MODE = WEATHER_LOCATION_AUTOMATIC;

    /** Allowed range for the width-based font scale (fraction of screen width). */
    public static final float MIN_FONT_SCALE = 0.20f;
    public static final float MAX_FONT_SCALE = 1.00f;

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
        if (FONT_ROBOTO.equals(fontFamily) || FONT_GOOGLE_SANS.equals(fontFamily)) {
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

    public boolean isSmallSeconds() {
        return preferences.getBoolean(KEY_SMALL_SECONDS, DEFAULT_SMALL_SECONDS);
    }

    public void setSmallSeconds(boolean smallSeconds) {
        preferences.edit().putBoolean(KEY_SMALL_SECONDS, smallSeconds).apply();
    }

    public boolean isUse24Hour() {
        return preferences.getBoolean(KEY_USE_24_HOUR, DEFAULT_USE_24_HOUR);
    }

    public void setUse24Hour(boolean use24Hour) {
        preferences.edit().putBoolean(KEY_USE_24_HOUR, use24Hour).apply();
    }

    public boolean isClockUseEnglish() {
        return preferences.getBoolean(KEY_CLOCK_USE_ENGLISH, DEFAULT_CLOCK_USE_ENGLISH);
    }

    public void setClockUseEnglish(boolean useEnglish) {
        preferences.edit().putBoolean(KEY_CLOCK_USE_ENGLISH, useEnglish).apply();
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

    public void setManualWeatherLocation(String locationId, String province, String city, String district) {
        preferences.edit()
                .putString(KEY_WEATHER_LOCATION_ID, locationId == null ? "" : locationId)
                .putString(KEY_WEATHER_PROVINCE, province == null ? "" : province)
                .putString(KEY_WEATHER_CITY, city == null ? "" : city)
                .putString(KEY_WEATHER_DISTRICT, district == null ? "" : district)
                .apply();
    }

    /** Restores background, font size and font color settings to their defaults (black background, white text). */
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
                .putBoolean(KEY_BLINK_COLON, DEFAULT_BLINK_COLON)
                .putBoolean(KEY_ANIMATE_TIME_CHANGES, DEFAULT_ANIMATE_TIME_CHANGES)
                .putBoolean(KEY_BOLD_TEXT, DEFAULT_BOLD_TEXT)
                .putString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY)
                .putBoolean(KEY_SHOW_SECONDS, DEFAULT_SHOW_SECONDS)
                .putBoolean(KEY_SHOW_LUNAR, DEFAULT_SHOW_LUNAR)
                .putBoolean(KEY_SMALL_SECONDS, DEFAULT_SMALL_SECONDS)
                .putBoolean(KEY_USE_24_HOUR, DEFAULT_USE_24_HOUR)
                .putBoolean(KEY_CLOCK_USE_ENGLISH, DEFAULT_CLOCK_USE_ENGLISH)
                .putBoolean(KEY_WEATHER_ENABLED, DEFAULT_WEATHER_ENABLED)
                .putInt(KEY_WEATHER_INTERVAL_MINUTES, DEFAULT_WEATHER_INTERVAL_MINUTES)
                .putString(KEY_WEATHER_LOCATION_MODE, DEFAULT_WEATHER_LOCATION_MODE)
                .remove(KEY_WEATHER_LOCATION_ID)
                .remove(KEY_WEATHER_PROVINCE)
                .remove(KEY_WEATHER_CITY)
                .remove(KEY_WEATHER_DISTRICT)
                .apply();
    }

    private static float clampScale(float scale) {
        return Math.max(MIN_FONT_SCALE, Math.min(MAX_FONT_SCALE, scale));
    }
}