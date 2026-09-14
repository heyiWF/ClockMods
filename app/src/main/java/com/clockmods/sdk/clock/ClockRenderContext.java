package com.clockmods.sdk.clock;

/** Per-frame geometry and host policy supplied to a {@link ClockRenderer}. */
public final class ClockRenderContext {
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;
    private final float density;
    private final float scaledDensity;
    private final long frameTimeMillis;
    private final ClockBackground background;
    private final float bottomInset;
    private final float worldClockScroll;
    private final boolean worldClockStripHosted;

    public ClockRenderContext(float left, float top, float right, float bottom, float density,
            float scaledDensity, long frameTimeMillis) {
        this(left, top, right, bottom, density, scaledDensity, frameTimeMillis,
                null, 0f);
    }

    public ClockRenderContext(float left, float top, float right, float bottom, float density,
            float scaledDensity, long frameTimeMillis,
            ClockBackground background) {
        this(left, top, right, bottom, density, scaledDensity, frameTimeMillis,
                background, 0f);
    }

    /**
     * Creates a frame context with a host-owned bottom overlay reservation. The canvas bounds stay
     * unchanged so backgrounds still cover the full view; renderers can use the inset for metadata
     * that must remain above an overlaid attribution or control pill.
     */
    public ClockRenderContext(float left, float top, float right, float bottom, float density,
            float scaledDensity, long frameTimeMillis,
            ClockBackground background, float bottomInset) {
        this(left, top, right, bottom, density, scaledDensity, frameTimeMillis,
                background, bottomInset, 0f);
    }

    /** Creates a frame with host-owned world-clock horizontal scroll offset. */
    public ClockRenderContext(float left, float top, float right, float bottom, float density,
            float scaledDensity, long frameTimeMillis,
            ClockBackground background, float bottomInset, float worldClockScroll) {
        this(left, top, right, bottom, density, scaledDensity, frameTimeMillis,
                background, bottomInset, worldClockScroll, false);
    }

    /** A native host may draw the city strip itself; styles still reserve its space. */
    public ClockRenderContext(float left, float top, float right, float bottom, float density,
            float scaledDensity, long frameTimeMillis,
            ClockBackground background, float bottomInset, float worldClockScroll,
            boolean worldClockStripHosted) {
        if (right < left || bottom < top) {
            throw new IllegalArgumentException("Render bounds must not be inverted");
        }
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.density = Math.max(0.01f, density);
        this.scaledDensity = Math.max(0.01f, scaledDensity);
        this.frameTimeMillis = frameTimeMillis;
        this.background = background;
        this.bottomInset = Math.max(0f, Math.min(bottomInset, bottom - top));
        this.worldClockScroll = Math.max(0f, worldClockScroll);
        this.worldClockStripHosted = worldClockStripHosted;
    }

    public float getLeft() { return left; }
    public float getTop() { return top; }
    public float getRight() { return right; }
    public float getBottom() { return bottom; }
    public float getWidth() { return right - left; }
    public float getHeight() { return bottom - top; }
    public float getCenterX() { return (left + right) * 0.5f; }
    public float getCenterY() { return (top + bottom) * 0.5f; }
    public float getDensity() { return density; }
    public float getScaledDensity() { return scaledDensity; }
    public long getFrameTimeMillis() { return frameTimeMillis; }
    public ClockBackground getBackground() { return background; }
    public float getBottomInset() { return bottomInset; }
    public float getWorldClockScroll() { return worldClockScroll; }
    public boolean isWorldClockStripHosted() { return worldClockStripHosted; }
}
