package com.clockmods.widget;
import com.clockmods.widget.model.*;
import com.clockmods.widget.render.*;
import com.clockmods.widget.update.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetWeatherIconFactoryTest {
@Test public void boundsAndUnsafeAssetNames() {
 assertEquals(16,WidgetWeatherIconFactory.boundedSize(-1)); assertEquals(144,WidgetWeatherIconFactory.boundedSize(10000));
 assertEquals("999",WidgetWeatherIconFactory.normalizedCode("../secret")); assertEquals("999",WidgetWeatherIconFactory.normalizedCode(null)); assertEquals("100",WidgetWeatherIconFactory.normalizedCode("100"));
}
}
