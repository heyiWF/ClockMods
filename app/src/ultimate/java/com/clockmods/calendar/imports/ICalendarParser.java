package com.clockmods.calendar.imports;

import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.text.ICalReader;
import biweekly.io.TimezoneAssignment;
import biweekly.property.*;
import biweekly.util.ICalDate;
import biweekly.util.DateTimeComponents;
import biweekly.util.Recurrence;
import biweekly.util.Period;
import biweekly.util.com.google.ical.compat.javautil.DateIterator;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Bounded, offline RFC 5545 file reader. No calendar URL or attachment is fetched. */
public final class ICalendarParser {
    public static final int MAX_BYTES=8*1024*1024, MAX_EVENTS=10000, MAX_OCCURRENCES=10000;
    private static final TimeZone UTC=TimeZone.getTimeZone("UTC");
    private final List<ICalendar> calendars;
    private final TimeZone displayZone;
    public final int eventCount;
    public final String name;

    private ICalendarParser(List<ICalendar> calendars, TimeZone zone, int count, String name) {
        this.calendars=calendars; this.displayZone=zone; this.eventCount=count; this.name=name;
    }
    public static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[16384];int n;
        while((n=input.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new CalendarImportException("limit");out.write(buffer,0,n);}
        return out.toByteArray();
    }
    public static ICalendarParser parse(byte[] bytes,TimeZone zone) throws IOException {
        if(bytes.length>MAX_BYTES)throw new CalendarImportException("limit");
        String text;
        try { text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString(); }
        catch(CharacterCodingException invalid){throw new CalendarImportException("invalid");}
        if(text.startsWith("\uFEFF"))text=text.substring(1);
        preflight(text);
        List<ICalendar> calendars=new ArrayList<>();int count=0;String name="";
        try(ICalReader reader=new ICalReader(text)) {
            reader.setDefaultTimezone(zone);
            ICalendar calendar;
            while((calendar=reader.readNext())!=null) {
                // A bad date/rule must not silently disappear from an otherwise successful import.
                for(biweekly.io.ParseWarning warning:reader.getWarnings()) {
                    Integer code=warning.getCode();
                    // biweekly warns about common IANA IDs without a leading slash, even when resolved.
                    if(Integer.valueOf(37).equals(code)||Integer.valueOf(43).equals(code))continue;
                    throw new CalendarImportException(Integer.valueOf(38).equals(code)?"timezone":"invalid");
                }
                calendars.add(calendar);
                RawProperty title=calendar.getExperimentalProperty("X-WR-CALNAME");
                if(name.isEmpty() && title!=null)name=clean(title.getValue(),200);
                for(VEvent event:calendar.getEvents()) {validate(event);if(++count>MAX_EVENTS)throw new CalendarImportException("limit");}
            }
        } catch(CalendarImportException failure){throw failure;}
        catch(RuntimeException failure){throw new CalendarImportException("invalid");}
        if(count==0)throw new CalendarImportException("empty");
        return new ICalendarParser(calendars,(TimeZone)zone.clone(),count,name);
    }
    private static void preflight(String text) throws CalendarImportException {
        String[] lines=text.replace("\r\n","\n").replace('\r','\n').split("\n",-1);
        if(lines.length>150000)throw new CalendarImportException("limit");
        Deque<String> stack=new ArrayDeque<>();boolean root=false;
        for(String line:lines) {
            if(line.length()>65536)throw new CalendarImportException("limit");
            if(line.startsWith(" ")||line.startsWith("\t")||line.trim().isEmpty())continue;
            String upper=line.toUpperCase(Locale.ROOT);
            if(upper.startsWith("BEGIN:")) {
                String component=upper.substring(6).trim();
                if(stack.isEmpty()){if(!component.equals("VCALENDAR"))throw new CalendarImportException("invalid");root=true;}
                stack.push(component);if(stack.size()>12)throw new CalendarImportException("limit");
            } else if(upper.startsWith("END:")) {
                if(stack.isEmpty()||!stack.pop().equals(upper.substring(4).trim()))throw new CalendarImportException("invalid");
            } else if(stack.isEmpty())throw new CalendarImportException("invalid");
        }
        if(!root||!stack.isEmpty())throw new CalendarImportException("invalid");
    }
    private static boolean cancelled(VEvent event){return event.getStatus()!=null&&event.getStatus().isCancelled();}
    private static void validate(VEvent event) throws CalendarImportException {
        if(event.getRecurrenceId()!=null && event.getRecurrenceId().getRange()!=null)
            throw new CalendarImportException("unsupported");
        if(cancelled(event))return;
        if(event.getDateStart()==null || event.getDateStart().getValue()==null)throw new CalendarImportException("invalid");
        ICalDate start=event.getDateStart().getValue();
        if(event.getDateStart().getParameters().getTimezoneId()!=null)throw new CalendarImportException("timezone");
        if(event.getDateEnd()!=null) {
            ICalDate end=event.getDateEnd().getValue();
            if(end==null||start.hasTime()!=end.hasTime() || event.getDuration()!=null)throw new CalendarImportException("invalid");
            if(start.hasTime()?end.before(start):!civil(end).isAfter(civil(start)))throw new CalendarImportException("invalid");
            if(event.getDateEnd().getParameters().getTimezoneId()!=null)throw new CalendarImportException("timezone");
        }
        if(event.getDuration()!=null && (event.getDuration().getValue()==null||event.getDuration().getValue().isPrior()))throw new CalendarImportException("invalid");
        for(RecurrenceRule rule:event.getProperties(RecurrenceRule.class))validateRule(rule.getValue());
        for(ExceptionRule rule:event.getExceptionRules())validateRule(rule.getValue());
    }
    private static void validateRule(Recurrence rule)throws CalendarImportException {
        if(rule==null||rule.getFrequency()==null)throw new CalendarImportException("invalid");
        if(rule.getInterval()!=null&&rule.getInterval()<1 || rule.getCount()!=null&&rule.getCount()<1)throw new CalendarImportException("invalid");
        // Sub-hourly feeds are unsuitable for day cells and can expand millions of rows.
        String frequency=rule.getFrequency().name();
        if(frequency.equals("SECONDLY")||frequency.equals("MINUTELY"))throw new CalendarImportException("unsupported");
    }
    public List<ImportedEvent> between(LocalDate first,LocalDate exclusiveEnd) throws IOException {
        if(!exclusiveEnd.isAfter(first)||ChronoUnit.DAYS.between(first,exclusiveEnd)>62)throw new IllegalArgumentException("Use a month-sized window");
        List<ImportedEvent> result=new ArrayList<>();
        for(ICalendar calendar:calendars) {
            Map<String,VEvent> masters=new LinkedHashMap<>();Map<String,Map<String,VEvent>> overrides=new LinkedHashMap<>();int index=0;
            for(VEvent event:calendar.getEvents()) {
                String uid=event.getUid()==null?"anonymous:"+index:event.getUid().getValue();index++;
                if(event.getRecurrenceId()==null)masters.put(uid,newer(masters.get(uid),event));
                else {
                    Map<String,VEvent> entries=overrides.computeIfAbsent(uid,key->new LinkedHashMap<>());
                    String key=key(event.getRecurrenceId().getValue());entries.put(key,newer(entries.get(key),event));
                }
            }
            for(Map.Entry<String,VEvent> entry:masters.entrySet()) {
                VEvent event=entry.getValue();if(cancelled(event))continue;
                Map<String,VEvent> changed=overrides.getOrDefault(entry.getKey(),Collections.emptyMap());
                expand(calendar,event,entry.getKey(),changed,first,exclusiveEnd,result);
            }
            // Moved exceptions can enter this window even if their original occurrence did not.
            for(Map.Entry<String,Map<String,VEvent>> group:overrides.entrySet()) {
                if(masters.containsKey(group.getKey())&&cancelled(masters.get(group.getKey())))continue;
                for(VEvent event:group.getValue().values())if(!cancelled(event)) {
                    Span span=new Span(event,zone(calendar,event));
                    add(event,group.getKey()+"/"+key(event.getRecurrenceId().getValue()),moment(event.getDateStart().getValue()),span,first,exclusiveEnd,result);
                }
            }
        }
        result.sort(Comparator.comparing((ImportedEvent event)->!event.allDay).thenComparingLong(event->event.start).thenComparing(event->event.id));
        return Collections.unmodifiableList(result);
    }
    private TimeZone zone(ICalendar calendar,VEvent event) {
        TimezoneAssignment assignment=calendar.getTimezoneInfo().getTimezone(event.getDateStart());
        if(assignment!=null)return assignment.getTimeZone();
        return calendar.getTimezoneInfo().isFloating(event.getDateStart())?displayZone:UTC;
    }
    private void expand(ICalendar calendar,VEvent original,String uid,Map<String,VEvent> changed,
            LocalDate first,LocalDate last,List<ImportedEvent> result)throws IOException {
        boolean allDay=!original.getDateStart().getValue().hasTime();
        TimeZone timezone=allDay?UTC:zone(calendar,original);
        Span span=new Span(original,timezone);
        VEvent recurrence=new VEvent(original);
        if(allDay)normalizeAllDay(recurrence);
        RecurrenceDates initial=new RecurrenceDates();initial.getDates().add(recurrence.getDateStart().getValue());
        recurrence.addRecurrenceDates(initial);
        Map<Long,Span> periodSpans=new HashMap<>();
        for(RecurrenceDates dates:recurrence.getRecurrenceDates())for(Period period:dates.getPeriods()) {
            dates.getDates().add(period.getStartDate());
            periodSpans.put(period.getStartDate().getTime(),new Span(period,timezone));
        }
        ZoneId queryZone=allDay?ZoneOffset.UTC:displayZone.toZoneId();
        long lower=first.atStartOfDay(queryZone).toInstant().toEpochMilli()-span.lookbackMillis();
        long upper=last.atStartOfDay(queryZone).toInstant().toEpochMilli();
        for(Span extra:periodSpans.values())lower=Math.min(lower,first.atStartOfDay(queryZone).toInstant().toEpochMilli()-extra.lookbackMillis());
        try {
            DateIterator dates=recurrence.getDateIterator(timezone);dates.advanceTo(new Date(lower));int iterations=0;
            while(dates.hasNext()) {
                long start=dates.next().getTime();if(start>=upper)break;
                if(++iterations>MAX_OCCURRENCES)throw new CalendarImportException("limit");
                String occurrence=allDay?"D"+Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate():"T"+start;
                if(changed.containsKey(occurrence))continue;
                add(original,uid+"/"+occurrence,start,periodSpans.getOrDefault(start,span),first,last,result);
            }
        } catch(CalendarImportException failure){throw failure;}
        catch(RuntimeException failure){throw new CalendarImportException("invalid");}
    }
    private void add(VEvent source,String id,long start,Span span,LocalDate first,LocalDate last,List<ImportedEvent> result)throws IOException {
        long end=span.end(start);if(end<start)throw new CalendarImportException("invalid");
        boolean allDay=!source.getDateStart().getValue().hasTime();
        LocalDate a=Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate(),b=Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate();
        long from=first.atStartOfDay(displayZone.toZoneId()).toInstant().toEpochMilli(),to=last.atStartOfDay(displayZone.toZoneId()).toInstant().toEpochMilli();
        if(allDay?(!a.isBefore(last)||!b.isAfter(first)):(start>=to||end<from||end==from&&end!=start))return;
        result.add(new ImportedEvent(id,clean(source.getSummary()==null?"":source.getSummary().getValue(),2000),
                clean(source.getDescription()==null?"":source.getDescription().getValue(),16000),
                clean(source.getLocation()==null?"":source.getLocation().getValue(),2000),allDay,start,end,a,b));
        if(result.size()>MAX_OCCURRENCES)throw new CalendarImportException("limit");
    }
    private static VEvent newer(VEvent a,VEvent b) {
        if(a==null)return b;
        int left=a.getSequence()==null?0:a.getSequence().getValue(),right=b.getSequence()==null?0:b.getSequence().getValue();
        if(left!=right)return left>right?a:b;
        long x=a.getDateTimeStamp()==null?0:a.getDateTimeStamp().getValue().getTime();
        long y=b.getDateTimeStamp()==null?0:b.getDateTimeStamp().getValue().getTime();return x>y?a:b;
    }
    private static String clean(String value,int limit){if(value==null)return "";String s=value.replace("\r\n","\n").replace('\r','\n').replace('\u0000',' ').trim();int end=Math.min(limit,s.length());if(end>0&&end<s.length()&&Character.isHighSurrogate(s.charAt(end-1)))end--;return s.substring(0,end);}
    private static LocalDate civil(ICalDate date) {
        DateTimeComponents raw=date.getRawComponents();
        if(raw!=null)return LocalDate.of(raw.getYear(),raw.getMonth(),raw.getDate());
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    private static long moment(ICalDate date){return date.hasTime()?date.getTime():civil(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();}
    private static String key(ICalDate date){return date.hasTime()?"T"+date.getTime():"D"+civil(date);}
    private static ICalDate utcDate(ICalDate date){return new ICalDate(new Date(moment(date)),true);}
    private static void normalizeAllDay(VEvent event) {
        event.getDateStart().setValue(utcDate(event.getDateStart().getValue()));
        for(ExceptionDates dates:event.getExceptionDates())for(int i=0;i<dates.getValues().size();i++)dates.getValues().set(i,utcDate(dates.getValues().get(i)));
        for(RecurrenceDates dates:event.getRecurrenceDates())for(int i=0;i<dates.getDates().size();i++)dates.getDates().set(i,utcDate(dates.getDates().get(i)));
        for(RecurrenceRule rule:event.getProperties(RecurrenceRule.class))if(rule.getValue().getUntil()!=null&&!rule.getValue().getUntil().hasTime())
            rule.setValue(new Recurrence.Builder(rule.getValue()).until(utcDate(rule.getValue().getUntil())).build());
        for(ExceptionRule rule:event.getExceptionRules())if(rule.getValue().getUntil()!=null&&!rule.getValue().getUntil().hasTime())
            rule.setValue(new Recurrence.Builder(rule.getValue()).until(utcDate(rule.getValue().getUntil())).build());
    }
    private static final class Span {
        final long millis;final biweekly.util.Duration duration;final TimeZone zone;
        Span(VEvent event,TimeZone zone)throws CalendarImportException {
            this.zone=zone;duration=event.getDuration()==null?null:event.getDuration().getValue();
            ICalDate start=event.getDateStart().getValue();
            millis=event.getDateEnd()!=null?moment(event.getDateEnd().getValue())-moment(start):start.hasTime()?0:86400000L;
            if(!start.hasTime()&&duration!=null&&(n(duration.getHours())+n(duration.getMinutes())+n(duration.getSeconds())!=0 || n(duration.getWeeks())+n(duration.getDays())<=0))throw new CalendarImportException("invalid");
            check();
        }
        Span(Period period,TimeZone zone)throws CalendarImportException {
            this.zone=zone;duration=period.getDuration();millis=period.getEndDate()==null?0:period.getEndDate().getTime()-period.getStartDate().getTime();check();
        }
        void check()throws CalendarImportException {if(millis<0||lookbackMillis()>3660L*86400000L||duration!=null&&duration.isPrior())throw new CalendarImportException("limit");}
        long end(long start) {
            if(duration==null)return start+millis;
            Calendar c=Calendar.getInstance(zone);c.setTimeInMillis(start);c.add(Calendar.DAY_OF_MONTH,n(duration.getWeeks())*7+n(duration.getDays()));
            return c.getTimeInMillis()+1000L*(n(duration.getHours())*3600L+n(duration.getMinutes())*60L+n(duration.getSeconds()));
        }
        long lookbackMillis(){return duration==null?millis+2*86400000L:(n(duration.getWeeks())*7L+n(duration.getDays())+2)*86400000L+n(duration.getHours())*3600000L+n(duration.getMinutes())*60000L+n(duration.getSeconds())*1000L;}
        static int n(Integer n){return n==null?0:n;}
    }
}
