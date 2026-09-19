package com.clockmods.widget.update;
import android.app.*;
import android.content.*;
import com.clockmods.widget.model.WidgetConfig;
import java.time.*;
import java.util.List;
public final class WidgetMidnightScheduler {
    private WidgetMidnightScheduler() { }
    public static long nextMidnight(long now,List<WidgetConfig> configs) {
        long next=Long.MAX_VALUE;
        for(WidgetConfig c:configs) {
            ZonedDateTime local=Instant.ofEpochMilli(now).atZone(c.zone().toZoneId());
            next=Math.min(next,local.toLocalDate().plusDays(1).atStartOfDay(local.getZone()).toInstant().toEpochMilli());
        }
        return next;
    }
    public static void schedule(Context c,List<WidgetConfig> configs) {
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        PendingIntent alarm=PendingIntent.getBroadcast(c,0,new Intent(c,WidgetDataChangedReceiver.class).setAction(WidgetDataChangedReceiver.MIDNIGHT),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(alarm);
        if(configs.isEmpty()) return;
        // Inexact RTC alarm: no exact-alarm permission or waking a sleeping device for a date label.
        manager.set(AlarmManager.RTC,nextMidnight(System.currentTimeMillis(),configs)+1000,alarm);
    }
}
