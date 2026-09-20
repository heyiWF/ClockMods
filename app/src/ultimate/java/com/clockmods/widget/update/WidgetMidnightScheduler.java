package com.clockmods.widget.update;
import android.app.*;
import android.content.*;
import com.clockmods.widget.model.WidgetConfig;
import com.clockmods.widget.render.WidgetTimeZone;
import java.time.*;
import java.util.List;
public final class WidgetMidnightScheduler {
    private WidgetMidnightScheduler() { }
    public static long nextMidnight(long now,java.util.TimeZone zone) {
        ZonedDateTime local=Instant.ofEpochMilli(now).atZone(zone.toZoneId());
        return local.toLocalDate().plusDays(1).atStartOfDay(local.getZone()).toInstant().toEpochMilli();
    }
    public static void schedule(Context c,List<WidgetConfig> configs) {
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        PendingIntent alarm=PendingIntent.getBroadcast(c,0,new Intent(c,WidgetDataChangedReceiver.class).setAction(WidgetDataChangedReceiver.MIDNIGHT),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(alarm);
        if(configs.isEmpty()) return;
        // Inexact RTC alarm: no exact-alarm permission or waking a sleeping device for a date label.
        manager.set(AlarmManager.RTC,nextMidnight(System.currentTimeMillis(),WidgetTimeZone.from(c))+1000,alarm);
    }
}
