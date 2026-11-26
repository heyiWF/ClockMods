package com.clockmods.ultimate.settings

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.clockmods.ultimate.navigation.BackPreviewStack
import com.clockmods.ultimate.navigation.PageBackPreview
import com.clockmods.ultimate.navigation.rememberPageBackPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Frame by frame Back preview: cancellation only rewinds, commits only leave, new gestures win. */
class PageBackPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var preview: PageBackPreview
    private var page by mutableStateOf<Int?>(1)
    private var backs = 0

    private fun showStack() {
        compose.setContent {
            preview = rememberPageBackPreview()
            BackPreviewStack(
                preview = preview,
                page = page,
                onBack = { page = null; backs++ },
                modifier = Modifier.fillMaxSize(),
                beneath = { Box(Modifier.fillMaxSize().background(Color.DarkGray)) },
                top = { _, _ -> Box(Modifier.fillMaxSize().background(Color.Blue)) },
            )
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun start(edge: Int = BackEventCompat.EDGE_LEFT) = compose.runOnUiThread {
        compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(
            BackEventCompat(2f, 600f, 0f, edge),
        )
    }

    private fun progress(value: Float, edge: Int = BackEventCompat.EDGE_LEFT) =
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(2f + 400f * value, 600f, value, edge),
            )
        }

    private fun frame() = compose.mainClock.advanceTimeByFrame()

    @Test fun cancellationOnlyRewinds() {
        showStack()
        start()
        progress(0.2f); frame()
        progress(0.55f); frame()
        progress(0.9f); frame()
        assertTrue("Gesture did not shrink the page", preview.offset.value > 0f)
        assertTrue(preview.card)
        val beforeCancel = preview.offset.value
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        var previous = beforeCancel
        repeat(40) {
            frame()
            val value = preview.offset.value
            assertTrue("Rewind moved away from the page", value <= previous + 0.01f)
            previous = value
        }
        assertEquals(0f, preview.offset.value, 0.01f)
        assertEquals(0, backs)
        assertEquals(1, page)
        assertFalse("Rewind left the page as a card", preview.card)
    }

    @Test fun commitSlidesAwayAndLeavesOnce() {
        showStack()
        start()
        progress(0.5f); frame()
        val beforeCommit = preview.offset.value
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        var previous = beforeCommit
        repeat(60) {
            frame()
            val value = preview.offset.value
            assertTrue("Commit moved backwards", value >= previous - 0.01f)
            previous = value
        }
        assertEquals(1, backs)
        assertNull(page)
        assertTrue("Commit never slid the page away", preview.offset.value > preview.widthPx)
    }

    @Test fun newGestureDuringRewindOwnsTheTimeline() {
        showStack()
        start()
        progress(0.8f); frame()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        frame(); frame()
        start()
        progress(0.3f)
        val eased = ((0.3f - 0.015f) / (1f - 0.015f)).coerceIn(0f, 1f)
        val expected = preview.shiftPx * CubicBezierEasing(0f, 0f, 0f, 1f).transform(eased)
        assertEquals(expected, preview.offset.value, 1f)
        val snapped = preview.offset.value
        repeat(10) {
            frame()
            assertTrue("Old rewind kept running", preview.offset.value <= snapped + 0.01f)
        }
        assertEquals(0, backs)
        assertEquals(1, page)
    }
}
