package com.clockmods.time;

/**
 * A curated list of countries/regions and the time zone that represents them,
 * modelled after Android's region-based time zone selection.
 *
 * <p>Selecting a region determines the time zone used for clock display. The
 * first entry ({@link #FOLLOW_SYSTEM_INDEX}) represents "follow the system".
 */
public final class RegionTimeZones {
    public static final int FOLLOW_SYSTEM_INDEX = 0;

    // Display names live in res/values*/strings.xml as the string-array "region_names",
    // parallel (by index) to ZONE_IDS below, so they follow the interface language.

    /** Corresponding IANA time zone ids; empty string means "follow the system". */
    public static final String[] ZONE_IDS = {
            "",
            "Asia/Shanghai",
            "Asia/Urumqi",
            "Asia/Hong_Kong",
            "Asia/Macau",
            "Asia/Taipei",
            "Asia/Tokyo",
            "Asia/Seoul",
            "Asia/Singapore",
            "Asia/Bangkok",
            "Asia/Jakarta",
            "Asia/Kolkata",
            "Asia/Dubai",
            "Europe/Moscow",
            "Europe/London",
            "Europe/Paris",
            "Europe/Berlin",
            "Europe/Istanbul",
            "Africa/Johannesburg",
            "Africa/Cairo",
            "America/Sao_Paulo",
            "America/Argentina/Buenos_Aires",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "America/Anchorage",
            "Pacific/Honolulu",
            "America/Toronto",
            "America/Mexico_City",
            "Australia/Sydney",
            "Australia/Perth",
            "Pacific/Auckland",
    };

    private RegionTimeZones() {
    }

    /** @return the index of the given zone id, or {@link #FOLLOW_SYSTEM_INDEX} if not found. */
    public static int indexOfZoneId(String zoneId) {
        if (zoneId == null || zoneId.length() == 0) {
            return FOLLOW_SYSTEM_INDEX;
        }
        for (int i = 0; i < ZONE_IDS.length; i++) {
            if (ZONE_IDS[i].equals(zoneId)) {
                return i;
            }
        }
        return FOLLOW_SYSTEM_INDEX;
    }
}
