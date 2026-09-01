package com.clockmods;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import com.clockmods.background.ClockPreferences;
import com.clockmods.ui.DateFormatter;

import java.util.Locale;

/**
 * Applies the user's interface-language choice ({@link ClockPreferences#getClockLanguage()}) as an
 * Android resource-configuration locale, so every {@code getString(...)} lookup, system-formatted
 * value and layout direction follows the setting &mdash; not just the clock face.
 *
 * <p>Activities call {@link #wrap(Context)} from {@code attachBaseContext}; background components
 * that build user-facing text (notifications, services) wrap their context the same way. The lunar
 * calendar and almanac are intentionally locale-independent and stay Chinese regardless.
 */
public final class LocaleManager {
    private LocaleManager() {
    }

    /** @return the locale that matches the stored interface-language preference. */
    public static Locale resolveLocale(Context context) {
        String language = new ClockPreferences(context).getClockLanguage();
        if (ClockPreferences.LANGUAGE_ENGLISH.equals(language)) {
            return Locale.ENGLISH;
        }
        if (ClockPreferences.LANGUAGE_TRADITIONAL.equals(language)) {
            return Locale.TRADITIONAL_CHINESE; // zh-TW -> res/values-zh-rTW
        }
        return Locale.CHINA; // zh-Hans (default) -> res/values
    }

    /** Maps a stored interface-language code to the matching {@link DateFormatter.Lang}. */
    public static DateFormatter.Lang dateLang(String clockLanguage) {
        if (ClockPreferences.LANGUAGE_ENGLISH.equals(clockLanguage)) {
            return DateFormatter.Lang.ENGLISH;
        }
        if (ClockPreferences.LANGUAGE_TRADITIONAL.equals(clockLanguage)) {
            return DateFormatter.Lang.TRADITIONAL;
        }
        return DateFormatter.Lang.CHINESE;
    }

    /**
     * Returns a context whose resources are configured for the selected interface language. On
     * API 17+ this is a fresh configuration context; on older devices it
     * mutates the shared resources configuration as a fallback and returns the original context.
     */
    @SuppressLint("AppBundleLocaleChanges") // Deliberate, user-driven in-app language override.
    public static Context wrap(Context context) {
        if (context == null) {
            return null;
        }
        Locale locale = resolveLocale(context);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
}
