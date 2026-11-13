package com.clockmods.pro;

/** Preserve the monotonic time base across page changes and process recreation. */
final class StopwatchTimebase {
    static long elapsed(long accumulated, boolean running, long startedRealtime, long startedWall,
            long nowRealtime, long nowWall) {
        long base = Math.max(0, accumulated);
        if (!running) return base;
        long delta = startedRealtime > 0 && nowRealtime >= startedRealtime
                ? nowRealtime - startedRealtime : startedWall > 0 ? Math.max(0, nowWall - startedWall) : 0;
        return Long.MAX_VALUE - base < delta ? Long.MAX_VALUE : base + delta;
    }
}
