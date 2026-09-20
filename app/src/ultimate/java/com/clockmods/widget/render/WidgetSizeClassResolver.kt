package com.clockmods.widget.render

import com.clockmods.widget.model.WidgetSizeClass

object WidgetSizeClassResolver {
    @JvmStatic fun resolve(width: Float, height: Float): WidgetSizeClass = when {
        width < 180 || height < 100 -> WidgetSizeClass.COMPACT
        height < 180 -> if (width < 260) WidgetSizeClass.SMALL else WidgetSizeClass.WIDE
        else -> if (width < 260) WidgetSizeClass.TALL else WidgetSizeClass.LARGE
    }
}
