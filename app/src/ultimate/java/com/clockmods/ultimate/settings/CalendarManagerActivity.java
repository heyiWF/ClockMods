package com.clockmods.ultimate.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.calendar.imports.UserCalendarStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/** Material 3 calendar library with source-file cards and file-level actions. */
public final class CalendarManagerActivity extends AppCompatActivity {
    private LinearLayout content;
    private ScrollView scroll;
    private MaterialButton add;
    private CalendarImportModel model;
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> { if (!uris.isEmpty()) model.importFiles(uris); });
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleManager.wrap(base)); }
    @Override public void applyOverrideConfiguration(android.content.res.Configuration configuration) {
        if (configuration != null) configuration.setLocale(LocaleManager.resolveLocale(this));
        super.applyOverrideConfiguration(configuration);
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        UserCalendarStore.get(this);
        model = new ViewModelProvider(this).get(CalendarImportModel.class);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(com.google.android.material.R.attr.colorSurface));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle(R.string.calendar_import_title);
        toolbar.setNavigationIcon(R.drawable.import_ic_back);
        toolbar.setNavigationIconTint(color(com.google.android.material.R.attr.colorOnSurface));
        toolbar.setNavigationContentDescription(R.string.font_import_back);
        toolbar.setNavigationOnClickListener(view -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));
        scroll = new ScrollView(this); scroll.setClipToPadding(false);
        LinearLayout center = new LinearLayout(this); center.setGravity(Gravity.CENTER_HORIZONTAL);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(8), dp(24), dp(24));
        center.addView(content, new LinearLayout.LayoutParams(dp(Math.min(getResources().getConfiguration().screenWidthDp, 840)), -2));
        scroll.addView(center); scroll.setPadding(0, 0, 0, dp(104));
        FrameLayout library = new FrameLayout(this);
        library.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        root.addView(library, new LinearLayout.LayoutParams(-1, 0, 1));
        add = new ExtendedFloatingActionButton(this); add.setText(R.string.calendar_import_add);
        add.setIconResource(R.drawable.import_ic_add); add.setMinHeight(dp(56));
        add.setOnClickListener(view -> picker.launch(new String[]{"*/*"}));
        FrameLayout.LayoutParams action = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        action.setMargins(dp(16), dp(16), dp(16), dp(16));
        library.addView(add, action); setContentView(root);
        model.state.observe(this, ignored -> render());
        UserCalendarStore.get(this).changes.observe(this, ignored -> render());
    }
    private void render() {
        int scrollY = scroll.getScrollY();
        content.removeAllViews(); add.setEnabled(!model.busy);
        text(content, getString(R.string.calendar_import_summary), 16, false, false);
        if (model.busy) {
            LinearProgressIndicator progress = new LinearProgressIndicator(this);
            progress.setIndeterminate(true); content.addView(progress, new LinearLayout.LayoutParams(-1, dp(8)));
            text(content, getString(R.string.calendar_import_busy), 14, false, true);
        }
        for (CalendarImportModel.Result result : model.results) {
            TextView message = text(card(com.google.android.material.R.attr.colorSecondaryContainer),
                    getString(result.message, result.name), 14, false, false);
            message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        if (UserCalendarStore.get(this).files().isEmpty()) {
            LinearLayout empty = card(com.google.android.material.R.attr.colorSurfaceContainerLow);

            text(empty, getString(R.string.calendar_import_empty), 22, true, false);
            text(empty, getString(R.string.calendar_import_empty_hint), 15, false, true);
        }
        for (UserCalendarStore.Source source : UserCalendarStore.get(this).files()) {
            LinearLayout group = card(com.google.android.material.R.attr.colorSurfaceContainerLow);
            text(group, source.title, 22, true, false).setAccessibilityHeading(true);
            text(group, source.name, 16, false, false);
            text(group, getString(R.string.calendar_import_count, source.eventCount,
                    android.text.format.Formatter.formatShortFileSize(this, source.bytes)), 14, false, true);
            if (UserCalendarStore.get(this).error(source.id) != null)
                text(group, getString(R.string.calendar_import_query_error), 14, false, true);
            MaterialButton remove = new MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle);
            remove.setText(R.string.calendar_import_delete); remove.setIconResource(R.drawable.import_ic_delete);
            remove.setTextColor(color(androidx.appcompat.R.attr.colorError));
            remove.setIconTint(ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorError)));
            remove.setContentDescription(getString(R.string.calendar_import_delete) + " " + source.name);
            remove.setMinHeight(dp(48)); remove.setEnabled(!model.busy);
            remove.setOnClickListener(view -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.calendar_import_delete)
                    .setMessage(getString(R.string.calendar_import_delete_confirm, source.name))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.calendar_import_delete, (dialog, which) -> model.delete(source.id)).show());
            LinearLayout.LayoutParams action = new LinearLayout.LayoutParams(-2, -2);
            action.gravity = Gravity.END; group.addView(remove, action);
        }
        content.post(() -> scroll.scrollTo(0, scrollY));
    }
    private LinearLayout card(int colorAttr) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(28)); card.setCardElevation(0); card.setStrokeWidth(0); card.setCardBackgroundColor(color(colorAttr));
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(16), dp(20), dp(12)); card.addView(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(16); content.addView(card, params); return body;
    }
    private TextView text(LinearLayout parent, String value, int size, boolean bold, boolean supporting) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size);
        text.setTextColor(color(supporting ? com.google.android.material.R.attr.colorOnSurfaceVariant : com.google.android.material.R.attr.colorOnSurface));
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setPadding(0, dp(6), 0, dp(6)); parent.addView(text, new LinearLayout.LayoutParams(-1, -2)); return text;
    }
    private int color(int attr) { return MaterialColors.getColor(this, attr, "CalendarManagerActivity"); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
