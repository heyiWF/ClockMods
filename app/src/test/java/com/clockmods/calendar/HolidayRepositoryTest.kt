package com.clockmods.calendar

import org.junit.Assert
import org.junit.Test

class HolidayRepositoryTest {
    @Test
    fun parsesOffAndWorkDays() {
        val statuses = HolidayRepository.parse(
            "{\"days\":[{\"name\":\"春节\",\"date\":\"2026-02-17\",\"isOffDay\":true}," +
                "{\"name\":\"春节\",\"date\":\"2026-02-14\",\"isOffDay\":false}]}",
        )
        Assert.assertTrue(statuses["2026-02-17"]!!.offDay)
        Assert.assertFalse(statuses["2026-02-14"]!!.offDay)
    }
}
