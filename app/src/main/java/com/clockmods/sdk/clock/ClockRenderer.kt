package com.clockmods.sdk.clock

import android.graphics.Canvas

/** Stateless rendering contract for SDK clock styles. */
fun interface ClockRenderer {
    fun render(
        canvas: Canvas,
        context: ClockRenderContext,
        state: ClockState,
        theme: ClockThemeTokens,
    )
}
