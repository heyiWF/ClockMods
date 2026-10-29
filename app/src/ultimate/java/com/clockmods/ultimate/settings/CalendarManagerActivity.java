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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/** Material 3 calendar library with source-file cards and file-level actions. */
public final class CalendarManagerActivity extends AppCompatActivity {
    private LinearLayout content;
    private ScrollView scroll;
    private MaterialToolbar toolbar;
    private FloatingActionButton add;
    private View menuScrim;
    private LinearLayout importActions;
    private boolean menuExpanded;
    private float menuProgress;
    private android.animation.ValueAnimator menuAnimator;
    private android.graphics.drawable.RotateDrawable addIcon;
    private final androidx.activity.OnBackPressedCallback closeMenu = new androidx.activity.OnBackPressedCallback(false) {
        @Override public void handleOnBackPressed() { setMenuExpanded(false, true); }
    };
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
        toolbar = new MaterialToolbar(this);
        toolbar.setTitle(R.string.calendar_import_title);
        toolbar.setNavigationIcon(R.drawable.import_ic_back);
        toolbar.setNavigationIconTint(color(com.google.android.material.R.attr.colorOnSurface));
        toolbar.setNavigationContentDescription(R.string.font_import_back);
        toolbar.setNavigationOnClickListener(view -> { if (menuExpanded) setMenuExpanded(false, true); else finish(); });
        toolbar.setOnClickListener(view -> { if (menuExpanded) setMenuExpanded(false, true); });
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
        menuScrim = new View(this);
        menuScrim.setBackgroundColor(color(com.google.android.material.R.attr.colorSurface));
        menuScrim.setAlpha(0f); menuScrim.setVisibility(View.GONE);
        menuScrim.setOnClickListener(view -> setMenuExpanded(false, true));
        library.addView(menuScrim, new FrameLayout.LayoutParams(-1, -1));
        importActions = new LinearLayout(this); importActions.setOrientation(LinearLayout.VERTICAL);
        importActions.setGravity(Gravity.END); importActions.setVisibility(View.INVISIBLE);
        MaterialButton linkAction = importAction(R.string.calendar_import_link, R.drawable.import_ic_link);
        linkAction.setOnClickListener(view -> { setMenuExpanded(false, true); showLinkDialog(); });
        MaterialButton fileAction = importAction(R.string.calendar_import_file, R.drawable.import_ic_file);
        fileAction.setOnClickListener(view -> { setMenuExpanded(false, true); picker.launch(new String[]{"*/*"}); });
        FrameLayout.LayoutParams actions = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        actions.setMargins(dp(16), dp(16), dp(16), dp(80)); library.addView(importActions, actions);
        add = new FloatingActionButton(this);
        addIcon = new android.graphics.drawable.RotateDrawable();
        addIcon.setDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.import_ic_add));
        addIcon.setFromDegrees(0); addIcon.setToDegrees(45);
        add.setImageDrawable(addIcon);
        add.setContentDescription(getString(R.string.calendar_import_add));
        add.setShapeAppearanceModel(add.getShapeAppearanceModel().toBuilder().setAllCornerSizes(dp(16)).build());
        add.setOnClickListener(view -> setMenuExpanded(!menuExpanded, true));
        FrameLayout.LayoutParams action = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
        action.setMargins(dp(16), dp(16), dp(16), dp(16));
        library.addView(add, action); setContentView(root);
        getOnBackPressedDispatcher().addCallback(this, closeMenu);
        if (savedInstanceState != null && savedInstanceState.getBoolean("import_menu"))
            setMenuExpanded(true, false);
        model.state.observe(this, ignored -> render());
        UserCalendarStore.get(this).changes.observe(this, ignored -> render());
    }
    private void render() {
        int scrollY = scroll.getScrollY();
        content.removeAllViews(); add.setEnabled(!model.busy);
        if (model.busy && menuExpanded) setMenuExpanded(false, true);
        text(content, getString(R.string.calendar_import_summary), 16, false, false);
        com.google.android.material.materialswitch.MaterialSwitch carousel = new com.google.android.material.materialswitch.MaterialSwitch(this);
        carousel.setText(R.string.calendar_import_carousel); carousel.setMinHeight(dp(48));
        carousel.setChecked(UserCalendarStore.get(this).showInDateCarousel());
        carousel.setOnCheckedChangeListener((button, checked) -> UserCalendarStore.get(this).setShowInDateCarousel(checked));
        content.addView(carousel, new LinearLayout.LayoutParams(-1, -2));
        text(content, getString(R.string.calendar_import_carousel_hint), 14, false, true);
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
            if (!source.sourceUrl.isEmpty()) {
                com.google.android.material.materialswitch.MaterialSwitch sync = subscriptionSwitch(source.syncEnabled, !model.busy);
                sync.setOnCheckedChangeListener((button, checked) -> model.setSyncEnabled(source.id, checked));
                group.addView(sync, new LinearLayout.LayoutParams(-1, -2));
                if (source.lastSyncedAt > 0) text(group, getString(R.string.calendar_sync_last,
                        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                                .format(new java.util.Date(source.lastSyncedAt))), 14, false, true);
                if (source.syncFailed) text(group, getString(R.string.calendar_sync_failed), 14, false, true);
                if (source.syncEnabled) {
                    MaterialButton refresh = new MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle);
                    refresh.setText(R.string.calendar_sync_now); refresh.setEnabled(!model.busy);
                    refresh.setOnClickListener(view -> model.syncNow(source)); group.addView(refresh);
                }
            }
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
    private void showLinkDialog() {
        com.google.android.material.textfield.TextInputLayout field = new com.google.android.material.textfield.TextInputLayout(this);
        field.setHint(getString(R.string.calendar_import_url));
        field.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        field.setStartIconDrawable(R.drawable.import_ic_link);
        com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(field.getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setSingleLine(true); field.addView(input);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        form.addView(field, new LinearLayout.LayoutParams(-1, -2));
        com.google.android.material.materialswitch.MaterialSwitch sync = subscriptionSwitch(false, true);
        sync.setPadding(dp(16), dp(16), dp(16), dp(16));
        MaterialCardView syncCard = new MaterialCardView(this);
        syncCard.setRadius(dp(16)); syncCard.setCardElevation(0); syncCard.setStrokeWidth(0);
        syncCard.setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceContainerHigh));
        syncCard.addView(sync, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams syncLayout = new LinearLayout.LayoutParams(-1, -2);
        syncLayout.topMargin = dp(16); form.addView(syncCard, syncLayout);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.calendar_import_link).setMessage(R.string.calendar_import_link_hint)
                .setView(form).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.calendar_import_add, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String value = input.getText() == null ? "" : input.getText().toString();
            try { com.clockmods.calendar.imports.CalendarLink.normalize(value); }
            catch (java.io.IOException failure) { field.setError(getString(R.string.calendar_import_url_invalid)); return; }
            model.importLink(value, sync.isChecked()); dialog.dismiss();
        }));
        dialog.show();
    }
    private com.google.android.material.materialswitch.MaterialSwitch subscriptionSwitch(boolean checked, boolean enabled) {
        com.google.android.material.materialswitch.MaterialSwitch control = new com.google.android.material.materialswitch.MaterialSwitch(this);
        String title = getString(R.string.calendar_sync_enabled);
        android.text.SpannableString text = new android.text.SpannableString(title + "\n" + getString(R.string.calendar_sync_interval));
        text.setSpan(new android.text.style.AbsoluteSizeSpan(14, true), title.length() + 1, text.length(), 0);
        text.setSpan(new android.text.style.ForegroundColorSpan(color(com.google.android.material.R.attr.colorOnSurfaceVariant)),
                title.length() + 1, text.length(), 0);
        control.setText(text); control.setTextSize(16); control.setSwitchPadding(dp(16));
        control.setMinHeight(dp(64)); control.setChecked(checked); control.setEnabled(enabled);
        return control;
    }
    private MaterialButton importAction(int title, int icon) {
        RevealButton button = new RevealButton(this);
        button.setText(title); button.setIconResource(icon); button.setCornerRadius(dp(28));
        button.setBackgroundTintList(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorSecondaryContainer)));
        button.setTextColor(color(com.google.android.material.R.attr.colorOnSecondaryContainer));
        button.setIconTint(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorOnSecondaryContainer)));
        button.setInsetTop(0); button.setInsetBottom(0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(56));
        params.topMargin=dp(8); importActions.addView(button,params);return button;
    }
    private void setMenuExpanded(boolean expanded, boolean animate) {
        menuExpanded = expanded; closeMenu.setEnabled(expanded);
        add.setContentDescription(getString(expanded ? R.string.calendar_import_close : R.string.calendar_import_add));
        scroll.setImportantForAccessibility(expanded ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        if (menuAnimator != null) menuAnimator.cancel();
        menuScrim.setVisibility(View.VISIBLE); importActions.setVisibility(View.VISIBLE);
        menuAnimator = android.animation.ValueAnimator.ofFloat(menuProgress, expanded ? 1f : 0f);
        menuAnimator.setDuration(animate ? 200 : 0);
        menuAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        menuAnimator.addUpdateListener(animation -> {
            menuProgress = (float) animation.getAnimatedValue();
            menuScrim.setAlpha(.94f * menuProgress);
            toolbar.setAlpha(1f - .94f * menuProgress);
            addIcon.setLevel(Math.round(menuProgress * 10000));
            add.setShapeAppearanceModel(add.getShapeAppearanceModel().toBuilder()
                    .setAllCornerSizes(dp(16) + dp(12) * menuProgress).build());
            add.setBackgroundTintList(ColorStateList.valueOf(blend(
                    com.google.android.material.R.attr.colorPrimaryContainer, androidx.appcompat.R.attr.colorPrimary)));
            add.setImageTintList(ColorStateList.valueOf(blend(
                    com.google.android.material.R.attr.colorOnPrimaryContainer, com.google.android.material.R.attr.colorOnPrimary)));
            if (menuProgress == 0f && !menuExpanded) {
                menuScrim.setVisibility(View.GONE); importActions.setVisibility(View.INVISIBLE);
            }
        });
        menuAnimator.start();
        for (int i = 0; i < importActions.getChildCount(); i++) {
            RevealButton item = (RevealButton) importActions.getChildAt(i);
            item.setEnabled(expanded && !model.busy);
            item.setImportantForAccessibility(expanded ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            item.expand(expanded, animate, expanded ? importActions.getChildCount() - 1 - i : i);
        }
    }
    private int blend(int collapsed, int expanded) {
        return androidx.core.graphics.ColorUtils.blendARGB(color(collapsed), color(expanded), menuProgress);
    }
    /** Fixed text/baseline, pill revealed from its right edge; nearest action opens first. */
    private static final class RevealButton extends MaterialButton {
        private final android.graphics.Path clip = new android.graphics.Path();
        private float fraction;
        private android.animation.ValueAnimator animator;
        RevealButton(Context context) { super(context); }
        void expand(boolean expanded, boolean animate, int order) {
            if (animator != null) animator.cancel();
            animator = android.animation.ValueAnimator.ofFloat(fraction, expanded ? 1f : 0f);
            animator.setDuration(animate ? (expanded ? 200 : 100) : 0);
            // Reference frames: 50 ms lead-in, 50 ms between items; reverse order on close.
            animator.setStartDelay(animate && (fraction == 0f || fraction == 1f)
                    ? (expanded ? 50L : 0L) + order * 50L : 0);
            animator.setInterpolator(expanded ? new android.view.animation.PathInterpolator(.2f, 0f, 0f, 1f)
                    : new android.view.animation.PathInterpolator(.4f, 0f, 1f, 1f));
            animator.addUpdateListener(animation -> {
                fraction = (float) animation.getAnimatedValue();
                setAlpha(Math.min(1f, fraction * 3f)); invalidate();
            });
            animator.start();
        }
        @Override public void draw(android.graphics.Canvas canvas) {
            if (fraction <= 0f) return;
            int save = canvas.save();
            float width = getWidth() * fraction;
            float radius = Math.min(getHeight() / 2f, width / 2f);
            clip.reset(); clip.addRoundRect(getWidth() - width, 0, getWidth(), getHeight(),
                    radius, radius, android.graphics.Path.Direction.CW);
            canvas.clipPath(clip); super.draw(canvas); canvas.restoreToCount(save);
        }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("import_menu",menuExpanded);super.onSaveInstanceState(state);
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
