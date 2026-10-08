package com.clockmods.ui;

/** Calendar marquee timing, shared by permanent almanac lines and rotating footer lines. */
public final class CalendarMarqueeTiming {
    public static final long PAUSE_MILLIS = 1000L;
    public static final float SPEED_DP_PER_SECOND = 40f;

    private CalendarMarqueeTiming() {}

    public static long scrollMillis(float distance, float density) {
        return com.clockmods.sdk.style.MarqueeSpec.DEFAULT.scrollMillis(distance, density);
    }
    public static float loopDistance(float width, float size, float density) {
        return com.clockmods.sdk.style.MarqueeSpec.DEFAULT.loopDistance(width, size, density);
    }
    public static float loopOffset(long elapsed, float distance, float density) {
        return com.clockmods.sdk.style.MarqueeSpec.DEFAULT.loopOffset(elapsed, distance, density);
    }
    public static float scrollOffset(long elapsed, float overflow, float density) {
        return com.clockmods.sdk.style.MarqueeSpec.DEFAULT.scrollOffset(elapsed, overflow, density);
    }
    public static long holdMillis(float overflow, float density) {
        return com.clockmods.sdk.style.MarqueeSpec.DEFAULT.holdMillis(overflow, density);
    }
}
