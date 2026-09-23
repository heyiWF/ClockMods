package com.clockmods.pro.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything a {@link CalendarLayout} needs to draw one page, already formatted.
 *
 * <p>Follows the discipline {@code ClockState} set on the clock side: <strong>strings only</strong>.
 * No {@code SolarDay}, {@code LunarDay}, {@code Taboo}, {@code HolidayStatus}, {@code Context} or
 * colour ever reaches a layout through here — the host resolves all of that, so a layout cannot
 * accidentally do date maths or resource IO while it is being measured.</p>
 *
 * <p>The second reason it exists is cost. A layout declares what it needs per day through
 * {@link CalendarLayout#requiredDayDetails()}; fields it did not ask for arrive empty and were
 * never computed. A style with no lunar text pays zero almanac constructions per page.</p>
 */
public final class CalendarPageState {
    /**
     * A slice of per-day data a layout can ask the host to resolve. Each one costs something —
     * {@link #LUNAR} and {@link #FESTIVALS} an almanac construction, {@link #HOLIDAY} a lookup in
     * the statutory calendar — so ask for the minimum.
     */
    public enum DayDetail { LUNAR, FESTIVALS, HOLIDAY, ALMANAC_TABOO }

    /** The selected day, as an index into {@link #days} plus its pre-formatted footer lines. */
    public static final class Selection {
        /** Nothing on this page is selected — the selected day lies outside the window. */
        public static final Selection NONE = new Selection(-1, "", "", "");

        public final int index;
        public final String dateLine;
        /** 宜 line, or empty when the day has none or the layout did not ask for it. */
        public final String suitableLine;
        /** 忌 line, same convention as {@link #suitableLine}. */
        public final String avoidLine;
        public final String lunarLine;
        public final String festivalsLine;

        public Selection(int index, String dateLine, String suitableLine, String avoidLine) {
            this(index, dateLine, suitableLine, avoidLine, "", "");
        }

        public Selection(int index, String dateLine, String suitableLine, String avoidLine,
                String lunarLine, String festivalsLine) {
            this.index = index;
            this.dateLine = text(dateLine);
            this.suitableLine = text(suitableLine);
            this.avoidLine = text(avoidLine);
            this.lunarLine = text(lunarLine);
            this.festivalsLine = text(festivalsLine);
        }
    }

    /**
     * The page heading in the two shapes layouts ask for it: one line for a toolbar, or split for a
     * typographic masthead. Both are formatted by the host, so a layout never sees a month number.
     */
    public static final class Title {
        public static final Title NONE = new Title("", "", "");

        /** The whole heading on one line, e.g. 2026年8月 or August 2026. */
        public final String full;
        /** The month alone, e.g. 8月 or August. */
        public final String month;
        /** The year alone, e.g. 2026. */
        public final String year;
        /**
         * A qualifier for the window the page actually shows — 第35周 for a week page, the weekday
         * name for a day page. A month page leaves it empty: its heading already names the whole
         * window, and repeating that underneath would be noise.
         */
        public final String sub;

        public Title(String full, String month, String year) {
            this(full, month, year, "");
        }

        public Title(String full, String month, String year, String sub) {
            this.full = text(full);
            this.month = text(month);
            this.year = text(year);
            this.sub = text(sub);
        }
    }

    /** One day in the window. Absent details are empty strings or empty lists, never null. */
    public static final class DayInfo {
        public final int year;
        /** Zero-based, as in {@link java.util.Calendar#MONTH}. */
        public final int month;
        public final int dayOfMonth;
        /** As in {@link java.util.Calendar#DAY_OF_WEEK}. */
        public final int dayOfWeek;
        /** Whether the day belongs to the month the page is anchored on. */
        public final boolean currentMonth;
        public final boolean today;
        public final boolean weekend;
        public final String dayNumber;
        /** Short lunar label, e.g. 廿一 or 七月. Requires {@link DayDetail#LUNAR}. */
        public final String lunarShort;
        /** Solar terms and festivals, in display order. Requires {@link DayDetail#FESTIVALS}. */
        public final List<String> festivals;
        /** Statutory holiday name, e.g. 中秋节. Requires {@link DayDetail#HOLIDAY}. */
        public final String holidayName;
        /** 休 or 班, or empty when the day carries no statutory arrangement. */
        public final String holidayBadge;
        /** Only meaningful when {@link #holidayBadge} is non-empty. */
        public final boolean holidayOffDay;
        /** Full spoken description, assembled by the host so no layout has to. */
        public final String contentDescription;

        private DayInfo(Builder builder) {
            year = builder.year;
            month = builder.month;
            dayOfMonth = builder.dayOfMonth;
            dayOfWeek = builder.dayOfWeek;
            currentMonth = builder.currentMonth;
            today = builder.today;
            weekend = builder.weekend;
            dayNumber = builder.dayNumber;
            lunarShort = builder.lunarShort;
            festivals = Collections.unmodifiableList(builder.festivals);
            holidayName = builder.holidayName;
            holidayBadge = builder.holidayBadge;
            holidayOffDay = builder.holidayOffDay;
            contentDescription = builder.contentDescription;
        }

        public static final class Builder {
            private final int year, month, dayOfMonth, dayOfWeek;
            private final boolean currentMonth, today, weekend;
            private final String dayNumber;
            private String lunarShort = "";
            private List<String> festivals = Collections.emptyList();
            private String holidayName = "";
            private String holidayBadge = "";
            private boolean holidayOffDay;
            private String contentDescription = "";

            public Builder(int year, int month, int dayOfMonth, int dayOfWeek, boolean currentMonth,
                    boolean today, boolean weekend, String dayNumber) {
                this.year = year;
                this.month = month;
                this.dayOfMonth = dayOfMonth;
                this.dayOfWeek = dayOfWeek;
                this.currentMonth = currentMonth;
                this.today = today;
                this.weekend = weekend;
                this.dayNumber = text(dayNumber);
            }

            public Builder lunar(String value) { lunarShort = text(value); return this; }

            public Builder festivals(List<String> value) {
                festivals = value == null || value.isEmpty()
                        ? Collections.<String>emptyList() : new ArrayList<>(value);
                return this;
            }

            public Builder holiday(String name, String badge, boolean offDay) {
                holidayName = text(name);
                holidayBadge = text(badge);
                holidayOffDay = offDay;
                return this;
            }

            public Builder contentDescription(String value) {
                contentDescription = text(value);
                return this;
            }

            public DayInfo build() { return new DayInfo(this); }
        }
    }

    /** Cells per row: 7 for a month or a week page, 1 for a single day. */
    public final int columns;
    public final List<DayInfo> days;
    public final Selection selection;
    /** Page heading, e.g. 2026年8月, plus its month and year halves. */
    public final Title title;
    /** Weekday names already rotated to the user's week start; empty for layouts without a header. */
    public final String[] weekdayNames;
    /** Index-aligned with {@link #weekdayNames}. */
    public final boolean[] weekdayWeekend;
    public final boolean highlightWeekends;

    public CalendarPageState(int columns, List<DayInfo> days, Selection selection, Title title,
            String[] weekdayNames, boolean[] weekdayWeekend, boolean highlightWeekends) {
        this.columns = columns;
        this.days = Collections.unmodifiableList(new ArrayList<>(days));
        this.selection = selection == null ? Selection.NONE : selection;
        this.title = title == null ? Title.NONE : title;
        this.weekdayNames = weekdayNames == null ? new String[0] : weekdayNames.clone();
        this.weekdayWeekend = weekdayWeekend == null ? new boolean[0] : weekdayWeekend.clone();
        this.highlightWeekends = highlightWeekends;
    }

    private CalendarPageState(CalendarPageState source, Selection selection) {
        columns = source.columns;
        days = source.days;
        this.selection = selection;
        title = source.title;
        weekdayNames = source.weekdayNames;
        weekdayWeekend = source.weekdayWeekend;
        highlightWeekends = source.highlightWeekends;
    }

    /**
     * The same page with a different selected day. Shares {@link #days}, which is what makes moving
     * the highlight cheap enough to leave every cell's carousel running.
     */
    public CalendarPageState withSelection(Selection selection) {
        return new CalendarPageState(this, selection == null ? Selection.NONE : selection);
    }

    private static String text(String value) { return value == null ? "" : value; }
}
