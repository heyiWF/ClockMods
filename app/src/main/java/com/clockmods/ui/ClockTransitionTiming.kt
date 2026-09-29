package com.clockmods.ui

/** Shared motion timing for Compose-hosted clock renderers. */
object ClockTransitionTiming {
    const val DURATION_MILLIS = 300
    const val SCAN_DURATION_MILLIS = 480
    const val SLIDE_DISTANCE_FRACTION = .18f

    /** Share of a supporting line's own width that a horizontal sweep travels. */
    const val SUPPORTING_SLIDE_SHARE = .12f

    /** Clamp for [supportingSlideDistance], measured in em of the line's own font. */
    const val SUPPORTING_SLIDE_MIN_EM = .5f
    const val SUPPORTING_SLIDE_MAX_EM = 1.6f

    /**
     * Travel for a support line that swaps its whole content. [SLIDE_DISTANCE_FRACTION] of the font
     * size is about a third of one glyph, which is plainly visible on a two-digit clock but all but
     * invisible on a line a dozen glyphs wide, so the sweep is measured against the line and clamped
     * to a font-relative range.
     */
    fun supportingSlideDistance(lineWidth: Float, fontSize: Float): Float {
        if (fontSize <= 0f) return 0f
        return (lineWidth * SUPPORTING_SLIDE_SHARE)
            .coerceIn(fontSize * SUPPORTING_SLIDE_MIN_EM, fontSize * SUPPORTING_SLIDE_MAX_EM)
    }

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
