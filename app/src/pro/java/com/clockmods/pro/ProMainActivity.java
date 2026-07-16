package com.clockmods.pro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProMainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE = 3001;
    private static final int REQUEST_NOTIFICATIONS = 3002;
    private ViewPager2 pager;
    private HourlyChimeController hourlyChimeController;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExperienceBridge.applyThemeFeatures(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pro);

        pager = findViewById(R.id.pro_pager);
        hourlyChimeController = new HourlyChimeController(
            (RadialChimeView) findViewById(R.id.radial_chime),
            new BackgroundRepository(this));
        pager.setAdapter(new ProPagerAdapter(this));
        pager.setOffscreenPageLimit(1);
        TabLayout navigation = findViewById(R.id.pro_navigation);
        new TabLayoutMediator(navigation, pager, (tab, position) -> {
            ProPage page = ProPage.values()[position];
            tab.setText(page.titleRes);
            tab.setIcon(page.iconRes);
        }).attach();
        requestNotificationPermissionIfNeeded();
    }

    @Override protected void onResume() {
        super.onResume();
        hourlyChimeController.start();
    }

    @Override protected void onPause() {
        hourlyChimeController.stop();
        super.onPause();
    }

    public void showSettings() {
        BackgroundRepository repository = new BackgroundRepository(this);
        SettingsDialog dialog = new SettingsDialog(this, repository, new SettingsDialog.Listener() {
            @Override public void onColorApplied(int color) { repository.setCurrentColor(color); }
            @Override public void onImageModeApplied() { repository.useImage(); }
            @Override public void onChooseImage() {
                startActivityForResult(ExperienceBridge.createImagePickerIntent(), REQUEST_IMAGE);
            }
            @Override public void onFontSettingsApplied() { refreshClockPage(); }
            @Override public void onDismissed() { }
        });
        dialog.show();
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