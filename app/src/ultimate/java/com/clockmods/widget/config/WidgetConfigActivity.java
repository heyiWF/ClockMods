package com.clockmods.widget.config;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.clockmods.R;
import com.clockmods.widget.model.WidgetConfig;
import com.clockmods.widget.model.WidgetKind;
import com.clockmods.widget.model.WidgetThemeSpec;
import com.clockmods.widget.render.WidgetFontRegistry;
import com.clockmods.widget.render.WidgetThemeRegistry;
import com.clockmods.widget.store.WidgetConfigStore;
import com.clockmods.widget.update.WidgetUpdateCoordinator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Shared launcher configuration flow for every widget provider.
 *
 * <p>Material 3 throughout: a toolbar, filter chips for the theme strip, {@link MaterialSwitch}
 * rows, exposed dropdown menus and {@link Slider}s. The view tree is built once and afterwards
 * only synced from the in-memory draft, so changing a setting never rebuilds the screen or loses
 * the scroll position. Only "Done" commits; the close button and system back both cancel, and a
 * configuration change keeps the draft in the saved instance state.
 */
public final class WidgetConfigActivity extends AppCompatActivity {
    private static final String[] TAP_ACTIONS = {"open_clock", "open_calendar", "open_weather", "open_config"};
    private static final float SCALE_MIN = .85f;
    private static final float SCALE_MAX = 1.20f;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    private WidgetConfig draft;
    private LinearLayout content;
    private WidgetPreviewView preview;

    private final List<WidgetThemeSpec> themes = new ArrayList<>();
    private final List<Chip> themeChips = new ArrayList<>();

    private MaterialSwitch dateSwitch;
    private MaterialSwitch weekdaySwitch;
    private MaterialSwitch lunarSwitch;
    private MaterialSwitch locationSwitch;
    private MaterialSwitch descriptionSwitch;
    private MaterialSwitch secondsSwitch;
    private MaterialSwitch darkTextSwitch;

    private TextInputLayout timeFormatBox;
    private MaterialAutoCompleteTextView timeFormatField;
    private MaterialAutoCompleteTextView tapField;
    private MaterialAutoCompleteTextView fontField;
    private String[] fontLabels;

    private Slider opacitySlider;
    private Slider scaleSlider;
    private TextView opacityValue;
    private TextView scaleValue;

    private String[] timeFormatLabels;
    private String[] tapLabels;
    private boolean building;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Launcher flow: anything that is not an explicit "Done" must drop a freshly added widget.
        setResult(RESULT_CANCELED);
        int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        WidgetKind kind = WidgetUpdateCoordinator.kindFor(this, id);
        if (kind == null) { finish(); return; }
        WidgetUpdateCoordinator.execute(() -> {
            WidgetConfig loaded = state == null || !state.containsKey("draft")
                    ? new WidgetConfigStore(this).getOrDefault(id, kind)
                    : WidgetConfigStore.decode(state.getString("draft"), id, kind);
            runOnUiThread(() -> { if (isFinishing() || isDestroyed()) return; draft = loaded; build(); });
        });
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (draft != null) out.putString("draft", WidgetConfigStore.encode(draft));
    }

    private void build() {
        building = true;
        setContentView(R.layout.activity_widget_config);
        content = findViewById(R.id.widget_config_content);
        // The serialized draft is authoritative. Android must not restore stale child-view state
        // over sync(), especially when the asynchronous load finishes during Activity restoration.
        content.setSaveFromParentEnabled(false);
        MaterialToolbar toolbar = findViewById(R.id.widget_config_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        applySystemBarInsets(findViewById(R.id.widget_config_root));

        section(R.string.widget_preview);
        preview = new WidgetPreviewView(this);
        preview.setId(R.id.widget_config_preview);
        int[] box = previewBox();
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(box[0]), dp(box[1]));
        previewParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(preview, previewParams);

        section(R.string.widget_theme);
        ChipGroup group = new ChipGroup(this);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(dp(8));
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setClipToPadding(false);
        strip.addView(group, new ViewGroup.LayoutParams(WRAP, WRAP));
        LinearLayout.LayoutParams stripParams = new LinearLayout.LayoutParams(MATCH, WRAP);
        stripParams.bottomMargin = dp(4);
        content.addView(strip, stripParams);
        LayoutInflater inflater = LayoutInflater.from(this);
        themes.clear();
        themeChips.clear();
        themes.addAll(WidgetThemeRegistry.all(this));
        for (WidgetThemeSpec theme : themes) {
            Chip chip = (Chip) inflater.inflate(R.layout.widget_theme_chip, group, false);
            chip.setText(theme.displayNameRes);
            chip.setId(View.generateViewId());
            chip.setOnClickListener(v -> {
                if (building) return;
                draft = draft.toBuilder().themeId(theme.id)
                        .backgroundAlpha(theme.defaultBackgroundAlpha).build();
                sync();
            });
            group.addView(chip);
            themeChips.add(chip);
        }
        darkTextSwitch = toggle(R.string.widget_dark_text, View.generateViewId(),
                v -> draft.toBuilder().darkText(v).build());

        fontLabels = WidgetFontRegistry.labels(this);
        TextInputLayout fontBox = drawer(fontLabels, R.string.widget_font, position ->
                draft = draft.toBuilder().fontId(WidgetFontRegistry.idAt(position)).build());
        fontField = (MaterialAutoCompleteTextView) fontBox.getEditText();

        section(R.string.widget_content);
        dateSwitch = toggle(R.string.widget_date, R.id.widget_config_date,
                v -> draft.toBuilder().showDate(v).build());
        weekdaySwitch = toggle(R.string.widget_weekday, View.generateViewId(),
                v -> draft.toBuilder().showWeekday(v).build());
        lunarSwitch = toggle(R.string.widget_lunar, View.generateViewId(),
                v -> draft.toBuilder().showLunar(v).build());
        locationSwitch = toggle(R.string.widget_location, View.generateViewId(),
                v -> draft.toBuilder().showLocation(v).build());
        descriptionSwitch = toggle(R.string.widget_description, View.generateViewId(),
                v -> draft.toBuilder().showWeatherDescription(v).build());
        secondsSwitch = toggle(R.string.widget_seconds, View.generateViewId(),
                v -> draft.toBuilder().showSeconds(v).build());

        // The selectors below carry their own Material floating labels, so they are not given
        // a section heading as well — that only duplicated the same word twice.
        timeFormatLabels = new String[]{
                getString(R.string.widget_system), getString(R.string.widget_24h), getString(R.string.widget_12h)};
        timeFormatBox = drawer(timeFormatLabels, R.string.widget_time_format, position ->
                draft = draft.toBuilder()
                        .useSystemTimeFormat(position == 0).use24Hour(position != 2).build());
        timeFormatField = (MaterialAutoCompleteTextView) timeFormatBox.getEditText();

        tapLabels = new String[]{
                getString(R.string.widget_open_clock), getString(R.string.widget_open_calendar),
                getString(R.string.widget_open_weather), getString(R.string.widget_open_config)};
        TextInputLayout tapBox = drawer(tapLabels, R.string.widget_tap, position ->
                draft = draft.toBuilder().tapAction(TAP_ACTIONS[position]).build());
        tapField = (MaterialAutoCompleteTextView) tapBox.getEditText();

        opacityValue = sliderHeader(R.string.widget_opacity);
        opacitySlider = slider(0, 255, value -> percentLabel(Math.round(value) * 100 / 255), value -> {
            draft = draft.toBuilder().backgroundAlpha(value).build();
            opacityValue.setText(percentLabel(value * 100 / 255));
        });
        opacitySlider.setContentDescription(getString(R.string.widget_opacity));

        scaleValue = sliderHeader(R.string.widget_scale);
        scaleSlider = slider(Math.round(SCALE_MIN * 100), Math.round(SCALE_MAX * 100),
                value -> percentLabel((int) value), value -> {
            draft = draft.toBuilder().textScale(value / 100f).build();
            scaleValue.setText(percentLabel(value));
        });
        scaleSlider.setContentDescription(getString(R.string.widget_scale));

        MaterialButton reset = findViewById(R.id.widget_config_reset);
        reset.setOnClickListener(v -> {
            draft = WidgetConfig.builder(draft.appWidgetId, draft.kind).build();
            sync();
        });
        MaterialButton done = findViewById(R.id.widget_config_done);
        done.setOnClickListener(v -> commit(done));

        building = false;
        sync();
    }

    /** Pushes the draft into every control. Listeners stay inert while this runs. */
    private void sync() {
        building = true;
        try {
            for (int i = 0; i < themeChips.size(); i++) {
                themeChips.get(i).setChecked(themes.get(i).id.equals(draft.themeId));
            }
            dateSwitch.setChecked(draft.showDate);
            weekdaySwitch.setChecked(draft.showWeekday);
            lunarSwitch.setChecked(draft.showLunar);
            locationSwitch.setChecked(draft.showLocation);
            descriptionSwitch.setChecked(draft.showWeatherDescription);
            secondsSwitch.setChecked(draft.showSeconds);
            darkTextSwitch.setChecked(draft.darkText);

            timeFormatField.setText(timeFormatLabels[timeFormatIndex()], false);
            tapField.setText(tapLabels[Math.max(0, Arrays.asList(TAP_ACTIONS).indexOf(draft.tapAction))], false);
            fontField.setText(fontLabels[WidgetFontRegistry.indexOf(draft.fontId)], false);

            opacitySlider.setValue(draft.backgroundAlpha);
            opacityValue.setText(percentLabel(draft.backgroundAlpha * 100 / 255));
            scaleSlider.setValue(Math.round(draft.textScale * 100f));
            scaleValue.setText(percentLabel(Math.round(draft.textScale * 100f)));

            boolean clock = draft.kind == WidgetKind.DIGITAL || draft.kind == WidgetKind.WEATHER;
            setRowVisible(lunarSwitch, draft.kind != WidgetKind.ANALOG);
            setRowVisible(locationSwitch, draft.kind == WidgetKind.WEATHER);
            setRowVisible(descriptionSwitch, draft.kind == WidgetKind.WEATHER);
            setRowVisible(secondsSwitch, clock);
            setRowVisible(timeFormatBox, clock);
            setRowVisible(darkTextSwitch, "transparent.clean".equals(draft.themeId));
        } finally {
            building = false;
        }
        preview.show(draft);
    }

    private int timeFormatIndex() {
        return draft.useSystemTimeFormat ? 0 : draft.use24Hour ? 1 : 2;
    }

    private void commit(MaterialButton done) {
        done.setEnabled(false);
        WidgetConfig saved = draft.toBuilder().updatedAt(System.currentTimeMillis()).build();
        WidgetUpdateCoordinator.execute(() -> {
            if (WidgetUpdateCoordinator.kindFor(this, saved.appWidgetId) == null) {
                runOnUiThread(this::finish);
                return;
            }
            new WidgetConfigStore(this).save(saved);
            WidgetUpdateCoordinator.updateOne(this, saved.appWidgetId);
            WidgetUpdateCoordinator.reconcile(this);
            runOnUiThread(() -> {
                setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, saved.appWidgetId));
                finish();
            });
        });
    }

    private void applySystemBarInsets(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void section(int titleRes) {
        TextView title = new TextView(this);
        TextViewCompat.setTextAppearance(title, R.style.TextAppearance_ClockMods_Config_Section);
        title.setText(titleRes);
        title.setPadding(0, dp(20), 0, dp(8));
        content.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private MaterialSwitch toggle(int labelRes, int id, Function<Boolean, WidgetConfig> change) {
        MaterialSwitch control = new MaterialSwitch(this);
        control.setId(id);
        control.setText(labelRes);
        control.setMinHeight(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH, WRAP);
        params.topMargin = dp(2);
        content.addView(control, params);
        control.setOnCheckedChangeListener((v, value) -> {
            if (building) return;
            draft = change.apply(value);
            sync();
        });
        return control;
    }

    private TextInputLayout drawer(String[] items, int hintRes, IntConsumer change) {
        // Inflated from widget_config_dropdown.xml, which carries the documented Material 3 style
        //   @style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu
        // (material-components-android, Menus: "Exposed dropdown menu"). The style has to come from
        // XML: TextInputLayout only exposes constructors that take a default *attribute*, never a
        // default *style*, and passing the exposed-dropdown attribute leaves it on the plain
        // text-field look, i.e. an underlined filled box instead of an outlined selector.
        TextInputLayout box = (TextInputLayout) getLayoutInflater()
                .inflate(R.layout.widget_config_dropdown, content, false);
        box.setHint(hintRes);
        MaterialAutoCompleteTextView field = box.findViewById(R.id.widget_config_dropdown_field);
        // Each inflation starts with the same XML ID; keep accessibility/state lookup unambiguous.
        field.setId(View.generateViewId());
        field.setThreshold(0);
        field.setSimpleItems(items);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH, WRAP);
        params.topMargin = dp(12);
        content.addView(box, params);
        // A non-editable dropdown only opens from the end icon by default; opening the menu from
        // the whole field is the behaviour users expect from a list selector.
        field.setOnClickListener(v -> field.showDropDown());
        field.setOnItemClickListener((parent, view, position, rowId) -> {
            if (building) return;
            change.accept(position);
            sync();
        });
        return box;
    }

    private Slider slider(int from, int to, LabelFormatter format, IntConsumer change) {
        Slider slider = new Slider(this);
        slider.setValueFrom(from);
        slider.setValueTo(to);
        // No stepSize on purpose: Material draws a tick mark per step, and a 0..255 alpha range
        // would turn the track into a dotted line. Values are rounded to whole numbers instead.
        slider.setLabelFormatter(format);
        slider.addOnChangeListener((v, value, fromUser) -> {
            if (building) return;
            change.accept(Math.round(value));
            preview.show(draft);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH, WRAP);
        content.addView(slider, params);
        return slider;
    }

    /**
     * Heading row shared by both sliders: the section title on the left and the live value on the
     * right, on the same baseline, so the value reads as part of the heading instead of floating.
     */
    private TextView sliderHeader(int titleRes) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(20), 0, 0);
        TextView title = new TextView(this);
        TextViewCompat.setTextAppearance(title, R.style.TextAppearance_ClockMods_Config_Section);
        title.setText(titleRes);
        TextView value = new TextView(this);
        TextViewCompat.setTextAppearance(value, R.style.TextAppearance_ClockMods_Config_Value);
        row.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(value, new LinearLayout.LayoutParams(WRAP, WRAP));
        content.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
        return value;
    }

    private void setRowVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private String percentLabel(int percent) {
        return percent + "%";
    }

    /**
     * Preview box per widget kind. Sizes follow the recommended cell footprint (4x1, 4x2, 2x2) so
     * the draft is shown in the same size class the Launcher will use, and therefore with the same
     * set of modules visible.
     */
    private int[] previewBox() {
        int width;
        int height;
        switch (draft.kind) {
            case DIGITAL: width = 340; height = 96; break;
            case WEATHER: width = 340; height = 156; break;
            case CALENDAR: width = 220; height = 156; break;
            default: width = 176; height = 176; break;
        }
        int available = getResources().getConfiguration().screenWidthDp - 40;
        if (available > 0) width = Math.min(width, available);
        return new int[]{width, height};
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
