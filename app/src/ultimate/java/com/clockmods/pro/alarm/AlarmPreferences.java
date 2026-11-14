package com.clockmods.pro.alarm;

import android.content.Context;
import android.content.SharedPreferences;

/** Opt-in controls shared by alarm and timer occurrences. */
public final class AlarmPreferences {
    public static final String RANDOM = "random_buttons", VOLUME = "volume_snooze", POWER = "screen_off_snooze";
    private AlarmPreferences() {}
    public static SharedPreferences settings(Context context) { return context.getSharedPreferences("alarm_settings", 0); }
    public static boolean enabled(Context context, String key) { return settings(context).getBoolean(key, false); }
    public static boolean swapped(Context context, long id) { return context.getSharedPreferences("pro_alarm", 0).getBoolean("ring_swap_" + id, false); }
}
