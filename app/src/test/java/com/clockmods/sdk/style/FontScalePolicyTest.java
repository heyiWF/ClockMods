package com.clockmods.sdk.style;

import org.junit.Test;
import static org.junit.Assert.*;

public class FontScalePolicyTest {
    @Test public void preservesCapsThroughOneHundredPercent() {
        assertEquals(40f, FontScalePolicy.cap(80f, 40f, .5f), 0f);
        assertEquals(40f, FontScalePolicy.cap(80f, 40f, 1f), 0f);
    }

    @Test public void removesCapsOnlyAboveOneHundredPercent() {
        assertEquals(80f, FontScalePolicy.cap(80f, 40f, 1.01f), 0f);
        assertEquals(120f, FontScalePolicy.cap(120f, 40f, 1.5f), 0f);
        assertEquals(160f, FontScalePolicy.cap(160f, 40f, 2f), 0f);
    }

    @Test public void invalidScalesNeverDisableProtection() {
        assertFalse(FontScalePolicy.allowsOverflow(Float.NaN));
        assertFalse(FontScalePolicy.allowsOverflow(Float.POSITIVE_INFINITY));
    }
}
