package com.clockmods.pro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import com.clockmods.background.ClockPreferences;
import com.clockmods.ui.ClockView;
import com.clockmods.ui.StatusBarView;
import com.clockmods.weather.WeatherController;
import com.clockmods.weather.WeatherModels.WeatherState;

public final class ProClockFragment extends Fragment {
    private static final int REQUEST_LOCATION = 3101;

    private ClockView clockView;
    private StatusBarView statusBarView;
    private View weatherAttribution;
    private WeatherController weatherController;

    @Nullable
    @Override
    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_clock, container, false);
        clockView = root.findViewById(R.id.clock_view);
        statusBarView = root.findViewById(R.id.status_bar_view);
        weatherAttribution = root.findViewById(R.id.weather_attribution);
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        clockView.setBackgroundRepository(repository);
        statusBarView.setBackgroundRepository(repository);
        statusBarView.setVisibility(repository.isShowStatusIcons() ? View.VISIBLE : View.GONE);

        weatherController = new WeatherController(requireContext(),
                state -> { if (clockView != null) clockView.setWeatherState(state); });
        applyWeatherEnabled(repository);
        weatherAttribution.setOnClickListener(view ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.qweather.com"))));

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
        startWeatherIfEnabled();
    }

    @Override
    public void onPause() {
        clockView.stop();
        statusBarView.stop();
        if (weatherController != null) weatherController.stop();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (weatherController != null) weatherController.shutdown();
        weatherController = null;
        super.onDestroyView();
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
        applyWeatherEnabled(repository);
        startWeatherIfEnabled();
    }

    private void applyWeatherEnabled(BackgroundRepository repository) {
        boolean enabled = repository.isWeatherEnabled();
        if (weatherAttribution != null) {
            weatherAttribution.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        if (!enabled && weatherController != null) {
            weatherController.stop();
            clockView.setWeatherState(null);
        }
    }

    private void startWeatherIfEnabled() {
        if (weatherController == null) return;
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        if (!repository.isWeatherEnabled()) return;
        boolean automatic = ClockPreferences.WEATHER_LOCATION_AUTOMATIC.equals(
                repository.getWeatherLocationMode());
        if (automatic
                && requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                && requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            clockView.setWeatherMessage("等待定位权限…");
            return;
        }
        weatherController.start(repository.getWeatherIntervalMinutes());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION || weatherController == null) return;
        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        }
        if (granted) {
            weatherController.start(new BackgroundRepository(requireContext())
                    .getWeatherIntervalMinutes());
        } else if (clockView != null) {
            clockView.setWeatherMessage("未授予定位权限");
        }
    }
}