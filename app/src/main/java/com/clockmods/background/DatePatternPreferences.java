package com.clockmods.background;

import android.content.SharedPreferences;
import com.clockmods.LocaleManager;
import com.clockmods.ui.DateFormatter;

/** Language-scoped date patterns; existing Chinese and English keys remain compatible. */
public final class DatePatternPreferences {
    private DatePatternPreferences() {}
    private static String suffix(String language) {
        if ("en".equals(language) || "ja".equals(language) || "ko".equals(language)) return language;
        return "cn";
    }
    public static String get(SharedPreferences preferences, String language) {
        String fallback = DateFormatter.defaultPattern(LocaleManager.dateLang(language));
        String stored = preferences.getString("date_pattern_" + suffix(language), fallback);
        return DateFormatter.isValidPattern(stored) ? stored : fallback;
    }
    public static boolean save(SharedPreferences preferences, String language, String pattern) {
        String value = pattern == null ? "" : pattern.trim();
        if (!DateFormatter.isValidPattern(value)) return false;
        String suffix = suffix(language);
        preferences.edit().putString("date_pattern_" + suffix, value)
                .putString("date_core_" + suffix, "").putString("date_combo_" + suffix, "")
                .putBoolean("date_custom_enabled_" + suffix, true)
                .putString("date_custom_text_" + suffix, value).apply();
        return true;
    }
    public static void clearAdditionalLanguages(SharedPreferences.Editor editor) {
        for (String suffix : new String[]{"ja", "ko"})
            for (String prefix : new String[]{"date_pattern_", "date_core_", "date_combo_",
                    "date_custom_enabled_", "date_custom_text_"}) editor.remove(prefix + suffix);
    }
}
