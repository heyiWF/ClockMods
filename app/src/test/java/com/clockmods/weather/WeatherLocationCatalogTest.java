package com.clockmods.weather;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WeatherLocationCatalogTest {
    private static final String CSV = "China-City-List v202506200,,,,,,,,,,,,,\n"
            + "Location_ID,Location_Name_EN,Location_Name_ZH,ISO_3166_1,Country_Region_EN,Country_Region_ZH,Adm1_Name_EN,Adm1_Name_ZH,Adm2_Name_EN,Adm2_Name_ZH,Timezone,Latitude,Longitude,AD_code\n"
            + "101280601,Shenzhen,深圳,CN,China,中国,Guangdong,广东省,Shenzhen,深圳市,Asia/Shanghai,22.5470,114.0859,440300\n"
            + "101280606,Baoan,宝安,CN,China,中国,Guangdong,广东省,Shenzhen,深圳市,Asia/Shanghai,22.7547,113.8287,440306\n"
            + "101280109,Tianhe,天河,CN,China,中国,Guangdong,广东省,Guangzhou,广州市,Asia/Shanghai,23.1246,113.3612,440106\n";

    @Test
    public void filtersProvinceCityAndDistrictInSourceOrder() throws Exception {
        WeatherLocationCatalog catalog = WeatherLocationCatalog.parse(new StringReader(CSV));
        assertEquals("广东省", catalog.provinces().get(0));
        assertEquals(2, catalog.cities("广东省").size());
        List<WeatherLocationCatalog.LocationEntry> districts =
                catalog.districts("广东省", "深圳市");
        assertEquals(2, districts.size());
        assertEquals("宝安", districts.get(1).district);
        assertEquals("101280606", districts.get(1).locationId);
    }
}