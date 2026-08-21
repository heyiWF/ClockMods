package com.clockmods.ultimate.settings;

import android.content.Context;
import android.content.SharedPreferences;

/** Process-wide and persistent change signal shared by embedded settings Activities. */
final class UltimateSettingsChangeTracker {
    private static final String PREFERENCES = "clockmods_ultimate_settings_changes";
    private static final String KEY_REVISION = "revision";
    private static final String KEY_SOURCE = "source";

    private UltimateSettingsChangeTracker() {
    }

    static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES,
                Context.MODE_PRIVATE);
    }

    static long revision(Context context) {
        return preferences(context).getLong(KEY_REVISION, 0L);
    }

    static String lastSource(Context context) {
        return preferences(context).getString(KEY_SOURCE, "");
    }

    static boolean isRevisionKey(String key) {
        return KEY_REVISION.equals(key);
    }

    static synchronized void record(Context context, String source) {
        SharedPreferences preferences = preferences(context);
        long nextRevision = preferences.getLong(KEY_REVISION, 0L) + 1L;
        preferences.edit()
                .putString(KEY_SOURCE, source == null ? "" : source)
                .putLong(KEY_REVISION, nextRevision)
                .apply();
    }
}
