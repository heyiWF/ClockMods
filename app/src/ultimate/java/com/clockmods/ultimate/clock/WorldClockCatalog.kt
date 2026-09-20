package com.clockmods.ultimate.clock

import com.clockmods.sdk.clock.WorldClockEntry
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.TimeZone

/** Searchable city catalog used by the world-clock editor. */
class WorldClockCatalog private constructor() {
    companion object {
        private val FEATURED = listOf(
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
            city("chihuahua", "Chihuahua", "墨西哥", "America/Chihuahua", "MX"),
        )
        private val ENTRIES: List<WorldClockEntry> = buildEntries()

        @JvmStatic
        fun all(): List<WorldClockEntry> = ENTRIES

        @JvmStatic
        fun search(query: String?): List<WorldClockEntry> {
            val needle = query?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (needle.isEmpty()) return ENTRIES
            return ENTRIES.filter { entry ->
                (entry.getCity() + " " + entry.getCountry() + " " + entry.getZoneId() + " " +
                    entry.getId()).lowercase(Locale.ROOT).contains(needle)
            }
        }

        @JvmStatic
        fun find(id: String?): WorldClockEntry? = if (id == null) null else ENTRIES.firstOrNull {
            it.getId() == id
        }

        @JvmStatic
        fun defaults(): List<WorldClockEntry> = listOfNotNull(
            find("beijing"), find("tokyo"), find("london"), find("new_york")
        )

        private fun buildEntries(): List<WorldClockEntry> {
            val entries = LinkedHashMap<String, WorldClockEntry>()
            FEATURED.forEach { entries[it.getId()] = it }
            for (zoneId in TimeZone.getAvailableIDs()) {
                if (!zoneId.contains('/')) continue
                val id = "zone:${zoneId.lowercase(Locale.ROOT)}"
                if (entries.containsKey(id) || containsZone(entries, zoneId)) continue
                val slash = zoneId.lastIndexOf('/')
                val city = zoneId.substring(slash + 1).replace('_', ' ')
                val region = zoneId.substring(0, zoneId.indexOf('/')).replace('_', ' ')
                entries[id] = city(if (city.isEmpty()) zoneId else city, region, zoneId)
            }
            return Collections.unmodifiableList(ArrayList(entries.values))
        }

        private fun containsZone(entries: Map<String, WorldClockEntry>, zoneId: String): Boolean =
            entries.values.any { it.getZoneId() == zoneId }

        private fun city(name: String, region: String, zoneId: String): WorldClockEntry =
            WorldClockEntry("zone:${zoneId.lowercase(Locale.ROOT)}", name, region, zoneId, "")

        private fun city(
            id: String,
            name: String,
            country: String,
            zoneId: String,
            flag: String,
        ): WorldClockEntry = WorldClockEntry(id, name, country, zoneId, flag)
    }
}
