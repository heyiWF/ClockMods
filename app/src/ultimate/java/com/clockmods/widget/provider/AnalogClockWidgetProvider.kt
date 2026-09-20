package com.clockmods.widget.provider

import com.clockmods.widget.model.WidgetKind

class AnalogClockWidgetProvider : BaseClockModsWidgetProvider() {
    override fun getWidgetKind(): WidgetKind = WidgetKind.ANALOG
}
