package com.clockmods.widget.provider

import com.clockmods.widget.model.WidgetKind

class DigitalClockWidgetProvider : BaseClockModsWidgetProvider() {
    override fun getWidgetKind(): WidgetKind = WidgetKind.DIGITAL
}
