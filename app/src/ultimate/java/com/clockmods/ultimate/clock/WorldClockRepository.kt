package com.clockmods.ultimate.clock

import android.content.Context
import android.content.SharedPreferences
import com.clockmods.sdk.clock.WorldClockEntry
import org.json.JSONArray
import java.util.LinkedHashSet

/** Persists the world-clock feature state and its ordered city selection. */
class WorldClockRepository {
    private val preferences: SharedPreferences

    constructor(context: Context) {
        requireNotNull(context) { "context must not be null" }
        preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    /** Constructor retained for JVM tests and isolated preference stores. */
    constructor(preferences: SharedPreferences) {
        this.preferences = requireNotNull(preferences) { "preferences required" }
    }

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getSelected(): List<WorldClockEntry> {
        val encoded = preferences.getString(KEY_CITIES, null)
            ?: return ArrayList(WorldClockCatalog.defaults())
        return try {
            decode(encoded)
        } catch (_: Exception) {
            ArrayList(WorldClockCatalog.defaults())
        }
    }

    fun save(entries: List<WorldClockEntry?>?) {
        preferences.edit().putString(KEY_CITIES, encode(entries)).apply()
    }

    fun restoreDefaults() {
        preferences.edit().clear().apply()
    }

    companion object {
        @JvmField val PREFERENCES_NAME = "clockmods_world_clock"
        @JvmField val DEFAULT_ENABLED = false
        const val MAX_SELECTED = 6
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CITIES = "cities"

        @JvmStatic
        fun encode(entries: List<WorldClockEntry?>?): String {
            val array = JSONArray()
            val seen = LinkedHashSet<String>()
            for (entry in entries.orEmpty()) {
                if (entry == null) continue
                if (seen.add(entry.getId())) array.put(entry.getId())
                if (array.length() == MAX_SELECTED) break
            }
            return array.toString()
        }

        @JvmStatic
        @Throws(Exception::class)
        fun decode(encoded: String): List<WorldClockEntry> {
            val array = JSONArray(encoded)
            val result = ArrayList<WorldClockEntry>()
            val seen = LinkedHashSet<String>()
            for (index in 0 until array.length()) {
                val entry = WorldClockCatalog.find(array.optString(index, ""))
                if (entry != null && seen.add(entry.getId())) result.add(entry)
                if (result.size == MAX_SELECTED) break
            }
            return result
        }
    }
}
