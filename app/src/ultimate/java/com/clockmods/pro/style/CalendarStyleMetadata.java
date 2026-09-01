package com.clockmods.pro.style;

import androidx.annotation.StringRes;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identity and declared behaviour of a {@link CalendarStyle}, mirroring
 * {@link com.clockmods.sdk.clock.ClockStyleMetadata} — including its id grammar, so the two
 * galleries name things the same way.
 *
 * <p>Unlike the clock's, the name and summary are string resources: the calendar gallery has always
 * been localised through {@code ultimate_strings.xml} and there is no plugin surface that would
 * need to supply raw text.</p>
 */
public final class CalendarStyleMetadata {
    /** Composition family. Purely descriptive — behaviour comes from {@link #getCapabilities()}. */
    public enum Kind { DASHBOARD, WALL, ALMANAC, AGENDA, POSTER }

    private static final Pattern ID_PATTERN =
            Pattern.compile("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)+");

    private final String id;
    private final int nameRes;
    private final int summaryRes;
    private final Kind kind;
    private final CalendarLayoutCapabilities capabilities;
    private final int version;
    private final int minApi;

    public CalendarStyleMetadata(String id, @StringRes int nameRes, @StringRes int summaryRes,
            Kind kind, CalendarLayoutCapabilities capabilities, int version, int minApi) {
        this.id = requireStyleId(id);
        this.nameRes = requireResource(nameRes, "name");
        this.summaryRes = requireResource(summaryRes, "summary");
        if (kind == null) throw new IllegalArgumentException("Calendar style kind is required");
        if (capabilities == null) {
            throw new IllegalArgumentException("Calendar style capabilities are required");
        }
        if (version < 1) throw new IllegalArgumentException("Calendar style version must be >= 1");
        if (minApi < 1) throw new IllegalArgumentException("Calendar style minApi must be >= 1");
        this.kind = kind;
        this.capabilities = capabilities;
        this.version = version;
        this.minApi = minApi;
    }

    public String getId() { return id; }

    @StringRes public int getNameRes() { return nameRes; }

    @StringRes public int getSummaryRes() { return summaryRes; }

    public Kind getKind() { return kind; }

    public CalendarLayoutCapabilities getCapabilities() { return capabilities; }

    public int getVersion() { return version; }

    public int getMinApi() { return minApi; }

    public boolean supportsApi(int apiLevel) { return apiLevel >= minApi; }

    private static String requireStyleId(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Calendar style id is required");
        }
        if (!id.equals(id.toLowerCase(Locale.US)) || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Calendar style id must be lowercase and qualified, "
                    + "e.g. calendar.graphite: " + id);
        }
        return id;
    }

    private static int requireResource(int value, String what) {
        if (value == 0) {
            throw new IllegalArgumentException("Calendar style " + what + " resource is required");
        }
        return value;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CalendarStyleMetadata)) return false;
        CalendarStyleMetadata that = (CalendarStyleMetadata) other;
        return version == that.version && minApi == that.minApi && nameRes == that.nameRes
                && summaryRes == that.summaryRes && id.equals(that.id) && kind == that.kind
                && capabilities.equals(that.capabilities);
    }

    @Override public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + nameRes;
        result = 31 * result + summaryRes;
        result = 31 * result + kind.hashCode();
        result = 31 * result + capabilities.hashCode();
        result = 31 * result + version;
        result = 31 * result + minApi;
        return result;
    }

    @Override public String toString() {
        return id + "@" + version + " (" + kind + ")";
    }
}
