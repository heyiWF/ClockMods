package com.clockmods.calendar.imports;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/** One-shot HTTPS import. Never retain subscription tokens or log the source URL. */
public final class CalendarLink {
    private CalendarLink() { }
    public static URL normalize(String input) throws IOException {
        try {
            String text = input.trim();
            if (text.regionMatches(true, 0, "webcal://", 0, 9)) text = "https://" + text.substring(9);
            URI uri = new URI(text);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null)
                throw new CalendarImportException("url");
            return uri.toURL();
        } catch (Exception failure) { throw new CalendarImportException("url"); }
    }
    public static UserCalendarStore.Source importFrom(UserCalendarStore store, String link) throws IOException {
        URL url = normalize(link);
        for (int redirect = 0; redirect <= 5; redirect++) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "text/calendar, application/octet-stream;q=0.8");
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new CalendarImportException("network");
                    url = normalize(new URL(url, location).toString());
                    continue;
                }
                if (status != 200) throw new CalendarImportException("network");
                if (connection.getContentLengthLong() > 8L * 1024 * 1024)
                    throw new CalendarImportException("limit");
                String path = url.getPath();
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (name.isEmpty() || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".ics")) name = "calendar.ics";
                try (InputStream stream = connection.getInputStream()) { return store.importStream(stream, name); }
            } catch (CalendarImportException failure) { throw failure; }
            catch (IOException failure) { throw new CalendarImportException("network"); }
            finally { connection.disconnect(); }
        }
        throw new CalendarImportException("network");
    }
}
