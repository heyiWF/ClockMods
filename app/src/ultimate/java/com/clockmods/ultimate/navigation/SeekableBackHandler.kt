package com.clockmods.ultimate.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Reverse the existing preview timeline; changing its target would replay an enter transition. */
internal suspend fun <T> SeekableTransitionState<T>.rewindBackPreview(origin: T) {
    val initialFraction = fraction
    if (initialFraction > 0f) {
        val durationNanos = (180_000_000L * initialFraction).toLong().coerceAtLeast(1L)
        val started = withFrameNanos { it }
        do {
            val elapsed = withFrameNanos { it } - started
            val time = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
            seekTo(initialFraction * (1f - LinearOutSlowInEasing.transform(time)))
        } while (elapsed < durationNanos)
    }
    snapTo(origin)
}

@Composable
internal fun <T> SeekableBackHandler(
    enabled: Boolean,
    state: SeekableTransitionState<T>,
    current: T,
    previous: T,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rewind by remember { mutableStateOf<Job?>(null) }
    PredictiveBackHandler(enabled) { events ->
        // A new gesture owns the timeline immediately, including during cancellation recovery.
        rewind?.cancel()
        rewind?.join()
        if (state.currentState != current) state.snapTo(current)
        try {
            events.collect { state.seekTo(it.progress.coerceIn(0f, 1f), previous) }
            onBack()
        } catch (cancelled: CancellationException) {
            rewind = scope.launch { state.rewindBackPreview(current) }
            throw cancelled
        }
    }
}
