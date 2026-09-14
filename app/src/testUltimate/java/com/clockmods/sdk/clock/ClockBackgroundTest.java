package com.clockmods.sdk.clock;

import org.junit.Assert;
import org.junit.Test;

public class ClockBackgroundTest {
    @Test
    public void exposesThreeStableModes() {
        Assert.assertArrayEquals(new ClockBackground.Mode[] {
                ClockBackground.Mode.THEME,
                ClockBackground.Mode.COLOR,
                ClockBackground.Mode.IMAGE
        }, ClockBackground.Mode.values());
    }

    @Test
    public void themeBackgroundUsesNeutralDefaults() {
        ClockBackground background = ClockBackground.theme(false);

        Assert.assertEquals(ClockBackground.Mode.THEME, background.getMode());
        Assert.assertTrue(background.usesThemeSurface());
        Assert.assertFalse(background.hasImage());
        Assert.assertNull(background.getBitmap());
        Assert.assertEquals(0, background.getColor());
        Assert.assertFalse(background.isDimmed());
    }

    @Test
    public void colorBackgroundPreservesColorAndDimPolicy() {
        ClockBackground background = ClockBackground.color(0xFF123456, true);

        Assert.assertEquals(ClockBackground.Mode.COLOR, background.getMode());
        Assert.assertFalse(background.usesThemeSurface());
        Assert.assertFalse(background.hasImage());
        Assert.assertNull(background.getBitmap());
        Assert.assertEquals(0xFF123456, background.getColor());
        Assert.assertTrue(background.isDimmed());
    }

    @Test
    public void missingImageFallsBackToColorMode() {
        ClockBackground background = ClockBackground.image(null, 0xFF654321, true);

        Assert.assertEquals(ClockBackground.Mode.COLOR, background.getMode());
        Assert.assertFalse(background.hasImage());
        Assert.assertEquals(0xFF654321, background.getColor());
        Assert.assertTrue(background.isDimmed());
    }

    @Test
    public void legacyRenderContextHasNoHostBackgroundByDefault() {
        ClockRenderContext context = new ClockRenderContext(
                0f, 0f, 100f, 80f, 1f, 1f, 42L);

        Assert.assertNull(context.getBackground());
    }
}
