package com.clockmods.ultimate.compose

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/** Three-page viewport: the neighbouring page is revealed at the exact distance of the drag. */
@Composable
internal fun CalendarSwipePager(
    pageKey: String,
    modifier: Modifier,
    previous: () -> Unit,
    next: () -> Unit,
    adjacent: (Int) -> @Composable () -> Unit,
    current: @Composable () -> Unit,
) {
    val onPrevious by rememberUpdatedState(previous)
    val onNext by rememberUpdatedState(next)
    val scope = rememberCoroutineScope()
    var offset by remember(pageKey) { mutableFloatStateOf(0f) }
    var settling by remember(pageKey) { mutableStateOf(false) }
    BoxWithConstraints(modifier.clipToBounds()) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val threshold = max(with(LocalDensity.current) { 48.dp.toPx() }, width * .20f)
        val gesture = Modifier.pointerInput(pageKey, width) {
            detectHorizontalDragGestures(
                onDragStart = { if (!settling) offset = 0f },
                onHorizontalDrag = { change, distance ->
                    if (!settling) {
                        change.consume()
                        offset = (offset + distance).coerceIn(-width, width)
                    }
                },
                onDragCancel = {
                    scope.launch {
                        settling = true
                        animate(offset, 0f, animationSpec = tween(180)) { value, _ -> offset = value }
                        settling = false
                    }
                },
                onDragEnd = {
                    if (!settling) scope.launch {
                        settling = true
                        val direction = when {
                            offset <= -threshold -> 1
                            offset >= threshold -> -1
                            else -> 0
                        }
                        val target = -direction * width
                        animate(offset, target, animationSpec = tween(220)) { value, _ -> offset = value }
                        Snapshot.withMutableSnapshot {
                            if (direction < 0) onPrevious() else if (direction > 0) onNext()
                            offset = 0f
                            settling = false
                        }
                    }
                },
            )
        }
        Box(Modifier.fillMaxSize().then(gesture)) {
            if (offset > 0f) {
                val page = remember(pageKey, -1) { adjacent(-1) }
                Box(Modifier.fillMaxSize().graphicsLayer { translationX = offset - width }) { page() }
            } else if (offset < 0f) {
                val page = remember(pageKey, 1) { adjacent(1) }
                Box(Modifier.fillMaxSize().graphicsLayer { translationX = offset + width }) { page() }
            }
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = offset }) { current() }
        }
    }
}
