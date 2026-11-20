package com.clockmods.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Configuration;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.clockmods.R;
import com.clockmods.background.AutoStartManager;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ApiCompat;
import com.clockmods.time.RegionTimeZones;

public class SettingsDialog extends Dialog {
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

    private static final int MIN_FONT_PERCENT = 20;
    private static final int MAX_FONT_PERCENT = 150;
    private static final int FONT_PERCENT_RANGE = MAX_FONT_PERCENT - MIN_FONT_PERCENT;
    private static final int MIN_STATUS_ICON_PERCENT =
            Math.round(ClockPreferences.MIN_STATUS_ICON_SCALE * 100f);
    private static final int MAX_STATUS_ICON_PERCENT =
            Math.round(ClockPreferences.MAX_STATUS_ICON_SCALE * 100f);
    private static final int STATUS_ICON_PERCENT_RANGE =
            MAX_STATUS_ICON_PERCENT - MIN_STATUS_ICON_PERCENT;

    private static final int SWATCHES_PER_ROW = 4;
    // One vertical rhythm for the whole sheet: cards are separated by GAP_CARD, a card's
    // title sits GAP_TITLE above its first control, a label sits GAP_LABEL above the
    // control it names, and GAP_GROUP starts a new labelled group inside a card.
    private static final int GAP_CARD = 10;
    private static final int GAP_TITLE = 8;
    private static final int GAP_LABEL = 6;
    private static final int GAP_GROUP = 12;
    // Two switch rows stacked in one card were 48dp apart in the font card and 56dp apart
    // everywhere else, so the same control looked tighter in one place than another.
    private static final int GAP_ROW = 8;
    /** Every tap target in the sheet is at least this tall. */
    private static final int ROW_HEIGHT = 48;
    /** One height for every button plate, so Cancel and Apply match the buttons below them. */
    private static final int BUTTON_HEIGHT = 48;
    /** Horizontal gap between two buttons that sit side by side. */
    private static final int BUTTON_GAP = 8;
    // Swatch cells share the card width equally; the height keeps them near-square.
    private static final int SWATCH_GAP = 10;
    private static final int SWATCH_HEIGHT = 46;

    private static final int MODE_COLOR_ID = 30003;
    private static final int MODE_IMAGE_ID = 30004;

    // ---- Dark theme palette (kept in one place so controls cannot drift apart) ----
    private static final int COLOR_SURFACE = PanelTheme.SURFACE;
    private static final int COLOR_CARD = PanelTheme.CARD;
    private static final int COLOR_TITLE = PanelTheme.TITLE;
    private static final int COLOR_SECTION = PanelTheme.SECTION;
    private static final int COLOR_PRIMARY_TEXT = PanelTheme.PRIMARY_TEXT;
    private static final int COLOR_SECONDARY_TEXT = PanelTheme.SECONDARY_TEXT;
    private static final int COLOR_ACCENT = PanelTheme.ACCENT;
    private static final int COLOR_SWATCH_STROKE = PanelTheme.SWATCH_STROKE;

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
    private final CompoundButton dimBackgroundSwitch;
    private final CompoundButton scheduleDimBackgroundSwitch;
    private final View dimScheduleControls;
    private final Button dimStartButton;
    private final Button dimEndButton;

    private final ColorPickerView timeColorPicker;
    private final ColorPickerView dateColorPicker;
    private final SeekBar timeSizeBar;
    private final SeekBar dateSizeBar;
    private final TextView timeSizeValue;
    private final TextView dateSizeValue;
    private final CompoundButton blinkColonSwitch;
    private final CompoundButton animateTimeChangesSwitch;
    private final CompoundButton portraitStackedSwitch;
    private final CompoundButton boldTextSwitch;
    private final Spinner fontFamilySpinner;
    private final CompoundButton showSecondsSwitch;
    private final CompoundButton smallSecondsSwitch;
    private final CompoundButton showLunarSwitch;
    private final CompoundButton dateLunarDualLineSwitch;
    private final CompoundButton statusIconsSwitch;
    private final SeekBar statusIconSizeBar;
    private final TextView statusIconSizeValue;
    private final View statusIconSizeControls;
    private final CompoundButton use24HourSwitch;
    private final CompoundButton autoStartSwitch;
    private final CompoundButton clockUseEnglishSwitch;
    private final SegmentedSelector orientationSelector;
    private final CompoundButton networkTimeSwitch;
    private TimePickerDialog dimTimePicker;
    private final View functionLockedControls;
    private final RadioGroup syncIntervalGroup;
    private final Button regionButton;
    private final RadioButton sync30Min;
    private final RadioButton sync1Hour;
    private final RadioButton sync6Hour;
    private final RadioButton sync1Day;
    // ---- Date-format section (rebuilt when the interface language toggles) ----
    private LinearLayout dateFormatContainer;
    private boolean dateSectionEnglish;
    private final boolean originalClockUseEnglish;
    private String pendingDatePatternCn;
    private String pendingDatePatternEn;
    private Spinner dateFixedSpinner;
    private TextView datePreviewLabel;
    private final android.widget.EditText customMessageInput;
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
        WidgetStyles.styleButton(cancel, false);
        cancel.setOnClickListener(view -> cancel());
        header.addView(cancel, new LinearLayout.LayoutParams(dp(76), dp(BUTTON_HEIGHT)));

        Button apply = new Button(context);
        apply.setText(R.string.apply);
        WidgetStyles.styleButton(apply, true);
        apply.setOnClickListener(view -> applySelection());
        // Two action buttons never touch: the gap keeps the pair readable as two targets.
        LinearLayout.LayoutParams applyParams =
                new LinearLayout.LayoutParams(dp(76), dp(BUTTON_HEIGHT));
        applyParams.leftMargin = dp(BUTTON_GAP);
        header.addView(apply, applyParams);
        dialogRoot.addView(header, matchWrap(dp(56)));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, 0, padding, dp(18));
        content.setBackgroundColor(COLOR_SURFACE);

        boolean twoColumn = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        // ---- Style board ----
        final LinearLayout styleContent = new LinearLayout(context);
        styleContent.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout functionContent = new LinearLayout(context);
        functionContent.setOrientation(LinearLayout.VERTICAL);

        LinearLayout backgroundSection = createSection(context, styleContent, R.string.background_settings_group);

        RadioGroup modeGroup = new RadioGroup(context);
        modeGroup.setOrientation(RadioGroup.HORIZONTAL);
        colorMode = new RadioButton(context);
        colorMode.setText(R.string.solid_color);
        colorMode.setTextColor(COLOR_PRIMARY_TEXT);
        colorMode.setId(MODE_COLOR_ID);
        WidgetStyles.styleToggle(colorMode);
        imageMode = new RadioButton(context);
        imageMode.setText(R.string.background_image);
        imageMode.setTextColor(COLOR_PRIMARY_TEXT);
        imageMode.setId(MODE_IMAGE_ID);
        WidgetStyles.styleToggle(imageMode);
        modeGroup.addView(colorMode, new RadioGroup.LayoutParams(0, dp(48), 1f));
        modeGroup.addView(imageMode, new RadioGroup.LayoutParams(0, dp(48), 1f));
        backgroundSection.addView(modeGroup, topMargin(matchWrap(dp(48)), dp(GAP_TITLE)));

        LinearLayout colorControlsLayout = new LinearLayout(context);
        colorControlsLayout.setOrientation(LinearLayout.VERTICAL);
        colorControls = colorControlsLayout;

        backgroundPreview = createSwatchPreview(context, repository.getCurrentColor());
        colorControlsLayout.addView(backgroundPreview, topMargin(matchWrap(dp(34)), dp(8)));

        backgroundPicker = ColorPickerView.create(context);
        backgroundPicker.setColor(repository.getCurrentColor());
        backgroundPicker.setOnColorChangedListener(color -> {
            setSwatchColor(backgroundPreview, color);
            colorMode.setChecked(true);
        });
        colorControlsLayout.addView(createColorSwatches(context, BACKGROUND_COLORS, color -> {
            backgroundPicker.setColor(color);
            setSwatchColor(backgroundPreview, color);
            colorMode.setChecked(true);
        }), topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        addAdvancedPicker(context, colorControlsLayout, backgroundPicker);
        backgroundSection.addView(colorControlsLayout, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout imageControlsLayout = new LinearLayout(context);
        imageControlsLayout.setOrientation(LinearLayout.VERTICAL);
        imageControls = imageControlsLayout;
        imageControlsLayout.addView(createImagePreview(context), topMargin(matchWrap(dp(180)), dp(8)));

        Button chooseImage = new Button(context);
        chooseImage.setText(R.string.choose_image);
        WidgetStyles.styleButton(chooseImage, false);
        chooseImage.setOnClickListener(view -> listener.onChooseImage());
        imageControlsLayout.addView(chooseImage, topMargin(matchWrap(dp(BUTTON_HEIGHT)), dp(GAP_ROW)));

        dimBackgroundSwitch = createCheckbox(context, R.string.dim_background,
            repository.isDimBackground());
        imageControlsLayout.addView(dimBackgroundSwitch, topMargin(matchWrap(dp(48)), dp(GAP_TITLE)));
        scheduleDimBackgroundSwitch = createCheckbox(context, R.string.schedule_dim_background,
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
        WidgetStyles.styleButton(dimStartButton, false);
        WidgetStyles.styleButton(dimEndButton, false);
        updateDimTimeLabels();
        dimStartButton.setOnClickListener(view -> showDimTimePicker(true));
        dimEndButton.setOnClickListener(view -> showDimTimePicker(false));
        dimTimeRow.addView(dimStartButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams endTimeParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        endTimeParams.leftMargin = dp(BUTTON_GAP);
        dimTimeRow.addView(dimEndButton, endTimeParams);
        imageControlsLayout.addView(dimTimeRow, topMargin(matchWrap(dp(48)), dp(GAP_LABEL)));
        scheduleDimBackgroundSwitch.setOnCheckedChangeListener(
            (button, checked) -> updateDimScheduleState(checked));
        updateDimScheduleState(scheduleDimBackgroundSwitch.isChecked());
        backgroundSection.addView(imageControlsLayout, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        modeGroup.setOnCheckedChangeListener((group, checkedId) ->
                updateBackgroundControls(imageMode.isChecked()));

        // Font section
        LinearLayout fontSection = createSection(context, styleContent, R.string.font_settings_group);

        fontSection.addView(createSubLabel(context, R.string.font_family),
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_TITLE)));
        fontFamilySpinner = createFontFamilySpinner(context, repository.getFontFamily());
        fontSection.addView(fontFamilySpinner, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        // Bold applies to both time and date, so it sits with the shared font family, not under time.
        boldTextSwitch = createCheckbox(context, R.string.bold_text, repository.isBoldText());
        fontSection.addView(boldTextSwitch, topMargin(matchWrap(dp(48)), dp(GAP_LABEL)));

        fontSection.addView(createSubLabel(context, R.string.time_font_settings),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_GROUP)));
        timeSizeValue = new TextView(context);
        timeSizeBar = new SeekBar(context);
        fontSection.addView(createSizeRow(context, R.string.font_size, timeSizeBar, timeSizeValue),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        configureSizeBar(timeSizeBar, timeSizeValue, repository.getTimeFontScale());
        fontSection.addView(createSubLabel(context, R.string.font_color),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_GROUP)));
        timeColorPicker = ColorPickerView.create(context);
        timeColorPicker.setColor(timeColor);
        timeColorPicker.setOnColorChangedListener(color -> timeColor = color);
        fontSection.addView(createColorSwatches(context, TEXT_COLORS, color -> {
            timeColor = color;
            timeColorPicker.setColor(color);
        }), topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        addAdvancedPicker(context, fontSection, timeColorPicker);

        blinkColonSwitch = createCheckbox(context, R.string.blink_colon, repository.isBlinkColon());
        fontSection.addView(blinkColonSwitch, topMargin(matchWrap(dp(ROW_HEIGHT)), dp(GAP_ROW)));
        animateTimeChangesSwitch = createCheckbox(context, R.string.animate_time_changes,
            repository.isAnimateTimeChanges());
        fontSection.addView(animateTimeChangesSwitch, topMargin(matchWrap(dp(ROW_HEIGHT)), dp(GAP_ROW)));
        // 竖屏专用：开启后竖屏时钟改为竖排大字（时/分/秒分行），横屏保持不变。
        portraitStackedSwitch = createCheckbox(context, R.string.portrait_stacked_clock,
            repository.isPortraitStacked());
        fontSection.addView(portraitStackedSwitch, topMargin(matchWrap(dp(ROW_HEIGHT)), dp(GAP_ROW)));
        showSecondsSwitch = createCheckbox(context, R.string.show_seconds, repository.isShowSeconds());
        fontSection.addView(showSecondsSwitch, topMargin(matchWrap(dp(ROW_HEIGHT)), dp(GAP_ROW)));
        smallSecondsSwitch = createCheckbox(context, R.string.small_seconds, repository.isSmallSeconds());
        fontSection.addView(smallSecondsSwitch, topMargin(matchWrap(dp(ROW_HEIGHT)), dp(GAP_ROW)));
        showSecondsSwitch.setOnCheckedChangeListener(
            (button, checked) -> updateSmallSecondsState(checked));
        updateSmallSecondsState(showSecondsSwitch.isChecked());

        fontSection.addView(createSubLabel(context, R.string.date_font_settings),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_GROUP)));
        dateSizeValue = new TextView(context);
        dateSizeBar = new SeekBar(context);
        fontSection.addView(createSizeRow(context, R.string.font_size, dateSizeBar, dateSizeValue),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        configureSizeBar(dateSizeBar, dateSizeValue, repository.getDateFontScale());
        fontSection.addView(createSubLabel(context, R.string.font_color),
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_GROUP)));
        dateColorPicker = ColorPickerView.create(context);
        dateColorPicker.setColor(dateColor);
        dateColorPicker.setOnColorChangedListener(color -> dateColor = color);
        fontSection.addView(createColorSwatches(context, TEXT_COLORS, color -> {
            dateColor = color;
            dateColorPicker.setColor(color);
        }), topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        addAdvancedPicker(context, fontSection, dateColorPicker);
        // The lunar date is part of the date line, so its toggle sits with the date settings.
        showLunarSwitch = createCheckbox(context, R.string.show_lunar, repository.isShowLunar());
        fontSection.addView(showLunarSwitch, topMargin(matchWrap(dp(ROW_HEIGHT)), dp(GAP_ROW)));
        dateLunarDualLineSwitch = createCheckbox(context, R.string.date_lunar_dual_line,
                repository.isDateLunarDualLine());
        addSwitchWithSummary(fontSection, dateLunarDualLineSwitch,
                R.string.date_lunar_dual_line_desc, dp(GAP_ROW));

        // Status bar section
        LinearLayout statusSection = createSection(context,
                twoColumn ? functionContent : styleContent, R.string.status_settings_group);
        statusIconsSwitch = createCheckbox(context, R.string.show_status_icons,
                repository.isShowStatusIcons());
        statusSection.addView(statusIconsSwitch, topMargin(matchWrap(dp(48)), dp(GAP_TITLE)));

        statusIconSizeValue = new TextView(context);
        statusIconSizeBar = new SeekBar(context);
        statusIconSizeControls = createStatusIconSizeRow(
                context, statusIconSizeBar, statusIconSizeValue);
        statusSection.addView(statusIconSizeControls,
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        configureStatusIconSizeBar(repository.getStatusIconScale());
        statusIconsSwitch.setOnCheckedChangeListener(
                (button, checked) -> updateStatusIconSizeState(checked));
        updateStatusIconSizeState(statusIconsSwitch.isChecked());

        // Reset applies to both boards and is placed after their shared content below.
        Button reset = new Button(context);
        reset.setText(R.string.reset_default);
        WidgetStyles.styleButton(reset, false);
        reset.setOnClickListener(view -> restoreDefaults());

        // ---- Function board ----
        LinearLayout displaySection = createSection(context, functionContent, R.string.display_settings_group);
        displaySection.addView(createSubLabel(context, R.string.screen_orientation),
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_TITLE)));
        orientationSelector = new SegmentedSelector(context,
            new CharSequence[] {
                context.getString(R.string.orientation_follow_system),
                context.getString(R.string.orientation_portrait),
                context.getString(R.string.orientation_landscape)
            }, COLOR_ACCENT, 0xFF252830, Color.WHITE, COLOR_SECONDARY_TEXT);
        orientationSelector.setSelectedIndex(repository.getScreenOrientation());
        displaySection.addView(orientationSelector, topMargin(matchWrap(dp(46)), dp(GAP_LABEL)));
        autoStartSwitch = createCheckbox(context, R.string.auto_start, repository.isAutoStart());
        addSwitchWithSummary(displaySection, autoStartSwitch, R.string.auto_start_desc, dp(GAP_ROW));

        LinearLayout timeSection = createSection(context, functionContent, R.string.time_settings_group);
        use24HourSwitch = createCheckbox(context, R.string.use_24_hour, repository.isUse24Hour());
        timeSection.addView(use24HourSwitch, topMargin(matchWrap(dp(48)), dp(GAP_TITLE)));

        networkTimeSwitch = createCheckbox(context, R.string.use_network_time,
                repository.isUseNetworkTime());
        addSwitchWithSummary(timeSection, networkTimeSwitch,
                R.string.use_network_time_desc, dp(GAP_ROW));

        LinearLayout locked = new LinearLayout(context);
        locked.setOrientation(LinearLayout.VERTICAL);
        functionLockedControls = locked;

        locked.addView(createSubLabel(context, R.string.sync_interval),
                matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
        syncIntervalGroup = new RadioGroup(context);
        syncIntervalGroup.setOrientation(RadioGroup.VERTICAL);
        sync30Min = createSyncOption(context, R.string.sync_interval_30min);
        sync1Hour = createSyncOption(context, R.string.sync_interval_1hour);
        sync6Hour = createSyncOption(context, R.string.sync_interval_6hour);
        sync1Day = createSyncOption(context, R.string.sync_interval_1day);
        syncIntervalGroup.addView(sync30Min, matchWrap(dp(ROW_HEIGHT)));
        syncIntervalGroup.addView(sync1Hour, matchWrap(dp(ROW_HEIGHT)));
        syncIntervalGroup.addView(sync6Hour, matchWrap(dp(ROW_HEIGHT)));
        syncIntervalGroup.addView(sync1Day, matchWrap(dp(ROW_HEIGHT)));
        checkSyncOption(repository.getSyncIntervalMinutes());
        locked.addView(syncIntervalGroup, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));

        timeSection.addView(locked, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));

        LinearLayout regionSection = createSection(context, functionContent, R.string.region_settings_group);
        selectedRegionIndex = RegionTimeZones.indexOfZoneId(repository.getTimeZoneId());
        regionButton = new Button(context);
        regionButton.setGravity(ApiCompat.gravityStart() | Gravity.CENTER_VERTICAL);
        WidgetStyles.styleButton(regionButton, false);
        regionButton.setText(regionSummary());
        regionButton.setOnClickListener(view -> showRegionChooser());
        regionSection.addView(regionButton, topMargin(matchWrap(dp(48)), dp(GAP_TITLE)));

        LinearLayout languageSection = createSection(context, functionContent, R.string.clock_language_settings_group);
        clockUseEnglishSwitch = createCheckbox(context, R.string.clock_use_english,
                repository.isClockUseEnglish());
        languageSection.addView(clockUseEnglishSwitch, topMargin(matchWrap(dp(48)), dp(GAP_TITLE)));
        originalClockUseEnglish = repository.isClockUseEnglish();

        LinearLayout dateFormatSection = createSection(context, functionContent, R.string.date_format_settings_group);
        dateFormatContainer = new LinearLayout(context);
        dateFormatContainer.setOrientation(LinearLayout.VERTICAL);
        dateFormatSection.addView(dateFormatContainer,
            topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        pendingDatePatternCn = repository.getDatePatternCn();
        pendingDatePatternEn = repository.getDatePatternEn();
        dateSectionEnglish = originalClockUseEnglish;
        rebuildDateFormatSection(context, dateSectionEnglish);
        clockUseEnglishSwitch.setOnCheckedChangeListener((button, checked) -> {
            captureDateSectionInto(dateSectionEnglish);
            dateSectionEnglish = checked;
            rebuildDateFormatSection(context, checked);
        });

        LinearLayout messageSection = createSection(context, functionContent, R.string.custom_message_settings_group);
        customMessageInput = new android.widget.EditText(context);
        customMessageInput.setSingleLine(true);
        customMessageInput.setHint(R.string.custom_message_hint);
        WidgetStyles.styleTextInput(customMessageInput);
        customMessageInput.setText(repository.getCustomMessage());
        messageSection.addView(customMessageInput, topMargin(matchWrap(dp(48)), dp(GAP_LABEL)));
        // The explanation used to live in the field's hint, where it wrapped to two lines and
        // had its second line clipped by the 48dp field. It belongs on a summary line instead.
        addSummary(messageSection, R.string.custom_message_desc);

        networkTimeSwitch.setOnCheckedChangeListener((button, checked) -> updateFunctionLockedState(checked));
        updateFunctionLockedState(networkTimeSwitch.isChecked());

        // Keep the same controls and draft values in either layout.
        if (twoColumn) {
            LinearLayout columns = new LinearLayout(context);
            columns.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            rightParams.leftMargin = dp(12);
            columns.addView(styleContent, leftParams);
            columns.addView(functionContent, rightParams);
            content.addView(columns, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            SegmentedSelector boardSelector = new SegmentedSelector(context,
                    new CharSequence[] {context.getString(R.string.tab_style),
                            context.getString(R.string.tab_function)},
                    COLOR_ACCENT, 0xFF252830, Color.WHITE, COLOR_SECONDARY_TEXT);
            LinearLayout fixedTabs = new LinearLayout(context);
            fixedTabs.setPadding(padding, 0, padding, 0);
            fixedTabs.addView(boardSelector, topMargin(matchWrap(dp(46)), dp(12)));
            dialogRoot.addView(fixedTabs, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

            functionContent.setVisibility(View.GONE);
            content.addView(styleContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(functionContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
            boardSelector.setOnSelectionChangedListener(index -> {
                boolean style = index == 0;
                styleContent.setVisibility(style ? View.VISIBLE : View.GONE);
                functionContent.setVisibility(style ? View.GONE : View.VISIBLE);
            });
        }
        content.addView(reset, topMargin(matchWrap(dp(48)), dp(18)));

        ScrollView scrollView = new PanelScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_SURFACE);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(12);
        dialogRoot.addView(scrollView, scrollParams);
        setContentView(dialogRoot);
        ButtonTextSizer.applyToTree(dialogRoot);

        boolean usingImage = ClockPreferences.MODE_IMAGE.equals(repository.getBackgroundMode());
        imageMode.setChecked(usingImage);
        colorMode.setChecked(!usingImage);
        updateBackgroundControls(usingImage);
        setOnDismissListener(dialog -> {
            dismissDimTimePicker();
            listener.onDismissed();
        });
    }

    private void dismissDimTimePicker() {
        if (dimTimePicker != null) {
            TimePickerDialog picker = dimTimePicker;
            dimTimePicker = null;
            if (picker.isShowing()) {
                picker.dismiss();
            }
        }
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
            // Asking for the raw screen height made the sheet taller than the area the window
            // manager actually hands to a dialog (it starts below the status bar), so the last
            // ~25dp - the bottom of the reset button - stayed off-screen. MATCH_PARENT lets the
            // window manager size it to exactly the space left below the status bar.
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
    }

    /** Each settings module uses the same inset, rounded card as the mylite layout. */
    private LinearLayout createSection(Context context, LinearLayout parent, int titleRes) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable card = new GradientDrawable();
        card.setColor(COLOR_CARD);
        card.setCornerRadius(dp(10));
        section.setBackgroundDrawable(card);
        // Equal inset on all four sides: the previous 12/10 mix made a card's bottom edge look
        // tighter than its sides whenever the last row was a line of text.
        section.setPadding(dp(12), dp(12), dp(12), dp(12));
        TextView title = createSectionLabel(context, titleRes);
        title.setPadding(0, 0, 0, dp(2));
        section.addView(title);
        parent.addView(section,
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_CARD)));
        return section;
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

    private CompoundButton createCheckbox(Context context, int textRes, boolean checked) {
        CompoundButton control = new CheckBox(context);
        control.setText(textRes);
        control.setTextColor(COLOR_PRIMARY_TEXT);
        WidgetStyles.styleToggle(control);
        control.setChecked(checked);
        return control;
    }

    /**
     * Adds {@code control} followed by a secondary summary line, emulating a two-line
     * title-and-summary switch row without a Preference screen.
     */
    private void addSwitchWithSummary(LinearLayout container, CompoundButton control, int summaryRes,
            int topMarginPx) {
        container.addView(control, topMargin(matchWrap(dp(ROW_HEIGHT)), topMarginPx));
        addSummary(container, summaryRes);
    }

    /** Secondary explanation under a control: dim, 12.5sp, the sheet's only hint style. */
    private void addSummary(LinearLayout container, int summaryRes) {
        TextView summary = new TextView(getContext());
        summary.setText(summaryRes);
        summary.setTextColor(COLOR_SECONDARY_TEXT);
        summary.setTextSize(12.5f);
        container.addView(summary, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(2)));
    }

    private void updateSmallSecondsState(boolean showSeconds) {
        smallSecondsSwitch.setEnabled(showSeconds);
        ApiCompat.setAlphaCompat(smallSecondsSwitch, showSeconds ? 1f : 0.4f);
    }

    private final java.util.Map<TextView, TextView> fontSizeWarnings = new java.util.HashMap<>();

    /**
     * A labelled slider. The name and its readout share the first line and the slider takes
     * the full card width underneath: with the label, a 120dp readout and the slider all on
     * one line the slider was left with only ~72dp, which is far too cramped to drag.
     */
    private View createSizeRow(Context context, int labelRes, SeekBar bar, TextView valueLabel) {
        LinearLayout control = new LinearLayout(context);
        control.setOrientation(LinearLayout.VERTICAL);

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText(labelRes);
        label.setTextColor(COLOR_SECONDARY_TEXT);
        label.setTextSize(13);
        headerRow.addView(label, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        valueLabel.setTextColor(COLOR_PRIMARY_TEXT);
        valueLabel.setTextSize(13);
        valueLabel.setGravity(ApiCompat.gravityEnd());
        // Keep the "占屏幕宽度 xxx%" readout on a single line even for 3-digit percentages.
        valueLabel.setSingleLine(true);
        headerRow.addView(valueLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        control.addView(headerRow, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        bar.setMax(FONT_PERCENT_RANGE);
        WidgetStyles.styleSeekBar(bar);
        // A full-width track is easy to hit horizontally, but the bar's own height is only
        // ~29dp; the extra height is padding above and below the track and gives the thumb a
        // finger-sized touch target without moving anything visually.
        control.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

        TextView warning = new TextView(context);
        warning.setText(R.string.font_size_overflow_warning);
        warning.setTextSize(12);
        warning.setTextColor(valueLabel.getCurrentTextColor());
        warning.setPadding(0, dp(2), 0, dp(2));
        warning.setVisibility(View.GONE);
        control.addView(warning);
        fontSizeWarnings.put(valueLabel, warning);
        return control;
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
        fontSizeWarnings.get(valueLabel).setVisibility(
                progressToPercent(progress) > 100 ? View.VISIBLE : View.GONE);
    }

    private View createStatusIconSizeRow(Context context, SeekBar bar, TextView valueLabel) {
        return createSizeRow(context, R.string.status_icon_size, bar, valueLabel);
    }

    private void configureStatusIconSizeBar(float currentScale) {
        statusIconSizeBar.setProgress(statusIconScaleToProgress(currentScale));
        updateStatusIconSizeLabel(statusIconSizeBar.getProgress());
        statusIconSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateStatusIconSizeLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateStatusIconSizeLabel(int progress) {
        statusIconSizeValue.setText(getContext().getString(
                R.string.status_icon_size_percent, progress + MIN_STATUS_ICON_PERCENT));
    }

    private void updateStatusIconSizeState(boolean enabled) {
        setViewTreeEnabled(statusIconSizeControls, enabled);
        ApiCompat.setAlphaCompat(statusIconSizeControls, enabled ? 1f : 0.45f);
    }

    private View createSwatchPreview(Context context, int color) {
        View preview = new View(context);
        preview.setBackgroundDrawable(createRoundedDrawable(color));
        return preview;
    }

    /** Refresh only the imported image, preserving every un-applied setting. */
    public void onImageImported() {
        android.view.ViewGroup controls = (android.view.ViewGroup) imageControls;
        View previous = controls.getChildAt(0);
        android.view.ViewGroup.LayoutParams layout = previous.getLayoutParams();
        controls.removeViewAt(0);
        controls.addView(createImagePreview(getContext()), 0, layout);
        imageMode.setChecked(true);
        colorMode.setChecked(false);
        updateBackgroundControls(true);
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
        dismissDimTimePicker();
        dimTimePicker = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            if (start) {
                dimStartMinutes = hourOfDay * 60 + minute;
            } else {
                dimEndMinutes = hourOfDay * 60 + minute;
            }
            updateDimTimeLabels();
        }, minutes / 60, minutes % 60, true);
        dimTimePicker.show();
    }

    private void updateDimTimeLabels() {
        dimStartButton.setText(ClockTimeText.align(getContext().getString(R.string.dim_start_time)
                + " " + formatMinutes(dimStartMinutes)));
        dimEndButton.setText(ClockTimeText.align(getContext().getString(R.string.dim_end_time)
                + " " + formatMinutes(dimEndMinutes)));
    }

    private void updateDimScheduleState(boolean enabled) {
        ApiCompat.setAlphaCompat(dimScheduleControls, enabled ? 1f : 0.4f);
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

    private View createColorSwatches(Context context, int[] colors, SwatchListener swatchListener) {
        // HorizontalScrollView is API 3, so the swatches wrap into rows of four instead:
        // Android 1.0 can then lay the row out with plain LinearLayouts only.
        // The cells share the card width with equal weights, so the last column ends exactly
        // at the card edge (a fixed width left a lopsided gap on the right) and the rows are
        // separated vertically instead of touching.
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = null;
        for (int i = 0; i < colors.length; i++) {
            if (i % SWATCHES_PER_ROW == 0) {
                row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(SWATCH_HEIGHT));
                if (i > 0) {
                    rowParams.topMargin = dp(SWATCH_GAP);
                }
                column.addView(row, rowParams);
            }
            final int color = colors[i];
            View swatch = new View(context);
            swatch.setBackgroundDrawable(createRoundedDrawable(color));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i % SWATCHES_PER_ROW != 0) {
                params.leftMargin = dp(SWATCH_GAP);
            }
            row.addView(swatch, params);
            swatch.setOnClickListener(view -> swatchListener.onPicked(color));
        }
        return column;
    }

    private void addAdvancedPicker(Context context, LinearLayout parent, ColorPickerView picker) {
        CompoundButton advancedSwitch = createCheckbox(context, R.string.advanced, false);
        picker.setVisibility(View.GONE);
        advancedSwitch.setOnCheckedChangeListener((button, checked) ->
                picker.setVisibility(checked ? View.VISIBLE : View.GONE));
        parent.addView(advancedSwitch, topMargin(matchWrap(dp(48)), dp(GAP_TITLE)));
        parent.addView(picker, matchWrap(dp(128)));
    }

    private RadioButton createSyncOption(Context context, int textRes) {
        RadioButton option = new RadioButton(context);
        option.setText(textRes);
        option.setTextColor(COLOR_PRIMARY_TEXT);
        option.setTextSize(14);
        WidgetStyles.styleToggle(option);
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

    private String[] regionNames() {
        return getContext().getResources().getStringArray(R.array.region_names);
    }

    private String regionSummary() {
        return getContext().getString(R.string.region_time_zone) + ": "
                + regionNames()[selectedRegionIndex];
    }

    private void updateFunctionLockedState(boolean enabled) {
        ApiCompat.setAlphaCompat(functionLockedControls, enabled ? 1f : 0.4f);
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
        PanelChooser.show(getContext(), getContext().getString(R.string.select_region),
                regionNames(), selectedRegionIndex, index -> {
                    selectedRegionIndex = index;
                    regionButton.setText(regionSummary());
                });
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
        statusIconSizeBar.setProgress(statusIconScaleToProgress(
                ClockPreferences.DEFAULT_STATUS_ICON_SCALE));
        updateStatusIconSizeLabel(statusIconSizeBar.getProgress());
        blinkColonSwitch.setChecked(ClockPreferences.DEFAULT_BLINK_COLON);
        animateTimeChangesSwitch.setChecked(ClockPreferences.DEFAULT_ANIMATE_TIME_CHANGES);
        portraitStackedSwitch.setChecked(ClockPreferences.DEFAULT_PORTRAIT_STACKED);
        boldTextSwitch.setChecked(ClockPreferences.DEFAULT_BOLD_TEXT);
        fontFamilySpinner.setSelection(0);
        showSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_SECONDS);
        smallSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SMALL_SECONDS);
        showLunarSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_LUNAR);
        dateLunarDualLineSwitch.setChecked(ClockPreferences.DEFAULT_DATE_LUNAR_DUAL_LINE);
        use24HourSwitch.setChecked(ClockPreferences.DEFAULT_USE_24_HOUR);
        clockUseEnglishSwitch.setChecked(ClockPreferences.DEFAULT_CLOCK_USE_ENGLISH);
        pendingDatePatternCn = ClockPreferences.DEFAULT_DATE_PATTERN_CN;
        pendingDatePatternEn = ClockPreferences.DEFAULT_DATE_PATTERN_EN;
        rebuildDateFormatSection(getContext(), dateSectionEnglish);
        customMessageInput.setText(ClockPreferences.DEFAULT_CUSTOM_MESSAGE);
        orientationSelector.setSelectedIndex(ClockPreferences.DEFAULT_SCREEN_ORIENTATION);
        autoStartSwitch.setChecked(ClockPreferences.DEFAULT_AUTO_START);
        dimBackgroundSwitch.setChecked(ClockPreferences.DEFAULT_DIM_BACKGROUND);
        scheduleDimBackgroundSwitch.setChecked(ClockPreferences.DEFAULT_SCHEDULE_DIM_BACKGROUND);
        dimStartMinutes = ClockPreferences.DEFAULT_DIM_START_MINUTES;
        dimEndMinutes = ClockPreferences.DEFAULT_DIM_END_MINUTES;
        updateDimTimeLabels();

        networkTimeSwitch.setChecked(ClockPreferences.DEFAULT_USE_NETWORK_TIME);
        checkSyncOption(ClockPreferences.DEFAULT_SYNC_INTERVAL_MINUTES);
        updateFunctionLockedState(networkTimeSwitch.isChecked());
        selectedRegionIndex = RegionTimeZones.indexOfZoneId(ClockPreferences.DEFAULT_TIME_ZONE_ID);
        regionButton.setText(regionSummary());

        Toast.makeText(getContext(), R.string.reset_default, Toast.LENGTH_SHORT).show();
    }

    private void applySelection() {
        repository.setTimeFontScale(progressToScale(timeSizeBar.getProgress()));
        repository.setDateFontScale(progressToScale(dateSizeBar.getProgress()));
        repository.setTimeColor(timeColor);
        repository.setDateColor(dateColor);
        repository.setShowStatusIcons(statusIconsSwitch.isChecked());
        repository.setStatusIconScale(statusIconProgressToScale(statusIconSizeBar.getProgress()));
        repository.setBlinkColon(blinkColonSwitch.isChecked());
        repository.setAnimateTimeChanges(animateTimeChangesSwitch.isChecked());
        repository.setPortraitStacked(portraitStackedSwitch.isChecked());
        repository.setBoldText(boldTextSwitch.isChecked());
        repository.setFontFamily(fontFamilyForIndex(fontFamilySpinner.getSelectedItemPosition()));
        repository.setShowSeconds(showSecondsSwitch.isChecked());
        repository.setSmallSeconds(showSecondsSwitch.isChecked() && smallSecondsSwitch.isChecked());
        repository.setShowLunar(showLunarSwitch.isChecked());
        repository.setDateLunarDualLine(dateLunarDualLineSwitch.isChecked());
        repository.setUse24Hour(use24HourSwitch.isChecked());
        repository.setClockUseEnglish(clockUseEnglishSwitch.isChecked());
        captureDateSectionInto(dateSectionEnglish);
        repository.setDatePatternCn(pendingDatePatternCn);
        repository.setDatePatternEn(pendingDatePatternEn);
        repository.setCustomMessage(customMessageInput.getText().toString());
        repository.setScreenOrientation(orientationSelector.getSelectedIndex());
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
        repository.setSyncIntervalMinutes(selectedSyncMinutes());
        repository.setTimeZoneId(RegionTimeZones.ZONE_IDS[selectedRegionIndex]);
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
        if (clockUseEnglishSwitch.isChecked() != originalClockUseEnglish) {
            listener.onLanguageChanged();
        }
        dismiss();
    }

    private static DateFormatter.Lang langOf(boolean english) {
        return english ? DateFormatter.Lang.ENGLISH : DateFormatter.Lang.CHINESE;
    }

    private void rebuildDateFormatSection(Context context, boolean english) {
        dateFormatContainer.removeAllViews();
        DateFormatter.Lang lang = langOf(english);
        final String[] fixed = DateFormatter.fixedFormats(lang);
        String[] labels = new String[fixed.length];
        for (int i = 0; i < fixed.length; i++) {
            labels[i] = DateFormatter.preview(fixed[i], lang);
        }
        int selection = indexOf(fixed, pendingPattern(english));
        if (selection < 0) {
            selection = 0;
        }
        setPendingPattern(english, fixed[selection]);
        dateFixedSpinner = createPanelSpinner(context, labels,
                R.string.date_format_settings_group);
        dateFixedSpinner.setSelection(selection);
        dateFormatContainer.addView(dateFixedSpinner, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        dateFixedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                setPendingPattern(english, fixed[(pos < 0 || pos >= fixed.length) ? 0 : pos]);
                updateDatePreview(english);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        datePreviewLabel = new TextView(context);
        datePreviewLabel.setTextColor(COLOR_SECONDARY_TEXT);
        dateFormatContainer.addView(datePreviewLabel,
                topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(GAP_LABEL)));
        updateDatePreview(english);
        ButtonTextSizer.applyToTree(dateFormatContainer);
    }

    private void updateDatePreview(boolean english) {
        if (datePreviewLabel == null) {
            return;
        }
        String rendered = DateFormatter.preview(pendingPattern(english), langOf(english));
        datePreviewLabel.setText(getContext().getString(R.string.date_format_preview, rendered));
    }

    private void captureDateSectionInto(boolean english) {
        if (dateFixedSpinner == null) {
            return;
        }
        String[] fixed = DateFormatter.fixedFormats(langOf(english));
        int pos = dateFixedSpinner.getSelectedItemPosition();
        setPendingPattern(english, fixed[(pos < 0 || pos >= fixed.length) ? 0 : pos]);
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

    private static int statusIconScaleToProgress(float scale) {
        int percent = Math.round(ClockPreferences.normalizeStatusIconScale(scale) * 100f);
        return Math.max(0, Math.min(STATUS_ICON_PERCENT_RANGE,
                percent - MIN_STATUS_ICON_PERCENT));
    }

    private static float statusIconProgressToScale(int progress) {
        int clampedProgress = Math.max(0, Math.min(STATUS_ICON_PERCENT_RANGE, progress));
        return (clampedProgress + MIN_STATUS_ICON_PERCENT) / 100f;
    }

    private Spinner createFontFamilySpinner(Context context, String family) {
        Spinner spinner = createPanelSpinner(context,
                com.clockmods.background.FontCatalog.displayNames(context), R.string.font_family);
        spinner.setSelection(com.clockmods.background.FontCatalog.indexOf(family));
        return spinner;
    }

    /**
     * A spinner wearing the sheet's dark plate with a drawn chevron whose popup is our own dark
     * chooser: the framework popup is a light window with white row dividers.
     */
    private Spinner createPanelSpinner(Context context, String[] labels, int titleRes) {
        PanelSpinner spinner = new PanelSpinner(context, context.getString(titleRes));
        spinner.setAdapter(new PanelSpinnerAdapter(context, labels));
        return spinner;
    }

    private static String fontFamilyForIndex(int index) {
        return com.clockmods.background.FontCatalog.idForIndex(index);
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
