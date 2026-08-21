package com.clockmods.ultimate.settings;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.OptIn;
import androidx.core.util.Consumer;
import androidx.window.core.ExperimentalWindowApi;
import androidx.window.embedding.ActivityEmbeddingController;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitInfo;
import androidx.window.java.embedding.SplitControllerCallbackAdapter;

import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.AutoStartManager;
import com.clockmods.background.ClockPreferences;
import com.clockmods.background.FontCatalog;
import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.sdk.clock.ClockStyleCapabilities;
import com.clockmods.sdk.clock.ClockStyleRegistry;
import com.clockmods.time.RegionTimeZones;
import com.clockmods.ui.ColorPickerView;
import com.clockmods.ui.DateFormatter;
import com.clockmods.ui.WeatherLocationChooser;
import com.clockmods.ultimate.clock.UltimateClockPreferences;
import com.clockmods.ultimate.clock.UltimateClockStyles;
import com.clockmods.weather.WeatherLocationCatalog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Ultimate settings shell.  Pages are deliberately kept as plain views so a third-party
 * style provider can add a page or a control without depending on the clock renderer.  The
 * durable contract for the style picker is the shared-preferences file and keys below.
 */
@OptIn(markerClass = ExperimentalWindowApi.class)
public class UltimateSettingsActivity extends AppCompatActivity {
    public static final String ACTION_OPEN = "com.clockmods.ultimate.action.OPEN_SETTINGS";
    public static final String ACTION_OPEN_SUBPAGE =
            "com.clockmods.ultimate.action.OPEN_SETTINGS_SUBPAGE";
    public static final String EXTRA_SETTINGS_CHANGED =
            "com.clockmods.ultimate.settings.extra.CHANGED";
    public static final String EXTRA_STYLE_ID =
            "com.clockmods.ultimate.settings.extra.STYLE_ID";
    public static final String EXTRA_CHANGE_SOURCE =
            "com.clockmods.ultimate.settings.extra.CHANGE_SOURCE";

    public static final String STYLE_PREFERENCES = UltimateClockPreferences.PREFERENCES_NAME;
    public static final String KEY_STYLE_ID = UltimateClockPreferences.KEY_STYLE_ID;
    public static final String KEY_SECOND_MOTION = UltimateClockPreferences.KEY_SECOND_MOTION;
    public static final String KEY_FOLLOW_REDUCED_MOTION =
            UltimateClockPreferences.KEY_FOLLOW_REDUCED_MOTION;

    // Keep the settings contract tied to the SDK's stable IDs. These aliases preserve a small
    // public surface for integrations while preventing the UI and registry from drifting apart.
    public static final String STYLE_GLASS_ATELIER = UltimateClockStyles.STYLE_GLASS_ATELIER;
    public static final String STYLE_PRO_CLASSIC = UltimateClockStyles.STYLE_PRO_CLASSIC;
    public static final String STYLE_NOIR_INSTRUMENT = UltimateClockStyles.STYLE_NOIR_INSTRUMENT;
    public static final String STYLE_PAPER_STATION = UltimateClockStyles.STYLE_PAPER_STATION;
    public static final String STYLE_ORBIT_NEON = UltimateClockStyles.STYLE_ORBIT_NEON;
    public static final String STYLE_DIGITAL_GRID = UltimateClockStyles.STYLE_DIGITAL_GRID;
    public static final String STYLE_TYPOGRAPHIC = UltimateClockStyles.STYLE_TYPOGRAPHIC;

    public static final String MOTION_SMOOTH = "smooth";
    public static final String MOTION_TICK = "tick";
    public static final String MOTION_OFF = "off";

    private static final String STATE_CHANGED = "ultimate_settings_changed";
    private static final String STATE_CHANGE_SOURCE = "ultimate_settings_change_source";
    private static final String STATE_SELECTED_PAGE = "ultimate_settings_selected_page";
    private static final String STATE_REVISION_AT_OPEN = "ultimate_settings_revision_at_open";
    private static final String CHANGE_SOURCE_CLOCK_LANGUAGE = "clock_language";
    private static final String EXTRA_PAGE_ID =
            "com.clockmods.ultimate.settings.extra.PAGE_ID";
    private static final int REQUEST_BACKGROUND_IMAGE = 4801;
    private static final int REQUEST_SUBPAGE = 4802;

    private enum Page {
        HOME("home"),
        STYLE("style"),
        BACKGROUND("background"),
        TIME_DATE("time_date"),
        WEATHER("weather"),
        CALENDAR("calendar"),
        CHIME("chime"),
        SYSTEM("system");

        final String id;

        Page(String id) {
            this.id = id;
        }

        static Page fromId(String id) {
            if (id != null) {
                for (Page page : values()) {
                    if (page.id.equals(id)) return page;
                }
            }
            return HOME;
        }
    }

    private interface BooleanChange {
        void apply(boolean value);
    }

    private interface IntChange {
        void apply(int value);
    }

    private interface FloatChange {
        void apply(float value);
    }

    private BackgroundRepository repository;
    private UltimateClockPreferences ultimatePreferences;
    private LinearLayout rootLayout;
    private MaterialToolbar toolbar;
    private FrameLayout pageHost;
    private LinearLayout currentPageBody;
    private SplitControllerCallbackAdapter splitCallbackAdapter;
    private boolean splitListenerRegistered;
    private Page currentPage = Page.HOME;
    private Page selectedSubPage = Page.STYLE;
    private boolean settingsChanged;
    private String lastChangeSource = "";
    private long settingsRevisionAtOpen;
    private long observedSettingsRevision;
    private SharedPreferences settingsChangePreferences;
    private final List<MaterialCardView> styleCards = new ArrayList<>();
    private final List<StyleSpec> styleSpecs = new ArrayList<>();
    private final EnumMap<Page, NavigationItem> navigationItems = new EnumMap<>(Page.class);
    private final List<NavigationGroup> navigationGroups = new ArrayList<>();
    private TextView navigationEmptyState;
    private Page navigationSelectedPage;
    private boolean navigationSelectionEmbedded;
    private final ClockStyleRegistry styleRegistry = UltimateClockStyles.sharedRegistry();
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private final Consumer<List<SplitInfo>> splitInfoListener = splitInfoList ->
            updateEmbeddingChrome();
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsChangeListener =
            (preferences, key) -> {
                if (!UltimateSettingsChangeTracker.isRevisionKey(key)) return;
                long revision = UltimateSettingsChangeTracker.revision(this);
                if (revision == observedSettingsRevision) return;
                observedSettingsRevision = revision;
                syncExternalChanges();
                if (settingsChanged) publishResult();
                if (CHANGE_SOURCE_CLOCK_LANGUAGE.equals(lastChangeSource) && toolbar != null) {
                    toolbar.post(() -> {
                        if (!isFinishing() && !isDestroyed()) recreate();
                    });
                }
            };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            overrideConfiguration.setLocale(LocaleManager.resolveLocale(this));
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, UltimateSettingsActivity.class).setAction(ACTION_OPEN);
    }

    public static Intent createSubpageIntent(Context context, String pageId) {
        return new Intent(context, UltimateSubSettingsActivity.class)
                .setAction(ACTION_OPEN_SUBPAGE)
                .putExtra(EXTRA_PAGE_ID, pageId);
    }

    static Intent createDefaultSubpageIntent(Context context) {
        return createSubpageIntent(context, Page.STYLE.id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        repository = new BackgroundRepository(this);
        ultimatePreferences = new UltimateClockPreferences(this);
        SplitController splitController = SplitController.getInstance(this);
        if (splitController.getSplitSupportStatus()
                == SplitController.SplitSupportStatus.SPLIT_AVAILABLE) {
            splitCallbackAdapter = new SplitControllerCallbackAdapter(splitController);
        }
        settingsChangePreferences = UltimateSettingsChangeTracker.preferences(this);
        observedSettingsRevision = UltimateSettingsChangeTracker.revision(this);
        settingsRevisionAtOpen = savedInstanceState == null
                ? observedSettingsRevision
                : savedInstanceState.getLong(STATE_REVISION_AT_OPEN, observedSettingsRevision);
        settingsChangePreferences.registerOnSharedPreferenceChangeListener(
                settingsChangeListener);
        settingsChanged = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_CHANGED, false);
        if (savedInstanceState != null) {
            lastChangeSource = savedInstanceState.getString(STATE_CHANGE_SOURCE, "");
            selectedSubPage = Page.fromId(savedInstanceState.getString(
                    STATE_SELECTED_PAGE, Page.STYLE.id));
        }
        currentPage = Page.fromId(getIntent().getStringExtra(EXTRA_PAGE_ID));
        configureEdgeToEdge();
        buildShell();
        showPage(currentPage);
        syncExternalChanges();
        if (!settingsChanged) {
            setResult(RESULT_CANCELED);
        } else {
            publishResult();
        }
    }

    private void configureEdgeToEdge() {
        Window window = getWindow();
        View decorView = window.getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            int surface = surfaceColor();
            boolean lightBars = luminance(surface) > 0.55f;
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                int appearance = lightBars
                        ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                        : 0;
                controller.setSystemBarsAppearance(appearance,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
    }

    private void buildShell() {
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor());
        rootLayout.setFitsSystemWindows(false);
        rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
            int type = WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout();
            android.graphics.Insets bars = insets.getInsets(type);
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        navigationItems.clear();
        toolbar = createToolbar();
        rootLayout.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
        pageHost = new FrameLayout(this);
        pageHost.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        rootLayout.addView(pageHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(rootLayout);
        rootLayout.requestApplyInsets();
        rootLayout.post(this::updateEmbeddingChrome);
    }

    private MaterialToolbar createToolbar() {
        MaterialToolbar result = new MaterialToolbar(this);
        result.setTitleTextColor(onSurfaceColor());
        result.setMinimumHeight(dp(64));
        result.setNavigationIcon(R.drawable.ultimate_ic_arrow_back);
        result.setNavigationIconTint(onSurfaceColor());
        result.setNavigationOnClickListener(view -> finishAfterTransition());
        return result;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        syncExternalChanges();
        outState.putBoolean(STATE_CHANGED, settingsChanged);
        outState.putString(STATE_CHANGE_SOURCE, lastChangeSource);
        outState.putString(STATE_SELECTED_PAGE, selectedSubPage.id);
        outState.putLong(STATE_REVISION_AT_OPEN, settingsRevisionAtOpen);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void finish() {
        syncExternalChanges();
        if (settingsChanged) {
            publishResult();
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        unregisterSplitListener();
        if (settingsChangePreferences != null) {
            settingsChangePreferences.unregisterOnSharedPreferenceChangeListener(
                    settingsChangeListener);
        }
        imageExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (splitCallbackAdapter != null && !splitListenerRegistered) {
            splitCallbackAdapter.addSplitListener(this, getMainExecutor(), splitInfoListener);
            splitListenerRegistered = true;
        }
    }

    @Override
    protected void onStop() {
        unregisterSplitListener();
        super.onStop();
    }

    private void unregisterSplitListener() {
        if (splitCallbackAdapter != null && splitListenerRegistered) {
            splitCallbackAdapter.removeSplitListener(splitInfoListener);
            splitListenerRegistered = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncExternalChanges();
        if (settingsChanged) publishResult();
        updateEmbeddingChrome();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        updateEmbeddingChrome();
    }

    private void showPage(Page page) {
        Page targetPage = page == null ? Page.HOME : page;
        styleCards.clear();
        View incoming;
        int titleRes;
        switch (targetPage) {
            case STYLE:
                titleRes = R.string.ultimate_style_title;
                incoming = stylePage();
                break;
            case BACKGROUND:
                titleRes = R.string.ultimate_background_title;
                incoming = backgroundPage();
                break;
            case TIME_DATE:
                titleRes = R.string.ultimate_time_date_title;
                incoming = timeDatePage();
                break;
            case WEATHER:
                titleRes = R.string.ultimate_weather_title;
                incoming = weatherPage();
                break;
            case CALENDAR:
                titleRes = R.string.ultimate_calendar_title;
                incoming = calendarPage();
                break;
            case CHIME:
                titleRes = R.string.ultimate_chime_title;
                incoming = chimePage();
                break;
            case SYSTEM:
                titleRes = R.string.ultimate_system_title;
                incoming = systemPage();
                break;
            case HOME:
            default:
                titleRes = R.string.ultimate_settings_title;
                incoming = homePage();
                break;
        }

        toolbar.setTitle(titleRes);

        pageHost.removeAllViews();
        pageHost.addView(incoming);

        currentPage = targetPage;
        updateEmbeddingChrome();
        pageHost.setContentDescription(getString(R.string.ultimate_page_accessibility));
    }

    private View homePage() {
        navigationItems.clear();
        navigationGroups.clear();
        LinearLayout body = pageBody(0);
        addNavigationSearch(body);

        NavigationGroup appearanceGroup = addNavigationGroup(body,
                R.dimen.ultimate_settings_home_search_group_gap);
        addCategory(appearanceGroup, Page.STYLE, R.string.ultimate_category_clock_style,
                R.string.ultimate_category_clock_style_summary, R.drawable.ultimate_ic_palette,
                () -> openPage(Page.STYLE));
        addCategory(appearanceGroup, Page.BACKGROUND, R.string.ultimate_category_background,
                R.string.ultimate_category_background_summary, R.drawable.ultimate_ic_wallpaper,
                () -> openPage(Page.BACKGROUND));

        NavigationGroup settingsGroup = addNavigationGroup(body,
                R.dimen.ultimate_settings_home_group_gap);
        addCategory(settingsGroup, Page.TIME_DATE, R.string.ultimate_category_time_date,
                R.string.ultimate_category_time_date_summary, R.drawable.ultimate_ic_schedule,
                () -> openPage(Page.TIME_DATE));
        addCategory(settingsGroup, Page.WEATHER, R.string.ultimate_category_weather,
                R.string.ultimate_category_weather_summary, R.drawable.ultimate_ic_weather,
                () -> openPage(Page.WEATHER));
        addCategory(settingsGroup, Page.CALENDAR, R.string.ultimate_category_calendar,
                R.string.ultimate_category_calendar_summary, R.drawable.ultimate_ic_calendar,
                () -> openPage(Page.CALENDAR));
        addCategory(settingsGroup, Page.CHIME, R.string.ultimate_category_chime,
                R.string.ultimate_category_chime_summary, R.drawable.ultimate_ic_notifications,
                () -> openPage(Page.CHIME));
        addCategory(settingsGroup, Page.SYSTEM, R.string.ultimate_category_system,
                R.string.ultimate_category_system_summary, R.drawable.ultimate_ic_language,
                () -> openPage(Page.SYSTEM));

        navigationEmptyState = label(R.string.ultimate_settings_search_no_results, 14, false);
        navigationEmptyState.setGravity(Gravity.CENTER);
        navigationEmptyState.setPadding(0, dp(32), 0, dp(32));
        navigationEmptyState.setVisibility(View.GONE);
        body.addView(navigationEmptyState, wrapParams());
        applyNavigationFilter("");
        return scrollable(body);
    }

    private void openPage(Page page) {
        if (currentPage != Page.HOME || page == null || page == Page.HOME) return;
        boolean embedded = isActivityEmbedded();
        if (embedded && page == selectedSubPage) return;
        selectedSubPage = page;
        updateNavigationSelection(embedded ? selectedSubPage : null, embedded);
        Intent intent = createSubpageIntent(this, page.id);
        startActivityForResult(intent, REQUEST_SUBPAGE);
    }

    private boolean isActivityEmbedded() {
        return ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this);
    }

    private void updateEmbeddingChrome() {
        if (toolbar == null) return;
        boolean embedded = isActivityEmbedded();
        boolean detailPage = currentPage != Page.HOME;
        updatePaneAppearance(embedded, detailPage);
        if (!detailPage) {
            toolbar.setTitle(embedded ? R.string.ultimate_settings_pane_title
                    : R.string.ultimate_settings_title);
        }
        if (embedded && detailPage) {
            toolbar.setNavigationIcon((android.graphics.drawable.Drawable) null);
            toolbar.setNavigationOnClickListener(null);
            toolbar.setNavigationContentDescription((CharSequence) null);
        } else {
            toolbar.setNavigationIcon(R.drawable.ultimate_ic_arrow_back);
            toolbar.setNavigationIconTint(onSurfaceColor());
            toolbar.setNavigationOnClickListener(view -> finishAfterTransition());
            toolbar.setNavigationContentDescription(detailPage
                    ? R.string.ultimate_back_to_settings
                    : R.string.ultimate_close_settings);
        }
        updateNavigationSelection(embedded && !detailPage ? selectedSubPage : null,
                embedded && !detailPage);
    }

    private void updatePaneAppearance(boolean embedded, boolean detailPage) {
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(detailPage ? surfaceColor() : settingsHomeCanvasColor());
        }
        if (toolbar != null) {
            toolbar.setVisibility(embedded && !detailPage ? View.GONE : View.VISIBLE);
        }
        if (currentPageBody != null) {
            int horizontalPadding;
            int topPadding;
            if (!detailPage) {
                horizontalPadding = getResources().getDimensionPixelSize(
                        R.dimen.ultimate_settings_home_padding_horizontal);
                topPadding = getResources().getDimensionPixelSize(
                        R.dimen.ultimate_settings_home_padding_top);
            } else {
                horizontalPadding = getResources().getDimensionPixelSize(embedded
                        ? R.dimen.ultimate_settings_page_padding_horizontal_two_pane
                        : R.dimen.ultimate_settings_page_padding_horizontal);
                topPadding = dp(12);
            }
            currentPageBody.setPadding(horizontalPadding, topPadding,
                    horizontalPadding, dp(32));
        }
    }

    private View stylePage() {
        LinearLayout body = pageBody(0);
        addSectionLabel(body, R.string.ultimate_style_gallery);
        HorizontalScrollView galleryScroll = new HorizontalScrollView(this);
        galleryScroll.setHorizontalScrollBarEnabled(false);
        galleryScroll.setClipToPadding(false);
        LinearLayout gallery = new LinearLayout(this);
        gallery.setOrientation(LinearLayout.HORIZONTAL);
        gallery.setPadding(0, dp(4), dp(20), dp(8));
        buildStyleSpecs();
        final String selectedId = selectedStyleId();
        final ClockStyle selectedStyle = styleRegistry.resolveForApi(
                selectedId, android.os.Build.VERSION.SDK_INT);
        final ClockStyleCapabilities capabilities =
                selectedStyle.getMetadata().getCapabilities();
        final boolean proClassic = STYLE_PRO_CLASSIC.equals(selectedId);
        int cardSpacing = getResources().getDimensionPixelSize(
                R.dimen.ultimate_style_gallery_item_spacing);
        for (int styleIndex = 0; styleIndex < styleSpecs.size(); styleIndex++) {
            StyleSpec spec = styleSpecs.get(styleIndex);
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dp(8));
            card.setCardElevation(0f);
            card.setUseCompatPadding(false);
            card.setCheckable(true);
            card.setClickable(true);
            card.setFocusable(true);
            card.setContentDescription(getString(R.string.ultimate_style_choose,
                    spec.name));
            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(dp(8), dp(8), dp(8), dp(8));
            UltimateThemePreviewView preview = new UltimateThemePreviewView(this, spec.style);
            preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            cardContent.addView(preview, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(118)));
            TextView name = label(spec.name, 15, true);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cardContent.addView(name, topMargin(wrapParams(), dp(8)));
            TextView summary = label(spec.summary, 12, false);
            summary.setMaxLines(1);
            summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cardContent.addView(summary, topMargin(wrapParams(), dp(2)));
            card.addView(cardContent, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.setOnClickListener(view -> {
                ultimatePreferences.setStyleId(spec.id);
                updateStyleCards(spec.id);
                markChanged("style_id");
                view.announceForAccessibility(getString(R.string.ultimate_style_selected,
                        spec.name));
                showPage(Page.STYLE);
            });
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    dp(182), dp(188));
            if (styleIndex < styleSpecs.size() - 1) {
                cardParams.setMarginEnd(cardSpacing);
            }
            gallery.addView(card, cardParams);
            styleCards.add(card);
        }
        galleryScroll.addView(gallery, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(galleryScroll, topMargin(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(202)), dp(4)));
        updateStyleCards(selectedId);

        if (!proClassic && capabilities.supports(
                ClockStyleCapabilities.Capability.SECONDS)) {
            addSectionLabel(body, R.string.ultimate_style_second_motion, 22);
            TextView motionSummary = label(
                    R.string.ultimate_style_second_motion_summary, 13, false);
            body.addView(motionSummary, topMargin(wrapParams(), dp(2)));
            ClockState.SecondHandMotion motion = ultimatePreferences.getSecondHandMotion();
            if (capabilities.supports(ClockStyleCapabilities.Capability.SMOOTH_SECONDS)) {
                int motionPosition = motion == ClockState.SecondHandMotion.TICK ? 1
                        : motion == ClockState.SecondHandMotion.OFF ? 2 : 0;
                addSegmented(body, new int[] {R.string.ultimate_motion_smooth,
                                R.string.ultimate_motion_tick, R.string.ultimate_motion_off},
                        motionPosition, value -> {
                            ClockState.SecondHandMotion selected = value == 0
                                    ? ClockState.SecondHandMotion.SWEEP
                                    : value == 1 ? ClockState.SecondHandMotion.TICK
                                    : ClockState.SecondHandMotion.OFF;
                            ultimatePreferences.setSecondHandMotion(selected);
                            markChanged("second_motion");
                        });
            } else {
                int motionPosition = motion == ClockState.SecondHandMotion.OFF ? 1 : 0;
                addSegmented(body, new int[] {R.string.ultimate_motion_tick,
                                R.string.ultimate_motion_off}, motionPosition, value -> {
                            ultimatePreferences.setSecondHandMotion(value == 0
                                    ? ClockState.SecondHandMotion.TICK
                                    : ClockState.SecondHandMotion.OFF);
                            markChanged("second_motion");
                        });
            }
        }
        if (!proClassic && capabilities.supports(
                ClockStyleCapabilities.Capability.REDUCED_MOTION)) {
            addSwitch(body, R.string.ultimate_style_follow_reduced_motion,
                    R.string.ultimate_style_follow_reduced_motion_summary,
                    ultimatePreferences.isFollowSystemReducedMotion(), value -> {
                        ultimatePreferences.setFollowSystemReducedMotion(value);
                        markChanged("follow_reduced_motion");
                    });
        }

        if (proClassic) {
            addProClassicTypographySettings(body);
        }
        return scrollable(body);
    }

    private void addProClassicTypographySettings(LinearLayout body) {
        addSectionLabel(body, R.string.ultimate_typography_section, 22);
        String[] fontNames = FontCatalog.displayNames(this);
        int fontIndex = FontCatalog.indexOf(repository.getFontFamily());
        addActionRow(body, R.string.ultimate_font_family,
                fontNames[fontIndex],
                R.drawable.ultimate_ic_chevron_right, () -> showSingleChoiceDialog(
                        R.string.ultimate_font_family, fontNames,
                        FontCatalog.indexOf(repository.getFontFamily()), value ->
                                repository.setFontFamily(FontCatalog.idForIndex(value)),
                        "font_family", true));
        addSwitch(body, R.string.ultimate_bold_text,
                R.string.ultimate_bold_text_summary, repository.isBoldText(), value -> {
                    repository.setBoldText(value);
                    markChanged("bold_text");
                });
    }

    private void addProClassicTimeAppearanceSettings(LinearLayout body) {
        addSectionLabel(body, R.string.ultimate_time_appearance_section, 22);
        addActionRow(body, R.string.ultimate_time_size,
                percentSummary(repository.getTimeFontScale()),
                R.drawable.ultimate_ic_chevron_right, () -> showScaleDialog(
                        R.string.ultimate_time_size, repository.getTimeFontScale(),
                        ClockPreferences.MIN_FONT_SCALE, ClockPreferences.MAX_FONT_SCALE,
                        repository::setTimeFontScale, "time_font_scale"));
        addActionRow(body, R.string.ultimate_time_color,
                colorSummary(repository.getTimeColor()), R.drawable.ultimate_ic_chevron_right,
                () -> showColorDialog(R.string.ultimate_time_color,
                        R.string.ultimate_time_color_picker, repository.getTimeColor(),
                        repository::setTimeColor, "time_color"));
        addSwitch(body, R.string.ultimate_blink_colon,
                R.string.ultimate_blink_colon_summary, repository.isBlinkColon(), value -> {
                    repository.setBlinkColon(value);
                    markChanged("blink_colon");
                });
        addSwitch(body, R.string.ultimate_animate_time_changes,
                R.string.ultimate_animate_time_changes_summary,
                repository.isAnimateTimeChanges(), value -> {
                    repository.setAnimateTimeChanges(value);
                    markChanged("animate_time_changes");
                });
        String[] transitionNames = {
                getString(R.string.ultimate_transition_fade),
                getString(R.string.ultimate_transition_slide_up),
                getString(R.string.ultimate_transition_slide_down),
                getString(R.string.ultimate_transition_scale),
                getString(R.string.ultimate_transition_flip)
        };
        addActionRow(body, R.string.ultimate_time_transition,
                transitionNames[transitionIndex(repository.getTimeTransition())],
                R.drawable.ultimate_ic_chevron_right, () -> showSingleChoiceDialog(
                        R.string.ultimate_time_transition, transitionNames,
                        transitionIndex(repository.getTimeTransition()), value ->
                                repository.setTimeTransition(transitionForIndex(value)),
                        "time_transition", true));
        MaterialSwitch smallSeconds = addSwitch(body, R.string.ultimate_small_seconds,
                R.string.ultimate_small_seconds_summary, repository.isSmallSeconds(), value -> {
                    repository.setSmallSeconds(value);
                    markChanged("small_seconds");
                });
        smallSeconds.setEnabled(repository.isShowSeconds());
        addSwitch(body, R.string.ultimate_portrait_stacked,
                R.string.ultimate_portrait_stacked_summary, repository.isPortraitStacked(), value -> {
                    repository.setPortraitStacked(value);
                    markChanged("portrait_stacked");
                });
    }

    private void addProClassicDateAppearanceSettings(LinearLayout body) {
        addSectionLabel(body, R.string.ultimate_date_appearance_section, 22);
        addActionRow(body, R.string.ultimate_date_size,
                percentSummary(repository.getDateFontScale()),
                R.drawable.ultimate_ic_chevron_right, () -> showScaleDialog(
                        R.string.ultimate_date_size, repository.getDateFontScale(),
                        ClockPreferences.MIN_FONT_SCALE, ClockPreferences.MAX_FONT_SCALE,
                        repository::setDateFontScale, "date_font_scale"));
        addActionRow(body, R.string.ultimate_date_color,
                colorSummary(repository.getDateColor()), R.drawable.ultimate_ic_chevron_right,
                () -> showColorDialog(R.string.ultimate_date_color,
                        R.string.ultimate_date_color_picker, repository.getDateColor(),
                        repository::setDateColor, "date_color"));
        addSwitch(body, R.string.ultimate_date_lunar_dual_line,
                R.string.ultimate_date_lunar_dual_line_summary,
                repository.isDateLunarDualLine(), value -> {
                    repository.setDateLunarDualLine(value);
                    markChanged("date_lunar_dual_line");
                });
    }

    private View backgroundPage() {
        LinearLayout body = pageBody(0);
        addSectionLabel(body, R.string.ultimate_background_source_section);
        String backgroundMode = ultimatePreferences.getBackgroundMode();
        if (UltimateClockPreferences.BACKGROUND_MODE_IMAGE.equals(backgroundMode)
                && !repository.hasImage()) {
            backgroundMode = UltimateClockPreferences.BACKGROUND_MODE_THEME;
            ultimatePreferences.setBackgroundMode(backgroundMode);
        }
        if (UltimateClockPreferences.BACKGROUND_MODE_IMAGE.equals(backgroundMode)) {
            repository.useImage();
        } else {
            repository.useColor();
        }
        int backgroundPosition = UltimateClockPreferences.BACKGROUND_MODE_COLOR.equals(
                backgroundMode) ? 1 : UltimateClockPreferences.BACKGROUND_MODE_IMAGE.equals(
                backgroundMode) ? 2 : 0;
        addSegmented(body, new int[] {R.string.ultimate_background_theme,
                        R.string.ultimate_background_solid_color,
                        R.string.ultimate_background_image}, backgroundPosition, value -> {
                    if (value == 0) {
                        ultimatePreferences.setBackgroundMode(
                                UltimateClockPreferences.BACKGROUND_MODE_THEME);
                        repository.useColor();
                        markChanged("background_mode");
                    } else if (value == 1) {
                        ultimatePreferences.setBackgroundMode(
                                UltimateClockPreferences.BACKGROUND_MODE_COLOR);
                        repository.useColor();
                        markChanged("background_mode");
                    } else if (repository.hasImage()) {
                        ultimatePreferences.setBackgroundMode(
                                UltimateClockPreferences.BACKGROUND_MODE_IMAGE);
                        repository.useImage();
                        markChanged("background_mode");
                    } else {
                        showPage(Page.BACKGROUND);
                        launchImagePicker();
                    }
                });
        addActionRow(body, R.string.ultimate_background_color,
                colorSummary(repository.getCurrentColor()), R.drawable.ultimate_ic_chevron_right,
                () -> showColorDialog(R.string.ultimate_background_color,
                        R.string.ultimate_background_color_picker, repository.getCurrentColor(),
                        color -> {
                            repository.setCurrentColor(color);
                            ultimatePreferences.setBackgroundMode(
                                    UltimateClockPreferences.BACKGROUND_MODE_COLOR);
                        }, "background_color"));
        addActionRow(body, R.string.ultimate_background_import,
                repository.hasImage() ? R.string.ultimate_background_replace_summary
                        : R.string.ultimate_background_import_summary,
                R.drawable.ultimate_ic_chevron_right, this::launchImagePicker);

        addSectionLabel(body, R.string.ultimate_display_section);
        addSwitch(body, R.string.ultimate_show_status_icons,
                R.string.ultimate_show_status_icons_summary, repository.isShowStatusIcons(), value -> {
                    repository.setShowStatusIcons(value);
                    markChanged("show_status_icons");
                    showPage(Page.BACKGROUND);
                });
        View statusScale = addActionRow(body, R.string.ultimate_status_icon_size,
                percentSummary(repository.getStatusIconScale()),
                R.drawable.ultimate_ic_chevron_right, () -> showScaleDialog(
                        R.string.ultimate_status_icon_size, repository.getStatusIconScale(),
                        ClockPreferences.MIN_STATUS_ICON_SCALE,
                        ClockPreferences.MAX_STATUS_ICON_SCALE,
                        repository::setStatusIconScale, "status_icon_scale"));
        setViewTreeEnabled(statusScale, repository.isShowStatusIcons());

        addSectionLabel(body, R.string.ultimate_dimming_section, 22);
        addSwitch(body, R.string.ultimate_dim_background,
                R.string.ultimate_dim_background_summary, repository.isDimBackground(), value -> {
                    repository.setDimBackground(value);
                    markChanged("dim_background");
                });
        addSwitch(body, R.string.ultimate_schedule_dim_background,
                R.string.ultimate_schedule_dim_background_summary,
                repository.isScheduleDimBackground(), value -> {
                    repository.setScheduleDimBackground(value);
                    markChanged("schedule_dim_background");
                    showPage(Page.BACKGROUND);
                });
        View dimStart = addActionRow(body, R.string.ultimate_dim_start,
                formatMinutes(repository.getDimStartMinutes()),
                R.drawable.ultimate_ic_chevron_right, () -> showTimePicker(
                        R.string.ultimate_dim_start, repository.getDimStartMinutes(),
                        repository::setDimStartMinutes, "dim_start", Page.BACKGROUND));
        View dimEnd = addActionRow(body, R.string.ultimate_dim_end,
                formatMinutes(repository.getDimEndMinutes()),
                R.drawable.ultimate_ic_chevron_right, () -> showTimePicker(
                        R.string.ultimate_dim_end, repository.getDimEndMinutes(),
                        repository::setDimEndMinutes, "dim_end", Page.BACKGROUND));
        setViewTreeEnabled(dimStart, repository.isScheduleDimBackground());
        setViewTreeEnabled(dimEnd, repository.isScheduleDimBackground());
        return scrollable(body);
    }

    private View timeDatePage() {
        LinearLayout body = pageBody(0);
        addSectionLabel(body, R.string.ultimate_time_section);
        addSwitch(body, R.string.ultimate_use_24_hour,
                R.string.ultimate_use_24_hour_summary, repository.isUse24Hour(), value -> {
                    repository.setUse24Hour(value);
                    markChanged("use_24_hour");
                });
        addSwitch(body, R.string.ultimate_show_seconds,
                R.string.ultimate_show_seconds_summary, repository.isShowSeconds(), value -> {
                    repository.setShowSeconds(value);
                    markChanged("show_seconds");
                    if (STYLE_PRO_CLASSIC.equals(selectedStyleId())) {
                        showPage(Page.TIME_DATE);
                    }
                });
        if (STYLE_PRO_CLASSIC.equals(selectedStyleId())) {
            addProClassicTimeAppearanceSettings(body);
        }

        addSectionLabel(body, R.string.ultimate_time_sync_section, 22);
        addSwitch(body, R.string.ultimate_network_time,
                R.string.ultimate_network_time_summary, repository.isUseNetworkTime(), value -> {
                    repository.setUseNetworkTime(value);
                    markChanged("network_time");
                    showPage(Page.TIME_DATE);
                });
        int[] syncMinutes = {30, 60, 360, 1440};
        String[] syncNames = {
                getString(R.string.ultimate_sync_30_minutes),
                getString(R.string.ultimate_sync_1_hour),
                getString(R.string.ultimate_sync_6_hours),
                getString(R.string.ultimate_sync_1_day)
        };
        int syncIndex = indexOfValue(syncMinutes, repository.getSyncIntervalMinutes());
        View syncRow = addActionRow(body, R.string.ultimate_sync_interval,
                syncNames[syncIndex], R.drawable.ultimate_ic_chevron_right,
                () -> showSingleChoiceDialog(R.string.ultimate_sync_interval, syncNames,
                        indexOfValue(syncMinutes, repository.getSyncIntervalMinutes()), value ->
                                repository.setSyncIntervalMinutes(syncMinutes[value]),
                        "sync_interval", true));
        setViewTreeEnabled(syncRow, repository.isUseNetworkTime());
        String[] regionNames = getResources().getStringArray(R.array.region_names);
        int regionIndex = RegionTimeZones.indexOfZoneId(repository.getTimeZoneId());
        addActionRow(body, R.string.ultimate_time_zone, regionNames[regionIndex],
                R.drawable.ultimate_ic_chevron_right, () -> showSingleChoiceDialog(
                        R.string.ultimate_time_zone, regionNames,
                        RegionTimeZones.indexOfZoneId(repository.getTimeZoneId()), value ->
                                repository.setTimeZoneId(RegionTimeZones.ZONE_IDS[value]),
                        "time_zone", true));

        addSectionLabel(body, R.string.ultimate_date_section, 22);
        addSwitch(body, R.string.ultimate_show_lunar,
                R.string.ultimate_show_lunar_summary, repository.isShowLunar(), value -> {
                    repository.setShowLunar(value);
                    markChanged("show_lunar");
                });
        addActionRow(body, R.string.ultimate_date_format, currentDatePattern(),
                R.drawable.ultimate_ic_chevron_right, this::showDatePatternEditor);
        if (STYLE_PRO_CLASSIC.equals(selectedStyleId())) {
            addProClassicDateAppearanceSettings(body);
        }
        return scrollable(body);
    }

    private View weatherPage() {
        LinearLayout body = pageBody(0);
        addSectionLabel(body, R.string.ultimate_weather_section);
        MaterialSwitch enabled = addSwitch(body, R.string.ultimate_weather_enabled,
                R.string.ultimate_weather_enabled_summary, repository.isWeatherEnabled(), value -> {
                    repository.setWeatherEnabled(value);
                    markChanged("weather_enabled");
                });

        LinearLayout weatherControls = new LinearLayout(this);
        weatherControls.setOrientation(LinearLayout.VERTICAL);
        MaterialSwitch detailed = addSwitch(weatherControls, R.string.ultimate_weather_detailed,
                R.string.ultimate_weather_detailed_summary, repository.isWeatherDetailed(), value -> {
                    repository.setWeatherDetailed(value);
                    markChanged("weather_detailed");
                });

        addSectionLabel(weatherControls, R.string.ultimate_custom_message_section, 22);
        String message = repository.getCustomMessage();
        addActionRow(weatherControls, R.string.ultimate_custom_message,
                message.length() == 0 ? getString(R.string.ultimate_custom_message_empty) : message,
                R.drawable.ultimate_ic_chevron_right, this::showCustomMessageEditor);

        addSectionLabel(weatherControls, R.string.ultimate_weather_location_section, 22);
        boolean manualLocation = ClockPreferences.WEATHER_LOCATION_MANUAL.equals(
                repository.getWeatherLocationMode());
        addSegmented(weatherControls, new int[] {R.string.ultimate_weather_location_auto,
                        R.string.ultimate_weather_location_manual}, manualLocation ? 1 : 0, value -> {
                    repository.setWeatherLocationMode(value == 1
                            ? ClockPreferences.WEATHER_LOCATION_MANUAL
                            : ClockPreferences.WEATHER_LOCATION_AUTOMATIC);
                    markChanged("weather_location_mode");
                    showPage(Page.WEATHER);
                });
        View locationRow = addActionRow(weatherControls,
                R.string.ultimate_weather_choose_location, weatherLocationSummary(),
                R.drawable.ultimate_ic_chevron_right, () -> WeatherLocationChooser.show(this,
                        repository.getWeatherProvince(), repository.getWeatherCity(),
                        repository.getWeatherDistrict(), new WeatherLocationChooser.Listener() {
                            @Override
                            public void onLocationSelected(
                                    WeatherLocationCatalog.LocationEntry location) {
                                repository.setManualWeatherLocation(location.locationId,
                                        location.province, location.city, location.district,
                                        location.latitude, location.longitude);
                                repository.setWeatherLocationMode(
                                        ClockPreferences.WEATHER_LOCATION_MANUAL);
                                markChanged("weather_location");
                                showPage(Page.WEATHER);
                            }
                        }));

        addSectionLabel(weatherControls, R.string.ultimate_weather_refresh_section, 22);
        int[] weatherMinutes = {10, 30, 60, 180, 360, 720};
        String[] weatherIntervals = {
                getString(R.string.ultimate_weather_10_minutes),
                getString(R.string.ultimate_weather_30_minutes),
                getString(R.string.ultimate_weather_1_hour),
                getString(R.string.ultimate_weather_3_hours),
                getString(R.string.ultimate_weather_6_hours),
                getString(R.string.ultimate_weather_12_hours)
        };
        int intervalIndex = indexOfValue(weatherMinutes, repository.getWeatherIntervalMinutes());
        addActionRow(weatherControls, R.string.ultimate_weather_refresh_interval,
                weatherIntervals[intervalIndex], R.drawable.ultimate_ic_chevron_right,
                () -> showSingleChoiceDialog(R.string.ultimate_weather_refresh_interval,
                        weatherIntervals,
                        indexOfValue(weatherMinutes, repository.getWeatherIntervalMinutes()),
                        value -> repository.setWeatherIntervalMinutes(weatherMinutes[value]),
                        "weather_interval", true));

        addSectionLabel(weatherControls, R.string.ultimate_weather_icon_section, 22);
        addSwitch(weatherControls, R.string.ultimate_weather_icon_fill,
                R.string.ultimate_weather_icon_fill_summary, repository.isWeatherIconFill(),
                value -> {
                    repository.setWeatherIconFill(value);
                    markChanged("weather_icon_fill");
                });
        addSwitch(weatherControls, R.string.ultimate_weather_dynamic_color,
                R.string.ultimate_weather_dynamic_color_summary,
                repository.isWeatherIconDynamicColor(), value -> {
                    repository.setWeatherIconDynamicColor(value);
                    markChanged("weather_icon_dynamic_color");
                });
        body.addView(weatherControls, wrapParams());

        enabled.setOnCheckedChangeListener((button, value) -> {
            repository.setWeatherEnabled(value);
            markChanged("weather_enabled");
            showPage(Page.WEATHER);
        });
        setViewTreeEnabled(weatherControls, enabled.isChecked());
        setViewTreeEnabled(locationRow, enabled.isChecked() && manualLocation);
        return scrollable(body);
    }

    private View calendarPage() {
        LinearLayout body = pageBody(0);
        addSectionLabel(body, R.string.ultimate_calendar_week_section);
        int weekStart = repository.getCalendarWeekStart()
                == ClockPreferences.CALENDAR_WEEK_START_MONDAY ? 1 : 0;
        addSegmented(body, new int[] {R.string.ultimate_calendar_sunday,
                        R.string.ultimate_calendar_monday}, weekStart, value -> {
                    repository.setCalendarWeekStart(value == 1
                            ? ClockPreferences.CALENDAR_WEEK_START_MONDAY
                            : ClockPreferences.CALENDAR_WEEK_START_SUNDAY);
                    markChanged("calendar_week_start");
                });
        addSwitch(body, R.string.ultimate_calendar_highlight_weekends,
                R.string.ultimate_calendar_highlight_weekends_summary,
                repository.isCalendarHighlightWeekends(), value -> {
                    repository.setCalendarHighlightWeekends(value);
                    markChanged("calendar_highlight_weekends");
                });
        return scrollable(body);
    }

    private View chimePage() {
        LinearLayout body = pageBody(0);
        addSectionLabel(body, R.string.ultimate_chime_cues_section);
        addSwitch(body, R.string.ultimate_hourly_chime,
                R.string.ultimate_hourly_chime_summary, repository.isHourlyChimeEnabled(), value -> {
                    repository.setHourlyChimeEnabled(value);
                    markChanged("hourly_chime");
                });
        addSwitch(body, R.string.ultimate_half_hour_chime,
                R.string.ultimate_half_hour_chime_summary,
                repository.isHalfHourChimeEnabled(), value -> {
                    repository.setHalfHourChimeEnabled(value);
                    markChanged("half_hour_chime");
                });

        addSectionLabel(body, R.string.ultimate_chime_quiet_section, 22);
        addSwitch(body, R.string.ultimate_chime_quiet_hours,
                R.string.ultimate_chime_quiet_hours_summary,
                repository.isHourlyChimeQuietEnabled(), value -> {
                    repository.setHourlyChimeQuietEnabled(value);
                    markChanged("chime_quiet_hours");
                    showPage(Page.CHIME);
                });
        View quietStart = addActionRow(body, R.string.ultimate_chime_quiet_start,
                formatMinutes(repository.getHourlyChimeQuietStart()),
                R.drawable.ultimate_ic_chevron_right, () -> showTimePicker(
                        R.string.ultimate_chime_quiet_start,
                        repository.getHourlyChimeQuietStart(),
                        repository::setHourlyChimeQuietStart,
                        "chime_quiet_start", Page.CHIME));
        View quietEnd = addActionRow(body, R.string.ultimate_chime_quiet_end,
                formatMinutes(repository.getHourlyChimeQuietEnd()),
                R.drawable.ultimate_ic_chevron_right, () -> showTimePicker(
                        R.string.ultimate_chime_quiet_end,
                        repository.getHourlyChimeQuietEnd(),
                        repository::setHourlyChimeQuietEnd,
                        "chime_quiet_end", Page.CHIME));
        setViewTreeEnabled(quietStart, repository.isHourlyChimeQuietEnabled());
        setViewTreeEnabled(quietEnd, repository.isHourlyChimeQuietEnabled());
        return scrollable(body);
    }

    private View systemPage() {
        LinearLayout body = pageBody(0);
        addSectionLabel(body, R.string.ultimate_orientation_section);
        int orientation = repository.getScreenOrientation();
        int orientationPosition = orientation == ClockPreferences.ORIENTATION_PORTRAIT ? 1
                : orientation == ClockPreferences.ORIENTATION_LANDSCAPE ? 2 : 0;
        addSegmented(body, new int[] {R.string.ultimate_orientation_follow,
                        R.string.ultimate_orientation_portrait,
                        R.string.ultimate_orientation_landscape}, orientationPosition, value -> {
                    int mode = value == 1 ? ClockPreferences.ORIENTATION_PORTRAIT
                            : value == 2 ? ClockPreferences.ORIENTATION_LANDSCAPE
                            : ClockPreferences.ORIENTATION_FOLLOW_SYSTEM;
                    repository.setScreenOrientation(mode);
                    markChanged("screen_orientation");
                });
        addSectionLabel(body, R.string.ultimate_language_section, 22);
        String language = repository.getClockLanguage();
        int languagePosition = ClockPreferences.LANGUAGE_TRADITIONAL.equals(language) ? 1
                : ClockPreferences.LANGUAGE_ENGLISH.equals(language) ? 2 : 0;
        addSegmented(body, new int[] {R.string.ultimate_language_simplified,
                        R.string.ultimate_language_traditional, R.string.ultimate_language_english},
                languagePosition, value -> {
                    String selected = value == 1 ? ClockPreferences.LANGUAGE_TRADITIONAL
                            : value == 2 ? ClockPreferences.LANGUAGE_ENGLISH
                            : ClockPreferences.LANGUAGE_SIMPLIFIED;
                    repository.setClockLanguage(selected);
                    markChanged(CHANGE_SOURCE_CLOCK_LANGUAGE);
                });
        addSwitch(body, R.string.ultimate_auto_start,
                R.string.ultimate_auto_start_summary, repository.isAutoStart(), value -> {
                    repository.setAutoStart(value);
                    AutoStartManager.setEnabled(this, value);
                    if (value) AutoStartManager.requestHomeRole(this);
                    markChanged("auto_start");
                });
        return scrollable(body);
    }

    private LinearLayout pageBody(int introRes) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        currentPageBody = body;
        int horizontalPadding = getResources().getDimensionPixelSize(
                R.dimen.ultimate_settings_page_padding_horizontal);
        body.setPadding(horizontalPadding, dp(12), horizontalPadding, dp(32));
        if (introRes != 0) {
            TextView intro = label(introRes, 14, false);
            intro.setLineSpacing(0f, 1.15f);
            body.addView(intro, wrapParams());
        }
        return body;
    }

    private View scrollable(LinearLayout body) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private void addSectionLabel(LinearLayout parent, int textRes) {
        addSectionLabel(parent, textRes, 18);
    }

    private void addSectionLabel(LinearLayout parent, int textRes, int topMarginDp) {
        TextView title = label(textRes, 13, true);
        title.setTextColor(primaryColor());
        title.setAllCaps(false);
        title.setContentDescription(getString(textRes));
        title.setAccessibilityHeading(true);
        parent.addView(title, topMargin(wrapParams(), dp(topMarginDp)));
    }

    private void addNavigationSearch(LinearLayout parent) {
        int radius = getResources().getDimensionPixelSize(
                R.dimen.ultimate_settings_home_search_radius);
        TextInputLayout searchBox = new TextInputLayout(this);
        searchBox.setHintEnabled(false);
        searchBox.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_NONE);
        GradientDrawable searchBackground = roundedRectangle(
                settingsHomeContainerColor(), radius);
        searchBackground.setStroke(dp(1), settingsHomeOutlineColor());
        searchBox.setBackground(searchBackground);
        searchBox.setClipToOutline(true);
        searchBox.setStartIconDrawable(R.drawable.ultimate_ic_search);
        searchBox.setStartIconTintList(ColorStateList.valueOf(onSurfaceVariantColor()));
        searchBox.setStartIconCheckable(false);
        searchBox.setStartIconContentDescription((CharSequence) null);
        searchBox.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        searchBox.setEndIconTintList(ColorStateList.valueOf(onSurfaceVariantColor()));

        TextInputEditText searchInput = new TextInputEditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setTextColor(onSurfaceColor());
        searchInput.setHintTextColor(onSurfaceVariantColor());
        searchInput.setHint(R.string.ultimate_settings_search_hint);
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setGravity(Gravity.CENTER_VERTICAL);
        searchInput.setTypeface(Typeface.DEFAULT);
        searchInput.setBackground(null);
        searchInput.setPadding(0, 0, dp(4), 0);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count,
                    int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before,
                    int count) {}
            @Override public void afterTextChanged(Editable text) {
                applyNavigationFilter(text == null ? "" : text.toString());
            }
        });
        searchBox.addView(searchInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(searchBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(
                        R.dimen.ultimate_settings_home_search_height)));
    }

    private NavigationGroup addNavigationGroup(LinearLayout parent, int topMarginRes) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackground(roundedRectangle(settingsHomeContainerColor(),
                getResources().getDimension(
                        R.dimen.ultimate_settings_home_group_radius)));
        container.setClipToOutline(true);
        NavigationGroup group = new NavigationGroup(container);
        navigationGroups.add(group);
        LinearLayout.LayoutParams params = wrapParams();
        params.topMargin = getResources().getDimensionPixelSize(topMarginRes);
        parent.addView(container, params);
        return group;
    }

    private void addCategory(NavigationGroup group, Page page, int titleRes, int summaryRes,
            int iconRes, Runnable action) {
        if (!group.items.isEmpty()) {
            NavigationItem previous = group.items.get(group.items.size() - 1);
            View separator = new View(this);
            separator.setBackgroundColor(settingsHomeCanvasColor());
            group.container.addView(separator, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelSize(
                            R.dimen.ultimate_settings_home_divider_height)));
            previous.separatorAfter = separator;
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPaddingRelative(dp(16), dp(4), dp(16), dp(4));
        row.setFocusable(true);
        row.setClickable(true);
        ImageView leadingIcon = new ImageView(this);
        leadingIcon.setImageResource(iconRes);
        leadingIcon.setImageTintList(ColorStateList.valueOf(onSurfaceVariantColor()));
        leadingIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(leadingIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(titleRes, 16, false);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView summary = label(summaryRes, 12, false);
        summary.setMaxLines(1);
        summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.addView(title, wrapParams());
        text.addView(summary, topMargin(wrapParams(), dp(1)));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dp(12));
        row.addView(text, textParams);
        row.setContentDescription(getString(titleRes) + ", " + getString(summaryRes));
        row.setOnClickListener(view -> action.run());
        group.container.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(
                        R.dimen.ultimate_settings_home_row_height)));
        NavigationItem item = new NavigationItem(page, group, row, title, summary, leadingIcon);
        group.items.add(item);
        navigationItems.put(page, item);
        updateNavigationGroupPositions(group);
    }

    private void applyNavigationFilter(String query) {
        String normalized = query == null ? ""
                : query.trim().toLowerCase(Locale.getDefault());
        boolean anyVisible = false;
        boolean firstVisibleGroup = true;
        for (NavigationGroup group : navigationGroups) {
            List<NavigationItem> visibleItems = new ArrayList<>();
            for (NavigationItem item : group.items) {
                boolean visible = normalized.isEmpty()
                        || item.title.getText().toString().toLowerCase(Locale.getDefault())
                                .contains(normalized)
                        || item.summary.getText().toString().toLowerCase(Locale.getDefault())
                                .contains(normalized);
                item.row.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (item.separatorAfter != null) item.separatorAfter.setVisibility(View.GONE);
                if (visible) visibleItems.add(item);
            }
            group.container.setVisibility(visibleItems.isEmpty() ? View.GONE : View.VISIBLE);
            if (!visibleItems.isEmpty()) {
                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) group.container.getLayoutParams();
                params.topMargin = getResources().getDimensionPixelSize(firstVisibleGroup
                        ? R.dimen.ultimate_settings_home_search_group_gap
                        : R.dimen.ultimate_settings_home_group_gap);
                group.container.setLayoutParams(params);
                firstVisibleGroup = false;
            }
            for (int index = 0; index < visibleItems.size(); index++) {
                NavigationItem item = visibleItems.get(index);
                item.position = rowPosition(index, visibleItems.size());
                if (item.separatorAfter != null && index < visibleItems.size() - 1) {
                    item.separatorAfter.setVisibility(View.VISIBLE);
                }
            }
            anyVisible |= !visibleItems.isEmpty();
        }
        if (navigationEmptyState != null) {
            navigationEmptyState.setVisibility(anyVisible ? View.GONE : View.VISIBLE);
        }
        updateNavigationSelection(navigationSelectedPage, navigationSelectionEmbedded);
    }

    private void updateNavigationGroupPositions(NavigationGroup group) {
        for (int index = 0; index < group.items.size(); index++) {
            group.items.get(index).position = rowPosition(index, group.items.size());
        }
    }

    private static RowPosition rowPosition(int index, int count) {
        if (count <= 1) return RowPosition.SINGLE;
        if (index == 0) return RowPosition.FIRST;
        if (index == count - 1) return RowPosition.LAST;
        return RowPosition.MIDDLE;
    }

    private void updateNavigationSelection(Page selectedPage, boolean embedded) {
        navigationSelectedPage = selectedPage;
        navigationSelectionEmbedded = embedded;
        for (Page page : navigationItems.keySet()) {
            NavigationItem item = navigationItems.get(page);
            if (item == null) continue;
            boolean selected = page == selectedPage;
            item.row.setSelected(selected);
            item.row.setActivated(selected);
            item.row.setBackground(navigationBackground(selected, item.position));
            item.title.setTextColor(onSurfaceColor());
            item.summary.setTextColor(onSurfaceVariantColor());
            item.leadingIcon.setImageTintList(ColorStateList.valueOf(onSurfaceVariantColor()));
            item.row.setStateDescription(selected
                    ? getString(R.string.ultimate_settings_selected_state) : null);
        }
    }

    private RippleDrawable navigationBackground(boolean selected, RowPosition position) {
        float[] radii = navigationCornerRadii(position);
        GradientDrawable content = roundedRectangle(
                selected ? settingsHomeSelectedColor() : Color.TRANSPARENT, radii);
        GradientDrawable mask = roundedRectangle(Color.WHITE, radii);
        ColorStateList ripple = ColorStateList.valueOf(
                withAlpha(onSurfaceVariantColor(), 0.18f));
        return new RippleDrawable(ripple, content, mask);
    }

    private float[] navigationCornerRadii(RowPosition position) {
        float radius = getResources().getDimension(
                R.dimen.ultimate_settings_home_group_radius);
        switch (position) {
            case FIRST:
                return new float[] {radius, radius, radius, radius, 0f, 0f, 0f, 0f};
            case LAST:
                return new float[] {0f, 0f, 0f, 0f, radius, radius, radius, radius};
            case MIDDLE:
                return new float[8];
            case SINGLE:
            default:
                return new float[] {radius, radius, radius, radius,
                        radius, radius, radius, radius};
        }
    }

    private GradientDrawable roundedRectangle(int color, float radius) {
        return roundedRectangle(color, new float[] {radius, radius, radius, radius,
                radius, radius, radius, radius});
    }

    private GradientDrawable roundedRectangle(int color, float[] radii) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(color);
        shape.setCornerRadii(radii);
        return shape;
    }

    private View addActionRow(LinearLayout parent, int titleRes, int summaryRes, int iconRes,
            Runnable action) {
        return addActionRow(parent, getString(titleRes), getString(summaryRes), iconRes, action);
    }

    private View addActionRow(LinearLayout parent, int titleRes, CharSequence summary, int iconRes,
            Runnable action) {
        return addActionRow(parent, getString(titleRes), summary, iconRes, action);
    }

    private View addActionRow(LinearLayout parent, CharSequence titleValue,
            CharSequence summaryValue, int iconRes, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(64));
        row.setPadding(dp(4), dp(6), 0, dp(6));
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackgroundResource(selectableItemBackground());
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView title = label(titleValue, 16, true);
        TextView summary = label(summaryValue, 13, false);
        summary.setMaxLines(2);
        summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.addView(title, wrapParams());
        text.addView(summary, topMargin(wrapParams(), dp(2)));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (iconRes != 0) {
            ImageView chevron = new ImageView(this);
            chevron.setImageResource(iconRes);
            chevron.setImageTintList(android.content.res.ColorStateList.valueOf(onSurfaceVariantColor()));
            chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            chevron.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.addView(chevron, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }
        row.setContentDescription(titleValue + ", " + summaryValue);
        row.setOnClickListener(view -> action.run());
        parent.addView(row, wrapParams());
        addDivider(parent);
        return row;
    }

    private MaterialSwitch addSwitch(LinearLayout parent, int titleRes, int summaryRes,
            boolean checked, BooleanChange change) {
        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setPadding(0, dp(4), 0, dp(4));
        MaterialSwitch control = new MaterialSwitch(this);
        control.setText(titleRes);
        control.setTextSize(16);
        control.setMinHeight(dp(48));
        control.setChecked(checked);
        control.setContentDescription(getString(titleRes));
        holder.addView(control, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        TextView summary = label(summaryRes, 13, false);
        summary.setMaxLines(3);
        summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        holder.addView(summary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        control.setOnCheckedChangeListener((button, value) -> change.apply(value));
        parent.addView(holder, wrapParams());
        addDivider(parent);
        return control;
    }

    private MaterialButtonToggleGroup addSegmented(LinearLayout parent, int[] labels, int checked,
            IntChange change) {
        MaterialButtonToggleGroup group = new MaterialButtonToggleGroup(this);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setContentDescription(getString(R.string.ultimate_page_accessibility));
        int[] ids = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            MaterialButton button = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            ids[i] = View.generateViewId();
            button.setId(ids[i]);
            button.setText(labels[i]);
            button.setTextSize(13);
            button.setAllCaps(false);
            button.setMaxLines(1);
            button.setEllipsize(android.text.TextUtils.TruncateAt.END);
            button.setInsetTop(0);
            button.setInsetBottom(0);
            button.setMinHeight(dp(48));
            button.setContentDescription(getString(labels[i]));
            group.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        int safeChecked = checked < 0 || checked >= ids.length ? 0 : checked;
        group.check(ids[safeChecked]);
        group.addOnButtonCheckedListener((toggleGroup, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == checkedId) {
                    change.apply(i);
                    return;
                }
            }
        });
        parent.addView(group, topMargin(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)), dp(6)));
        addDivider(parent);
        return group;
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(withAlpha(onSurfaceVariantColor(), 0.22f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        lp.leftMargin = dp(4);
        parent.addView(divider, lp);
    }

    private TextView label(int textRes, int sizeSp, boolean strong) {
        return label(getString(textRes), sizeSp, strong);
    }

    private TextView label(CharSequence value, int sizeSp, boolean strong) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(onSurfaceColor());
        text.setTypeface(strong ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        text.setLineSpacing(0f, 1.12f);
        return text;
    }

    private void showSingleChoiceDialog(int titleRes, String[] items, int selected,
            IntChange change, String source, boolean refreshPage) {
        int safeSelected = selected < 0 || selected >= items.length ? 0 : selected;
        Page pageToRefresh = currentPage;
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setSingleChoiceItems(items, safeSelected, (dialog, which) -> {
                    change.apply(which);
                    markChanged(source);
                    dialog.dismiss();
                    if (refreshPage) showPage(pageToRefresh);
                })
                .setNegativeButton(R.string.ultimate_cancel, null)
                .show();
    }

    private void showScaleDialog(int titleRes, float current, float minimum, float maximum,
            FloatChange change, String source) {
        int minPercent = Math.round(minimum * 100f);
        int maxPercent = Math.round(maximum * 100f);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(8), dp(24), 0);
        TextView value = label(percentSummary(current), 16, true);
        SeekBar slider = new SeekBar(this);
        slider.setMax(maxPercent - minPercent);
        slider.setProgress(Math.max(0, Math.min(maxPercent - minPercent,
                Math.round(current * 100f) - minPercent)));
        slider.setContentDescription(getString(titleRes));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText(getString(R.string.ultimate_percent_value, progress + minPercent));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        content.addView(value, wrapParams());
        content.addView(slider, topMargin(wrapParams(), dp(8)));
        Page pageToRefresh = currentPage;
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setView(content)
                .setNegativeButton(R.string.ultimate_cancel, null)
                .setPositiveButton(R.string.ultimate_apply, (dialog, which) -> {
                    change.apply((slider.getProgress() + minPercent) / 100f);
                    markChanged(source);
                    showPage(pageToRefresh);
                })
                .show();
    }

    private void showColorDialog(int titleRes, int accessibilityRes, int current,
            IntChange change, String source) {
        ColorPickerView picker = new ColorPickerView(this);
        picker.setColor(current);
        picker.setAccessibilityLabel(getString(accessibilityRes));
        FrameLayout holder = new FrameLayout(this);
        holder.setPadding(dp(24), dp(8), dp(24), 0);
        holder.addView(picker, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(230)));
        Page pageToRefresh = currentPage;
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setView(holder)
                .setNegativeButton(R.string.ultimate_cancel, null)
                .setPositiveButton(R.string.ultimate_apply, (dialog, which) -> {
                    change.apply(picker.getColor());
                    markChanged(source);
                    showPage(pageToRefresh);
                })
                .show();
    }

    private void showTimePicker(int titleRes, int initialMinutes, IntChange change,
            String source, Page pageToRefresh) {
        int normalized = ((initialMinutes % (24 * 60)) + 24 * 60) % (24 * 60);
        TimePickerDialog picker = new TimePickerDialog(this, (view, hour, minute) -> {
            change.apply(hour * 60 + minute);
            markChanged(source);
            showPage(pageToRefresh);
        }, normalized / 60, normalized % 60, true);
        picker.setTitle(titleRes);
        picker.show();
    }

    private void showDatePatternEditor() {
        boolean english = ClockPreferences.LANGUAGE_ENGLISH.equals(repository.getClockLanguage());
        String current = english ? repository.getDatePatternEn() : repository.getDatePatternCn();
        EditText input = dialogTextInput(current, DateFormatter.MAX_PATTERN_LENGTH);
        input.setHint(english ? DateFormatter.DEFAULT_PATTERN_EN : DateFormatter.DEFAULT_PATTERN_CN);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ultimate_date_format)
                .setMessage(R.string.ultimate_date_format_help)
                .setView(input)
                .setNegativeButton(R.string.ultimate_cancel, null)
                .setNeutralButton(R.string.ultimate_restore_default, (ignored, which) -> {
                    String value = english ? DateFormatter.DEFAULT_PATTERN_EN
                            : DateFormatter.DEFAULT_PATTERN_CN;
                    saveDatePattern(english, value);
                })
                .setPositiveButton(R.string.ultimate_apply, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String value = input.getText().toString().trim();
                    if (!DateFormatter.isValidPattern(value)) {
                        input.setError(getString(R.string.ultimate_date_format_invalid));
                        return;
                    }
                    saveDatePattern(english, value);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void saveDatePattern(boolean english, String value) {
        if (english) {
            repository.setDatePatternEn(value);
        } else {
            repository.setDatePatternCn(value);
        }
        repository.setDateFormatState(english, "", "", true, value);
        markChanged("date_format");
        showPage(Page.TIME_DATE);
    }

    private void showCustomMessageEditor() {
        EditText input = dialogTextInput(repository.getCustomMessage(),
                ClockPreferences.MAX_CUSTOM_MESSAGE_LENGTH);
        input.setHint(R.string.ultimate_custom_message_hint);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ultimate_custom_message)
                .setView(input)
                .setNegativeButton(R.string.ultimate_cancel, null)
                .setPositiveButton(R.string.ultimate_apply, (dialog, which) -> {
                    repository.setCustomMessage(input.getText().toString());
                    markChanged("custom_message");
                    showPage(Page.WEATHER);
                })
                .show();
    }

    private EditText dialogTextInput(String value, int maxLength) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        input.setText(value);
        input.setSelection(input.length());
        return input;
    }

    private String currentDatePattern() {
        return ClockPreferences.LANGUAGE_ENGLISH.equals(repository.getClockLanguage())
                ? repository.getDatePatternEn() : repository.getDatePatternCn();
    }

    private String weatherLocationSummary() {
        String province = repository.getWeatherProvince();
        String city = repository.getWeatherCity();
        String district = repository.getWeatherDistrict();
        StringBuilder summary = new StringBuilder();
        appendLocationPart(summary, province);
        appendLocationPart(summary, city);
        appendLocationPart(summary, district);
        return summary.length() == 0
                ? getString(R.string.ultimate_weather_location_not_set) : summary.toString();
    }

    private static void appendLocationPart(StringBuilder value, String part) {
        if (part == null || part.length() == 0) return;
        if (value.length() > 0) value.append(" / ");
        value.append(part);
    }

    private String percentSummary(float scale) {
        return getString(R.string.ultimate_percent_value, Math.round(scale * 100f));
    }

    private static String colorSummary(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }

    private static String formatMinutes(int minutes) {
        int normalized = ((minutes % (24 * 60)) + 24 * 60) % (24 * 60);
        return String.format(Locale.getDefault(), "%02d:%02d", normalized / 60, normalized % 60);
    }

    private static int indexOfValue(int[] values, int value) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == value) return index;
        }
        return 0;
    }

    private static int transitionIndex(String transition) {
        if (ClockPreferences.TRANSITION_SLIDE_UP.equals(transition)) return 1;
        if (ClockPreferences.TRANSITION_SLIDE_DOWN.equals(transition)) return 2;
        if (ClockPreferences.TRANSITION_SCALE.equals(transition)) return 3;
        if (ClockPreferences.TRANSITION_FLIP.equals(transition)) return 4;
        return 0;
    }

    private static String transitionForIndex(int index) {
        if (index == 1) return ClockPreferences.TRANSITION_SLIDE_UP;
        if (index == 2) return ClockPreferences.TRANSITION_SLIDE_DOWN;
        if (index == 3) return ClockPreferences.TRANSITION_SCALE;
        if (index == 4) return ClockPreferences.TRANSITION_FLIP;
        return ClockPreferences.TRANSITION_FADE;
    }

    private static void setViewTreeEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.45f);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setDescendantsEnabled(group.getChildAt(index), enabled);
            }
        }
    }

    private static void setDescendantsEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setDescendantsEnabled(group.getChildAt(index), enabled);
            }
        }
    }

    private void buildStyleSpecs() {
        if (!styleSpecs.isEmpty()) return;
        for (ClockStyle style : styleRegistry.getStyles()) {
            if (!style.getMetadata().supportsApi(android.os.Build.VERSION.SDK_INT)) continue;
            String id = style.getMetadata().getId();
            int nameRes = styleNameResource(id);
            int summaryRes = styleSummaryResource(id);
            String name = nameRes == 0 ? style.getMetadata().getName() : getString(nameRes);
            String summary = summaryRes == 0
                    ? style.getMetadata().getDescription() : getString(summaryRes);
            styleSpecs.add(new StyleSpec(style, name, summary));
        }
    }

    private String selectedStyleId() {
        buildStyleSpecs();
        String value = ultimatePreferences.getStyleId();
        return styleRegistry.resolveForApi(value, android.os.Build.VERSION.SDK_INT)
                .getMetadata().getId();
    }

    private void updateStyleCards(String selectedId) {
        for (int i = 0; i < styleCards.size() && i < styleSpecs.size(); i++) {
            MaterialCardView card = styleCards.get(i);
            boolean selected = styleSpecs.get(i).id.equals(selectedId);
            card.setChecked(selected);
            card.setStrokeWidth(dp(selected ? 2 : 1));
            card.setStrokeColor(selected ? primaryColor() : withAlpha(onSurfaceVariantColor(), 0.55f));
            card.setCardBackgroundColor(surfaceContainerColor());
            card.setStateDescription(selected ? getString(R.string.ultimate_style_selected,
                    styleSpecs.get(i).name) : null);
        }
    }

    private static int styleNameResource(String id) {
        if (STYLE_PRO_CLASSIC.equals(id)) return R.string.ultimate_style_pro_classic_name;
        if (STYLE_GLASS_ATELIER.equals(id)) return R.string.ultimate_style_glass_name;
        if (STYLE_NOIR_INSTRUMENT.equals(id)) return R.string.ultimate_style_noir_name;
        if (STYLE_PAPER_STATION.equals(id)) return R.string.ultimate_style_paper_name;
        if (STYLE_ORBIT_NEON.equals(id)) return R.string.ultimate_style_orbit_name;
        if (STYLE_DIGITAL_GRID.equals(id)) return R.string.ultimate_style_grid_name;
        if (STYLE_TYPOGRAPHIC.equals(id)) return R.string.ultimate_style_typographic_name;
        return 0;
    }

    private static int styleSummaryResource(String id) {
        if (STYLE_PRO_CLASSIC.equals(id)) return R.string.ultimate_style_pro_classic_summary;
        if (STYLE_GLASS_ATELIER.equals(id)) return R.string.ultimate_style_glass_summary;
        if (STYLE_NOIR_INSTRUMENT.equals(id)) return R.string.ultimate_style_noir_summary;
        if (STYLE_PAPER_STATION.equals(id)) return R.string.ultimate_style_paper_summary;
        if (STYLE_ORBIT_NEON.equals(id)) return R.string.ultimate_style_orbit_summary;
        if (STYLE_DIGITAL_GRID.equals(id)) return R.string.ultimate_style_grid_summary;
        if (STYLE_TYPOGRAPHIC.equals(id)) return R.string.ultimate_style_typographic_summary;
        return 0;
    }

    private void launchImagePicker() {
        Intent picker;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            picker = new Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*");
        } else {
            picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("image/*");
        }
        try {
            startActivityForResult(picker, REQUEST_BACKGROUND_IMAGE);
        } catch (RuntimeException ignored) {
            // A device without a document provider simply leaves the previous background intact.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SUBPAGE) {
            if (resultCode == RESULT_OK) {
                settingsChanged = true;
                if (data != null) {
                    String source = data.getStringExtra(EXTRA_CHANGE_SOURCE);
                    lastChangeSource = source == null ? "" : source;
                }
            }
            syncExternalChanges();
            if (!isActivityEmbedded()) selectedSubPage = Page.STYLE;
            updateEmbeddingChrome();
            if (settingsChanged) publishResult();
            return;
        }
        if (requestCode != REQUEST_BACKGROUND_IMAGE || resultCode != RESULT_OK || data == null
                || data.getData() == null) return;
        importBackground(data.getData());
    }

    private void importBackground(Uri uri) {
        imageExecutor.execute(() -> {
            boolean success = false;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input != null) {
                    int longSide = Math.max(getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels);
                    repository.saveImage(input, longSide);
                    success = true;
                }
            } catch (IOException | SecurityException ignored) {
                success = false;
            }
            final boolean imported = success;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (imported) {
                    ultimatePreferences.setBackgroundMode(
                            UltimateClockPreferences.BACKGROUND_MODE_IMAGE);
                    repository.useImage();
                    markChanged("background_image");
                    showPage(Page.BACKGROUND);
                    Toast.makeText(this, R.string.ultimate_background_imported,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.ultimate_background_import_failed,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void markChanged(String source) {
        UltimateSettingsChangeTracker.record(this, source);
        settingsChanged = true;
        lastChangeSource = source == null ? "" : source;
        publishResult();
    }

    private void syncExternalChanges() {
        if (UltimateSettingsChangeTracker.revision(this) == settingsRevisionAtOpen) return;
        settingsChanged = true;
        String source = UltimateSettingsChangeTracker.lastSource(this);
        lastChangeSource = source == null ? "" : source;
    }

    private void publishResult() {
        Intent result = new Intent()
                .putExtra(EXTRA_SETTINGS_CHANGED, true)
                .putExtra(EXTRA_STYLE_ID, selectedStyleId())
                .putExtra(EXTRA_CHANGE_SOURCE, lastChangeSource);
        setResult(RESULT_OK, result);
    }

    private int selectableItemBackground() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return value.resourceId;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(LinearLayout.LayoutParams params, int margin) {
        params.topMargin = margin;
        return params;
    }

    private int surfaceColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface,
                Color.rgb(20, 20, 22));
    }

    private int surfaceContainerColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant,
                Color.rgb(38, 38, 42));
    }

    private int settingsHomeCanvasColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface,
                Color.rgb(16, 20, 24));
    }

    private int settingsHomeContainerColor() {
        return MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceBright,
                Color.rgb(54, 57, 62));
    }

    private int settingsHomeSelectedColor() {
        return MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainer,
                Color.rgb(29, 32, 36));
    }

    private int settingsHomeOutlineColor() {
        return blendColors(settingsHomeContainerColor(), onSurfaceVariantColor(), 0.10f);
    }

    private int onSurfaceColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface,
                Color.WHITE);
    }

    private int onSurfaceVariantColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                Color.LTGRAY);
    }

    private int primaryColor() {
        return MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary,
                Color.rgb(120, 190, 255));
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha), Color.red(color),
                Color.green(color), Color.blue(color));
    }

    private static int blendColors(int background, int foreground, float foregroundRatio) {
        float ratio = Math.max(0f, Math.min(1f, foregroundRatio));
        return Color.rgb(
                Math.round(Color.red(background)
                        + (Color.red(foreground) - Color.red(background)) * ratio),
                Math.round(Color.green(background)
                        + (Color.green(foreground) - Color.green(background)) * ratio),
                Math.round(Color.blue(background)
                        + (Color.blue(foreground) - Color.blue(background)) * ratio));
    }

    private static float luminance(int color) {
        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;
        r = r <= .03928f ? r / 12.92f : (float) Math.pow((r + .055f) / 1.055f, 2.4);
        g = g <= .03928f ? g / 12.92f : (float) Math.pow((g + .055f) / 1.055f, 2.4);
        b = b <= .03928f ? b / 12.92f : (float) Math.pow((b + .055f) / 1.055f, 2.4);
        return .2126f * r + .7152f * g + .0722f * b;
    }

    private enum RowPosition {
        SINGLE,
        FIRST,
        MIDDLE,
        LAST
    }

    private static final class NavigationGroup {
        final LinearLayout container;
        final List<NavigationItem> items = new ArrayList<>();

        NavigationGroup(LinearLayout container) {
            this.container = container;
        }
    }

    private static final class NavigationItem {
        final Page page;
        final NavigationGroup group;
        final LinearLayout row;
        final TextView title;
        final TextView summary;
        final ImageView leadingIcon;
        View separatorAfter;
        RowPosition position = RowPosition.SINGLE;

        NavigationItem(Page page, NavigationGroup group, LinearLayout row, TextView title,
                TextView summary, ImageView leadingIcon) {
            this.page = page;
            this.group = group;
            this.row = row;
            this.title = title;
            this.summary = summary;
            this.leadingIcon = leadingIcon;
        }
    }

    private static final class StyleSpec {
        final ClockStyle style;
        final String id;
        final String name;
        final String summary;

        StyleSpec(ClockStyle style, String name, String summary) {
            this.style = style;
            this.id = style.getMetadata().getId();
            this.name = name;
            this.summary = summary;
        }
    }

    /** Fixed-time preview rendered by the same SDK renderer used on the live clock. */
    private final class UltimateThemePreviewView extends View {
        private final ClockStyle style;

        UltimateThemePreviewView(Context context, ClockStyle style) {
            super(context);
            this.style = style;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (getWidth() <= 0 || getHeight() <= 0) return;
            Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
            calendar.set(2026, Calendar.JUNE, 18, 10, 9, 36);
            calendar.set(Calendar.MILLISECOND, 0);
            ClockState state = ClockState.builder(calendar.getTimeInMillis())
                    .timeZone(calendar.getTimeZone())
                    .locale(Locale.getDefault())
                    .use24Hour(true)
                    .showSeconds(true)
                    .secondHandMotion(ClockState.SecondHandMotion.TICK)
                    .dateText("2026 / 06 / 18  WED")
                    .timeZoneText("LOCAL")
                    .weatherText("24 C  CLEAR")
                    .build();
            ClockRenderContext context = new ClockRenderContext(0f, 0f, getWidth(), getHeight(),
                    getResources().getDisplayMetrics().density,
                    getResources().getDisplayMetrics().scaledDensity,
                    calendar.getTimeInMillis(), true);
            int saveCount = canvas.save();
            try {
                style.getRenderer().render(canvas, context, state, style.getThemeTokens());
            } finally {
                canvas.restoreToCount(saveCount);
            }
        }
    }
}
