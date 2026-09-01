package com.clockmods.pro.style;

/** The current time, already formatted in the app's time zone and locale by the host. */
public final class CalendarClockState {
    public final String time;
    public final String seconds;
    public final boolean showSeconds;
    /** AM/PM text; empty in 24-hour mode. */
    public final String period;
    public final boolean showPeriod;

    public CalendarClockState(String time, String seconds, boolean showSeconds, String period,
            boolean showPeriod) {
        this.time = time == null ? "" : time;
        this.seconds = seconds == null ? "" : seconds;
        this.showSeconds = showSeconds;
        this.period = period == null ? "" : period;
        this.showPeriod = showPeriod;
    }
}
