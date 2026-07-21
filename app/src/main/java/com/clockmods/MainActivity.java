package com.clockmods;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import android.widget.TextView;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.ui.ClockView;
import com.clockmods.ui.SettingsDialog;
import com.clockmods.ui.StatusBarView;
import com.clockmods.weather.WeatherController;
import com.clockmods.weather.WeatherModels.WeatherState;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_IMAGE = 1001;
    private static final int REQUEST_LOCATION = 1002;

    private ClockView clockView;
    private StatusBarView statusBarView;
    private BackgroundRepository backgroundRepository;
    private View weatherAttribution;
    private WeatherController weatherController;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private boolean timeReceiverRegistered;
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clockView.invalidate();
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExperienceBridge.applyThemeFeatures(this);
        super.onCreate(savedInstanceState);
        enableEdgeToEdgeCompat();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        clockView = findViewById(R.id.clock_view);
        statusBarView = findViewById(R.id.status_bar_view);
        weatherAttribution = findViewById(R.id.weather_attribution);
        backgroundRepository = new BackgroundRepository(this);
        weatherController = new WeatherController(this, new WeatherController.Listener() {
            @Override public void onWeatherState(WeatherState state) { clockView.setWeatherState(state); }
        });
        clockView.setBackgroundRepository(backgroundRepository);
        statusBarView.setBackgroundRepository(backgroundRepository);
        applyStatusIconVisibility();
        applyWeatherEnabled();
        weatherAttribution.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.qweather.com")));
            }
        });
        GestureDetector settingsGestureDetector = new GestureDetector(this,
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
        clockView.setOnTouchListener((view, event) ->
                settingsGestureDetector.onTouchEvent(event));
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
                    showSettings();
                    return true;
                }
                return super.performAccessibilityAction(host, action, arguments);
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
        startWeatherIfEnabled();
    }

    @Override
    protected void onPause() {
        clockView.stop();
        statusBarView.stop();
        unregisterTimeReceiver();
        weatherController.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        imageExecutor.shutdownNow();
        weatherController.shutdown();
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
        showSystemBars();
        SettingsDialog dialog = new SettingsDialog(this, backgroundRepository, new SettingsDialog.Listener() {
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
                clockView.invalidate();
                applyStatusIconVisibility();
                statusBarView.invalidate();
                applyWeatherEnabled();
                startWeatherIfEnabled();
            }

            @Override
            public void onDismissed() {
                hideSystemBars();
            }
        });
        dialog.show();
    }

    private void applyStatusIconVisibility() {
        statusBarView.setVisibility(
                backgroundRepository.isShowStatusIcons() ? View.VISIBLE : View.GONE);
    }

    private void applyWeatherEnabled() {
        boolean enabled = backgroundRepository.isWeatherEnabled();
        weatherAttribution.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) {
            weatherController.stop();
            clockView.setWeatherState(null);
        }
    }

    private void startWeatherIfEnabled() {
        if (!backgroundRepository.isWeatherEnabled()) return;
        boolean automatic = ClockPreferences.WEATHER_LOCATION_AUTOMATIC.equals(
            backgroundRepository.getWeatherLocationMode());
        if (automatic && Build.VERSION.SDK_INT >= 23
            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            clockView.setWeatherMessage("等待定位权限…");
            return;
        }
        weatherController.start(backgroundRepository.getWeatherIntervalMinutes());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION) return;
        boolean granted = false;
        for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        if (granted) weatherController.start(backgroundRepository.getWeatherIntervalMinutes());
        else clockView.setWeatherMessage("未授予定位权限");
    }

    private void launchImagePicker() {
        startActivityForResult(ExperienceBridge.createImagePickerIntent(), REQUEST_IMAGE);
    }

    private void importImage(final Uri uri) {
        imageExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    if (inputStream != null) {
                        int displayLongSide = Math.max(
                            getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels);
                        backgroundRepository.saveImage(inputStream, displayLongSide);
                        success = true;
                    }
                } catch (IOException | SecurityException ignored) {
                    success = false;
                }
                final boolean imported = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                imported ? R.string.image_saved : R.string.image_error,
                                Toast.LENGTH_SHORT).show();
                        if (imported) {
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
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.systemBars());
            }
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            hideSystemBarsKitKat(decorView);
        } else if (Build.VERSION.SDK_INT >= 16) {
            hideSystemBarsJellyBean(decorView);
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    @SuppressLint("InlinedApi")
    private static void hideSystemBarsKitKat(View decorView) {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @SuppressLint("InlinedApi")
    private static void hideSystemBarsJellyBean(View decorView) {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
}