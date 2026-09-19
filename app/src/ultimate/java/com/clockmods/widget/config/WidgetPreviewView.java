package com.clockmods.widget.config;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import com.clockmods.widget.model.WidgetConfig;
import com.clockmods.widget.render.WidgetRemoteViewsFactory;
import com.clockmods.widget.render.WidgetSizeClassResolver;
import com.clockmods.widget.update.WidgetUpdateCoordinator;

/**
 * Renders the draft through the real {@code WidgetRemoteViewsFactory}, so what the user previews
 * is byte-for-byte the layout the Launcher will inflate. Drafts never enter the preference store.
 *
 * <p>The size class is resolved from this view's own box rather than from the screen, which keeps
 * the preview in the same size class — and therefore the same visible module set — as the
 * recommended cell footprint for the widget kind.
 */
public final class WidgetPreviewView extends FrameLayout {
    private int generation;

    public WidgetPreviewView(Context context) {
        super(context);
    }

    public void show(WidgetConfig config) {
        final int current = ++generation;
        final float density = getResources().getDisplayMetrics().density;
        final float width = measuredPx(true) / density;
        final float height = measuredPx(false) / density;
        // RemoteViews refuses to invoke framework methods on AppCompat subclasses
        // ("view: androidx.appcompat.widget.AppCompatImageView can't use method with RemoteViews").
        // An Activity inflater always carries AppCompat's view factory, so the preview must be
        // inflated from the application context exactly like AppWidgetHostView does.
        final Context inflationContext = getContext().getApplicationContext();
        WidgetUpdateCoordinator.execute(() -> {
            RemoteViews views = WidgetRemoteViewsFactory.create(
                    getContext(), config, WidgetSizeClassResolver.resolve(width, height));
            post(() -> {
                if (current != generation) return;
                removeAllViews();
                addView(views.apply(inflationContext, this),
                        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                disableClicks(this);
            });
        });
    }

    /** Falls back to the requested dp box so the very first preview already uses the right size. */
    private int measuredPx(boolean horizontal) {
        int size = horizontal ? getWidth() : getHeight();
        if (size > 0) return size;
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null) {
            size = horizontal ? params.width : params.height;
            if (size > 0) return size;
        }
        return Math.round((horizontal ? 340 : 156) * getResources().getDisplayMetrics().density);
    }

    private static void disableClicks(View view) {
        view.setOnClickListener(null);
        view.setClickable(false);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) disableClicks(group.getChildAt(i));
        }
    }

    @Override protected void onDetachedFromWindow() {
        generation++;
        super.onDetachedFromWindow();
    }
}
