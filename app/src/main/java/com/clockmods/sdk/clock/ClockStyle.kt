package com.clockmods.sdk.clock

/** A discoverable style composed from metadata, reusable theme tokens, and a renderer. */
interface ClockStyle {
    fun getMetadata(): ClockStyleMetadata
    fun getThemeTokens(): ClockThemeTokens
    fun getRenderer(): ClockRenderer
}
