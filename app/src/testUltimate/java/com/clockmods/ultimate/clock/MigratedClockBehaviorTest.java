package com.clockmods.ultimate.clock;

import android.graphics.RectF;

import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.WorldClockEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class MigratedClockBehaviorTest {
    @Test
    public void smoothSecondsIncludeMillisecondsAndTickSecondsDoNot() {
        Assert.assertEquals(12.625f, UltimateClockStyles.secondProgress(12, 625,
                ClockState.SecondHandMotion.SWEEP, false), 0f);
        Assert.assertEquals(12f, UltimateClockStyles.secondProgress(12, 625,
                ClockState.SecondHandMotion.TICK, false), 0f);
        Assert.assertEquals(12f, UltimateClockStyles.secondProgress(12, 625,
                ClockState.SecondHandMotion.SWEEP, true), 0f);
    }

    @Test
    public void blendDateSplitsOnlyTheAppendedLunarValue() {
        String[] split = UltimateClockStyles.splitDateAndLunar(
                "2026 / 09 / 11 周五 / 丙午[马]年八月初一");
        Assert.assertArrayEquals(new String[] {
                "2026 / 09 / 11 周五", "丙午[马]年八月初一"
        }, split);

        Assert.assertArrayEquals(new String[] {"09 / 11 / 2026", ""},
                UltimateClockStyles.splitDateAndLunar("09 / 11 / 2026"));
    }

    @Test
    public void ribbonSecondsAreHalfTheHourMinuteTextSize() {
        Assert.assertEquals(48f, UltimateClockStyles.ribbonSecondsTextSize(96f), 0f);
        Assert.assertEquals(0f, UltimateClockStyles.ribbonSecondsTextSize(-1f), 0f);
    }

    @Test
    public void portraitBubblesStackHourAndMinuteAndUseAvailableHeight() {
        float[] geometry = UltimateClockStyles.bubblesPortraitGeometry(900f, 1276f, 2.5f);
        float hourBottom = geometry[1];
        float minuteTop = geometry[2] - geometry[3];
        float minuteBottom = geometry[2] + geometry[3];

        Assert.assertTrue(hourBottom + geometry[4] <= minuteTop + .01f);
        Assert.assertTrue(minuteBottom <= 1276f * .91f + .01f);
        Assert.assertTrue(minuteBottom >= 1276f * .85f);
    }

    @Test
    public void portraitRibbonKeepsAWideCapsuleProfile() {
        float[] geometry = UltimateClockStyles.ribbonPortraitGeometry(900f, 1276f);
        float outerWidth = 900f * .91f;
        float ribbonWidth = 900f * .853f;

        Assert.assertTrue(outerWidth / geometry[1] > 2f);
        Assert.assertTrue(ribbonWidth / geometry[2] > 3f);
        Assert.assertTrue(geometry[2] <= 900f * .27f);
    }

    @Test
    public void worldClockStripStaysInsidePortraitAndLandscapeSafeBounds() {
        RectF portrait = UltimateClockStyles.worldClockStripBounds(
                0f, 0f, 1080f, 2400f, 3f, 84f);
        Assert.assertEquals(0f, portrait.left, .01f);
        Assert.assertEquals(1080f, portrait.right, .01f);
        Assert.assertTrue(portrait.top > 0f);
        Assert.assertEquals(2400f - 84f - 36f, portrait.bottom, .01f);

        RectF landscape = UltimateClockStyles.worldClockStripBounds(
                0f, 0f, 2400f, 1080f, 3f, 84f);
        Assert.assertTrue(landscape.top > 1080f * .60f);
        Assert.assertEquals(0f, landscape.left, .01f);
        Assert.assertEquals(2400f, landscape.right, .01f);
        Assert.assertTrue(landscape.bottom <= 1080f - 84f);
        Assert.assertTrue(landscape.bottom - landscape.top >= 3f * 64f);
    }

    @Test
    public void cityCatalogIsLargeSearchableAndHasStableUniqueIds() {
        List<WorldClockEntry> all = WorldClockCatalog.all();
        Assert.assertTrue(all.size() > 100);
        Set<String> ids = new HashSet<>();
        for (WorldClockEntry entry : all) Assert.assertTrue(ids.add(entry.getId()));
        Assert.assertTrue(WorldClockCatalog.search("墨西哥").size() >= 4);
        Assert.assertFalse(WorldClockCatalog.search("America/New_York").isEmpty());
    }

    @Test
    public void worldClockContentFitsLandscapeButOverflowsPortraitWithEndPadding() {
        float landscapeInset = UltimateClockStyles.worldClockContentInset(
                UltimateClockStyles.STYLE_DUAL_BLOCKS, 2560f, 1000f, 2.25f);
        Assert.assertEquals(2560f * .029f, landscapeInset, .01f);
        Assert.assertTrue(UltimateClockStyles.worldClockContentWidth(4, 2560f, 1440f, 2.25f)
                + landscapeInset * 2f < 2560f);
        float portraitInset = UltimateClockStyles.worldClockContentInset(
                UltimateClockStyles.STYLE_DUAL_BLOCKS, 1440f, 1950f, 2.25f);
        Assert.assertEquals(1440f * .055f, portraitInset, .01f);
        Assert.assertTrue(UltimateClockStyles.worldClockContentWidth(4, 1440f, 2560f, 2.25f)
                + portraitInset * 2f > 1440f);
        Assert.assertEquals(0f, UltimateClockStyles.worldClockContentWidth(
                0, 1440f, 2560f, 2.25f), 0f);
    }

    @Test
    public void missingCountryFlagsUseANeutralGlobeWithoutChangingCityOrZone() {
        WorldClockEntry unknown = new WorldClockEntry("test", "Test city", "", "GMT", null);
        Assert.assertEquals("\uD83C\uDF10", unknown.getFlagEmoji());
        Assert.assertEquals("Test city", unknown.getCity());
        Assert.assertEquals("GMT", unknown.getZoneId());
        Assert.assertEquals("\uD83C\uDDE8\uD83C\uDDF3", WorldClockCatalog.find("beijing").getFlagEmoji());
        Assert.assertTrue(WorldClockCatalog.all().stream()
                .filter(entry -> entry.getFlag().isEmpty())
                .allMatch(entry -> "\uD83C\uDF10".equals(entry.getFlagEmoji())));
    }

    @Test
    public void defaultsContainRequestedCitiesAndSameZoneCitiesCanCoexist() {
        Assert.assertEquals(4, WorldClockCatalog.defaults().size());
        Assert.assertNotNull(WorldClockCatalog.find("beijing"));
        Assert.assertNotNull(WorldClockCatalog.find("tokyo"));
        Assert.assertNotNull(WorldClockCatalog.find("london"));
        Assert.assertNotNull(WorldClockCatalog.find("new_york"));
        Assert.assertNotEquals(WorldClockCatalog.find("beijing"),
                WorldClockCatalog.find("shanghai"));
        Assert.assertEquals(WorldClockCatalog.find("beijing").getZoneId(),
                WorldClockCatalog.find("shanghai").getZoneId());
    }

    @Test
    public void worldClockPersistenceCapsSelectionAndKeepsOrder() throws Exception {
        List<WorldClockEntry> many = new ArrayList<>(
                WorldClockCatalog.all().subList(0, 120));
        many.add(many.get(0));
        List<WorldClockEntry> decoded = WorldClockRepository.decode(
                WorldClockRepository.encode(many));
        Assert.assertEquals(WorldClockRepository.MAX_SELECTED, decoded.size());
        Assert.assertEquals(many.subList(0, WorldClockRepository.MAX_SELECTED), decoded);
    }
}
