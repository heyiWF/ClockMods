package com.clockmods.pro;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.clockmods.R;
import com.clockmods.LocaleManager;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.ui.ButtonTextSizer;
import com.clockmods.pro.chime.HourlyChimeController;
import com.clockmods.pro.chime.RadialChimeView;
import com.clockmods.ultimate.AntiBurnController;
import com.clockmods.ultimate.AntiBurnPreferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class ProMainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATIONS = 3002;
    private static final String STATE_SELECTED_PAGE = "selected_page";
    private static final long CHROME_VISIBLE_MILLIS = 3000L;
    private final Handler chromeHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideChromeRunnable = this::hideChrome;
    private static final int[] NAVIGATION_IDS = {
            R.id.navigation_clock, R.id.navigation_calendar, R.id.navigation_pomodoro,
            R.id.navigation_alarm, R.id.navigation_countdown, R.id.navigation_stopwatch
    };
    private ViewPager2 pager;
    private BottomNavigationView navigation;
    private GestureDetector chromeGestureDetector;
    private boolean navigationTouchSequence;
    private RadialChimeView radialChimeView;
    private HourlyChimeController hourlyChimeController;
    private AntiBurnController antiBurnController;
    private int selectedPage = ProPage.CLOCK.ordinal();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        // AppCompat resets the locale to the system default here; re-assert the chosen one.
        if (overrideConfiguration != null) {
            overrideConfiguration.setLocale(LocaleManager.resolveLocale(this));
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExperienceBridge.applyThemeFeatures(this);
        super.onCreate(savedInstanceState);
        applyScreenOrientation();
        enableEdgeToEdge();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pro);

        pager = findViewById(R.id.pro_pager);
        View contentHost = pager;
        View antiBurnOverlay = findViewById(R.id.pro_anti_burn_overlay);
        antiBurnController = new AntiBurnController(contentHost, antiBurnOverlay,
                new AntiBurnPreferences(this));
        radialChimeView = findViewById(R.id.radial_chime);
        hourlyChimeController = new HourlyChimeController(radialChimeView,
            new BackgroundRepository(this));
        pager.setAdapter(createPagerAdapter());
        pager.setOffscreenPageLimit(1);
        navigation = findViewById(R.id.pro_navigation);
        if (savedInstanceState != null) {
            selectedPage = savedInstanceState.getInt(STATE_SELECTED_PAGE,
                ProPage.CLOCK.ordinal());
        }
        pager.setCurrentItem(selectedPage, false);
        ProFontApplier.apply(navigation);
        ButtonTextSizer.applyAllTextToTree(navigation);
        navigation.setSelectedItemId(NAVIGATION_IDS[selectedPage]);
        navigation.setOnItemSelectedListener(item -> {
            for (int position = 0; position < NAVIGATION_IDS.length; position++) {
                if (NAVIGATION_IDS[position] == item.getItemId()) {
                    pager.setCurrentItem(position, true);
                    return true;
                }
            }
            return false;
        });
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                dispatchCalendarDestinationChange(selectedPage, position);
                selectedPage = position;
                navigation.setSelectedItemId(NAVIGATION_IDS[position]);
                updateChromeForPage();
            }
        });
        chromeGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent event) { return true; }
                    @Override public boolean onSingleTapConfirmed(MotionEvent event) {
                        showChromeTemporarily();
                        return false;
                    }
                });
        // The Ultimate experience presents its first-launch setup wizard before asking for
        // notifications. Other hosts can keep the existing prompt timing.
        if (!shouldDeferNotificationPermission()) {
            requestNotificationPermissionIfNeeded();
        }
    }

    /** Creates the adapter for the Ultimate clock and its integrated tool destinations. */
    protected ProPagerAdapter createPagerAdapter() {
        return new ProPagerAdapter(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_SELECTED_PAGE,
                pager == null ? selectedPage : pager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            navigationTouchSequence = radialChimeView != null
                    && radialChimeView.getVisibility() == View.VISIBLE;
            if (!navigationTouchSequence) {
                navigationTouchSequence = navigation != null
                    && navigation.getVisibility() == View.VISIBLE
                    && event.getY() >= navigation.getTop();
            }
            if (!navigationTouchSequence && isCalendarPage()) {
                navigationTouchSequence = isInteractiveAt(pager, event.getRawX(), event.getRawY());
            }
        }
        if (chromeGestureDetector != null && !navigationTouchSequence) {
            chromeGestureDetector.onTouchEvent(event);
        }
        boolean handled = super.dispatchTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            navigationTouchSequence = false;
        }
        return handled;
    }

    private boolean isInteractiveAt(View view, float rawX, float rawY) {
        if (view == null || view.getVisibility() != View.VISIBLE || !contains(view, rawX, rawY)) {
            return false;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = group.getChildCount() - 1; index >= 0; index--) {
                if (isInteractiveAt(group.getChildAt(index), rawX, rawY)) return true;
            }
        }
        return view.isClickable() || view.isLongClickable();
    }

    private static boolean contains(View view, float rawX, float rawY) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return rawX >= location[0] && rawX < location[0] + view.getWidth()
                && rawY >= location[1] && rawY < location[1] + view.getHeight();
    }

    @Override protected void onResume() {
        super.onResume();
        hourlyChimeController.start();
        if (antiBurnController != null) antiBurnController.start();
        updateChromeForPage();
    }

    @Override protected void onPause() {
        hourlyChimeController.stop();
        if (antiBurnController != null) antiBurnController.stop();
        super.onPause();
    }

    public abstract void showSettings();

    protected void showChromeTemporarily() {
        navigation.setVisibility(View.VISIBLE);
        showSystemBars();
        chromeHandler.removeCallbacks(hideChromeRunnable);
        if (isImmersivePage()) {
            chromeHandler.postDelayed(hideChromeRunnable, CHROME_VISIBLE_MILLIS);
        }
    }

    private void hideChrome() {
        chromeHandler.removeCallbacks(hideChromeRunnable);
        if (navigation != null) {
            navigation.setVisibility(isImmersivePage() ? View.GONE : View.VISIBLE);
        }
        hideSystemBars();
    }

    private void updateChromeForPage() {
        chromeHandler.removeCallbacks(hideChromeRunnable);
        if (navigation == null || pager == null) return;
        boolean immersive = isImmersivePage();
        navigation.setVisibility(immersive ? View.GONE : View.VISIBLE);
        applyPagerNavigationSpace(immersive);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), !immersive);
        if (immersive) hideSystemBars();
        else showSystemBars();
    }

    private void applyPagerNavigationSpace(boolean immersive) {
        pager.setPadding(pager.getPaddingLeft(), pager.getPaddingTop(), pager.getPaddingRight(),
                immersive ? 0 : getResources().getDimensionPixelSize(
                        R.dimen.pro_navigation_height));
    }

    private boolean isImmersivePage() {
        if (pager == null) return true;
        int page = pager.getCurrentItem();
        return page == ProPage.CLOCK.ordinal() || page == ProPage.CALENDAR.ordinal();
    }

    private boolean isClockPage() {
        return pager == null || pager.getCurrentItem() == ProPage.CLOCK.ordinal();
    }

    private boolean isCalendarPage() {
        return pager != null && pager.getCurrentItem() == ProPage.CALENDAR.ordinal();
    }

    private void enableEdgeToEdge() {
        // WindowCompat, not Window: the platform setter is deprecated, and the compat one also
        // keeps the inset behaviour identical across the versions this app still runs on.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    private void applyScreenOrientation() {
        int mode = new ClockPreferences(this).getScreenOrientation();
        setRequestedOrientation(ClockPreferences.toActivityInfoOrientation(mode));
    }

    private void hideSystemBars() {
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsets.Type.systemBars());
        }
    }

    private void showSystemBars() {
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.show(WindowInsets.Type.systemBars());
        }
    }

    /** Allows a first-launch host to defer the prompt until its onboarding flow is complete. */
    protected final void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    protected boolean shouldDeferNotificationPermission() {
        return false;
    }

    @Override protected void onDestroy() {
        chromeHandler.removeCallbacks(hideChromeRunnable);
        super.onDestroy();
    }

    protected void refreshClockPage() {
        androidx.fragment.app.Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag("f" + ProPage.CLOCK.ordinal());
        if (fragment instanceof SettingsRefreshable) {
            ((SettingsRefreshable) fragment).refreshSettings();
        }
    }

    protected void refreshSettingsPages() {
        refreshClockPage();
        ProFontApplier.apply(navigation);
        for (int position = 0; position < ProPage.values().length; position++) {
            androidx.fragment.app.Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + position);
            if (fragment == null || fragment.getView() == null) continue;
            ProFontApplier.apply(fragment.getView());
            if (fragment instanceof ProCalendarFragment) {
                ((ProCalendarFragment) fragment).refreshSettings();
            }
        }
        applyScreenOrientation();
        if (antiBurnController != null) antiBurnController.refresh();
    }

    private void dispatchCalendarDestinationChange(int previous, int current) {
        if (previous == current) return;
        androidx.fragment.app.Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag("f" + ProPage.CALENDAR.ordinal());
        if (!(fragment instanceof ProCalendarFragment)) return;
        ProCalendarFragment calendar = (ProCalendarFragment) fragment;
        if (previous == ProPage.CALENDAR.ordinal()) calendar.onCalendarDestinationExited();
        if (current == ProPage.CALENDAR.ordinal()) calendar.onCalendarDestinationEntered(true);
    }
}
