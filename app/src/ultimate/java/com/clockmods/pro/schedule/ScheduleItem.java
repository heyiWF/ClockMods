package com.clockmods.pro.schedule;

import androidx.annotation.Nullable;

/**
 * One entry on a day's schedule. An all-day item carries {@link #TIME_NONE} and sorts first; a
 * timed item carries an hour/minute and sorts by that. {@link #id} is stable so a row can be edited
 * or removed after a reload without pairing rows to list indices that shift on every sort.
 */
public final class ScheduleItem {
    public static final int TIME_NONE = -1;

    public final String id;
    public final String title;
    public final int hour;
    public final int minute;

    public ScheduleItem(String id, String title, int hour, int minute) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "" : title;
        this.hour = hour;
        this.minute = minute;
    }

    public boolean hasTime() {
        return hour != TIME_NONE && minute != TIME_NONE;
    }

    /** "HH:mm" label for a timed item, or "" for an all-day one. */
    public String timeLabel() {
        return hasTime() ? String.format(java.util.Locale.US, "%02d:%02d", hour, minute) : "";
    }

    /** All-day items (no time) sort first, then by time, then by insertion order. */
    public int sortKey() {
        return hasTime() ? hour * 100 + minute : -1;
    }

    @Override public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (!(other instanceof ScheduleItem)) return false;
        ScheduleItem that = (ScheduleItem) other;
        return id.equals(that.id) && title.equals(that.title) && hour == that.hour
                && minute == that.minute;
    }

    @Override public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + hour;
        result = 31 * result + minute;
        return result;
    }
}
