package com.clockmods.ultimate.clock

import android.content.Context
import android.content.SharedPreferences
import com.clockmods.background.BackgroundRepository
import com.clockmods.background.ClockPreferences
import com.clockmods.sdk.clock.ClockState
import java.util.Locale

/** Persistence owned by the Ultimate clock host, deliberately separate from style renderers. */
class UltimateClockPreferences {
    private val preferences: SharedPreferences

    constructor(context: Context) {
        requireNotNull(context) { "context must not be null" }
        preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        migrateLegacyImageBackground(context)
    }

    /** Constructor retained for JVM tests and callers that provide an isolated preference store. */
    constructor(preferences: SharedPreferences) {
        this.preferences = requireNotNull(preferences) { "preferences must not be null" }
    }

    private fun migrateLegacyImageBackground(context: Context) {
        if (preferences.contains(KEY_BACKGROUND_MODE)) return
        val legacy = BackgroundRepository(context)
        val migratedMode = if (ClockPreferences.MODE_IMAGE == legacy.getBackgroundMode() && legacy.hasImage()) {
            BACKGROUND_MODE_IMAGE
        } else {
            BACKGROUND_MODE_THEME
        }
        preferences.edit().putString(KEY_BACKGROUND_MODE, migratedMode).apply()
    }

    fun getStyleId(): String = normalizeStyleId(preferences.getString(KEY_STYLE_ID, DEFAULT_STYLE_ID))

    fun setStyleId(styleId: String?) {
        preferences.edit().putString(KEY_STYLE_ID, normalizeStyleId(styleId)).apply()
    }

    fun getSecondHandMotion(): ClockState.SecondHandMotion {
        val stored = preferences.getString(KEY_SECOND_HAND_MOTION, "smooth")
        return when (stored?.trim()?.lowercase(Locale.US)) {
            "smooth", "sweep" -> ClockState.SecondHandMotion.SWEEP
            "tick", "ticking" -> ClockState.SecondHandMotion.TICK
            "off" -> ClockState.SecondHandMotion.OFF
            else -> DEFAULT_SECOND_HAND_MOTION
        }
    }

    fun setSecondHandMotion(motion: ClockState.SecondHandMotion?) {
        val safe = motion ?: DEFAULT_SECOND_HAND_MOTION
        val stored = when (safe) {
            ClockState.SecondHandMotion.SWEEP -> "smooth"
            ClockState.SecondHandMotion.TICK -> "tick"
            ClockState.SecondHandMotion.OFF -> "off"
        }
        preferences.edit().putString(KEY_SECOND_HAND_MOTION, stored).apply()
    }

    fun getBackgroundMode(): String = normalizeBackgroundMode(
        preferences.getString(KEY_BACKGROUND_MODE, DEFAULT_BACKGROUND_MODE)
    )

    fun setBackgroundMode(mode: String?) {
        preferences.edit().putString(KEY_BACKGROUND_MODE, normalizeBackgroundMode(mode)).apply()
    }

    fun getPalette(styleId: String?): ClockPalette {
        val defaults = ClockPalette.DEFAULT
        if (!ClockPalette.supports(styleId)) return defaults
        return ClockPalette(
            preferences.getInt("palette_background__$styleId", defaults.background),
            preferences.getInt("palette_panel__$styleId", defaults.panel),
            preferences.getInt("palette_accent__$styleId", defaults.accent),
            preferences.getBoolean("palette_blur__$styleId", false),
            preferences.getInt("palette_blur_strength__$styleId", defaults.blurStrength),
            preferences.getInt("palette_blur_brightness__$styleId", defaults.blurBrightness),
        ).withCardShadow(preferences.getBoolean("palette_card_shadow__$styleId", true))
    }

    fun setPalette(styleId: String?, palette: ClockPalette?) {
        if (!ClockPalette.supports(styleId) || palette == null) return
        preferences.edit()
            .putInt("palette_background__$styleId", palette.background)
            .putInt("palette_panel__$styleId", palette.panel)
            .putInt("palette_accent__$styleId", palette.accent)
            .putBoolean("palette_blur__$styleId", palette.gaussianBlur)
            .putInt("palette_blur_strength__$styleId", palette.blurStrength)
            .putInt("palette_blur_brightness__$styleId", palette.blurBrightness)
            .putBoolean("palette_card_shadow__$styleId", palette.cardShadow)
            .apply()
    }

    fun restoreDefaults() {
        val editor = preferences.edit()
        preferences.all.keys.filter { it.startsWith("palette_") }.forEach(editor::remove)
        editor.putString(KEY_STYLE_ID, DEFAULT_STYLE_ID)
            .putString(KEY_SECOND_HAND_MOTION, "smooth")
            .putString(KEY_BACKGROUND_MODE, DEFAULT_BACKGROUND_MODE)
            .apply()
    }

    companion object {
        @JvmField val PREFERENCES_NAME = "clockmods_ultimate_style"
        @JvmField val KEY_STYLE_ID = "style_id"
        @JvmField val KEY_SECOND_MOTION = "second_motion"
        @JvmField val KEY_BACKGROUND_MODE = "background_mode"
        @JvmField val BACKGROUND_MODE_THEME = "theme"
        @JvmField val BACKGROUND_MODE_COLOR = "color"
        @JvmField val BACKGROUND_MODE_IMAGE = "image"
        @JvmField val DEFAULT_STYLE_ID = UltimateClockStyles.STYLE_GLASS_ATELIER
        @JvmField val DEFAULT_SECOND_HAND_MOTION = ClockState.SecondHandMotion.SWEEP
        @JvmField val DEFAULT_BACKGROUND_MODE = BACKGROUND_MODE_THEME

        private const val KEY_SECOND_HAND_MOTION = "second_motion"

        @JvmStatic
        fun normalizeStyleId(styleId: String?): String {
            if (styleId == null || styleId.trim().isEmpty()) return DEFAULT_STYLE_ID
            val trimmed = styleId.trim()
            return if (trimmed.length > 120) DEFAULT_STYLE_ID else trimmed
        }

        @JvmStatic
        fun normalizeBackgroundMode(mode: String?): String = when (mode) {
            BACKGROUND_MODE_COLOR -> BACKGROUND_MODE_COLOR
            BACKGROUND_MODE_IMAGE -> BACKGROUND_MODE_IMAGE
            else -> BACKGROUND_MODE_THEME
        }
    }
}
