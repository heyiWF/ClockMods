package com.clockmods.widget.render;
import com.clockmods.widget.model.WidgetSizeClass;
public final class WidgetSizeClassResolver {
    private WidgetSizeClassResolver() { }
    public static WidgetSizeClass resolve(float width, float height) {
        if (width < 180 || height < 100) return WidgetSizeClass.COMPACT;
        if (height < 180) return width < 260 ? WidgetSizeClass.SMALL : WidgetSizeClass.WIDE;
        return width < 260 ? WidgetSizeClass.TALL : WidgetSizeClass.LARGE;
    }
}
