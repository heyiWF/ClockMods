package com.clockmods.ultimate.clock;

import java.util.ArrayList;

/** Prior text for each time draw call in one live clock view. */
final class ClockDigitTransitionTracker {
    private static final class Slot {
        String current;
        String previous;

        Slot(String current) { this.current = current; }
    }

    private final ArrayList<Slot> slots = new ArrayList<>();
    private int index;

    void beginFrame() { index = 0; }

    String previousFor(String value, boolean transitionInProgress) {
        int slotIndex = index++;
        if (slotIndex >= slots.size()) {
            slots.add(new Slot(value));
            return null;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.current.equals(value)) {
            slot.previous = transitionInProgress ? slot.current : null;
            slot.current = value;
        } else if (!transitionInProgress) {
            slot.previous = null;
        }
        return slot.previous;
    }

    static boolean changedDigit(String previous, String current, int index) {
        return previous != null && previous.length() == current.length()
                && index < current.length() && previous.charAt(index) != current.charAt(index)
                && Character.isDigit(previous.charAt(index))
                && Character.isDigit(current.charAt(index));
    }
}
