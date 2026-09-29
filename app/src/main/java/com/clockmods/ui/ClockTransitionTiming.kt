package com.clockmods.ui

/** Shared motion timing for Compose-hosted clock renderers. */
object ClockTransitionTiming {
    const val DURATION_MILLIS = 300
    const val SCAN_DURATION_MILLIS = 480
    const val SLIDE_DISTANCE_FRACTION = .18f

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
