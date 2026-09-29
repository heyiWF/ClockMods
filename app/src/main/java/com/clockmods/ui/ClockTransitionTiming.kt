package com.clockmods.ui

/** Shared motion timing for Compose-hosted clock renderers. */
object ClockTransitionTiming {
    const val DURATION_MILLIS = 300

    /** A sweep covers ground, so it is given longer than the in-place transitions. */
    const val SLIDE_DURATION_MILLIS = 440

    /**
     * A support line sweeps a whole sentence at a time, which is more to read than one digit
     * changing, so it is deliberately slower than the clock it belongs to.
     */
    const val SUPPORTING_DURATION_MILLIS = 660

    const val SCAN_DURATION_MILLIS = 480

    /** Share of its own font size that a swap travels. */
    const val SLIDE_DISTANCE_FRACTION = .27f

    /** Share of a supporting line's own width that a horizontal sweep travels. */
    const val SUPPORTING_SLIDE_SHARE = .18f

    /** Clamp for [supportingSlideDistance], measured in em of the line's own font. */
    const val SUPPORTING_SLIDE_MIN_EM = .75f
    const val SUPPORTING_SLIDE_MAX_EM = 2.4f

    /**
     * Travel for a support line that swaps its whole content. [SLIDE_DISTANCE_FRACTION] of the font
     * size is about a quarter of one glyph, which is plainly visible on a two-digit clock but all
     * but invisible on a line a dozen glyphs wide, so the sweep is measured against the line and
     * clamped to a font-relative range.
     */
    fun supportingSlideDistance(lineWidth: Float, fontSize: Float): Float {
        if (fontSize <= 0f) return 0f
        return (lineWidth * SUPPORTING_SLIDE_SHARE)
            .coerceIn(fontSize * SUPPORTING_SLIDE_MIN_EM, fontSize * SUPPORTING_SLIDE_MAX_EM)
    }

    /**
     * Opacity of one copy of a horizontal sweep, [travel] being how much of [supportingSlideDistance]
     * (or of the digit's own sweep) it has covered.
     *
     * The two copies slide as a rigid pair exactly that travel apart, so they only read as motion
     * while both are still visible: the fade has to be spread over the whole distance covered. The
     * squared ramp the sweep used to apply finished the fade inside the first third of the run,
     * which is why lengthening the travel changed nothing on screen — both copies were already
     * transparent across the extra distance. Measuring the fade in covered distance instead keeps
     * the extra travel visible and stretches the fade with it.
     */
    fun sweepAlpha(travel: Float, outgoing: Boolean): Float {
        val covered = travel.coerceIn(0f, 1f)
        return if (outgoing) 1f - covered else covered
    }

    /**
     * How far the outgoing copy of a sweep may travel. A sweep translates ink horizontally and
     * nothing else, so the ink has to stay on the canvas: text that slid past the screen edge reads
     * exactly like text that was clipped, which is the one thing a sweep must never look like.
     * [inkLeft] and [inkWidth] describe the marked line, [canvasWidth] the surface it is drawn on.
     */
    fun outwardSweepTravel(distance: Float, inkLeft: Float, inkWidth: Float,
        canvasWidth: Float): Float =
        distance.coerceAtMost((canvasWidth - inkLeft - inkWidth).coerceAtLeast(0f))

    /**
     * How far the incoming copy of a sweep may start to the left of [inkLeft] and still stay on the
     * canvas. It only ever sits at or left of its settled position, so this side is bounded on its
     * own — a supporting line hugging the right edge can still sweep in from the left.
     */
    fun inwardSweepTravel(distance: Float, inkLeft: Float): Float =
        distance.coerceAtMost(inkLeft.coerceAtLeast(0f))

    fun easeOutCubic(progress: Float): Float {
        val remaining = 1f - progress.coerceIn(0f, 1f)
        return 1f - remaining * remaining * remaining
    }

    fun scanPhase(progress: Float): Float = easeOutCubic(
        if (progress < .5f) progress * 2f else (progress - .5f) * 2f,
    )

    fun scanEdge(left: Float, width: Float, feather: Float, progress: Float): Float {
        val phase = scanPhase(progress)
        return if (progress < .5f) left - feather + (width + feather) * phase
            else left + (width + feather) * phase
    }

    fun changedDigitPair(previous: String, current: String, index: Int): Boolean {
        if (previous.length != current.length || index !in current.indices ||
            !current[index].isDigit()) return false
        var runStart = index
        while (runStart > 0 && current[runStart - 1].isDigit()) runStart--
        val pairStart = runStart + (index - runStart) / 2 * 2
        val end = minOf(pairStart + 2, current.length)
        return (pairStart until end).any { current[it].isDigit() && previous[it] != current[it] }
    }
}
