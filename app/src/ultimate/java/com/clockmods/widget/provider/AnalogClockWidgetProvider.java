package com.clockmods.widget.provider;
import com.clockmods.widget.model.WidgetKind;
public final class AnalogClockWidgetProvider extends BaseClockModsWidgetProvider {
    @Override protected WidgetKind getWidgetKind() { return WidgetKind.ANALOG; }
}
