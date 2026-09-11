package com.clockmods.ultimate;

import android.content.Context;
import android.content.SharedPreferences;

/** Global anti-burn settings shared by every Ultimate destination. */
public final class AntiBurnPreferences {
    public static final String PREFERENCES_NAME = "clockmods_anti_burn";
    public static final boolean DEFAULT_ENABLED = false;
    public static final int DEFAULT_PERIOD_MINUTES = 10;
    public static final float DEFAULT_AMPLITUDE_DP = 4f;
    public static final boolean DEFAULT_AUTO_DIM = true;
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PERIOD = "period_minutes";
    private static final String KEY_AMPLITUDE = "amplitude_dp";
    private static final String KEY_AUTO_DIM = "auto_dim";
    private final SharedPreferences preferences;

    public AntiBurnPreferences(Context context) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() { return preferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED); }
    public void setEnabled(boolean value) { preferences.edit().putBoolean(KEY_ENABLED, value).apply(); }
    public int getPeriodMinutes() { return normalizePeriod(preferences.getInt(KEY_PERIOD,
            DEFAULT_PERIOD_MINUTES)); }
    public void setPeriodMinutes(int value) { preferences.edit().putInt(KEY_PERIOD,
            normalizePeriod(value)).apply(); }
    public float getAmplitudeDp() { return normalizeAmplitude(preferences.getFloat(KEY_AMPLITUDE,
            DEFAULT_AMPLITUDE_DP)); }
    public void setAmplitudeDp(float value) { preferences.edit().putFloat(KEY_AMPLITUDE,
            normalizeAmplitude(value)).apply(); }
    public boolean isAutoDim() { return preferences.getBoolean(KEY_AUTO_DIM, DEFAULT_AUTO_DIM); }
    public void setAutoDim(boolean value) { preferences.edit().putBoolean(KEY_AUTO_DIM, value).apply(); }

    public static int normalizePeriod(int value) {
        if (value == 1 || value == 30 || value == 60) return value;
        return 10;
    }

    public static float normalizeAmplitude(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return DEFAULT_AMPLITUDE_DP;
        return Math.max(0f, Math.min(12f, value));
    }
}
