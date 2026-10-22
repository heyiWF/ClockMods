package com.clockmods.weather;

import android.content.Context;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WeatherLocationCatalog {
    public static final class LocationEntry {
        public final String locationId;
        public final String province;
        public final String city;
        public final String district;
        public final double latitude;
        public final double longitude;

        LocationEntry(String locationId, String province, String city, String district,
                double latitude, double longitude) {
            this.locationId = locationId;
            this.province = province;
            this.city = city;
            this.district = district;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private final List<LocationEntry> entries;

    private WeatherLocationCatalog(List<LocationEntry> entries) {
        this.entries = entries;
    }

    public static WeatherLocationCatalog load(Context context) throws IOException {
        InputStream input = context.getAssets().open("China-City-List-latest.csv");
        try {
            return parse(new InputStreamReader(input, "UTF-8"));
        } finally {
            input.close();
        }
    }

    static WeatherLocationCatalog parse(Reader source) throws IOException {
        BufferedReader reader = source instanceof BufferedReader
                ? (BufferedReader) source : new BufferedReader(source);
        String line;
        boolean foundHeader = false;
        while (true) {
            reader.mark(65536);
            line = reader.readLine();
            if (line == null) break;
            if (line.startsWith("Location_ID,")) {
                reader.reset();
                foundHeader = true;
                break;
            }
        }
        if (!foundHeader) throw new IOException("Missing weather location catalog header");
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
        List<LocationEntry> entries = new ArrayList<>();
        try (CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                String locationId = value(record, "Location_ID");
                String province = value(record, "Adm1_Name_ZH");
                String city = value(record, "Adm2_Name_ZH");
                String district = value(record, "Location_Name_ZH");
                if (locationId.length() > 0 && province.length() > 0
                        && city.length() > 0 && district.length() > 0) {
                    entries.add(new LocationEntry(locationId, province, city, district,
                            parseCoordinate(value(record, "Latitude")),
                            parseCoordinate(value(record, "Longitude"))));
                }
            }
        }
        if (entries.isEmpty()) throw new IOException("Empty weather location catalog");
        return new WeatherLocationCatalog(entries);
    }

    public List<String> provinces() {
        Set<String> values = new LinkedHashSet<>();
        for (LocationEntry entry : entries) values.add(entry.province);
        return new ArrayList<>(values);
    }

    public List<String> cities(String province) {
        Set<String> values = new LinkedHashSet<>();
        for (LocationEntry entry : entries) {
            if (entry.province.equals(province)) values.add(entry.city);
        }
        return new ArrayList<>(values);
    }

    public List<LocationEntry> districts(String province, String city) {
        List<LocationEntry> values = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        for (LocationEntry entry : entries) {
            if (entry.province.equals(province) && entry.city.equals(city)
                    && names.add(entry.district)) values.add(entry);
        }
        return values;
    }

    public LocationEntry findById(String locationId) {
        if (locationId == null || locationId.length() == 0) return null;
        for (LocationEntry entry : entries) {
            if (entry.locationId.equals(locationId)) return entry;
        }
        return null;
    }

    private static double parseCoordinate(String value) {
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return Double.NaN; }
    }

    private static String value(CSVRecord record, String name) {
        return record.isMapped(name) ? record.get(name).trim() : "";
    }
}