package com.clockmods.calendar;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class HolidayRepositoryTest {
    @Test
    public void parsesOffAndWorkDays() throws Exception {
        Map<String, HolidayRepository.HolidayStatus> statuses = HolidayRepository.parse(
                "{\"days\":[{\"name\":\"春节\",\"date\":\"2026-02-17\",\"isOffDay\":true},"
                        + "{\"name\":\"春节\",\"date\":\"2026-02-14\",\"isOffDay\":false}]}");

        Assert.assertTrue(statuses.get("2026-02-17").offDay);
        Assert.assertFalse(statuses.get("2026-02-14").offDay);
    }
}