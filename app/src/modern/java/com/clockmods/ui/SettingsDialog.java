package com.clockmods.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.app.TimePickerDialog;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.clockmods.BuildConfig;
import com.clockmods.R;
import com.clockmods.background.AutoStartManager;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.time.RegionTimeZones;
import com.clockmods.weather.WeatherLocationCatalog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class SettingsDialog extends BottomSheetDialog {
    public interface Listener {
        void onColorApplied(int color);
        void onImageModeApplied();
        void onChooseImage();
        void onFontSettingsApplied();
        void onDismissed();
        /** Called when the interface language changed and the host should recreate itself. */
        void onLanguageChanged();
    }

    private interface SwatchListener {
        void onPicked(int color);
    }

    private static final int COLOR_MODE_ID = 10001;
    private static final int IMAGE_MODE_ID = 10002;
    private static final int MIN_FONT_PERCENT = 20;
    private static final int MAX_FONT_PERCENT = 150;
    private static final int MIN_STATUS_ICON_PERCENT =
            Math.round(ClockPreferences.MIN_STATUS_ICON_SCALE * 100f);
    private static final int MAX_STATUS_ICON_PERCENT =
            Math.round(ClockPreferences.MAX_STATUS_ICON_SCALE * 100f);
    // Card grouping: each run of controls between two section labels becomes one rounded card.
    private static final int CARD_RADIUS = 16;   // dp, Material card corner
    private static final int CARD_PADDING = 16;  // dp, inner padding
    private static final int SECTION_GAP = 12;   // dp, gap between cards
    private static final String SECTION_LABEL_TAG = "clockmods:section";
    private final BackgroundRepository repository;
    private final Listener listener;
    private final ColorPickerView backgroundPicker;
    private final View backgroundPreview;
    private final MaterialButtonToggleGroup modeGroup;
    private final View colorControls;
    private final View imageControls;
    private final MaterialSwitch dimBackgroundSwitch;
    private final MaterialSwitch scheduleDimBackgroundSwitch;
    private final View dimScheduleControls;
    private final MaterialButton dimStartButton;
    private final MaterialButton dimEndButton;

    private final ColorPickerView timeColorPicker;
    private final ColorPickerView dateColorPicker;
    private final Slider timeSizeBar;
    private final Slider dateSizeBar;
    private final TextView timeSizeValue;
    private final TextView dateSizeValue;
    private final MaterialSwitch blinkColonSwitch;
    private final MaterialSwitch animateTimeChangesSwitch;
    private final Spinner proTransitionSpinner;
    private final MaterialSwitch portraitStackedSwitch;
    private final MaterialSwitch proHourlyChimeSwitch;
    private final MaterialSwitch proHalfHourChimeSwitch;
    private final MaterialSwitch proHourlyQuietSwitch;
    private final MaterialButton proQuietStartButton;
    private final MaterialButton proQuietEndButton;
    private int proQuietStartMinutes;
    private int proQuietEndMinutes;
    private final MaterialSwitch boldTextSwitch;
    private final Spinner fontFamilySpinner;
    private final MaterialSwitch showSecondsSwitch;
    private final MaterialSwitch smallSecondsSwitch;
    private final MaterialSwitch showLunarSwitch;
    private final MaterialSwitch dateLunarDualLineSwitch;
    private final MaterialSwitch statusIconsSwitch;
    private final Slider statusIconSizeBar;
    private final TextView statusIconSizeValue;
    private final View statusIconSizeControls;
    private final MaterialSwitch use24HourSwitch;
    // Non-Pro flavors show a binary English switch; Pro shows a three-way language toggle instead.
    private MaterialSwitch clockUseEnglishSwitch;
    private MaterialButtonToggleGroup clockLanguageGroup;
    private final MaterialButtonToggleGroup orientationGroup;
    private final MaterialSwitch autoStartSwitch;
    private final MaterialSwitch networkTimeSwitch;
    private final View functionLockedControls;
    private final MaterialButtonToggleGroup syncIntervalGroup;
    private final MaterialButton regionButton;
    private final MaterialSwitch weatherSwitch;
    private final Spinner weatherLocationModeSpinner;
    private final MaterialButton weatherLocationButton;
    private final Spinner weatherIntervalSpinner;
    private final MaterialSwitch weatherDetailedSwitch;
    // Pro-only weather-icon options (null on other flavours).
    private MaterialSwitch weatherIconFillSwitch;
    private MaterialSwitch weatherIconDynamicColorSwitch;
    private final MaterialButtonToggleGroup calendarWeekStartGroup;
    private final MaterialSwitch calendarHighlightWeekendsSwitch;
    // ---- Date-format section (rebuilt when the interface language toggles) ----
    private LinearLayout dateFormatContainer;
    private boolean dateSectionEnglish;
    private final String originalClockLanguage;
    // The language currently chosen in the UI, and the Chinese variant used for date previews.
    private String selectedLanguage;
    private DateFormatter.Lang chineseVariant = DateFormatter.Lang.CHINESE;
    private String pendingDatePatternCn;
    private String pendingDatePatternEn;
    // Pro per-language UI restoration state (coreIndex == cores length means "custom").
    private int proCoreIndexCn;
    private int proCoreIndexEn;
    private int proComboIndexCn;
    private int proComboIndexEn;
    private String proCustomTextCn = "";
    private String proCustomTextEn = "";
    // Widgets for the currently shown language (null in the flavor that does not use them).
    private Spinner dateCoreSpinner;
    private Spinner dateComboSpinner;
    private View dateComboRow;
    private EditText dateCustomInput;
    private View dateCustomRow;
    private TextView dateCustomError;
    private Spinner dateFixedSpinner;
    private TextView datePreviewLabel;
    private final EditText customMessageInput;
    private String selectedWeatherLocationId;
    private String selectedWeatherProvince;
    private String selectedWeatherCity;
    private String selectedWeatherDistrict;
    private double selectedWeatherLatitude = Double.NaN;
    private double selectedWeatherLongitude = Double.NaN;
    private int timeColor;
    private int dateColor;
    private int selectedRegionIndex;
    private int dimStartMinutes;
    private int dimEndMinutes;

    private static final int SYNC_30MIN_ID = 20001;
    private static final int SYNC_1HOUR_ID = 20002;
    private static final int SYNC_6HOUR_ID = 20003;
    private static final int SYNC_1DAY_ID = 20004;

    private static final int ORIENTATION_FOLLOW_ID = 40001;
    private static final int ORIENTATION_PORTRAIT_ID = 40002;
    private static final int ORIENTATION_LANDSCAPE_ID = 40003;

    private static final int CALENDAR_WEEK_START_SUNDAY_ID = 50001;
    private static final int CALENDAR_WEEK_START_MONDAY_ID = 50002;

    private static final int LANGUAGE_SIMPLIFIED_ID = 60001;
    private static final int LANGUAGE_TRADITIONAL_ID = 60002;
    private static final int LANGUAGE_ENGLISH_ID = 60003;

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
        this.selectedWeatherLatitude = repository.getWeatherLatitude();
        this.selectedWeatherLongitude = repository.getWeatherLongitude();
        this.proQuietStartMinutes = repository.getHourlyChimeQuietStart();
        this.proQuietEndMinutes = repository.getHourlyChimeQuietEnd();
        int[] backgroundColors = createBackgroundColors(context);
        int[] textColors = createTextColors(context);
        BottomSheetBehavior<?> behavior = getBehavior();
        behavior.setDraggable(false);
        behavior.setHideable(false);
        behavior.setSkipCollapsed(true);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = dp(24);
        root.setPadding(horizontalPadding, dp(12), horizontalPadding, 0);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText(R.string.background_settings);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

        MaterialButton cancel = new MaterialButton(context, null,
            android.R.attr.borderlessButtonStyle);
        cancel.setText(R.string.cancel);
        cancel.setSingleLine(true);
        cancel.setOnClickListener(view -> cancel());
        header.addView(cancel, new LinearLayout.LayoutParams(dp(88), dp(48)));

        MaterialButton apply = new MaterialButton(context);
        apply.setText(R.string.apply);
        apply.setSingleLine(true);
        apply.setOnClickListener(view -> applySelection());
        header.addView(apply, new LinearLayout.LayoutParams(dp(88), dp(48)));
        root.addView(header, matchWrap(dp(48)));

        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(0, 0, 0, dp(28));

        // ---- Tabs ----
        final MaterialButtonToggleGroup boardSelector = new MaterialButtonToggleGroup(context);
        boardSelector.setSingleSelection(true);
        boardSelector.setSelectionRequired(true);
        int styleTabId = View.generateViewId();
        int functionTabId = View.generateViewId();
        boardSelector.addView(createModeButton(context, styleTabId, R.string.tab_style),
            weightedButtonParams());
        boardSelector.addView(createModeButton(context, functionTabId, R.string.tab_function),
            weightedButtonParams());
        boardSelector.check(styleTabId);
        root.addView(boardSelector, topMargin(matchWrap(dp(48)), dp(12)));

        // ---- Style board ----
        final LinearLayout styleContent = new LinearLayout(context);
        styleContent.setOrientation(LinearLayout.VERTICAL);

        // Background section
        styleContent.addView(createSectionLabel(context, R.string.background_settings_group), sectionLabelParams());

        modeGroup = new MaterialButtonToggleGroup(context);
        modeGroup.setSingleSelection(true);
        modeGroup.setSelectionRequired(true);
        MaterialButton colorMode = createModeButton(context, COLOR_MODE_ID, R.string.solid_color);
        MaterialButton imageMode = createModeButton(context, IMAGE_MODE_ID, R.string.background_image);
        modeGroup.addView(colorMode, weightedButtonParams());
        modeGroup.addView(imageMode, weightedButtonParams());
        styleContent.addView(modeGroup, topMargin(matchWrap(dp(48)), dp(4)));

        LinearLayout colorControlsLayout = new LinearLayout(context);
        colorControlsLayout.setOrientation(LinearLayout.VERTICAL);
        colorControls = colorControlsLayout;

        backgroundPreview = createSwatchPreview(context, repository.getCurrentColor());
        colorControlsLayout.addView(backgroundPreview, topMargin(matchWrap(dp(40)), dp(16)));

        backgroundPicker = new ColorPickerView(context);
        backgroundPicker.setAccessibilityLabel(context.getString(R.string.background_color_picker));
        backgroundPicker.setColor(repository.getCurrentColor());
        backgroundPicker.setOnColorChangedListener(color -> {
            setSwatchColor(backgroundPreview, color);
            modeGroup.check(COLOR_MODE_ID);
        });
        colorControlsLayout.addView(createColorSwatches(context, backgroundColors, color -> {
            backgroundPicker.setColor(color);
            setSwatchColor(backgroundPreview, color);
            modeGroup.check(COLOR_MODE_ID);
        }), topMargin(matchWrap(dp(52)), dp(12)));
        addAdvancedPicker(context, colorControlsLayout, backgroundPicker);
        styleContent.addView(colorControlsLayout, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout imageControlsLayout = new LinearLayout(context);
        imageControlsLayout.setOrientation(LinearLayout.VERTICAL);
        imageControls = imageControlsLayout;
        imageControlsLayout.addView(createImagePreview(context), topMargin(matchWrap(dp(180)), dp(12)));

        MaterialButton chooseImage = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        chooseImage.setText(R.string.choose_image);
        chooseImage.setIconResource(R.drawable.ic_photo_library);
        chooseImage.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        chooseImage.setOnClickListener(view -> {
            dismiss();
            listener.onChooseImage();
        });
        imageControlsLayout.addView(chooseImage, topMargin(matchWrap(dp(52)), dp(12)));

        dimBackgroundSwitch = createStyleSwitch(context, R.string.dim_background,
            repository.isDimBackground());
        imageControlsLayout.addView(dimBackgroundSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        scheduleDimBackgroundSwitch = createStyleSwitch(context, R.string.schedule_dim_background,
            repository.isScheduleDimBackground());
        imageControlsLayout.addView(scheduleDimBackgroundSwitch, matchWrap(dp(48)));

        TextView dimNote = createSubLabel(context, R.string.dim_schedule_note);
        imageControlsLayout.addView(dimNote, topMargin(subLabelParams(), dp(2)));

        LinearLayout dimTimeRow = new LinearLayout(context);
        dimScheduleControls = dimTimeRow;
        dimStartButton = new MaterialButton(context, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle);
        dimEndButton = new MaterialButton(context, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle);
        updateDimTimeLabels();
        dimStartButton.setOnClickListener(view -> showDimTimePicker(true));
        dimEndButton.setOnClickListener(view -> showDimTimePicker(false));
        dimTimeRow.addView(dimStartButton, weightedButtonParams());
        LinearLayout.LayoutParams endTimeParams = weightedButtonParams();
        endTimeParams.leftMargin = dp(8);
        dimTimeRow.addView(dimEndButton, endTimeParams);
        imageControlsLayout.addView(dimTimeRow, topMargin(matchWrap(dp(52)), dp(8)));
        scheduleDimBackgroundSwitch.setOnCheckedChangeListener(
            (button, checked) -> updateDimScheduleState(checked));
        updateDimScheduleState(scheduleDimBackgroundSwitch.isChecked());
        styleContent.addView(imageControlsLayout, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateBackgroundControls(checkedId == IMAGE_MODE_ID);
            }
        });

        // Font section
        styleContent.addView(createSectionLabel(context, R.string.font_settings_group),
                topMargin(sectionLabelParams(), dp(20)));

        styleContent.addView(createSubLabel(context, R.string.font_family), subLabelParams());
        fontFamilySpinner = createFontFamilySpinner(context, repository.getFontFamily());
        styleContent.addView(fontFamilySpinner, topMargin(matchWrap(dp(48)), dp(4)));
        // Bold applies to both time and date, so it sits with the shared font family, not under time.
        boldTextSwitch = createStyleSwitch(context, R.string.bold_text, repository.isBoldText());
        styleContent.addView(boldTextSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        // Time font
        styleContent.addView(createSubLabel(context, R.string.time_font_settings), subLabelParams());
        timeSizeValue = new TextView(context);
        timeSizeBar = new Slider(context);
        styleContent.addView(createSizeRow(context, R.string.font_size, timeSizeBar, timeSizeValue), sizeRowParams());
        configureSizeBar(timeSizeBar, timeSizeValue, repository.getTimeFontScale());
        styleContent.addView(createSubLabel(context, R.string.font_color), subLabelParams());
        timeColorPicker = new ColorPickerView(context);
        timeColorPicker.setAccessibilityLabel(context.getString(R.string.time_color_picker));
        timeColorPicker.setColor(timeColor);
        timeColorPicker.setOnColorChangedListener(color -> timeColor = color);
        styleContent.addView(createColorSwatches(context, textColors, color -> {
            timeColor = color;
            timeColorPicker.setColor(color);
        }), topMargin(matchWrap(dp(52)), dp(4)));
        addAdvancedPicker(context, styleContent, timeColorPicker);

        blinkColonSwitch = createStyleSwitch(context, R.string.blink_colon, repository.isBlinkColon());
        styleContent.addView(blinkColonSwitch, topMargin(matchWrap(dp(48)), dp(12)));
        animateTimeChangesSwitch = createStyleSwitch(context, R.string.animate_time_changes,
            repository.isAnimateTimeChanges());
        styleContent.addView(animateTimeChangesSwitch, matchWrap(dp(48)));
        // 竖屏专用：开启后竖屏时钟改为竖排大字（时/分/秒分行），横屏保持不变；所有版本可用。
        portraitStackedSwitch = createStyleSwitch(context, R.string.portrait_stacked_clock,
                repository.isPortraitStacked());
        styleContent.addView(portraitStackedSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        if ("pro".equals(BuildConfig.FLAVOR)) {
            styleContent.addView(createSubLabel(context, R.string.pro_time_transition), subLabelParams());
            proTransitionSpinner = createStringSpinner(context, R.array.pro_time_transitions,
                    transitionIndex(repository.getTimeTransition()));
            styleContent.addView(proTransitionSpinner, topMargin(matchWrap(dp(48)), dp(4)));
                proHourlyChimeSwitch = createStyleSwitch(context, R.string.pro_hourly_chime,
                    repository.isHourlyChimeEnabled());
                styleContent.addView(proHourlyChimeSwitch, topMargin(matchWrap(dp(48)), dp(8)));
                proHalfHourChimeSwitch = createStyleSwitch(context, R.string.pro_half_hour_chime,
                    repository.isHalfHourChimeEnabled());
                styleContent.addView(proHalfHourChimeSwitch, matchWrap(dp(48)));
                proHourlyQuietSwitch = createStyleSwitch(context, R.string.pro_hourly_chime_quiet,
                    repository.isHourlyChimeQuietEnabled());
                styleContent.addView(proHourlyQuietSwitch, matchWrap(dp(48)));
                LinearLayout quietRow = new LinearLayout(context);
                quietRow.setOrientation(LinearLayout.HORIZONTAL);
                proQuietStartButton = new MaterialButton(context);
                proQuietEndButton = new MaterialButton(context);
                updateProQuietLabels();
                proQuietStartButton.setOnClickListener(view -> showProQuietTimePicker(true));
                proQuietEndButton.setOnClickListener(view -> showProQuietTimePicker(false));
                quietRow.addView(proQuietStartButton, weightedButtonParams());
                LinearLayout.LayoutParams quietEndParams = weightedButtonParams();
                quietEndParams.leftMargin = dp(8);
                quietRow.addView(proQuietEndButton, quietEndParams);
                styleContent.addView(quietRow, topMargin(matchWrap(dp(50)), dp(4)));
        } else {
            proTransitionSpinner = null;
                proHourlyChimeSwitch = null;
                proHalfHourChimeSwitch = null;
                proHourlyQuietSwitch = null;
                proQuietStartButton = null;
                proQuietEndButton = null;
        }
        showSecondsSwitch = createStyleSwitch(context, R.string.show_seconds, repository.isShowSeconds());
        styleContent.addView(showSecondsSwitch, matchWrap(dp(48)));
        smallSecondsSwitch = createStyleSwitch(context, R.string.small_seconds, repository.isSmallSeconds());
        styleContent.addView(smallSecondsSwitch, matchWrap(dp(48)));
        showSecondsSwitch.setOnCheckedChangeListener(
            (button, checked) -> updateSmallSecondsState(checked));
        updateSmallSecondsState(showSecondsSwitch.isChecked());

        // Date font
        styleContent.addView(createSubLabel(context, R.string.date_font_settings),
                topMargin(subLabelParams(), dp(16)));
        dateSizeValue = new TextView(context);
        dateSizeBar = new Slider(context);
        styleContent.addView(createSizeRow(context, R.string.font_size, dateSizeBar, dateSizeValue), sizeRowParams());
        configureSizeBar(dateSizeBar, dateSizeValue, repository.getDateFontScale());
        styleContent.addView(createSubLabel(context, R.string.font_color), subLabelParams());
        dateColorPicker = new ColorPickerView(context);
        dateColorPicker.setAccessibilityLabel(context.getString(R.string.date_color_picker));
        dateColorPicker.setColor(dateColor);
        dateColorPicker.setOnColorChangedListener(color -> dateColor = color);
        styleContent.addView(createColorSwatches(context, textColors, color -> {
            dateColor = color;
            dateColorPicker.setColor(color);
        }), topMargin(matchWrap(dp(52)), dp(4)));
        addAdvancedPicker(context, styleContent, dateColorPicker);
        // The lunar date is part of the date line, so its toggle sits with the date settings.
        showLunarSwitch = createStyleSwitch(context, R.string.show_lunar, repository.isShowLunar());
        styleContent.addView(showLunarSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        dateLunarDualLineSwitch = createStyleSwitch(context, R.string.date_lunar_dual_line,
                repository.isDateLunarDualLine());
        addSwitchWithSummary(styleContent, dateLunarDualLineSwitch,
                R.string.date_lunar_dual_line_desc, dp(8));

        // Status bar section
        styleContent.addView(createSectionLabel(context, R.string.status_settings_group),
                topMargin(sectionLabelParams(), dp(20)));
        statusIconsSwitch = new MaterialSwitch(context);
        statusIconsSwitch.setText(R.string.show_status_icons);
        statusIconsSwitch.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        statusIconsSwitch.setChecked(repository.isShowStatusIcons());
        styleContent.addView(statusIconsSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        statusIconSizeValue = new TextView(context);
        statusIconSizeBar = new Slider(context);
        statusIconSizeControls = createSizeRow(context, R.string.status_icon_size,
                statusIconSizeBar, statusIconSizeValue);
        statusIconSizeBar.setValueFrom(MIN_STATUS_ICON_PERCENT);
        statusIconSizeBar.setValueTo(MAX_STATUS_ICON_PERCENT);
        configureStatusIconSizeBar(repository.getStatusIconScale());
        styleContent.addView(statusIconSizeControls, sizeRowParams());
        statusIconsSwitch.setOnCheckedChangeListener(
                (button, checked) -> updateStatusIconSizeState(checked));
        updateStatusIconSizeState(statusIconsSwitch.isChecked());

        // Shared reset action is mounted after both boards so it remains visible on either tab.
        MaterialButton reset = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        reset.setText(R.string.reset_default);
        reset.setOnClickListener(view -> restoreDefaults());

        // ---- Function board ----
        final LinearLayout functionContent = new LinearLayout(context);
        functionContent.setOrientation(LinearLayout.VERTICAL);
        functionContent.setVisibility(View.GONE);

        functionContent.addView(createSectionLabel(context, R.string.display_settings_group),
                sectionLabelParams());
        functionContent.addView(createSubLabel(context, R.string.screen_orientation),
                topMargin(subLabelParams(), dp(8)));
        orientationGroup = new MaterialButtonToggleGroup(context);
        orientationGroup.setSingleSelection(true);
        orientationGroup.setSelectionRequired(true);
        orientationGroup.addView(createModeButton(context, ORIENTATION_FOLLOW_ID,
                R.string.orientation_follow_system), weightedButtonParams());
        orientationGroup.addView(createModeButton(context, ORIENTATION_PORTRAIT_ID,
                R.string.orientation_portrait), weightedButtonParams());
        orientationGroup.addView(createModeButton(context, ORIENTATION_LANDSCAPE_ID,
                R.string.orientation_landscape), weightedButtonParams());
        orientationGroup.check(idForOrientation(repository.getScreenOrientation()));
        functionContent.addView(orientationGroup, topMargin(matchWrap(dp(48)), dp(6)));
        autoStartSwitch = createStyleSwitch(context, R.string.auto_start, repository.isAutoStart());
        addSwitchWithSummary(functionContent, autoStartSwitch, R.string.auto_start_desc, dp(8));

        functionContent.addView(createSectionLabel(context, R.string.time_settings_group),
                topMargin(sectionLabelParams(), dp(20)));
        use24HourSwitch = new MaterialSwitch(context);
        use24HourSwitch.setText(R.string.use_24_hour);
        use24HourSwitch.setTextAppearance(
            com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        use24HourSwitch.setChecked(repository.isUse24Hour());
        functionContent.addView(use24HourSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        networkTimeSwitch = new MaterialSwitch(context);
        networkTimeSwitch.setText(R.string.use_network_time);
        networkTimeSwitch.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        networkTimeSwitch.setChecked(repository.isUseNetworkTime());
        addSwitchWithSummary(functionContent, networkTimeSwitch,
                R.string.use_network_time_desc, dp(8));

        // Controls that are only meaningful when network time is enabled.
        LinearLayout locked = new LinearLayout(context);
        locked.setOrientation(LinearLayout.VERTICAL);
        functionLockedControls = locked;

        locked.addView(createSubLabel(context, R.string.sync_interval),
                topMargin(subLabelParams(), dp(4)));
        syncIntervalGroup = new MaterialButtonToggleGroup(context);
        syncIntervalGroup.setSingleSelection(true);
        syncIntervalGroup.setSelectionRequired(true);
        syncIntervalGroup.addView(createModeButton(context, SYNC_30MIN_ID, R.string.sync_interval_30min),
                weightedButtonParams());
        syncIntervalGroup.addView(createModeButton(context, SYNC_1HOUR_ID, R.string.sync_interval_1hour),
                weightedButtonParams());
        syncIntervalGroup.addView(createModeButton(context, SYNC_6HOUR_ID, R.string.sync_interval_6hour),
                weightedButtonParams());
        syncIntervalGroup.addView(createModeButton(context, SYNC_1DAY_ID, R.string.sync_interval_1day),
                weightedButtonParams());
        syncIntervalGroup.check(syncIntervalIdForMinutes(repository.getSyncIntervalMinutes()));
        locked.addView(syncIntervalGroup, topMargin(matchWrap(dp(48)), dp(4)));

        functionContent.addView(locked, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        functionContent.addView(createSectionLabel(context, R.string.region_settings_group),
                topMargin(sectionLabelParams(), dp(20)));
        selectedRegionIndex = RegionTimeZones.indexOfZoneId(repository.getTimeZoneId());
        regionButton = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        regionButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        regionButton.setText(regionSummary());
        regionButton.setOnClickListener(view -> showRegionChooser());
        functionContent.addView(regionButton, topMargin(matchWrap(dp(52)), dp(8)));

        functionContent.addView(createSectionLabel(context, R.string.clock_language_settings_group),
            topMargin(sectionLabelParams(), dp(20)));
        originalClockLanguage = repository.getClockLanguage();
        selectedLanguage = originalClockLanguage;
        chineseVariant = ClockPreferences.LANGUAGE_TRADITIONAL.equals(selectedLanguage)
                ? DateFormatter.Lang.TRADITIONAL : DateFormatter.Lang.CHINESE;
        if (isPro()) {
            // Pro exposes Simplified / Traditional / English; the other flavors keep a binary switch.
            clockLanguageGroup = new MaterialButtonToggleGroup(context);
            clockLanguageGroup.setSingleSelection(true);
            clockLanguageGroup.setSelectionRequired(true);
            clockLanguageGroup.addView(createModeButton(context, LANGUAGE_SIMPLIFIED_ID,
                    R.string.clock_language_simplified), weightedButtonParams());
            clockLanguageGroup.addView(createModeButton(context, LANGUAGE_TRADITIONAL_ID,
                    R.string.clock_language_traditional), weightedButtonParams());
            clockLanguageGroup.addView(createModeButton(context, LANGUAGE_ENGLISH_ID,
                    R.string.clock_language_english), weightedButtonParams());
            clockLanguageGroup.check(idForLanguage(selectedLanguage));
            functionContent.addView(clockLanguageGroup, topMargin(matchWrap(dp(48)), dp(8)));
        } else {
            clockUseEnglishSwitch = new MaterialSwitch(context);
            clockUseEnglishSwitch.setText(R.string.clock_use_english);
            clockUseEnglishSwitch.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
            clockUseEnglishSwitch.setChecked(repository.isClockUseEnglish());
            functionContent.addView(clockUseEnglishSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        }

        functionContent.addView(createSectionLabel(context, R.string.date_format_settings_group),
            topMargin(sectionLabelParams(), dp(20)));
        dateFormatContainer = new LinearLayout(context);
        dateFormatContainer.setOrientation(LinearLayout.VERTICAL);
        functionContent.addView(dateFormatContainer,
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(4)));
        initDateFormatState();
        dateSectionEnglish = ClockPreferences.LANGUAGE_ENGLISH.equals(selectedLanguage);
        rebuildDateFormatSection(context, dateSectionEnglish);
        if (clockLanguageGroup != null) {
            clockLanguageGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) onLanguageSelectionChanged(context, languageForId(checkedId));
            });
        } else {
            clockUseEnglishSwitch.setOnCheckedChangeListener((button, checked) ->
                    onLanguageSelectionChanged(context, checked
                            ? ClockPreferences.LANGUAGE_ENGLISH : ClockPreferences.LANGUAGE_SIMPLIFIED));
        }

        functionContent.addView(createSectionLabel(context, R.string.custom_message_settings_group),
            topMargin(sectionLabelParams(), dp(20)));
        customMessageInput = new EditText(context);
        customMessageInput.setSingleLine(true);
        customMessageInput.setHint(R.string.custom_message_hint);
        customMessageInput.setText(repository.getCustomMessage());
        functionContent.addView(customMessageInput, topMargin(matchWrap(dp(48)), dp(4)));

        functionContent.addView(createSectionLabel(context, R.string.weather_settings_group),
            topMargin(sectionLabelParams(), dp(20)));
        weatherSwitch = new MaterialSwitch(context);
        weatherSwitch.setText(R.string.show_weather);
        weatherSwitch.setTextAppearance(
            com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        weatherSwitch.setChecked(repository.isWeatherEnabled());
        functionContent.addView(weatherSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        functionContent.addView(createSubLabel(context, R.string.weather_location),
            topMargin(subLabelParams(), dp(4)));
        weatherLocationModeSpinner = new Spinner(context);
        weatherLocationModeSpinner.setAdapter(new ArrayAdapter<>(context,
            android.R.layout.simple_spinner_dropdown_item, new String[] {
                context.getString(R.string.weather_location_automatic),
                context.getString(R.string.weather_location_manual)
            }));
        weatherLocationModeSpinner.setSelection(ClockPreferences.WEATHER_LOCATION_MANUAL.equals(
            repository.getWeatherLocationMode()) ? 1 : 0);
        functionContent.addView(weatherLocationModeSpinner, topMargin(matchWrap(dp(48)), dp(4)));
        weatherLocationButton = new MaterialButton(context, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle);
        weatherLocationButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        updateWeatherLocationSummary();
        weatherLocationButton.setOnClickListener(view -> WeatherLocationChooser.show(context,
            selectedWeatherProvince, selectedWeatherCity, selectedWeatherDistrict,
            new WeatherLocationChooser.Listener() {
                @Override public void onLocationSelected(WeatherLocationCatalog.LocationEntry location) {
                    selectedWeatherLocationId = location.locationId;
                    selectedWeatherProvince = location.province;
                    selectedWeatherCity = location.city;
                    selectedWeatherDistrict = location.district;
                    selectedWeatherLatitude = location.latitude;
                    selectedWeatherLongitude = location.longitude;
                    updateWeatherLocationSummary();
                }
            }));
        functionContent.addView(weatherLocationButton, topMargin(matchWrap(dp(52)), dp(4)));
        functionContent.addView(createSubLabel(context, R.string.weather_update_interval),
            topMargin(subLabelParams(), dp(4)));
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
        weatherDetailedSwitch = new MaterialSwitch(context);
        weatherDetailedSwitch.setText(R.string.show_detailed_weather);
        weatherDetailedSwitch.setTextAppearance(
            com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        weatherDetailedSwitch.setChecked(repository.isWeatherDetailed());
        functionContent.addView(weatherDetailedSwitch, topMargin(matchWrap(dp(48)), dp(8)));
        // Pro-only weather-icon options: solid-fill vs line style, and Material 3 accent tinting.
        if (isPro()) {
            weatherIconFillSwitch = createStyleSwitch(context, R.string.weather_icon_fill,
                    repository.isWeatherIconFill());
            addSwitchWithSummary(functionContent, weatherIconFillSwitch,
                    R.string.weather_icon_fill_desc, dp(8));
            weatherIconDynamicColorSwitch = createStyleSwitch(context, R.string.weather_icon_dynamic_color,
                    repository.isWeatherIconDynamicColor());
            addSwitchWithSummary(functionContent, weatherIconDynamicColorSwitch,
                    R.string.weather_icon_dynamic_color_desc, dp(8));
        }
        // Pro-only 月历 section: the calendar page exists only in Pro, so this is its own group
        // rather than being tucked under 天气.
        if ("pro".equals(BuildConfig.FLAVOR)) {
            functionContent.addView(createSectionLabel(context, R.string.calendar_settings_group),
                    topMargin(sectionLabelParams(), dp(20)));
            functionContent.addView(createSubLabel(context, R.string.calendar_week_start),
                    topMargin(subLabelParams(), dp(8)));
            calendarWeekStartGroup = new MaterialButtonToggleGroup(context);
            calendarWeekStartGroup.setSingleSelection(true);
            calendarWeekStartGroup.setSelectionRequired(true);
            calendarWeekStartGroup.addView(createModeButton(context,
                    CALENDAR_WEEK_START_SUNDAY_ID, R.string.calendar_week_start_sunday),
                    weightedButtonParams());
            calendarWeekStartGroup.addView(createModeButton(context,
                    CALENDAR_WEEK_START_MONDAY_ID, R.string.calendar_week_start_monday),
                    weightedButtonParams());
            calendarWeekStartGroup.check(idForCalendarWeekStart(
                    repository.getCalendarWeekStart()));
            functionContent.addView(calendarWeekStartGroup,
                    topMargin(matchWrap(dp(48)), dp(6)));
            calendarHighlightWeekendsSwitch = createStyleSwitch(context,
                    R.string.calendar_highlight_weekends,
                    repository.isCalendarHighlightWeekends());
            functionContent.addView(calendarHighlightWeekendsSwitch,
                    topMargin(matchWrap(dp(48)), dp(8)));
        } else {
            calendarWeekStartGroup = null;
            calendarHighlightWeekendsSwitch = null;
        }
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

        // Group each board's sections into rounded cards for clearer visual separation.
        groupIntoCards(styleContent);
        groupIntoCards(functionContent);

        // ---- Boards container ----
        final LinearLayout boards = new LinearLayout(context);
        boards.setOrientation(LinearLayout.VERTICAL);
        boards.addView(styleContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
        boards.addView(functionContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        boardSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean style = checkedId == styleTabId;
            styleContent.setVisibility(style ? View.VISIBLE : View.GONE);
            functionContent.setVisibility(style ? View.GONE : View.VISIBLE);
        });

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(boards, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(reset, topMargin(matchWrap(dp(48)), dp(20)));

        scrollContent.addView(content, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(scrollContent, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(12);
        root.addView(scrollView, scrollParams);
        setContentView(root);
        ButtonTextSizer.applyToTree(root);

        boolean usingImage = ClockPreferences.MODE_IMAGE.equals(repository.getBackgroundMode());
        modeGroup.check(usingImage ? IMAGE_MODE_ID : COLOR_MODE_ID);
        updateBackgroundControls(usingImage);
        setOnDismissListener(dialog -> listener.onDismissed());
    }

    @Override
    protected void onStart() {
        super.onStart();
        BottomSheetBehavior<?> behavior = getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private TextView createSectionLabel(Context context, int textRes) {
        TextView label = new TextView(context);
        label.setText(textRes);
        label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        label.setTextColor(MaterialColors.getColor(context,
                androidx.appcompat.R.attr.colorPrimary, Color.DKGRAY));
        // Marker so groupIntoCards() can detect where each section begins.
        label.setTag(SECTION_LABEL_TAG);
        return label;
    }

    /**
     * Wraps each run of views between section labels into a rounded Material card, giving the
     * flat settings list clear visual grouping. Purely a view-tree transform: the same child
     * views — and the fields that reference them — are preserved, only reparented.
     */
    private void groupIntoCards(LinearLayout content) {
        int count = content.getChildCount();
        java.util.List<View> children = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) children.add(content.getChildAt(i));
        content.removeAllViews();
        LinearLayout body = null;
        boolean first = true;
        for (View child : children) {
            if (SECTION_LABEL_TAG.equals(child.getTag())) {
                body = newCardBody(content, first);
                first = false;
                // The label becomes the card title; drop its inter-section top margin.
                if (child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    ((LinearLayout.LayoutParams) child.getLayoutParams()).topMargin = 0;
                }
            }
            if (body == null) {
                content.addView(child, child.getLayoutParams());  // defensive: pre-section content
            } else {
                body.addView(child, child.getLayoutParams());
            }
        }
    }

    /** Creates an empty card, appends it to {@code parent}, and returns its content body. */
    private LinearLayout newCardBody(LinearLayout parent, boolean first) {
        MaterialCardView card = new MaterialCardView(getContext());
        card.setRadius(dp(CARD_RADIUS));
        card.setCardElevation(0f);
        card.setStrokeWidth(Math.max(1, dp(1)));
        card.setStrokeColor(MaterialColors.getColor(card,
                com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY));
        card.setCardBackgroundColor(MaterialColors.getColor(card,
                com.google.android.material.R.attr.colorSurfaceContainerHigh, Color.WHITE));
        LinearLayout cardBody = new LinearLayout(getContext());
        cardBody.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(CARD_PADDING);
        cardBody.setPadding(pad, pad, pad, pad);
        card.addView(cardBody, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = first ? 0 : dp(SECTION_GAP);
        parent.addView(card, lp);
        return cardBody;
    }

    private TextView createSubLabel(Context context, int textRes) {
        TextView label = new TextView(context);
        label.setText(textRes);
        label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        return label;
    }

    private MaterialSwitch createStyleSwitch(Context context, int textRes, boolean checked) {
        MaterialSwitch control = new MaterialSwitch(context);
        control.setText(textRes);
        control.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        control.setChecked(checked);
        return control;
    }

    /**
     * Adds {@code control} followed by a secondary summary line, emulating a two-line
     * title-and-summary switch row without a Preference screen.
     */
    private void addSwitchWithSummary(LinearLayout container, MaterialSwitch control, int summaryRes,
            int topMarginPx) {
        container.addView(control, topMargin(matchWrap(dp(48)), topMarginPx));
        TextView summary = new TextView(getContext());
        summary.setText(summaryRes);
        summary.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        summary.setTextColor(MaterialColors.getColor(getContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY));
        container.addView(summary, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(2)));
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
        weatherDetailedSwitch.setEnabled(enabled);
        weatherDetailedSwitch.setAlpha(enabled ? 1f : 0.4f);
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

    private View createSizeRow(Context context, int labelRes, Slider bar, TextView valueLabel) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText(labelRes);
        label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        if (bar.getId() == View.NO_ID) {
            bar.setId(View.generateViewId());
        }
        label.setLabelFor(bar.getId());
        row.addView(label, new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));

        bar.setValueFrom(MIN_FONT_PERCENT);
        bar.setValueTo(MAX_FONT_PERCENT);
        bar.setStepSize(1f);
        bar.setTickVisible(false);
        bar.setContentDescription(context.getString(labelRes));
        row.addView(bar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        valueLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        valueLabel.setGravity(Gravity.END);
        // Keep the "占屏幕宽度 xxx%" readout on a single line even for 3-digit percentages.
        valueLabel.setSingleLine(true);
        row.addView(valueLabel, new LinearLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void configureSizeBar(Slider bar, TextView valueLabel, float currentScale) {
        bar.setValue(scaleToProgress(currentScale));
        updateSizeLabel(bar, valueLabel, bar.getValue());
        bar.addOnChangeListener((slider, value, fromUser) ->
            updateSizeLabel(slider, valueLabel, value));
    }

    private void configureStatusIconSizeBar(float currentScale) {
        statusIconSizeBar.setValue(statusIconScaleToProgress(currentScale));
        updateStatusIconSizeLabel(statusIconSizeBar.getValue());
        statusIconSizeBar.addOnChangeListener((slider, value, fromUser) ->
                updateStatusIconSizeLabel(value));
    }

    private void updateStatusIconSizeLabel(float progress) {
        String value = getContext().getString(
                R.string.status_icon_size_percent, progressToPercent(progress));
        statusIconSizeValue.setText(value);
        if (Build.VERSION.SDK_INT >= 30) {
            statusIconSizeBar.setStateDescription(value);
        }
    }

    private void updateStatusIconSizeState(boolean enabled) {
        statusIconSizeControls.setAlpha(enabled ? 1f : 0.4f);
        setViewTreeEnabled(statusIconSizeControls, enabled);
    }

    private void updateSizeLabel(Slider bar, TextView valueLabel, float progress) {
        String value = getContext().getString(
                R.string.font_size_percent, progressToPercent(progress));
        valueLabel.setText(value);
        if (Build.VERSION.SDK_INT >= 30) {
            bar.setStateDescription(value);
        }
    }

    private MaterialButton createModeButton(Context context, int id, int textRes) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setId(id);
        button.setText(textRes);
        button.setCheckable(true);
        button.setContentDescription(context.getString(textRes));
        return button;
    }

    private View createSwatchPreview(Context context, int color) {
        View preview = new View(context);
        MaterialShapeDrawable background = new MaterialShapeDrawable(
                ShapeAppearanceModel.builder().setAllCornerSizes(dp(8)).build());
        background.setFillColor(ColorStateList.valueOf(color));
        preview.setBackground(background);
        return preview;
    }

    private View createImagePreview(Context context) {
        ImageView preview = new ImageView(context);
        preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackgroundColor(MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorSurfaceVariant, Color.DKGRAY));
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
        dimScheduleControls.setEnabled(enabled);
        dimScheduleControls.setAlpha(enabled ? 1f : 0.4f);
        setViewTreeEnabled(dimScheduleControls, enabled);
    }

    private static String formatMinutes(int minutes) {
        return String.format(java.util.Locale.CHINA, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private void setSwatchColor(View preview, int color) {
        MaterialShapeDrawable background = (MaterialShapeDrawable) preview.getBackground();
        background.setFillColor(ColorStateList.valueOf(color));
    }

    private View createColorSwatches(Context context, int[] colors, SwatchListener swatchListener) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (final int color : colors) {
            MaterialButton swatch = new MaterialButton(context);
            swatch.setContentDescription(getContext().getString(
                    R.string.use_color_value, String.format("#%06X", color & 0xFFFFFF)));
            swatch.setBackgroundTintList(ColorStateList.valueOf(color));
            swatch.setStrokeColor(ColorStateList.valueOf(0x33000000));
            swatch.setStrokeWidth(dp(1));
            swatch.setMinWidth(0);
            swatch.setInsetTop(0);
            swatch.setInsetBottom(0);
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
        MaterialSwitch advancedSwitch = createStyleSwitch(context, R.string.advanced, false);
        picker.setVisibility(View.GONE);
        advancedSwitch.setOnCheckedChangeListener((button, checked) ->
                picker.setVisibility(checked ? View.VISIBLE : View.GONE));
        parent.addView(advancedSwitch, topMargin(matchWrap(dp(48)), dp(4)));
        parent.addView(picker, matchWrap(dp(128)));
    }

    private static int[] createBackgroundColors(Context context) {
        return new int[] {
                themeColor(context, androidx.appcompat.R.attr.colorPrimary, 0xFF1D4ED8),
                themeColor(context, com.google.android.material.R.attr.colorSecondary, 0xFF0F766E),
                themeColor(context, com.google.android.material.R.attr.colorTertiary, 0xFF9F1239),
                0xFF101418, 0xFF334155, 0xFF3F6212, 0xFFB45309, 0xFFF8FAFC
        };
    }

    private static int[] createTextColors(Context context) {
        return new int[] {
                themeColor(context, androidx.appcompat.R.attr.colorPrimary, 0xFF38BDF8),
                themeColor(context, com.google.android.material.R.attr.colorSecondary, 0xFF34D399),
                themeColor(context, com.google.android.material.R.attr.colorTertiary, 0xFFF472B6),
                0xFFFFFFFF, 0xFFF8FAFC, 0xFFFACC15, 0xFFF87171, 0xFF111827
        };
    }

    private static int themeColor(Context context, int attribute, int fallback) {
        return MaterialColors.getColor(context, attribute, fallback);
    }

    private static int syncIntervalIdForMinutes(int minutes) {
        switch (minutes) {
            case 30:
                return SYNC_30MIN_ID;
            case 360:
                return SYNC_6HOUR_ID;
            case 1440:
                return SYNC_1DAY_ID;
            case 60:
            default:
                return SYNC_1HOUR_ID;
        }
    }

    private static int minutesForSyncIntervalId(int id) {
        if (id == SYNC_30MIN_ID) {
            return 30;
        }
        if (id == SYNC_6HOUR_ID) {
            return 360;
        }
        if (id == SYNC_1DAY_ID) {
            return 1440;
        }
        return 60;
    }

    private static int idForOrientation(int mode) {
        switch (mode) {
            case ClockPreferences.ORIENTATION_PORTRAIT:
                return ORIENTATION_PORTRAIT_ID;
            case ClockPreferences.ORIENTATION_LANDSCAPE:
                return ORIENTATION_LANDSCAPE_ID;
            default:
                return ORIENTATION_FOLLOW_ID;
        }
    }

    private static int orientationForId(int id) {
        if (id == ORIENTATION_PORTRAIT_ID) {
            return ClockPreferences.ORIENTATION_PORTRAIT;
        }
        if (id == ORIENTATION_LANDSCAPE_ID) {
            return ClockPreferences.ORIENTATION_LANDSCAPE;
        }
        return ClockPreferences.ORIENTATION_FOLLOW_SYSTEM;
    }

    private static int idForCalendarWeekStart(int firstDayOfWeek) {
        return firstDayOfWeek == ClockPreferences.CALENDAR_WEEK_START_MONDAY
                ? CALENDAR_WEEK_START_MONDAY_ID : CALENDAR_WEEK_START_SUNDAY_ID;
    }

    private static int calendarWeekStartForId(int id) {
        return id == CALENDAR_WEEK_START_MONDAY_ID
                ? ClockPreferences.CALENDAR_WEEK_START_MONDAY
                : ClockPreferences.CALENDAR_WEEK_START_SUNDAY;
    }

    private String[] regionNames() {
        return getContext().getResources().getStringArray(R.array.region_names);
    }

    private String regionSummary() {
        return getContext().getString(R.string.region_time_zone) + ": "
                + regionNames()[selectedRegionIndex];
    }

    private void updateFunctionLockedState(boolean enabled) {
        functionLockedControls.setEnabled(enabled);
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
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.select_region)
                .setSingleChoiceItems(regionNames(), selectedRegionIndex,
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

        modeGroup.check(COLOR_MODE_ID);
        backgroundPicker.setColor(defaultBackground);
        setSwatchColor(backgroundPreview, defaultBackground);

        timeColor = defaultTextColor;
        dateColor = defaultTextColor;
        timeColorPicker.setColor(defaultTextColor);
        dateColorPicker.setColor(defaultTextColor);

        timeSizeBar.setValue(scaleToProgress(ClockPreferences.DEFAULT_TIME_FONT_SCALE));
        updateSizeLabel(timeSizeBar, timeSizeValue, timeSizeBar.getValue());
        dateSizeBar.setValue(scaleToProgress(ClockPreferences.DEFAULT_DATE_FONT_SCALE));
        updateSizeLabel(dateSizeBar, dateSizeValue, dateSizeBar.getValue());

        statusIconsSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_STATUS_ICONS);
        statusIconSizeBar.setValue(statusIconScaleToProgress(
                ClockPreferences.DEFAULT_STATUS_ICON_SCALE));
        updateStatusIconSizeLabel(statusIconSizeBar.getValue());
        blinkColonSwitch.setChecked(ClockPreferences.DEFAULT_BLINK_COLON);
        animateTimeChangesSwitch.setChecked(ClockPreferences.DEFAULT_ANIMATE_TIME_CHANGES);
        if (proTransitionSpinner != null) proTransitionSpinner.setSelection(0);
        portraitStackedSwitch.setChecked(ClockPreferences.DEFAULT_PORTRAIT_STACKED);
        if (proHourlyChimeSwitch != null) {
            proHourlyChimeSwitch.setChecked(ClockPreferences.DEFAULT_HOURLY_CHIME);
            proHalfHourChimeSwitch.setChecked(ClockPreferences.DEFAULT_HALF_HOUR_CHIME);
            proHourlyQuietSwitch.setChecked(ClockPreferences.DEFAULT_HOURLY_CHIME_QUIET);
            proQuietStartMinutes = ClockPreferences.DEFAULT_HOURLY_CHIME_QUIET_START;
            proQuietEndMinutes = ClockPreferences.DEFAULT_HOURLY_CHIME_QUIET_END;
            updateProQuietLabels();
        }
        boldTextSwitch.setChecked(ClockPreferences.DEFAULT_BOLD_TEXT);
        fontFamilySpinner.setSelection(0);
        showSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_SECONDS);
        smallSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SMALL_SECONDS);
        showLunarSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_LUNAR);
        dateLunarDualLineSwitch.setChecked(ClockPreferences.DEFAULT_DATE_LUNAR_DUAL_LINE);
        if (calendarWeekStartGroup != null) {
            calendarWeekStartGroup.check(idForCalendarWeekStart(
                    ClockPreferences.DEFAULT_CALENDAR_WEEK_START));
            calendarHighlightWeekendsSwitch.setChecked(
                    ClockPreferences.DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS);
        }
        use24HourSwitch.setChecked(ClockPreferences.DEFAULT_USE_24_HOUR);
        selectedLanguage = ClockPreferences.DEFAULT_CLOCK_LANGUAGE;
        chineseVariant = DateFormatter.Lang.CHINESE;
        dateSectionEnglish = false;
        if (clockLanguageGroup != null) {
            clockLanguageGroup.check(idForLanguage(selectedLanguage));
        } else {
            clockUseEnglishSwitch.setChecked(false);
        }
        resetDateFormatToDefaults();
        customMessageInput.setText(ClockPreferences.DEFAULT_CUSTOM_MESSAGE);
        orientationGroup.check(idForOrientation(ClockPreferences.DEFAULT_SCREEN_ORIENTATION));
        autoStartSwitch.setChecked(ClockPreferences.DEFAULT_AUTO_START);
        weatherSwitch.setChecked(ClockPreferences.DEFAULT_WEATHER_ENABLED);
        weatherLocationModeSpinner.setSelection(0);
        selectedWeatherLocationId = "";
        selectedWeatherProvince = "";
        selectedWeatherCity = "";
        selectedWeatherDistrict = "";
        updateWeatherLocationSummary();
        weatherIntervalSpinner.setSelection(weatherIntervalIndex(
            ClockPreferences.DEFAULT_WEATHER_INTERVAL_MINUTES));
        if (weatherIconFillSwitch != null) {
            weatherIconFillSwitch.setChecked(ClockPreferences.DEFAULT_WEATHER_ICON_FILL);
            weatherIconDynamicColorSwitch.setChecked(ClockPreferences.DEFAULT_WEATHER_ICON_DYNAMIC_COLOR);
        }
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
        if (modeGroup.getCheckedButtonId() == IMAGE_MODE_ID && !repository.hasImage()) {
            Toast.makeText(getContext(), R.string.select_image_first, Toast.LENGTH_SHORT).show();
            return;
        }
        repository.setTimeFontScale(progressToScale(timeSizeBar.getValue()));
        repository.setDateFontScale(progressToScale(dateSizeBar.getValue()));
        repository.setTimeColor(timeColor);
        repository.setDateColor(dateColor);
        repository.setShowStatusIcons(statusIconsSwitch.isChecked());
        repository.setStatusIconScale(progressToScale(statusIconSizeBar.getValue()));
        repository.setBlinkColon(blinkColonSwitch.isChecked());
        repository.setAnimateTimeChanges(animateTimeChangesSwitch.isChecked());
        if (proTransitionSpinner != null) {
            repository.setTimeTransition(transitionForIndex(
                    proTransitionSpinner.getSelectedItemPosition()));
        }
        repository.setPortraitStacked(portraitStackedSwitch.isChecked());
        if (proHourlyChimeSwitch != null) {
            repository.setHourlyChimeEnabled(proHourlyChimeSwitch.isChecked());
            repository.setHalfHourChimeEnabled(proHalfHourChimeSwitch.isChecked());
            repository.setHourlyChimeQuietEnabled(proHourlyQuietSwitch.isChecked());
            repository.setHourlyChimeQuietStart(proQuietStartMinutes);
            repository.setHourlyChimeQuietEnd(proQuietEndMinutes);
        }
        repository.setBoldText(boldTextSwitch.isChecked());
        repository.setFontFamily(fontFamilyForIndex(fontFamilySpinner.getSelectedItemPosition()));
        repository.setShowSeconds(showSecondsSwitch.isChecked());
        repository.setSmallSeconds(showSecondsSwitch.isChecked() && smallSecondsSwitch.isChecked());
        repository.setShowLunar(showLunarSwitch.isChecked());
        repository.setDateLunarDualLine(dateLunarDualLineSwitch.isChecked());
        repository.setUse24Hour(use24HourSwitch.isChecked());
        repository.setClockLanguage(selectedLanguage);
        captureDateSectionInto(dateSectionEnglish);
        repository.setDatePatternCn(pendingDatePatternCn);
        repository.setDatePatternEn(pendingDatePatternEn);
        if (isPro()) {
            persistProDateState(false);
            persistProDateState(true);
        }
        repository.setCustomMessage(customMessageInput.getText().toString());
        repository.setScreenOrientation(orientationForId(orientationGroup.getCheckedButtonId()));
        boolean autoStartOn = autoStartSwitch.isChecked();
        boolean autoStartWasOn = repository.isAutoStart();
        repository.setAutoStart(autoStartOn);
        AutoStartManager.setEnabled(getContext(), autoStartOn);
        if (autoStartOn && !autoStartWasOn) {
            AutoStartManager.requestHomeRole(getContext());
        }
        repository.setDimBackground(dimBackgroundSwitch.isChecked());
        repository.setScheduleDimBackground(scheduleDimBackgroundSwitch.isChecked());
        repository.setDimStartMinutes(dimStartMinutes);
        repository.setDimEndMinutes(dimEndMinutes);
        repository.setUseNetworkTime(networkTimeSwitch.isChecked());
        repository.setSyncIntervalMinutes(minutesForSyncIntervalId(syncIntervalGroup.getCheckedButtonId()));
        repository.setTimeZoneId(RegionTimeZones.ZONE_IDS[selectedRegionIndex]);
        repository.setWeatherEnabled(weatherSwitch.isChecked());
        repository.setManualWeatherLocation(selectedWeatherLocationId, selectedWeatherProvince,
            selectedWeatherCity, selectedWeatherDistrict, selectedWeatherLatitude, selectedWeatherLongitude);
        repository.setWeatherDetailed(weatherDetailedSwitch.isChecked());
        if (weatherIconFillSwitch != null) {
            repository.setWeatherIconFill(weatherIconFillSwitch.isChecked());
            repository.setWeatherIconDynamicColor(weatherIconDynamicColorSwitch.isChecked());
        }
        if (calendarWeekStartGroup != null) {
            repository.setCalendarWeekStart(calendarWeekStartForId(
                    calendarWeekStartGroup.getCheckedButtonId()));
            repository.setCalendarHighlightWeekends(
                    calendarHighlightWeekendsSwitch.isChecked());
        }
        repository.setWeatherLocationMode(manualWeather
            ? ClockPreferences.WEATHER_LOCATION_MANUAL : ClockPreferences.WEATHER_LOCATION_AUTOMATIC);
        repository.setWeatherIntervalMinutes(weatherIntervalMinutes(
            weatherIntervalSpinner.getSelectedItemPosition()));
        listener.onFontSettingsApplied();

        if (modeGroup.getCheckedButtonId() == IMAGE_MODE_ID) {
            listener.onImageModeApplied();
        } else {
            listener.onColorApplied(backgroundPicker.getColor());
        }
        if (!selectedLanguage.equals(originalClockLanguage)) {
            listener.onLanguageChanged();
        }
        dismiss();
    }

    private static float scaleToProgress(float scale) {
        return Math.round(Math.max(ClockPreferences.MIN_FONT_SCALE,
                Math.min(ClockPreferences.MAX_FONT_SCALE, scale)) * 100f);
    }

    private static float progressToScale(float progress) {
        return Math.round(progress) / 100f;
    }

    private static float statusIconScaleToProgress(float scale) {
        return Math.round(ClockPreferences.normalizeStatusIconScale(scale) * 100f);
    }

    private static int progressToPercent(float progress) {
        return Math.round(progress);
    }

    private static boolean isPro() {
        return "pro".equals(BuildConfig.FLAVOR);
    }

    private DateFormatter.Lang langOf(boolean english) {
        return english ? DateFormatter.Lang.ENGLISH : chineseVariant;
    }

    private void onLanguageSelectionChanged(Context context, String language) {
        selectedLanguage = language;
        chineseVariant = ClockPreferences.LANGUAGE_TRADITIONAL.equals(language)
                ? DateFormatter.Lang.TRADITIONAL : DateFormatter.Lang.CHINESE;
        boolean english = ClockPreferences.LANGUAGE_ENGLISH.equals(language);
        captureDateSectionInto(dateSectionEnglish);
        dateSectionEnglish = english;
        rebuildDateFormatSection(context, english);
    }

    private static int idForLanguage(String language) {
        if (ClockPreferences.LANGUAGE_ENGLISH.equals(language)) return LANGUAGE_ENGLISH_ID;
        if (ClockPreferences.LANGUAGE_TRADITIONAL.equals(language)) return LANGUAGE_TRADITIONAL_ID;
        return LANGUAGE_SIMPLIFIED_ID;
    }

    private static String languageForId(int id) {
        if (id == LANGUAGE_ENGLISH_ID) return ClockPreferences.LANGUAGE_ENGLISH;
        if (id == LANGUAGE_TRADITIONAL_ID) return ClockPreferences.LANGUAGE_TRADITIONAL;
        return ClockPreferences.LANGUAGE_SIMPLIFIED;
    }

    private void initDateFormatState() {
        pendingDatePatternCn = repository.getDatePatternCn();
        pendingDatePatternEn = repository.getDatePatternEn();
        proCoreIndexCn = resolveCoreIndex(false);
        proCoreIndexEn = resolveCoreIndex(true);
        proComboIndexCn = resolveComboIndex(false);
        proComboIndexEn = resolveComboIndex(true);
        proCustomTextCn = repository.getDateCustomText(false);
        proCustomTextEn = repository.getDateCustomText(true);
    }

    private void resetDateFormatToDefaults() {
        pendingDatePatternCn = ClockPreferences.DEFAULT_DATE_PATTERN_CN;
        pendingDatePatternEn = ClockPreferences.DEFAULT_DATE_PATTERN_EN;
        proCustomTextCn = "";
        proCustomTextEn = "";
        proCoreIndexCn = defaultCoreIndex(false);
        proCoreIndexEn = defaultCoreIndex(true);
        proComboIndexCn = defaultComboIndex(false);
        proComboIndexEn = defaultComboIndex(true);
        rebuildDateFormatSection(getContext(), dateSectionEnglish);
    }

    private int resolveCoreIndex(boolean english) {
        String[] cores = DateFormatter.dateCores(langOf(english));
        if (repository.isDateCustomEnabled(english)) {
            return cores.length; // the "custom…" entry
        }
        int stored = indexOf(cores, repository.getDateCore(english));
        return stored >= 0 ? stored : defaultCoreIndex(english);
    }

    private int resolveComboIndex(boolean english) {
        String[] combos = DateFormatter.weekdayCombos(langOf(english));
        int stored = indexOf(combos, repository.getDateCombo(english));
        return stored >= 0 ? stored : defaultComboIndex(english);
    }

    private int defaultCoreIndex(boolean english) {
        String[] cores = DateFormatter.dateCores(langOf(english));
        int index = indexOf(cores, english ? "yyyy/M/d" : "yyyy年M月d日");
        return index >= 0 ? index : 0;
    }

    private int defaultComboIndex(boolean english) {
        int index = indexOf(DateFormatter.weekdayCombos(langOf(english)), "DATE EEEE");
        return index >= 0 ? index : 0;
    }

    private static int indexOf(String[] values, String target) {
        if (target == null || target.length() == 0) {
            return -1;
        }
        for (int i = 0; i < values.length; i++) {
            if (target.equals(values[i])) {
                return i;
            }
        }
        return -1;
    }

    private void rebuildDateFormatSection(Context context, boolean english) {
        dateFormatContainer.removeAllViews();
        dateCoreSpinner = null;
        dateComboSpinner = null;
        dateComboRow = null;
        dateCustomInput = null;
        dateCustomRow = null;
        dateCustomError = null;
        dateFixedSpinner = null;
        if (isPro()) {
            buildProDateControls(context, english);
        } else {
            buildFixedDateControls(context, english);
        }
        datePreviewLabel = new TextView(context);
        datePreviewLabel.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        dateFormatContainer.addView(datePreviewLabel,
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(8)));
        updateDatePreview(english);
        ButtonTextSizer.applyToTree(dateFormatContainer);
    }

    private void buildProDateControls(Context context, boolean english) {
        DateFormatter.Lang lang = langOf(english);
        String[] cores = DateFormatter.dateCores(lang);
        String[] coreLabels = new String[cores.length + 1];
        for (int i = 0; i < cores.length; i++) {
            coreLabels[i] = DateFormatter.preview(cores[i], lang);
        }
        coreLabels[cores.length] = context.getString(R.string.date_format_custom);
        dateFormatContainer.addView(createSubLabel(context, R.string.date_format_date_label),
                subLabelParams());
        dateCoreSpinner = buildStringSpinner(context, coreLabels, proCoreIndex(english));
        dateFormatContainer.addView(dateCoreSpinner, dateSpinnerParams());

        String[] combos = DateFormatter.weekdayCombos(lang);
        String[] comboLabels = new String[combos.length];
        for (int i = 0; i < combos.length; i++) {
            comboLabels[i] = DateFormatter.comboLabel(combos[i], lang);
        }
        LinearLayout comboRow = new LinearLayout(context);
        comboRow.setOrientation(LinearLayout.VERTICAL);
        comboRow.addView(createSubLabel(context, R.string.date_format_weekday_label), subLabelParams());
        dateComboSpinner = buildStringSpinner(context, comboLabels, proComboIndex(english));
        comboRow.addView(dateComboSpinner, dateSpinnerParams());
        dateComboRow = comboRow;
        dateFormatContainer.addView(comboRow, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout customRow = new LinearLayout(context);
        customRow.setOrientation(LinearLayout.VERTICAL);
        MaterialButton customHelpButton = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        customHelpButton.setText(R.string.date_format_help_button);
        customHelpButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        customHelpButton.setOnClickListener(v -> showDateFormatHelp(english));
        customRow.addView(customHelpButton, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
        dateCustomInput = new EditText(context);
        dateCustomInput.setSingleLine(true);
        dateCustomInput.setHint(R.string.date_format_custom_hint);
        dateCustomInput.setText(proCustomText(english));
        customRow.addView(dateCustomInput, dateSpinnerParams());
        dateCustomError = new TextView(context);
        dateCustomError.setText(R.string.date_format_custom_invalid);
        dateCustomError.setTextColor(themeColor(context,
                androidx.appcompat.R.attr.colorError, 0xFFB3261E));
        dateCustomError.setVisibility(View.GONE);
        customRow.addView(dateCustomError,
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(2)));
        dateCustomRow = customRow;
        dateFormatContainer.addView(customRow, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        boolean custom = proCoreIndex(english) == cores.length;
        dateComboRow.setVisibility(custom ? View.GONE : View.VISIBLE);
        dateCustomRow.setVisibility(custom ? View.VISIBLE : View.GONE);

        dateCoreSpinner.setOnItemSelectedListener(onItemSelected(() -> {
            boolean isCustom = dateCoreSpinner.getSelectedItemPosition() == cores.length;
            dateComboRow.setVisibility(isCustom ? View.GONE : View.VISIBLE);
            dateCustomRow.setVisibility(isCustom ? View.VISIBLE : View.GONE);
            updateDatePreview(english);
        }));
        dateComboSpinner.setOnItemSelectedListener(onItemSelected(() -> updateDatePreview(english)));
        dateCustomInput.addTextChangedListener(onTextChanged(() -> updateDatePreview(english)));
    }

    /** Explains the custom date-format tokens for the language currently being edited (Pro only). */
    private void showDateFormatHelp(boolean english) {
        int body = english ? R.string.date_format_help_body_en : R.string.date_format_help_body_cn;
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.date_format_help_title)
                .setMessage(body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void buildFixedDateControls(Context context, boolean english) {
        DateFormatter.Lang lang = langOf(english);
        String[] fixed = DateFormatter.fixedFormats(lang);
        String[] labels = new String[fixed.length];
        for (int i = 0; i < fixed.length; i++) {
            labels[i] = DateFormatter.preview(fixed[i], lang);
        }
        int selection = indexOf(fixed, pendingPattern(english));
        if (selection < 0) {
            selection = 0;
        }
        setPendingPattern(english, fixed[selection]);
        dateFixedSpinner = buildStringSpinner(context, labels, selection);
        dateFormatContainer.addView(dateFixedSpinner, dateSpinnerParams());
        dateFixedSpinner.setOnItemSelectedListener(onItemSelected(() -> {
            setPendingPattern(english,
                    fixed[clampIndex(dateFixedSpinner.getSelectedItemPosition(), fixed.length)]);
            updateDatePreview(english);
        }));
    }

    private void updateDatePreview(boolean english) {
        if (datePreviewLabel == null) {
            return;
        }
        DateFormatter.Lang lang = langOf(english);
        String pattern = currentPatternFromWidgets(english);
        boolean valid = DateFormatter.isValidPattern(pattern);
        if (dateCustomError != null) {
            boolean customVisible = dateCustomRow != null
                    && dateCustomRow.getVisibility() == View.VISIBLE;
            dateCustomError.setVisibility(customVisible && !valid ? View.VISIBLE : View.GONE);
        }
        if (valid) {
            setPendingPattern(english, pattern);
        }
        String rendered = DateFormatter.preview(pendingPattern(english), lang);
        datePreviewLabel.setText(context().getString(R.string.date_format_preview, rendered));
    }

    private String currentPatternFromWidgets(boolean english) {
        DateFormatter.Lang lang = langOf(english);
        if (isPro()) {
            if (dateCoreSpinner == null) {
                return pendingPattern(english);
            }
            String[] cores = DateFormatter.dateCores(lang);
            int coreSel = dateCoreSpinner.getSelectedItemPosition();
            if (coreSel == cores.length) {
                return dateCustomInput == null ? "" : dateCustomInput.getText().toString();
            }
            String[] combos = DateFormatter.weekdayCombos(lang);
            String core = cores[clampIndex(coreSel, cores.length)];
            String combo = combos[clampIndex(
                    dateComboSpinner == null ? 0 : dateComboSpinner.getSelectedItemPosition(),
                    combos.length)];
            return DateFormatter.composeCombo(combo, core);
        }
        if (dateFixedSpinner == null) {
            return pendingPattern(english);
        }
        String[] fixed = DateFormatter.fixedFormats(lang);
        return fixed[clampIndex(dateFixedSpinner.getSelectedItemPosition(), fixed.length)];
    }

    private void captureDateSectionInto(boolean english) {
        if (isPro()) {
            if (dateCoreSpinner == null) {
                return;
            }
            setProCoreIndex(english, dateCoreSpinner.getSelectedItemPosition());
            if (dateComboSpinner != null) {
                setProComboIndex(english, dateComboSpinner.getSelectedItemPosition());
            }
            if (dateCustomInput != null) {
                setProCustomText(english, dateCustomInput.getText().toString());
            }
        }
        String pattern = currentPatternFromWidgets(english);
        if (DateFormatter.isValidPattern(pattern)) {
            setPendingPattern(english, pattern);
        }
    }

    private void persistProDateState(boolean english) {
        String[] cores = DateFormatter.dateCores(langOf(english));
        String[] combos = DateFormatter.weekdayCombos(langOf(english));
        int coreIndex = proCoreIndex(english);
        boolean custom = coreIndex == cores.length;
        String core = (!custom && coreIndex >= 0 && coreIndex < cores.length) ? cores[coreIndex] : "";
        int comboIndex = proComboIndex(english);
        String combo = (comboIndex >= 0 && comboIndex < combos.length) ? combos[comboIndex] : "";
        repository.setDateFormatState(english, core, combo, custom, proCustomText(english));
    }

    private Spinner buildStringSpinner(Context context, String[] labels, int selection) {
        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (selection >= 0 && selection < labels.length) {
            spinner.setSelection(selection);
        }
        return spinner;
    }

    private LinearLayout.LayoutParams dateSpinnerParams() {
        return topMargin(matchWrap(dp(48)), dp(4));
    }

    private static int clampIndex(int index, int length) {
        return (index < 0 || index >= length) ? 0 : index;
    }

    private Context context() {
        return getContext();
    }

    private AdapterView.OnItemSelectedListener onItemSelected(Runnable action) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                action.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
    }

    private TextWatcher onTextChanged(Runnable action) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { action.run(); }
            @Override public void afterTextChanged(Editable s) { }
        };
    }

    private int proCoreIndex(boolean english) {
        return english ? proCoreIndexEn : proCoreIndexCn;
    }

    private void setProCoreIndex(boolean english, int value) {
        if (english) {
            proCoreIndexEn = value;
        } else {
            proCoreIndexCn = value;
        }
    }

    private int proComboIndex(boolean english) {
        return english ? proComboIndexEn : proComboIndexCn;
    }

    private void setProComboIndex(boolean english, int value) {
        if (english) {
            proComboIndexEn = value;
        } else {
            proComboIndexCn = value;
        }
    }

    private String proCustomText(boolean english) {
        return english ? proCustomTextEn : proCustomTextCn;
    }

    private void setProCustomText(boolean english, String value) {
        if (english) {
            proCustomTextEn = value;
        } else {
            proCustomTextCn = value;
        }
    }

    private String pendingPattern(boolean english) {
        return english ? pendingDatePatternEn : pendingDatePatternCn;
    }

    private void setPendingPattern(boolean english, String pattern) {
        if (english) {
            pendingDatePatternEn = pattern;
        } else {
            pendingDatePatternCn = pattern;
        }
    }

    private Spinner createFontFamilySpinner(Context context, String family) {
        Spinner spinner = new Spinner(context);
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                com.clockmods.background.FontCatalog.displayNames(context)));
        spinner.setSelection(com.clockmods.background.FontCatalog.indexOf(family));
        return spinner;
    }

    private static String fontFamilyForIndex(int index) {
        return com.clockmods.background.FontCatalog.idForIndex(index);
    }

    private Spinner createStringSpinner(Context context, int arrayRes, int selection) {
        Spinner spinner = new Spinner(context);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, arrayRes,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selection);
        return spinner;
    }

    private static int transitionIndex(String transition) {
        if (ClockPreferences.TRANSITION_SLIDE_UP.equals(transition)) return 1;
        if (ClockPreferences.TRANSITION_SLIDE_DOWN.equals(transition)) return 2;
        if (ClockPreferences.TRANSITION_SCALE.equals(transition)) return 3;
        if (ClockPreferences.TRANSITION_FLIP.equals(transition)) return 4;
        return 0;
    }

    private static String transitionForIndex(int index) {
        String[] transitions = {ClockPreferences.TRANSITION_FADE,
                ClockPreferences.TRANSITION_SLIDE_UP, ClockPreferences.TRANSITION_SLIDE_DOWN,
                ClockPreferences.TRANSITION_SCALE, ClockPreferences.TRANSITION_FLIP};
        return transitions[Math.max(0, Math.min(index, transitions.length - 1))];
    }

    private void showProQuietTimePicker(boolean start) {
        int current = start ? proQuietStartMinutes : proQuietEndMinutes;
        new TimePickerDialog(getContext(), (view, hour, minute) -> {
            if (start) proQuietStartMinutes = hour * 60 + minute;
            else proQuietEndMinutes = hour * 60 + minute;
            updateProQuietLabels();
        }, current / 60, current % 60, true).show();
    }

    private void updateProQuietLabels() {
        if (proQuietStartButton == null || proQuietEndButton == null) return;
        proQuietStartButton.setText(getContext().getString(R.string.pro_quiet_start,
                formatMinutes(proQuietStartMinutes)));
        proQuietEndButton.setText(getContext().getString(R.string.pro_quiet_end,
                formatMinutes(proQuietEndMinutes)));
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        return new LinearLayout.LayoutParams(0, dp(48), 1f);
    }

    private LinearLayout.LayoutParams sectionLabelParams() {
        return topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(8));
    }

    private LinearLayout.LayoutParams subLabelParams() {
        return topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(12));
    }

    private LinearLayout.LayoutParams sizeRowParams() {
        return topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(4));
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
