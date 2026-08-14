package com.clockmods.ui;

/** Shared phase clock for every per-cell calendar label carousel. */
final class CalendarCarouselTimeline {
    static final long HOLD_MS = 3000L;
    static final long TRANSITION_MS = 200L;
    static final long CYCLE_MS = HOLD_MS + TRANSITION_MS;

    private CalendarCarouselTimeline() {}

    static long elapsedAt(long uptimeMillis) {
        return uptimeMillis % CYCLE_MS;
    }

    static int indexAt(long uptimeMillis, int itemCount) {
        if (itemCount <= 0) throw new IllegalArgumentException("itemCount must be positive");
        return (int) ((uptimeMillis / CYCLE_MS) % itemCount);
    }
}
