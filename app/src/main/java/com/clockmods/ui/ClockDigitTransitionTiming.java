package com.clockmods.ui;

/** Timing shared by Pro Classic and the Ultimate clock faces. */
public final class ClockDigitTransitionTiming {
    public static final long DURATION_MILLIS = 300L;

    /** A sweep covers ground, so it is given longer than the in-place transitions. */
    public static final long SLIDE_DURATION_MILLIS = 440L;

    /**
     * A support line sweeps a whole sentence at a time, which is more to read than one digit
     * changing, so it is deliberately slower than the clock it belongs to.
     */
    public static final long SUPPORTING_DURATION_MILLIS = 660L;

    public static final long SCAN_DURATION_MILLIS = 480L;

    /** Share of its own font size that a swap travels. */
    public static final float SLIDE_DISTANCE_FRACTION = .27f;

    /** Share of a supporting line's own width that a horizontal sweep travels. */
    public static final float SUPPORTING_SLIDE_SHARE = .18f;

    /** Clamp for {@link #supportingSlideDistance}, measured in em of the line's own font. */
    public static final float SUPPORTING_SLIDE_MIN_EM = .75f;
    public static final float SUPPORTING_SLIDE_MAX_EM = 2.4f;

    private ClockDigitTransitionTiming() {}

    public static float easeOutCubic(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        float remaining = 1f - clamped;
        return 1f - remaining * remaining * remaining;
    }

    /**
     * Travel for a support line that swaps its whole content. {@link #SLIDE_DISTANCE_FRACTION} of
     * the font size is about a quarter of one glyph, which is plainly visible on a two-digit clock
     * but all but invisible on a line a dozen glyphs wide, so the sweep is measured against the
     * line and clamped to a font-relative range.
     */
    public static float supportingSlideDistance(float lineWidth, float fontSize) {
        if (fontSize <= 0f) return 0f;
        return Math.min(fontSize * SUPPORTING_SLIDE_MAX_EM,
                Math.max(fontSize * SUPPORTING_SLIDE_MIN_EM,
                        lineWidth * SUPPORTING_SLIDE_SHARE));
    }

    /**
     * Opacity of one copy of a horizontal sweep, {@code travel} being how much of that sweep it
     * has covered.
     *
     * The two copies slide as a rigid pair exactly that travel apart, so they only read as motion
     * while both are still visible: the fade has to be spread over the whole distance covered. The
     * squared ramp the sweep used to apply finished the fade inside the first third of the run,
     * which is why lengthening the travel changed nothing on screen — both copies were already
     * transparent across the extra distance. Measuring the fade in covered distance instead keeps
     * the extra travel visible and stretches the fade with it.
     */
    public static float sweepAlpha(float travel, boolean outgoing) {
        float covered = Math.max(0f, Math.min(1f, travel));
        return outgoing ? 1f - covered : covered;
    }

    /**
     * How far the outgoing copy of a sweep may travel. A sweep translates ink horizontally and
     * nothing else, so the ink has to stay on the canvas: text that slid past the screen edge reads
     * exactly like text that was clipped, which is the one thing a sweep must never look like.
     */
    public static float outwardSweepTravel(float distance, float inkLeft, float inkWidth,
            float canvasWidth) {
        return Math.min(distance, Math.max(0f, canvasWidth - inkLeft - inkWidth));
    }

    /**
     * How far the incoming copy of a sweep may start to the left of {@code inkLeft} and still stay
     * on the canvas. It only ever sits at or left of its settled position, so this side is bounded
     * on its own — a supporting line hugging the right edge can still sweep in from the left.
     */
    public static float inwardSweepTravel(float distance, float inkLeft) {
        return Math.min(distance, Math.max(0f, inkLeft));
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
