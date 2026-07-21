package com.clockmods.pro;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.ui.ClockView;
import com.clockmods.ui.StatusBarView;

public final class ProClockFragment extends Fragment {
    private ClockView clockView;
    private StatusBarView statusBarView;

    @Nullable
    @Override
    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_clock, container, false);
        clockView = root.findViewById(R.id.clock_view);
        statusBarView = root.findViewById(R.id.status_bar_view);
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        clockView.setBackgroundRepository(repository);
        statusBarView.setBackgroundRepository(repository);
        statusBarView.setVisibility(repository.isShowStatusIcons() ? View.VISIBLE : View.GONE);

        GestureDetector detector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent event) { return true; }
                    @Override public boolean onDoubleTap(MotionEvent event) {
                        ((ProMainActivity) requireActivity()).showSettings();
                        return true;
                    }
                });
        clockView.setOnTouchListener((view, event) -> detector.onTouchEvent(event));
        clockView.setContentDescription(getString(R.string.open_settings_accessibility));
        clockView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClickable(true);
                info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                    ((ProMainActivity) requireActivity()).showSettings();
                    return true;
                }
                return super.performAccessibilityAction(host, action, arguments);
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        clockView.start();
        statusBarView.start();
    }

    @Override
    public void onPause() {
        clockView.stop();
        statusBarView.stop();
        super.onPause();
    }

    void refreshSettings() {
        if (clockView == null) return;
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        clockView.setBackgroundRepository(repository);
        clockView.requestBackgroundReload();
        clockView.invalidate();
        statusBarView.setBackgroundRepository(repository);
        statusBarView.setVisibility(repository.isShowStatusIcons() ? View.VISIBLE : View.GONE);
        statusBarView.invalidate();
    }
}