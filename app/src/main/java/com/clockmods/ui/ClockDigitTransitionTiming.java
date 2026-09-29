package com.clockmods.ui;

/** Timing shared by Pro Classic and the Ultimate clock faces. */
public final class ClockDigitTransitionTiming {
    public static final long DURATION_MILLIS = 300L;
    public static final long SCAN_DURATION_MILLIS = 480L;
    public static final float SLIDE_DISTANCE_FRACTION = .18f;

    private ClockDigitTransitionTiming() {}

    public static float easeOutCubic(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        float remaining = 1f - clamped;
        return 1f - remaining * remaining * remaining;
    }

    /** Each half of a scan has its own eased sweep; the new text starts after the old ends. */
    public static float scanPhase(float progress) {
        return easeOutCubic(progress < .5f ? progress * 2f : (progress - .5f) * 2f);
    }

    public static float scanEdge(float left, float width, float feather, float progress) {
        float phase = scanPhase(progress);
        return progress < .5f
                ? left - feather + (width + feather) * phase
                : left + (width + feather) * phase;
    }

    /** A change in either digit moves the complete two-digit field. */
    public static boolean changedDigitPair(String previous, String current, int index) {
        if (previous == null || previous.length() != current.length()
                || index < 0 || index >= current.length()
                || !Character.isDigit(current.charAt(index))) return false;
        int start = index;
        while (start > 0 && Character.isDigit(current.charAt(start - 1))) start--;
        int pairStart = start + ((index - start) / 2) * 2;
        int end = Math.min(pairStart + 2, current.length());
        for (int i = pairStart; i < end && Character.isDigit(current.charAt(i)); i++) {
            if (previous.charAt(i) != current.charAt(i)) return true;
        }
        return false;
    }
}
