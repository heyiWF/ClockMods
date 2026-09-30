package com.clockmods.sdk.clock;

/** Measured by the renderer, then used by the host to schedule the next carousel item. */
public final class MessageMarqueeLayout {
    public static final long PAUSE_MILLIS = 1000L;
    public static final float SPEED_DP_PER_SECOND = 40f;
    public static final float EDGE_FRACTION = .08f;
    public long displayMillis = 3000L;
    public float textWidth, availableWidth, textSize, distance, offset;

    public void update(float width, float available, float size, float density, long elapsed) {
        textWidth = width;
        availableWidth = available;
        textSize = size;
        distance = width <= available ? 0f : width - available + 2f * available * EDGE_FRACTION;
        float speed = SPEED_DP_PER_SECOND * Math.max(.01f, density);
        displayMillis = Math.max(3000L, 2L * PAUSE_MILLIS
                + (long) Math.ceil(distance / (double) speed * 1000d));
        offset = Math.min(distance, Math.max(0L, elapsed - PAUSE_MILLIS) * speed / 1000f);
    }
}
