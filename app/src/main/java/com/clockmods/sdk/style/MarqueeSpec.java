package com.clockmods.sdk.style;

/** Immutable, Android-independent motion policy. Distances are pixels; configuration is dp. */
public final class MarqueeSpec {
    public static final MarqueeSpec DEFAULT = new MarqueeSpec(40f, 1000L, 24f);
    /** No further frame is needed until content, geometry or activation changes. */
    public static final long IDLE = Long.MAX_VALUE;
    public final float speedDpPerSecond;
    public final long pauseMillis;
    public final float gapDp;

    public MarqueeSpec(float speedDpPerSecond, long pauseMillis, float gapDp) {
        this.speedDpPerSecond = finiteRange(speedDpPerSecond, 10f, 120f, 40f);
        this.pauseMillis = Math.max(0L, Math.min(5000L, pauseMillis));
        this.gapDp = finiteRange(gapDp, 8f, 96f, 24f);
    }

    private static float finiteRange(float value, float min, float max, float fallback) {
        return Float.isNaN(value) || Float.isInfinite(value) ? fallback
                : Math.max(min, Math.min(max, value));
    }

    public long scrollMillis(float distance, float density) {
        return Math.max(1L, (long) Math.ceil(Math.max(0f, distance)
                / (speedDpPerSecond * Math.max(.01f, density)) * 1000d));
    }

    public float loopDistance(float textWidth, float textSize, float density) {
        return textWidth + Math.max(density * gapDp, textSize * 1.5f);
    }

    public float loopOffset(long elapsedMillis, float distance, float density) {
        if (distance <= 0f) return 0f;
        long scroll = scrollMillis(distance, density);
        long elapsed = Math.max(0L, elapsedMillis) % (pauseMillis + scroll);
        return elapsed < pauseMillis ? 0f : distance * (elapsed - pauseMillis) / scroll;
    }

    public float scrollOffset(long elapsedMillis, float overflow, float density) {
        if (overflow <= 0f) return 0f;
        return overflow * Math.max(0f, Math.min(1f,
                (elapsedMillis - pauseMillis) / (float) scrollMillis(overflow, density)));
    }

    public long holdMillis(float overflow, float density) {
        return overflow <= 0f ? 3000L : Math.max(3000L,
                2L * pauseMillis + scrollMillis(overflow, density));
    }

    /** Sleep through the head pause, then use frames during motion. */
    public long loopFrameDelay(long elapsedMillis, float distance, float density) {
        if (distance <= 0f) return IDLE;
        long elapsed = Math.max(0L, elapsedMillis) % (pauseMillis + scrollMillis(distance, density));
        return elapsed < pauseMillis ? pauseMillis - elapsed : 16L;
    }

    /** One-shot footer scroll: no redraws after reaching the tail. */
    public long scrollFrameDelay(long elapsedMillis, float overflow, float density) {
        if (overflow <= 0f) return IDLE;
        long elapsed = Math.max(0L, elapsedMillis);
        if (elapsed < pauseMillis) return pauseMillis - elapsed;
        return elapsed < pauseMillis + scrollMillis(overflow, density) ? 16L : IDLE;
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof MarqueeSpec)) return false;
        MarqueeSpec value = (MarqueeSpec) other;
        return speedDpPerSecond == value.speedDpPerSecond && pauseMillis == value.pauseMillis
                && gapDp == value.gapDp;
    }

    @Override public int hashCode() {
        return 31 * (31 * Float.floatToIntBits(speedDpPerSecond) + (int) pauseMillis)
                + Float.floatToIntBits(gapDp);
    }
}
