package com.clockmods.pro.alarm

import android.content.Context
import android.content.SharedPreferences

class AlarmStore(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("pro_alarm", Context.MODE_PRIVATE)

    fun hour(): Int = preferences.getInt("hour", 7)
    fun minute(): Int = preferences.getInt("minute", 30)
    fun enabled(): Boolean = preferences.getBoolean("enabled", false)

    /**
     * True while an alarm is actively ringing.  Persisted so the ringing surface can be rebuilt
     * after the process is killed (low memory) or the device reboots mid-ring, which would
     * otherwise leave the user with a ringing alarm and no way to stop it.
     */
    fun isRinging(): Boolean = preferences.getBoolean("ringing", false)

    fun setRinging(ringing: Boolean) {
        preferences.edit().putBoolean("ringing", ringing).apply()
    }

    fun save(hour: Int, minute: Int, enabled: Boolean) {
        preferences.edit()
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putBoolean("enabled", enabled)
            .apply()
    }

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("enabled", enabled).apply()
    }
}
