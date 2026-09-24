package com.clockmods.ultimate.compose

import com.clockmods.R
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceStatusIconTest {
    @Test
    fun batteryFillIncreasesWithReportedPercentage() {
        val levels = listOf(
            0 to R.drawable.ic_battery_android_0,
            14 to R.drawable.ic_battery_android_1,
            28 to R.drawable.ic_battery_android_2,
            42 to R.drawable.ic_battery_android_3,
            56 to R.drawable.ic_battery_android_4,
            70 to R.drawable.ic_battery_android_5,
            72 to R.drawable.ic_battery_android_6,
            86 to R.drawable.ic_battery_android_full,
        )
        levels.forEach { (percent, icon) ->
            assertEquals("$percent%", icon, batteryIcon(percent, charging = false))
        }
        assertEquals(R.drawable.ic_battery_android_bolt, batteryIcon(72, charging = true))
    }
}
