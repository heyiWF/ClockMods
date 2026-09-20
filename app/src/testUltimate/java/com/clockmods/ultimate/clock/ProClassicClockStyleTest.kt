package com.clockmods.ultimate.clock

import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.ClockStyleMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class ProClassicClockStyleTest {
    @Test
    fun freshRegistryExposesProClassicWithoutChangingFallback() {
        val registry = UltimateClockStyles.createRegistry()

        assertEquals(UltimateClockStyles.STYLE_GLASS_ATELIER, registry.getFallbackId())
        val proClassic = registry.find(UltimateClockStyles.STYLE_PRO_CLASSIC)
        assertNotNull(proClassic)
        assertSame(proClassic, registry.resolve(UltimateClockStyles.STYLE_PRO_CLASSIC))
        assertEquals(
            UltimateClockStyles.STYLE_GLASS_ATELIER,
            registry.resolve("unknown.style").getMetadata().getId(),
        )

        val metadata = proClassic!!.getMetadata()
        assertEquals(ClockStyleMetadata.Kind.DIGITAL, metadata.getKind())
        assertEquals(
            setOf(
                ClockStyleCapabilities.Capability.SECONDS,
                ClockStyleCapabilities.Capability.DATE,
                ClockStyleCapabilities.Capability.TIME_ZONE,
                ClockStyleCapabilities.Capability.WEATHER,
                ClockStyleCapabilities.Capability.STATUS,
                ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
            ),
            metadata.getCapabilities().asSet(),
        )
    }
}
