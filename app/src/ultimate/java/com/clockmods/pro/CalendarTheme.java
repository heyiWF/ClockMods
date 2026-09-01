package com.clockmods.pro;

import android.graphics.drawable.GradientDrawable;

import com.clockmods.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A calendar page preset: the complete colour set the month dashboard paints itself with, plus
 * whether the preset keeps the clock/weather column or renders the month panel alone.
 *
 * <p>The layouts still inflate with the {@code graphite} palette from
 * {@code res/values/calendar_dashboard.xml}; every themed surface is then re-tinted from
 * {@link ProCalendarFragment}, because the same views exist in three layout variant sets and a
 * preset has to reach all of them without duplicating the palette per variant.</p>
 */
public final class CalendarTheme {
    /** Keep in sync with {@code ClockPreferences.DEFAULT_CALENDAR_THEME}. */
    public static final String ID_GRAPHITE = "calendar.graphite";
    public static final String ID_PAPER = "calendar.paper";
    public static final String ID_POSTER = "calendar.poster";
    public static final String ID_AGENDA = "calendar.agenda";
    public static final String ID_CARBON = "calendar.carbon";

    public final String id;
    public final int nameRes;
    public final int summaryRes;
    /** False for the pure-calendar presets: the clock, weather and forecast column is dropped. */
    public final boolean sideColumn;
    /** True when the preset keeps the user's own clock colour and status-icon tint (the default). */
    public final boolean followsUserTimeColor;
    public final int backgroundStart;
    public final int backgroundEnd;
    public final int panel;
    /** Hairline around every card; {@code 0} means the preset draws no border. */
    public final int panelStroke;
    public final float cornerRadiusDp;
    public final int text;
    public final int secondary;
    public final int day;
    /** Weekday header row; the default preset keeps it at full strength, the rest subdue it. */
    public final int weekday;
    public final int accent;
    public final int weatherIcon;
    /** Forecast glyphs for the non-today columns; light presets soften them off {@link #text}. */
    public final int forecastIcon;
    public final int today;
    /** Wash drawn behind today's number — a translucent tint of {@link #today}, or {@code 0}. */
    public final int todayFill;
    public final int restBadge;
    public final int workBadge;
    public final int weekend;
    public final int suitable;
    public final int avoid;
    public final int selectionFill;
    public final int selectionStroke;

    private CalendarTheme(Builder builder) {
        id = builder.id;
        nameRes = builder.nameRes;
        summaryRes = builder.summaryRes;
        sideColumn = builder.sideColumn;
        followsUserTimeColor = builder.followsUserTimeColor;
        backgroundStart = builder.backgroundStart;
        backgroundEnd = builder.backgroundEnd;
        panel = builder.panel;
        panelStroke = builder.panelStroke;
        cornerRadiusDp = builder.cornerRadiusDp;
        text = builder.text;
        secondary = builder.secondary;
        day = builder.day;
        weekday = builder.weekday;
        accent = builder.accent;
        weatherIcon = builder.weatherIcon;
        forecastIcon = builder.forecastIcon != 0 ? builder.forecastIcon : builder.text;
        today = builder.today;
        todayFill = builder.todayFill;
        restBadge = builder.restBadge;
        workBadge = builder.workBadge;
        weekend = builder.weekend;
        suitable = builder.suitable;
        avoid = builder.avoid;
        selectionFill = builder.selectionFill;
        selectionStroke = builder.selectionStroke;
    }

    /** Page canvas. A flat preset simply repeats the same colour at both stops. */
    public GradientDrawable newPageBackground() {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {backgroundStart, backgroundEnd});
        background.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return background;
    }

    /** Card surface. Every card gets its own instance: a Drawable cannot share bounds. */
    public GradientDrawable newPanelBackground(float density) {
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(panel);
        surface.setCornerRadius(cornerRadiusDp * density);
        if (panelStroke != 0) surface.setStroke(Math.max(1, Math.round(density)), panelStroke);
        return surface;
    }

    /** Highlight behind the selected day cell. */
    public GradientDrawable newSelectionBackground(float density) {
        GradientDrawable selection = new GradientDrawable();
        selection.setColor(selectionFill);
        selection.setCornerRadius(6f * density);
        selection.setStroke(Math.max(1, Math.round(density)), selectionStroke);
        return selection;
    }

    /**
     * The catalogue in gallery order; the first entry is the fallback. One entry per
     * composition — a palette on its own does not earn a place here, because a user choosing
     * a theme is choosing a layout, not a colourway. {@code calendar.carbon} is the single
     * deliberate exception: it is graphite with the lights off, kept because an OLED panel
     * showing true black is a different thing to look at, not a different tint.
     */
    public static List<CalendarTheme> presets() {
        return PRESETS;
    }

    /** Resolves a stored id, falling back to the default preset for anything unknown. */
    public static CalendarTheme resolve(String themeId) {
        for (int index = 0; index < PRESETS.size(); index++) {
            if (PRESETS.get(index).id.equals(themeId)) return PRESETS.get(index);
        }
        return PRESETS.get(0);
    }

    private static final class Builder {
        private final String id;
        private int nameRes;
        private int summaryRes;
        private boolean sideColumn;
        private boolean followsUserTimeColor;
        private int backgroundStart;
        private int backgroundEnd;
        private int panel;
        private int panelStroke;
        private float cornerRadiusDp = 12f;
        private int text;
        private int secondary;
        private int day;
        private int weekday;
        private int accent;
        private int weatherIcon;
        private int forecastIcon;
        private int today;
        private int todayFill;
        private int restBadge;
        private int workBadge;
        private int weekend;
        private int suitable;
        private int avoid;
        private int selectionFill;
        private int selectionStroke;

        Builder(String id, int nameRes, int summaryRes) {
            this.id = id;
            this.nameRes = nameRes;
            this.summaryRes = summaryRes;
        }

        Builder sideColumn(boolean value) { sideColumn = value; return this; }
        Builder followsUserTimeColor(boolean value) { followsUserTimeColor = value; return this; }
        Builder background(int start, int end) {
            backgroundStart = start;
            backgroundEnd = end;
            return this;
        }
        Builder panel(int fill, int stroke, float radiusDp) {
            panel = fill;
            panelStroke = stroke;
            cornerRadiusDp = radiusDp;
            return this;
        }
        Builder type(int primary, int support, int dayNumber) {
            text = primary;
            secondary = support;
            day = dayNumber;
            return this;
        }
        Builder weekday(int value) { weekday = value; return this; }
        Builder accent(int value, int icon) { accent = value; weatherIcon = icon; return this; }
        /** Overrides the forecast glyphs, which otherwise inherit the primary text colour. */
        Builder forecast(int icon) { forecastIcon = icon; return this; }
        Builder today(int value, int fill) { today = value; todayFill = fill; return this; }
        Builder badges(int rest, int work) { restBadge = rest; workBadge = work; return this; }
        Builder weekend(int value) { weekend = value; return this; }
        Builder almanac(int good, int bad) { suitable = good; avoid = bad; return this; }
        Builder selection(int fill, int stroke) {
            selectionFill = fill;
            selectionStroke = stroke;
            return this;
        }

        CalendarTheme build() { return new CalendarTheme(this); }
    }

    private static final List<CalendarTheme> PRESETS = Collections.unmodifiableList(Arrays.asList(
            // The shipped dashboard. Its palette mirrors res/values/calendar_dashboard.xml so
            // switching back to it restores the pre-preset appearance exactly.
            new Builder(ID_GRAPHITE, R.string.ultimate_calendar_theme_graphite_name,
                    R.string.ultimate_calendar_theme_graphite_summary)
                    .sideColumn(true).followsUserTimeColor(true)
                    .background(0xFF171918, 0xFF171918)
                    .panel(0xFF363836, 0, 10f)
                    .type(0xFFF2F3F2, 0xFFD0D2D0, 0xFFFFFFFF)
                    .weekday(0xFFF2F3F2)
                    .accent(0xFF2693FF, 0xFFFFFFFF)
                    .today(0xFF16E13B, 0)
                    .badges(0xFF16E13B, 0xFFFFC11A)
                    .weekend(0xFFFF9B9B)
                    .almanac(0xFF16E13B, 0xFFFF5A5A)
                    .selection(0xFF33404B, 0xFF2693FF)
                    .build(),
            // 纯黑碳素: the original dashboard's palette on a true-black canvas, for OLED. The
            // paper is #000 rather than graphite's near-black, the cards drop to a single dark
            // step and every accent stays — nothing else moves, so it reads as the shipped theme
            // with the lights off.
            new Builder(ID_CARBON, R.string.ultimate_calendar_theme_carbon_name,
                    R.string.ultimate_calendar_theme_carbon_summary)
                    .sideColumn(true).followsUserTimeColor(true)
                    .background(0xFF000000, 0xFF000000)
                    .panel(0xFF191A19, 0, 10f)
                    .type(0xFFE8E9E8, 0xFF9A9D9A, 0xFFF2F3F2)
                    .weekday(0xFF9A9D9A)
                    .accent(0xFF2693FF, 0xFFFFFFFF)
                    .today(0xFF16E13B, 0)
                    .badges(0xFF16E13B, 0xFFFFC11A)
                    .weekend(0xFFFF9B9B)
                    .almanac(0xFF16E13B, 0xFFFF5A5A)
                    .selection(0xFF262A2E, 0xFF2693FF)
                    .build(),
            new Builder(ID_PAPER, R.string.ultimate_calendar_theme_paper_name,
                    R.string.ultimate_calendar_theme_paper_summary)
                    .background(0xFFF5F1E6, 0xFFEBE4D3)
                    .panel(0xFFFCFAF4, 0xFFDFD7C4, 8f)
                    .type(0xFF262420, 0xFF7C7466, 0xFF33302A)
                    .weekday(0xFF7C7466)
                    .accent(0xFF8A6034, 0xFF8A6034)
                    .today(0xFFB5392A, 0x22B5392A)
                    .badges(0xFF4A7A4E, 0xFFB07A21)
                    .weekend(0xFFA8492F)
                    .almanac(0xFF3F6B45, 0xFFA8342A)
                    .selection(0xFFEFE6D2, 0xFFB5392A)
                    .build(),
            // 墨白排版. A poster, not a card layout: no panel, no rounded corners, one vermilion
            // accent against ink on warm white. The greys are deliberately close together — the
            // composition carries the hierarchy, so the palette does not have to.
            new Builder(ID_POSTER, R.string.ultimate_calendar_theme_poster_name,
                    R.string.ultimate_calendar_theme_poster_summary)
                    .background(0xFFFAFAF8, 0xFFF2F2EE)
                    .panel(0x00000000, 0, 0f)
                    .type(0xFF16181A, 0xFF9AA0A6, 0xFF1F2226)
                    .weekday(0xFFB0B5BA)
                    .accent(0xFFC8362F, 0xFFC8362F)
                    // No disc behind today: the poster marks it with a dot under the number, so a
                    // wash would be a second, competing mark.
                    .today(0xFFC8362F, 0)
                    .badges(0xFF4A7A4E, 0xFFB07A21)
                    .weekend(0xFF9BA1A6)
                    .almanac(0xFF3F6B45, 0xFFA8342A)
                    .selection(0x00000000, 0xFF16181A)
                    .build(),
            // 靛蓝周程: a paper planner rather than a dashboard. Everything is stationery ink on a
            // cool white sheet, so the one saturated colour — indigo — can carry today, the
            // selection and the heading without competing with anything.
            new Builder(ID_AGENDA, R.string.ultimate_calendar_theme_agenda_name,
                    R.string.ultimate_calendar_theme_agenda_summary)
                    .background(0xFFF5F6FB, 0xFFE7EAF6)
                    .panel(0xFFFFFFFF, 0xFFDCE0F0, 18f)
                    .type(0xFF1B1F3B, 0xFF6E748F, 0xFF232845)
                    .weekday(0xFF8B90A8)
                    .accent(0xFF3B4A9E, 0xFF3B4A9E)
                    .forecast(0xFF6E748F)
                    .today(0xFF3B4A9E, 0x1F3B4A9E)
                    .badges(0xFF2F8F6B, 0xFFC1811F)
                    .weekend(0xFFC24B57)
                    .almanac(0xFF2F8F6B, 0xFFC0453F)
                    .selection(0xFFE7EAF9, 0xFF3B4A9E)
                    .build()));
}
