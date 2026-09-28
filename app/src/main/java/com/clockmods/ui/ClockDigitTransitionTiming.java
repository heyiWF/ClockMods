package com.clockmods.ui;

/** Timing shared by Pro Classic and the Ultimate clock faces. */
public final class ClockDigitTransitionTiming {
    public static final long DURATION_MILLIS = 300L;

    private ClockDigitTransitionTiming() {}

    public static float easeOutCubic(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        float remaining = 1f - clamped;
        return 1f - remaining * remaining * remaining;
    }
}
