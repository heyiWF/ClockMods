package com.clockmods.pro.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmNotificationsPolicyTest {
    @Test
    fun fullScreenIntentIsAttachedOnlyWithSpecialAccess() {
        assertFalse(AlarmNotifications.shouldAttachFullScreenIntent(false))
        assertTrue(AlarmNotifications.shouldAttachFullScreenIntent(true))
    }

    /**
     * A background alarm broadcast cannot launch an activity directly on Android 11+.  The UI may
     * only be opened from the receiver when notifications are hidden *and* the full-screen intent
     * route is available, because only that path carries a launch exemption.
     */
    @Test
    fun ringingActivityIsOpenedOnlyWhenNotificationsAreHiddenAndFullScreenIntentWorks() {
        assertTrue(AlarmNotifications.shouldOpenRingingActivityFromReceiver(false, true))
        assertFalse(AlarmNotifications.shouldOpenRingingActivityFromReceiver(true, true))
        assertFalse(AlarmNotifications.shouldOpenRingingActivityFromReceiver(true, false))
        assertFalse(AlarmNotifications.shouldOpenRingingActivityFromReceiver(false, false))
    }
}
