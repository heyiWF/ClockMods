package com.clockmods.ultimate;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SetupWizardSelectionTest {
    @Test public void everyRequiredStepNeedsAnExplicitChoice() {
        assertFalse(SetupWizardActivity.canContinueStep(0, null, false, null, null, null));
        assertTrue(SetupWizardActivity.canContinueStep(0, "zh-CN", false, null, null, null));
        assertFalse(SetupWizardActivity.canContinueStep(1, "zh-CN", true, null, null, null));
        assertTrue(SetupWizardActivity.canContinueStep(1, "zh-CN", false, null, null, null));
        assertTrue(SetupWizardActivity.canContinueStep(1, "zh-CN", true, "celsius", null, null));
        assertFalse(SetupWizardActivity.canContinueStep(2, "zh-CN", false, null, null, null));
        assertTrue(SetupWizardActivity.canContinueStep(2, "zh-CN", false, null, "glass.atelier", null));
        assertFalse(SetupWizardActivity.canContinueStep(3, "zh-CN", false, null,
                "glass.atelier", null));
        assertTrue(SetupWizardActivity.canContinueStep(3, "zh-CN", false, null,
                "glass.atelier", "calendar.graphite"));
        assertFalse(SetupWizardActivity.canContinueStep(4, "zh-CN", false, null,
                "glass.atelier", "calendar.graphite"));
    }
}
