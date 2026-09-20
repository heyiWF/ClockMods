package com.clockmods.widget.update

object WidgetRefreshPolicy {
    @JvmStatic fun shouldSchedule(weatherCount: Int): Boolean = weatherCount > 0

    @JvmStatic fun shouldRetry(configured: Boolean, transientFailure: Boolean, attempt: Int): Boolean =
        configured && transientFailure && attempt < 2
}
