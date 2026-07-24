package com.clockmods.pro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.ui.SettingsDialog;
import com.clockmods.pro.chime.HourlyChimeController;
import com.clockmods.pro.chime.RadialChimeView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProMainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE = 3001;
    private static final int REQUEST_NOTIFICATIONS = 3002;
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
    private HourlyChimeController hourlyChimeController;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExperienceBridge.applyThemeFeatures(this);
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pro);

        pager = findViewById(R.id.pro_pager);
        hourlyChimeController = new HourlyChimeController(
            (RadialChimeView) findViewById(R.id.radial_chime),
            new BackgroundRepository(this));
        pager.setAdapter(new ProPagerAdapter(this));
        pager.setOffscreenPageLimit(1);
        navigation = findViewById(R.id.pro_navigation);
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
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (chromeGestureDetector != null) {
            chromeGestureDetector.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
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
            @Override public void onFontSettingsApplied() { refreshClockPage(); }
            @Override public void onDismissed() { updateChromeForPage(); }
        });
        dialog.show();
    }

    private void showChromeTemporarily() {
        navigation.setVisibility(View.VISIBLE);
        showSystemBars();
        chromeHandler.removeCallbacks(hideChromeRunnable);
        if (isClockPage()) {
            chromeHandler.postDelayed(hideChromeRunnable, CHROME_VISIBLE_MILLIS);
        }
    }

    private void hideChrome() {
        chromeHandler.removeCallbacks(hideChromeRunnable);
        if (navigation != null) {
            navigation.setVisibility(isClockPage() ? View.GONE : View.VISIBLE);
        }
        hideSystemBars();
    }

    private void updateChromeForPage() {
        chromeHandler.removeCallbacks(hideChromeRunnable);
        if (navigation == null || pager == null) return;
        navigation.setVisibility(isClockPage() ? View.GONE : View.VISIBLE);
        hideSystemBars();
    }

    private boolean isClockPage() {
        return pager == null || pager.getCurrentItem() == ProPage.CLOCK.ordinal();
    }

    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
}