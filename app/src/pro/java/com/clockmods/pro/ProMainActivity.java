package com.clockmods.pro;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import android.widget.Toast;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.clockmods.R;
import com.clockmods.LocaleManager;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.ui.ButtonTextSizer;
import com.clockmods.ui.SettingsDialog;
import com.clockmods.pro.chime.HourlyChimeController;
import com.clockmods.pro.chime.RadialChimeView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProMainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE = 3001;
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
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
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
        getWindow().setBackgroundDrawable(new ColorDrawable(surfaceColor()));
        applyScreenOrientation();
        enableEdgeToEdge();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pro);

        pager = findViewById(R.id.pro_pager);
        radialChimeView = findViewById(R.id.radial_chime);
        hourlyChimeController = new HourlyChimeController(radialChimeView,
            new BackgroundRepository(this));
        pager.setAdapter(new ProPagerAdapter(this));
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
        requestNotificationPermissionIfNeeded();
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
        updateChromeForPage();
    }

    @Override protected void onPause() {
        hourlyChimeController.stop();
        super.onPause();
    }

    public void showSettings() {
        navigation.setVisibility(View.VISIBLE);
        showSystemBars();
        chromeHandler.removeCallbacks(hideChromeRunnable);
        BackgroundRepository repository = new BackgroundRepository(this);
        SettingsDialog dialog = new SettingsDialog(this, repository, new SettingsDialog.Listener() {
            @Override public void onColorApplied(int color) { repository.setCurrentColor(color); }
            @Override public void onImageModeApplied() { repository.useImage(); }
            @Override public void onChooseImage() {
                startActivityForResult(ExperienceBridge.createImagePickerIntent(), REQUEST_IMAGE);
            }
            @Override public void onFontSettingsApplied() { refreshSettingsPages(); }
            @Override public void onDismissed() { updateChromeForPage(); }
            @Override public void onLanguageChanged() { recreate(); }
        });
        dialog.show();
        ProFontApplier.apply(dialog.getWindow().getDecorView());
    }

    private void showChromeTemporarily() {
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
        getWindow().setDecorFitsSystemWindows(!immersive);
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(
                    !immersive && MaterialColors.isColorLight(surfaceColor()) ? lightBars : 0,
                    lightBars);
        }
        if (immersive) hideSystemBars();
        else showSystemBars();
    }

    private int surfaceColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface,
                "ProMainActivity");
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
        getWindow().setDecorFitsSystemWindows(false);
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

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE || resultCode != RESULT_OK || data == null
                || data.getData() == null) return;
        importImage(data.getData());
    }

    private void importImage(Uri uri) {
        imageExecutor.execute(() -> {
            boolean success = false;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input != null) {
                    int longSide = Math.max(getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels);
                    new BackgroundRepository(this).saveImage(input, longSide);
                    success = true;
                }
            } catch (IOException | SecurityException ignored) {
                success = false;
            }
            boolean imported = success;
            runOnUiThread(() -> {
                Toast.makeText(this, imported ? R.string.image_saved : R.string.image_error,
                        Toast.LENGTH_SHORT).show();
                if (imported) refreshClockPage();
            });
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    @Override protected void onDestroy() {
        chromeHandler.removeCallbacks(hideChromeRunnable);
        imageExecutor.shutdownNow();
        super.onDestroy();
    }

    private void refreshClockPage() {
        androidx.fragment.app.Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag("f" + ProPage.CLOCK.ordinal());
        if (fragment instanceof ProClockFragment) {
            ((ProClockFragment) fragment).refreshSettings();
        }
    }

    private void refreshSettingsPages() {
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
