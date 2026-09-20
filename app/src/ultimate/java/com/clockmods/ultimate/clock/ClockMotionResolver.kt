package com.clockmods.ultimate.clock

import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockStyle
import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.ClockStyleMetadata

/** Resolves the persisted second-hand preference against a style's capabilities. */
object ClockMotionResolver {
    @JvmStatic
    fun resolve(
        style: ClockStyle,
        showSeconds: Boolean,
        requestedMotion: ClockState.SecondHandMotion?,
    ): ClockState.SecondHandMotion {
        val capabilities = style.getMetadata().getCapabilities()
        if (!showSeconds || !capabilities.supports(ClockStyleCapabilities.Capability.SECONDS)) {
            return ClockState.SecondHandMotion.OFF
        }
        // Digital faces always render numeric seconds. An OFF value left by an analog face must
        // not hide them; the separate show-seconds preference remains authoritative.
        if (style.getMetadata().getKind() == ClockStyleMetadata.Kind.DIGITAL) {
            return ClockState.SecondHandMotion.TICK
        }
        val safeMotion = requestedMotion ?: ClockState.SecondHandMotion.TICK
        return if (
            safeMotion == ClockState.SecondHandMotion.SWEEP &&
            !capabilities.supports(ClockStyleCapabilities.Capability.SMOOTH_SECONDS)
        ) {
            ClockState.SecondHandMotion.TICK
        } else {
            safeMotion
        }
    }
}
