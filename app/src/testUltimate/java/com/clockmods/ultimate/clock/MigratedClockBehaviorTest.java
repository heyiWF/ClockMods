package com.clockmods.ultimate.clock;

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
    public void cityCatalogIsLargeSearchableAndHasStableUniqueIds() {
        List<WorldClockEntry> all = WorldClockCatalog.all();
        Assert.assertTrue(all.size() > 100);
        Set<String> ids = new HashSet<>();
        for (WorldClockEntry entry : all) Assert.assertTrue(ids.add(entry.getId()));
        Assert.assertTrue(WorldClockCatalog.search("墨西哥").size() >= 4);
        Assert.assertFalse(WorldClockCatalog.search("America/New_York").isEmpty());
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
