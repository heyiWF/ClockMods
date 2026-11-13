package com.clockmods.pro;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.widget.TextView;
import com.clockmods.ui.ClockTimeText;

/** Only changed digits roll; separators retain their measured, font-specific baseline. */
final class RollingTimeText {
    private final TextView view;
    private String previous = "";
    private ValueAnimator animator;
    private float progress = 1;

    RollingTimeText(TextView view) { this.view = view; }

    void set(String text, boolean animate) {
        if (text.equals(previous)) return;
        if (animator != null) animator.cancel();
        SpannableString value = new SpannableString(ClockTimeText.align(text));
        boolean rolling = animate && view.isLaidOut() && previous.length() == text.length()
                && ValueAnimator.areAnimatorsEnabled();
        if (rolling) for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i)) && text.charAt(i) != previous.charAt(i))
                value.setSpan(new Digit(previous.charAt(i), text.charAt(i)), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        previous = text;
        progress = rolling ? 0 : 1;
        view.setText(value);
        if (rolling) {
            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(240);
            animator.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
            animator.addUpdateListener(frame -> { progress = (float) frame.getAnimatedValue(); view.invalidate(); });
            animator.start();
        }
    }

    void dispose() { if (animator != null) animator.cancel(); }

    private final class Digit extends ReplacementSpan {
        private final String oldValue, newValue;
        Digit(char oldValue, char newValue) { this.oldValue = String.valueOf(oldValue); this.newValue = String.valueOf(newValue); }
        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt metrics) {
            if (metrics != null) paint.getFontMetricsInt(metrics);
            return (int) Math.ceil(Math.max(paint.measureText(oldValue), paint.measureText(newValue)));
        }
        @Override public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int baseline, int bottom, Paint paint) {
            float width = getSize(paint, text, start, end, null);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float height = metrics.descent - metrics.ascent;
            int saved = canvas.save();
            canvas.clipRect(x, baseline + metrics.ascent, x + width, baseline + metrics.descent);
            canvas.drawText(oldValue, x + (width - paint.measureText(oldValue)) / 2, baseline - progress * height, paint);
            canvas.drawText(newValue, x + (width - paint.measureText(newValue)) / 2, baseline + (1 - progress) * height, paint);
            canvas.restoreToCount(saved);
        }
    }
}
