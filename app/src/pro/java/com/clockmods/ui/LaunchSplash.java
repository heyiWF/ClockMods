package com.clockmods.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.PathInterpolator;

/** Android 12+ system launch surface; no extra Activity and no artificial startup delay. */
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
                    ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.12f),
                    ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.12f),
                    ObjectAnimator.ofFloat(icon, View.TRANSLATION_Y, 0f, -12f * activity.getResources().getDisplayMetrics().density));
            exit.setDuration(320);
            exit.setInterpolator(new PathInterpolator(.2f, 0f, 0f, 1f));
            exit.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) { splash.remove(); }
                @Override public void onAnimationCancel(Animator animation) { splash.remove(); }
            });
            exit.start();
        });
    }
}
