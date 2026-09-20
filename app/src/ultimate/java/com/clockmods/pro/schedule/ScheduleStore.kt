package com.clockmods.pro.schedule

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/** Daily schedule persistence, keyed by civil date. */
class ScheduleStore(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun itemsFor(year: Int, month0: Int, dayOfMonth: Int): MutableList<ScheduleItem> {
        val stored = preferences.getString(keyFor(year, month0, dayOfMonth), null)
        if (stored.isNullOrEmpty()) return ArrayList()
        return decode(stored).apply { sortBy(ScheduleItem::sortKey) }
    }

    fun hasItems(year: Int, month0: Int, dayOfMonth: Int): Boolean =
        preferences.contains(keyFor(year, month0, dayOfMonth))

    fun save(
        year: Int,
        month0: Int,
        dayOfMonth: Int,
        id: String?,
        title: String?,
        hour: Int,
        minute: Int,
    ): Boolean {
        val key = keyFor(year, month0, dayOfMonth)
        val existing = itemsFor(year, month0, dayOfMonth)
        val targetId = id?.takeIf { it.isNotEmpty() } ?: newId()
        val replacement = ScheduleItem(targetId, title, hour, minute)
        val index = existing.indexOfFirst { it.id == targetId }
        if (!canSave(existing.size, replacingExisting = index >= 0)) return false
        if (index >= 0) {
            existing[index] = replacement
        } else {
            existing.add(replacement)
        }
        write(key, existing)
        return true
    }

    fun remove(year: Int, month0: Int, dayOfMonth: Int, id: String?) {
        val key = keyFor(year, month0, dayOfMonth)
        val existing = itemsFor(year, month0, dayOfMonth)
        if (existing.removeAll { it.id == id }) {
            write(key, existing)
        }
    }

    private fun write(key: String, items: List<ScheduleItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            try {
                obj.put(FIELD_ID, item.id)
                obj.put(FIELD_TITLE, item.title)
                obj.put(FIELD_HOUR, item.hour)
                obj.put(FIELD_MINUTE, item.minute)
            } catch (_: JSONException) {
                continue
            }
            array.put(obj)
        }
        preferences.edit().putString(key, array.toString()).apply()
    }

    companion object {
        const val MAX_ITEMS_PER_DAY = 24

        private const val PREFS_NAME = "pro_schedule"
        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_HOUR = "hour"
        private const val FIELD_MINUTE = "minute"

        @JvmStatic
        fun keyFor(year: Int, month0: Int, dayOfMonth: Int): String =
            String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, dayOfMonth)

        internal fun canSave(itemCount: Int, replacingExisting: Boolean): Boolean =
            replacingExisting || itemCount < MAX_ITEMS_PER_DAY

        private fun newId(): String =
            java.lang.Long.toHexString(System.currentTimeMillis()) + "_" +
                java.lang.Long.toHexString(System.nanoTime())

        private fun decode(stored: String): MutableList<ScheduleItem> {
            val items = ArrayList<ScheduleItem>()
            try {
                val array = JSONArray(stored)
                var index = 0
                while (index < array.length() && items.size < MAX_ITEMS_PER_DAY) {
                    val obj = array.optJSONObject(index++) ?: continue
                    var id = obj.optString(FIELD_ID, "")
                    val title = obj.optString(FIELD_TITLE, "")
                    val hour = obj.optInt(FIELD_HOUR, ScheduleItem.TIME_NONE)
                    val minute = obj.optInt(FIELD_MINUTE, ScheduleItem.TIME_NONE)
                    if (id.isEmpty()) id = newId()
                    items.add(ScheduleItem(id, title, hour, minute))
                }
            } catch (_: JSONException) {
                return ArrayList()
            }
            return items
        }
    }
}
