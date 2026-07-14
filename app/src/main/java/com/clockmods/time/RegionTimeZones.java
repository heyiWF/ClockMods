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

    /** Display names shown to the user (region + representative city). */
    public static final String[] DISPLAY_NAMES = {
            "跟随系统",
            "中国（北京）",
            "中国（乌鲁木齐）",
            "中国香港",
            "中国澳门",
            "中国台湾（台北）",
            "日本（东京）",
            "韩国（首尔）",
            "新加坡",
            "泰国（曼谷）",
            "印度尼西亚（雅加达）",
            "印度（加尔各答）",
            "阿联酋（迪拜）",
            "俄罗斯（莫斯科）",
            "英国（伦敦）",
            "法国（巴黎）",
            "德国（柏林）",
            "土耳其（伊斯坦布尔）",
            "南非（约翰内斯堡）",
            "埃及（开罗）",
            "巴西（圣保罗）",
            "阿根廷（布宜诺斯艾利斯）",
            "美国东部（纽约）",
            "美国中部（芝加哥）",
            "美国山区（丹佛）",
            "美国太平洋（洛杉矶）",
            "美国阿拉斯加",
            "美国夏威夷",
            "加拿大（多伦多）",
            "墨西哥（墨西哥城）",
            "澳大利亚（悉尼）",
            "澳大利亚（珀斯）",
            "新西兰（奥克兰）",
    };

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
