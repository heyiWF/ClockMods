package com.clockmods.pro.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.platform.ExperienceBridge;
import com.clockmods.pro.CalendarDashboardSizing;
import com.clockmods.pro.CalendarDayNumberView;
import com.clockmods.pro.CalendarTheme;
import com.clockmods.pro.MonthGestureLayout;
import com.clockmods.pro.style.CalendarLayoutCapabilities.Capability;
import com.clockmods.ui.CalendarFooterCarouselView;
import com.clockmods.ui.CalendarLabelCarouselView;
import com.clockmods.ui.ClockTypefaceResolver;
import com.clockmods.ui.QWeatherLogoView;
import com.clockmods.ui.StatusBarView;
import com.clockmods.ui.WeatherIconView;
import com.clockmods.weather.WeatherModels;
import com.clockmods.weather.WeatherTemperatureFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.loadingindicator.LoadingIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * The month dashboard: a clock/weather column beside a 7×6 month panel. This is the composition the
 * calendar page has always had, lifted out of {@code ProCalendarFragment} unchanged.
 *
 * <p>It is also its own {@link CalendarPager} — the two grid layers that slide past each other are
 * part of the composition, so there is nothing to gain from a separate object.</p>
 *
 * <p>The presets that drop the side column reuse this class with {@link Capability#CLOCK} absent,
 * which is exactly what the old {@code CalendarTheme.sideColumn} flag did.</p>
 */
public class DashboardCalendarLayout implements CalendarLayout, CalendarPager {
    private static final Set<CalendarPageState.DayDetail> DAY_DETAILS =
            EnumSet.of(CalendarPageState.DayDetail.LUNAR, CalendarPageState.DayDetail.FESTIVALS,
                    CalendarPageState.DayDetail.HOLIDAY);
    private static final Set<CalendarPageState.DayDetail> SELECTION_DETAILS =
            EnumSet.of(CalendarPageState.DayDetail.LUNAR,
                    CalendarPageState.DayDetail.ALMANAC_TABOO);

    private final CalendarLayoutCapabilities capabilities;
    private final List<CalendarLabelCarouselView> labelCarousels = new ArrayList<>();
    /** Index-aligned with the bound page's days; replaces the old {@code grid.getChildAt(i)} pairing. */
    private final List<View> dayCells = new ArrayList<>();

    private Context context;
    private View root;
    private CalendarLayoutHost host;

    private TextView timeView, secondsView, periodView, title, currentTemperature, feelsLike,
            feelsLikeLabel, weatherSummary;
    private CalendarFooterCarouselView footer;
    private GridLayout weekdayGrid, grid, previewGrid;
    private StatusBarView statusBar;
    private WeatherIconView currentWeatherIcon;
    private LoadingIndicator weatherLoading;
    private View clockCard, currentWeatherCard, forecastCard, attribution, leftPane, monthPane;
    private View monthToolbar;
    private MaterialButton previousButton, todayButton, nextButton;
    private MonthGestureLayout monthPanel;
    private LinearLayout[] forecastColumns;

    private ClockPreferences preferences;
    private CalendarTheme theme = CalendarTheme.resolve(null);
    private CalendarPageState pageState;
    private boolean active;
    private int previewDirection;

    /** Margins the month pane was inflated with, restored whenever the side column comes back. */
    private boolean monthPaneMarginCaptured;
    private int monthPaneMarginTop, monthPaneMarginStart;

    private boolean responsiveSizingPosted;
    private final Runnable responsiveSizingRunnable = () -> {
        responsiveSizingPosted = false;
        if (root != null) applyResponsiveSizing();
    };

    public DashboardCalendarLayout(CalendarLayoutCapabilities capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("Capabilities must not be null");
        }
        this.capabilities = capabilities;
    }

    // region lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            CalendarLayoutHost host) {
        this.context = inflater.getContext();
        this.host = host;
        root = inflater.inflate(R.layout.calendar_layout_dashboard, container, false);
        bindViews(root);
        configureViews();
        return root;
    }

    private void bindViews(View root) {
        timeView = root.findViewById(R.id.calendar_time);
        secondsView = root.findViewById(R.id.calendar_seconds);
        periodView = root.findViewById(R.id.calendar_period);
        title = root.findViewById(R.id.calendar_month_title);
        monthToolbar = root.findViewById(R.id.calendar_month_toolbar);
        footer = root.findViewById(R.id.calendar_selected_footer);
        weekdayGrid = root.findViewById(R.id.calendar_weekdays);
        grid = root.findViewById(R.id.calendar_grid);
        previewGrid = root.findViewById(R.id.calendar_grid_preview);
        statusBar = root.findViewById(R.id.calendar_status_bar);
        currentWeatherIcon = root.findViewById(R.id.calendar_current_weather_icon);
        weatherLoading = root.findViewById(R.id.calendar_weather_loading);
        currentTemperature = root.findViewById(R.id.calendar_current_temperature);
        feelsLike = root.findViewById(R.id.calendar_feels_like);
        feelsLikeLabel = root.findViewById(R.id.calendar_feels_like_label);
        weatherSummary = root.findViewById(R.id.calendar_weather_summary);
        clockCard = root.findViewById(R.id.calendar_clock_card);
        currentWeatherCard = root.findViewById(R.id.calendar_current_weather_card);
        forecastCard = root.findViewById(R.id.calendar_forecast_card);
        attribution = root.findViewById(R.id.calendar_weather_attribution);
        leftPane = root.findViewById(R.id.calendar_left_pane);
        monthPane = root.findViewById(R.id.calendar_month_pane);
        monthPanel = root.findViewById(R.id.calendar_month_panel);
        previousButton = root.findViewById(R.id.calendar_previous);
        todayButton = root.findViewById(R.id.calendar_today);
        nextButton = root.findViewById(R.id.calendar_next);
        forecastColumns = new LinearLayout[] {root.findViewById(R.id.calendar_forecast_today),
                root.findViewById(R.id.calendar_forecast_tomorrow),
                root.findViewById(R.id.calendar_forecast_after_tomorrow)};
    }

    private void configureViews() {
        title.setAccessibilityHeading(true);
        previousButton.setOnClickListener(view -> host.onPageRequested(-1));
        nextButton.setOnClickListener(view -> host.onPageRequested(1));
        todayButton.setOnClickListener(view -> host.onTodayRequested());
        title.setOnClickListener(view -> host.onMonthPickerRequested());
        title.setTooltipText(context.getString(R.string.calendar_jump_title));
        attribution.setOnClickListener(view -> host.onAttributionClicked());
        monthPanel.setMonthGestureListener(new MonthGestureLayout.Listener() {
            @Override
            public void onMonthDrag(float offset) {
                host.onPageDragged(offset);
            }

            @Override
            public void onMonthDragFinished(int direction) {
                host.onPageDragFinished(direction);
            }
        });
        clockCard.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        currentWeatherCard.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        forecastCard.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        monthPanel.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        scheduleResponsiveSizing();
    }

    @Override
    public void onDestroyView() {
        if (monthPanel != null) monthPanel.removeCallbacks(responsiveSizingRunnable);
        responsiveSizingPosted = false;
        labelCarousels.clear();
        dayCells.clear();
        pageState = null;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
        if (statusBar != null) {
            if (active) statusBar.start(); else statusBar.stop();
        }
        for (CalendarLabelCarouselView carousel : labelCarousels) carousel.setActive(active);
        if (footer != null) footer.setActive(active);
    }

    // endregion
    // region settings and theme

    @Override
    public void applySettings(CalendarTheme theme, ClockPreferences preferences,
            BackgroundRepository background) {
        this.theme = theme;
        this.preferences = preferences;
        statusBar.setBackgroundRepository(background);
        statusBar.setContentAlignedStart(true);
        statusBar.setTintOverride(theme.followsUserTimeColor ? 0 : theme.text);
        statusBar.setVisibility(preferences.isShowStatusIcons() ? View.VISIBLE : View.GONE);
        timeView.setTextColor(theme.followsUserTimeColor ? preferences.getTimeColor() : theme.text);
        currentWeatherIcon.setIconColor(preferences.isWeatherIconDynamicColor()
                ? ExperienceBridge.resolveAccentColor(context, theme.weatherIcon)
                : theme.weatherIcon);
        // Pure-calendar presets drop the whole clock/weather column, so the weather cards inside it
        // never become visible and the pollers stay idle regardless of the weather setting.
        boolean weatherEnabled = capabilities.supports(Capability.WEATHER)
                && preferences.isWeatherEnabled();
        applyTheme();
        currentWeatherCard.setVisibility(weatherEnabled ? View.VISIBLE : View.GONE);
        forecastCard.setVisibility(weatherEnabled ? View.VISIBLE : View.GONE);
        attribution.setVisibility(weatherEnabled ? View.VISIBLE : View.GONE);
    }

    private void applyTheme() {
        float density = context.getResources().getDisplayMetrics().density;
        root.setBackground(theme.newPageBackground());
        applyPanelBackground(clockCard);
        applyPanelBackground(currentWeatherCard);
        applyPanelBackground(forecastCard);
        applyPanelBackground(monthPanel);
        boolean sideColumn = capabilities.supports(Capability.CLOCK);
        if (leftPane != null) leftPane.setVisibility(sideColumn ? View.VISIBLE : View.GONE);
        if (monthPane != null && monthPane.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            // The gutter between the two panes is dead space once the side column is gone. Which
            // edge carries it depends on the orientation, so both are captured and restored.
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) monthPane.getLayoutParams();
            if (!monthPaneMarginCaptured) {
                monthPaneMarginCaptured = true;
                monthPaneMarginTop = params.topMargin;
                monthPaneMarginStart = params.getMarginStart();
            }
            params.topMargin = sideColumn ? monthPaneMarginTop : 0;
            params.setMarginStart(sideColumn ? monthPaneMarginStart : 0);
            monthPane.setLayoutParams(params);
        }
        title.setTextColor(theme.text);
        previousButton.setIconTint(ColorStateList.valueOf(theme.secondary));
        todayButton.setIconTint(ColorStateList.valueOf(theme.accent));
        nextButton.setIconTint(ColorStateList.valueOf(theme.secondary));
        secondsView.setTextColor(theme.accent);
        periodView.setTextColor(theme.secondary);
        currentTemperature.setTextColor(theme.text);
        feelsLikeLabel.setTextColor(theme.secondary);
        feelsLike.setTextColor(theme.accent);
        weatherSummary.setTextColor(theme.text);
        if (weatherLoading != null) {
            // The default indicator follows the Material theme, but a calendar preset paints its
            // own surface — so it takes the preset's accent on a transparent container instead.
            weatherLoading.setIndicatorColor(theme.accent);
            weatherLoading.setContainerColor(Color.TRANSPARENT);
        }
        tintAttribution(density);
    }

    private void applyPanelBackground(View card) {
        if (card == null) return;
        // GradientDrawable reports no intrinsic padding, so setBackground leaves the XML padding
        // alone on every platform we ship to — but the codebase saves and restores it anyway.
        int left = card.getPaddingLeft();
        int top = card.getPaddingTop();
        int right = card.getPaddingRight();
        int bottom = card.getPaddingBottom();
        card.setBackground(theme.newPanelBackground(
                context.getResources().getDisplayMetrics().density));
        card.setPadding(left, top, right, bottom);
    }

    /** The attribution row has no ids of its own; its two labels and the logo are tinted by type. */
    private void tintAttribution(float density) {
        if (!(attribution instanceof ViewGroup)) return;
        ViewGroup row = (ViewGroup) attribution;
        for (int index = 0; index < row.getChildCount(); index++) {
            View child = row.getChildAt(index);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(withAlpha(theme.secondary, 0.66f));
            } else if (child instanceof QWeatherLogoView) {
                ((QWeatherLogoView) child).setLogoColor(withAlpha(theme.secondary, 0.8f));
            }
        }
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(255 * alpha) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void applyTypefaces() {
        Typeface typeface = ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), preferences.isBoldText());
        timeView.setTypeface(typeface);
        secondsView.setTypeface(typeface);
        periodView.setTypeface(typeface);
    }

    // endregion
    // region sizing

    @Override
    public void applyResponsiveSizing() {
        applyClockSizing();
        applyCurrentWeatherSizing();
        applyForecastSizing();
        applyMonthSizing();
    }

    private void scheduleResponsiveSizing() {
        if (responsiveSizingPosted || monthPanel == null) return;
        responsiveSizingPosted = true;
        if (!monthPanel.post(responsiveSizingRunnable)) responsiveSizingPosted = false;
    }

    private void applyClockSizing() {
        if (clockCard == null || clockCard.getWidth() <= 0 || clockCard.getHeight() <= 0) return;
        float density = context.getResources().getDisplayMetrics().density;
        float timeSize = CalendarDashboardSizing.clockTimeSize(clockCard.getWidth(),
                clockCard.getHeight(), preferences.isShowSeconds(), density);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_PX, timeSize);
        secondsView.setTextSize(TypedValue.COMPLEX_UNIT_PX, timeSize * 0.46f);
        periodView.setTextSize(TypedValue.COMPLEX_UNIT_PX, timeSize * 0.25f);
        ViewGroup.LayoutParams statusParams = statusBar.getLayoutParams();
        int statusWidth = Math.round(clockCard.getWidth() * 0.38f);
        int statusHeight = Math.round(Math.min(clockCard.getHeight() * 0.16f,
                clockCard.getWidth() * 0.065f));
        if (statusParams.width != statusWidth || statusParams.height != statusHeight) {
            statusParams.width = statusWidth;
            statusParams.height = statusHeight;
            statusBar.setLayoutParams(statusParams);
        }
        ViewGroup.LayoutParams rawPeriodParams = periodView.getLayoutParams();
        if (rawPeriodParams instanceof android.widget.RelativeLayout.LayoutParams) {
            android.widget.RelativeLayout.LayoutParams periodParams =
                    (android.widget.RelativeLayout.LayoutParams) rawPeriodParams;
            int bottomMargin = -Math.round(timeSize * 0.04f);
            if (periodParams.bottomMargin != bottomMargin) {
                periodParams.bottomMargin = bottomMargin;
                periodView.setLayoutParams(periodParams);
            }
        }
    }

    private void applyCurrentWeatherSizing() {
        if (currentWeatherCard == null || currentWeatherCard.getWidth() <= 0
                || currentWeatherCard.getHeight() <= 0) return;
        float width = currentWeatherCard.getWidth();
        float height = currentWeatherCard.getHeight();
        float density = context.getResources().getDisplayMetrics().density;
        float iconSize = CalendarDashboardSizing.weatherIconSize(width, height, density);
        float temperatureSize = CalendarDashboardSizing.weatherTemperatureSize(
                width, height, density);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(currentTemperature,
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(feelsLikeLabel,
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(feelsLike,
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(weatherSummary,
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        currentTemperature.setTextSize(TypedValue.COMPLEX_UNIT_PX, temperatureSize);
        feelsLikeLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, temperatureSize * 0.26f);
        feelsLike.setTextSize(TypedValue.COMPLEX_UNIT_PX, temperatureSize * 0.53f);
        weatherSummary.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                CalendarDashboardSizing.weatherSummarySize(width, height, density));
        ViewGroup.LayoutParams iconParams = currentWeatherIcon.getLayoutParams();
        int roundedIconSize = Math.round(iconSize);
        boolean iconSizeChanged;
        if (currentWeatherCard instanceof LinearLayout
                && ((LinearLayout) currentWeatherCard).getOrientation() == LinearLayout.HORIZONTAL) {
            iconSizeChanged = iconParams.height != roundedIconSize;
            iconParams.height = roundedIconSize;
        } else {
            iconSizeChanged = iconParams.width != roundedIconSize
                    || iconParams.height != roundedIconSize;
            iconParams.width = roundedIconSize;
            iconParams.height = roundedIconSize;
        }
        if (iconSizeChanged) currentWeatherIcon.setLayoutParams(iconParams);
        if (weatherLoading != null) {
            weatherLoading.setIndicatorSize(Math.round(iconSize * 0.62f));
            weatherLoading.setContainerWidth(roundedIconSize);
            weatherLoading.setContainerHeight(roundedIconSize);
        }
        int horizontalPadding = Math.round(CalendarDashboardSizing.spacing(
                width * 0.02f, density, 12f));
        int verticalPadding = Math.round(CalendarDashboardSizing.spacing(
                height * 0.035f, density, 8f));
        setPadding(currentWeatherCard, horizontalPadding, verticalPadding);
    }

    private void applyForecastSizing() {
        if (forecastCard == null || forecastCard.getWidth() <= 0 || forecastCard.getHeight() <= 0) {
            return;
        }
        float density = context.getResources().getDisplayMetrics().density;
        float cardHeight = forecastCard.getHeight();
        int horizontalPadding = Math.round(CalendarDashboardSizing.spacing(
                forecastCard.getWidth() * 0.012f, density, 8f));
        int verticalPadding = Math.round(CalendarDashboardSizing.spacing(
                cardHeight * 0.012f, density, 6f));
        setPadding(forecastCard, horizontalPadding, verticalPadding);
        float columnWidth = Math.max(1f,
                (forecastCard.getWidth() - horizontalPadding * 2f) / 3f);
        int iconSize = Math.round(CalendarDashboardSizing.forecastIconSize(
                columnWidth, cardHeight, density));
        int iconGap = Math.max(1, Math.round(CalendarDashboardSizing.spacing(
                cardHeight * 0.05f, density, 15f)));
        int detailGap = Math.max(1, Math.round(CalendarDashboardSizing.spacing(
                cardHeight * 0.024f, density, 8f)));
        for (LinearLayout column : forecastColumns) {
            for (int index = 0; index < column.getChildCount(); index++) {
                View child = column.getChildAt(index);
                if (child instanceof WeatherIconView) {
                    LinearLayout.LayoutParams params =
                            (LinearLayout.LayoutParams) child.getLayoutParams();
                    if (params.width != iconSize || params.height != iconSize
                            || params.leftMargin != 0 || params.topMargin != iconGap
                            || params.rightMargin != 0 || params.bottomMargin != iconGap) {
                        params.width = iconSize;
                        params.height = iconSize;
                        params.setMargins(0, iconGap, 0, iconGap);
                        child.setLayoutParams(params);
                    }
                } else if (child instanceof TextView && child.getTag() instanceof Integer) {
                    int sizeRes = (Integer) child.getTag();
                    TextView text = (TextView) child;
                    LinearLayout.LayoutParams params =
                            (LinearLayout.LayoutParams) child.getLayoutParams();
                    int topMargin = sizeRes == R.dimen.calendar_forecast_min_text_size
                            ? detailGap : 0;
                    if (params.leftMargin != 0 || params.topMargin != topMargin
                            || params.rightMargin != 0 || params.bottomMargin != 0) {
                        params.setMargins(0, topMargin, 0, 0);
                        child.setLayoutParams(params);
                    }
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(text,
                            TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                    text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            forecastTextSize(sizeRes, columnWidth, cardHeight, density));
                }
            }
        }
    }

    private float forecastTextSize(int sizeRes, float columnWidth, float cardHeight,
            float density) {
        if (sizeRes == R.dimen.calendar_forecast_label_size) {
            return CalendarDashboardSizing.forecastHeadingSize(columnWidth, cardHeight, density);
        }
        if (sizeRes == R.dimen.calendar_forecast_min_text_size) {
            return CalendarDashboardSizing.forecastDetailSize(columnWidth, cardHeight, density);
        }
        return CalendarDashboardSizing.forecastTextSize(columnWidth, cardHeight, density);
    }

    private void applyMonthSizing() {
        if (monthPanel == null || monthPanel.getWidth() <= 0 || monthPanel.getHeight() <= 0) return;
        int panelWidth = monthPanel.getWidth() - monthPanel.getPaddingLeft()
                - monthPanel.getPaddingRight();
        int panelHeight = monthPanel.getHeight() - monthPanel.getPaddingTop()
                - monthPanel.getPaddingBottom();
        float density = context.getResources().getDisplayMetrics().density;
        int toolbarHeight = Math.round(CalendarDashboardSizing.monthToolbarHeight(
                panelHeight, density));
        int weekdayHeight = Math.round(CalendarDashboardSizing.monthWeekdayHeight(
                panelHeight, density));
        int footerHeight = Math.round(CalendarDashboardSizing.monthFooterHeight(
                panelHeight, density));
        setViewHeight(monthToolbar, toolbarHeight);
        setViewHeight(weekdayGrid, weekdayHeight);
        setViewHeight(footer, footerHeight);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                CalendarDashboardSizing.monthTitleSize(toolbarHeight, density));
        applyMonthButtonSize(previousButton, toolbarHeight);
        applyMonthButtonSize(todayButton, toolbarHeight);
        applyMonthButtonSize(nextButton, toolbarHeight);
        float weekdaySize = CalendarDashboardSizing.monthWeekdaySize(
                panelWidth / 7f, weekdayHeight, density);
        for (int index = 0; index < weekdayGrid.getChildCount(); index++) {
            View weekday = weekdayGrid.getChildAt(index);
            if (weekday instanceof TextView) {
                ((TextView) weekday).setTextSize(TypedValue.COMPLEX_UNIT_PX, weekdaySize);
            }
        }
        footer.setTextSizePx(CalendarDashboardSizing.monthFooterSize(footerHeight, density));
        float cellWidth = Math.max(1f, (grid.getWidth() > 0 ? grid.getWidth() : panelWidth) / 7f);
        float remainingGridHeight = Math.max(1f, panelHeight - toolbarHeight - weekdayHeight
                - footerHeight);
        float cellHeight = Math.max(1f, remainingGridHeight / 6f);
        applyGridSizing(grid, cellWidth, cellHeight, density);
        applyGridSizing(previewGrid, cellWidth, cellHeight, density);
    }

    private void applyMonthButtonSize(MaterialButton button, int toolbarHeight) {
        ViewGroup.LayoutParams params = button.getLayoutParams();
        if (params.width != toolbarHeight || params.height != toolbarHeight) {
            params.width = toolbarHeight;
            params.height = toolbarHeight;
            button.setLayoutParams(params);
        }
        int iconSize = Math.round(toolbarHeight * 0.44f);
        if (button.getIconSize() != iconSize) button.setIconSize(iconSize);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
    }

    private void applyGridSizing(GridLayout target, float cellWidth, float cellHeight,
            float density) {
        float lunarSize = CalendarDashboardSizing.monthLunarSize(cellWidth, cellHeight, density);
        int labelHeight = Math.max(1, Math.round(
                CalendarDashboardSizing.monthLabelHeight(cellHeight, lunarSize)));
        int horizontalPadding = Math.max(1, Math.round(CalendarDashboardSizing.spacing(
                cellWidth * 0.015f, density, 3f)));
        int verticalPadding = Math.max(1, Math.round(CalendarDashboardSizing.spacing(
                cellHeight * 0.025f, density, 3f)));
        for (int index = 0; index < target.getChildCount(); index++) {
            View child = target.getChildAt(index);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout cell = (LinearLayout) child;
            setPadding(cell, horizontalPadding, verticalPadding);
            for (int item = 0; item < cell.getChildCount(); item++) {
                View content = cell.getChildAt(item);
                if (content instanceof CalendarLabelCarouselView) {
                    ((CalendarLabelCarouselView) content).setTextSizePx(lunarSize);
                    ViewGroup.LayoutParams params = content.getLayoutParams();
                    if (params.height != labelHeight) {
                        params.height = labelHeight;
                        content.setLayoutParams(params);
                    }
                }
            }
        }
    }

    private static void setViewHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.height == height) return;
        params.height = height;
        view.setLayoutParams(params);
    }

    private static void setPadding(View view, int horizontal, int vertical) {
        if (view.getPaddingLeft() == horizontal && view.getPaddingTop() == vertical
                && view.getPaddingRight() == horizontal
                && view.getPaddingBottom() == vertical) return;
        view.setPadding(horizontal, vertical, horizontal, vertical);
    }

    // endregion
    // region binding

    @Override
    public void bindClock(CalendarClockState clock) {
        if (timeView == null) return;
        timeView.setText(clock.time);
        secondsView.setVisibility(clock.showSeconds ? View.VISIBLE : View.GONE);
        secondsView.setText(clock.seconds);
        if (periodView != null) {
            periodView.setVisibility(clock.showPeriod ? View.VISIBLE : View.GONE);
            if (clock.showPeriod) periodView.setText(clock.period);
        }
    }

    @Override
    public void bindWeather(WeatherModels.WeatherState state) {
        if (currentTemperature == null) return;
        // Only an in-flight request spins; an error keeps its message, since a spinner that never
        // stops reads as a hang rather than as a failure.
        setWeatherLoading(state.data == null
                && state.status == WeatherModels.Status.LOADING);
        if (state.data == null) {
            weatherSummary.setText(state.message == null
                    ? context.getString(R.string.calendar_forecast_loading) : state.message);
            return;
        }
        WeatherModels.WeatherDisplayData data = state.data;
        currentWeatherIcon.setIconCode(data.icon, preferences.isWeatherIconFill());
        currentTemperature.setText(temperatureLabel(data.temperature));
        String feel = data.detail == null || empty(data.detail.feelsLike)
                ? "--" : data.detail.feelsLike;
        feelsLike.setText(temperatureLabel(feel));
        StringBuilder summary =
                new StringBuilder(WeatherModels.locationText(data.city, data.district));
        append(summary, data.text);
        if (data.detail != null) {
            String scale = empty(data.detail.windScale) ? ""
                    : context.getString(R.string.weather_wind_scale_format, data.detail.windScale);
            append(summary, join(data.detail.windDir, scale));
            if (!empty(data.detail.humidity)) {
                append(summary, context.getString(R.string.weather_humidity_format,
                        data.detail.humidity));
            }
        }
        weatherSummary.setText(summary.toString());
    }

    @Override
    public void bindForecast(WeatherModels.DailyForecastState state) {
        if (forecastColumns == null) return;
        if (state.data == null) {
            boolean loading = state.status == WeatherModels.Status.LOADING;
            for (int index = 0; index < forecastColumns.length; index++) {
                LinearLayout column = forecastColumns[index];
                column.removeAllViews();
                if (loading) {
                    // One indicator for the whole card, in the middle column: three spinners for
                    // a single request would suggest three requests.
                    if (index == 1) column.addView(newForecastLoadingIndicator());
                    continue;
                }
                addForecastText(column, state.message == null
                        ? context.getString(R.string.calendar_forecast_loading) : state.message,
                        R.dimen.calendar_forecast_text_size, false);
            }
            applyForecastSizing();
            return;
        }
        Calendar date = Calendar.getInstance(appTimeZone());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(appTimeZone());
        String[] forecastLabels =
                context.getResources().getStringArray(R.array.forecast_day_labels);
        for (int index = 0; index < forecastColumns.length; index++) {
            LinearLayout column = forecastColumns[index];
            column.removeAllViews();
            WeatherModels.DailyForecast forecast =
                    state.data.findByDate(format.format(date.getTime()));
            if (forecast != null) {
                addForecastHeading(column, forecastLabels[index], forecast.textDay, index == 0);
                WeatherIconView icon = new WeatherIconView(context);
                int accent = preferences.isWeatherIconDynamicColor()
                        ? ExperienceBridge.resolveAccentColor(context, theme.weatherIcon)
                        : theme.weatherIcon;
                icon.setIconColor(index == 0 ? accent : theme.forecastIcon);
                icon.setIconCode(forecast.iconDay, preferences.isWeatherIconFill());
                int iconSize = context.getResources().getDimensionPixelSize(
                        R.dimen.calendar_forecast_icon_size);
                int iconGap = context.getResources().getDimensionPixelSize(
                        R.dimen.calendar_forecast_icon_gap);
                LinearLayout.LayoutParams iconParams =
                        new LinearLayout.LayoutParams(iconSize, iconSize);
                iconParams.gravity = Gravity.CENTER_HORIZONTAL;
                iconParams.setMargins(0, iconGap, 0, iconGap);
                column.addView(icon, iconParams);
                addForecastText(column, temperatureRangeLabel(forecast.tempMin, forecast.tempMax),
                        R.dimen.calendar_forecast_text_size, false);
                StringBuilder details = new StringBuilder();
                if (!empty(forecast.humidity)) {
                    append(details, context.getString(R.string.weather_humidity_format,
                            forecast.humidity));
                }
                String windScale = empty(forecast.windScaleDay) ? ""
                        : context.getString(R.string.weather_wind_scale_format,
                                forecast.windScaleDay);
                append(details, join(forecast.windDirDay, windScale));
                if (details.length() > 0 && context.getResources().getBoolean(
                        R.bool.calendar_forecast_show_details)) {
                    TextView detailView = addForecastText(column, details.toString(),
                            R.dimen.calendar_forecast_min_text_size, false);
                    detailView.setTextColor(theme.secondary);
                }
            } else {
                addForecastHeading(column, forecastLabels[index], "", index == 0);
                addForecastText(column, context.getString(R.string.calendar_forecast_unavailable),
                        R.dimen.calendar_forecast_text_size, false);
            }
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
        applyForecastSizing();
    }

    private String temperatureLabel(String celsius) {
        String unit = preferences.getWeatherTemperatureUnit();
        return WeatherTemperatureFormatter.replaceUnit(
                context.getString(R.string.weather_temperature_format,
                        WeatherTemperatureFormatter.numeric(celsius, unit)), unit);
    }

    private String temperatureRangeLabel(String minimumCelsius, String maximumCelsius) {
        String unit = preferences.getWeatherTemperatureUnit();
        return WeatherTemperatureFormatter.replaceUnit(
                context.getString(R.string.weather_temperature_range_format,
                        WeatherTemperatureFormatter.numeric(minimumCelsius, unit),
                        WeatherTemperatureFormatter.numeric(maximumCelsius, unit)), unit);
    }

    private void addForecastHeading(LinearLayout parent, String dayLabel, String weather,
            boolean accent) {
        String text = join(dayLabel, weather);
        SpannableString heading = new SpannableString(text);
        heading.setSpan(new StyleSpan(Typeface.BOLD), 0, dayLabel.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (accent) {
            heading.setSpan(new ForegroundColorSpan(theme.accent),
                    0, dayLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        addForecastText(parent, heading, R.dimen.calendar_forecast_label_size, false);
    }

    private TextView addForecastText(LinearLayout parent, CharSequence text, int sizeRes,
            boolean accent) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTag(sizeRes);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(sizeRes));
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setTextColor(accent ? theme.accent : theme.text);
        view.setTypeface(ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), false));
        int maximumTextSize = context.getResources().getDimensionPixelSize(sizeRes);
        int minimumTextSize = context.getResources().getDimensionPixelSize(
                R.dimen.calendar_forecast_min_text_size);
        if (maximumTextSize > minimumTextSize) {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(view,
                    minimumTextSize, maximumTextSize, 1, TypedValue.COMPLEX_UNIT_PX);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        parent.addView(view, params);
        return view;
    }

    @Override
    public void bindWeekdays(CalendarPageState state) {
        if (weekdayGrid == null) return;
        weekdayGrid.removeAllViews();
        for (int offset = 0; offset < state.weekdayNames.length; offset++) {
            TextView label = new TextView(context);
            label.setText(state.weekdayNames[offset]);
            label.setGravity(Gravity.CENTER);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    context.getResources().getDimension(R.dimen.calendar_weekday_text_size));
            label.setTextColor(state.highlightWeekends && state.weekdayWeekend[offset]
                    ? theme.weekend : theme.weekday);
            label.setTypeface(ClockTypefaceResolver.resolveTime(context,
                    preferences.getFontFamily(), false));
            weekdayGrid.addView(label, cellParams());
        }
        applyMonthSizing();
    }

    @Override
    public void bind(CalendarPageState state) {
        if (grid == null) return;
        pageState = state;
        title.setText(state.title.full);
        grid.removeAllViews();
        labelCarousels.clear();
        dayCells.clear();
        for (int index = 0; index < state.days.size(); index++) {
            View cell = createDayCell(state.days.get(index), state,
                    index == state.selection.index, true);
            dayCells.add(cell);
            grid.addView(cell, cellParams());
        }
        updateFooter();
        applyMonthSizing();
    }

    /**
     * Refreshes only the selection highlight across the bound page, leaving every cell's carousel
     * running (used when the selected day changes within the visible window).
     */
    @Override
    public void updateSelection(CalendarPageState state) {
        pageState = state;
        int count = Math.min(dayCells.size(), state.days.size());
        for (int index = 0; index < count; index++) {
            applyCellSelection(dayCells.get(index), index == state.selection.index);
        }
        updateFooter();
    }

    private View createDayCell(CalendarPageState.DayInfo day, CalendarPageState state,
            boolean selected, boolean interactive) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(1), dp(2), dp(1), dp(2));
        cell.setAlpha(day.currentMonth ? 1f : 0.38f);
        CalendarDayNumberView solar = new CalendarDayNumberView(context);
        Typeface calendarTypeface = ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), false);
        solar.setTypefaces(calendarTypeface, calendarTypeface);
        int dayColor = theme.day;
        if (state.highlightWeekends && day.weekend) {
            dayColor = theme.weekend;
        }
        if (day.today) {
            dayColor = theme.today;
            solar.setFill(theme.todayFill);
        }
        solar.setDay(day.dayNumber, dayColor);
        if (day.holidayBadge.length() > 0) {
            solar.setBadge(day.holidayBadge,
                    day.holidayOffDay ? theme.restBadge : theme.workBadge);
        }
        float cellWidth = grid == null || grid.getWidth() <= 0 ? 0f : grid.getWidth() / 7f;
        float cellHeight = grid == null || grid.getHeight() <= 0 ? 0f : grid.getHeight() / 6f;
        float density = context.getResources().getDisplayMetrics().density;
        boolean measured = cellWidth > 0f && cellHeight > 0f;
        float lunarSize = measured
                ? CalendarDashboardSizing.monthLunarSize(cellWidth, cellHeight, density)
                : context.getResources().getDimension(R.dimen.calendar_dashboard_lunar_size);
        int labelHeight = cellHeight > 0f
                ? Math.max(1, Math.round(
                        CalendarDashboardSizing.monthLabelHeight(cellHeight, lunarSize)))
                : context.getResources().getDimensionPixelSize(R.dimen.calendar_label_height);
        // A pure-calendar preset hands the month the whole screen, and a stretched number view
        // would then leave as much air inside a cell as between two rows. Pinning it to the glyph
        // keeps the number and its label reading as one unit however tall the cell grows; the
        // 1.5 headroom is what stops the view from re-deriving a smaller size from its own height.
        float contentHeight = cellHeight - cell.getPaddingTop() - cell.getPaddingBottom();
        int solarHeight = measured
                ? Math.round(Math.min(
                        CalendarDashboardSizing.monthDaySize(
                                cellWidth - cell.getPaddingLeft() - cell.getPaddingRight(),
                                cellHeight, density) * 1.5f,
                        contentHeight - labelHeight))
                : 0;
        cell.addView(solar, solarHeight > 0
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, solarHeight)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        List<String> labels = day.festivals;
        List<String> carouselItems = new ArrayList<>();
        carouselItems.add(day.lunarShort);
        carouselItems.addAll(labels);
        CalendarLabelCarouselView lunar = new CalendarLabelCarouselView(context);
        lunar.setItems(carouselItems);
        lunar.setTextSizePx(lunarSize);
        lunar.setTextColor(labels.isEmpty() ? theme.secondary : theme.text);
        lunar.setTypeface(calendarTypeface);
        lunar.setActive(interactive && active);
        if (interactive) labelCarousels.add(lunar);
        cell.addView(lunar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, labelHeight));
        cell.setTag(day.contentDescription);
        applyCellSelection(cell, selected);
        cell.setClickable(interactive);
        cell.setFocusable(interactive);
        if (interactive) cell.setOnClickListener(view -> host.onDaySelected(day));
        return cell;
    }

    /**
     * Applies (or clears) the selected-day highlight and accessibility suffix on a cell without
     * touching its lunar/festival carousel, so an in-progress rotation is preserved.
     */
    private void applyCellSelection(View cell, boolean selected) {
        int left = cell.getPaddingLeft();
        int top = cell.getPaddingTop();
        int right = cell.getPaddingRight();
        int bottom = cell.getPaddingBottom();
        cell.setBackground(selected ? theme.newSelectionBackground(
                context.getResources().getDisplayMetrics().density) : null);
        cell.setPadding(left, top, right, bottom);
        Object base = cell.getTag();
        if (base instanceof String) {
            cell.setContentDescription(selected
                    ? base + context.getString(R.string.calendar_selected_suffix) : (String) base);
        }
    }

    private void updateFooter() {
        if (footer == null) return;
        CalendarPageState.Selection selection = pageState == null
                ? CalendarPageState.Selection.NONE : pageState.selection;
        footer.setTypeface(ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), false));
        footer.setTextSizePx(footer.getHeight() > 0
                ? CalendarDashboardSizing.monthFooterSize(footer.getHeight(),
                        context.getResources().getDisplayMetrics().density)
                : context.getResources().getDimension(R.dimen.calendar_footer_text_size));
        List<CalendarFooterCarouselView.Item> items = new ArrayList<>();
        items.add(new CalendarFooterCarouselView.Item(selection.dateLine, theme.text));
        if (selection.suitableLine.length() > 0) {
            items.add(new CalendarFooterCarouselView.Item(selection.suitableLine, theme.suitable)
                    .withPinnedPrefix(
                            context.getString(R.string.calendar_suitable_prefix)));
        }
        if (selection.avoidLine.length() > 0) {
            items.add(new CalendarFooterCarouselView.Item(selection.avoidLine, theme.avoid)
                    .withPinnedPrefix(
                            context.getString(R.string.calendar_avoid_prefix)));
        }
        footer.setItems(items);
        footer.setActive(active);
    }

    /** Swaps the icon slot for the indicator, and back, without disturbing the card's layout. */
    private void setWeatherLoading(boolean loading) {
        if (weatherLoading == null || currentWeatherIcon == null) return;
        weatherLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        currentWeatherIcon.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private LoadingIndicator newForecastLoadingIndicator() {
        LoadingIndicator indicator = new LoadingIndicator(context);
        indicator.setIndicatorColor(theme.accent);
        indicator.setContainerColor(Color.TRANSPARENT);
        indicator.setContentDescription(context.getString(R.string.calendar_forecast_loading));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        indicator.setLayoutParams(params);
        return indicator;
    }

    @Override
    public void announcePage() {
        if (title != null) ViewCompat.setAccessibilityPaneTitle(title, title.getText());
    }

    @Override
    public Set<CalendarPageState.DayDetail> requiredDayDetails() {
        return DAY_DETAILS;
    }

    @Override
    public Set<CalendarPageState.DayDetail> requiredSelectionDetails() {
        return SELECTION_DETAILS;
    }

    // endregion
    // region paging

    @Nullable
    @Override
    public CalendarPager getPager() {
        return capabilities.supports(Capability.PAGE_SWIPE) ? this : null;
    }

    @Override
    public PageUnit unit() {
        return PageUnit.MONTH;
    }

    @Override
    public float pageWidth() {
        return grid == null ? 0f : grid.getWidth();
    }

    @Override
    public int preparedDirection() {
        return previewGrid != null && previewGrid.getVisibility() == View.VISIBLE
                ? previewDirection : 0;
    }

    @Override
    public void bindPreview(int direction, CalendarPageState adjacent) {
        if (grid == null || previewGrid == null) return;
        previewGrid.removeAllViews();
        for (int index = 0; index < adjacent.days.size(); index++) {
            previewGrid.addView(createDayCell(adjacent.days.get(index), adjacent,
                    index == adjacent.selection.index, false), cellParams());
        }
        previewDirection = direction;
        previewGrid.setTranslationX(direction * grid.getWidth());
        previewGrid.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPageOffset(float offsetPx) {
        if (grid == null) return;
        grid.setTranslationX(offsetPx);
        if (previewGrid != null && previewDirection != 0) {
            previewGrid.setTranslationX(offsetPx + previewDirection * grid.getWidth());
        }
    }

    @Override
    public void animateCommit(int direction, Runnable onSettled) {
        if (grid == null || previewGrid == null) return;
        final float width = grid.getWidth();
        grid.animate().cancel();
        previewGrid.animate().cancel();
        grid.animate().translationX(direction > 0 ? -width : width)
                .setDuration(210L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        previewGrid.animate().translationX(0f)
                .setDuration(210L)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(onSettled)
                .start();
    }

    @Override
    public void animateSnapBack(Runnable onSettled) {
        if (grid == null || previewGrid == null) return;
        grid.animate().cancel();
        previewGrid.animate().cancel();
        grid.animate().translationX(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        // With no neighbour built there is nothing to slide back, and nothing to settle either.
        if (previewDirection != 0) {
            previewGrid.animate().translationX(previewDirection * grid.getWidth())
                    .setDuration(180L)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(onSettled)
                    .start();
        }
    }

    @Override
    public void resetPages() {
        if (grid == null || previewGrid == null) return;
        grid.animate().cancel();
        previewGrid.animate().cancel();
        grid.setTranslationX(0f);
        previewGrid.setVisibility(View.INVISIBLE);
        previewGrid.removeAllViews();
        previewDirection = 0;
    }

    // endregion
    // region helpers

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    private TimeZone appTimeZone() {
        String id = preferences == null ? "" : preferences.getTimeZoneId();
        return id == null || id.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(id);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static boolean empty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static void append(StringBuilder value, String part) {
        if (empty(part)) return;
        if (value.length() > 0) value.append(" · ");
        value.append(part);
    }

    private static String join(String first, String second) {
        if (empty(first)) return second;
        if (empty(second)) return first;
        return first + " " + second;
    }

    // endregion
}
