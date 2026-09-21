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
 @Test public void oneRowCardsSplitOnWidth() {
  // One-row cards put the lead value and the caption on the same line once the card is wide enough,
  // and stack them below that. Both are short cards, so both stay clear of the two-line classes.
  float[][] cases={{WidgetSizeClassResolver.ROW_MIN_WIDTH-1,56},{WidgetSizeClassResolver.ROW_MIN_WIDTH,56},{300,56},{600,56},{WidgetSizeClassResolver.ROW_MIN_WIDTH-1,99}};
  WidgetSizeClass[] expected={WidgetSizeClass.COMPACT,WidgetSizeClass.ROW,WidgetSizeClass.ROW,WidgetSizeClass.ROW,WidgetSizeClass.COMPACT};
  for(int i=0;i<cases.length;i++) assertEquals(expected[i],WidgetSizeClassResolver.resolve(cases[i][0],cases[i][1]));
 }
 @Test public void rowThresholdOnlySplitsOneRowCards() {
  // The threshold only splits one-row cards: a taller card still falls through to the stacked
  // classes and keeps its module set.
  assertEquals(WidgetSizeClass.SMALL,WidgetSizeClassResolver.resolve(WidgetSizeClassResolver.ROW_MIN_WIDTH,120));
  assertEquals(WidgetSizeClass.WIDE,WidgetSizeClassResolver.resolve(260,120));
  assertEquals(WidgetSizeClass.TALL,WidgetSizeClassResolver.resolve(240,180));
 }
}
