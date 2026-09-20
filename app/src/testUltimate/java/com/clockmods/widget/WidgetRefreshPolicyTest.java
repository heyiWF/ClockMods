package com.clockmods.widget;
import com.clockmods.widget.model.*;
import com.clockmods.widget.render.*;
import com.clockmods.widget.update.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetRefreshPolicyTest {
@Test public void onlyUsefulBoundedWork() {
 assertFalse(WidgetRefreshPolicy.shouldSchedule(0)); assertTrue(WidgetRefreshPolicy.shouldSchedule(1));
 assertFalse(WidgetRefreshPolicy.shouldRetry(false,true,0)); assertFalse(WidgetRefreshPolicy.shouldRetry(true,false,0));
 assertTrue(WidgetRefreshPolicy.shouldRetry(true,true,1)); assertFalse(WidgetRefreshPolicy.shouldRetry(true,true,2));
}
@Test public void midnightAccountsForAppZoneAndDst() {
 long now=java.time.Instant.parse("2026-03-08T05:00:00Z").toEpochMilli();
 assertEquals(23*3600000L,WidgetMidnightScheduler.nextMidnight(now,WidgetTimeZone.resolve("America/New_York"))-now);
 assertEquals(java.time.Instant.parse("2026-03-08T16:00:00Z").toEpochMilli(),WidgetMidnightScheduler.nextMidnight(now,WidgetTimeZone.resolve("Asia/Shanghai")));
 assertEquals(java.util.TimeZone.getDefault().getID(),WidgetTimeZone.resolve("").getID());
 assertEquals(java.util.TimeZone.getDefault().getID(),WidgetTimeZone.resolve(null).getID());
 assertEquals("Asia/Shanghai",WidgetTimeZone.resolve(" Asia/Shanghai ").getID());
}
}
