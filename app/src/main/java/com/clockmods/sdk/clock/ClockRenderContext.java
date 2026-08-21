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
    private final boolean reducedMotion;
    private final ClockBackground background;

    public ClockRenderContext(float left, float top, float right, float bottom, float density,
            float scaledDensity, long frameTimeMillis, boolean reducedMotion) {
        this(left, top, right, bottom, density, scaledDensity, frameTimeMillis, reducedMotion,
                null);
    }

    public ClockRenderContext(float left, float top, float right, float bottom, float density,
            float scaledDensity, long frameTimeMillis, boolean reducedMotion,
            ClockBackground background) {
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
        this.reducedMotion = reducedMotion;
        this.background = background;
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
    public boolean isReducedMotion() { return reducedMotion; }
    public ClockBackground getBackground() { return background; }
}
