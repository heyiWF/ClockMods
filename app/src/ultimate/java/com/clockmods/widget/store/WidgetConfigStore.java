package com.clockmods.widget.store;
import android.content.Context;
import android.content.SharedPreferences;
import com.clockmods.widget.model.*;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
/** JSON codec is also usable without Android, for migrations and external preset tooling. */
public final class WidgetConfigStore {
    private final SharedPreferences prefs;
    public WidgetConfigStore(Context context) { this(context.getSharedPreferences("clockmods_widgets", Context.MODE_PRIVATE)); }
    public WidgetConfigStore(SharedPreferences prefs) { this.prefs = prefs; }
    public WidgetConfig getOrDefault(int id, WidgetKind kind) {
        String raw = prefs.getString("widget_" + id, null);
        WidgetConfig c = decode(raw, id, kind);
        if (raw != null && !encode(c).equals(raw)) save(c);
        return c;
    }
    public void save(WidgetConfig c) { prefs.edit().putString("widget_" + c.appWidgetId, encode(c)).apply(); }
    public void delete(int id) { prefs.edit().remove("widget_" + id).apply(); }
    public boolean contains(int id) { return prefs.contains("widget_" + id); }
    public List<WidgetConfig> getAll() {
        List<WidgetConfig> out = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith("widget_")) continue;
            try {
                int id = Integer.parseInt(key.substring(7));
                String raw = prefs.getString(key, null);
                WidgetKind kind = WidgetKind.DIGITAL;
                try { kind = WidgetKind.valueOf(new JSONObject(raw).optString("kind")); } catch (Exception ignored) { }
                out.add(decode(raw, id, kind));
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }
    public List<Integer> getIdsUsingWeather() {
        List<Integer> out = new ArrayList<>();
        for (WidgetConfig c : getAll()) if (c.kind == WidgetKind.WEATHER) out.add(c.appWidgetId);
        return out;
    }
    public static String encode(WidgetConfig c) {
        try {
            JSONObject j = new JSONObject().put("schemaVersion", c.schemaVersion);
            j.put("appWidgetId", c.appWidgetId);
            j.put("kind", c.kind.name());
            j.put("themeId", c.themeId);
            j.put("timeZoneId", c.timeZoneId);
            j.put("useSystemTimeZone", c.useSystemTimeZone);
            j.put("useSystemTimeFormat", c.useSystemTimeFormat);
            j.put("use24Hour", c.use24Hour);
            j.put("showSeconds", c.showSeconds);
            j.put("showDate", c.showDate);
            j.put("showWeekday", c.showWeekday);
            j.put("showLunar", c.showLunar);
            j.put("showWeatherDescription", c.showWeatherDescription);
            j.put("showLocation", c.showLocation);
            j.put("backgroundAlpha", c.backgroundAlpha);
            j.put("textScale", c.textScale);
            j.put("tapAction", c.tapAction);
            j.put("updatedAt", c.updatedAt);
            j.put("darkText", c.darkText);
            j.put("fontId", c.fontId);
            return j.toString();
        } catch (org.json.JSONException e) { throw new IllegalArgumentException(e); }
    }
    public static WidgetConfig decode(String raw, int id, WidgetKind kind) {
        WidgetConfig.Builder b = WidgetConfig.builder(id, kind);
        WidgetConfig d = b.build();
        if (raw == null) return d;
        try {
            JSONObject j = new JSONObject(raw);
            j = migrate(j, j.optInt("schemaVersion", 0));
            b.themeId(j.optString("themeId", d.themeId));
            b.timeZoneId(j.optString("timeZoneId", d.timeZoneId));
            b.useSystemTimeZone(j.optBoolean("useSystemTimeZone", d.useSystemTimeZone));
            b.useSystemTimeFormat(j.optBoolean("useSystemTimeFormat", d.useSystemTimeFormat));
            b.use24Hour(j.optBoolean("use24Hour", d.use24Hour));
            b.showSeconds(j.optBoolean("showSeconds", d.showSeconds));
            b.showDate(j.optBoolean("showDate", d.showDate));
            b.showWeekday(j.optBoolean("showWeekday", d.showWeekday));
            b.showLunar(j.optBoolean("showLunar", d.showLunar));
            b.showWeatherDescription(j.optBoolean("showWeatherDescription", d.showWeatherDescription));
            b.showLocation(j.optBoolean("showLocation", d.showLocation));
            b.backgroundAlpha(j.optInt("backgroundAlpha", d.backgroundAlpha));
            b.textScale((float) j.optDouble("textScale", d.textScale));
            b.tapAction(j.optString("tapAction", d.tapAction));
            b.updatedAt(j.optLong("updatedAt", d.updatedAt));
            b.darkText(j.optBoolean("darkText", d.darkText));
            b.fontId(j.optString("fontId", d.fontId));
            return b.build();
        } catch (Exception ignored) { return d; }
    }
    private static JSONObject migrate(JSONObject source, int fromVersion) throws org.json.JSONException {
        if (fromVersion > 1) throw new IllegalArgumentException("Unsupported widget schema");
        if (fromVersion == 0) source.put("schemaVersion", 1);
        return source;
    }
}
