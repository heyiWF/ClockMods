package com.clockmods.widget.model;
import com.clockmods.background.FontCatalog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
/** Immutable, normalized per-instance contract. Persisted IDs must remain stable. */
public final class WidgetConfig {
    public static final String[] THEME_IDS = {"system.dynamic", "glass.light", "instrument.dark", "paper.warm", "neon.night", "transparent.clean"};
    /** Follow the selected theme's font structure — the default. */
    public static final String FONT_THEME = "theme";
    /**
     * Stable, persisted font ids: the follow-theme mode first, then exactly the families the app
     * offers, in the app's own order. Derived from {@link FontCatalog} on purpose so the widget can
     * never drift away from the list the app shows.
     */
    public static final String[] FONT_IDS = buildFontIds();
    private static final Set<String> ZONES = new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));
    private static final Set<String> FONTS = new HashSet<>(Arrays.asList(FONT_IDS));

    private static String[] buildFontIds() {
        List<FontCatalog.FontOption> families = FontCatalog.options();
        String[] ids = new String[families.size() + 1];
        ids[0] = FONT_THEME;
        for (int i = 0; i < families.size(); i++) ids[i + 1] = families.get(i).id;
        return ids;
    }
    public final int schemaVersion = 1;
    public final int appWidgetId;
    public final WidgetKind kind;
    public final String themeId;
    public final String timeZoneId;
    public final boolean useSystemTimeZone;
    public final boolean useSystemTimeFormat;
    public final boolean use24Hour;
    public final boolean showSeconds;
    public final boolean showDate;
    public final boolean showWeekday;
    public final boolean showLunar;
    public final boolean showWeatherDescription;
    public final boolean showLocation;
    public final int backgroundAlpha;
    public final float textScale;
    public final String tapAction;
    public final long updatedAt;
    public final boolean darkText;
    public final String fontId;
    private WidgetConfig(Builder b) {
        appWidgetId = b.appWidgetId;
        kind = b.kind == null ? WidgetKind.DIGITAL : b.kind;
        themeId = Arrays.asList(THEME_IDS).contains(b.themeId) ? b.themeId : THEME_IDS[0];
        boolean validZone = ZONES.contains(b.timeZoneId);
        timeZoneId = validZone ? b.timeZoneId : TimeZone.getDefault().getID();
        useSystemTimeZone = b.useSystemTimeZone || !validZone;
        useSystemTimeFormat = b.useSystemTimeFormat;
        use24Hour = b.use24Hour;
        showSeconds = b.showSeconds && (kind == WidgetKind.DIGITAL || kind == WidgetKind.WEATHER);
        showDate = b.showDate; showWeekday = b.showWeekday; showLunar = b.showLunar;
        showWeatherDescription = b.showWeatherDescription; showLocation = b.showLocation;
        backgroundAlpha = Math.max(0, Math.min(255, b.backgroundAlpha));
        textScale = Float.isNaN(b.textScale) ? 1f : Math.max(.85f, Math.min(1.2f, b.textScale));
        tapAction = Arrays.asList("open_clock", "open_calendar", "open_weather", "open_config").contains(b.tapAction) ? b.tapAction : "open_clock";
        updatedAt = Math.max(0, b.updatedAt); darkText = b.darkText;
        fontId = FONTS.contains(b.fontId) ? b.fontId : FONT_THEME;
    }
    public TimeZone zone() { return useSystemTimeZone ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneId); }
    public Builder toBuilder() { return new Builder(this); }
    public static Builder builder(int id, WidgetKind kind) { return new Builder(id, kind); }
    public static final class Builder {
        private int appWidgetId;
        private WidgetKind kind;
        private String themeId = "system.dynamic";
        private String timeZoneId = TimeZone.getDefault().getID();
        private boolean useSystemTimeZone = true;
        private boolean useSystemTimeFormat = true;
        private boolean use24Hour = true;
        private boolean showSeconds;
        private boolean showDate = true;
        private boolean showWeekday = true;
        private boolean showLunar = true;
        private boolean showWeatherDescription = true;
        private boolean showLocation = true;
        private int backgroundAlpha = 255;
        private float textScale = 1f;
        private String tapAction = "open_clock";
        private long updatedAt = System.currentTimeMillis();
        private boolean darkText;
        private String fontId = FONT_THEME;
        private Builder(int id, WidgetKind kind) { this.appWidgetId = id; this.kind = kind; }
        private Builder(WidgetConfig c) {
            appWidgetId = c.appWidgetId;
            kind = c.kind;
            themeId = c.themeId;
            timeZoneId = c.timeZoneId;
            useSystemTimeZone = c.useSystemTimeZone;
            useSystemTimeFormat = c.useSystemTimeFormat;
            use24Hour = c.use24Hour;
            showSeconds = c.showSeconds;
            showDate = c.showDate;
            showWeekday = c.showWeekday;
            showLunar = c.showLunar;
            showWeatherDescription = c.showWeatherDescription;
            showLocation = c.showLocation;
            backgroundAlpha = c.backgroundAlpha;
            textScale = c.textScale;
            tapAction = c.tapAction;
            updatedAt = c.updatedAt;
            darkText = c.darkText;
            fontId = c.fontId;
        }
        public Builder appWidgetId(int value) { appWidgetId = value; return this; }
        public Builder kind(WidgetKind value) { kind = value; return this; }
        public Builder themeId(String value) { themeId = value; return this; }
        public Builder timeZoneId(String value) { timeZoneId = value; return this; }
        public Builder useSystemTimeZone(boolean value) { useSystemTimeZone = value; return this; }
        public Builder useSystemTimeFormat(boolean value) { useSystemTimeFormat = value; return this; }
        public Builder use24Hour(boolean value) { use24Hour = value; return this; }
        public Builder showSeconds(boolean value) { showSeconds = value; return this; }
        public Builder showDate(boolean value) { showDate = value; return this; }
        public Builder showWeekday(boolean value) { showWeekday = value; return this; }
        public Builder showLunar(boolean value) { showLunar = value; return this; }
        public Builder showWeatherDescription(boolean value) { showWeatherDescription = value; return this; }
        public Builder showLocation(boolean value) { showLocation = value; return this; }
        public Builder backgroundAlpha(int value) { backgroundAlpha = value; return this; }
        public Builder textScale(float value) { textScale = value; return this; }
        public Builder tapAction(String value) { tapAction = value; return this; }
        public Builder updatedAt(long value) { updatedAt = value; return this; }
        public Builder darkText(boolean value) { darkText = value; return this; }
        public Builder fontId(String value) { fontId = value; return this; }
        public WidgetConfig build() { return new WidgetConfig(this); }
    }
}
