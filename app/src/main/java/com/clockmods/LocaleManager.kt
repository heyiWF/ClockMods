package com.clockmods

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import com.clockmods.background.ClockPreferences
import com.clockmods.ui.DateFormatter
import java.util.Locale

/** Applies the user's interface-language choice to Android's resource configuration. */
object LocaleManager {
    @JvmStatic
    fun resolveLocale(context: Context): Locale =
        resolveLocale(ClockPreferences(context).getClockLanguage())

    @JvmStatic
    fun resolveLocale(language: String?): Locale = when (language) {
        ClockPreferences.LANGUAGE_ENGLISH -> Locale.ENGLISH
        ClockPreferences.LANGUAGE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
        else -> Locale.CHINA
    }

    @JvmStatic
    fun dateLang(clockLanguage: String?): DateFormatter.Lang = when (clockLanguage) {
        ClockPreferences.LANGUAGE_ENGLISH -> DateFormatter.Lang.ENGLISH
        ClockPreferences.LANGUAGE_TRADITIONAL -> DateFormatter.Lang.TRADITIONAL
        else -> DateFormatter.Lang.CHINESE
    }

    @SuppressLint("AppBundleLocaleChanges")
    @JvmStatic
    fun wrap(context: Context?): Context? {
        if (context == null) return null
        val locale = resolveLocale(context)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}
