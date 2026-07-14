package com.clockmods.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.app.TimePickerDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.time.RegionTimeZones;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;

public class SettingsDialog extends BottomSheetDialog {
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

    private static final int COLOR_MODE_ID = 10001;
    private static final int IMAGE_MODE_ID = 10002;
    private static final int SEEK_MAX = 100;
    private static final int[] BACKGROUND_COLORS = {
            0xFF101418, 0xFF334155, 0xFF1D4ED8, 0xFF0F766E,
            0xFF3F6212, 0xFF9F1239, 0xFFB45309, 0xFFF8FAFC
    };
    private static final int[] TEXT_COLORS = {
            0xFFFFFFFF, 0xFFF8FAFC, 0xFFFACC15, 0xFF38BDF8,
            0xFF34D399, 0xFFF472B6, 0xFFF87171, 0xFF111827
    };

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
    private final SeekBar timeSizeBar;
    private final SeekBar dateSizeBar;
    private final TextView timeSizeValue;
    private final TextView dateSizeValue;
    private final MaterialSwitch blinkColonSwitch;
    private final MaterialSwitch animateTimeChangesSwitch;
    private final MaterialSwitch boldTextSwitch;
    private final MaterialSwitch showSecondsSwitch;
    private final MaterialSwitch smallSecondsSwitch;
    private final MaterialSwitch showLunarSwitch;
    private final MaterialSwitch statusIconsSwitch;
    private final MaterialSwitch use24HourSwitch;
    private final MaterialSwitch clockUseEnglishSwitch;
    private final MaterialSwitch networkTimeSwitch;
    private final View functionLockedControls;
    private final MaterialButtonToggleGroup syncIntervalGroup;
    private final MaterialButton regionButton;
    private int timeColor;
    private int dateColor;
    private int selectedRegionIndex;
    private int dimStartMinutes;
    private int dimEndMinutes;

    private static final int SYNC_30MIN_ID = 20001;
    private static final int SYNC_1HOUR_ID = 20002;
    private static final int SYNC_6HOUR_ID = 20003;
    private static final int SYNC_1DAY_ID = 20004;

    public SettingsDialog(Context context, BackgroundRepository repository, Listener listener) {
        super(context);
        this.repository = repository;
        this.listener = listener;
        this.timeColor = repository.getTimeColor();
        this.dateColor = repository.getDateColor();
        this.dimStartMinutes = repository.getDimStartMinutes();
        this.dimEndMinutes = repository.getDimEndMinutes();
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
        final TabLayout tabLayout = new TabLayout(context);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_style));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_function));
        scrollContent.addView(tabLayout, topMargin(matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT), dp(32)));

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
        backgroundPicker.setColor(repository.getCurrentColor());
        backgroundPicker.setOnColorChangedListener(color -> {
            setSwatchColor(backgroundPreview, color);
            modeGroup.check(COLOR_MODE_ID);
        });
        colorControlsLayout.addView(backgroundPicker, topMargin(matchWrap(dp(200)), dp(12)));

        colorControlsLayout.addView(createColorSwatches(context, BACKGROUND_COLORS, color -> {
            backgroundPicker.setColor(color);
            setSwatchColor(backgroundPreview, color);
            modeGroup.check(COLOR_MODE_ID);
        }), topMargin(matchWrap(dp(52)), dp(8)));
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

        // Time font
        styleContent.addView(createSubLabel(context, R.string.time_font_settings), subLabelParams());
        timeSizeValue = new TextView(context);
        timeSizeBar = new SeekBar(context);
        styleContent.addView(createSizeRow(context, R.string.font_size, timeSizeBar, timeSizeValue), sizeRowParams());
        configureSizeBar(timeSizeBar, timeSizeValue, repository.getTimeFontScale());
        styleContent.addView(createSubLabel(context, R.string.font_color), subLabelParams());
        timeColorPicker = new ColorPickerView(context);
        timeColorPicker.setColor(timeColor);
        timeColorPicker.setOnColorChangedListener(color -> timeColor = color);
        styleContent.addView(timeColorPicker, topMargin(matchWrap(dp(180)), dp(4)));
        styleContent.addView(createColorSwatches(context, TEXT_COLORS, color -> {
            timeColor = color;
            timeColorPicker.setColor(color);
        }), topMargin(matchWrap(dp(52)), dp(8)));

        blinkColonSwitch = createStyleSwitch(context, R.string.blink_colon, repository.isBlinkColon());
        styleContent.addView(blinkColonSwitch, topMargin(matchWrap(dp(48)), dp(12)));
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

        // Date font
        styleContent.addView(createSubLabel(context, R.string.date_font_settings),
                topMargin(subLabelParams(), dp(16)));
        dateSizeValue = new TextView(context);
        dateSizeBar = new SeekBar(context);
        styleContent.addView(createSizeRow(context, R.string.font_size, dateSizeBar, dateSizeValue), sizeRowParams());
        configureSizeBar(dateSizeBar, dateSizeValue, repository.getDateFontScale());
        styleContent.addView(createSubLabel(context, R.string.font_color), subLabelParams());
        dateColorPicker = new ColorPickerView(context);
        dateColorPicker.setColor(dateColor);
        dateColorPicker.setOnColorChangedListener(color -> dateColor = color);
        styleContent.addView(dateColorPicker, topMargin(matchWrap(dp(180)), dp(4)));
        styleContent.addView(createColorSwatches(context, TEXT_COLORS, color -> {
            dateColor = color;
            dateColorPicker.setColor(color);
        }), topMargin(matchWrap(dp(52)), dp(8)));

        // Status bar section
        styleContent.addView(createSectionLabel(context, R.string.status_settings_group),
                topMargin(sectionLabelParams(), dp(20)));
        statusIconsSwitch = new MaterialSwitch(context);
        statusIconsSwitch.setText(R.string.show_status_icons);
        statusIconsSwitch.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        statusIconsSwitch.setChecked(repository.isShowStatusIcons());
        styleContent.addView(statusIconsSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        // Reset default lives in the style board.
        MaterialButton reset = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        reset.setText(R.string.reset_default);
        reset.setOnClickListener(view -> restoreDefaults());
        styleContent.addView(reset, topMargin(matchWrap(dp(48)), dp(20)));

        // ---- Function board ----
        final LinearLayout functionContent = new LinearLayout(context);
        functionContent.setOrientation(LinearLayout.VERTICAL);
        functionContent.setVisibility(View.GONE);

        functionContent.addView(createSectionLabel(context, R.string.time_settings_group), sectionLabelParams());
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
        functionContent.addView(networkTimeSwitch, topMargin(matchWrap(dp(48)), dp(8)));

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
        clockUseEnglishSwitch = new MaterialSwitch(context);
        clockUseEnglishSwitch.setText(R.string.clock_use_english);
        clockUseEnglishSwitch.setTextAppearance(
            com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        clockUseEnglishSwitch.setChecked(repository.isClockUseEnglish());
        functionContent.addView(clockUseEnglishSwitch, topMargin(matchWrap(dp(48)), dp(8)));

        networkTimeSwitch.setOnCheckedChangeListener((button, checked) -> updateFunctionLockedState(checked));
        updateFunctionLockedState(networkTimeSwitch.isChecked());

        // ---- Boards container ----
        final LinearLayout boards = new LinearLayout(context);
        boards.setOrientation(LinearLayout.VERTICAL);
        boards.addView(styleContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));
        boards.addView(functionContent, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean style = tab.getPosition() == 0;
                styleContent.setVisibility(style ? View.VISIBLE : View.GONE);
                functionContent.setVisibility(style ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(boards, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollContent.addView(content, matchWrap(ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(scrollContent, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

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
        return label;
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

    private void updateSmallSecondsState(boolean showSeconds) {
        smallSecondsSwitch.setEnabled(showSeconds);
        smallSecondsSwitch.setAlpha(showSeconds ? 1f : 0.4f);
    }

    private View createSizeRow(Context context, int labelRes, SeekBar bar, TextView valueLabel) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText(labelRes);
        label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        row.addView(label, new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));

        bar.setMax(SEEK_MAX);
        row.addView(bar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        valueLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        valueLabel.setGravity(Gravity.END);
        row.addView(valueLabel, new LinearLayout.LayoutParams(dp(88), ViewGroup.LayoutParams.WRAP_CONTENT));
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

    private MaterialButton createModeButton(Context context, int id, int textRes) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setId(id);
        button.setText(textRes);
        button.setCheckable(true);
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
            swatch.setContentDescription(getContext().getString(R.string.use_color));
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

    private String regionSummary() {
        return getContext().getString(R.string.region_time_zone) + ": "
                + RegionTimeZones.DISPLAY_NAMES[selectedRegionIndex];
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

        modeGroup.check(COLOR_MODE_ID);
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
        showSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_SECONDS);
        smallSecondsSwitch.setChecked(ClockPreferences.DEFAULT_SMALL_SECONDS);
        showLunarSwitch.setChecked(ClockPreferences.DEFAULT_SHOW_LUNAR);
        use24HourSwitch.setChecked(ClockPreferences.DEFAULT_USE_24_HOUR);
        clockUseEnglishSwitch.setChecked(ClockPreferences.DEFAULT_CLOCK_USE_ENGLISH);
        dimBackgroundSwitch.setChecked(ClockPreferences.DEFAULT_DIM_BACKGROUND);
        scheduleDimBackgroundSwitch.setChecked(ClockPreferences.DEFAULT_SCHEDULE_DIM_BACKGROUND);
        dimStartMinutes = ClockPreferences.DEFAULT_DIM_START_MINUTES;
        dimEndMinutes = ClockPreferences.DEFAULT_DIM_END_MINUTES;
        updateDimTimeLabels();

        Toast.makeText(getContext(), R.string.reset_default, Toast.LENGTH_SHORT).show();
    }

    private void applySelection() {
        repository.setTimeFontScale(progressToScale(timeSizeBar.getProgress()));        repository.setDateFontScale(progressToScale(dateSizeBar.getProgress()));
        repository.setTimeColor(timeColor);
        repository.setDateColor(dateColor);
        repository.setShowStatusIcons(statusIconsSwitch.isChecked());
        repository.setBlinkColon(blinkColonSwitch.isChecked());
        repository.setAnimateTimeChanges(animateTimeChangesSwitch.isChecked());
        repository.setBoldText(boldTextSwitch.isChecked());
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
        repository.setSyncIntervalMinutes(minutesForSyncIntervalId(syncIntervalGroup.getCheckedButtonId()));
        repository.setTimeZoneId(RegionTimeZones.ZONE_IDS[selectedRegionIndex]);
        listener.onFontSettingsApplied();

        if (modeGroup.getCheckedButtonId() == IMAGE_MODE_ID) {
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
        float range = ClockPreferences.MAX_FONT_SCALE - ClockPreferences.MIN_FONT_SCALE;
        return Math.round((scale - ClockPreferences.MIN_FONT_SCALE) / range * SEEK_MAX);
    }

    private static float progressToScale(int progress) {
        float range = ClockPreferences.MAX_FONT_SCALE - ClockPreferences.MIN_FONT_SCALE;
        return ClockPreferences.MIN_FONT_SCALE + (progress / (float) SEEK_MAX) * range;
    }

    private static int progressToPercent(int progress) {
        return Math.round(progressToScale(progress) * 100f);
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
