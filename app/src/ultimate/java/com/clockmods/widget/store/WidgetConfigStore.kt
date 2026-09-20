package com.clockmods.widget.store

import android.content.Context
import android.content.SharedPreferences
import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import org.json.JSONObject

/** JSON codec is also usable without Android, for migrations and external preset tooling. */
class WidgetConfigStore {
    private val prefs: SharedPreferences

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    constructor(prefs: SharedPreferences) {
        this.prefs = prefs
    }

    fun getOrDefault(id: Int, kind: WidgetKind): WidgetConfig {
        val raw = prefs.getString(key(id), null)
        val config = decode(raw, id, kind)
        if (raw != null && encode(config) != raw) save(config)
        return config
    }

    fun save(config: WidgetConfig) {
        prefs.edit().putString(key(config.appWidgetId), encode(config)).apply()
    }

    fun delete(id: Int) {
        prefs.edit().remove(key(id)).apply()
    }

    fun contains(id: Int): Boolean = prefs.contains(key(id))

    fun getAll(): List<WidgetConfig> {
        val out = ArrayList<WidgetConfig>()
        for (entryKey in prefs.all.keys) {
            if (!entryKey.startsWith(KEY_PREFIX)) continue
            try {
                val id = entryKey.substring(KEY_PREFIX.length).toInt()
                val raw = prefs.getString(entryKey, null)
                val kind = try {
                    WidgetKind.valueOf(JSONObject(raw ?: "").optString("kind"))
                } catch (_: Exception) {
                    WidgetKind.DIGITAL
                }
                out += decode(raw, id, kind)
            } catch (_: NumberFormatException) {
                // Ignore unrelated or damaged preference keys, matching the legacy behavior.
            }
        }
        return out
    }

    fun getIdsUsingWeather(): List<Int> = getAll()
        .filter { it.kind == WidgetKind.WEATHER }
        .map { it.appWidgetId }

    companion object {
        private const val PREFS_NAME = "clockmods_widgets"
        private const val KEY_PREFIX = "widget_"

        private fun key(id: Int): String = KEY_PREFIX + id

        @JvmStatic fun encode(config: WidgetConfig): String = JSONObject()
            .put("schemaVersion", config.schemaVersion)
            .put("appWidgetId", config.appWidgetId)
            .put("kind", config.kind.name)
            .put("themeId", config.themeId)
            .put("timeZoneId", config.timeZoneId)
            .put("useSystemTimeZone", config.useSystemTimeZone)
            .put("useSystemTimeFormat", config.useSystemTimeFormat)
            .put("use24Hour", config.use24Hour)
            .put("showSeconds", config.showSeconds)
            .put("showDate", config.showDate)
            .put("showWeekday", config.showWeekday)
            .put("showLunar", config.showLunar)
            .put("showWeatherDescription", config.showWeatherDescription)
            .put("showLocation", config.showLocation)
            .put("backgroundAlpha", config.backgroundAlpha)
            .put("textScale", config.textScale)
            .put("tapAction", config.tapAction)
            .put("updatedAt", config.updatedAt)
            .put("darkText", config.darkText)
            .put("fontId", config.fontId)
            .toString()

        @JvmStatic fun decode(raw: String?, id: Int, kind: WidgetKind): WidgetConfig {
            val builder = WidgetConfig.builder(id, kind)
            val defaults = builder.build()
            if (raw == null) return defaults
            return try {
                val source = JSONObject(raw)
                val json = migrate(source, source.optInt("schemaVersion", 0))
                builder
                    .themeId(json.optString("themeId", defaults.themeId))
                    .timeZoneId(json.optString("timeZoneId", defaults.timeZoneId))
                    .useSystemTimeZone(json.optBoolean("useSystemTimeZone", defaults.useSystemTimeZone))
                    .useSystemTimeFormat(json.optBoolean("useSystemTimeFormat", defaults.useSystemTimeFormat))
                    .use24Hour(json.optBoolean("use24Hour", defaults.use24Hour))
                    .showSeconds(json.optBoolean("showSeconds", defaults.showSeconds))
                    .showDate(json.optBoolean("showDate", defaults.showDate))
                    .showWeekday(json.optBoolean("showWeekday", defaults.showWeekday))
                    .showLunar(json.optBoolean("showLunar", defaults.showLunar))
                    .showWeatherDescription(json.optBoolean("showWeatherDescription", defaults.showWeatherDescription))
                    .showLocation(json.optBoolean("showLocation", defaults.showLocation))
                    .backgroundAlpha(json.optInt("backgroundAlpha", defaults.backgroundAlpha))
                    .textScale(json.optDouble("textScale", defaults.textScale.toDouble()).toFloat())
                    .tapAction(json.optString("tapAction", defaults.tapAction))
                    .updatedAt(json.optLong("updatedAt", defaults.updatedAt))
                    .darkText(json.optBoolean("darkText", defaults.darkText))
                    .fontId(json.optString("fontId", defaults.fontId))
                    .build()
            } catch (_: Exception) {
                defaults
            }
        }

        private fun migrate(source: JSONObject, fromVersion: Int): JSONObject {
            require(fromVersion <= 1) { "Unsupported widget schema" }
            if (fromVersion == 0) source.put("schemaVersion", 1)
            return source
        }
    }
}
