package com.clockmods.widget.provider

import com.clockmods.widget.model.WidgetKind

class WeatherClockWidgetProvider : BaseClockModsWidgetProvider() {
    override fun getWidgetKind(): WidgetKind = WidgetKind.WEATHER
}
