package com.clockmods.ultimate.clock;

import android.graphics.Canvas;

import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockRenderer;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.sdk.clock.ClockStyleCapabilities;
import com.clockmods.sdk.clock.ClockStyleMetadata;
import com.clockmods.sdk.clock.ClockStyleRegistry;
import com.clockmods.sdk.clock.ClockThemeTokens;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ClockStyleRegistryTest {
    @Test
    public void builtInsHaveUniqueCompleteMetadata() {
        List<ClockStyle> styles = UltimateClockStyles.builtIns();
        Assert.assertEquals(12, styles.size());
        Assert.assertEquals(Arrays.asList(
                UltimateClockStyles.STYLE_PRO_CLASSIC,
                UltimateClockStyles.STYLE_GLASS_ATELIER,
                UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
                UltimateClockStyles.STYLE_PAPER_STATION,
                UltimateClockStyles.STYLE_ORBIT_NEON,
                UltimateClockStyles.STYLE_DIGITAL_GRID,
                UltimateClockStyles.STYLE_TYPOGRAPHIC,
                UltimateClockStyles.STYLE_DUAL_BLOCKS,
                UltimateClockStyles.STYLE_ORBIT,
                UltimateClockStyles.STYLE_BUBBLES,
                UltimateClockStyles.STYLE_BLEND,
                UltimateClockStyles.STYLE_RIBBON), styleIds(styles));
        Set<String> ids = new HashSet<String>();
        for (ClockStyle style : styles) {
            ClockStyleMetadata metadata = style.getMetadata();
            Assert.assertTrue(ids.add(metadata.getId()));
            Assert.assertTrue(metadata.getId(), metadata.getId().matches(
                    "[a-z][a-z0-9]*(?:[._-][a-z0-9]+)+"));
            Assert.assertFalse(metadata.getName().isEmpty());
            Assert.assertFalse(metadata.getDescription().isEmpty());
            Assert.assertNotNull(metadata.getKind());
            Assert.assertNotNull(metadata.getCapabilities());
            Assert.assertTrue(metadata.getVersion() >= 1);
            Assert.assertTrue(metadata.getMinApi() >= 1);
            Assert.assertNotNull(style.getThemeTokens());
            Assert.assertNotNull(style.getRenderer());
        }
    }

    @Test
    public void migratedStylesExposeWorldClockAndExpectedSecondMotion() {
        ClockStyleRegistry registry = UltimateClockStyles.createRegistry();
        for (String id : Arrays.asList(UltimateClockStyles.STYLE_DUAL_BLOCKS,
                UltimateClockStyles.STYLE_ORBIT, UltimateClockStyles.STYLE_BUBBLES,
                UltimateClockStyles.STYLE_BLEND, UltimateClockStyles.STYLE_RIBBON)) {
            ClockStyleCapabilities capabilities = registry.find(id).getMetadata().getCapabilities();
            Assert.assertTrue(id, capabilities.supports(
                    ClockStyleCapabilities.Capability.WORLD_CLOCK));
            Assert.assertTrue(id, capabilities.supports(
                    ClockStyleCapabilities.Capability.SECONDS));
            Assert.assertEquals(id, UltimateClockStyles.STYLE_ORBIT.equals(id)
                            || UltimateClockStyles.STYLE_BLEND.equals(id),
                    capabilities.supports(ClockStyleCapabilities.Capability.SMOOTH_SECONDS));
        }
        Assert.assertFalse(registry.find(UltimateClockStyles.STYLE_GLASS_ATELIER)
                .getMetadata().getCapabilities().supports(
                        ClockStyleCapabilities.Capability.WORLD_CLOCK));
    }

    @Test
    public void digitalFacesIgnoreAnAnalogFacesStoredMotionMode() {
        ClockStyleRegistry registry = UltimateClockStyles.createRegistry();
        ClockStyle ribbon = registry.find(UltimateClockStyles.STYLE_RIBBON);

        Assert.assertEquals(ClockState.SecondHandMotion.TICK,
                UltimateClockView.resolveSecondHandMotion(ribbon, true,
                        ClockState.SecondHandMotion.OFF));
        Assert.assertEquals(ClockState.SecondHandMotion.OFF,
                UltimateClockView.resolveSecondHandMotion(ribbon, false,
                        ClockState.SecondHandMotion.SWEEP));
    }

    @Test
    public void analogFacesKeepTheirConfiguredMotionMode() {
        ClockStyleRegistry registry = UltimateClockStyles.createRegistry();
        ClockStyle glass = registry.find(UltimateClockStyles.STYLE_GLASS_ATELIER);

        Assert.assertEquals(ClockState.SecondHandMotion.OFF,
                UltimateClockView.resolveSecondHandMotion(glass, true,
                        ClockState.SecondHandMotion.OFF));
        Assert.assertEquals(ClockState.SecondHandMotion.SWEEP,
                UltimateClockView.resolveSecondHandMotion(glass, true,
                        ClockState.SecondHandMotion.SWEEP));
    }

    @Test
    public void registryRejectsDuplicateIdentifiers() {
        ClockStyleRegistry registry = new ClockStyleRegistry();
        registry.register(fakeStyle("sample.clock", 14));
        try {
            registry.register(fakeStyle("sample.clock", 14));
            Assert.fail("Duplicate style ids must be rejected");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("sample.clock"));
        }
    }

    @Test
    public void registryPreservesRegistrationOrderAndReturnsImmutableSnapshots() {
        ClockStyleRegistry registry = new ClockStyleRegistry();
        ClockStyle first = fakeStyle("first.clock", 14);
        ClockStyle second = fakeStyle("second.clock", 14);
        registry.register(first).register(second);

        List<ClockStyle> snapshot = registry.getStyles();
        Assert.assertEquals(Arrays.asList("first.clock", "second.clock"), styleIds(snapshot));
        try {
            snapshot.add(fakeStyle("mutating.clock", 14));
            Assert.fail("Registry style snapshots must be immutable");
        } catch (UnsupportedOperationException expected) {
            Assert.assertEquals(2, snapshot.size());
        }

        registry.register(fakeStyle("third.clock", 14));
        Assert.assertEquals(2, snapshot.size());
        Assert.assertEquals(Arrays.asList("first.clock", "second.clock", "third.clock"),
                styleIds(registry.getStyles()));
    }

    @Test
    public void firstRegistrationIsFallbackUntilExplicitlyChanged() {
        ClockStyleRegistry registry = new ClockStyleRegistry();
        ClockStyle first = fakeStyle("first.clock", 14);
        ClockStyle second = fakeStyle("second.clock", 14);
        registry.register(first).register(second);

        Assert.assertEquals("first.clock", registry.getFallbackId());
        Assert.assertSame(first, registry.resolve(null));
        registry.setFallback("second.clock");
        Assert.assertEquals("second.clock", registry.getFallbackId());
        Assert.assertSame(second, registry.resolve("missing.clock"));

        try {
            registry.setFallback("unregistered.clock");
            Assert.fail("Fallback must already be registered");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("second.clock", registry.getFallbackId());
        }
    }

    @Test
    public void registryResolvesUnknownAndUnsupportedStylesToFallback() {
        ClockStyleRegistry registry = new ClockStyleRegistry();
        ClockStyle fallback = fakeStyle("fallback.clock", 14);
        ClockStyle future = fakeStyle("future.clock", 35);
        registry.register(fallback).register(future).setFallback("fallback.clock");

        Assert.assertSame(fallback, registry.resolve("missing.clock"));
        Assert.assertSame(future, registry.resolve("future.clock"));
        Assert.assertSame(fallback, registry.resolveForApi("future.clock", 31));
        Assert.assertSame(future, registry.resolveForApi("future.clock", 35));
    }

    @Test
    public void metadataValidatesSdkFacingIdentityAndCompatibility() {
        ClockStyleMetadata metadata = fakeStyle("partner.meridian", 23).getMetadata();
        Assert.assertEquals("partner.meridian", metadata.getId());
        Assert.assertEquals(ClockStyleMetadata.Kind.DIGITAL, metadata.getKind());
        Assert.assertTrue(metadata.getCapabilities().supports(
                ClockStyleCapabilities.Capability.DATE));
        Assert.assertFalse(metadata.supportsApi(22));
        Assert.assertTrue(metadata.supportsApi(23));

        try {
            fakeStyle("Invalid Id", 14);
            Assert.fail("Invalid public style ids must be rejected");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("lowercase"));
        }
        try {
            fakeStyle("unscoped", 14);
            Assert.fail("Public style ids must include a namespace");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("namespaced"));
        }
    }

    @Test
    public void sharedAndIsolatedRegistriesHaveExplicitLifetimes() {
        Assert.assertSame(UltimateClockStyles.sharedRegistry(),
                UltimateClockStyles.sharedRegistry());
        ClockStyleRegistry isolated = UltimateClockStyles.createRegistry();
        Assert.assertNotSame(UltimateClockStyles.sharedRegistry(), isolated);
        Assert.assertEquals(styleIds(UltimateClockStyles.builtIns()),
                styleIds(isolated.getStyles()));
    }

    private static ClockStyle fakeStyle(final String id, int minApi) {
        final ClockStyleMetadata metadata = new ClockStyleMetadata(id, "Sample", "Test style",
                ClockStyleMetadata.Kind.DIGITAL,
                ClockStyleCapabilities.of(ClockStyleCapabilities.Capability.DATE), 1, minApi);
        final ClockThemeTokens tokens = ClockThemeTokens.builder().build();
        final ClockRenderer renderer = new ClockRenderer() {
            @Override public void render(Canvas canvas, ClockRenderContext context,
                    ClockState state, ClockThemeTokens theme) { }
        };
        return new ClockStyle() {
            @Override public ClockStyleMetadata getMetadata() { return metadata; }
            @Override public ClockThemeTokens getThemeTokens() { return tokens; }
            @Override public ClockRenderer getRenderer() { return renderer; }
        };
    }

    private static List<String> styleIds(List<ClockStyle> styles) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<String>(styles.size());
        for (ClockStyle style : styles) ids.add(style.getMetadata().getId());
        return ids;
    }
}
