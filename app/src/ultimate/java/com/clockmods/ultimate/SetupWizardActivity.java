package com.clockmods.ultimate;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

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

/**
 * A deliberately small first-launch flow. It only asks for choices that make the clock useful
 * immediately; every item remains editable from the full settings screen later.
 */
public final class SetupWizardActivity extends AppCompatActivity {
    private static final String PREFS = "clockmods_onboarding";
    private static final String KEY_COMPLETED = "completed";

    private int step;
    private LinearLayout pageHost;
    private TextView stepLabel;
    private MaterialButton backButton;
    private MaterialButton nextButton;
    private RadioGroup languageGroup;
    private RadioGroup unitGroup;
    private MaterialSwitch weatherSwitch;
    private String selectedStyle;

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
        preferences = new ClockPreferences(this);
        ultimatePreferences = new UltimateClockPreferences(this);
        selectedStyle = ultimatePreferences.getStyleId();
        buildShell();
        showStep(savedInstanceState == null ? 0 : savedInstanceState.getInt("step", 0));
    }

    private void buildShell() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor());
        root.setPadding(dp(24), dp(24), dp(24), dp(16));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(R.string.setup_wizard_title, 24, true);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stepLabel = text(null, 14, false);
        stepLabel.setTextColor(onSurfaceVariantColor());
        header.addView(stepLabel);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView intro = text(R.string.setup_wizard_intro, 14, false);
        intro.setTextColor(onSurfaceVariantColor());
        intro.setLineSpacing(0f, 1.15f);
        root.addView(intro, topMargin(wrap(), dp(8)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(pageHost, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        backButton = new MaterialButton(this, null,
                androidx.appcompat.R.attr.borderlessButtonStyle);
        backButton.setText(R.string.setup_wizard_skip);
        backButton.setOnClickListener(v -> finishWithoutChanges());
        actions.addView(backButton, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        nextButton = new MaterialButton(this);
        nextButton.setText(R.string.setup_wizard_next);
        nextButton.setOnClickListener(v -> onNext());
        actions.addView(nextButton);
        root.addView(actions, topMargin(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT), dp(12)));
        setContentView(root);
    }

    private void showStep(int requestedStep) {
        step = Math.max(0, Math.min(2, requestedStep));
        pageHost.removeAllViews();
        stepLabel.setText(getString(R.string.setup_wizard_step, step + 1, 3));
        if (step == 0) languagePage();
        else if (step == 1) weatherPage();
        else stylePage();
        backButton.setText(getString(step == 0 ? R.string.setup_wizard_skip : R.string.setup_wizard_back));
        nextButton.setText(getString(step == 2 ? R.string.setup_wizard_done : R.string.setup_wizard_next));
    }

    private void languagePage() {
        pageHost.addView(text(R.string.setup_wizard_language_title, 21, true), topMargin(wrap(), dp(28)));
        TextView summary = text(R.string.setup_wizard_language_summary, 14, false);
        summary.setTextColor(onSurfaceVariantColor());
        pageHost.addView(summary, topMargin(wrap(), dp(8)));
        languageGroup = new RadioGroup(this);
        languageGroup.setOrientation(RadioGroup.VERTICAL);
        addLanguageOption(ClockPreferences.LANGUAGE_SIMPLIFIED, R.string.ultimate_language_simplified);
        addLanguageOption(ClockPreferences.LANGUAGE_TRADITIONAL, R.string.ultimate_language_traditional);
        addLanguageOption(ClockPreferences.LANGUAGE_ENGLISH, R.string.ultimate_language_english);
        pageHost.addView(languageGroup, topMargin(wrap(), dp(20)));
        String current = preferences.getClockLanguage();
        for (int i = 0; i < languageGroup.getChildCount(); i++) {
            RadioButton button = (RadioButton) languageGroup.getChildAt(i);
            if (String.valueOf(button.getTag()).equals(current)) button.setChecked(true);
        }
    }

    private void addLanguageOption(String value, int labelRes) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(labelRes);
        button.setTag(value);
        button.setTextSize(16);
        button.setPadding(0, dp(10), 0, dp(10));
        languageGroup.addView(button, new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void weatherPage() {
        pageHost.addView(text(R.string.setup_wizard_weather_title, 21, true), topMargin(wrap(), dp(28)));
        TextView summary = text(R.string.setup_wizard_weather_summary, 14, false);
        summary.setTextColor(onSurfaceVariantColor());
        pageHost.addView(summary, topMargin(wrap(), dp(8)));
        weatherSwitch = new MaterialSwitch(this);
        weatherSwitch.setText(R.string.setup_wizard_weather_enabled);
        weatherSwitch.setTextSize(16);
        weatherSwitch.setChecked(preferences.isWeatherEnabled());
        pageHost.addView(weatherSwitch, topMargin(wrap(), dp(20)));

        TextView unitLabel = text(R.string.setup_wizard_temperature_title, 15, true);
        pageHost.addView(unitLabel, topMargin(wrap(), dp(20)));
        unitGroup = new RadioGroup(this);
        unitGroup.setOrientation(RadioGroup.HORIZONTAL);
        addUnitOption(ClockPreferences.WEATHER_UNIT_CELSIUS, R.string.ultimate_weather_celsius);
        addUnitOption(ClockPreferences.WEATHER_UNIT_FAHRENHEIT, R.string.ultimate_weather_fahrenheit);
        pageHost.addView(unitGroup, topMargin(wrap(), dp(4)));
        String current = preferences.getWeatherTemperatureUnit();
        for (int i = 0; i < unitGroup.getChildCount(); i++) {
            RadioButton button = (RadioButton) unitGroup.getChildAt(i);
            if (String.valueOf(button.getTag()).equals(current)) button.setChecked(true);
        }
    }

    private void addUnitOption(String value, int labelRes) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(labelRes);
        button.setTag(value);
        button.setTextSize(16);
        button.setPadding(0, dp(8), dp(20), dp(8));
        unitGroup.addView(button, new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void stylePage() {
        pageHost.addView(text(R.string.setup_wizard_style_title, 21, true), topMargin(wrap(), dp(28)));
        TextView summary = text(R.string.setup_wizard_style_summary, 14, false);
        summary.setTextColor(onSurfaceVariantColor());
        pageHost.addView(summary, topMargin(wrap(), dp(8)));

        addStyleCard(UltimateClockStyles.STYLE_GLASS_ATELIER,
                R.string.ultimate_style_glass_name, R.string.ultimate_style_glass_summary);
        addStyleCard(UltimateClockStyles.STYLE_NOIR_INSTRUMENT,
                R.string.ultimate_style_noir_name, R.string.ultimate_style_noir_summary);
        addStyleCard(UltimateClockStyles.STYLE_PAPER_STATION,
                R.string.ultimate_style_paper_name, R.string.ultimate_style_paper_summary);
    }

    private void addStyleCard(String id, int titleRes, int summaryRes) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCheckable(true);
        card.setClickable(true);
        card.setFocusable(true);
        card.setRadius(dp(14));
        card.setCardElevation(0f);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView title = text(titleRes, 16, true);
        content.addView(title);
        TextView subtitle = text(summaryRes, 13, false);
        subtitle.setTextColor(onSurfaceVariantColor());
        content.addView(subtitle, topMargin(wrap(), dp(4)));
        card.addView(content);
        card.setOnClickListener(v -> {
            selectedStyle = id;
            updateStyleCards();
        });
        card.setTag(id);
        pageHost.addView(card, topMargin(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT), dp(14)));
        updateStyleCards();
    }

    private void updateStyleCards() {
        for (int i = 0; i < pageHost.getChildCount(); i++) {
            View child = pageHost.getChildAt(i);
            if (!(child instanceof MaterialCardView) || child.getTag() == null) continue;
            MaterialCardView card = (MaterialCardView) child;
            boolean selected = String.valueOf(card.getTag()).equals(selectedStyle);
            card.setChecked(selected);
            card.setStrokeWidth(dp(selected ? 2 : 1));
            card.setStrokeColor(selected ? primaryColor() : onSurfaceVariantColor());
        }
    }

    private void onNext() {
        if (step < 2) {
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
        if (languageGroup != null && languageGroup.getCheckedRadioButtonId() != -1) {
            RadioButton selected = findViewById(languageGroup.getCheckedRadioButtonId());
            preferences.setClockLanguage(String.valueOf(selected.getTag()));
        }
        if (weatherSwitch != null) {
            preferences.setWeatherEnabled(weatherSwitch.isChecked());
            // Keep onboarding friction-free; the weather controller will request location access
            // on the first refresh when automatic location is selected.
            preferences.setWeatherLocationMode(ClockPreferences.WEATHER_LOCATION_AUTOMATIC);
        }
        if (unitGroup != null && unitGroup.getCheckedRadioButtonId() != -1) {
            RadioButton selected = findViewById(unitGroup.getCheckedRadioButtonId());
            preferences.setWeatherTemperatureUnit(String.valueOf(selected.getTag()));
        }
        ultimatePreferences.setStyleId(selectedStyle);
    }

    private void finishWithoutChanges() {
        if (step > 0) {
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
        outState.putInt("step", step);
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
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK);
    }

    private int onSurfaceColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
    }

    private int onSurfaceVariantColor() {
        return MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                onSurfaceColor());
    }

    private int primaryColor() {
        return MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary,
                onSurfaceColor());
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
