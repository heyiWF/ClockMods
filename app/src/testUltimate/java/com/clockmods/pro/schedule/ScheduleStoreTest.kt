package com.clockmods.pro.schedule

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleStoreTest {
    @Test
    fun itemLimitOnlyRejectsNewItems() {
        assertTrue(ScheduleStore.canSave(ScheduleStore.MAX_ITEMS_PER_DAY - 1, false))
        assertFalse(ScheduleStore.canSave(ScheduleStore.MAX_ITEMS_PER_DAY, false))
        assertTrue(ScheduleStore.canSave(ScheduleStore.MAX_ITEMS_PER_DAY, true))
    }
}
