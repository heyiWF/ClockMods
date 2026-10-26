package com.clockmods.background;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Installed membership is independent of the bundled renderer catalog. */
public final class ThemeLibrary {
    public static final String ORDER = "theme_library_order";
    private static final String FAVORITES = "theme_library_favorites";
    private final SharedPreferences store;
    private final SharedPreferences typography;
    private final boolean calendar;
    private final List<String> catalog;

    public ThemeLibrary(Context context, boolean calendar, List<String> catalog) {
        this(context.getSharedPreferences(calendar ? "clock_prefs" : "clockmods_ultimate_style", 0),
                context.getSharedPreferences("clock_prefs", 0), calendar, catalog);
    }

    public ThemeLibrary(SharedPreferences store, SharedPreferences typography,
            boolean calendar, List<String> catalog) {
        if (catalog.isEmpty() || new HashSet<>(catalog).size() != catalog.size())
            throw new IllegalArgumentException("Invalid theme catalog");
        this.store = store;
        this.typography = typography;
        this.calendar = calendar;
        this.catalog = Collections.unmodifiableList(new ArrayList<>(catalog));
        if (!calendar && store.contains("second_motion")) {
            SharedPreferences.Editor editor = store.edit();
            String legacy = store.getString("second_motion", "smooth");
            for (String id : catalog) if (!store.contains("second_motion__" + id))
                editor.putString("second_motion__" + id, legacy);
            editor.remove("second_motion").apply();
        }
    }

    public List<String> catalog() { return catalog; }

    public List<String> installed() {
        if (!store.contains(ORDER)) return new ArrayList<>(catalog);
        List<String> result = new ArrayList<>();
        for (String id : decode(store.getString(ORDER, "")))
            if (catalog.contains(id) && !result.contains(id)) result.add(id);
        if (result.isEmpty()) result.add(catalog.get(0));
        return result;
    }

    public boolean isInstalled(String id) { return installed().contains(id); }
    public boolean isFavorite(String id) {
        return store.getStringSet(FAVORITES, Collections.emptySet()).contains(id);
    }

    public void favorite(String id) {
        requireKnown(id);
        Set<String> favorites = new HashSet<>(store.getStringSet(FAVORITES, Collections.emptySet()));
        if (!favorites.remove(id)) favorites.add(id);
        store.edit().putStringSet(FAVORITES, favorites).apply();
    }

    public String selected() {
        String fallback = calendar ? "calendar.graphite" : "glass.atelier";
        String value = store.getString(calendar ? "calendar_theme" : "style_id", fallback);
        return isInstalled(value) ? value : installed().get(0);
    }

    public void select(String id) {
        if (!isInstalled(id)) throw new IllegalArgumentException("Theme is not installed");
        store.edit().putString(calendar ? "calendar_theme" : "style_id", id).apply();
    }

    public void install(String id) {
        requireKnown(id);
        List<String> ids = installed();
        if (ids.contains(id)) return;
        clearTypography(id);
        SharedPreferences.Editor editor = store.edit();
        clearStyle(editor, id);
        String scope = calendar ? "calendar:" + id : id;
        boolean bold = Arrays.asList("ultimate.dual_blocks", "ultimate.orbit", "ultimate.bubbles",
                "ultimate.blend", "ultimate.ribbon").contains(id);
        typography.edit().putString("font_family__" + scope, ClockPreferences.FONT_SYSTEM)
                .putInt("font_weight__" + scope, bold ? 700 : 400)
                .putFloat("time_font_scale__" + scope, 0f)
                .putFloat("date_font_scale__" + scope, 0f)
                .putFloat("supporting_font_scale__" + scope, 0f).apply();
        ids.add(id);
        editor.putString(ORDER, encode(ids)).apply();
    }

    /** Returns false for the last installed theme, without changing any settings. */
    public boolean remove(String id) {
        List<String> ids = installed();
        if (!ids.contains(id)) return true;
        if (ids.size() == 1) return false;
        String selected = selected();
        int index = ids.indexOf(id);
        ids.remove(id);
        clearTypography(id);
        SharedPreferences.Editor editor = store.edit();
        clearStyle(editor, id);
        if (selected.equals(id)) editor.putString(calendar ? "calendar_theme" : "style_id",
                ids.get(Math.min(index, ids.size() - 1)));
        editor.putString(ORDER, encode(ids)).apply();
        return true;
    }

    /** Used by render hosts too, including after a global settings reset. */
    public static String resolveSelection(SharedPreferences preferences, String requested) {
        if (!preferences.contains(ORDER)) return requested;
        List<String> ids = decode(preferences.getString(ORDER, ""));
        return ids.isEmpty() || ids.contains(requested) ? requested : ids.get(0);
    }

    private void clearTypography(String id) {
        String scope = calendar ? "calendar:" + id : id;
        SharedPreferences.Editor editor = typography.edit();
        for (String key : typography.getAll().keySet()) {
            if (isTypographyKey(key, scope)) editor.remove(key);
        }
        // Pro Classic owns the original (unscoped) face controls.
        if (!calendar && "pro.classic".equals(id)) {
            for (String key : Arrays.asList("time_font_scale", "date_font_scale", "time_color",
                    "date_color", "blink_colon", "animate_time_changes", "time_transition",
                    "weather_transition", "small_seconds", "portrait_stacked", "date_lunar_dual_line"))
                editor.remove(key);
        }
        editor.apply();
    }

    static boolean isTypographyKey(String key, String scope) {
        for (String prefix : Arrays.asList("font_family__", "font_weight__", "time_font_scale__",
                "date_font_scale__", "supporting_font_scale__"))
            if (key.equals(prefix + scope)) return true;
        return key.startsWith("script_font__" + scope + "__")
                || key.startsWith("calendar_marquee__" + scope + "_");
    }

    private void clearStyle(SharedPreferences.Editor editor, String id) {
        if (calendar) return;
        for (String key : store.getAll().keySet()) {
            if (key.endsWith("__" + id) && (key.startsWith("palette_")
                    || key.startsWith("digit_animation__") || key.startsWith("digit_transition__")
                    || key.startsWith("weather_transition__") || key.startsWith("second_motion__"))) editor.remove(key);
        }
    }

    private void requireKnown(String id) {
        if (!catalog.contains(id)) throw new IllegalArgumentException("Unknown theme");
    }
    private static List<String> decode(String value) {
        return value == null || value.isEmpty() ? new ArrayList<>()
                : new ArrayList<>(new LinkedHashSet<>(Arrays.asList(value.split("\\|"))));
    }
    private static String encode(List<String> ids) { return String.join("|", ids); }
}
