package com.clockmods.widget;
import com.clockmods.widget.model.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetConfigTest {
 @Test public void defaultsAndNormalization() {
  for (WidgetKind k : WidgetKind.values()) {
   WidgetConfig c=WidgetConfig.builder(8,k).build();
   assertEquals(k,c.kind); assertEquals(8,c.appWidgetId); assertFalse(c.showSeconds);
   assertTrue(c.useSystemTimeZone); assertTrue(c.useSystemTimeFormat);
   assertEquals(WidgetConfig.FONT_THEME,c.fontId);
  }
  WidgetConfig c=WidgetConfig.builder(1,WidgetKind.ANALOG).themeId("bad").timeZoneId("bad").useSystemTimeZone(false).showSeconds(true).backgroundAlpha(900).textScale(Float.NaN).tapAction("bad").build();
  assertEquals("system.dynamic",c.themeId); assertTrue(c.useSystemTimeZone); assertFalse(c.showSeconds); assertEquals(255,c.backgroundAlpha); assertEquals(1f,c.textScale,0f); assertEquals("open_clock",c.tapAction);
  assertEquals(.85f,c.toBuilder().textScale(-5).build().textScale,0f);
  assertEquals(0,c.toBuilder().backgroundAlpha(-1).build().backgroundAlpha);
  assertEquals(1.2f,c.toBuilder().textScale(Float.POSITIVE_INFINITY).build().textScale,0f);
 }
 @Test public void fontIdIsValidatedAgainstTheCatalog() {
  WidgetConfig c=WidgetConfig.builder(1,WidgetKind.DIGITAL).fontId("no.such.font").build();
  assertEquals(WidgetConfig.FONT_THEME,c.fontId);
  for (String id : WidgetConfig.FONT_IDS) assertEquals(id,c.toBuilder().fontId(id).build().fontId);
 }
 @Test public void fixedZoneSurvivesSystemChanges() {
  WidgetConfig c=WidgetConfig.builder(1,WidgetKind.DIGITAL).timeZoneId("America/New_York").useSystemTimeZone(false).showSeconds(true).build();
  assertEquals("America/New_York",c.zone().getID()); assertTrue(c.showSeconds);
 }
}
