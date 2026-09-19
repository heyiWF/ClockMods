package com.clockmods.widget;
import com.clockmods.widget.model.*;
import com.clockmods.widget.store.WidgetConfigStore;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetConfigStoreTest {
 @Test public void roundTripAndProviderAuthority() {
  WidgetConfig c=WidgetConfig.builder(42,WidgetKind.WEATHER).themeId("paper.warm").backgroundAlpha(102).showSeconds(true).useSystemTimeFormat(false).use24Hour(false).timeZoneId("America/New_York").useSystemTimeZone(false).darkText(true).build();
  String json=WidgetConfigStore.encode(c);
  assertEquals(json,WidgetConfigStore.encode(WidgetConfigStore.decode(json,42,WidgetKind.WEATHER)));
  WidgetConfig a=WidgetConfigStore.decode(json,43,WidgetKind.ANALOG);
  assertEquals(43,a.appWidgetId); assertEquals(WidgetKind.ANALOG,a.kind); assertFalse(a.showSeconds);
 }
 @Test public void migrationMissingFieldsAndDamage() {
  for(String raw:new String[]{null,"{broken","{}","{\"schemaVersion\":0}","{\"schemaVersion\":99}"}) {
   WidgetConfig c=WidgetConfigStore.decode(raw,5,WidgetKind.CALENDAR);
   assertEquals(1,c.schemaVersion); assertEquals(5,c.appWidgetId); assertEquals(WidgetKind.CALENDAR,c.kind); assertEquals("system.dynamic",c.themeId);
  }
  assertEquals("paper.warm",WidgetConfigStore.decode("{\"themeId\":\"paper.warm\"}",1,WidgetKind.DIGITAL).themeId);
 }
}
