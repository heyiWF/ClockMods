package com.clockmods.ultimate;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.ultimate.clock.UltimateClockPreferences;
import com.clockmods.ultimate.clock.UltimateClockStyles;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

/**
 * A deliberately small first-launch flow. It follows the Android setup-wizard pattern: a quiet
 * top header, one focused step in a contained surface, and a persistent bottom navigation row.
 */
public final class SetupWizardActivity extends AppCompatActivity {
    private static final String PREFS = "clockmods_onboarding";
    private static final String KEY_COMPLETED = "completed";

    private static final String STATE_STEP = "setup_wizard_step";
    private static final String STATE_LANGUAGE = "setup_wizard_language";
    private static final String STATE_WEATHER = "setup_wizard_weather";
    private static final String STATE_UNIT = "setup_wizard_unit";
    private static final String STATE_STYLE = "setup_wizard_style";

    private int step;
    private LinearLayout pageHost;
    private LinearLayout pageContent;
    private TextView stepLabel;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton backButton;
    private MaterialButton nextButton;
    private RadioGroup languageGroup;
    private RadioGroup unitGroup;
    private MaterialSwitch weatherSwitch;
    private String selectedStyle;
    private String selectedLanguage;
    private String selectedTemperatureUnit;
    private boolean selectedWeatherEnabled;
    /** True for short landscape windows such as an 800x400 automotive/emulator display. */
    private boolean compactHeight;
    /** True only when the current resource configuration is genuinely landscape. */
    private boolean compactLandscape;
    private final List<MaterialCardView> styleCards = new ArrayList<>();
    private final List<MaterialRadioButton> styleRadios = new ArrayList<>();

    private ClockPreferences preferences;
    private UltimateClockPreferences ultimatePreferences;

    public static boolean isCompleted(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_COMPLETED, false);
    }

    /** Marks onboarding as complete for integrations that provide their own entry point. */
    public static void markCompleted(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COMPLETED, true).apply();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExperienceBridge.applyThemeFeatures(this);
        super.onCreate(savedInstanceState);
        // A few embedded/emulator displays expose a 400x800 app buffer while the physical panel
        // is rotated to 800x400. Treat that rotated portrait buffer as compact too, but do not
        // shrink normal 360dp portrait phones. The regular landscape case is covered by the
        // configuration's short height; the rotated-buffer case is identified by display rotation.
        android.content.res.Configuration configuration = getResources().getConfiguration();
        boolean regularLandscape = configuration.orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                && configuration.screenHeightDp <= 480;
        int displayRotation = getDisplay() == null ? android.view.Surface.ROTATION_0
                : getDisplay().getRotation();
        boolean rotatedBuffer = (displayRotation == android.view.Surface.ROTATION_90
                || displayRotation == android.view.Surface.ROTATION_270)
                && configuration.screenWidthDp <= 360
                && configuration.screenHeightDp > configuration.screenWidthDp;
        compactHeight = regularLandscape || rotatedBuffer;
        android.graphics.Rect windowBounds = getWindowManager().getCurrentWindowMetrics()
                .getBounds();
        boolean windowLandscape = windowBounds.width() > windowBounds.height();
        // Some emulator shells expose a portrait app buffer while the display is rotated. In
        // that case the rotation itself is the reliable landscape signal.
        boolean rotatedDisplay = displayRotation == android.view.Surface.ROTATION_90
                || displayRotation == android.view.Surface.ROTATION_270;
        compactLandscape = compactHeight && (windowLandscape || rotatedDisplay);
        preferences = new ClockPreferences(this);
        ultimatePreferences = new UltimateClockPreferences(this);
        selectedLanguage = preferences.getClockLanguage();
        selectedTemperatureUnit = preferences.getWeatherTemperatureUnit();
        selectedWeatherEnabled = preferences.isWeatherEnabled();
        selectedStyle = ultimatePreferences.getStyleId();
        if (savedInstanceState != null) {
            selectedLanguage = savedInstanceState.getString(STATE_LANGUAGE, selectedLanguage);
            selectedTemperatureUnit = savedInstanceState.getString(STATE_UNIT,
                    selectedTemperatureUnit);
            selectedWeatherEnabled = savedInstanceState.getBoolean(STATE_WEATHER,
                    selectedWeatherEnabled);
            selectedStyle = savedInstanceState.getString(STATE_STYLE, selectedStyle);
        }
        buildShell();
        showStep(savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_STEP, 0));
    }

    private void buildShell() {
        EdgeToEdge.enable(this);
        getWindow().setNavigationBarContrastEnforced(false);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(surfaceColor());
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int type = android.view.WindowInsets.Type.systemBars()
                    | android.view.WindowInsets.Type.displayCutout();
            android.graphics.Insets bars = insets.getInsets(type);
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Glif keeps a readable content column on large displays while using the full available
        // width on phones. The 24dp shell inset mirrors suw_glif_margin_sides.
        int availableWidth = getResources().getDisplayMetrics().widthPixels;
        // Never force the 280dp Glif minimum on a narrower window: overflowing the shell causes
        // clipped text and asymmetric insets on small emulator/automotive displays.
        int contentWidth = Math.min(dp(688), availableWidth);
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(24), dp(compactHeight ? 8 : 16), dp(24), 0);
        FrameLayout.LayoutParams shellParams = new FrameLayout.LayoutParams(
                contentWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        shellParams.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(shell, shellParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ultimate_ic_schedule);
        icon.setColorFilter(primaryColor(), PorterDuff.Mode.SRC_IN);
        icon.setContentDescription(null);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int headerIconSize = dp(compactHeight ? 28 : 32);
        icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        titleRow.addView(icon, new LinearLayout.LayoutParams(headerIconSize, headerIconSize));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(R.string.setup_wizard_title, compactHeight ? 20 : 24, false);
        titleBlock.addView(title);
        TextView intro = text(R.string.setup_wizard_intro, compactHeight ? 12 : 14, false);
        intro.setTextColor(onSurfaceVariantColor());
        intro.setLineSpacing(0f, 1.15f);
        if (compactLandscape) intro.setVisibility(View.GONE);
        titleBlock.addView(intro, topMargin(wrap(), dp(compactHeight ? 1 : 3)));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart(dp(12));
        titleRow.addView(titleBlock, titleParams);
        header.addView(titleRow);

        LinearLayout progressRow = new LinearLayout(this);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        progressIndicator = new LinearProgressIndicator(this);
        progressIndicator.setIndeterminate(false);
        progressIndicator.setTrackCornerRadius(dp(4));
        progressIndicator.setTrackThickness(dp(compactHeight ? 3 : 4));
        // Glif's progress track is a simple bar; Material 3's optional stop marker would add a
        // distracting dot at the end of steps one and two.
        progressIndicator.setTrackStopIndicatorSize(0);
        progressIndicator.setIndicatorColor(primaryColor());
        progressIndicator.setTrackColor(withAlpha(onSurfaceVariantColor(), 0.22f));
        progressRow.addView(progressIndicator, new LinearLayout.LayoutParams(0,
                dp(compactHeight ? 3 : 4), 1f));
        stepLabel = text(null, compactHeight ? 12 : 13, false);
        stepLabel.setTextColor(onSurfaceVariantColor());
        LinearLayout.LayoutParams stepParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stepParams.setMarginStart(dp(12));
        progressRow.addView(stepLabel, stepParams);
        header.addView(progressRow, topMargin(wrap(), dp(compactLandscape ? 6
                : compactHeight ? 8 : 16)));
        shell.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        int pageInset = compactLandscape ? 4 : compactHeight ? 8 : 16;
        pageHost.setPadding(0, dp(pageInset), 0, dp(pageInset));
        scroll.addView(pageHost, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        shell.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        int actionHeight = dp(compactHeight ? 56 : 72);
        // Keep the platform-recommended 48dp touch target even in the compact footer; the
        // compact mode saves space from shell/header/list spacing instead.
        int buttonHeight = dp(48);
        actions.setMinimumHeight(actionHeight);
        actions.setPadding(0, dp(compactHeight ? 4 : 8), 0, dp(compactHeight ? 4 : 8));
        backButton = new MaterialButton(this, null,
                androidx.appcompat.R.attr.borderlessButtonStyle);
        backButton.setText(R.string.setup_wizard_skip);
        backButton.setMinHeight(buttonHeight);
        backButton.setMinWidth(0);
        backButton.setOnClickListener(v -> finishWithoutChanges());
        actions.addView(backButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, buttonHeight));
        Space actionSpacer = new Space(this);
        actions.addView(actionSpacer, new LinearLayout.LayoutParams(0, 1, 1f));
        nextButton = new MaterialButton(this);
        nextButton.setText(R.string.setup_wizard_next);
        nextButton.setMinHeight(buttonHeight);
        nextButton.setMinWidth(dp(compactHeight ? 112 : 120));
        nextButton.setInsetTop(0);
        nextButton.setInsetBottom(0);
        nextButton.setCornerRadius(dp(4));
        nextButton.setOnClickListener(v -> onNext());
        actions.addView(nextButton);
        shell.addView(actions);
        setContentView(root);
    }

    private void showStep(int requestedStep) {
        step = Math.max(0, Math.min(2, requestedStep));
        pageHost.removeAllViews();
        styleCards.clear();
        styleRadios.clear();
        // GlifLayout-style content: the scroll view itself is the single middle pane. Keeping
        // the step free of a surrounding card gives the radio/list controls native breathing
        // room and avoids nested rounded surfaces.
        pageContent = pageHost;
        int pageInset = compactLandscape ? 4 : compactHeight ? 8 : 16;
        pageContent.setPadding(0, dp(pageInset), 0, dp(pageInset));

        stepLabel.setText(getString(R.string.setup_wizard_step, step + 1, 3));
        if (progressIndicator != null) progressIndicator.setProgress((step + 1) * 100 / 3);
        if (step == 0) languagePage();
        else if (step == 1) weatherPage();
        else stylePage();
        backButton.setText(getString(step == 0 ? R.string.setup_wizard_skip : R.string.setup_wizard_back));
        nextButton.setText(getString(step == 2 ? R.string.setup_wizard_done : R.string.setup_wizard_next));
    }

    private void languagePage() {
        pageContent.addView(text(R.string.setup_wizard_language_title,
                compactHeight ? 20 : 24, false));
        TextView summary = text(R.string.setup_wizard_language_summary, 14, false);
        if (compactHeight) summary.setTextSize(12);
        summary.setTextColor(onSurfaceVariantColor());
        if (compactLandscape) summary.setVisibility(View.GONE);
        pageContent.addView(summary, topMargin(wrap(), dp(compactHeight ? 3 : 6)));

        languageGroup = new RadioGroup(this);
        languageGroup.setOrientation(compactLandscape ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL);
        addLanguageOption(ClockPreferences.LANGUAGE_SIMPLIFIED, R.string.ultimate_language_simplified);
        addLanguageOption(ClockPreferences.LANGUAGE_TRADITIONAL, R.string.ultimate_language_traditional);
        addLanguageOption(ClockPreferences.LANGUAGE_ENGLISH, R.string.ultimate_language_english);
        pageContent.addView(languageGroup, topMargin(wrap(), dp(compactLandscape ? 4
                : compactHeight ? 8 : 16)));
        for (int i = 0; i < languageGroup.getChildCount(); i++) {
            RadioButton button = (RadioButton) languageGroup.getChildAt(i);
            if (String.valueOf(button.getTag()).equals(selectedLanguage)) button.setChecked(true);
        }
    }

    private void addLanguageOption(String value, int labelRes) {
        MaterialRadioButton button = new MaterialRadioButton(this);
        button.setId(View.generateViewId());
        button.setText(labelRes);
        button.setTag(value);
        button.setTextSize(compactHeight ? 15 : 16);
        int rowHeight = dp(compactLandscape ? 48 : compactHeight ? 44 : 56);
        button.setMinHeight(rowHeight);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(compactHeight ? 4 : 8), 0, 0, 0);
        if (compactLandscape) {
            languageGroup.addView(button, new RadioGroup.LayoutParams(0, rowHeight, 1f));
        } else {
            languageGroup.addView(button, new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));
        }
    }

    private void weatherPage() {
        pageContent.addView(text(R.string.setup_wizard_weather_title,
                compactHeight ? 20 : 24, false));
        TextView summary = text(R.string.setup_wizard_weather_summary, 14, false);
        if (compactHeight) summary.setTextSize(12);
        summary.setTextColor(onSurfaceVariantColor());
        if (compactLandscape) summary.setVisibility(View.GONE);
        pageContent.addView(summary, topMargin(wrap(), dp(compactHeight ? 3 : 6)));

        LinearLayout weatherRow = listRow(compactLandscape ? 48 : compactHeight ? 56 : 72);
        LinearLayout weatherLabels = new LinearLayout(this);
        weatherLabels.setOrientation(LinearLayout.VERTICAL);
        TextView weatherTitle = text(R.string.setup_wizard_weather_enabled,
                compactHeight ? 15 : 16, false);
        weatherLabels.addView(weatherTitle);
        TextView weatherHint = text(R.string.setup_wizard_weather_enabled_summary,
                compactHeight ? 12 : 14, false);
        weatherHint.setTextColor(onSurfaceVariantColor());
        if (compactLandscape) weatherHint.setVisibility(View.GONE);
        weatherLabels.addView(weatherHint, topMargin(wrap(), dp(2)));
        weatherSwitch = new MaterialSwitch(this);
        weatherSwitch.setText(null);
        weatherSwitch.setPadding(0, 0, 0, 0);
        weatherSwitch.setChecked(selectedWeatherEnabled);
        weatherRow.addView(weatherLabels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        weatherRow.addView(weatherSwitch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        weatherRow.setOnClickListener(v -> weatherSwitch.toggle());
        // In a short landscape window the footer is persistent, so keep the switch row and
        // temperature choices adjacent. The controls retain their 48dp touch targets while the
        // extra vertical gap used by portrait Glif layouts is removed.
        if (compactLandscape) {
            weatherRow.setPadding(0, 0, 0, 0);
        }
        pageContent.addView(weatherRow, topMargin(wrap(), dp(compactLandscape ? 0
                : compactHeight ? 10 : 20)));

        TextView unitLabel = text(R.string.setup_wizard_temperature_title,
                compactHeight ? 13 : 14, false);
        unitGroup = new RadioGroup(this);
        unitGroup.setOrientation(compactLandscape ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL);
        addUnitOption(ClockPreferences.WEATHER_UNIT_CELSIUS, R.string.ultimate_weather_celsius);
        addUnitOption(ClockPreferences.WEATHER_UNIT_FAHRENHEIT, R.string.ultimate_weather_fahrenheit);
        if (compactLandscape) {
            unitLabel.setVisibility(View.GONE);
            LinearLayout unitRow = new LinearLayout(this);
            unitRow.setOrientation(LinearLayout.HORIZONTAL);
            unitRow.setGravity(Gravity.CENTER_VERTICAL);
            unitRow.setMinimumHeight(dp(48));
            LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                    0, dp(48), 1f);
            unitRow.addView(unitGroup, groupParams);
            pageContent.addView(unitRow, topMargin(wrap(), 0));
        } else {
            pageContent.addView(unitLabel, topMargin(wrap(), dp(compactHeight ? 12 : 24)));
            pageContent.addView(unitGroup, topMargin(wrap(), dp(compactHeight ? 4 : 8)));
        }
        for (int i = 0; i < unitGroup.getChildCount(); i++) {
            RadioButton button = (RadioButton) unitGroup.getChildAt(i);
            if (String.valueOf(button.getTag()).equals(selectedTemperatureUnit)) button.setChecked(true);
        }
        weatherSwitch.setOnCheckedChangeListener((button, checked) -> {
            selectedWeatherEnabled = checked;
            setRadioGroupEnabled(unitGroup, checked);
        });
        setRadioGroupEnabled(unitGroup, selectedWeatherEnabled);
    }

    private void addUnitOption(String value, int labelRes) {
        MaterialRadioButton button = new MaterialRadioButton(this);
        button.setId(View.generateViewId());
        button.setText(labelRes);
        button.setTag(value);
        button.setTextSize(compactHeight ? 15 : 16);
        int rowHeight = dp(compactLandscape ? 48 : compactHeight ? 44 : 56);
        button.setMinHeight(rowHeight);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(compactHeight ? 4 : 8), 0, 0, 0);
        if (compactLandscape) {
            unitGroup.addView(button, new RadioGroup.LayoutParams(0, rowHeight, 1f));
        } else {
            unitGroup.addView(button, new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));
        }
    }

    private void stylePage() {
        pageContent.addView(text(R.string.setup_wizard_style_title,
                compactHeight ? 20 : 24, false));
        TextView summary = text(R.string.setup_wizard_style_summary, 14, false);
        if (compactHeight) summary.setTextSize(12);
        summary.setTextColor(onSurfaceVariantColor());
        if (compactLandscape) summary.setVisibility(View.GONE);
        pageContent.addView(summary, topMargin(wrap(), dp(compactHeight ? 3 : 6)));

        TextView settingsHint = text(R.string.setup_wizard_settings_hint,
                compactHeight ? 12 : 14, false);
        settingsHint.setTextColor(primaryColor());
        settingsHint.setLineSpacing(0f, 1.15f);
        pageContent.addView(settingsHint, topMargin(wrap(), dp(compactHeight ? 6 : 10)));

        addStyleCard(UltimateClockStyles.STYLE_GLASS_ATELIER,
                R.string.ultimate_style_glass_name, R.string.ultimate_style_glass_summary);
        addStyleCard(UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
                R.string.ultimate_style_noir_name, R.string.ultimate_style_noir_summary);
        addStyleCard(UltimateClockStyles.STYLE_PAPER_STATION,
                R.string.ultimate_style_paper_name, R.string.ultimate_style_paper_summary);
    }

    private void addStyleCard(String id, int titleRes, int summaryRes) {
        MaterialCardView card = new MaterialCardView(this);
        // Selection is represented by the explicit trailing radio and stroke colour. Leaving the
        // card non-checkable prevents MaterialCardView from adding its default checked icon.
        card.setCheckable(false);
        // Keep this explicit as a guard against theme defaults adding the Material checked icon.
        card.setCheckedIcon(null);
        card.setClickable(true);
        card.setFocusable(true);
        card.setRadius(dp(14));
        card.setCardElevation(0f);
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(surfaceContainerLowColor());
        card.setContentDescription(getString(titleRes) + ", " + getString(summaryRes));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(14), dp(12), dp(10), dp(12));
        View swatch = new View(this);
        GradientDrawable swatchBackground = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, styleSwatchColors(id));
        swatchBackground.setCornerRadius(dp(14));
        swatch.setBackground(swatchBackground);
        content.addView(swatch, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(titleRes, 16, false);
        labels.addView(title);
        TextView subtitle = text(summaryRes, 13, false);
        subtitle.setTextColor(onSurfaceVariantColor());
        subtitle.setMaxLines(2);
        labels.addView(subtitle, topMargin(wrap(), dp(3)));
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.setMarginStart(dp(14));
        content.addView(labels, labelsParams);

        MaterialRadioButton radio = new MaterialRadioButton(this);
        radio.setClickable(false);
        radio.setFocusable(false);
        radio.setBackground(null);
        radio.setContentDescription(null);
        content.addView(radio, new LinearLayout.LayoutParams(dp(48), dp(48)));
        card.addView(content);
        card.setOnClickListener(v -> {
            selectedStyle = id;
            updateStyleCards();
        });
        card.setTag(id);
        styleCards.add(card);
        styleRadios.add(radio);
        pageContent.addView(card, topMargin(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT), dp(12)));
        updateStyleCards();
    }

    private void updateStyleCards() {
        for (int i = 0; i < styleCards.size(); i++) {
            MaterialCardView card = styleCards.get(i);
            boolean selected = String.valueOf(card.getTag()).equals(selectedStyle);
            card.setStrokeWidth(dp(selected ? 2 : 1));
            card.setStrokeColor(selected ? primaryColor() : outlineVariantColor());
            styleRadios.get(i).setChecked(selected);
            String state = selected ? getString(R.string.ultimate_settings_selected_state) : null;
            card.setStateDescription(state);
        }
    }

    private int[] styleSwatchColors(String id) {
        if (UltimateClockStyles.STYLE_NOIR_INSTRUMENT.equals(id)) {
            return new int[]{Color.rgb(22, 27, 36), Color.rgb(83, 105, 137)};
        }
        if (UltimateClockStyles.STYLE_PAPER_STATION.equals(id)) {
            return new int[]{Color.rgb(238, 226, 204), Color.rgb(183, 126, 90)};
        }
        return new int[]{Color.rgb(101, 174, 190), Color.rgb(226, 241, 232)};
    }

    private void onNext() {
        if (step < 2) {
            captureCurrentStep();
            step++;
            showStep(step);
        } else {
            applyChoices();
            markCompleted(this);
            setResult(RESULT_OK);
            finish();
        }
    }

    private void applyChoices() {
        captureCurrentStep();
        preferences.setClockLanguage(selectedLanguage);
        preferences.setWeatherEnabled(selectedWeatherEnabled);
        // Keep onboarding friction-free; the weather controller will request location access
        // on the first refresh when automatic location is selected.
        if (!ClockPreferences.WEATHER_LOCATION_MANUAL.equals(
                preferences.getWeatherLocationMode())) {
            preferences.setWeatherLocationMode(ClockPreferences.WEATHER_LOCATION_AUTOMATIC);
        }
        preferences.setWeatherTemperatureUnit(selectedTemperatureUnit);
        ultimatePreferences.setStyleId(selectedStyle);
    }

    /** Keeps choices when the user moves back and forth without writing partial setup to disk. */
    private void captureCurrentStep() {
        if (step == 0 && languageGroup != null) {
            selectedLanguage = checkedTag(languageGroup, selectedLanguage);
        } else if (step == 1) {
            if (weatherSwitch != null) selectedWeatherEnabled = weatherSwitch.isChecked();
            if (unitGroup != null) {
                selectedTemperatureUnit = checkedTag(unitGroup, selectedTemperatureUnit);
            }
        }
    }

    private static String checkedTag(RadioGroup group, String fallback) {
        int checkedId = group.getCheckedRadioButtonId();
        if (checkedId == -1) return fallback;
        View checked = group.findViewById(checkedId);
        Object tag = checked == null ? null : checked.getTag();
        return tag == null ? fallback : String.valueOf(tag);
    }

    private static void setRadioGroupEnabled(RadioGroup group, boolean enabled) {
        group.setEnabled(enabled);
        group.setAlpha(enabled ? 1f : 0.5f);
        for (int index = 0; index < group.getChildCount(); index++) {
            group.getChildAt(index).setEnabled(enabled);
        }
    }

    private void finishWithoutChanges() {
        if (step > 0) {
            captureCurrentStep();
            step--;
            showStep(step);
            return;
        }
        markCompleted(this);
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        finishWithoutChanges();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        captureCurrentStep();
        outState.putInt(STATE_STEP, step);
        outState.putString(STATE_LANGUAGE, selectedLanguage);
        outState.putBoolean(STATE_WEATHER, selectedWeatherEnabled);
        outState.putString(STATE_UNIT, selectedTemperatureUnit);
        outState.putString(STATE_STYLE, selectedStyle);
        super.onSaveInstanceState(outState);
    }

    private TextView text(int resId, int size, boolean bold) {
        return text(resId == 0 ? null : getString(resId), size, bold);
    }

    private TextView text(CharSequence value, int size, boolean bold) {
        TextView view = new TextView(this);
        if (value != null) view.setText(value);
        view.setTextSize(size);
        view.setTextColor(onSurfaceColor());
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private int surfaceColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface,
                Color.rgb(20, 20, 22));
    }

    private int surfaceContainerLowColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow,
                surfaceColor());
    }

    private int outlineVariantColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant,
                withAlpha(onSurfaceVariantColor(), 0.45f));
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

    private LinearLayout listRow(int minHeightDp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(minHeightDp));
        row.setPadding(0, dp(8), 0, dp(8));
        row.setClickable(true);
        row.setFocusable(true);
        android.util.TypedValue value = new android.util.TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true)
                && value.resourceId != 0) {
            row.setBackgroundResource(value.resourceId);
        }
        return row;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(LinearLayout.LayoutParams params, int margin) {
        params.topMargin = margin;
        return params;
    }
}
