package com.clockmods.ultimate.clock;

import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.sdk.clock.ClockStyleCapabilities;
import com.clockmods.sdk.clock.ClockStyleMetadata;
import com.clockmods.sdk.clock.ClockStyleRegistry;

import org.junit.Assert;
import org.junit.Test;

import java.util.EnumSet;

public class ProClassicClockStyleTest {
    @Test
    public void freshRegistryExposesProClassicWithoutChangingFallback() {
        ClockStyleRegistry registry = UltimateClockStyles.createRegistry();

        Assert.assertEquals(UltimateClockStyles.STYLE_GLASS_ATELIER,
                registry.getFallbackId());
        ClockStyle proClassic = registry.find(UltimateClockStyles.STYLE_PRO_CLASSIC);
        Assert.assertNotNull(proClassic);
        Assert.assertSame(proClassic,
                registry.resolve(UltimateClockStyles.STYLE_PRO_CLASSIC));
        Assert.assertEquals(UltimateClockStyles.STYLE_GLASS_ATELIER,
                registry.resolve("unknown.style").getMetadata().getId());

        ClockStyleMetadata metadata = proClassic.getMetadata();
        Assert.assertEquals(ClockStyleMetadata.Kind.DIGITAL, metadata.getKind());
        Assert.assertEquals(EnumSet.of(
                ClockStyleCapabilities.Capability.SECONDS,
                ClockStyleCapabilities.Capability.DATE,
                ClockStyleCapabilities.Capability.TIME_ZONE,
                ClockStyleCapabilities.Capability.WEATHER,
                ClockStyleCapabilities.Capability.STATUS,
                ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR),
                metadata.getCapabilities().asSet());
    }
}
