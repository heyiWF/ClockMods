package com.clockmods.sdk.clock;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Declares optional state and motion features understood by a clock style. */
public final class ClockStyleCapabilities {
    public enum Capability {
        SECONDS,
        SMOOTH_SECONDS,
        DATE,
        TIME_ZONE,
        WEATHER,
        STATUS,
        TWENTY_FOUR_HOUR,
        REDUCED_MOTION
    }

    private static final ClockStyleCapabilities NONE =
            new ClockStyleCapabilities(EnumSet.noneOf(Capability.class));

    private final EnumSet<Capability> values;

    private ClockStyleCapabilities(EnumSet<Capability> values) {
        this.values = values.clone();
    }

    public static ClockStyleCapabilities none() {
        return NONE;
    }

    public static ClockStyleCapabilities of(Capability... values) {
        if (values == null || values.length == 0) {
            return none();
        }
        EnumSet<Capability> result = EnumSet.noneOf(Capability.class);
        for (Capability value : values) {
            if (value == null) {
                throw new IllegalArgumentException("Capability must not be null");
            }
            result.add(value);
        }
        return new ClockStyleCapabilities(result);
    }

    public boolean supports(Capability capability) {
        return capability != null && values.contains(capability);
    }

    public Set<Capability> asSet() {
        EnumSet<Capability> copy = EnumSet.noneOf(Capability.class);
        copy.addAll(values);
        return Collections.unmodifiableSet(copy);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClockStyleCapabilities
                && values.equals(((ClockStyleCapabilities) other).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
