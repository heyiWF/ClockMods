package com.clockmods.ultimate.clock

import android.graphics.Canvas
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockRenderer
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockStyle
import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.ClockStyleMetadata
import com.clockmods.sdk.clock.ClockStyleRegistry
import com.clockmods.sdk.clock.ClockThemeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.HashSet

class ClockStyleRegistryTest {
    @Test
    fun builtInsHaveUniqueCompleteMetadata() {
        val styles = UltimateClockStyles.builtIns()
        assertEquals(12, styles.size)
        assertEquals(
            listOf(
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
                UltimateClockStyles.STYLE_RIBBON,
            ),
            styleIds(styles),
        )
        val ids = HashSet<String>()
        for (style in styles) {
            val metadata = style.getMetadata()
            assertTrue(ids.add(metadata.getId()))
            assertTrue(metadata.getId(), metadata.getId().matches(Regex("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)+")))
            assertTrue(metadata.getName().isNotEmpty())
            assertTrue(metadata.getDescription().isNotEmpty())
            assertNotNull(metadata.getKind())
            assertNotNull(metadata.getCapabilities())
            assertTrue(metadata.getVersion() >= 1)
            assertTrue(metadata.getMinApi() >= 1)
            assertNotNull(style.getThemeTokens())
            assertNotNull(style.getRenderer())
        }
    }

    @Test
    fun migratedStylesExposeWorldClockAndExpectedSecondMotion() {
        val registry = UltimateClockStyles.createRegistry()
        for (id in listOf(UltimateClockStyles.STYLE_DUAL_BLOCKS, UltimateClockStyles.STYLE_ORBIT, UltimateClockStyles.STYLE_BUBBLES, UltimateClockStyles.STYLE_BLEND, UltimateClockStyles.STYLE_RIBBON)) {
            val capabilities = registry.find(id)!!.getMetadata().getCapabilities()
            assertTrue(id, capabilities.supports(ClockStyleCapabilities.Capability.WORLD_CLOCK))
            assertTrue(id, capabilities.supports(ClockStyleCapabilities.Capability.SECONDS))
            assertEquals(id == UltimateClockStyles.STYLE_ORBIT || id == UltimateClockStyles.STYLE_BLEND, capabilities.supports(ClockStyleCapabilities.Capability.SMOOTH_SECONDS))
        }
        assertFalse(registry.find(UltimateClockStyles.STYLE_GLASS_ATELIER)!!.getMetadata().getCapabilities().supports(ClockStyleCapabilities.Capability.WORLD_CLOCK))
    }

    @Test
    fun digitalFacesIgnoreAnAnalogFacesStoredMotionMode() {
        val ribbon = UltimateClockStyles.createRegistry().find(UltimateClockStyles.STYLE_RIBBON)!!
        assertEquals(ClockState.SecondHandMotion.TICK, ClockMotionResolver.resolve(ribbon, true, ClockState.SecondHandMotion.OFF))
        assertEquals(ClockState.SecondHandMotion.OFF, ClockMotionResolver.resolve(ribbon, false, ClockState.SecondHandMotion.SWEEP))
    }

    @Test
    fun analogFacesKeepTheirConfiguredMotionMode() {
        val glass = UltimateClockStyles.createRegistry().find(UltimateClockStyles.STYLE_GLASS_ATELIER)!!
        assertEquals(ClockState.SecondHandMotion.OFF, ClockMotionResolver.resolve(glass, true, ClockState.SecondHandMotion.OFF))
        assertEquals(ClockState.SecondHandMotion.SWEEP, ClockMotionResolver.resolve(glass, true, ClockState.SecondHandMotion.SWEEP))
    }

    @Test
    fun registryRejectsDuplicateIdentifiers() {
        val registry = ClockStyleRegistry()
        registry.register(fakeStyle("sample.clock", 14))
        try {
            registry.register(fakeStyle("sample.clock", 14))
            fail("Duplicate style ids must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("sample.clock"))
        }
    }

    @Test
    fun registryPreservesRegistrationOrderAndReturnsImmutableSnapshots() {
        val registry = ClockStyleRegistry()
        val first = fakeStyle("first.clock", 14)
        val second = fakeStyle("second.clock", 14)
        registry.register(first).register(second)
        val snapshot = registry.getStyles()
        assertEquals(listOf("first.clock", "second.clock"), styleIds(snapshot))
        try {
            (snapshot as MutableList).add(fakeStyle("mutating.clock", 14))
            fail("Registry style snapshots must be immutable")
        } catch (expected: UnsupportedOperationException) {
            assertEquals(2, snapshot.size)
        }
        registry.register(fakeStyle("third.clock", 14))
        assertEquals(2, snapshot.size)
        assertEquals(listOf("first.clock", "second.clock", "third.clock"), styleIds(registry.getStyles()))
    }

    @Test
    fun firstRegistrationIsFallbackUntilExplicitlyChanged() {
        val registry = ClockStyleRegistry()
        val first = fakeStyle("first.clock", 14)
        val second = fakeStyle("second.clock", 14)
        registry.register(first).register(second)
        assertEquals("first.clock", registry.getFallbackId())
        assertSame(first, registry.resolve(null))
        registry.setFallback("second.clock")
        assertEquals("second.clock", registry.getFallbackId())
        assertSame(second, registry.resolve("missing.clock"))
        try {
            registry.setFallback("unregistered.clock")
            fail("Fallback must already be registered")
        } catch (_: IllegalArgumentException) {
            assertEquals("second.clock", registry.getFallbackId())
        }
    }

    @Test
    fun registryResolvesUnknownAndUnsupportedStylesToFallback() {
        val registry = ClockStyleRegistry()
        val fallback = fakeStyle("fallback.clock", 14)
        val future = fakeStyle("future.clock", 35)
        registry.register(fallback).register(future).setFallback("fallback.clock")
        assertSame(fallback, registry.resolve("missing.clock"))
        assertSame(future, registry.resolve("future.clock"))
        assertSame(fallback, registry.resolveForApi("future.clock", 31))
        assertSame(future, registry.resolveForApi("future.clock", 35))
    }

    @Test
    fun metadataValidatesSdkFacingIdentityAndCompatibility() {
        val metadata = fakeStyle("partner.meridian", 23).getMetadata()
        assertEquals("partner.meridian", metadata.getId())
        assertEquals(ClockStyleMetadata.Kind.DIGITAL, metadata.getKind())
        assertTrue(metadata.getCapabilities().supports(ClockStyleCapabilities.Capability.DATE))
        assertFalse(metadata.supportsApi(22))
        assertTrue(metadata.supportsApi(23))
        try {
            fakeStyle("Invalid Id", 14)
            fail("Invalid public style ids must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("lowercase"))
        }
        try {
            fakeStyle("unscoped", 14)
            fail("Public style ids must include a namespace")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("namespaced"))
        }
    }

    @Test
    fun sharedAndIsolatedRegistriesHaveExplicitLifetimes() {
        assertSame(UltimateClockStyles.sharedRegistry(), UltimateClockStyles.sharedRegistry())
        val isolated = UltimateClockStyles.createRegistry()
        assertNotSame(UltimateClockStyles.sharedRegistry(), isolated)
        assertEquals(styleIds(UltimateClockStyles.builtIns()), styleIds(isolated.getStyles()))
    }

    private fun fakeStyle(id: String, minApi: Int): ClockStyle {
        val metadata = ClockStyleMetadata(
            id, "Sample", "Test style", ClockStyleMetadata.Kind.DIGITAL,
            ClockStyleCapabilities.of(ClockStyleCapabilities.Capability.DATE), 1, minApi,
        )
        val tokens = ClockThemeTokens.builder().build()
        val renderer = ClockRenderer { _, _, _, _ -> }
        return object : ClockStyle {
            override fun getMetadata() = metadata
            override fun getThemeTokens() = tokens
            override fun getRenderer() = renderer
        }
    }

    private fun styleIds(styles: List<ClockStyle>): List<String> = styles.map { it.getMetadata().getId() }
}
