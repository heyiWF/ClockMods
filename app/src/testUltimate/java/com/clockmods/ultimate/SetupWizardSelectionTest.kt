package com.clockmods.ultimate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupWizardSelectionTest {
    @Test
    fun eachRequiredChoiceBlocksNextUntilSelected() {
        assertFalse(canContinueSetupStep(0, null, false, null, null, null))
        assertTrue(canContinueSetupStep(0, "zh-Hans", false, null, null, null))

        // Weather off has no temperature choice; weather on requires one.
        assertTrue(canContinueSetupStep(1, "zh-Hans", false, null, null, null))
        assertFalse(canContinueSetupStep(1, "zh-Hans", true, null, null, null))
        assertTrue(canContinueSetupStep(1, "zh-Hans", true, "celsius", null, null))

        assertFalse(canContinueSetupStep(2, "zh-Hans", false, null, null, null))
        assertTrue(canContinueSetupStep(2, "zh-Hans", false, null, "pro.classic", null))
        assertFalse(canContinueSetupStep(3, "zh-Hans", false, null, "pro.classic", null))
        assertTrue(canContinueSetupStep(3, "zh-Hans", false, null, "pro.classic", "calendar.graphite"))
    }
}
