package com.clockmods.weather;

/** Allows one failure toast across current weather and forecast per app process. */
final class WeatherFailureDeduplicator {
    private boolean shown;

    synchronized boolean shouldShow() {
        if (shown) return false;
        shown = true;
        return true;
    }
}
