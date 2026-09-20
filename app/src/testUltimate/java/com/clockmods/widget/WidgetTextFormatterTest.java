package com.clockmods.widget;
import com.clockmods.widget.model.*;
import com.clockmods.widget.render.*;
import com.clockmods.widget.update.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetTextFormatterTest {
@Test public void formatsRespectZoneAndLocale() {
 long now=java.time.Instant.parse("2026-09-18T17:00:00Z").toEpochMilli();
 java.util.TimeZone sh=java.util.TimeZone.getTimeZone("Asia/Shanghai"), ny=java.util.TimeZone.getTimeZone("America/New_York");
 assertEquals("9月19日",WidgetTextFormatter.formatGregorianDate(now,sh,java.util.Locale.CHINA));
 assertEquals("Sep 18",WidgetTextFormatter.formatGregorianDate(now,ny,java.util.Locale.ENGLISH));
 assertEquals("星期六",WidgetTextFormatter.formatWeekday(now,sh,java.util.Locale.TAIWAN));
 assertEquals("HH:mm:ss",WidgetTextFormatter.timePattern(true,true));assertEquals("h:mm a",WidgetTextFormatter.timePattern(false,false));
 // The locale-aware overload asks ICU for the locale's own arrangement. Under the JVM test stubs
 // that API is a no-op, so it must fall back to the locale-free pattern rather than return null.
 assertEquals("h:mm a",WidgetTextFormatter.timePattern(java.util.Locale.ENGLISH,false,false));
 assertEquals("HH:mm:ss",WidgetTextFormatter.timePattern(java.util.Locale.CHINA,true,true));
 assertNotEquals(WidgetTextFormatter.formatLunar(now,sh,java.util.Locale.CHINA),WidgetTextFormatter.formatLunar(now,ny,java.util.Locale.CHINA));
 assertEquals("国庆节 · 休",WidgetTextFormatter.withHolidayStatus("国庆节","国庆节","休"));
 assertEquals("秋分 · 中秋节 · 休",WidgetTextFormatter.withHolidayStatus("秋分 · 中秋节","中秋节","休"));
 assertEquals("",WidgetTextFormatter.withHolidayStatus("",null,""));
 assertEquals("A · B",WidgetTextFormatter.join("",null,"A","B","A"));
 assertEquals("",WidgetTextFormatter.formatWeatherSummary(null,true));
 com.clockmods.weather.WeatherModels.WeatherDisplayData data=new com.clockmods.weather.WeatherModels.WeatherDisplayData("1",null,null,"Clear","100","20",now);
 assertEquals("Clear",WidgetTextFormatter.formatWeatherSummary(data,true));
 for(int day=1;day<=30;day++) {
  String text=WidgetTextFormatter.formatHolidayAndSolarTerm(now+day*86400000L,sh,java.util.Locale.CHINA);
  assertFalse(text.startsWith(" · ")); assertFalse(text.endsWith(" · "));
 }
}
}
