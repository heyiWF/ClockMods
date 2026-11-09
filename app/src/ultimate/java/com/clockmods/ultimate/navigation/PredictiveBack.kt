package com.clockmods.ultimate.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Register inside dialog content so the callback belongs to its window, not the Activity. */
@Composable
internal fun PredictiveDialogBack(onDismiss: () -> Unit) {
    val view = LocalView.current
    val dismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(view) {
        val registration = DialogBackAnimation.install(view) { dismiss() }
        onDispose { registration.close() }
    }
}

@Composable
internal fun predictiveBackProgress(enabled: Boolean, onBack: () -> Unit): Float {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val action by rememberUpdatedState(onBack)
    PredictiveBackHandler(enabled) { events ->
        try {
            progress.stop()
            events.collect { progress.snapTo(it.progress) }
            action()
            progress.snapTo(0f)
        } catch (cancelled: CancellationException) {
            scope.launch { progress.animateTo(0f, tween(180)) }
        }
    }
    return progress.value
}
