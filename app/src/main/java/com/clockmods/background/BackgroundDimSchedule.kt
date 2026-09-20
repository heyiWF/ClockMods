package com.clockmods.background

object BackgroundDimSchedule {
    @JvmStatic
    fun isActive(currentMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        if (startMinutes == endMinutes) return false
        return if (startMinutes < endMinutes) {
            currentMinutes >= startMinutes && currentMinutes < endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}
