package com.clockmods.calendar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections

class HolidayRepository(context: Context) {
    class HolidayStatus(
        @JvmField val name: String,
        @JvmField val date: String,
        @JvmField val offDay: Boolean,
    )

    private val statuses: Map<String, HolidayStatus>

    init {
        val loaded = HashMap<String, HolidayStatus>()
        for (year in 2025..2027) {
            try {
                context.assets.open("holidays/$year.json").use { input ->
                    loaded.putAll(parse(read(input)))
                }
            } catch (_: Exception) {
                // Optional year asset.
            }
        }
        statuses = Collections.unmodifiableMap(loaded)
    }

    fun statusOn(date: String): HolidayStatus? = statuses[date]

    companion object {
        @JvmStatic
        @Throws(Exception::class)
        fun parse(value: String): Map<String, HolidayStatus> {
            val days = JSONObject(value).getJSONArray("days")
            val output = HashMap<String, HolidayStatus>()
            for (index in 0 until days.length()) {
                val day = days.getJSONObject(index)
                val name = day.getString("name").trim()
                val date = day.getString("date").trim()
                if (name.isEmpty() || !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) continue
                output[date] = HolidayStatus(name, date, day.getBoolean("isOffDay"))
            }
            return output
        }

        @Throws(Exception::class)
        private fun read(input: InputStream): String =
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
    }
}
