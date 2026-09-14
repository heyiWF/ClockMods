package com.clockmods.ultimate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.ultimate.clock.UltimateClockStyles;

import java.util.Calendar;
import java.util.Locale;

/** Static thumbnail using the live style renderer, without timers or preference writes. */
@SuppressLint("ViewConstructor") // Created programmatically with an explicit style, never inflated.
final class SetupClockPreviewView extends View {
    private final ClockStyle style;
    private final ClockState state;
    private ClockRenderContext renderContext;

    SetupClockPreviewView(Context context, String styleId) {
        super(context);
        style = UltimateClockStyles.sharedRegistry().find(styleId);
        Calendar sample = Calendar.getInstance();
        sample.set(2026, Calendar.JUNE, 18, 10, 9, 36);
        sample.set(Calendar.MILLISECOND, 0);
        state = ClockState.builder(sample.getTimeInMillis())
                .timeZone(sample.getTimeZone())
                .locale(Locale.getDefault())
                .use24Hour(true)
                .showSeconds(true)
                .secondHandMotion(ClockState.SecondHandMotion.TICK)
                .dateText("2026 / 06 / 18")
                .build();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        renderContext = width > 0 && height > 0 ? new ClockRenderContext(0, 0, 480,
                height * 480f / width, 1, 1, 0) : null;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (style == null || renderContext == null) return;
        // Render at clock-sized coordinates so minimum text sizes scale with the thumbnail.
        float scale = getWidth() / 480f;
        int save = canvas.save();
        try {
            canvas.scale(scale, scale);
            style.getRenderer().render(canvas, renderContext, state, style.getThemeTokens());
        } finally {
            canvas.restoreToCount(save);
        }
    }
}
