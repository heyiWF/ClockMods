package com.clockmods.sdk.clock;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class WeatherCarouselTest {
    @Test public void longMessageClearsFadeBeforeTheNextWeatherAndRepeats() {
        MessageMarqueeLayout layout = new MessageMarqueeLayout();
        layout.update(900f, 300f, 32f, 2f, 0L);
        assertEquals(648f, layout.distance, .01f);
        assertEquals(10100L, layout.displayMillis);
        WeatherCarousel carousel = new WeatherCarousel(Arrays.asList("Weather", "Humidity", "Message"), true);
        carousel.advance(0L, layout.displayMillis, 800L);
        assertTrue(carousel.advance(3000L, layout.displayMillis, 800L));
        assertFalse(carousel.advance(6000L, layout.displayMillis, 800L));
        assertTrue(carousel.advance(6800L, layout.displayMillis, 800L));
        assertTrue(carousel.messageActive());
        assertFalse(carousel.advance(17699L, layout.displayMillis, 800L));
        layout.update(900f, 300f, 32f, 2f, carousel.elapsed(17699L));
        assertEquals(layout.distance, layout.offset, .01f);
        float tail = 300f * MessageMarqueeLayout.EDGE_FRACTION + 900f - layout.offset;
        assertEquals(300f * (1f - MessageMarqueeLayout.EDGE_FRACTION), tail, .01f);
        assertTrue(carousel.advance(17700L, layout.displayMillis, 800L));
        assertEquals("Weather", carousel.text());
        assertTrue(carousel.previousMessage);
        assertEquals(10100L, carousel.previousMessageElapsed);
        assertTrue(carousel.advance(21500L, layout.displayMillis, 800L));
        assertTrue(carousel.advance(25300L, layout.displayMillis, 800L));
        assertTrue(carousel.messageActive());
        assertEquals(0L, carousel.elapsed(25300L));
    }

    @Test public void shortAndSingleMessagesDoNotConsumeExtraReadingTime() {
        MessageMarqueeLayout layout = new MessageMarqueeLayout();
        layout.update(200f, 300f, 32f, 2f, 50000L);
        assertEquals(0f, layout.offset, 0f);
        assertEquals(3000L, layout.displayMillis);
        WeatherCarousel carousel = new WeatherCarousel(Arrays.asList("Message"), true);
        assertFalse(carousel.advance(123L, layout.displayMillis, 800L));
        assertFalse(carousel.advance(50000L, layout.displayMillis, 800L));
        assertTrue(carousel.continuous());
        assertEquals(49877L, carousel.elapsed(50000L));
    }
}
