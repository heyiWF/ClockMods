package com.clockmods.widget;
import com.clockmods.widget.model.*;
import com.clockmods.widget.render.*;
import com.clockmods.widget.update.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetPendingIntentFactoryTest {
@Test public void actionsAndInstancesNeverShareIdentity() {
 java.util.Set<Integer> codes=new java.util.HashSet<>(); java.util.Set<String> uris=new java.util.HashSet<>();
 for(int id=1;id<1000;id++) for(WidgetPendingIntentFactory.Action a:WidgetPendingIntentFactory.Action.values()) {
  assertTrue(codes.add(WidgetPendingIntentFactory.requestCode(id,a))); assertTrue(uris.add(WidgetPendingIntentFactory.identity(id,a)));
 }
}
}
