package com.clockmods.sdk.clock;

import java.util.List;

/** Monotonic carousel timing; an incoming transition does not consume its reading window. */
public final class WeatherCarousel {
    private final List<String> items;
    private final int messageIndex;
    public int index;
    public long started = -1L;
    public long transitionStarted = -1L;
    public String previous = "";
    public boolean previousMessage;
    public long previousMessageElapsed;

    public WeatherCarousel(List<String> items, boolean hasMessage) {
        this.items = items;
        messageIndex = hasMessage ? items.size() - 1 : -1;
    }

    public String text() { return items.isEmpty() ? "" : items.get(index); }
    public boolean messageActive() { return !items.isEmpty() && index == messageIndex; }
    public boolean continuous() { return items.size() == 1 && messageActive(); }
    public long elapsed(long now) { return Math.max(0L, now - started); }

    public boolean advance(long now, long messageDisplayMillis, long transitionMillis) {
        if (started < 0L) started = now;
        if (items.size() < 2 || now < started) return false;
        long duration = messageActive() ? Math.max(3000L, messageDisplayMillis) : 3000L;
        if (elapsed(now) < duration) return false;
        previous = text();
        previousMessage = messageActive();
        previousMessageElapsed = elapsed(now);
        index = (index + 1) % items.size();
        transitionStarted = now;
        started = now + Math.max(0L, transitionMillis);
        return true;
    }
}
