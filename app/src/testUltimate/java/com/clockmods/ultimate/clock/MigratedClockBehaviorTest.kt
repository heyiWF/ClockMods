package com.clockmods.ultimate.clock

import android.graphics.RectF
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.WorldClockEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.HashSet

class MigratedClockBehaviorTest {
    @Test
    fun smoothSecondsIncludeMillisecondsAndTickSecondsDoNot() {
        assertEquals(12.625f, UltimateClockStyles.secondProgress(12, 625, ClockState.SecondHandMotion.SWEEP), 0f)
        assertEquals(12f, UltimateClockStyles.secondProgress(12, 625, ClockState.SecondHandMotion.TICK), 0f)
    }

    @Test
    fun blendDateSplitsOnlyTheAppendedLunarValue() {
        val split = UltimateClockStyles.splitDateAndLunar("2026 / 09 / 11 周五 / 丙午[马]年八月初一")
        assertArrayEquals(arrayOf("2026 / 09 / 11 周五", "丙午[马]年八月初一"), split)
        assertArrayEquals(arrayOf("09 / 11 / 2026", ""), UltimateClockStyles.splitDateAndLunar("09 / 11 / 2026"))
    }

    @Test
    fun ribbonSecondsAreHalfTheHourMinuteTextSize() {
        assertEquals(48f, UltimateClockStyles.ribbonSecondsTextSize(96f), 0f)
        assertEquals(0f, UltimateClockStyles.ribbonSecondsTextSize(-1f), 0f)
    }

    @Test
    fun portraitBubblesStackHourAndMinuteAndUseAvailableHeight() {
        val geometry = UltimateClockStyles.bubblesPortraitGeometry(900f, 1276f, 2.5f)
        val hourBottom = geometry[1]
        val minuteTop = geometry[2] - geometry[3]
        val minuteBottom = geometry[2] + geometry[3]
        assertTrue(hourBottom + geometry[4] <= minuteTop + .01f)
        assertTrue(minuteBottom <= 1276f * .91f + .01f)
        assertTrue(minuteBottom >= 1276f * .85f)
    }

    @Test
    fun portraitRibbonKeepsAWideCapsuleProfile() {
        val geometry = UltimateClockStyles.ribbonPortraitGeometry(900f, 1276f)
        val outerWidth = 900f * .91f
        val ribbonWidth = 900f * .853f
        assertTrue(outerWidth / geometry[1] > 2f)
        assertTrue(ribbonWidth / geometry[2] > 3f)
        assertTrue(geometry[2] <= 900f * .27f)
    }

    @Test
    fun worldClockStripStaysInsidePortraitAndLandscapeSafeBounds() {
        val portrait: RectF = UltimateClockStyles.worldClockStripBounds(0f, 0f, 1080f, 2400f, 3f, 84f)
        assertEquals(0f, portrait.left, .01f)
        assertEquals(1080f, portrait.right, .01f)
        assertTrue(portrait.top > 0f)
        assertEquals(2400f - 84f - 36f, portrait.bottom, .01f)
        val landscape = UltimateClockStyles.worldClockStripBounds(0f, 0f, 2400f, 1080f, 3f, 84f)
        assertTrue(landscape.top > 1080f * .60f)
        assertEquals(0f, landscape.left, .01f)
        assertEquals(2400f, landscape.right, .01f)
        assertTrue(landscape.bottom <= 1080f - 84f)
        assertTrue(landscape.bottom - landscape.top >= 3f * 64f)
    }

    @Test
    fun cityCatalogIsLargeSearchableAndHasStableUniqueIds() {
        val all = WorldClockCatalog.all()
        assertTrue(all.size > 100)
        val ids = HashSet<String>()
        for (entry in all) assertTrue(ids.add(entry.getId()))
        assertTrue(WorldClockCatalog.search("墨西哥").size >= 4)
        assertFalse(WorldClockCatalog.search("America/New_York").isEmpty())
    }

    @Test
    fun worldClockContentFitsLandscapeButOverflowsPortraitWithEndPadding() {
        val landscapeInset = UltimateClockStyles.worldClockContentInset(UltimateClockStyles.STYLE_DUAL_BLOCKS, 2560f, 1000f, 2.25f)
        assertEquals(2560f * .029f, landscapeInset, .01f)
        assertTrue(UltimateClockStyles.worldClockContentWidth(4, 2560f, 1440f, 2.25f) + landscapeInset * 2f < 2560f)
        val portraitInset = UltimateClockStyles.worldClockContentInset(UltimateClockStyles.STYLE_DUAL_BLOCKS, 1440f, 1950f, 2.25f)
        assertEquals(1440f * .055f, portraitInset, .01f)
        assertTrue(UltimateClockStyles.worldClockContentWidth(4, 1440f, 2560f, 2.25f) + portraitInset * 2f > 1440f)
        assertEquals(0f, UltimateClockStyles.worldClockContentWidth(0, 1440f, 2560f, 2.25f), 0f)
    }

    @Test
    fun missingCountryFlagsUseANeutralGlobeWithoutChangingCityOrZone() {
        val unknown = WorldClockEntry("test", "Test city", "", "GMT", null)
        assertEquals("🌐", unknown.getFlagEmoji())
        assertEquals("Test city", unknown.getCity())
        assertEquals("GMT", unknown.getZoneId())
        assertEquals("🇨🇳", WorldClockCatalog.find("beijing")!!.getFlagEmoji())
        assertTrue(WorldClockCatalog.all().filter { it.getFlag().isEmpty() }.all { it.getFlagEmoji() == "🌐" })
    }

    @Test
    fun defaultsContainRequestedCitiesAndSameZoneCitiesCanCoexist() {
        assertEquals(4, WorldClockCatalog.defaults().size)
        assertNotNull(WorldClockCatalog.find("beijing"))
        assertNotNull(WorldClockCatalog.find("tokyo"))
        assertNotNull(WorldClockCatalog.find("london"))
        assertNotNull(WorldClockCatalog.find("new_york"))
        assertNotEquals(WorldClockCatalog.find("beijing"), WorldClockCatalog.find("shanghai"))
        assertEquals(WorldClockCatalog.find("beijing")!!.getZoneId(), WorldClockCatalog.find("shanghai")!!.getZoneId())
    }

    @Test
    fun worldClockPersistenceCapsSelectionAndKeepsOrder() {
        val many = ArrayList(WorldClockCatalog.all().subList(0, 120))
        many.add(many[0])
        val decoded = WorldClockRepository.decode(WorldClockRepository.encode(many))
        assertEquals(WorldClockRepository.MAX_SELECTED, decoded.size)
        assertEquals(many.subList(0, WorldClockRepository.MAX_SELECTED), decoded)
    }
}
