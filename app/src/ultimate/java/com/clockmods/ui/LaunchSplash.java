package com.clockmods.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.PathInterpolator;

/** Android 12+ system launch surface; the launcher sector assembles before the exit fade. */
public final class LaunchSplash {
    private LaunchSplash() { }
    public static void install(Activity activity) {
        activity.getSplashScreen().setOnExitAnimationListener(splash -> {
            if (!ValueAnimator.areAnimatorsEnabled()) { splash.remove(); return; }
            View icon = splash.getIconView();
            AnimatorSet exit = new AnimatorSet();
            ObjectAnimator fade = ObjectAnimator.ofFloat(splash, View.ALPHA, 1f, 0f);
            if (icon == null) exit.playTogether(fade);
            else exit.playTogether(fade,
                    ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.03f),
                    ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.03f));
            // Finish the short branding sequence if the first frame was ready early.
            // Slow starts and static warm-start icons incur no additional wait.
            java.time.Instant started = splash.getIconAnimationStart();
            java.time.Duration duration = splash.getIconAnimationDuration();
            long remaining = started == null || duration == null ? 0L
                    : duration.toMillis() - java.time.Duration.between(started, java.time.Instant.now()).toMillis();
            exit.setStartDelay(Math.max(0L, Math.min(920L, remaining)));
            exit.setDuration(220);
            exit.setInterpolator(new PathInterpolator(.2f, 0f, 0f, 1f));
            exit.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) { splash.remove(); }
                @Override public void onAnimationCancel(Animator animation) { splash.remove(); }
            });
            exit.start();
        });
    }
}
