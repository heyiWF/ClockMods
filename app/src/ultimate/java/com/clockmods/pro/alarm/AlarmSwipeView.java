package com.clockmods.pro.alarm;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.clockmods.R;

import java.util.function.Consumer;

/** DeskClock-style pulse, directional hint and circular confirmation for one ringing occurrence. */
@SuppressLint("ViewConstructor")
public final class AlarmSwipeView extends LinearLayout {
    private final FrameLayout strip;
    private final ImageButton handle, snooze, stop;
    private final TextView hint;
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator pulse = ValueAnimator.ofFloat(0f, 1f);
    private ObjectAnimator bounce;
    private final int primary, onPrimary, onSurface, snoozeMinutes;
    private final Consumer<String> command;
    private final Runnable resultFinished;
    private boolean dragging, handled;
    private float downX, downY;

    public AlarmSwipeView(Context context, boolean swapped, int primary, int onPrimary, int onSurface,
                          int snoozeMinutes, Consumer<String> command, Runnable resultFinished) {
        super(context);
        this.primary = primary;
        this.onPrimary = onPrimary;
        this.onSurface = onSurface;
        this.snoozeMinutes = snoozeMinutes;
        this.command = command;
        this.resultFinished = resultFinished;
        setOrientation(VERTICAL);
        strip = new FrameLayout(context);
        addView(strip, new LayoutParams(-1, dp(112)));
        snooze = icon(R.drawable.alarm_ic_snooze, R.string.alarm_snooze, false);
        stop = icon(R.drawable.widget_config_close, R.string.alarm_dismiss, false);
        handle = icon(R.drawable.ic_alarm_filled, R.string.alarm_ringing, true);
        addIcon(swapped ? stop : snooze, Gravity.START, 56);
        addIcon(handle, Gravity.CENTER_HORIZONTAL, 60);
        addIcon(swapped ? snooze : stop, Gravity.END, 56);
        snooze.setOnClickListener(v -> confirm(snooze));
        stop.setOnClickListener(v -> confirm(stop));
        handle.setOnClickListener(v -> showHint(stop));
        hint = new TextView(context);
        hint.setText(R.string.alarm_swipe_hint);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(onSurface);
        hint.setVisibility(INVISIBLE);
        addView(hint);
        pulse.setDuration(1000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(frame -> invalidate());
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private ImageButton icon(int drawable, int description, boolean center) {
        ImageButton button = new ImageButton(getContext());
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(primary);
        disc.setAlpha(center ? 255 : 0);
        button.setBackground(disc);
        button.setPadding(0, 0, 0, 0);
        button.setImageResource(drawable);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setColorFilter(center ? onPrimary : onSurface);
        button.setContentDescription(getResources().getString(description));
        button.setFocusable(true);
        return button;
    }

    private void addIcon(View icon, int gravity, int size) {
        FrameLayout.LayoutParams cell = new FrameLayout.LayoutParams(dp(size), dp(size), gravity | Gravity.CENTER_VERTICAL);
        strip.addView(icon, cell);
    }

    private static float center(View view) { return (view.getLeft() + view.getRight()) / 2f; }

    @Override protected void dispatchDraw(Canvas canvas) {
        if (pulse.isRunning()) {
            float progress = (float) pulse.getAnimatedValue();
            pulsePaint.setColor(primary);
            pulsePaint.setAlpha(Math.round(100 * (1f - progress)));
            canvas.drawCircle(center(handle), strip.getTop() + handle.getTop() + handle.getHeight() / 2f,
                    dp(30) + dp(26) * progress, pulsePaint);
        }
        super.dispatchDraw(canvas);
    }

    private void startPulse() {
        if (isShown() && !handled && !dragging && !pulse.isStarted() && ValueAnimator.areAnimatorsEnabled()) pulse.start();
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); startPulse(); }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (pulse == null) return;
        if (visibility == VISIBLE) startPulse(); else pulse.cancel();
    }
    @Override protected void onDetachedFromWindow() {
        pulse.cancel();
        if (bounce != null) bounce.cancel();
        super.onDetachedFromWindow();
    }

    private void showHint(ImageButton target) {
        hint.setVisibility(VISIBLE);
        hint.setAlpha(0f);
        hint.animate().alpha(1f).setDuration(160).start();
        if (bounce != null) bounce.cancel();
        float offset = (center(target) - center(handle)) * .35f;
        bounce = ObjectAnimator.ofFloat(handle, View.TRANSLATION_X, 0, offset, 0);
        bounce.setDuration(500);
        bounce.start();
    }

    private void progress(ImageButton target, float fraction) {
        target.getBackground().setAlpha(Math.round(255 * fraction));
        target.setColorFilter(fraction > .5f ? onPrimary : onSurface);
        target.setScaleX(1 + .12f * fraction);
        target.setScaleY(1 + .12f * fraction);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) { return true; }

    @SuppressLint("ClickableViewAccessibility") // The three icons expose standard keyboard and accessibility actions.
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (handled) return true;
        float x = event.getX(), y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (bounce != null) bounce.cancel();
                handle.setTranslationX(0);
                downX = x;
                downY = y;
                dragging = x >= handle.getLeft() && x <= handle.getRight() && y >= handle.getTop() && y <= handle.getBottom();
                if (dragging) {
                    pulse.cancel();
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                boolean released = event.getActionMasked() == MotionEvent.ACTION_UP;
                if (!dragging) {
                    if (released && downY >= 0 && downY <= strip.getHeight() && Math.abs(x - downX) < dp(10))
                        showHint(Math.abs(x - center(snooze)) < Math.abs(x - center(stop)) ? snooze : stop);
                    return true;
                }
                float delta = x - downX;
                ImageButton target = (delta < 0) == (center(snooze) < center(handle)) ? snooze : stop;
                float distance = Math.max(1f, Math.abs(center(target) - center(handle)) - target.getWidth() / 3f);
                float fraction = Math.min(1f, Math.abs(delta) / distance);
                handle.setScaleX(1 - fraction);
                handle.setScaleY(1 - fraction);
                progress(target, fraction);
                progress(target == snooze ? stop : snooze, 0f);
                if (released) {
                    resetGesture();
                    if (fraction >= 1 && y >= 0 && y <= strip.getHeight()) target.performClick();
                    else if (Math.hypot(delta, y - downY) <= ViewConfiguration.get(getContext()).getScaledTouchSlop()) handle.performClick();
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                downY = -1;
                resetGesture();
                return true;
            default:
                return true;
        }
    }

    private void resetGesture() {
        dragging = false;
        getParent().requestDisallowInterceptTouchEvent(false);
        handle.setScaleX(1);
        handle.setScaleY(1);
        progress(snooze, 0);
        progress(stop, 0);
        startPulse();
    }

    private void confirm(ImageButton source) {
        if (handled) return;
        handled = true;
        pulse.cancel();
        boolean snoozed = source == snooze;
        FrameLayout root = getRootView().findViewById(android.R.id.content);
        int[] origin = new int[2], window = new int[2];
        source.getLocationInWindow(origin);
        root.getLocationInWindow(window);
        int x = origin[0] - window[0] + source.getWidth() / 2;
        int y = origin[1] - window[1] + source.getHeight() / 2;
        FrameLayout result = new FrameLayout(getContext());
        result.setBackgroundColor(primary);
        result.setClickable(true);
        TextView message = new TextView(getContext());
        message.setText(snoozed ? getResources().getString(R.string.alarm_swipe_snoozed, snoozeMinutes)
                : getResources().getString(R.string.alarm_swipe_stopped));
        message.setTextColor(onPrimary);
        message.setGravity(Gravity.CENTER);
        message.setTextSize(30);
        message.setPadding(dp(24), dp(24), dp(24), dp(24));
        message.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        result.addView(message, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        View content = root.getChildAt(0);
        int accessibility = content.getImportantForAccessibility();
        content.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        root.addView(result, new FrameLayout.LayoutParams(-1, -1));
        result.setVisibility(INVISIBLE);
        androidx.core.view.OneShotPreDrawListener.add(result, () -> {
            result.setVisibility(VISIBLE);
            float radius = (float) Math.hypot(Math.max(x, result.getWidth() - x), Math.max(y, result.getHeight() - y));
            android.animation.Animator reveal = ViewAnimationUtils.createCircularReveal(result, x, y, 0, radius);
            reveal.setDuration(500);
            reveal.start();
            message.setAlpha(0);
            message.animate().alpha(1).setStartDelay(250).setDuration(300).start();
            result.postDelayed(() -> {
                content.setImportantForAccessibility(accessibility);
                root.removeView(result);
                resultFinished.run();
            }, 2000);
        });
        command.accept(snoozed ? AlarmDelivery.SNOOZE : AlarmDelivery.STOP);
    }
}
