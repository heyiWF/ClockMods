package com.clockmods.sdk.clock;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Immutable clock data. It contains no layout, paint, or theme decisions. */
public final class ClockState {
    public enum SecondHandMotion { OFF, TICK, SWEEP }

    private final long timeMillis;
    private final TimeZone timeZone;
    private final Locale locale;
    private final boolean use24Hour;
    private final boolean showSeconds;
    private final SecondHandMotion secondHandMotion;
    private final String dateText;
    private final String timeZoneText;
    private final String weatherText;
    private final String statusText;
    private final List<WorldClockEntry> worldClocks;
    private final float timeScale;
    private final float dateScale;
    private final float supportingScale;

    private ClockState(Builder builder) {
        timeMillis = builder.timeMillis;
        timeZone = (TimeZone) builder.timeZone.clone();
        locale = builder.locale;
        use24Hour = builder.use24Hour;
        showSeconds = builder.showSeconds;
        secondHandMotion = builder.secondHandMotion;
        dateText = clean(builder.dateText);
        timeZoneText = clean(builder.timeZoneText);
        weatherText = clean(builder.weatherText);
        statusText = clean(builder.statusText);
        worldClocks = Collections.unmodifiableList(new ArrayList<WorldClockEntry>(
                builder.worldClocks));
        timeScale = positiveScale(builder.timeScale);
        dateScale = positiveScale(builder.dateScale);
        supportingScale = positiveScale(builder.supportingScale);
    }

    public static Builder builder(long timeMillis) {
        return new Builder(timeMillis);
    }

    public long getTimeMillis() { return timeMillis; }
    public TimeZone getTimeZone() { return (TimeZone) timeZone.clone(); }
    public Locale getLocale() { return locale; }
    public boolean isUse24Hour() { return use24Hour; }
    public boolean isShowSeconds() { return showSeconds; }
    public SecondHandMotion getSecondHandMotion() { return secondHandMotion; }
    public String getDateText() { return dateText; }
    public String getTimeZoneText() { return timeZoneText; }
    public String getWeatherText() { return weatherText; }
    public String getStatusText() { return statusText; }
    public List<WorldClockEntry> getWorldClocks() { return worldClocks; }
    public float getTimeScale() { return timeScale; }
    public float getDateScale() { return dateScale; }
    public float getSupportingScale() { return supportingScale; }

    public Calendar newCalendar() {
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTimeInMillis(timeMillis);
        return calendar;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {
        private final long timeMillis;
        private TimeZone timeZone = TimeZone.getDefault();
        private Locale locale = Locale.getDefault();
        private boolean use24Hour = true;
        private boolean showSeconds = true;
        private SecondHandMotion secondHandMotion = SecondHandMotion.TICK;
        private String dateText = "";
        private String timeZoneText = "";
        private String weatherText = "";
        private String statusText = "";
        private List<WorldClockEntry> worldClocks = Collections.emptyList();
        private float timeScale = 1f;
        private float dateScale = 1f;
        private float supportingScale = 1f;

        private Builder(long timeMillis) {
            this.timeMillis = timeMillis;
        }

        public Builder timeZone(TimeZone value) {
            if (value == null) throw new IllegalArgumentException("timeZone must not be null");
            timeZone = (TimeZone) value.clone();
            return this;
        }

        public Builder locale(Locale value) {
            if (value == null) throw new IllegalArgumentException("locale must not be null");
            locale = value;
            return this;
        }

        public Builder use24Hour(boolean value) { use24Hour = value; return this; }
        public Builder showSeconds(boolean value) { showSeconds = value; return this; }

        public Builder secondHandMotion(SecondHandMotion value) {
            if (value == null) {
                throw new IllegalArgumentException("secondHandMotion must not be null");
            }
            secondHandMotion = value;
            return this;
        }

        public Builder dateText(String value) { dateText = value; return this; }
        public Builder timeZoneText(String value) { timeZoneText = value; return this; }
        public Builder weatherText(String value) { weatherText = value; return this; }
        public Builder statusText(String value) { statusText = value; return this; }

        public Builder worldClocks(List<WorldClockEntry> value) {
            worldClocks = value == null ? Collections.<WorldClockEntry>emptyList()
                    : new ArrayList<WorldClockEntry>(value);
            return this;
        }

        public Builder timeScale(float value) { timeScale = value; return this; }
        public Builder dateScale(float value) { dateScale = value; return this; }
        public Builder supportingScale(float value) { supportingScale = value; return this; }

        public ClockState build() {
            return new ClockState(this);
        }
    }

    private static float positiveScale(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) || value <= 0f ? 1f : value;
    }
}
