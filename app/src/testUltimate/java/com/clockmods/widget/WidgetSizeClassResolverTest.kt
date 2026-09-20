package com.clockmods.widget

import com.clockmods.widget.model.WidgetSizeClass
import com.clockmods.widget.render.WidgetSizeClassResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeClassResolverTest {
    @Test
    fun boundaries() {
        val cases = arrayOf(
            floatArrayOf(179f, 180f),
            floatArrayOf(180f, 99f),
            floatArrayOf(180f, 100f),
            floatArrayOf(259f, 179f),
            floatArrayOf(260f, 100f),
            floatArrayOf(260f, 179f),
            floatArrayOf(180f, 180f),
            floatArrayOf(259f, 180f),
            floatArrayOf(260f, 180f),
            floatArrayOf(500f, 500f),
        )
        val expected = arrayOf(
            WidgetSizeClass.COMPACT,
            WidgetSizeClass.COMPACT,
            WidgetSizeClass.SMALL,
            WidgetSizeClass.SMALL,
            WidgetSizeClass.WIDE,
            WidgetSizeClass.WIDE,
            WidgetSizeClass.TALL,
            WidgetSizeClass.TALL,
            WidgetSizeClass.LARGE,
            WidgetSizeClass.LARGE,
        )
        for (index in cases.indices) {
            assertEquals(expected[index], WidgetSizeClassResolver.resolve(cases[index][0], cases[index][1]))
        }
    }
}
