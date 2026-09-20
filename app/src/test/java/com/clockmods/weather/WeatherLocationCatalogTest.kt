package com.clockmods.weather

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherLocationCatalogTest {
    private val csv = "China-City-List v202506200,,,,,,,,,,,,,\n" +
        "Location_ID,Location_Name_EN,Location_Name_ZH,ISO_3166_1,Country_Region_EN,Country_Region_ZH,Adm1_Name_EN,Adm1_Name_ZH,Adm2_Name_EN,Adm2_Name_ZH,Timezone,Latitude,Longitude,AD_code\n" +
        "101280601,Shenzhen,深圳,CN,China,中国,Guangdong,广东省,Shenzhen,深圳市,Asia/Shanghai,22.5470,114.0859,440300\n" +
        "101280606,Baoan,宝安,CN,China,中国,Guangdong,广东省,Shenzhen,深圳市,Asia/Shanghai,22.7547,113.8287,440306\n" +
        "101280109,Tianhe,天河,CN,China,中国,Guangdong,广东省,Guangzhou,广州市,Asia/Shanghai,23.1246,113.3612,440106\n"

    @Test fun filtersProvinceCityAndDistrictInSourceOrder() {
        val catalog = WeatherLocationCatalog.parse(StringReader(csv))
        assertEquals("广东省", catalog.provinces()[0])
        assertEquals(2, catalog.cities("广东省").size)
        val districts = catalog.districts("广东省", "深圳市")
        assertEquals(2, districts.size)
        assertEquals("宝安", districts[1].district)
        assertEquals("101280606", districts[1].locationId)
    }

    @Test fun exposesEnglishNamesWithChineseFallback() {
        val catalog = WeatherLocationCatalog.parse(StringReader(csv))
        val baoan = catalog.districts("广东省", "深圳市")[1]
        assertEquals("Baoan", baoan.displayDistrict(true))
        assertEquals("Shenzhen", baoan.displayCity(true))
        assertEquals("宝安", baoan.displayDistrict(false))
    }
}
