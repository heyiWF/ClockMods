package com.clockmods.ultimate.compose

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockmods.sdk.style.MarqueeSpec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarLabelMotionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun longEventHoldsItsSuffixBeforeAdvancing() {
        val title = "Long imported event with a distinct ending"
        val spec = MarqueeSpec.DEFAULT
        var hold = 0L
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val style = TextStyle(fontSize = 16.sp)
            val density = LocalDensity.current
            val text = rememberTextMeasurer().measure(title, style = style, softWrap = false)
            hold = spec.holdMillis(text.size.width - with(density) { 72.dp.toPx() }, density.density)
            CompositionLocalProvider(LocalCalendarMotionActive provides true) {
                LunarCarouselText(listOf(title, "Next"), Color.Black, style,
                    Modifier.width(72.dp).background(Color.White).testTag("field"))
            }
        }
        compose.mainClock.advanceTimeBy(100)
        val field = compose.onNodeWithTag("field")
        val head = field.captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(hold - 800)
        val tail = field.captureToImage().asAndroidBitmap()
        assertFalse(head.sameAs(tail))
        compose.mainClock.advanceTimeBy(450)
        assertTrue(tail.sameAs(field.captureToImage().asAndroidBitmap()))
        compose.mainClock.advanceTimeBy(1100)
        compose.onNodeWithText("Next").assertExists()
        compose.onNodeWithText(title).assertDoesNotExist()
    }
}
