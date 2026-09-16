package com.clockmods.pro;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * The month canvas for a calendar style. It drives the page-portion drag: a horizontal gesture
 * drags the page toward the adjacent one and a fling or a long pull commits the turn.
 *
 * <p>For layouts that only want part of the surface to drag — the 周程's date strip, not its detail
 * card — call {@link #setDragRegion(View)}. A touch that begins outside the region is left alone
 * entirely: the layout neither drags nor claims the parent's stream, so an outer {@code ViewPager}
 * gets the gesture and switches pages. Without a region the whole panel drags, which is the
 * behaviour the month-grid styles want.</p>
 */
public final class MonthGestureLayout extends LinearLayout {
    public interface Listener {
        void onMonthDrag(float offset);
        void onMonthDragFinished(int direction);
    }

    private final int touchSlop;
    private final int minimumFlingVelocity;
    private Listener listener;
    private VelocityTracker velocityTracker;
    private float downX;
    private float downY;
    private boolean horizontal;
    private boolean vertical;
    private final MonthGestureRegion gestureRegion = new MonthGestureRegion();

    /** When set, only touches that start inside this view are treated as possible page drags. */
    private View dragRegion;
    private final Rect dragRegionBounds = new Rect();

    public MonthGestureLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
    }

    public void setMonthGestureListener(Listener listener) { this.listener = listener; }

    /** Restricts page drags to touches that begin inside {@code region}; {@code null} removes it. */
    public void setDragRegion(View region) { this.dragRegion = region; }

    private boolean inDragRegion(float x, float y) {
        if (dragRegion == null) return true;
        // getLeft()/getTop() are relative to the immediate parent, while a landscape style can put
        // the drag region several levels below this layout. Convert the descendant's drawing rect
        // into this layout's coordinates before comparing it with the delivered touch.
        dragRegion.getDrawingRect(dragRegionBounds);
        offsetDescendantRectToMyCoords(dragRegion, dragRegionBounds);
        return MonthGestureRegion.contains(dragRegionBounds.left, dragRegionBounds.top,
                dragRegionBounds.right, dragRegionBounds.bottom, x, y);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                recycleTracker();
                downX = event.getX();
                downY = event.getY();
                horizontal = false;
                vertical = false;
                gestureRegion.start(inDragRegion(downX, downY));
                if (!gestureRegion.acceptsGesture()) {
                    // A touch outside the drag region is not ours: don't block the parent's pager,
                    // and keep that decision for the entire stream so a later MOVE cannot steal it.
                    return false;
                }
                track(event);
                getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!gestureRegion.acceptsGesture()) return false;
                track(event);
                lockDirection(event);
                if (vertical) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    recycleTracker();
                    return false;
                }
                return horizontal;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (gestureRegion.acceptsGesture()) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                gestureRegion.finish();
                recycleTracker();
                return false;
            default:
                return false;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!gestureRegion.acceptsGesture()) return false;
        track(event);
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            lockDirection(event);
            if (horizontal && listener != null) listener.onMonthDrag(event.getX() - downX);
            return horizontal;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            float deltaX = event.getX() - downX;
            float velocity = 0f;
            if (velocityTracker != null) {
                velocityTracker.computeCurrentVelocity(1000);
                velocity = velocityTracker.getXVelocity();
            }
            int direction = 0;
            if (event.getActionMasked() == MotionEvent.ACTION_UP && horizontal
                    && (Math.abs(deltaX) > getWidth() * 0.25f
                    || Math.abs(velocity) > minimumFlingVelocity)) {
                direction = deltaX < 0f ? 1 : -1;
            }
            if (listener != null) listener.onMonthDragFinished(direction);
            getParent().requestDisallowInterceptTouchEvent(false);
            gestureRegion.finish();
            recycleTracker();
            return horizontal;
        }
        return true;
    }

    private void lockDirection(MotionEvent event) {
        if (horizontal || vertical) return;
        float deltaX = Math.abs(event.getX() - downX);
        float deltaY = Math.abs(event.getY() - downY);
        if (deltaX <= touchSlop && deltaY <= touchSlop) return;
        horizontal = deltaX > deltaY * 1.2f;
        vertical = !horizontal;
    }

    private void track(MotionEvent event) {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
    }

    private void recycleTracker() {
        if (velocityTracker != null) velocityTracker.recycle();
        velocityTracker = null;
    }
}
