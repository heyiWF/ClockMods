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
@Test public void midnightAccountsForDstAndEveryInstance() {
 long now=java.time.Instant.parse("2026-03-08T05:00:00Z").toEpochMilli();
 WidgetConfig ny=WidgetConfig.builder(1,WidgetKind.DIGITAL).useSystemTimeZone(false).timeZoneId("America/New_York").build();
 assertEquals(23*3600000L,WidgetMidnightScheduler.nextMidnight(now,java.util.Arrays.asList(ny))-now);
 WidgetConfig sh=ny.toBuilder().appWidgetId(2).timeZoneId("Asia/Shanghai").build();
 assertEquals(java.time.Instant.parse("2026-03-08T16:00:00Z").toEpochMilli(),WidgetMidnightScheduler.nextMidnight(now,java.util.Arrays.asList(ny,sh)));
 assertEquals(Long.MAX_VALUE,WidgetMidnightScheduler.nextMidnight(now,java.util.Collections.emptyList()));
}
}
