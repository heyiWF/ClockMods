package com.clockmods.sdk.clock;

import android.graphics.Canvas;

/** Stateless rendering contract for SDK clock styles. */
public interface ClockRenderer {
    void render(Canvas canvas, ClockRenderContext context, ClockState state,
            ClockThemeTokens theme);
}
