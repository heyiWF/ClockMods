package com.clockmods.ultimate.clock;

import android.content.Context;
import android.content.SharedPreferences;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.sdk.clock.ClockState;

/** Persistence owned by the Ultimate clock host, deliberately separate from style renderers. */
public final class UltimateClockPreferences {
    public static final String PREFERENCES_NAME = "clockmods_ultimate_style";
    public static final String KEY_STYLE_ID = "style_id";
    public static final String KEY_SECOND_MOTION = "second_motion";
    public static final String KEY_FOLLOW_REDUCED_MOTION = "follow_reduced_motion";
    public static final String KEY_BACKGROUND_MODE = "background_mode";
    public static final String BACKGROUND_MODE_THEME = "theme";
    public static final String BACKGROUND_MODE_COLOR = "color";
    public static final String BACKGROUND_MODE_IMAGE = "image";
    public static final String DEFAULT_STYLE_ID = UltimateClockStyles.STYLE_GLASS_ATELIER;
    public static final ClockState.SecondHandMotion DEFAULT_SECOND_HAND_MOTION =
            ClockState.SecondHandMotion.SWEEP;
    public static final boolean DEFAULT_FOLLOW_SYSTEM_REDUCED_MOTION = true;
    public static final String DEFAULT_BACKGROUND_MODE = BACKGROUND_MODE_THEME;

    private static final String PREFS_NAME = PREFERENCES_NAME;
    private static final String KEY_SECOND_HAND_MOTION = KEY_SECOND_MOTION;
    private static final String KEY_FOLLOW_SYSTEM_REDUCED_MOTION = KEY_FOLLOW_REDUCED_MOTION;

    private final SharedPreferences preferences;

    public UltimateClockPreferences(Context context) {
        this(preferencesFrom(context));
        migrateLegacyImageBackground(context);
    }

    UltimateClockPreferences(SharedPreferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("preferences must not be null");
        }
        this.preferences = preferences;
    }

    private static SharedPreferences preferencesFrom(Context context) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void migrateLegacyImageBackground(Context context) {
        if (preferences.contains(KEY_BACKGROUND_MODE)) return;
        BackgroundRepository legacy = new BackgroundRepository(context);
        String migratedMode = ClockPreferences.MODE_IMAGE.equals(legacy.getBackgroundMode())
                && legacy.hasImage() ? BACKGROUND_MODE_IMAGE : BACKGROUND_MODE_THEME;
        preferences.edit().putString(KEY_BACKGROUND_MODE, migratedMode).apply();
    }

    public String getStyleId() {
        return normalizeStyleId(preferences.getString(KEY_STYLE_ID, DEFAULT_STYLE_ID));
    }

    public void setStyleId(String styleId) {
        preferences.edit().putString(KEY_STYLE_ID, normalizeStyleId(styleId)).apply();
    }

    public ClockState.SecondHandMotion getSecondHandMotion() {
        String stored = preferences.getString(
                KEY_SECOND_HAND_MOTION, "smooth");
        if (stored != null) {
            String normalized = stored.trim().toLowerCase(java.util.Locale.US);
            if ("smooth".equals(normalized) || "sweep".equals(normalized)) {
                return ClockState.SecondHandMotion.SWEEP;
            }
            if ("tick".equals(normalized) || "ticking".equals(normalized)) {
                return ClockState.SecondHandMotion.TICK;
            }
            if ("off".equals(normalized)) return ClockState.SecondHandMotion.OFF;
        }
        return DEFAULT_SECOND_HAND_MOTION;
    }

    public void setSecondHandMotion(ClockState.SecondHandMotion motion) {
        ClockState.SecondHandMotion safe = motion == null
                ? DEFAULT_SECOND_HAND_MOTION : motion;
        String stored = safe == ClockState.SecondHandMotion.SWEEP ? "smooth"
                : safe == ClockState.SecondHandMotion.TICK ? "tick" : "off";
        preferences.edit().putString(KEY_SECOND_HAND_MOTION, stored).apply();
    }

    public boolean isFollowSystemReducedMotion() {
        return preferences.getBoolean(KEY_FOLLOW_SYSTEM_REDUCED_MOTION,
                DEFAULT_FOLLOW_SYSTEM_REDUCED_MOTION);
    }

    public void setFollowSystemReducedMotion(boolean follow) {
        preferences.edit().putBoolean(KEY_FOLLOW_SYSTEM_REDUCED_MOTION, follow).apply();
    }

    public String getBackgroundMode() {
        return normalizeBackgroundMode(preferences.getString(
                KEY_BACKGROUND_MODE, DEFAULT_BACKGROUND_MODE));
    }

    public void setBackgroundMode(String mode) {
        preferences.edit().putString(KEY_BACKGROUND_MODE, normalizeBackgroundMode(mode)).apply();
    }

    public ClockPalette getPalette(String styleId) {
        ClockPalette defaults = ClockPalette.DEFAULT;
        if (!ClockPalette.supports(styleId)) return defaults;
        return new ClockPalette(preferences.getInt("palette_background__" + styleId, defaults.background),
                preferences.getInt("palette_panel__" + styleId, defaults.panel),
                preferences.getInt("palette_accent__" + styleId, defaults.accent));
    }

    public void setPalette(String styleId, ClockPalette palette) {
        if (!ClockPalette.supports(styleId) || palette == null) return;
        preferences.edit().putInt("palette_background__" + styleId, palette.background)
                .putInt("palette_panel__" + styleId, palette.panel)
                .putInt("palette_accent__" + styleId, palette.accent).apply();
    }

    public void restoreDefaults() {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith("palette_")) editor.remove(key);
        }
        editor
                .putString(KEY_STYLE_ID, DEFAULT_STYLE_ID)
                .putString(KEY_SECOND_HAND_MOTION, "smooth")
                .putBoolean(KEY_FOLLOW_SYSTEM_REDUCED_MOTION,
                        DEFAULT_FOLLOW_SYSTEM_REDUCED_MOTION)
                .putString(KEY_BACKGROUND_MODE, DEFAULT_BACKGROUND_MODE)
                .apply();
    }

    static String normalizeStyleId(String styleId) {
        if (styleId == null || styleId.trim().length() == 0) return DEFAULT_STYLE_ID;
        String trimmed = styleId.trim();
        return trimmed.length() > 120 ? DEFAULT_STYLE_ID : trimmed;
    }

    static String normalizeBackgroundMode(String mode) {
        if (BACKGROUND_MODE_COLOR.equals(mode)) return BACKGROUND_MODE_COLOR;
        if (BACKGROUND_MODE_IMAGE.equals(mode)) return BACKGROUND_MODE_IMAGE;
        return BACKGROUND_MODE_THEME;
    }
}
