package com.clockmods.pro;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.clockmods.sdk.clock.ClockOverlayBounds;
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
     * The status capsule's own geometry, mirroring {@code fragment_ultimate_clock}: a 28dp row
     * parked one {@code margin} in from whichever edge the style asks for. A style that has room
     * for it in its composition returns a box; the host only falls back to the corner when a style
     * does not care.
     */
    private static final int STATUS_CAPSULE_MARGIN_DP = 20;
    private static final int STATUS_CAPSULE_MARGIN_TOP_DP = 16;
    private static final int STATUS_CAPSULE_HEIGHT_DP = 28;
    /**
     * The capsule is filled with the style's surface at full opacity. A sheer fill picked up
     * whatever sat behind it, so a corner straddling two surfaces rendered as two different shades;
     * an opaque chip reads as one deliberate surface instead.
     */
    private static final int STATUS_CAPSULE_STROKE_ALPHA = 0x3D;

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
    /** True while the clock view is frosting the plate behind the capsule instead of the host. */
    private boolean statusOverlayBlurred;

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
        // The style places the capsule from the host's own size, which is only known once it has
        // been laid out; this first call covers the case where the view tree is already measured.
        updateStatusOverlayPlacement();
        clockHost.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> updateStatusOverlayPlacement());
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
        ultimateClockView.setStatusOverlayPlateListener(this::onStatusOverlayPlate);
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
        updateStatusOverlayPlacement();
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
            applyStatusCapsule();
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

    /**
     * The clock view draws the capsule's frosted plate itself whenever the face renders with
     * Gaussian blur, so this drops the flat capsule for the plate and takes the foreground the
     * plate needs. Called again with {@code false} when the plate goes away — blur switched off, a
     * colour background, or the image not loaded yet — and the flat capsule comes back.
     */
    private void onStatusOverlayPlate(boolean blurred, int contentColor) {
        if (statusBarView == null) return;
        boolean previous = statusOverlayBlurred;
        statusOverlayBlurred = blurred;
        if (!blurred) {
            if (!previous) return;
            applyStatusCapsule();
            return;
        }
        statusBarView.setBackground(null);
        statusBarView.setTintOverride(contentColor);
        // The plate carries its own hairline and contrast, exactly like the face's cards.
        statusBarView.setContentShadowEnabled(false);
    }

    /**
     * Gives the status capsule the active style's own surface instead of one hard-coded dark pill
     * for every theme — cream paper takes an ink-on-paper label, an instrument panel takes charcoal,
     * a light glass face takes an almost-white chip with a hairline. The five palette styles follow
     * the colours the user picked for them, so the capsule matches their cards exactly.
     */
    private void applyStatusCapsule() {
        if (statusBarView == null) return;
        if (proClassicActive) {
            // Pro Classic owns its own face: the row sits straight on the user's background and the
            // icons keep the clock's own colour, which is what that style has always done.
            statusBarView.setBackground(null);
            statusBarView.setTintOverride(0);
            statusBarView.setContentShadowEnabled(true);
            return;
        }
        if (statusOverlayBlurred) {
            // The face is frosting a plate of its own behind this row, so anything the host paints
            // would sit on top of it. The plate's foreground arrives with the same report.
            statusBarView.setBackground(null);
            return;
        }
        UltimateClockPreferences appearance = new UltimateClockPreferences(requireContext());
        String styleId = appearance.getStyleId();
        int surface;
        int content;
        if (ClockPalette.supports(styleId)) {
            ClockPalette palette = appearance.getPalette(styleId);
            surface = palette.panel;
            content = palette.onPanel;
        } else {
            surface = UltimateClockStyles.sharedRegistry()
                    .resolveForApi(styleId, Build.VERSION.SDK_INT).getThemeTokens()
                    .getSurfaceColor();
            content = ClockPalette.foreground(surface);
        }
        GradientDrawable capsule = new GradientDrawable();
        capsule.setShape(GradientDrawable.RECTANGLE);
        capsule.setCornerRadius(dp(999));
        capsule.setColor(surface);
        capsule.setStroke(Math.max(1, dp(1)), withAlpha(content, STATUS_CAPSULE_STROKE_ALPHA));
        statusBarView.setBackground(capsule);
        statusBarView.setTintOverride(content);
        // With a capsule of its own the row has all the contrast it needs; the drop shadow only
        // smudged dark glyphs on light capsules.
        statusBarView.setContentShadowEnabled(false);
    }

    /**
     * Parks the status capsule where the active style can live with it, and tells the style where
     * that turned out to be.
     *
     * <p>The style owns the decision: 双块 and 混合 hand back a box inside the panel that owns the
     * top-right corner, 轨道 hands back the top-left one, and everything else keeps the corner the
     * host has always used. The style then uses {@link ClockOverlayBounds} to keep its own metadata
     * out of the box, which is what turns "put the capsule above the weather" into something both
     * sides agree on.</p>
     */
    private void updateStatusOverlayPlacement() {
        if (statusBarView == null || ultimateClockView == null) return;
        if (proClassicActive || statusBarView.getVisibility() != View.VISIBLE) {
            ultimateClockView.setStatusOverlay(null);
            return;
        }
        if (clockHost == null || clockHost.getWidth() <= 0 || clockHost.getHeight() <= 0) return;
        float hostWidth = clockHost.getWidth();
        float hostHeight = clockHost.getHeight();
        int capsuleHeight = dp(STATUS_CAPSULE_HEIGHT_DP);
        // The capsule hugs its content, so measure it the way it measures itself. The box's right
        // edge is what the corner styles care about, which is also what keeps the capsule from
        // drifting sideways as the battery reading gains a digit.
        statusBarView.measure(
                View.MeasureSpec.makeMeasureSpec(clockHost.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(capsuleHeight, View.MeasureSpec.EXACTLY));
        float[] box = UltimateClockStyles.statusCapsuleBounds(
                new UltimateClockPreferences(requireContext()).getStyleId(),
                hostWidth, hostHeight, getResources().getDisplayMetrics().density,
                dp(STATUS_CAPSULE_MARGIN_DP), dp(STATUS_CAPSULE_MARGIN_TOP_DP),
                statusBarView.getMeasuredWidth(), capsuleHeight);
        anchorCapsule(box, hostWidth);
        ultimateClockView.setStatusOverlay(new ClockOverlayBounds(box[0], box[1], box[2], box[3]));
    }

    /**
     * Moves the capsule to {@code box} by translation rather than by layout margins.
     *
     * <p>Margins are re-resolved against the relative {@code layout_marginEnd} the layout declares
     * every time the view tree is laid out, which silently pulled the capsule back to the screen
     * corner: the top margin stuck because it is absolute, and the end margin did not. A
     * translation is a draw-time offset that no layout pass touches, and the capsule is a
     * fixed-size overlay, so nothing else depended on the slot it occupies.</p>
     */
    private void anchorCapsule(float[] box, float hostWidth) {
        if (statusBarView == null || statusBarView.getWidth() <= 0) return;
        float capsuleWidth = statusBarView.getWidth();
        // Anchor whichever of the box's two side edges the style pinned, so a wider battery
        // reading grows the capsule away from that edge instead of shifting the whole row.
        boolean anchorStart = box[0] <= hostWidth - box[2];
        float desiredLeft = anchorStart ? box[0] : box[2] - capsuleWidth;
        statusBarView.setTranslationX(desiredLeft - statusBarView.getLeft());
        statusBarView.setTranslationY(box[1] - statusBarView.getTop());
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
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
