package com.clockmods.ultimate.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/** Virtual travel, in px, over which a Back gesture only shrinks the page it started on. */
private val GESTURE_SHIFT = 56.dp
/** Distance from the edge opposite the gesture at which the shrinking page stays anchored. */
private val CARD_ANCHOR = 82.dp
private val CARD_CORNER = 32.dp
private const val CARD_SHRINK = 0.10f
private const val SCRIM = 0.47f
/** Gestures barely past the edge must not prepare a preview. */
private const val LAZY_START = 0.015f
private val StandardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
private val Commit = tween<Float>(380, easing = EaseOutQuint)
private val Rewind = tween<Float>(320, easing = EaseOutQuint)
private val Slide = tween<Float>(260)

@Stable
internal class PageBackPreview internal constructor(val shiftPx: Float) {
    val offset = Animatable(0f)
    var widthPx by mutableFloatStateOf(0f)
    var topInWindow by mutableFloatStateOf(0f)
    /** The page shrinks toward the anchor instead of sliding as a plain sheet. */
    var card by mutableStateOf(false)
    var fromLeft by mutableStateOf(true)
    var pivotY by mutableFloatStateOf(Float.NaN)
    var recovery: Job? = null
}

@Composable
internal fun rememberPageBackPreview(): PageBackPreview {
    val density = LocalDensity.current
    return remember(density) { PageBackPreview(with(density) { GESTURE_SHIFT.toPx() }) }
}

/**
 * In-page navigation whose Back preview follows Telegram: the page beneath never moves, and the
 * page on top shrinks into a rounded card that only slides away once the gesture commits.
 * [onBack] must clear [page]; the top page stays composed until its exit animation ends.
 */
@Composable
internal fun <T> BackPreviewStack(
    preview: PageBackPreview,
    page: T?,
    onBack: () -> Unit,
    modifier: Modifier,
    beneath: @Composable () -> Unit,
    top: @Composable (page: T, back: () -> Unit) -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(page) }
    var exiting by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf(true) }
    val current by rememberUpdatedState(page)

    fun leave() {
        if (exiting) return
        exiting = true
        val leaving = shown
        scope.launch {
            preview.recovery?.cancelAndJoin()
            // A gesture that already shrank the page finishes by sliding the shrunken page away.
            if (preview.card) preview.offset.animateTo(preview.widthPx + preview.shiftPx, Commit)
            else preview.offset.animateTo(preview.widthPx, Slide)
            preview.card = false
            if (current == leaving && leaving != null) onBack()
            if (shown == leaving) shown = null
            exiting = false
        }
    }

    LaunchedEffect(page) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        val target = page ?: return@LaunchedEffect
        // A forward selection owns the timeline, even while the previous page is still leaving.
        exiting = false
        if (shown == target) {
            preview.offset.animateTo(0f, Slide)
        } else {
            shown = target
            preview.card = false
            preview.offset.snapTo(preview.widthPx)
            preview.offset.animateTo(0f, Slide)
        }
        preview.card = false
    }

    PredictiveBackHandler(page != null && !exiting) { events ->
        // A new gesture owns the timeline immediately, including during cancellation recovery.
        preview.recovery?.cancelAndJoin()
        var previewed = false
        try {
            events.collect { event ->
                if (!previewed) {
                    previewed = true
                    preview.card = true
                    preview.fromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT
                    val window = IntArray(2)
                    view.getLocationOnScreen(window)
                    preview.pivotY = event.touchY - window[1] - preview.topInWindow
                }
                val progress = ((event.progress - LAZY_START) / (1f - LAZY_START)).coerceIn(0f, 1f)
                preview.offset.snapTo(preview.shiftPx * StandardDecelerate.transform(progress))
            }
        } catch (cancelled: CancellationException) {
            if (current != null) {
                preview.recovery = scope.launch {
                    preview.offset.animateTo(0f, Rewind)
                    preview.card = false
                }
            }
            throw cancelled
        }
        leave()
    }

    Box(modifier.fillMaxSize().onGloballyPositioned { preview.widthPx = it.size.width.toFloat() }) {
        val hidden = shown != null
        Box(
            Modifier.fillMaxSize()
                .then(if (hidden) Modifier.semantics { hideFromAccessibility() } else Modifier)
                .drawWithContent { if (!hidden || preview.offset.value > 0f) drawContent() },
        ) { beneath() }
        if (hidden) Box(Modifier.fillMaxSize().pageBackScrim(preview))
        shown?.let { current ->
            key(current) {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned { preview.topInWindow = it.boundsInWindow().top }
                        .pageBackCard(preview),
                ) { top(current, ::leave) }
            }
        }
    }
}

@Composable
private fun Modifier.pageBackCard(preview: PageBackPreview): Modifier {
    val corner = with(LocalDensity.current) { CARD_CORNER.toPx() }
    return this.graphicsLayer {
        if (size.width <= 0f) return@graphicsLayer
        val travel = preview.offset.value
        val shrink = if (preview.card) (travel / preview.shiftPx).coerceIn(0f, 1f) else 0f
        val anchor = CARD_ANCHOR.toPx()
        transformOrigin = TransformOrigin(
            if (preview.fromLeft) 1f - anchor / size.width else anchor / size.width,
            if (preview.pivotY.isNaN()) 0.5f else (preview.pivotY / size.height).coerceIn(0f, 1f),
        )
        scaleX = 1f - CARD_SHRINK * shrink
        scaleY = scaleX
        translationX = if (preview.card) (travel - preview.shiftPx).coerceAtLeast(0f) else travel
        // Corners grow with the shrink, so a page at rest keeps its square corners.
        shape = RoundedCornerShape(corner * shrink)
        clip = travel > 0f
    }
}

private fun Modifier.pageBackScrim(preview: PageBackPreview): Modifier = drawWithContent {
    val alpha = SCRIM * ((size.width - preview.offset.value) / size.width).coerceIn(0f, 0.8f)
    if (alpha > 0f) drawRect(Color.Black.copy(alpha = alpha))
}
