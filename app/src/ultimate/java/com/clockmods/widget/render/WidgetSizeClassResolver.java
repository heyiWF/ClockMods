package com.clockmods.widget.render;
import com.clockmods.widget.model.WidgetSizeClass;
/**
 * Maps a widget's measured dp box to a layout kind.
 *
 * <p>A one-row card splits on width: past {@link #ROW_MIN_WIDTH} the lead value and the caption
 * column both fit on the single line, so the card lays them out as a row and uses its full width;
 * below that they would fight for the same space, so the card keeps the stacked arrangement.
 * That threshold is the width at which a Chinese date caption fits next to a 12-hour clock, which
 * is the widest the default content gets.
 */
public final class WidgetSizeClassResolver {
    /** Below this the one-row card stacks its two lines instead of placing them side by side. */
    public static final float ROW_MIN_WIDTH = 220f;
    private WidgetSizeClassResolver() { }
    public static WidgetSizeClass resolve(float width, float height) {
        if (height < 100) return width < ROW_MIN_WIDTH ? WidgetSizeClass.COMPACT : WidgetSizeClass.ROW;
        if (width < 180) return WidgetSizeClass.COMPACT;
        if (height < 180) return width < 260 ? WidgetSizeClass.SMALL : WidgetSizeClass.WIDE;
        return width < 260 ? WidgetSizeClass.TALL : WidgetSizeClass.LARGE;
    }
}
