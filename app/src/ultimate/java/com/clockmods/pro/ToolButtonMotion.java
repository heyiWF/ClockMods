package com.clockmods.pro;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import java.util.WeakHashMap;

/** Material controls retain their ripple and accessibility actions while the shape changes. */
final class ToolButtonMotion {
    private static final WeakHashMap<MaterialButton, Boolean> states = new WeakHashMap<>();
    private static final WeakHashMap<MaterialButton, ValueAnimator> animations = new WeakHashMap<>();

    static void install(MaterialButton button) {
        float density = button.getResources().getDisplayMetrics().density;
        StateListAnimator shape = new StateListAnimator();
        ObjectAnimator pressed = ObjectAnimator.ofInt(button, "cornerRadius", Math.round(20 * density));
        pressed.setDuration(150);
        ObjectAnimator released = ObjectAnimator.ofInt(button, "cornerRadius", Math.round(36 * density));
        released.setDuration(220);
        shape.addState(new int[]{android.R.attr.state_pressed}, pressed);
        shape.addState(new int[]{}, released);
        button.setStateListAnimator(shape);
    }

    static void running(MaterialButton button, boolean running) {
        Boolean previous = states.put(button, running);
        if (previous != null && previous == running) return;
        ValueAnimator old = animations.remove(button);
        if (old != null) old.cancel();
        int background = running ? 0xFFF4C95D : MaterialColors.getColor(button, androidx.appcompat.R.attr.colorPrimary);
        int foreground = running ? 0xFF302500 : MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnPrimary);
        int from = button.getBackgroundTintList().getDefaultColor();
        int fromText = button.getCurrentTextColor();
        ValueAnimator colors = ValueAnimator.ofFloat(0, 1);
        colors.setDuration(previous == null ? 0 : 240);
        ArgbEvaluator evaluator = new ArgbEvaluator();
        colors.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            button.setBackgroundTintList(ColorStateList.valueOf((int) evaluator.evaluate(fraction, from, background)));
            ColorStateList tint = ColorStateList.valueOf((int) evaluator.evaluate(fraction, fromText, foreground));
            button.setTextColor(tint); button.setIconTint(tint);
        });
        colors.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (animations.get(button) == animation) animations.remove(button);
            }
        });
        animations.put(button, colors);
        colors.start();
    }
}
