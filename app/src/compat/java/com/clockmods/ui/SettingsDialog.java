package com.clockmods.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.time.RegionTimeZones;
import com.clockmods.weather.WeatherLocationCatalog;

public class SettingsDialog extends Dialog {
    public interface Listener {
        void onColorApplied(int color);
        void onImageModeApplied();
        void onChooseImage();
        void onFontSettingsApplied();
        void onDismissed();
    }

    private interface SwatchListener {
        void onPicked(int color);
    }

    private static final int MIN_FONT_PERCENT = 20;
    private static final int MAX_FONT_PERCENT = 100;
    private static final int FONT_PERCENT_RANGE = MAX_FONT_PERCENT - MIN_FONT_PERCENT;

    private static final int MODE_COLOR_ID = 30003;
    private static final int MODE_IMAGE_ID = 30004;

    // ---- Dark theme palette ----
    private static final int COLOR_SURFACE = 0xFF1B1D22;
    private static final int COLOR_TITLE = 0xFFF3F4F6;
    private static final int COLOR_SECTION = 0xFF60A5FA;
    private static final int COLOR_PRIMARY_TEXT = 0xFFE5E7EB;
    private static final int COLOR_SECONDARY_TEXT = 0xFF9CA3AF;
    private static final int COLOR_ACCENT = 0xFF60A5FA;
    private static final int COLOR_SWATCH_STROKE = 0x33FFFFFF;

    private static final int[] BACKGROUND_COLORS = {
            0xFF101418, 0xFF1F2933, 0xFF173F5F, 0xFF20639B,
            0xFF2A6F4E, 0xFF7A3E48, 0xFF8C5B2D, 0xFFF4F1EA
    };
    private static final int[] TEXT_COLORS = {
            0xFFFFFFFF, 0xFFF4F1EA, 0xFFF4C430, 0xFF4FB0E5,
            0xFF3CB371, 0xFFE07A9A, 0xFFE05A5A, 0xFF111827
    };

    private final BackgroundRepository repository;
    private final Listener listener;
    private final ColorPickerView backgroundPicker;
    private final View backgroundPreview;
    private final RadioButton colorMode;
    private final RadioButton imageMode;
    private final View colorControls;
    private final View imageControls;
    private final Switch dimBackgroundSwitch;
    private final Switch scheduleDimBackgroundSwitch;
    private final View dimScheduleControls;
    private final Button dimStartButton;
    private final Button dimEndButton;

    private final ColorPickerView timeColorPicker;
    private final ColorPickerView dateColorPicker;
    private final SeekBar timeSizeBar;
    private final SeekBar dateSizeBar;
    private final TextView timeSizeValue;
    private final TextView dateSizeValue;
    private final Switch blinkColonSwitch;
    private final Switch animateTimeChangesSwitch;
    private final Switch boldTextSwitch;
    private final Spinner fontFamilySpinner;
    private final Switch showSecondsSwitch;
    private final Switch smallSecondsSwitch;
    private final Switch showLunarSwitch;
    private final Switch statusIconsSwitch;
    private final Switch use24HourSwitch;
    private final Switch clockUseEnglishSwitch;
    private final Switch networkTimeSwitch;
    private final View functionLockedControls;
    private final RadioGroup syncIntervalGroup;
    private final Button regionButton;
    private final RadioButton sync30Min;
    private final RadioButton sync1Hour;
    private final RadioButton sync6Hour;
    private final RadioButton sync1Day;
    private final Switch weatherSwitch;
    private final Spinner weatherLocationModeSpinner;
    private final Button weatherLocationButton;
    private final Spinner weatherIntervalSpinner;
    private String selectedWeatherLocationId;
    private String selectedWeatherProvince;
    private String selectedWeatherCity;
    private String selectedWeatherDistrict;
    private int selectedRegionIndex;
    private int dimStartMinutes;
    private int dimEndMinutes;
    private int timeColor;
    private int dateColor;

    public SettingsDialog(Context context, BackgroundRepository repository, Listener listener) {
        super(context);
        this.repository = repository;
        this.listener = listener;
        this.timeColor = repository.getTimeColor();
        this.dateColor = repository.getDateColor();
        this.dimStartMinutes = repository.getDimStartMinutes();
        this.dimEndMinutes = repository.getDimEndMinutes();
        this.selectedWeatherLocationId = repository.getWeatherLocationId();
        this.selectedWeatherProvince = repository.getWeatherProvince();
        this.selectedWeatherCity = repository.getWeatherCity();
        this.selectedWeatherDistrict = repository.getWeatherDistrict();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout dialogRoot = new LinearLayout(context);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setBackgroundColor(COLOR_SURFACE);

        int padding = dp(20);
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(padding, dp(8), padding, 0);

        TextView title = new TextView(context);
        title.setText(R.string.background_settings);
        title.setTextColor(COLOR_TITLE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button cancel = new Button(context);
        cancel.setText(R.string.cancel);
        tintButton(cancel);
        cancel.setOnClickListener(view -> cancel());
        header.addView(cancel, new LinearLayout.LayoutParams(dp(76), dp(44)));

        Button apply = new Button(context);
        apply.setText(R.string.apply);
        tintButton(apply);
        apply.setOnClickListener(view -> applySelection());
        header.addView(apply, new LinearLayout.LayoutParams(dp(76), dp(44)));
        dialogRoot.addView(header, matchWrap(dp(56)));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, 0, padding, dp(18));
        content.setBackgroundColor(COLOR_SURFACE);

        // ---- Segmented tab header (样式 / 功能) ----
        final SegmentedSelector boardSelector = new SegmentedSelector(context,
            new CharSequence[] {
                context.getString(R.string.tab_style),
                context.getString(R.string.tab_function)
            }, COLOR_ACCENT, 0xFF252830, Color.WHITE, COLOR_SECONDARY_TEXT);
        LinearLayout fixedTabs = new LinearLayout(context);
        fixedTabs.setPadding(padding, 0, padding, 0);
        fixedTabs.addView(boardSelector, topMargin(matchWrap(dp(46)), dp(12)));
        dialogRoot.addView(fixedTabs, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- Style board ----
        final LinearLayout styleContent = new LinearLayout(context);
        styleContent.setOrientation(LinearLayout.VERTICAL);

        styleContent.addView(createSectionLabel(context, R.string.background_settings_group),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(6)));

        RadioGroup modeGroup = new RadioGroup(context);
        modeGroup.setOrientation(RadioGroup.HORIZONTAL);
        colorMode = new RadioButton(context);
        colorMode.setText(R.string.solid_color);
        colorMode.setTextColor(COLOR_PRIMARY_TEXT);
        colorMode.setId(MODE_COLOR_ID);
        tintCompoundButton(colorMode);
        imageMode = new RadioButton(context);
        imageMode.setText(R.string.background_image);
        imageMode.setTextColor(COLOR_PRIMARY_TEXT);
        imageMode.setId(MODE_IMAGE_ID);
        tintCompoundButton(imageMode);
        modeGroup.addView(colorMode, new RadioGroup.LayoutParams(0, dp(46), 1f));
        modeGroup.addView(imageMode, new RadioGroup.LayoutParams(0, dp(46), 1f));
        styleContent.addView(modeGroup, topMargin(matchWrap(dp(48)), dp(2)));

        LinearLayout colorControlsLayout = new LinearLayout(context);
        colorControlsLayout.setOrientation(LinearLayout.VERTICAL);
        colorControls = colorControlsLayout;

        backgroundPreview = createSwatchPreview(context, repository.getCurrentColor());
        colorControlsLayout.addView(backgroundPreview, topMargin(matchWrap(dp(34)), dp(10)));

        backgroundPicker = new ColorPickerView(context);
        backgroundPicker.setColor(repository.getCurrentColor());
        backgroundPicker.setOnColorChangedListener(color -> {
            setSwatchColor(backgroundPreview, color);
            colorMode.setChecked(true);
        });
        colorControlsLayout.addView(createColorSwatches(context, BACKGROUND_COLORS, color -> {
            backgroundPicker.setColor(color);
            setSwatchColor(backgroundPreview, color);
            colorMode.setChecked(true);
        }), topMargin(matchWrap(dp(50)), dp(12)));
        addAdvancedPicker(context, colorControlsLayout, backgroundPicker);
        styleContent.addView(colorControlsLayout, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout imageControlsLayout = new LinearLayout(context);
        imageControlsLayout.setOrientation(LinearLayout.VERTICAL);
        imageControls = imageControlsLayout;
        imageControlsLayout.addView(createImagePreview(context), topMargin(matchWrap(dp(180)), dp(10)));

        Button chooseImage = new Button(context);
        chooseImage.setText(R.string.choose_image);
        tintButton(chooseImage);
        chooseImage.setOnClickListener(view -> {
            dismiss();
            listener.onChooseImage();
        });
        imageControlsLayout.addView(chooseImage, topMargin(matchWrap(dp(46)), dp(10)));

        dimBackgroundSwitch = createStyleSwitch(context, R.string.dim_background,
            repository.isDimBackground());
        imageControlsLayout.addView(dimBackgroundSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        scheduleDimBackgroundSwitch = createStyleSwitch(context, R.string.schedule_dim_background,
            repository.isScheduleDimBackground());
        imageControlsLayout.addView(scheduleDimBackgroundSwitch, matchWrap(dp(48)));

        TextView dimNote = createSubLabel(context, R.string.dim_schedule_note);
        dimNote.setTextColor(COLOR_SECONDARY_TEXT);
        imageControlsLayout.addView(dimNote,
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(2)));

        LinearLayout dimTimeRow = new LinearLayout(context);
        dimScheduleControls = dimTimeRow;
        dimStartButton = new Button(context);
        dimEndButton = new Button(context);
        tintButton(dimStartButton);
        tintButton(dimEndButton);
        updateDimTimeLabels();
        dimStartButton.setOnClickListener(view -> showDimTimePicker(true));
        dimEndButton.setOnClickListener(view -> showDimTimePicker(false));
        dimTimeRow.addView(dimStartButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams endTimeParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        endTimeParams.leftMargin = dp(8);
        dimTimeRow.addView(dimEndButton, endTimeParams);
        imageControlsLayout.addView(dimTimeRow, topMargin(matchWrap(dp(50)), dp(8)));
        scheduleDimBackgroundSwitch.setOnCheckedChangeListener(
            (button, checked) -> updateDimScheduleState(checked));
        updateDimScheduleState(scheduleDimBackgroundSwitch.isChecked());
        styleContent.addView(imageControlsLayout, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        modeGroup.setOnCheckedChangeListener((group, checkedId) ->
                updateBackgroundControls(imageMode.isChecked()));

        // Font section
        styleContent.addView(createSectionLabel(context, R.string.font_settings_group),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(18)));

        styleContent.addView(createSubLabel(context, R.string.font_family),
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(8)));
        fontFamilySpinner = createFontFamilySpinner(context, repository.getFontFamily());
        styleContent.addView(fontFamilySpinner, topMargin(matchWrap(dp(48)), dp(4)));

        styleContent.addView(createSubLabel(context, R.string.time_font_settings),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(8)));
        timeSizeValue = new TextView(context);
        timeSizeBar = new SeekBar(context);
        styleContent.addView(createSizeRow(context, R.string.font_size, timeSizeBar, timeSizeValue),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(4)));
        configureSizeBar(timeSizeBar, timeSizeValue, repository.getTimeFontScale());
        styleContent.addView(createSubLabel(context, R.string.font_color),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(10)));
        timeColorPicker = new ColorPickerView(context);
        timeColorPicker.setColor(timeColor);
        timeColorPicker.setOnColorChangedListener(color -> timeColor = color);
        styleContent.addView(createColorSwatches(context, TEXT_COLORS, color -> {
            timeColor = color;
            timeColorPicker.setColor(color);
        }), topMargin(matchWrap(dp(50)), dp(4)));
        addAdvancedPicker(context, styleContent, timeColorPicker);

        blinkColonSwitch = createStyleSwitch(context, R.string.blink_colon, repository.isBlinkColon());
        styleContent.addView(blinkColonSwitch, topMargin(matchWrap(dp(48)), dp(10)));
        animateTimeChangesSwitch = createStyleSwitch(context, R.string.animate_time_changes,
            repository.isAnimateTimeChanges());
        styleContent.addView(animateTimeChangesSwitch, matchWrap(dp(48)));
        boldTextSwitch = createStyleSwitch(context, R.string.bold_text, repository.isBoldText());
        styleContent.addView(boldTextSwitch, matchWrap(dp(48)));
        showSecondsSwitch = createStyleSwitch(context, R.string.show_seconds, repository.isShowSeconds());
        styleContent.addView(showSecondsSwitch, matchWrap(dp(48)));
        smallSecondsSwitch = createStyleSwitch(context, R.string.small_seconds, repository.isSmallSeconds());
        styleContent.addView(smallSecondsSwitch, matchWrap(dp(48)));
        showLunarSwitch = createStyleSwitch(context, R.string.show_lunar, repository.isShowLunar());
        styleContent.addView(showLunarSwitch, matchWrap(dp(48)));
        showSecondsSwitch.setOnCheckedChangeListener(
            (button, checked) -> updateSmallSecondsState(checked));
        updateSmallSecondsState(showSecondsSwitch.isChecked());

        styleContent.addView(createSubLabel(context, R.string.date_font_settings),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(16)));
        dateSizeValue = new TextView(context);
        dateSizeBar = new SeekBar(context);
        styleContent.addView(createSizeRow(context, R.string.font_size, dateSizeBar, dateSizeValue),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(4)));
        configureSizeBar(dateSizeBar, dateSizeValue, repository.getDateFontScale());
        styleContent.addView(createSubLabel(context, R.string.font_color),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(10)));
        dateColorPicker = new ColorPickerView(context);
        dateColorPicker.setColor(dateColor);
        dateColorPicker.setOnColorChangedListener(color -> dateColor = color);
        styleContent.addView(createColorSwatches(context, TEXT_COLORS, color -> {
            dateColor = color;
            dateColorPicker.setColor(color);
        }), topMargin(matchWrap(dp(50)), dp(4)));
        addAdvancedPicker(context, styleContent, dateColorPicker);

        // Status bar section
        styleContent.addView(createSectionLabel(context, R.string.status_settings_group),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(18)));
        statusIconsSwitch = new Switch(context);
        statusIconsSwitch.setText(R.string.show_status_icons);
        statusIconsSwitch.setTextColor(COLOR_PRIMARY_TEXT);
        tintCompoundButton(statusIconsSwitch);
        statusIconsSwitch.setChecked(repository.isShowStatusIcons());
        styleContent.addView(statusIconsSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        // Reset default lives in the style board.
        Button reset = new Button(context);
        reset.setText(R.string.reset_default);
        tintButton(reset);
        reset.setOnClickListener(view -> restoreDefaults());
        styleContent.addView(reset, topMargin(matchWrap(dp(48)), dp(18)));

        // ---- Function board ----
        final LinearLayout functionContent = new LinearLayout(context);
        functionContent.setOrientation(LinearLayout.VERTICAL);
        functionContent.setVisibility(View.GONE);

        functionContent.addView(createSectionLabel(context, R.string.time_settings_group),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(6)));
        use24HourSwitch = new Switch(context);
        use24HourSwitch.setText(R.string.use_24_hour);
        use24HourSwitch.setTextColor(COLOR_PRIMARY_TEXT);
        tintCompoundButton(use24HourSwitch);
        use24HourSwitch.setChecked(repository.isUse24Hour());
        functionContent.addView(use24HourSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        networkTimeSwitch = new Switch(context);
        networkTimeSwitch.setText(R.string.use_network_time);
        networkTimeSwitch.setTextColor(COLOR_PRIMARY_TEXT);
        tintCompoundButton(networkTimeSwitch);
        networkTimeSwitch.setChecked(repository.isUseNetworkTime());
        functionContent.addView(networkTimeSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        LinearLayout locked = new LinearLayout(context);
        locked.setOrientation(LinearLayout.VERTICAL);
        functionLockedControls = locked;

        locked.addView(createSubLabel(context, R.string.sync_interval),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(10)));
        syncIntervalGroup = new RadioGroup(context);
        syncIntervalGroup.setOrientation(RadioGroup.VERTICAL);
        sync30Min = createSyncOption(context, R.string.sync_interval_30min);
        sync1Hour = createSyncOption(context, R.string.sync_interval_1hour);
        sync6Hour = createSyncOption(context, R.string.sync_interval_6hour);
        sync1Day = createSyncOption(context, R.string.sync_interval_1day);
        syncIntervalGroup.addView(sync30Min, matchWrap(dp(42)));
        syncIntervalGroup.addView(sync1Hour, matchWrap(dp(42)));
        syncIntervalGroup.addView(sync6Hour, matchWrap(dp(42)));
        syncIntervalGroup.addView(sync1Day, matchWrap(dp(42)));
        checkSyncOption(repository.getSyncIntervalMinutes());
        locked.addView(syncIntervalGroup, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(2)));

        functionContent.addView(locked, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        functionContent.addView(createSectionLabel(context, R.string.region_settings_group),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(18)));
        selectedRegionIndex = RegionTimeZones.indexOfZoneId(repository.getTimeZoneId());
        regionButton = new Button(context);
        regionButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tintButton(regionButton);
        regionButton.setText(regionSummary());
        regionButton.setOnClickListener(view -> showRegionChooser());
        functionContent.addView(regionButton, topMargin(matchWrap(dp(48)), dp(8)));

        functionContent.addView(createSectionLabel(context, R.string.clock_language_settings_group),
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(18)));
        clockUseEnglishSwitch = new Switch(context);
        clockUseEnglishSwitch.setText(R.string.clock_use_english);
        clockUseEnglishSwitch.setTextColor(COLOR_PRIMARY_TEXT);
        tintCompoundButton(clockUseEnglishSwitch);
        clockUseEnglishSwitch.setChecked(repository.isClockUseEnglish());
        functionContent.addView(clockUseEnglishSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        functionContent.addView(createSectionLabel(context, R.string.weather_settings_group),
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(18)));
        weatherSwitch = new Switch(context);
        weatherSwitch.setText(R.string.show_weather);
        weatherSwitch.setTextColor(COLOR_PRIMARY_TEXT);
        tintCompoundButton(weatherSwitch);
        weatherSwitch.setChecked(repository.isWeatherEnabled());
        functionContent.addView(weatherSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        functionContent.addView(createSubLabel(context, R.string.weather_location),
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(4)));
        weatherLocationModeSpinner = new Spinner(context);
        weatherLocationModeSpinner.setAdapter(new ArrayAdapter<>(context,
            android.R.layout.simple_spinner_dropdown_item, new String[] {
                context.getString(R.string.weather_location_automatic),
                context.getString(R.string.weather_location_manual)
            }));
        weatherLocationModeSpinner.setSelection(ClockPreferences.WEATHER_LOCATION_MANUAL.equals(
            repository.getWeatherLocationMode()) ? 1 : 0);
        functionContent.addView(weatherLocationModeSpinner, topMargin(matchWrap(dp(48)), dp(4)));
        weatherLocationButton = new Button(context);
        weatherLocationButton.setTextColor(COLOR_PRIMARY_TEXT);
        weatherLocationButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tintButton(weatherLocationButton);
        updateWeatherLocationSummary();
        weatherLocationButton.setOnClickListener(view -> WeatherLocationChooser.show(context,
            selectedWeatherProvince, selectedWeatherCity, selectedWeatherDistrict,
            new WeatherLocationChooser.Listener() {
                @Override public void onLocationSelected(WeatherLocationCatalog.LocationEntry location) {
                    selectedWeatherLocationId = location.locationId;
                    selectedWeatherProvince = location.province;
                    selectedWeatherCity = location.city;
                    selectedWeatherDistrict = location.district;
                    updateWeatherLocationSummary();
                }
            }));
        functionContent.addView(weatherLocationButton, topMargin(matchWrap(dp(52)), dp(4)));
        functionContent.addView(createSubLabel(context, R.string.weather_update_interval),
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(8)));
        weatherIntervalSpinner = new Spinner(context);
        weatherIntervalSpinner.setAdapter(new ArrayAdapter<>(context,
            android.R.layout.simple_spinner_dropdown_item, new String[] {
                context.getString(R.string.weather_interval_10min),
                context.getString(R.string.weather_interval_30min),
                context.getString(R.string.weather_interval_1hour),
                context.getString(R.string.weather_interval_3hour),
                context.getString(R.string.weather_interval_6hour),
                context.getString(R.string.weather_interval_12hour)
            }));
        weatherIntervalSpinner.setSelection(weatherIntervalIndex(repository.getWeatherIntervalMinutes()));
        functionContent.addView(weatherIntervalSpinner, topMargin(matchWrap(dp(48)), dp(4)));
        weatherSwitch.setOnCheckedChangeListener((button, checked) -> updateWeatherState(checked));
        weatherLocationModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateWeatherState(weatherSwitch.isChecked());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        updateWeatherState(weatherSwitch.isChecked());

        networkTimeSwitch.setOnCheckedChangeListener((button, checked) -> updateFunctionLockedState(checked));
        updateFunctionLockedState(networkTimeSwitch.isChecked());

        // ---- Boards container ----
        content.addView(styleContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(functionContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        boardSelector.setOnSelectionChangedListener(index -> {
            boolean style = index == 0;
            styleContent.setVisibility(style ? View.VISIBLE : View.GONE);
            functionContent.setVisibility(style ? View.GONE : View.VISIBLE);
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_SURFACE);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(12);
        dialogRoot.addView(scrollView, scrollParams);
        setContentView(dialogRoot);

        boolean usingImage = ClockPreferences.MODE_IMAGE.equals(repository.getBackgroundMode());
        imageMode.setChecked(usingImage);
        colorMode.setChecked(!usingImage);
        updateBackgroundControls(usingImage);
        setOnDismissListener(dialog -> listener.onDismissed());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = Math.min(getContext().getResources().getDisplayMetrics().heightPixels, dp(760));
            window.setAttributes(params);
        }
    }

    private TextView createSectionLabel(Context context, int textRes) {
        TextView label = new TextView(context);
        label.setText(textRes);
        label.setTextColor(COLOR_SECTION);
        label.setTextSize(15);
        return label;
    }

    private TextView createSubLabel(Context context, int textRes) {
        TextView label = new TextView(context);
        label.setText(textRes);
        label.setTextColor(COLOR_PRIMARY_TEXT);
        label.setTextSize(14);
        return label;
    }

    private Switch createStyleSwitch(Context context, int textRes, boolean checked) {
        Switch control = new Switch(context);
        control.setText(textRes);
        control.setTextColor(COLOR_PRIMARY_TEXT);
        tintCompoundButton(control);
        control.setChecked(checked);
        return control;
    }

    private void updateSmallSecondsState(boolean showSeconds) {
        smallSecondsSwitch.setEnabled(showSeconds);
        smallSecondsSwitch.setAlpha(showSeconds ? 1f : 0.4f);
    }

    private void updateWeatherState(boolean enabled) {
        weatherLocationModeSpinner.setEnabled(enabled);
        weatherLocationModeSpinner.setAlpha(enabled ? 1f : 0.4f);
        boolean manual = enabled && weatherLocationModeSpinner.getSelectedItemPosition() == 1;
        weatherLocationButton.setVisibility(manual ? View.VISIBLE : View.GONE);
        weatherLocationButton.setEnabled(manual);
        weatherIntervalSpinner.setEnabled(enabled);
        weatherIntervalSpinner.setAlpha(enabled ? 1f : 0.4f);
    }

    private void updateWeatherLocationSummary() {
        weatherLocationButton.setText(selectedWeatherLocationId.length() == 0
            ? getContext().getString(R.string.weather_location_not_selected)
            : selectedWeatherProvince + " / " + selectedWeatherCity + " / " + selectedWeatherDistrict);
    }

    private static int weatherIntervalIndex(int minutes) {
        int[] values = {10, 30, 60, 180, 360, 720};
        for (int index = 0; index < values.length; index++) if (values[index] == minutes) return index;
        return 1;
    }

    private static int weatherIntervalMinutes(int index) {
        int[] values = {10, 30, 60, 180, 360, 720};
        return index >= 0 && index < values.length ? values[index] : 30;
    }

    private View createSizeRow(Context context, int labelRes, SeekBar bar, TextView valueLabel) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText(labelRes);
        label.setTextColor(COLOR_SECONDARY_TEXT);
        label.setTextSize(13);
        row.addView(label, new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));

        bar.setMax(FONT_PERCENT_RANGE);
        tintSeekBar(bar);
        row.addView(bar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        valueLabel.setTextColor(COLOR_PRIMARY_TEXT);
        valueLabel.setTextSize(13);
        valueLabel.setGravity(Gravity.END);
        row.addView(valueLabel, new LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void configureSizeBar(SeekBar bar, TextView valueLabel, float currentScale) {
        bar.setProgress(scaleToProgress(currentScale));
        updateSizeLabel(valueLabel, bar.getProgress());
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSizeLabel(valueLabel, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateSizeLabel(TextView valueLabel, int progress) {
        valueLabel.setText(getContext().getString(R.string.font_size_percent, progressToPercent(progress)));
    }

    private View createSwatchPreview(Context context, int color) {
        View preview = new View(context);
        preview.setBackgroundDrawable(createRoundedDrawable(color));
        return preview;
    }

    private View createImagePreview(Context context) {
        ImageView preview = new ImageView(context);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackgroundColor(0xFF101418);
        try {
            Bitmap bitmap = repository.loadImage(dp(600), dp(180));
            if (bitmap != null) {
                preview.setImageBitmap(bitmap);
            }
        } catch (java.io.IOException ignored) {
            preview.setImageDrawable(null);
        }
        return preview;
    }

    private void updateBackgroundControls(boolean usingImage) {
        colorControls.setVisibility(usingImage ? View.GONE : View.VISIBLE);
        imageControls.setVisibility(usingImage ? View.VISIBLE : View.GONE);
    }

    private void showDimTimePicker(final boolean start) {
        int minutes = start ? dimStartMinutes : dimEndMinutes;
        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            if (start) {
                dimStartMinutes = hourOfDay * 60 + minute;
            } else {
                dimEndMinutes = hourOfDay * 60 + minute;
            }
            updateDimTimeLabels();
        }, minutes / 60, minutes % 60, true).show();
    }

    private void updateDimTimeLabels() {
        dimStartButton.setText(getContext().getString(R.string.dim_start_time)
                + " " + formatMinutes(dimStartMinutes));
        dimEndButton.setText(getContext().getString(R.string.dim_end_time)
                + " " + formatMinutes(dimEndMinutes));
    }

    private void updateDimScheduleState(boolean enabled) {
        dimScheduleControls.setAlpha(enabled ? 1f : 0.4f);
        setViewTreeEnabled(dimScheduleControls, enabled);
    }

    private static String formatMinutes(int minutes) {
        return String.format(java.util.Locale.CHINA, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private void setSwatchColor(View preview, int color) {
        preview.setBackgroundDrawable(createRoundedDrawable(color));
    }

    private GradientDrawable createRoundedDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(6));
        drawable.setStroke(dp(1), COLOR_SWATCH_STROKE);
        return drawable;
    }

    private void tintButton(Button button) {
        button.setTextColor(COLOR_TITLE);
        button.getBackground().setColorFilter(COLOR_ACCENT, android.graphics.PorterDuff.Mode.MULTIPLY);
    }

    private void tintCompoundButton(android.widget.CompoundButton button) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            button.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        }
    }

    private void tintSeekBar(SeekBar bar) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            android.content.res.ColorStateList accent =
                    android.content.res.ColorStateList.valueOf(COLOR_ACCENT);
            bar.setProgressTintList(accent);
            bar.setThumbTintList(accent);
        }
    }

    private View createColorSwatches(Context context, int[] colors, SwatchListener swatchListener) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (final int color : colors) {
            View swatch = new View(context);
            swatch.setBackgroundDrawable(createRoundedDrawable(color));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
            params.rightMargin = dp(10);
            row.addView(swatch, params);
            swatch.setOnClickListener(view -> swatchListener.onPicked(color));
        }
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(row);
        return scroll;
    }

    private void addAdvancedPicker(Context context, LinearLayout parent, ColorPickerView picker) {
        Switch advancedSwitch = createStyleSwitch(context, R.string.advanced, false);
        picker.setVisibility(View.GONE);
        advancedSwitch.setOnCheckedChangeListener((button, checked) ->
                picker.setVisibility(checked ? View.VISIBLE : View.GONE));
        parent.addView(advancedSwitch, topMargin(matchWrap(dp(48)), dp(4)));
        parent.addView(picker, matchWrap(dp(128)));
    }

    private RadioButton createTabButton(Context context, int textRes) {
        RadioButton tab = new RadioButton(context);
        tab.setButtonDrawable(null);
        tab.setText(textRes);
        tab.setGravity(Gravity.CENTER);
        tab.setTextColor(COLOR_PRIMARY_TEXT);
        tab.setTextSize(15);
        return tab;
    }

    private RadioButton createSyncOption(Context context, int textRes) {
        RadioButton option = new RadioButton(context);
        option.setText(textRes);
        option.setTextColor(COLOR_PRIMARY_TEXT);
        option.setTextSize(14);
        tintCompoundButton(option);
        return option;
    }

    private void checkSyncOption(int minutes) {
        switch (minutes) {
            case 30:
                sync30Min.setChecked(true);
                break;
            case 360:
                sync6Hour.setChecked(true);
                break;
            case 1440:
                sync1Day.setChecked(true);
                break;
            case 60:
            default:
                sync1Hour.setChecked(true);
                break;
        }
    }

    private int selectedSyncMinutes() {
        if (sync30Min.isChecked()) {
            return 30;
        }
        if (sync6Hour.isChecked()) {
            return 360;
        }
        if (sync1Day.isChecked()) {
            return 1440;
        }
        return 60;
    }

    private String regionSummary() {
        return getContext().getString(R.string.region_time_zone) + ": "
                + RegionTimeZones.DISPLAY_NAMES[selectedRegionIndex];
    }

    private void updateFunctionLockedState(boolean enabled) {
        functionLockedControls.setAlpha(enabled ? 1f : 0.4f);
        setViewTreeEnabled(functionLockedControls, enabled);
    }

    private static void setViewTreeEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setViewTreeEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    private void showRegionChooser() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.select_region)
                .setSingleChoiceItems(RegionTimeZones.DISPLAY_NAMES, selectedRegionIndex,
                        (DialogInterface dialog, int which) -> {
                            selectedRegionIndex = which;
                            regionButton.setText(regionSummary());
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restoreDefaults() {
        int defaultBackground = ClockPreferences.DEFAULT_BACKGROUND_COLOR;
        int defaultTextColor = ClockPreferences.DEFAULT_TEXT_COLOR;

        colorMode.setChecked(true);
        imageMode.setChecked(false);
        backgroundPicker.setColor(defaultBackground);
        setSwatchColor(backgroundPreview, defaultBackground);

        timeColor = defaultTextColor;
        dateColor = defaultTextColor;
        timeColorPicker.setColor(defaultTextColor);
        dateColorPicker.setColor(defaultTextColor);

        timeSizeBar.setProgress(scaleToProgress(ClockPreferences.DEFAULT_TIME_FONT_SCALE));
        updateSizeLabel(timeSizeValue, timeSizeBar.getProgress());
        dateSizeBar.setProgress(scaleToProgress(ClockPreferences.DEFAULT_DATE_FONT_SCALE));
        updateSizeLabel(dateSizeValue, dateSizeBar.getProgress());

        statusIconsSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_STATUS_ICONS);
        blinkColonSwitch.setChecked(ClockPreferences.DEFAULT_BLINK_COLON);
        animateTimeChangesSwitch.setChecked(ClockPreferences.DEFAULT_ANIMATE_TIME_CHANGES);
        boldTextSwitch.setChecked(ClockPreferences.DEFAULT_BOLD_TEXT);
        fontFamilySpinner.setSelection(0);
        showSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_SECONDS);
        smallSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SMALL_SECONDS);
        showLunarSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_LUNAR);
        use24HourSwitch.setChecked(ClockPreferences.DEFAULT_USE_24_HOUR);
        clockUseEnglishSwitch.setChecked(ClockPreferences.DEFAULT_CLOCK_USE_ENGLISH);
        weatherSwitch.setChecked(ClockPreferences.DEFAULT_WEATHER_ENABLED);
        weatherLocationModeSpinner.setSelection(0);
        selectedWeatherLocationId = "";
        selectedWeatherProvince = "";
        selectedWeatherCity = "";
        selectedWeatherDistrict = "";
        updateWeatherLocationSummary();
        weatherIntervalSpinner.setSelection(weatherIntervalIndex(
            ClockPreferences.DEFAULT_WEATHER_INTERVAL_MINUTES));
        dimBackgroundSwitch.setChecked(ClockPreferences.DEFAULT_DIM_BACKGROUND);
        scheduleDimBackgroundSwitch.setChecked(ClockPreferences.DEFAULT_SCHEDULE_DIM_BACKGROUND);
        dimStartMinutes = ClockPreferences.DEFAULT_DIM_START_MINUTES;
        dimEndMinutes = ClockPreferences.DEFAULT_DIM_END_MINUTES;
        updateDimTimeLabels();

        Toast.makeText(getContext(), R.string.reset_default, Toast.LENGTH_SHORT).show();
    }

    private void applySelection() {
        boolean manualWeather = weatherLocationModeSpinner.getSelectedItemPosition() == 1;
        if (weatherSwitch.isChecked() && manualWeather && selectedWeatherLocationId.length() == 0) {
            Toast.makeText(getContext(), R.string.weather_location_not_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        repository.setTimeFontScale(progressToScale(timeSizeBar.getProgress()));
        repository.setDateFontScale(progressToScale(dateSizeBar.getProgress()));
        repository.setTimeColor(timeColor);
        repository.setDateColor(dateColor);
        repository.setShowStatusIcons(statusIconsSwitch.isChecked());
        repository.setBlinkColon(blinkColonSwitch.isChecked());
        repository.setAnimateTimeChanges(animateTimeChangesSwitch.isChecked());
        repository.setBoldText(boldTextSwitch.isChecked());
        repository.setFontFamily(fontFamilyForIndex(fontFamilySpinner.getSelectedItemPosition()));
        repository.setShowSeconds(showSecondsSwitch.isChecked());
        repository.setSmallSeconds(showSecondsSwitch.isChecked() && smallSecondsSwitch.isChecked());
        repository.setShowLunar(showLunarSwitch.isChecked());
        repository.setUse24Hour(use24HourSwitch.isChecked());
        repository.setClockUseEnglish(clockUseEnglishSwitch.isChecked());
        repository.setDimBackground(dimBackgroundSwitch.isChecked());
        repository.setScheduleDimBackground(scheduleDimBackgroundSwitch.isChecked());
        repository.setDimStartMinutes(dimStartMinutes);
        repository.setDimEndMinutes(dimEndMinutes);
        repository.setUseNetworkTime(networkTimeSwitch.isChecked());
        repository.setSyncIntervalMinutes(selectedSyncMinutes());
        repository.setTimeZoneId(RegionTimeZones.ZONE_IDS[selectedRegionIndex]);
        repository.setWeatherEnabled(weatherSwitch.isChecked());
        repository.setManualWeatherLocation(selectedWeatherLocationId, selectedWeatherProvince,
            selectedWeatherCity, selectedWeatherDistrict);
        repository.setWeatherLocationMode(manualWeather
            ? ClockPreferences.WEATHER_LOCATION_MANUAL : ClockPreferences.WEATHER_LOCATION_AUTOMATIC);
        repository.setWeatherIntervalMinutes(weatherIntervalMinutes(
            weatherIntervalSpinner.getSelectedItemPosition()));
        listener.onFontSettingsApplied();

        if (imageMode.isChecked()) {
            if (!repository.hasImage()) {
                Toast.makeText(getContext(), R.string.select_image_first, Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onImageModeApplied();
        } else {
            listener.onColorApplied(backgroundPicker.getColor());
        }
        dismiss();
    }

    private static int scaleToProgress(float scale) {
        int percent = Math.round(Math.max(ClockPreferences.MIN_FONT_SCALE,
                Math.min(ClockPreferences.MAX_FONT_SCALE, scale)) * 100f);
        return percent - MIN_FONT_PERCENT;
    }

    private static float progressToScale(int progress) {
        return (progress + MIN_FONT_PERCENT) / 100f;
    }

    private static int progressToPercent(int progress) {
        return progress + MIN_FONT_PERCENT;
    }

    private Spinner createFontFamilySpinner(Context context, String family) {
        Spinner spinner = new Spinner(context);
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                new String[] {context.getString(R.string.font_system), "Roboto", "Google Sans"}));
        spinner.setSelection(fontFamilyIndex(family));
        return spinner;
    }

    private static int fontFamilyIndex(String family) {
        if (ClockPreferences.FONT_ROBOTO.equals(family)) return 1;
        if (ClockPreferences.FONT_GOOGLE_SANS.equals(family)) return 2;
        return 0;
    }

    private static String fontFamilyForIndex(int index) {
        if (index == 1) return ClockPreferences.FONT_ROBOTO;
        if (index == 2) return ClockPreferences.FONT_GOOGLE_SANS;
        return ClockPreferences.FONT_SYSTEM;
    }

    private LinearLayout.LayoutParams matchWrap(int height) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    private LinearLayout.LayoutParams topMargin(LinearLayout.LayoutParams params, int margin) {
        params.topMargin = margin;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
