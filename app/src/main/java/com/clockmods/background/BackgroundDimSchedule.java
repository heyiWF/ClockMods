package com.clockmods.background;

public final class BackgroundDimSchedule {
    private BackgroundDimSchedule() {
    }

    public static boolean isActive(int currentMinutes, int startMinutes, int endMinutes) {
        if (startMinutes == endMinutes) {
            return false;
        }
        if (startMinutes < endMinutes) {
            return currentMinutes >= startMinutes && currentMinutes < endMinutes;
        }
        return currentMinutes >= startMinutes || currentMinutes < endMinutes;
    }
}