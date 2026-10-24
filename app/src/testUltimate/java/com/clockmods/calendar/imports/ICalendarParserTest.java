package com.clockmods.calendar.imports;

import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public class ICalendarParserTest {
    private static String event(String fields){return "BEGIN:VEVENT\r\nUID:test\r\nDTSTAMP:20260101T000000Z\r\n"+fields+"\r\nEND:VEVENT\r\n";}
    private static ICalendarParser parse(String events,String zone)throws Exception {
        return ICalendarParser.parse(("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ClockMods//Test//EN\r\n"+events+"END:VCALENDAR\r\n").getBytes(StandardCharsets.UTF_8),TimeZone.getTimeZone(zone));
    }
    private static List<ImportedEvent> october(ICalendarParser parser)throws Exception{return parser.between(LocalDate.of(2026,10,1),LocalDate.of(2026,11,1));}
    @Test public void allDayEndIsExclusiveAndIndependentOfDisplayZone()throws Exception {
        String data=event("DTSTART;VALUE=DATE:20261001\r\nDTEND;VALUE=DATE:20261003\r\nSUMMARY:Holiday");
        for(String zone:new String[]{"Pacific/Honolulu","Asia/Seoul","Pacific/Kiritimati"}){
            ImportedEvent e=october(parse(data,zone)).get(0);assertTrue(e.allDay);
            assertTrue(e.occursOn(LocalDate.of(2026,10,1),ZoneId.of(zone)));
            assertTrue(e.occursOn(LocalDate.of(2026,10,2),ZoneId.of(zone)));
            assertFalse(e.occursOn(LocalDate.of(2026,10,3),ZoneId.of(zone)));
        }
    }
    @Test public void floatingTimeUsesAppTimezoneAndReportsRange()throws Exception {
        ImportedEvent e=october(parse(event("DTSTART:20261021T090000\r\nDTEND:20261021T103000\r\nSUMMARY:Meeting"),"Asia/Tokyo")).get(0);
        assertEquals(Instant.parse("2026-10-21T00:00:00Z").toEpochMilli(),e.start);
        assertEquals("09:00–10:30",e.timeLabel(ZoneId.of("Asia/Tokyo")));
    }
    @Test public void crossMidnightAppearsOnBothDaysButNotExclusiveEnd()throws Exception {
        ImportedEvent e=october(parse(event("DTSTART:20261020T150000Z\r\nDTEND:20261021T160000Z\r\nSUMMARY:Night shift"),"Asia/Shanghai")).get(0);
        ZoneId z=ZoneId.of("Asia/Shanghai");
        assertTrue(e.occursOn(LocalDate.of(2026,10,20),z));assertTrue(e.occursOn(LocalDate.of(2026,10,21),z));
        assertFalse(e.occursOn(LocalDate.of(2026,10,22),z));
    }
    @Test public void pointEventAndImplicitAllDayHaveDifferentDurations()throws Exception {
        ImportedEvent timed=october(parse(event("DTSTART:20261021T090000Z\r\nSUMMARY:Call"),"UTC")).get(0);
        assertEquals(timed.start,timed.end);assertEquals("09:00",timed.timeLabel(ZoneOffset.UTC));
        ImportedEvent day=october(parse(event("DTSTART;VALUE=DATE:20261021"),"UTC")).get(0);
        assertEquals(LocalDate.of(2026,10,22),day.endDay);
    }
    @Test public void recurrenceIncludesStartExcludesDatesAndAddsRDates()throws Exception {
        List<ImportedEvent> events=october(parse(event("DTSTART;VALUE=DATE:20261001\r\nRRULE:FREQ=DAILY;COUNT=4\r\nEXDATE;VALUE=DATE:20261002\r\nRDATE;VALUE=DATE:20261008"),"Pacific/Honolulu"));
        assertEquals(Arrays.asList(1,3,4,8),days(events));
        assertEquals(Arrays.asList(1,8),days(october(parse(event("DTSTART;VALUE=DATE:20261001\r\nRDATE;VALUE=DATE:20261008"),"UTC"))));
    }
    @Test public void movedAndCancelledOccurrencesReplaceOriginals()throws Exception {
        String data=event("DTSTART:20261001T090000Z\r\nRRULE:FREQ=DAILY;COUNT=3\r\nSUMMARY:Original")
                +event("RECURRENCE-ID:20261002T090000Z\r\nDTSTART:20261008T110000Z\r\nSUMMARY:Moved")
                +event("RECURRENCE-ID:20261003T090000Z\r\nSTATUS:CANCELLED");
        List<ImportedEvent> events=october(parse(data,"UTC"));assertEquals(Arrays.asList(1,8),days(events));assertEquals("Moved",events.get(1).title);
    }
    @Test public void occurrenceMovedIntoMonthIsIncluded()throws Exception {
        String data=event("DTSTART:20260901T090000Z\r\nRRULE:FREQ=DAILY;COUNT=2")
                +event("RECURRENCE-ID:20260902T090000Z\r\nDTSTART:20261008T110000Z\r\nSUMMARY:Moved");
        assertEquals(Arrays.asList(8),days(october(parse(data,"UTC"))));
    }
    @Test public void periodRDateKeepsItsOwnEnd()throws Exception {
        List<ImportedEvent> events=october(parse(event("DTSTART:20261001T090000Z\r\nDTEND:20261001T100000Z\r\nRDATE;VALUE=PERIOD:20261008T090000Z/20261008T120000Z"),"UTC"));
        assertEquals(2,events.size());assertEquals("09:00–12:00",events.get(1).timeLabel(ZoneOffset.UTC));
    }
    @Test public void zonedRecurrenceKeepsWallTimeAcrossDst()throws Exception {
        ICalendarParser parser=parse(event("DTSTART;TZID=America/New_York:20261031T090000\r\nDURATION:PT1H\r\nRRULE:FREQ=DAILY;COUNT=3"),"Asia/Tokyo");
        List<ImportedEvent> events=parser.between(LocalDate.of(2026,10,31),LocalDate.of(2026,11,4));
        assertEquals(3,events.size());assertEquals(25*3600000L,events.get(1).start-events.get(0).start);
        assertEquals("09:00–10:00",events.get(1).timeLabel(ZoneId.of("America/New_York")));
    }
    @Test public void foldedAndEscapedTextIsDecoded()throws Exception {
        ImportedEvent e=october(parse(event("DTSTART;VALUE=DATE:20261021\r\nSUMMARY:Team\\, plan\r\n ning\r\nDESCRIPTION:One\\nTwo\\;Three"),"UTC")).get(0);
        assertEquals("Team, planning",e.title);assertEquals("One\nTwo;Three",e.description);
    }
    @Test public void malformedAndUnsupportedFilesAreNotPublished()throws Exception {
        for(String field:new String[]{"DTSTART:bad-date","DTSTART:20261021T090000Z\r\nRRULE:FREQ=MINUTELY","DTSTART;TZID=No/Such_Zone:20261021T090000"}) {
            try{parse(event(field),"UTC");fail(field);}catch(CalendarImportException expected){}
        }
        try{ICalendarParser.parse("BEGIN:VCALENDAR\nBEGIN:VEVENT".getBytes(StandardCharsets.UTF_8),TimeZone.getTimeZone("UTC"));fail();}catch(CalendarImportException expected){}
    }
    @Test public void suppliedPrivateFile()throws Exception {
        String path=System.getenv("CLOCKMODS_TEST_ICS");Assume.assumeTrue(path!=null);
        ICalendarParser parser=ICalendarParser.parse(Files.readAllBytes(Paths.get(path)),TimeZone.getTimeZone("Asia/Shanghai"));
        assertEquals(317,parser.eventCount);
        int occurrences=0;
        for(int year=2021;year<=2031;year++)for(int month=1;month<=12;month++){
            LocalDate first=LocalDate.of(year,month,1);
            for(ImportedEvent event:parser.between(first,first.plusMonths(1))){assertTrue(event.allDay);occurrences++;}
        }
        assertEquals(317,occurrences);
    }
    @Test public void latestSequenceWinsAndCancelledSeriesDisappears()throws Exception {
        String original=event("DTSTART;VALUE=DATE:20261001\r\nSEQUENCE:1\r\nSUMMARY:Old");
        String newer=event("DTSTART;VALUE=DATE:20261003\r\nSEQUENCE:2\r\nSUMMARY:New");
        List<ImportedEvent> events=october(parse(newer+original,"UTC"));
        assertEquals(1,events.size());assertEquals("New",events.get(0).title);
        assertEquals(Arrays.asList(3),days(events));
        assertTrue(october(parse(original+event("STATUS:CANCELLED\r\nSEQUENCE:3"),"UTC")).isEmpty());
    }
    @Test public void monthlyWeekdayRuleAndUntilAreExpanded()throws Exception {
        ICalendarParser parser=parse(event("DTSTART:20260105T090000Z\r\nRRULE:FREQ=MONTHLY;BYDAY=1MO;UNTIL=20261005T090000Z"),"UTC");
        assertEquals(Arrays.asList(5),days(october(parser)));
        assertTrue(parser.between(LocalDate.of(2026,11,1),LocalDate.of(2026,12,1)).isEmpty());
    }
    @Test public void nominalDayDurationFollowsDstWhileHourDurationIsExact()throws Exception {
        String start="DTSTART;TZID=America/New_York:20261031T120000\r\n";
        ImportedEvent day=october(parse(event(start+"DURATION:P1D"),"UTC")).get(0);
        ImportedEvent hours=october(parse(event(start+"DURATION:PT24H"),"UTC")).get(0);
        assertEquals(25*3600000L,day.end-day.start);assertEquals(24*3600000L,hours.end-hours.start);
    }
    @Test public void embeddedTimezoneIsResolvedOffline()throws Exception {
        String zone="BEGIN:VTIMEZONE\r\nTZID:Custom/Audit\r\nBEGIN:STANDARD\r\nDTSTART:19700101T000000\r\nTZOFFSETFROM:+0530\r\nTZOFFSETTO:+0530\r\nEND:STANDARD\r\nEND:VTIMEZONE\r\n";
        ImportedEvent e=october(parse(zone+event("DTSTART;TZID=Custom/Audit:20261021T090000\r\nDURATION:PT1H"),"UTC")).get(0);
        assertEquals("03:30–04:30",e.timeLabel(ZoneOffset.UTC));
    }
    @Test public void invalidAllDayDurationAndThisAndFutureAreRejected()throws Exception {
        for(String fields:new String[]{"DTSTART;VALUE=DATE:20261001\r\nDURATION:P0D", "DTSTART;VALUE=DATE:20261001\r\nDURATION:PT1H", "DTSTART:20261001T090000Z\r\nRECURRENCE-ID;RANGE=THISANDFUTURE:20261001T090000Z"}) {
            try{october(parse(event(fields),"UTC"));fail(fields);}catch(CalendarImportException expected){}
        }
    }
    @Test public void oversizedInputAndInvalidUtf8AreRejected()throws Exception {
        try{ICalendarParser.readBytes(new java.io.ByteArrayInputStream(new byte[ICalendarParser.MAX_BYTES+1]));fail();}
        catch(CalendarImportException expected){assertEquals("limit",expected.reason);}
        try{ICalendarParser.parse(new byte[]{(byte)0xC3,0x28},TimeZone.getTimeZone("UTC"));fail();}
        catch(CalendarImportException expected){assertEquals("invalid",expected.reason);}
    }
    private static List<Integer> days(List<ImportedEvent> events){List<Integer> out=new ArrayList<>();for(ImportedEvent e:events)out.add(Instant.ofEpochMilli(e.start).atZone(ZoneOffset.UTC).getDayOfMonth());return out;}
}
