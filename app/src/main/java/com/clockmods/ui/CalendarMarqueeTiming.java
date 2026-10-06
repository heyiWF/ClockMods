package com.clockmods.ui;

/** Calendar marquee timing, shared by permanent almanac lines and rotating footer lines. */
public final class CalendarMarqueeTiming {
    public static final long PAUSE_MILLIS = 1000L;
    public static final float SPEED_DP_PER_SECOND = 40f;

    private CalendarMarqueeTiming() {}

    public static long scrollMillis(float distance, float density) {
        return Math.max(1L, (long) Math.ceil(Math.max(0f, distance)
                / (SPEED_DP_PER_SECOND * Math.max(.01f, density)) * 1000d));
    }

    public static float loopDistance(float textWidth, float textSize, float density) {
        return textWidth + Math.max(density * 24f, textSize * 1.5f);
    }

    /** The second copy reaches the first one's origin, then rests there for one second. */
    public static float loopOffset(long elapsedMillis, float distance, float density) {
        if (distance <= 0f) return 0f;
        long scroll = scrollMillis(distance, density);
        long elapsed = Math.max(0L, elapsedMillis) % (PAUSE_MILLIS + scroll);
        return elapsed < PAUSE_MILLIS ? 0f
                : distance * (elapsed - PAUSE_MILLIS) / scroll;
    }

    public static float scrollOffset(long elapsedMillis, float overflow, float density) {
        if (overflow <= 0f) return 0f;
        return overflow * Math.max(0f, Math.min(1f,
                (elapsedMillis - PAUSE_MILLIS) / (float) scrollMillis(overflow, density)));
    }

    public static long holdMillis(float overflow, float density) {
        return overflow <= 0f ? 3000L : Math.max(3000L,
                2L * PAUSE_MILLIS + scrollMillis(overflow, density));
    }
}
