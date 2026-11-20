package com.clockmods.ui;

import java.util.Locale;

public final class ClockTimeFormatter {
    // String.format allocates a Formatter and several buffers per call; the clock calls
    // this several times on every animation frame. Hours/minutes/seconds are 0..59, so
    // precompute the strings once instead.
    private static final String[] TWO_DIGITS = new String[60];
    static {
        for (int i = 0; i < TWO_DIGITS.length; i++) {
            TWO_DIGITS[i] = i < 10 ? "0" + i : Integer.toString(i);
        }
    }

    private ClockTimeFormatter() {
    }

    public static DisplayTime format(int hour, int minute, int second,
            boolean showSeconds, boolean blinkColon, boolean smallSeconds,
            boolean use24Hour, boolean useEnglish) {
        boolean showColon = !blinkColon || second % 2 == 0;
        String separator = ":";
        int displayHour = use24Hour ? hour : hour % 12;
        if (!use24Hour && displayHour == 0) {
            displayHour = 12;
        }
        String periodText = use24Hour ? "" : periodText(hour, useEnglish);
        String hoursAndMinutes = twoDigits(displayHour) + separator + twoDigits(minute);
        if (!showSeconds) {
            return new DisplayTime(hoursAndMinutes, "", periodText, showColon);
        }
        if (smallSeconds) {
            return new DisplayTime(hoursAndMinutes, twoDigits(second), periodText, showColon);
        }
        return new DisplayTime(hoursAndMinutes + separator + twoDigits(second), "", periodText, showColon);
    }

    public static String formatHourlyChime(int hour, int minute,
            boolean use24Hour, boolean useEnglish) {
        if (use24Hour) {
            return twoDigits(hour) + ":" + twoDigits(minute);
        }
        int displayHour = hour % 12;
        if (displayHour == 0) {
            displayHour = 12;
        }
        String time = displayHour + ":" + twoDigits(minute);
        String period = periodText(hour, useEnglish);
        return useEnglish ? time + " " + period : period + time;
    }

    public static String periodText(int hour, boolean useEnglish) {
        if (useEnglish) {
            return hour < 12 ? "AM" : "PM";
        }
        return hour < 12 ? "上午" : "下午";
    }

    static String twoDigits(int value) {
        return value >= 0 && value < TWO_DIGITS.length
                ? TWO_DIGITS[value]
                : String.format(Locale.CHINA, "%02d", value);
    }

    public static final class DisplayTime {
        public final String mainText;
        public final String secondsText;
        public final String periodText;
        public final boolean colonVisible;

        private DisplayTime(String mainText, String secondsText, String periodText,
                boolean colonVisible) {
            this.mainText = mainText;
            this.secondsText = secondsText;
            this.periodText = periodText;
            this.colonVisible = colonVisible;
        }

        public boolean hasSmallSeconds() {
            return secondsText.length() > 0;
        }

        public boolean hasPeriod() {
            return periodText.length() > 0;
        }
    }
}