package com.clockmods.sdk.clock

import java.util.Collections
import java.util.EnumSet

/** Declares optional state and motion features understood by a clock style. */
class ClockStyleCapabilities private constructor(values: EnumSet<Capability>) {
    enum class Capability {
        SECONDS, SMOOTH_SECONDS, DATE, TIME_ZONE, WEATHER, STATUS, TWENTY_FOUR_HOUR, WORLD_CLOCK,
    }

    private val values = values.clone() as EnumSet<Capability>

    fun supports(capability: Capability?) = capability != null && values.contains(capability)

    fun asSet(): Set<Capability> = Collections.unmodifiableSet(values.clone() as EnumSet<Capability>)

    override fun equals(other: Any?) = other is ClockStyleCapabilities && values == other.values
    override fun hashCode() = values.hashCode()
    override fun toString() = values.toString()

    companion object {
        private val NONE = ClockStyleCapabilities(EnumSet.noneOf(Capability::class.java))

        @JvmStatic
        fun none() = NONE

        @JvmStatic
        fun of(vararg capabilities: Capability?): ClockStyleCapabilities {
            if (capabilities.isEmpty()) return none()
            val result = EnumSet.noneOf(Capability::class.java)
            capabilities.forEach { result.add(requireNotNull(it) { "Capability must not be null" }) }
            return ClockStyleCapabilities(result)
        }
    }
}
