package com.clockmods.ultimate.clock

/** Keeps the prior text of each time draw call for one live clock canvas. */
internal class ClockDigitTransitionTracker {
    private class Slot(var current: String, var previous: String? = null)

    private val slots = ArrayList<Slot>()
    private var index = 0

    fun beginFrame() {
        index = 0
    }

    fun previousFor(value: String, transitionInProgress: Boolean): String? {
        val slotIndex = index++
        if (slotIndex >= slots.size) {
            slots.add(Slot(value))
            return null
        }
        val slot = slots[slotIndex]
        if (slot.current != value) {
            slot.previous = if (transitionInProgress) slot.current else null
            slot.current = value
        } else if (!transitionInProgress) {
            slot.previous = null
        }
        return slot.previous
    }
}

internal fun changedDigitPositions(previous: String, current: String): List<Int> {
    if (previous.length != current.length) return emptyList()
    return current.indices.filter { index ->
        previous[index] != current[index] && previous[index].isDigit() && current[index].isDigit()
    }
}
