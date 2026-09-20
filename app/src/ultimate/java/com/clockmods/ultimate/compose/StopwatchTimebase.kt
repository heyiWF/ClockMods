package com.clockmods.ultimate.compose

/** Computes a running stopwatch value across recomposition, process death, and device reboot. */
internal object StopwatchTimebase {
    fun elapsed(
        accumulatedMillis: Long,
        running: Boolean,
        startedAtRealtimeMillis: Long,
        startedAtWallMillis: Long,
        nowRealtimeMillis: Long,
        nowWallMillis: Long,
    ): Long {
        if (!running) return accumulatedMillis.coerceAtLeast(0L)

        val realtimeDelta = if (
            startedAtRealtimeMillis > 0L && nowRealtimeMillis >= startedAtRealtimeMillis
        ) {
            nowRealtimeMillis - startedAtRealtimeMillis
        } else {
            -1L
        }
        val wallDelta = if (startedAtWallMillis > 0L && nowWallMillis >= startedAtWallMillis) {
            nowWallMillis - startedAtWallMillis
        } else {
            0L
        }
        val runningMillis = if (realtimeDelta >= 0L) realtimeDelta else wallDelta
        return saturatedAdd(accumulatedMillis.coerceAtLeast(0L), runningMillis)
    }

    private fun saturatedAdd(left: Long, right: Long): Long =
        if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
}
