package com.clockmods.pro.style;

import com.clockmods.pro.CalendarTheme;
import com.clockmods.pro.style.CalendarLayoutCapabilities.Capability;
import com.clockmods.pro.style.CalendarStyleMetadata.Kind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The built-in calendar gallery, in display order. The first entry is the fallback and must stay in
 * sync with {@code ClockPreferences.DEFAULT_CALENDAR_THEME} — {@code CalendarDefaultStyleTest}
 * turns that comment into a failing build.
 *
 * <p>Mirrors {@code UltimateClockStyles}: a memoised shared registry for the app, plus
 * {@link #createRegistry()} for tests that need an isolated one.</p>
 */
public final class UltimateCalendarStyles {
    /** Everything the month dashboard shows, which is everything. */
    private static final CalendarLayoutCapabilities DASHBOARD_CAPABILITIES =
            CalendarLayoutCapabilities.of(Capability.CLOCK, Capability.WEATHER, Capability.FORECAST,
                    Capability.STATUS_ICONS, Capability.PAGE_SWIPE, Capability.DAY_SELECTION,
                    Capability.MONTH_PICKER, Capability.WEEKDAY_HEADER, Capability.FOOTER_ALMANAC);

    /**
     * The pure-calendar presets: the same month panel with the whole clock/weather column dropped.
     * They do not declare {@link Capability#STATUS_ICONS} because today the capsule lives inside
     * that column and disappears with it — restoring it is a separate, deliberate change.
     */
    private static final CalendarLayoutCapabilities WALL_CAPABILITIES =
            CalendarLayoutCapabilities.of(Capability.PAGE_SWIPE, Capability.DAY_SELECTION,
                    Capability.MONTH_PICKER, Capability.WEEKDAY_HEADER, Capability.FOOTER_ALMANAC);

    /**
     * The poster keeps only what a bare month grid cannot do without. It does declare
     * {@link Capability#WEEKDAY_HEADER}, unlike the sketch this style was planned from: a 7×6 block
     * of numbers with no column labels is a puzzle, not a restraint.
     */
    private static final CalendarLayoutCapabilities POSTER_CAPABILITIES =
            CalendarLayoutCapabilities.of(Capability.PAGE_SWIPE, Capability.DAY_SELECTION,
                    Capability.MONTH_PICKER, Capability.WEEKDAY_HEADER);

    /**
     * 周程 keeps the weather readings but deliberately drops {@link Capability#CLOCK}: a week page
     * carrying a big clock as well would just be the dashboard with fewer days. It is also the
     * first style to declare {@link Capability#STATUS_ICONS} without a side column — the capsule
     * sits in its header row instead.
     *
     * <p>{@link Capability#WEEKDAY_HEADER} is declared even though there is no separate header row:
     * the weekday names are rendered inside the strip cells, which is still a style that shows
     * them.</p>
     */
    private static final CalendarLayoutCapabilities AGENDA_CAPABILITIES =
            CalendarLayoutCapabilities.of(Capability.WEATHER, Capability.FORECAST,
                    Capability.STATUS_ICONS, Capability.PAGE_SWIPE, Capability.DAY_SELECTION,
                    Capability.MONTH_PICKER, Capability.WEEKDAY_HEADER);

    private static final CalendarStyleRegistry SHARED_REGISTRY = createRegistry();

    private UltimateCalendarStyles() { }

    public static CalendarStyleRegistry createRegistry() {
        CalendarStyleRegistry registry = new CalendarStyleRegistry();
        for (CalendarStyle style : builtIns()) registry.register(style);
        registry.setFallback(CalendarTheme.ID_GRAPHITE);
        return registry;
    }

    public static CalendarStyleRegistry sharedRegistry() {
        return SHARED_REGISTRY;
    }

    private static List<CalendarStyle> builtIns() {
        return new ArrayList<>(Arrays.asList(
                new PresetStyle(CalendarTheme.ID_GRAPHITE, Kind.DASHBOARD, DASHBOARD_CAPABILITIES),
                new PresetStyle(CalendarTheme.ID_CARBON, Kind.DASHBOARD, DASHBOARD_CAPABILITIES),
                new PresetStyle(CalendarTheme.ID_PAPER, Kind.WALL, WALL_CAPABILITIES),
                new PresetStyle(CalendarTheme.ID_POSTER, Kind.POSTER, POSTER_CAPABILITIES),
                new PresetStyle(CalendarTheme.ID_AGENDA, Kind.AGENDA, AGENDA_CAPABILITIES)));
    }

    /** A gallery entry whose palette still lives in {@link CalendarTheme}. */
    private static final class PresetStyle implements CalendarStyle {
        private final CalendarStyleMetadata metadata;
        private final CalendarTheme theme;

        PresetStyle(String id, Kind kind, CalendarLayoutCapabilities capabilities) {
            theme = CalendarTheme.resolve(id);
            // resolve() silently falls back, which would turn a typo here into a duplicate palette
            // rather than a crash. Refuse instead.
            if (!theme.id.equals(id)) {
                throw new IllegalArgumentException("No CalendarTheme palette for id: " + id);
            }
            metadata = new CalendarStyleMetadata(id, theme.nameRes, theme.summaryRes, kind,
                    capabilities, 1, 31);
        }

        @Override public CalendarStyleMetadata getMetadata() { return metadata; }

        @Override public CalendarTheme getTheme() { return theme; }

        @Override public CalendarLayout newLayout() {
            switch (metadata.getKind()) {
                case POSTER:
                    return new PosterCalendarLayout(metadata.getCapabilities());
                case AGENDA:
                    return new AgendaCalendarLayout(metadata.getCapabilities());
                default:
                    return new DashboardCalendarLayout(metadata.getCapabilities());
            }
        }
    }
}
