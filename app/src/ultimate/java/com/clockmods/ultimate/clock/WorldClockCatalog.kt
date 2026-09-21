package com.clockmods.ultimate.clock

import com.clockmods.sdk.clock.WorldClockEntry
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.TimeZone

/** Searchable city catalog used by the world-clock editor. */
class WorldClockCatalog private constructor() {
    companion object {
        /** Language buckets the curated city labels can be rendered in. */
        const val LANG_SIMPLIFIED = 0
        const val LANG_TRADITIONAL = 1
        const val LANG_ENGLISH = 2

        /**
         * Curated cities carry every supported language so the editor and the clock face follow the
         * app language instead of always showing the Simplified Chinese name.
         */
        private class FeaturedCity(
            val id: String,
            val cityNames: Array<String>,
            val countryNames: Array<String>,
            val zoneId: String,
            val flag: String,
        )

        private val FEATURED = listOf(
            featured("shanghai", "上海", "中国", "Asia/Shanghai", "CN", "Shanghai", "China"),
            featured("beijing", "北京", "中国", "Asia/Shanghai", "CN", "Beijing", "China"),
            featured("urumqi", "乌鲁木齐", "中国", "Asia/Urumqi", "CN", "Urumqi", "China"),
            featured("tokyo", "东京", "日本", "Asia/Tokyo", "JP", "Tokyo", "Japan", "東京"),
            featured("seoul", "首尔", "韩国", "Asia/Seoul", "KR", "Seoul", "South Korea", "首爾", "韓國"),
            featured("singapore", "新加坡", "新加坡", "Asia/Singapore", "SG", "Singapore", "Singapore"),
            featured("bangkok", "曼谷", "泰国", "Asia/Bangkok", "TH", "Bangkok", "Thailand", "曼谷", "泰國"),
            featured("mumbai", "孟买", "印度", "Asia/Kolkata", "IN", "Mumbai", "India", "孟買"),
            featured("dubai", "迪拜", "阿联酋", "Asia/Dubai", "AE", "Dubai", "United Arab Emirates", "杜拜", "阿聯酋"),
            featured("moscow", "莫斯科", "俄罗斯", "Europe/Moscow", "RU", "Moscow", "Russia", "莫斯科", "俄羅斯"),
            featured("london", "伦敦", "英国", "Europe/London", "GB", "London", "United Kingdom", "倫敦", "英國"),
            featured("paris", "巴黎", "法国", "Europe/Paris", "FR", "Paris", "France", "巴黎", "法國"),
            featured("berlin", "柏林", "德国", "Europe/Berlin", "DE", "Berlin", "Germany", "柏林", "德國"),
            featured("istanbul", "伊斯坦布尔", "土耳其", "Europe/Istanbul", "TR", "Istanbul", "Türkiye", "伊斯坦堡"),
            featured("cairo", "开罗", "埃及", "Africa/Cairo", "EG", "Cairo", "Egypt", "開羅"),
            featured("johannesburg", "约翰内斯堡", "南非", "Africa/Johannesburg", "ZA", "Johannesburg", "South Africa", "約翰尼斯堡"),
            featured("sao_paulo", "圣保罗", "巴西", "America/Sao_Paulo", "BR", "São Paulo", "Brazil", "聖保羅"),
            featured("new_york", "纽约", "美国", "America/New_York", "US", "New York", "United States", "紐約", "美國"),
            featured("chicago", "芝加哥", "美国", "America/Chicago", "US", "Chicago", "United States", "芝加哥", "美國"),
            featured("denver", "丹佛", "美国", "America/Denver", "US", "Denver", "United States", "丹佛", "美國"),
            featured("los_angeles", "洛杉矶", "美国", "America/Los_Angeles", "US", "Los Angeles", "United States", "洛杉磯", "美國"),
            featured("toronto", "多伦多", "加拿大", "America/Toronto", "CA", "Toronto", "Canada", "多倫多"),
            featured("mexico_city", "墨西哥城", "墨西哥", "America/Mexico_City", "MX", "Mexico City", "Mexico"),
            featured("honolulu", "檀香山", "美国", "Pacific/Honolulu", "US", "Honolulu", "United States", "檀香山", "美國"),
            featured("sydney", "悉尼", "澳大利亚", "Australia/Sydney", "AU", "Sydney", "Australia", "雪梨", "澳大利亞"),
            featured("perth", "珀斯", "澳大利亚", "Australia/Perth", "AU", "Perth", "Australia", "珀斯", "澳大利亞"),
            featured("auckland", "奥克兰", "新西兰", "Pacific/Auckland", "NZ", "Auckland", "New Zealand", "奧克蘭", "紐西蘭"),
            featured("reykjavik", "雷克雅未克", "冰岛", "Atlantic/Reykjavik", "IS", "Reykjavík", "Iceland", "雷克雅未克", "冰島"),
            featured("thimphu", "廷布", "不丹", "Asia/Thimphu", "BT", "Thimphu", "Bhutan"),
            featured("dili", "帝力", "东帝汶", "Asia/Dili", "TL", "Dili", "Timor-Leste", "帝力", "東帝汶"),
            featured("bahia_banderas", "巴伊亚德班德拉斯", "墨西哥", "America/Bahia_Banderas", "MX", "Bahia Banderas", "Mexico", "巴伊亞德班德拉斯"),
            featured("cancun", "坎昆", "墨西哥", "America/Cancun", "MX", "Cancun", "Mexico"),
            featured("chihuahua", "奇瓦瓦", "墨西哥", "America/Chihuahua", "MX", "Chihuahua", "Mexico"),
        )

        private val LOCALIZED_NAMES: Map<String, FeaturedCity> = FEATURED.associateBy { it.id }

        private val ENTRIES: List<WorldClockEntry> = buildEntries()

        @JvmStatic
        fun all(): List<WorldClockEntry> = ENTRIES

        /** City label in the requested language, falling back to the catalog's default name. */
        @JvmStatic
        fun displayCity(entry: WorldClockEntry, language: Int): String =
            LOCALIZED_NAMES[entry.getId()]?.cityNames?.getOrNull(language) ?: entry.getCity()

        /** Country or region label in the requested language. */
        @JvmStatic
        fun displayCountry(entry: WorldClockEntry, language: Int): String =
            LOCALIZED_NAMES[entry.getId()]?.countryNames?.getOrNull(language) ?: entry.getCountry()

        /** Maps a clock locale onto one of the [LANG_SIMPLIFIED]/[LANG_TRADITIONAL]/[LANG_ENGLISH] buckets. */
        @JvmStatic
        fun languageOf(locale: Locale?): Int = when {
            locale == null -> LANG_SIMPLIFIED
            locale.language == Locale.ENGLISH.language -> LANG_ENGLISH
            locale.country.equals("TW", ignoreCase = true) ||
                locale.script.equals("Hant", ignoreCase = true) -> LANG_TRADITIONAL
            else -> LANG_SIMPLIFIED
        }

        @JvmStatic
        fun search(query: String?): List<WorldClockEntry> {
            val needle = query?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (needle.isEmpty()) return ENTRIES
            return ENTRIES.filter { entry ->
                val names = LOCALIZED_NAMES[entry.getId()]
                (entry.getCity() + " " + entry.getCountry() + " " +
                    names?.cityNames.orEmpty().joinToString(" ") + " " +
                    names?.countryNames.orEmpty().joinToString(" ") + " " +
                    entry.getZoneId() + " " + entry.getId())
                    .lowercase(Locale.ROOT).contains(needle)
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
            FEATURED.forEach {
                entries[it.id] = WorldClockEntry(it.id, it.cityNames[LANG_SIMPLIFIED],
                    it.countryNames[LANG_SIMPLIFIED], it.zoneId, it.flag)
            }
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

        /** [cityHant]/[countryHant] default to the Simplified names when the two scripts agree. */
        private fun featured(
            id: String,
            cityZh: String,
            countryZh: String,
            zoneId: String,
            flag: String,
            cityEn: String,
            countryEn: String,
            cityHant: String = cityZh,
            countryHant: String = countryZh,
        ) = FeaturedCity(
            id,
            arrayOf(cityZh, cityHant, cityEn),
            arrayOf(countryZh, countryHant, countryEn),
            zoneId,
            flag,
        )
    }
}
