package com.clockmods.pro;

/** Keeps the drag-region decision fixed for one complete touch stream. */
final class MonthGestureRegion {
    private boolean gestureEligible;

    MonthGestureRegion() { }

    void start(boolean containsDown) {
        gestureEligible = containsDown;
    }

    boolean acceptsGesture() {
        return gestureEligible;
    }

    void finish() {
        gestureEligible = false;
    }

    static boolean contains(int left, int top, int right, int bottom, float x, float y) {
        return x >= left && x < right && y >= top && y < bottom;
    }
}
