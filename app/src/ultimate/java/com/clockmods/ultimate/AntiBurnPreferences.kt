package com.clockmods.ultimate

import android.content.Context
import android.content.SharedPreferences

/** Global anti-burn settings shared by every Ultimate destination. */
class AntiBurnPreferences {
    private val preferences: SharedPreferences

    constructor(context: Context?) {
        preferences = requireNotNull(context) {
            "context must not be null"
        }.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    /** Constructor retained for JVM tests and isolated preference stores. */
    internal constructor(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)

    fun setEnabled(value: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun getPeriodMinutes(): Int = normalizePeriod(
        preferences.getInt(KEY_PERIOD, DEFAULT_PERIOD_MINUTES),
    )

    fun setPeriodMinutes(value: Int) {
        preferences.edit().putInt(KEY_PERIOD, normalizePeriod(value)).apply()
    }

    fun getAmplitudeDp(): Float = normalizeAmplitude(
        preferences.getFloat(KEY_AMPLITUDE, DEFAULT_AMPLITUDE_DP),
    )

    fun setAmplitudeDp(value: Float) {
        preferences.edit().putFloat(KEY_AMPLITUDE, normalizeAmplitude(value)).apply()
    }

    fun isAutoDim(): Boolean = preferences.getBoolean(KEY_AUTO_DIM, DEFAULT_AUTO_DIM)

    fun setAutoDim(value: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_DIM, value).apply()
    }

    fun restoreDefaults() {
        preferences.edit().clear().apply()
    }

    companion object {
        const val PREFERENCES_NAME = "clockmods_anti_burn"
        const val DEFAULT_ENABLED = false
        const val DEFAULT_PERIOD_MINUTES = 10
        const val DEFAULT_AMPLITUDE_DP = 4f
        const val DEFAULT_AUTO_DIM = true

        private const val KEY_ENABLED = "enabled"
        private const val KEY_PERIOD = "period_minutes"
        private const val KEY_AMPLITUDE = "amplitude_dp"
        private const val KEY_AUTO_DIM = "auto_dim"

        @JvmStatic
        fun normalizePeriod(value: Int): Int = when (value) {
            1, 30, 60 -> value
            else -> DEFAULT_PERIOD_MINUTES
        }

        @JvmStatic
        fun normalizeAmplitude(value: Float): Float =
            if (!value.isFinite()) DEFAULT_AMPLITUDE_DP else value.coerceIn(0f, 12f)
    }
}
