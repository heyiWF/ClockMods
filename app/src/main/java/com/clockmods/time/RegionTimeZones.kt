package com.clockmods.time

object RegionTimeZones {
    const val FOLLOW_SYSTEM_INDEX: Int = 0

    @JvmField
    val ZONE_IDS: Array<String> = arrayOf(
        "", "Asia/Shanghai", "Asia/Urumqi", "Asia/Hong_Kong", "Asia/Macau", "Asia/Taipei",
        "Asia/Tokyo", "Asia/Seoul", "Asia/Singapore", "Asia/Bangkok", "Asia/Jakarta",
        "Asia/Kolkata", "Asia/Dubai", "Europe/Moscow", "Europe/London", "Europe/Paris",
        "Europe/Berlin", "Europe/Istanbul", "Africa/Johannesburg", "Africa/Cairo",
        "America/Sao_Paulo", "America/Argentina/Buenos_Aires", "America/New_York",
        "America/Chicago", "America/Denver", "America/Los_Angeles", "America/Anchorage",
        "Pacific/Honolulu", "America/Toronto", "America/Mexico_City", "Australia/Sydney",
        "Australia/Perth", "Pacific/Auckland",
    )

    @JvmStatic
    fun indexOfZoneId(zoneId: String?): Int {
        if (zoneId.isNullOrEmpty()) return FOLLOW_SYSTEM_INDEX
        for (index in ZONE_IDS.indices) if (ZONE_IDS[index] == zoneId) return index
        return FOLLOW_SYSTEM_INDEX
    }
}
