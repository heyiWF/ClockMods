package com.clockmods.widget;
import com.clockmods.widget.render.WidgetSizeClassResolver;
import com.clockmods.widget.model.WidgetSizeClass;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetSizeClassResolverTest {
 @Test public void boundaries() {
  float[][] cases={{179,180},{180,99},{180,100},{259,179},{260,100},{260,179},{180,180},{259,180},{260,180},{500,500}};
  WidgetSizeClass[] expected={WidgetSizeClass.COMPACT,WidgetSizeClass.COMPACT,WidgetSizeClass.SMALL,WidgetSizeClass.SMALL,WidgetSizeClass.WIDE,WidgetSizeClass.WIDE,WidgetSizeClass.TALL,WidgetSizeClass.TALL,WidgetSizeClass.LARGE,WidgetSizeClass.LARGE};
  for(int i=0;i<cases.length;i++) assertEquals(expected[i],WidgetSizeClassResolver.resolve(cases[i][0],cases[i][1]));
 }
}
