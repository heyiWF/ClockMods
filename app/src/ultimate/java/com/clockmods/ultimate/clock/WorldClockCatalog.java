package com.clockmods.ultimate.clock;

import com.clockmods.sdk.clock.WorldClockEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/** Searchable city catalog used by the world-clock editor. */
public final class WorldClockCatalog {
    private static final List<WorldClockEntry> FEATURED = Arrays.asList(
            city("shanghai", "上海", "中国", "Asia/Shanghai", "CN"),
            city("beijing", "北京", "中国", "Asia/Shanghai", "CN"),
            city("urumqi", "乌鲁木齐", "中国", "Asia/Urumqi", "CN"),
            city("tokyo", "东京", "日本", "Asia/Tokyo", "JP"),
            city("seoul", "首尔", "韩国", "Asia/Seoul", "KR"),
            city("singapore", "新加坡", "新加坡", "Asia/Singapore", "SG"),
            city("bangkok", "曼谷", "泰国", "Asia/Bangkok", "TH"),
            city("mumbai", "孟买", "印度", "Asia/Kolkata", "IN"),
            city("dubai", "迪拜", "阿联酋", "Asia/Dubai", "AE"),
            city("moscow", "莫斯科", "俄罗斯", "Europe/Moscow", "RU"),
            city("london", "伦敦", "英国", "Europe/London", "GB"),
            city("paris", "巴黎", "法国", "Europe/Paris", "FR"),
            city("berlin", "柏林", "德国", "Europe/Berlin", "DE"),
            city("istanbul", "伊斯坦布尔", "土耳其", "Europe/Istanbul", "TR"),
            city("cairo", "开罗", "埃及", "Africa/Cairo", "EG"),
            city("johannesburg", "约翰内斯堡", "南非", "Africa/Johannesburg", "ZA"),
            city("sao_paulo", "圣保罗", "巴西", "America/Sao_Paulo", "BR"),
            city("new_york", "纽约", "美国", "America/New_York", "US"),
            city("chicago", "芝加哥", "美国", "America/Chicago", "US"),
            city("denver", "丹佛", "美国", "America/Denver", "US"),
            city("los_angeles", "洛杉矶", "美国", "America/Los_Angeles", "US"),
            city("toronto", "多伦多", "加拿大", "America/Toronto", "CA"),
            city("mexico_city", "墨西哥城", "墨西哥", "America/Mexico_City", "MX"),
            city("honolulu", "檀香山", "美国", "Pacific/Honolulu", "US"),
            city("sydney", "悉尼", "澳大利亚", "Australia/Sydney", "AU"),
            city("perth", "珀斯", "澳大利亚", "Australia/Perth", "AU"),
            city("auckland", "奥克兰", "新西兰", "Pacific/Auckland", "NZ"),
            city("reykjavik", "雷克雅未克", "冰岛", "Atlantic/Reykjavik", "IS"),
            city("thimphu", "Thimphu", "不丹", "Asia/Thimphu", "BT"),
            city("dili", "Dili", "东帝汶", "Asia/Dili", "TL"),
            city("bahia_banderas", "Bahia Banderas", "墨西哥", "America/Bahia_Banderas", "MX"),
            city("cancun", "Cancun", "墨西哥", "America/Cancun", "MX"),
            city("chihuahua", "Chihuahua", "墨西哥", "America/Chihuahua", "MX"));
    private static final List<WorldClockEntry> ENTRIES = buildEntries();

    private WorldClockCatalog() { }

    public static List<WorldClockEntry> all() { return ENTRIES; }

    public static List<WorldClockEntry> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.length() == 0) return ENTRIES;
        List<WorldClockEntry> result = new ArrayList<>();
        for (WorldClockEntry entry : ENTRIES) {
            String haystack = (entry.getCity() + " " + entry.getCountry() + " "
                    + entry.getZoneId() + " " + entry.getId()).toLowerCase(Locale.ROOT);
            if (haystack.contains(needle)) result.add(entry);
        }
        return result;
    }

    public static WorldClockEntry find(String id) {
        if (id == null) return null;
        for (WorldClockEntry entry : ENTRIES) {
            if (entry.getId().equals(id)) return entry;
        }
        return null;
    }

    public static List<WorldClockEntry> defaults() {
        return Arrays.asList(find("beijing"), find("tokyo"), find("london"), find("new_york"));
    }

    private static List<WorldClockEntry> buildEntries() {
        Map<String, WorldClockEntry> entries = new LinkedHashMap<>();
        for (WorldClockEntry entry : FEATURED) entries.put(entry.getId(), entry);
        for (String zoneId : TimeZone.getAvailableIDs()) {
            if (zoneId == null || zoneId.indexOf('/') < 0) continue;
            String id = "zone:" + zoneId.toLowerCase(Locale.ROOT);
            if (entries.containsKey(id) || containsZone(entries, zoneId)) continue;
            int slash = zoneId.lastIndexOf('/');
            String city = zoneId.substring(slash + 1).replace('_', ' ');
            String region = zoneId.substring(0, zoneId.indexOf('/')).replace('_', ' ');
            entries.put(id, city(city.length() == 0 ? zoneId : city, region, zoneId));
        }
        return Collections.unmodifiableList(new ArrayList<>(entries.values()));
    }

    private static boolean containsZone(Map<String, WorldClockEntry> entries, String zoneId) {
        for (WorldClockEntry entry : entries.values()) {
            if (entry.getZoneId().equals(zoneId)) return true;
        }
        return false;
    }

    private static WorldClockEntry city(String name, String region, String zoneId) {
        return new WorldClockEntry("zone:" + zoneId.toLowerCase(Locale.ROOT), name, region,
                zoneId, "");
    }

    private static WorldClockEntry city(String id, String name, String country,
            String zoneId, String flag) {
        return new WorldClockEntry(id, name, country, zoneId, flag);
    }
}
