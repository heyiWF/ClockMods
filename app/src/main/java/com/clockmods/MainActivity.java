package com.clockmods;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ApiCompat;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.ui.ClockView;
import com.clockmods.ui.SettingsDialog;
import com.clockmods.ui.StatusBarView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_IMAGE = 1001;

    private SettingsDialog settingsDialog;
    private ClockView clockView;
    private StatusBarView statusBarView;
    private BackgroundRepository backgroundRepository;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private boolean timeReceiverRegistered;
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // The clock view caches the resolved zone, so a system zone change has to
            // drop that cache explicitly before the repaint.
            if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                clockView.invalidateTimeZone();
            }
            clockView.invalidate();
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyScreenOrientation();
        enableEdgeToEdgeCompat();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        clockView = findViewById(R.id.clock_view);
        statusBarView = findViewById(R.id.status_bar_view);
        backgroundRepository = new BackgroundRepository(this);
        clockView.setBackgroundRepository(backgroundRepository);
        statusBarView.setBackgroundRepository(backgroundRepository);
        applyStatusIconVisibility();

        GestureDetector settingsGestureDetector = ApiCompat.newGestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent event) {
                showSettings();
                return true;
            }
        });
        // Double tap needs API 3; on Android 1.0 the single-tap listener below is the only entry.
        if (ApiCompat.SDK_INT >= 3) {
            clockView.setOnTouchListener((view, event) ->
                    settingsGestureDetector.onTouchEvent(event));
        }
        // On API 1-13 the accessibility delegate is unavailable; a plain click
        // listener keeps the "tap the clock to open settings" entry point alive.
        clockView.setOnClickListener(view -> showSettings());
        ApiCompat.setContentDescriptionCompat(
                clockView, getString(R.string.open_settings_accessibility));
        // The delegate lives in AccessibilityCompat so Android 1.0 never links API 14 types.
        ApiCompat.installAccessibilityDelegate(clockView, new Runnable() {
            @Override
            public void run() {
                showSettings();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        clockView.start();
        statusBarView.start();
        registerTimeReceiver();
    }

    @Override
    protected void onPause() {
        clockView.stop();
        statusBarView.stop();
        unregisterTimeReceiver();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (settingsDialog != null) {
            settingsDialog.setOnDismissListener(null);
            settingsDialog.dismiss();
            settingsDialog = null;
        }
        imageExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            hideSystemBars();
            return;
        }
        importImage(data.getData());
    }

    private void showSettings() {
        if (settingsDialog != null && settingsDialog.isShowing()) return;
        showSystemBars();
        settingsDialog = new SettingsDialog(this, backgroundRepository, new SettingsDialog.Listener() {
            @Override
            public void onColorApplied(int color) {
                backgroundRepository.setCurrentColor(color);
                clockView.requestBackgroundReload();
            }

            @Override
            public void onImageModeApplied() {
                backgroundRepository.useImage();
                clockView.requestBackgroundReload();
            }

            @Override
            public void onChooseImage() {
                launchImagePicker();
            }

            @Override
            public void onFontSettingsApplied() {
                applyScreenOrientation();
                clockView.invalidate();
                applyStatusIconVisibility();
                statusBarView.invalidate();
            }

            @Override
            public void onDismissed() {
                settingsDialog = null;
                hideSystemBars();
            }

            @Override
            public void onLanguageChanged() {
                ApiCompat.recreate(MainActivity.this);
            }
        });
        settingsDialog.show();
    }

    private void applyScreenOrientation() {
        int mode = new ClockPreferences(this).getScreenOrientation();
        setRequestedOrientation(ClockPreferences.toActivityInfoOrientation(mode));
    }

    private void applyStatusIconVisibility() {
        statusBarView.setVisibility(
                backgroundRepository.isShowStatusIcons() ? View.VISIBLE : View.GONE);
    }

    private void launchImagePicker() {
        startActivityForResult(ExperienceBridge.createImagePickerIntent(), REQUEST_IMAGE);
    }

    private void importImage(final Uri uri) {
        imageExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                InputStream inputStream = null;
                try {
                    inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        int displayLongSide = Math.max(
                            getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels);
                        backgroundRepository.saveImage(inputStream, displayLongSide);
                        success = true;
                    }
                } catch (IOException | SecurityException ignored) {
                    success = false;
                } finally {
                    ApiCompat.closeQuietly(inputStream);
                }
                final boolean imported = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || ApiCompat.isDestroyed(MainActivity.this)) return;
                        Toast.makeText(MainActivity.this,
                                imported ? R.string.image_saved : R.string.image_error,
                                Toast.LENGTH_SHORT).show();
                        if (imported) {
                            if (settingsDialog != null) settingsDialog.onImageImported();
                            clockView.requestBackgroundReload();
                        }
                        hideSystemBars();
                    }
                });
            }
        });
    }

    private void registerTimeReceiver() {
        if (timeReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        registerReceiver(timeReceiver, filter);
        timeReceiverRegistered = true;
    }

    private void unregisterTimeReceiver() {
        if (timeReceiverRegistered) {
            unregisterReceiver(timeReceiver);
            timeReceiverRegistered = false;
        }
    }

    private void enableEdgeToEdgeCompat() {
        if (ApiCompat.SDK_INT >= 21 && ApiCompat.hasSystemUiVisibility()) {
            ApiCompat.setSystemUiVisibility(getWindow().getDecorView(),
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    private void hideSystemBars() {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            showSystemBars();
            return;
        }
        if (ApiCompat.SDK_INT >= 30 && hideSystemBarsApi30()) {
            return;
        }
        if (ApiCompat.SDK_INT >= 19) {
            hideSystemBarsKitKat(getWindow().getDecorView());
        } else if (ApiCompat.SDK_INT >= 16) {
            hideSystemBarsJellyBean(getWindow().getDecorView());
        } else {
            // API 1-15: only FLAG_FULLSCREEN is available; never call
            // View.setSystemUiVisibility (introduced in API 14).
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * Reflective {@code WindowInsetsController} access keeps the API 30 class out of
     * the verifier's field/method resolution on older devices.
     */
    @SuppressWarnings("unchecked")
    private boolean hideSystemBarsApi30() {
        try {
            Method getInsetsController = WindowManager.class.getMethod("getInsetsController");
            Object controller = getInsetsController.invoke(getWindow());
            if (controller == null) {
                return false;
            }
            Class<?> controllerClass = controller.getClass();
            Method setBehavior = controllerClass.getMethod(
                    "setSystemBarsBehavior", int.class);
            Method hide = controllerClass.getMethod("hide", int.class);
            Class<?> windowInsetsType = Class.forName("android.view.WindowInsets$Type");
            int systemBars = ((Number) windowInsetsType.getMethod("systemBars")
                    .invoke(null)).intValue();
            int behaviorSwipe = ((Number) controllerClass
                    .getField("BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE").get(null)).intValue();
            setBehavior.invoke(controller, behaviorSwipe);
            hide.invoke(controller, systemBars);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressLint("InlinedApi")
    private static void hideSystemBarsKitKat(View decorView) {
        if (!ApiCompat.hasSystemUiVisibility()) return;
        ApiCompat.setSystemUiVisibility(decorView,
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @SuppressLint("InlinedApi")
    private static void hideSystemBarsJellyBean(View decorView) {
        if (!ApiCompat.hasSystemUiVisibility()) return;
        ApiCompat.setSystemUiVisibility(decorView,
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemBars() {
        if (ApiCompat.SDK_INT >= 30 && showSystemBarsApi30()) {
            return;
        }
        if (ApiCompat.hasSystemUiVisibility()) {
            ApiCompat.setSystemUiVisibility(getWindow().getDecorView(),
                    View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    /** Mirrors {@link #hideSystemBarsApi30()} for the restore path. */
    @SuppressWarnings("unchecked")
    private boolean showSystemBarsApi30() {
        try {
            Object controller = WindowManager.class.getMethod("getInsetsController")
                    .invoke(getWindow());
            if (controller == null) {
                return false;
            }
            Class<?> windowInsetsType = Class.forName("android.view.WindowInsets$Type");
            int systemBars = ((Number) windowInsetsType.getMethod("systemBars")
                    .invoke(null)).intValue();
            controller.getClass().getMethod("show", int.class).invoke(controller, systemBars);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
