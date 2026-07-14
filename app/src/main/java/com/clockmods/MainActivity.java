package com.clockmods;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.ui.ClockView;
import com.clockmods.ui.SettingsDialog;
import com.clockmods.ui.StatusBarView;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_IMAGE = 1001;

    private ClockView clockView;
    private StatusBarView statusBarView;
    private BackgroundRepository backgroundRepository;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private boolean timeReceiverRegistered;
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clockView.invalidate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExperienceBridge.applyThemeFeatures(this);
        super.onCreate(savedInstanceState);
        enableEdgeToEdgeCompat();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        clockView = findViewById(R.id.clock_view);
        statusBarView = findViewById(R.id.status_bar_view);
        backgroundRepository = new BackgroundRepository(this);
        clockView.setBackgroundRepository(backgroundRepository);
        statusBarView.setBackgroundRepository(backgroundRepository);
        applyStatusIconVisibility();
        clockView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                        backgroundRepository.saveImage(inputStream);
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