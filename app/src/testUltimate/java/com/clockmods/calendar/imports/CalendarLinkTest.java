package com.clockmods.calendar.imports;
import org.junit.Test;
import static org.junit.Assert.*;
public class CalendarLinkTest {
    @Test public void normalizesWebcalWithoutLosingSubscriptionQuery() throws Exception {
        assertEquals("https://example.org/private.ics?token=abc%2Fdef", CalendarLink.normalize(" WEBCAL://example.org/private.ics?token=abc%2Fdef ").toString());
    }
    @Test public void acceptsHttpsAndEncodedPaths() throws Exception {
        assertEquals("https://example.org/a%20b.ics", CalendarLink.normalize("https://example.org/a%20b.ics").toString());
    }
    @Test public void rejectsInsecureMalformedAndNonNetworkLinks() throws Exception {
        for (String input : new String[]{"", "http://example.org/a.ics", "file:///etc/a", "content://calendar/a", "https:///a.ics", "https://user:pass@example.org/a", "https://example.org/a#fragment", "javascript:alert(1)"}) {
            try { CalendarLink.normalize(input); fail(input); }
            catch (CalendarImportException failure) { assertEquals("url", failure.reason); }
        }
    }
}
