package com.clockmods.calendar.imports;

import com.clockmods.pro.schedule.ScheduleItem;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ImportedSchedulesTest {
    @Test public void mergeKeepsManualStorageSeparateAndSortsAllDayFirst() {
        ScheduleItem manual = new ScheduleItem("manual", "Manual", 8, 0);
        ScheduleItem timed = new ScheduleItem("import:timed", "Meeting", 9, 0, "source", "09:00–10:30", "Details");
        ScheduleItem allDay = new ScheduleItem("import:day", "Holiday", -1, -1, "source", "", "Details");
        List<ScheduleItem> originals = new ArrayList<>(Collections.singletonList(manual));
        List<ScheduleItem> imported = Arrays.asList(timed, allDay);
        assertEquals(Arrays.asList(allDay, manual, timed), ImportedSchedules.merge(originals, imported));
        assertEquals(Collections.singletonList(manual), originals);
        assertEquals(Arrays.asList(timed, allDay), imported);
    }
    @Test public void importedRangesAndManualTimeKeepTheirOwnPresentation() {
        assertEquals("09:15", new ScheduleItem("m", "Manual", 9, 15).timeLabel());
        ScheduleItem timed = new ScheduleItem("i", "Imported", 9, 0, "source", "09:00–10:30", "Details");
        assertEquals("09:00–10:30", timed.timeLabel());
        assertEquals("", new ScheduleItem("a", "All-day", -1, -1, "source", "", "Details").timeLabel());
        assertEquals(Arrays.asList("Imported"), ImportedSchedules.labels(Collections.singletonList(timed)));
    }
    @Test public void updatedSourceDetailsInvalidateRowEquality() {
        ScheduleItem first = new ScheduleItem("i", "Imported", 9, 0, "source", "09:00–10:30", "Room A");
        ScheduleItem changed = new ScheduleItem("i", "Imported", 9, 0, "source", "09:00–10:30", "Room B");
        assertNotEquals(first, changed);
    }
}
