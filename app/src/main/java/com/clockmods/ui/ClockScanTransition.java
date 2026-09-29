package com.clockmods.ui;

import android.graphics.Canvas;

/** A left-to-right sweep with a feathered opacity boundary. */
public final class ClockScanTransition {
    private static final int FEATHER_STEPS = 8;

    public interface Content {
        void draw(float opacity);
    }

    private ClockScanTransition() {}

    public static void draw(Canvas canvas, float left, float right, float top, float bottom,
            float edge, float feather, boolean revealing, Content content) {
        if (right <= left || bottom <= top) return;
        float soft = Math.max(1f, feather);
        if (revealing) {
            band(canvas, left, Math.min(right, edge - soft), top, bottom, 1f, content);
            for (int i = 0; i < FEATHER_STEPS; i++) {
                float x0 = edge - soft + soft * i / FEATHER_STEPS;
                float x1 = edge - soft + soft * (i + 1) / FEATHER_STEPS;
                band(canvas, Math.max(left, x0), Math.min(right, x1), top, bottom,
                        1f - (i + .5f) / FEATHER_STEPS, content);
            }
        } else {
            band(canvas, Math.max(left, edge + soft), right, top, bottom, 1f, content);
            for (int i = 0; i < FEATHER_STEPS; i++) {
                float x0 = edge + soft * i / FEATHER_STEPS;
                float x1 = edge + soft * (i + 1) / FEATHER_STEPS;
                band(canvas, Math.max(left, x0), Math.min(right, x1), top, bottom,
                        (i + .5f) / FEATHER_STEPS, content);
            }
        }
    }

    private static void band(Canvas canvas, float left, float right, float top, float bottom,
            float opacity, Content content) {
        if (right <= left || opacity <= 0f) return;
        int save = canvas.save();
        canvas.clipRect(left, top, right, bottom);
        content.draw(opacity);
        canvas.restoreToCount(save);
    }
}
