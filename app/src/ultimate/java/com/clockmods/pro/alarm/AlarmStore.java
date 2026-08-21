package com.clockmods.pro.alarm;

import android.content.Context;
import android.content.SharedPreferences;

public final class AlarmStore {
    private final SharedPreferences preferences;

    public AlarmStore(Context context) {
        preferences = context.getSharedPreferences("pro_alarm", Context.MODE_PRIVATE);
    }

    public int hour() { return preferences.getInt("hour", 7); }
    public int minute() { return preferences.getInt("minute", 30); }
    public boolean enabled() { return preferences.getBoolean("enabled", false); }

    public void save(int hour, int minute, boolean enabled) {
        preferences.edit().putInt("hour", hour).putInt("minute", minute)
                .putBoolean("enabled", enabled).apply();
    }

    public void setEnabled(boolean enabled) {
        preferences.edit().putBoolean("enabled", enabled).apply();
    }
}