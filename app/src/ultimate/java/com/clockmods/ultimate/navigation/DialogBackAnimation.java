package com.clockmods.ultimate.navigation;

import android.os.Build;
import android.view.View;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedDispatcher;

/** Window-local dialog preview. Cancellation never invokes a dialog action. */
public final class DialogBackAnimation {
    private DialogBackAnimation() {}

    public static AutoCloseable install(View view, Runnable dismiss) {
        if (Build.VERSION.SDK_INT < 34) return () -> {};
        return Api34.install(view.getRootView(), dismiss);
    }

    @androidx.annotation.RequiresApi(34)
    private static final class Api34 {
        static AutoCloseable install(View view, Runnable dismiss) {
            final OnBackInvokedDispatcher[] registered = new OnBackInvokedDispatcher[1];
            OnBackAnimationCallback callback = new OnBackAnimationCallback() {
                private void reset() {
                    view.animate().cancel();
                    view.animate().scaleX(1f).scaleY(1f).translationX(0f).setDuration(180).start();
                }
                @Override public void onBackStarted(BackEvent event) { view.animate().cancel(); }
                @Override public void onBackProgressed(BackEvent event) {
                    float progress = Math.max(0f, Math.min(1f, event.getProgress()));
                    view.setScaleX(1f - .08f * progress);
                    view.setScaleY(1f - .08f * progress);
                    view.setTranslationX((event.getSwipeEdge() == BackEvent.EDGE_LEFT ? 1 : -1)
                            * view.getResources().getDisplayMetrics().density * 24f * progress);
                }
                @Override public void onBackCancelled() { reset(); }
                @Override public void onBackInvoked() { dismiss.run(); }
            };
            View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View attached) {
                    if (registered[0] != null) return;
                    registered[0] = attached.findOnBackInvokedDispatcher();
                    if (registered[0] != null) registered[0].registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
                }
                @Override public void onViewDetachedFromWindow(View detached) {
                    if (registered[0] != null) registered[0].unregisterOnBackInvokedCallback(callback);
                    registered[0] = null;
                    view.animate().cancel();
                    view.setScaleX(1f); view.setScaleY(1f); view.setTranslationX(0f);
                }
            };
            view.addOnAttachStateChangeListener(listener);
            if (view.isAttachedToWindow()) listener.onViewAttachedToWindow(view);
            return () -> {
                listener.onViewDetachedFromWindow(view);
                view.removeOnAttachStateChangeListener(listener);
            };
        }
    }
}
