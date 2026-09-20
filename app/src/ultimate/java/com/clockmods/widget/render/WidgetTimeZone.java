package com.clockmods.widget.render;

import android.content.Context;
import com.clockmods.background.ClockPreferences;
import java.util.TimeZone;

/** The app's timezone is the single source for clocks, date labels and midnight updates. */
public final class WidgetTimeZone {
    private WidgetTimeZone() { }

    public static TimeZone from(Context context) {
        return resolve(new ClockPreferences(context).getTimeZoneId());
    }

    public static TimeZone resolve(String id) {
        return id == null || id.trim().isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(id.trim());
    }
}
