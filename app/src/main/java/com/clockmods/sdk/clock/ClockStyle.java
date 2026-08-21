package com.clockmods.sdk.clock;

/** A discoverable style composed from metadata, reusable theme tokens, and a renderer. */
public interface ClockStyle {
    ClockStyleMetadata getMetadata();
    ClockThemeTokens getThemeTokens();
    ClockRenderer getRenderer();
}
