package com.clockmods.pro;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
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
import com.clockmods.ultimate.UltimateMainActivity;
import com.clockmods.ultimate.clock.UltimateClockPreferences;
import com.clockmods.ultimate.clock.UltimateClockStyles;
import com.clockmods.ultimate.clock.UltimateClockView;
import com.clockmods.ultimate.clock.ClockPalette;
import com.clockmods.ui.ClockView;
import com.clockmods.weather.WeatherController;
import com.clockmods.weather.WeatherModels.WeatherState;
import com.clockmods.ui.StatusBarView;

import java.util.Map;

/** Ultimate clock destination with an SDK renderer host and the original Pro clock host. */
public final class UltimateClockFragment extends Fragment implements SettingsRefreshable {
    /**
     * Fragment.requestPermissions and its result callback are both retired; a launcher registered
     * at construction is the replacement, and it survives the process death the old pair did not.
     */
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult);

    private UltimateClockView ultimateClockView;
    private ClockView proClassicClockView;
    private ViewGroup clockHost;
    private StatusBarView statusBarView;
    private View weatherAttribution;
    private WeatherController weatherController;
    private boolean weatherControllerUsesDetailedPreference;
    private BackgroundRepository statusOverlayRepository;
    private WeatherState lastWeatherState;
    private String lastWeatherMessage;
    private boolean proClassicActive;
    private boolean resumed;

    @Nullable
    @Override
    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ultimate_clock, container, false);
        clockHost = (ViewGroup) root;
        ultimateClockView = root.findViewById(R.id.ultimate_clock_view);
        proClassicClockView = root.findViewById(R.id.pro_classic_clock_view);
        statusBarView = root.findViewById(R.id.status_bar_view);
        weatherAttribution = root.findViewById(R.id.weather_attribution);
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        statusOverlayRepository = new UltimateStatusOverlayRepository(requireContext());
        applyClockStyle(repository);
        statusBarView.setVisibility(repository.isShowStatusIcons() ? View.VISIBLE : View.GONE);
        ProFontApplier.apply(root,
                new UltimateClockPreferences(requireContext()).getStyleId());

        ensureWeatherController();
        applyWeatherEnabled(repository);
        if (weatherAttribution != null) {
            weatherAttribution.setOnClickListener(view -> startActivity(new Intent(
                    Intent.ACTION_VIEW, Uri.parse("https://www.qweather.com"))));
        }

        GestureDetector detector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent event) { return true; }
                    @Override public boolean onDoubleTap(MotionEvent event) {
                        ((UltimateMainActivity) requireActivity()).showSettings();
                        return true;
                    }
                });
        configureClockInteraction(ultimateClockView, detector);
        configureClockInteraction(proClassicClockView, detector);
        return root;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureClockInteraction(View clock, GestureDetector detector) {
        // The Ultimate host observes double taps before its native city scroller receives events.
        clock.setClickable(true);
        if (clock instanceof UltimateClockView) {
            ((UltimateClockView) clock).setGestureDetector(detector);
        } else {
            clock.setOnTouchListener((view, event) -> {
                detector.onTouchEvent(event);
                return false;
            });
        }
        clock.setContentDescription(getString(R.string.open_settings_accessibility));
        clock.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClickable(true);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
                        AccessibilityNodeInfo.ACTION_CLICK,
                        getString(R.string.open_settings_accessibility)));
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                    ((UltimateMainActivity) requireActivity()).showSettings();
                    return true;
                }
                return super.performAccessibilityAction(host, action, arguments);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        updateClockRunningState();
        if (statusBarView != null) statusBarView.start();
        startWeatherIfEnabled();
    }

    @Override
    public void onPause() {
        resumed = false;
        if (ultimateClockView != null) ultimateClockView.stop();
        if (proClassicClockView != null) proClassicClockView.stop();
        if (statusBarView != null) statusBarView.stop();
        if (weatherController != null) weatherController.stop();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (weatherController != null) weatherController.shutdown();
        detachClockView(ultimateClockView);
        detachClockView(proClassicClockView);
        weatherController = null;
        ultimateClockView = null;
        proClassicClockView = null;
        clockHost = null;
        statusBarView = null;
        weatherAttribution = null;
        statusOverlayRepository = null;
        lastWeatherState = null;
        lastWeatherMessage = null;
        super.onDestroyView();
    }

    @Override
    public void refreshSettings() {
        if (ultimateClockView == null || proClassicClockView == null) return;
        ProFontApplier.apply(getView(),
                new UltimateClockPreferences(requireContext()).getStyleId());
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        applyClockStyle(repository);
        ensureWeatherController();
        if (statusOverlayRepository == null) {
            statusOverlayRepository = new UltimateStatusOverlayRepository(requireContext());
        }
        statusBarView.setVisibility(repository.isShowStatusIcons() ? View.VISIBLE : View.GONE);
        statusBarView.invalidate();
        applyWeatherEnabled(repository);
        startWeatherIfEnabled();
    }

    private void applyClockStyle(BackgroundRepository repository) {
        proClassicActive = UltimateClockStyles.STYLE_PRO_CLASSIC.equals(
                new UltimateClockPreferences(requireContext()).getStyleId());
        attachActiveClock(repository);
        if (statusBarView != null) {
            if (statusOverlayRepository == null) {
                statusOverlayRepository = new UltimateStatusOverlayRepository(requireContext());
            }
            statusBarView.setBackgroundRepository(
                    proClassicActive ? repository : statusOverlayRepository);
            statusBarView.setBackgroundResource(
                    proClassicActive ? 0 : R.drawable.ultimate_overlay_pill);
            statusBarView.setPaddingRelative(dp(8), statusBarView.getPaddingTop(),
                    dp(proClassicActive ? 4 : 8), statusBarView.getPaddingBottom());
        }
        if (weatherAttribution != null) {
            // Attribution is deliberately a quiet, transparent label. The status bar keeps its
            // own pill treatment; weather attribution must never introduce a competing surface.
            weatherAttribution.setBackgroundResource(0);
            ViewGroup.MarginLayoutParams attributionParams =
                    (ViewGroup.MarginLayoutParams) weatherAttribution.getLayoutParams();
            attributionParams.height = dp(18);
            attributionParams.bottomMargin = dp(4);
            weatherAttribution.setLayoutParams(attributionParams);
            UltimateClockPreferences appearance = new UltimateClockPreferences(requireContext());
            boolean adaptive = ClockPalette.supports(appearance.getStyleId())
                    && !UltimateClockPreferences.BACKGROUND_MODE_IMAGE.equals(appearance.getBackgroundMode());
            ClockPalette palette = appearance.getPalette(appearance.getStyleId());
            if (UltimateClockPreferences.BACKGROUND_MODE_COLOR.equals(appearance.getBackgroundMode())) {
                palette = palette.withColor(0, repository.getCurrentColor());
            }
            int attributionColor = adaptive ? palette.mutedBackground : 0x66FFFFFF;
            ViewGroup attribution = (ViewGroup) weatherAttribution;
            for (int index = 0; index < attribution.getChildCount(); index++) {
                View child = attribution.getChildAt(index);
                if (child instanceof android.widget.TextView) {
                    ((android.widget.TextView) child).setTextColor(attributionColor);
                } else if (child instanceof com.clockmods.ui.QWeatherLogoView) {
                    child.setAlpha(adaptive ? 1f : .45f);
                    ((com.clockmods.ui.QWeatherLogoView) child).setLogoColor(
                            adaptive ? attributionColor : 0xCCFFFFFF);
                }
            }
        }
        updateBottomOverlayInset(repository.isWeatherEnabled());
        deliverLastWeather();
        updateClockRunningState();
    }

    private void attachActiveClock(BackgroundRepository repository) {
        if (clockHost == null || ultimateClockView == null || proClassicClockView == null) return;
        View active = proClassicActive ? proClassicClockView : ultimateClockView;
        View inactive = proClassicActive ? ultimateClockView : proClassicClockView;
        detachClockView(inactive);
        active.setVisibility(View.VISIBLE);
        if (active == ultimateClockView) {
            ultimateClockView.setBackgroundRepository(repository);
        } else {
            proClassicClockView.setBackgroundRepository(
                    new ProClassicBackgroundRepository(requireContext(), repository));
        }
        if (active.getParent() != clockHost) {
            detachClockView(active);
            clockHost.addView(active, 0);
        }
        active.invalidate();
    }

    private void detachClockView(View clock) {
        if (clock == null) return;
        if (clock == ultimateClockView) {
            ultimateClockView.stop();
        } else if (clock == proClassicClockView) {
            proClassicClockView.stop();
        }
        if (clock.getParent() instanceof ViewGroup) {
            ((ViewGroup) clock.getParent()).removeView(clock);
        }
    }

    private void updateClockRunningState() {
        if (ultimateClockView != null) ultimateClockView.stop();
        if (proClassicClockView != null) proClassicClockView.stop();
        if (!resumed) return;
        if (proClassicActive) {
            if (proClassicClockView != null) proClassicClockView.start();
        } else if (ultimateClockView != null) {
            ultimateClockView.start();
        }
    }

    private void deliverLastWeather() {
        if (lastWeatherState != null) {
            deliverWeatherState(lastWeatherState);
        } else if (lastWeatherMessage != null) {
            deliverWeatherMessage(lastWeatherMessage);
        }
    }

    private void deliverWeatherState(WeatherState state) {
        if (proClassicActive) {
            if (proClassicClockView != null) proClassicClockView.setWeatherState(state);
        } else if (ultimateClockView != null) {
            ultimateClockView.setWeatherState(state);
        }
    }

    private void deliverWeatherMessage(String message) {
        if (proClassicActive) {
            if (proClassicClockView != null) proClassicClockView.setWeatherMessage(message);
        } else if (ultimateClockView != null) {
            ultimateClockView.setWeatherMessage(message);
        }
    }

    private void showWeatherMessage(String message) {
        lastWeatherState = null;
        lastWeatherMessage = message;
        deliverWeatherMessage(message);
    }

    private void ensureWeatherController() {
        boolean usesDetailedPreference = proClassicActive;
        if (weatherController != null
                && weatherControllerUsesDetailedPreference == usesDetailedPreference) {
            return;
        }
        if (weatherController != null) weatherController.shutdown();
        weatherControllerUsesDetailedPreference = usesDetailedPreference;
        weatherController = new WeatherController(requireContext(), state -> {
            lastWeatherState = state;
            lastWeatherMessage = null;
            deliverWeatherState(state);
        }, usesDetailedPreference ? null : Boolean.FALSE);
    }

    private void clearWeather() {
        lastWeatherState = null;
        lastWeatherMessage = null;
        if (ultimateClockView != null) ultimateClockView.setWeatherState(null);
        if (proClassicClockView != null) proClassicClockView.setWeatherState(null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void applyWeatherEnabled(BackgroundRepository repository) {
        boolean enabled = repository.isWeatherEnabled();
        if (weatherAttribution != null) {
            weatherAttribution.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        updateBottomOverlayInset(enabled);
        if (!enabled && weatherController != null) {
            weatherController.stop();
            clearWeather();
        }
    }

    private void updateBottomOverlayInset(boolean weatherEnabled) {
        if (ultimateClockView == null) return;
        ultimateClockView.setBottomOverlayInset(
                !proClassicActive && weatherEnabled ? dp(28) : 0f);
    }

    private void startWeatherIfEnabled() {
        if (weatherController == null || !isAdded()) return;
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        if (!repository.isWeatherEnabled()) return;
        boolean automatic = ClockPreferences.WEATHER_LOCATION_AUTOMATIC.equals(
                repository.getWeatherLocationMode());
        if (automatic
                && requireContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                && requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION});
            showWeatherMessage(getString(R.string.weather_waiting_permission));
            return;
        }
        weatherController.start(repository.getWeatherIntervalMinutes());
    }

    private void onLocationPermissionResult(Map<String, Boolean> grants) {
        if (weatherController == null || !isAdded()) return;
        boolean granted = false;
        for (Boolean result : grants.values()) if (Boolean.TRUE.equals(result)) granted = true;
        BackgroundRepository repository = new BackgroundRepository(requireContext());
        if (!repository.isWeatherEnabled()) {
            weatherController.stop();
            clearWeather();
            return;
        }
        if (granted) {
            weatherController.start(repository.getWeatherIntervalMinutes());
        } else {
            showWeatherMessage(getString(R.string.weather_permission_denied));
        }
    }

    private static final class UltimateStatusOverlayRepository extends BackgroundRepository {
        UltimateStatusOverlayRepository(Context context) {
            super(context);
        }

        @Override public int getTimeColor() {
            return Color.WHITE;
        }
    }

    private static final class ProClassicBackgroundRepository extends BackgroundRepository {
        private final BackgroundRepository delegate;
        private final boolean useThemeBackground;

        ProClassicBackgroundRepository(Context context, BackgroundRepository delegate) {
            super(context);
            this.delegate = delegate;
            useThemeBackground = UltimateClockPreferences.BACKGROUND_MODE_THEME.equals(
                    new UltimateClockPreferences(context).getBackgroundMode());
        }

        @Override public String getBackgroundMode() {
            return useThemeBackground ? ClockPreferences.MODE_COLOR : delegate.getBackgroundMode();
        }

        @Override public int getCurrentColor() {
            return useThemeBackground ? Color.BLACK : delegate.getCurrentColor();
        }
    }
}
