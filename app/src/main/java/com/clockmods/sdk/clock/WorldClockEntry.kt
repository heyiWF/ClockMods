package com.clockmods.sdk.clock

import java.util.Locale
import java.util.TimeZone

/** Immutable city/time-zone data supplied to styles rendering a world-clock strip. */
class WorldClockEntry(
    id: String?,
    city: String?,
    country: String?,
    zoneId: String?,
    flag: String?,
) {
    private val id = required(id, "id")
    private val city = required(city, "city")
    private val country = clean(country)
    private val zoneId = required(zoneId, "zoneId")
    private val flag = clean(flag)

    init {
        if (TimeZone.getTimeZone(this.zoneId).id == "GMT" &&
            !this.zoneId.equals("GMT", ignoreCase = true)
        ) {
            throw IllegalArgumentException("Unknown time zone: $zoneId")
        }
    }

    fun getId() = id
    fun getCity() = city
    fun getCountry() = country
    fun getZoneId() = zoneId
    fun getFlag() = flag

    fun getFlagEmoji(): String {
        if (flag.isEmpty()) return "\uD83C\uDF10"
        if (flag.length != 2) return flag
        val upper = flag.uppercase(Locale.ROOT)
        val first = upper[0]
        val second = upper[1]
        if (first !in 'A'..'Z' || second !in 'A'..'Z') return flag
        return String(Character.toChars(0x1F1E6 + first.code - 'A'.code)) +
            String(Character.toChars(0x1F1E6 + second.code - 'A'.code))
    }

    override fun equals(other: Any?) = other is WorldClockEntry && id == other.id
    override fun hashCode() = id.hashCode()

    private companion object {
        fun clean(value: String?) = value?.trim().orEmpty()
        fun required(value: String?, label: String) = clean(value).also {
            if (it.isEmpty()) throw IllegalArgumentException("$label must not be blank")
        }
    }
}
