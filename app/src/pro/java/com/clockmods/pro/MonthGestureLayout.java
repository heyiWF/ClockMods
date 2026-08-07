package com.clockmods.pro;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

public final class MonthGestureLayout extends LinearLayout {
    interface Listener {
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

    public MonthGestureLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
    }

    void setMonthGestureListener(Listener listener) { this.listener = listener; }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        track(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                horizontal = false;
                vertical = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            case MotionEvent.ACTION_MOVE:
                lockDirection(event);
                if (vertical) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    recycleTracker();
                    return false;
                }
                return horizontal;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                recycleTracker();
                return false;
            default:
                return false;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
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