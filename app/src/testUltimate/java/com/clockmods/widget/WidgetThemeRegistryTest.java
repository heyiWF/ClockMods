package com.clockmods.widget;
import com.clockmods.widget.model.*;
import com.clockmods.widget.render.*;
import com.clockmods.widget.update.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetThemeRegistryTest {
@Test public void stableThemeIdsAndLayoutResourcesExist() throws Exception {
 java.util.Set<String> ids=new java.util.HashSet<>(java.util.Arrays.asList(WidgetConfig.THEME_IDS)); assertEquals(6,ids.size());
 assertTrue(ids.contains("system.dynamic"));assertTrue(ids.contains("transparent.clean"));
 java.nio.file.Path root=java.nio.file.Paths.get("src/ultimate/res/layout");
 for(String kind:new String[]{"digital","analog","weather","calendar"}) {
  assertTrue(java.nio.file.Files.exists(root.resolve("widget_"+kind+"_compact.xml")));
  assertTrue(java.nio.file.Files.exists(root.resolve("widget_"+kind+"_compact_serif.xml")));
  assertTrue(java.nio.file.Files.exists(root.resolve("widget_"+kind+"_compact_mono.xml")));
 }
}
}
