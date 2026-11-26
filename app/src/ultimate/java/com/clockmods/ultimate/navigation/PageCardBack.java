package com.clockmods.ultimate.navigation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Telegram-style Back for a page stacked over the window behind it. During the gesture the page
 * only shrinks toward the edge opposite the gesture; once the gesture commits it slides away
 * and its Activity is finished without a second transition. Callers must keep that Activity
 * see-through outside the page, or the shrink reveals an empty window.
 */
public final class PageCardBack extends OnBackPressedCallback {
    /** Virtual travel, over which a Back gesture only shrinks the page it started on. */
    private static final float GESTURE_SHIFT_DP = 56f;
    /** Distance from the edge opposite the gesture at which the shrinking page stays anchored. */
    private static final float CARD_ANCHOR_DP = 82f;
    private static final float CARD_CORNER_DP = 32f;
    private static final float CARD_SHRINK = 0.10f;
    private static final float SCRIM = 0.47f;
    /** How far the page travels while it fades in. */
    private static final float ENTER_SHIFT = 0.10f;
    private static final long ENTER_DURATION = 300L;
    /** Gestures barely past the edge must not prepare a preview. */
    private static final float LAZY_START = 0.015f;
    private static final Interpolator STANDARD_DECELERATE = new PathInterpolator(0f, 0f, 0f, 1f);
    private static final Interpolator EASE_OUT_QUINT = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
    /** Material's emphasised decelerate: a quick start into a long, soft settle. */
    private static final Interpolator EMPHASISED_DECELERATE =
            new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);

    private final Activity activity;
    private final View page;
    private final View scrim;
    private final float density;
    private final ViewOutlineProvider outline = new ViewOutlineProvider() {
        @Override public void getOutline(View view, Outline shape) {
            shape.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                    CARD_CORNER_DP * density * shrink);
        }
    };
    private float offset;
    private float shrink;
    private float pivotY;
    private boolean fromLeft = true;
    private boolean finished;
    private ValueAnimator animator;

    private PageCardBack(AppCompatActivity activity, View page, View scrim) {
        super(true);
        this.activity = activity;
        this.page = page;
        this.scrim = scrim;
        density = activity.getResources().getDisplayMetrics().density;
        page.setClipToOutline(true);
        page.setOutlineProvider(outline);
        activity.getOnBackPressedDispatcher().addCallback(activity, this);
    }

    /** Puts the content view on top of a scrim, which is what dims the page beneath. */
    public static PageCardBack install(AppCompatActivity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() != 1) return null;
        View page = content.getChildAt(0);
        ViewGroup.LayoutParams params = page.getLayoutParams();
        content.removeView(page);
        FrameLayout host = new FrameLayout(activity);
        View scrim = new View(activity);
        scrim.setBackground(new ColorDrawable(Color.BLACK));
        scrim.setVisibility(View.GONE);
        scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        host.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        host.addView(page, new FrameLayout.LayoutParams(-1, -1));
        content.addView(host, params);
        return new PageCardBack(activity, page, scrim);
    }

    /** Restores the page, for callers that must take the callback away mid-gesture. */
    public void reset() {
        stop();
        page.animate().cancel();
        page.setAlpha(1f);
        apply(0f);
        scrim.setVisibility(View.GONE);
        scrim.setAlpha(0f);
    }

    /**
     * Slides and fades the page in. The window itself is opened without an animation, so the
     * motion is the same on devices whose vendor build drops platform activity animations.
     */
    public void playEnterAnimation() {
        page.post(() -> {
            page.animate().cancel();
            int width = page.getWidth();
            if (width <= 0) {
                page.setAlpha(1f);
                page.setTranslationX(0f);
                return;
            }
            page.setAlpha(0f);
            page.setTranslationX(width * ENTER_SHIFT);
            page.animate().alpha(1f).translationX(0f)
                    .setDuration(ENTER_DURATION)
                    .setInterpolator(EMPHASISED_DECELERATE)
                    .start();
        });
    }

    @Override public void handleOnBackStarted(BackEventCompat event) {
        if (finished) return;
        // A gesture takes over the page's position; stop any entry animation still running.
        page.animate().cancel();
        page.setAlpha(1f);
        stop();
        fromLeft = event.getSwipeEdge() == BackEventCompat.EDGE_LEFT;
        int[] window = new int[2];
        int[] pageAt = new int[2];
        activity.getWindow().getDecorView().getLocationOnScreen(window);
        page.getLocationInWindow(pageAt);
        pivotY = Math.max(0f, Math.min(page.getHeight(),
                event.getTouchY() - window[1] - pageAt[1]));
        scrim.setVisibility(View.VISIBLE);
    }

    @Override public void handleOnBackProgressed(BackEventCompat event) {
        if (finished || scrim.getVisibility() != View.VISIBLE) return;
        float progress = Math.max(0f, Math.min(1f,
                (event.getProgress() - LAZY_START) / (1f - LAZY_START)));
        apply(GESTURE_SHIFT_DP * density * STANDARD_DECELERATE.getInterpolation(progress));
    }

    @Override public void handleOnBackCancelled() {
        if (finished || scrim.getVisibility() != View.VISIBLE) return;
        slide(0f, 320L, this::reset);
    }

    @Override public void handleOnBackPressed() {
        if (finished) return;
        // Without a gesture preview the Activity keeps its ordinary exit transition.
        if (scrim.getVisibility() != View.VISIBLE || page.getWidth() == 0) {
            activity.finishAfterTransition();
            return;
        }
        finished = true;
        slide(page.getWidth() + GESTURE_SHIFT_DP * density, 380L, () -> {
            if (Build.VERSION.SDK_INT >= 34) {
                Api34.closeWithoutTransition(activity);
            } else {
                activity.overridePendingTransition(0, 0);
            }
            activity.finish();
        });
    }

    /** [offset] is virtual travel in px: up to the gesture shift it only shrinks the page. */
    private void apply(float offset) {
        this.offset = offset;
        float shift = GESTURE_SHIFT_DP * density;
        // Corners grow with the shrink, so a page at rest keeps its square corners.
        shrink = Math.min(1f, offset / shift);
        float anchor = CARD_ANCHOR_DP * density;
        float scale = 1f - CARD_SHRINK * shrink;
        page.setPivotX(fromLeft ? page.getWidth() - anchor : anchor);
        page.setPivotY(pivotY);
        page.setScaleX(scale);
        page.setScaleY(scale);
        page.setTranslationX(Math.max(0f, offset - shift));
        page.invalidateOutline();
        scrim.setAlpha(SCRIM * Math.max(0f, Math.min(0.8f,
                (page.getWidth() - offset) / page.getWidth())));
    }

    private void slide(float target, long duration, Runnable onEnd) {
        animator = ValueAnimator.ofFloat(offset, target);
        animator.setDuration(duration);
        animator.setInterpolator(EASE_OUT_QUINT);
        animator.addUpdateListener(value -> apply((Float) value.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) { if (!cancelled) onEnd.run(); }
        });
        animator.start();
    }

    private void stop() {
        if (animator == null) return;
        ValueAnimator previous = animator;
        animator = null;
        previous.cancel();
    }

    @RequiresApi(34)
    private static final class Api34 {
        static void closeWithoutTransition(Activity activity) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0);
        }
    }
}
