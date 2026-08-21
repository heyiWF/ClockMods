package com.clockmods.sdk.clock;

import java.util.Locale;

/** Stable identity and compatibility information for a clock style. */
public final class ClockStyleMetadata {
    public enum Kind { ANALOG, DIGITAL, HYBRID }

    private final String id;
    private final String name;
    private final String description;
    private final Kind kind;
    private final ClockStyleCapabilities capabilities;
    private final int version;
    private final int minApi;

    public ClockStyleMetadata(String id, String name, String description, Kind kind,
            ClockStyleCapabilities capabilities, int version, int minApi) {
        this.id = requireStyleId(id);
        this.name = requireText(name, "name");
        this.description = requireText(description, "description");
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("capabilities must not be null");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be at least 1");
        }
        if (minApi < 1) {
            throw new IllegalArgumentException("minApi must be at least 1");
        }
        this.kind = kind;
        this.capabilities = capabilities;
        this.version = version;
        this.minApi = minApi;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Kind getKind() {
        return kind;
    }

    public ClockStyleCapabilities getCapabilities() {
        return capabilities;
    }

    public int getVersion() {
        return version;
    }

    public int getMinApi() {
        return minApi;
    }

    public boolean supportsApi(int apiLevel) {
        return apiLevel >= minApi;
    }

    private static String requireStyleId(String value) {
        String id = requireText(value, "id");
        if (!id.equals(id.toLowerCase(Locale.US))
                || !id.matches("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)+")) {
            throw new IllegalArgumentException(
                    "id must be a lowercase, namespaced identifier: " + id);
        }
        return id;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ClockStyleMetadata)) return false;
        ClockStyleMetadata that = (ClockStyleMetadata) other;
        return id.equals(that.id)
                && name.equals(that.name)
                && description.equals(that.description)
                && kind == that.kind
                && capabilities.equals(that.capabilities)
                && version == that.version
                && minApi == that.minApi;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + kind.hashCode();
        result = 31 * result + capabilities.hashCode();
        result = 31 * result + version;
        return 31 * result + minApi;
    }

    @Override
    public String toString() {
        return id + "@" + version + " (" + kind + ")";
    }
}
