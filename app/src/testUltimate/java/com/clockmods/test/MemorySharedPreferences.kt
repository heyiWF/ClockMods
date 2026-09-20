package com.clockmods.test

import android.content.SharedPreferences

internal class MemorySharedPreferences : SharedPreferences {
    private val values = HashMap<String, Any?>()

    override fun getAll(): Map<String, *> = HashMap(values)
    override fun getString(key: String?, defaultValue: String?): String? =
        values[key] as? String ?: defaultValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defaultValues: Set<String>?): Set<String>? =
        (values[key] as? Set<String>)?.toHashSet() ?: defaultValues

    override fun getInt(key: String?, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
    override fun getLong(key: String?, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
    override fun getFloat(key: String?, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
    override fun getBoolean(key: String?, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = MemoryEditor()
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class MemoryEditor : SharedPreferences.Editor {
        private val updates = HashMap<String, Any?>()
        private val removals = HashSet<String>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = put(key, value)
        override fun putStringSet(key: String?, value: Set<String>?): SharedPreferences.Editor =
            put(key, value?.toHashSet())

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = put(key, value)
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = put(key, value)
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = put(key, value)
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = put(key, value)

        private fun put(key: String?, value: Any?): SharedPreferences.Editor {
            if (key != null) updates[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removals += key
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) values.clear()
            removals.forEach(values::remove)
            updates.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
        }
    }
}
