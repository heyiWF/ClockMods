package com.clockmods.calendar.imports;

import android.content.Context;
import com.clockmods.R;
import com.clockmods.pro.schedule.ScheduleItem;
import java.time.LocalDate;
import java.util.*;

/** Presentation-only imported rows; these are never written to the manual schedule store. */
public final class ImportedSchedules {
    private ImportedSchedules() { }
    public static List<ScheduleItem> on(Context context, UserCalendarStore store,
            int year, int month, int day, TimeZone zone) {
        List<ScheduleItem> rows = new ArrayList<>();
        LocalDate date = LocalDate.of(year, month + 1, day);
        for (UserCalendarStore.Occurrence occurrence : store.eventsOn(year, month, day, zone)) {
            ImportedEvent event = occurrence.event;
            String title = event.title.isEmpty() ? context.getString(R.string.calendar_import_untitled) : event.title;
            int minute = event.minuteOfDay(date, zone.toZoneId());
            String range = event.timeLabel(zone.toZoneId());
            String dates = event.allDay ? event.firstDay + (event.endDay.equals(event.firstDay.plusDays(1))
                    ? "" : " – " + event.endDay.minusDays(1)) : "";
            String details = event.allDay ? dates + " · " + context.getString(R.string.ultimate_schedule_all_day) : range;
            if (!event.location.isEmpty()) details += "\n\n" + event.location;
            if (!event.description.isEmpty()) details += "\n\n" + event.description;
            details += "\n\n" + context.getString(R.string.calendar_import_source, occurrence.source.title)
                    + "\n" + context.getString(R.string.calendar_import_readonly);
            rows.add(new ScheduleItem("import:" + occurrence.id(), title, minute < 0 ? -1 : minute / 60,
                    minute < 0 ? -1 : minute % 60, occurrence.source.id, range, details));
        }
        return Collections.unmodifiableList(rows);
    }
    public static List<String> labels(List<ScheduleItem> rows) {
        List<String> labels = new ArrayList<>();
        for (ScheduleItem row : rows) labels.add(row.title);
        return Collections.unmodifiableList(labels);
    }
    public static List<ScheduleItem> merge(List<ScheduleItem> manual, List<ScheduleItem> imported) {
        List<ScheduleItem> rows = new ArrayList<>(manual); rows.addAll(imported);
        rows.sort(Comparator.comparingInt(ScheduleItem::sortKey));
        return Collections.unmodifiableList(rows);
    }
}
