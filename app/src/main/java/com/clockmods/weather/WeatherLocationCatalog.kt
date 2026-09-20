package com.clockmods.weather

import android.content.Context
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader

class WeatherLocationCatalog private constructor(private val entries: List<LocationEntry>) {
    class LocationEntry(
        @JvmField val locationId: String, @JvmField val province: String, @JvmField val city: String, @JvmField val district: String,
        provinceEn: String, cityEn: String, districtEn: String, @JvmField val latitude: Double, @JvmField val longitude: Double,
    ) {
        @JvmField val provinceEn = provinceEn.ifEmpty { province }
        @JvmField val cityEn = cityEn.ifEmpty { city }
        @JvmField val districtEn = districtEn.ifEmpty { district }
        fun displayProvince(english: Boolean) = if (english) provinceEn else province
        fun displayCity(english: Boolean) = if (english) cityEn else city
        fun displayDistrict(english: Boolean) = if (english) districtEn else district
    }
    fun provinces(): List<String> = entries.map { it.province }.distinct()
    fun provinceLabels(english: Boolean): List<String> = entries.distinctBy { it.province }.map { it.displayProvince(english) }
    fun cities(province: String): List<String> = entries.filter { it.province == province }.map { it.city }.distinct()
    fun cityLabels(province: String, english: Boolean): List<String> = entries.filter { it.province == province }.distinctBy { it.city }.map { it.displayCity(english) }
    fun districts(province: String, city: String): List<LocationEntry> = entries.filter { it.province == province && it.city == city }.distinctBy { it.district }
    fun findById(locationId: String?): LocationEntry? = if (locationId.isNullOrEmpty()) null else entries.firstOrNull { it.locationId == locationId }

    companion object {
        @JvmStatic @Throws(IOException::class)
        fun load(context: Context): WeatherLocationCatalog = context.assets.open("China-City-List-latest.csv").use { parse(InputStreamReader(it, Charsets.UTF_8)) }
        @JvmStatic @Throws(IOException::class)
        fun parse(source: Reader): WeatherLocationCatalog {
            val reader = if (source is BufferedReader) source else BufferedReader(source)
            var line: String?; var found = false
            while (true) { reader.mark(65536); line = reader.readLine() ?: break; if (line!!.startsWith("Location_ID,")) { reader.reset(); found = true; break } }
            if (!found) throw IOException("Missing weather location catalog header")
            val format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            val values = ArrayList<LocationEntry>()
            CSVParser.parse(reader, format).use { parser -> parser.forEach { record -> val id = value(record, "Location_ID"); val province = value(record, "Adm1_Name_ZH"); val city = value(record, "Adm2_Name_ZH"); val district = value(record, "Location_Name_ZH"); if (id.isNotEmpty() && province.isNotEmpty() && city.isNotEmpty() && district.isNotEmpty()) values += LocationEntry(id, province, city, district, value(record, "Adm1_Name_EN"), value(record, "Adm2_Name_EN"), value(record, "Location_Name_EN"), coordinate(value(record, "Latitude")), coordinate(value(record, "Longitude"))) } }
            if (values.isEmpty()) throw IOException("Empty weather location catalog")
            return WeatherLocationCatalog(values)
        }
        private fun value(record: org.apache.commons.csv.CSVRecord, name: String) = if (record.isMapped(name)) record.get(name).trim() else ""
        private fun coordinate(value: String) = value.toDoubleOrNull() ?: Double.NaN
    }
}
