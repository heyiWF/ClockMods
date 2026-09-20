package com.clockmods.widget.provider

import com.clockmods.widget.model.WidgetKind

class CalendarWidgetProvider : BaseClockModsWidgetProvider() {
    override fun getWidgetKind(): WidgetKind = WidgetKind.CALENDAR
}
