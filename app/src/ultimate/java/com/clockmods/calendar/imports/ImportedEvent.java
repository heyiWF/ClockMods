package com.clockmods.calendar.imports;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** An expanded occurrence. All-day boundaries are civil dates; timed boundaries are instants. */
public final class ImportedEvent {
    public final String id, title, description, location;
    public final boolean allDay;
    public final long start, end;
    public final LocalDate firstDay, endDay;
    ImportedEvent(String id, String title, String description, String location, boolean allDay,
            long start, long end, LocalDate firstDay, LocalDate endDay) {
        this.id=id; this.title=title; this.description=description; this.location=location;
        this.allDay=allDay; this.start=start; this.end=end; this.firstDay=firstDay; this.endDay=endDay;
    }
    public boolean occursOn(LocalDate day, ZoneId zone) {
        if (allDay) return !day.isBefore(firstDay) && day.isBefore(endDay);
        long from=day.atStartOfDay(zone).toInstant().toEpochMilli();
        long to=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        return start<to && (end>from || end==start && start>=from);
    }
    public int minuteOfDay(LocalDate day, ZoneId zone) {
        if(allDay)return -1;
        ZonedDateTime time=Instant.ofEpochMilli(start).atZone(zone);
        return time.toLocalDate().isBefore(day)?0:time.getHour()*60+time.getMinute();
    }
    public String timeLabel(ZoneId zone) {
        if(allDay)return "";
        ZonedDateTime a=Instant.ofEpochMilli(start).atZone(zone),b=Instant.ofEpochMilli(end).atZone(zone);
        DateTimeFormatter format=DateTimeFormatter.ofPattern(a.toLocalDate().equals(b.toLocalDate())?"HH:mm":"M/d HH:mm",Locale.ROOT);
        return a.format(format)+(end==start?"":"–"+b.format(format));
    }
}
