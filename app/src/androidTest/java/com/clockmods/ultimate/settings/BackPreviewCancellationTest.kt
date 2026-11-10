package com.clockmods.ultimate.settings

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.clockmods.ultimate.navigation.rewindBackPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BackPreviewCancellationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun partialCancellationOnlyRewindsExistingTimeline() = checkCancellation(.65f)
    @Test fun cancellationAtEdgeDoesNotReplayEnterAnimation() = checkCancellation(0f)

    private fun checkCancellation(progress: Float) {
        val state = SeekableTransitionState(1)
        lateinit var scope: CoroutineScope
        var position = 1f
        compose.setContent {
            scope = rememberCoroutineScope()
            val transition = rememberTransition(state)
            val value by transition.animateFloat(transitionSpec = { tween(260) }) { it.toFloat() }
            SideEffect { position = value }
        }
        compose.runOnIdle { scope.launch { state.seekTo(progress, 0) } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        var previous = position
        lateinit var recovery: Job
        compose.runOnIdle { recovery = scope.launch { state.rewindBackPreview(1) } }
        repeat(24) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle {
                assertTrue("Cancellation moved toward the previous page again", position + .001f >= previous)
                assertTrue("Cancellation overshot the current page", position <= 1.001f)
                if (!recovery.isCompleted) assertEquals("Preview target changed into a new enter transition", 0, state.targetState)
                previous = position
            }
        }
        compose.runOnIdle {
            assertTrue(recovery.isCompleted)
            assertEquals(1, state.currentState)
            assertEquals(1, state.targetState)
            assertEquals(1f, position, .001f)
        }
    }
}
