package com.clockmods.widget.provider;
import com.clockmods.widget.model.WidgetKind;
public final class DigitalClockWidgetProvider extends BaseClockModsWidgetProvider {
    @Override protected WidgetKind getWidgetKind() { return WidgetKind.DIGITAL; }
}
