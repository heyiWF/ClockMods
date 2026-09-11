package com.clockmods.sdk.clock;

import java.util.TimeZone;

/** Immutable city/time-zone data supplied to styles that render a world-clock strip. */
public final class WorldClockEntry {
    private final String id;
    private final String city;
    private final String country;
    private final String zoneId;
    private final String flag;

    public WorldClockEntry(String id, String city, String country, String zoneId, String flag) {
        this.id = required(id, "id");
        this.city = required(city, "city");
        this.country = clean(country);
        this.zoneId = required(zoneId, "zoneId");
        this.flag = clean(flag);
        if (TimeZone.getTimeZone(this.zoneId).getID().equals("GMT")
                && !"GMT".equalsIgnoreCase(this.zoneId)) {
            throw new IllegalArgumentException("Unknown time zone: " + zoneId);
        }
    }

    public String getId() { return id; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public String getZoneId() { return zoneId; }
    public String getFlag() { return flag; }

    /** Converts an ISO 3166-1 alpha-2 code to its Unicode regional-indicator flag. */
    public String getFlagEmoji() {
        if (flag.length() != 2) return flag;
        String upper = flag.toUpperCase(java.util.Locale.ROOT);
        char first = upper.charAt(0);
        char second = upper.charAt(1);
        if (first < 'A' || first > 'Z' || second < 'A' || second > 'Z') return flag;
        return new String(Character.toChars(0x1F1E6 + first - 'A'))
                + new String(Character.toChars(0x1F1E6 + second - 'A'));
    }

    private static String required(String value, String label) {
        String result = clean(value);
        if (result.length() == 0) throw new IllegalArgumentException(label + " must not be blank");
        return result;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }

    @Override public boolean equals(Object other) {
        return other instanceof WorldClockEntry && id.equals(((WorldClockEntry) other).id);
    }

    @Override public int hashCode() { return id.hashCode(); }
}
