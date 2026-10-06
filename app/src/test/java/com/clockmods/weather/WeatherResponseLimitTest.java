package com.clockmods.weather;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherResponseLimitTest {
    @Test public void preservesJsonWhitespaceAndReadsUtf8() throws Exception {
        String body = "{\n\"text\":\"晴\"\n}";
        assertEquals(body, QWeatherClient.read(new ByteArrayInputStream(body.getBytes("UTF-8"))));
    }
    @Test(expected = IOException.class) public void rejectsOversizedSingleLine() throws Exception {
        QWeatherClient.read(new ByteArrayInputStream(new byte[1024 * 1024 + 1]));
    }
    @Test(expected = IOException.class) public void rejectsEmptyStream() throws Exception {
        QWeatherClient.read(null);
    }
}
