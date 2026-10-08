package com.clockmods.ultimate.compose

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clockmods.sdk.style.MarqueeSpec

internal val LocalCalendarMarquee = staticCompositionLocalOf { MarqueeSpec.DEFAULT }
internal val LocalCalendarMotionActive = staticCompositionLocalOf { false }

/** The host owns motion activation; previews and disposed pages do not keep ticking. */
@Composable
internal fun CalendarMotionProvider(spec: MarqueeSpec, content: @Composable () -> Unit) {
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    CompositionLocalProvider(LocalCalendarMarquee provides spec,
        LocalCalendarMotionActive provides resumed, content = content)
}
