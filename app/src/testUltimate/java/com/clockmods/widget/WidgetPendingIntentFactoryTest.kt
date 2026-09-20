package com.clockmods.widget

import com.clockmods.ultimate.ComposeMainActivity
import com.clockmods.widget.render.WidgetPendingIntentFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPendingIntentFactoryTest {
    @Test
    fun actionsAndInstancesNeverShareIdentity() {
        val codes = HashSet<Int>()
        val uris = HashSet<String>()
        for (id in 1 until 1000) {
            for (action in WidgetPendingIntentFactory.Action.values()) {
                assertTrue(codes.add(WidgetPendingIntentFactory.requestCode(id, action)))
                assertTrue(uris.add(WidgetPendingIntentFactory.identity(id, action)))
            }
        }
    }

    @Test
    fun clockDestinationsMapToExplicitMainRoutes() {
        assertEquals(
            ComposeMainActivity.DESTINATION_CLOCK,
            WidgetPendingIntentFactory.destination(WidgetPendingIntentFactory.Action.CLOCK),
        )
        assertEquals(
            ComposeMainActivity.DESTINATION_CALENDAR,
            WidgetPendingIntentFactory.destination(WidgetPendingIntentFactory.Action.CALENDAR),
        )
        assertNull(WidgetPendingIntentFactory.destination(WidgetPendingIntentFactory.Action.WEATHER))
        assertNull(WidgetPendingIntentFactory.destination(WidgetPendingIntentFactory.Action.CONFIG))
        assertNull(WidgetPendingIntentFactory.destination(WidgetPendingIntentFactory.Action.REFRESH))
    }

    @Test
    fun launcherAcceptsCurrentExtrasAndLegacyWidgetUriActions() {
        assertEquals(
            ComposeMainActivity.DESTINATION_CALENDAR,
            ComposeMainActivity.resolveDestination(ComposeMainActivity.DESTINATION_CALENDAR, null),
        )
        assertEquals(
            ComposeMainActivity.DESTINATION_CLOCK,
            ComposeMainActivity.resolveDestination(null, "CLOCK"),
        )
        assertEquals(
            ComposeMainActivity.DESTINATION_CALENDAR,
            ComposeMainActivity.resolveDestination(null, "CALENDAR"),
        )
        assertNull(ComposeMainActivity.resolveDestination(null, "CONFIG"))
    }
}
