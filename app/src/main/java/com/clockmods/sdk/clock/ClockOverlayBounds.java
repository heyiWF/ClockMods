package com.clockmods.sdk.clock;

/**
 * Where a host parked its own overlay over the clock face — the status capsule, today.
 *
 * <p>A style has to keep its metadata out of this rectangle: the host draws something readable
 * there, and two layers of text in one place are unreadable. The coordinates are the ones the
 * style already draws in, so nothing has to be converted.</p>
 *
 * <p>Unlike {@link ClockRenderContext#getBottomInset()}, this is a box rather than a strip. The
 * overlay is a small capsule parked in one corner, and a style is free to keep drawing on the rest
 * of that edge — 轨道, for instance, pushes only the context row under it and leaves the date row
 * on the far side of the face exactly where the composition put it.</p>
 */
public final class ClockOverlayBounds {
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;

    public ClockOverlayBounds(float left, float top, float right, float bottom) {
        this.left = Math.min(left, right);
        this.top = Math.min(top, bottom);
        this.right = Math.max(left, right);
        this.bottom = Math.max(top, bottom);
    }

    public float getLeft() { return left; }
    public float getTop() { return top; }
    public float getRight() { return right; }
    public float getBottom() { return bottom; }
    public float getWidth() { return right - left; }
    public float getHeight() { return bottom - top; }

    /** True when a row spanning {@code rowLeft..rowRight} shares ground with the overlay. */
    public boolean spansHorizontally(float rowLeft, float rowRight) {
        return rowRight > left && rowLeft < right;
    }

    /** True when a row spanning {@code rowTop..rowBottom} shares ground with the overlay. */
    public boolean spansVertically(float rowTop, float rowBottom) {
        return rowBottom > top && rowTop < bottom;
    }

    /** Value equality so a host can skip repainting a frame whose overlay has not moved. */
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ClockOverlayBounds)) return false;
        ClockOverlayBounds that = (ClockOverlayBounds) other;
        return Float.compare(left, that.left) == 0 && Float.compare(top, that.top) == 0
                && Float.compare(right, that.right) == 0 && Float.compare(bottom, that.bottom) == 0;
    }

    @Override public int hashCode() {
        int result = Float.floatToIntBits(left);
        result = 31 * result + Float.floatToIntBits(top);
        result = 31 * result + Float.floatToIntBits(right);
        return 31 * result + Float.floatToIntBits(bottom);
    }
}
