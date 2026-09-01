package com.clockmods.pro.style;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
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

import com.clockmods.R;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.pro.CalendarDashboardSizing;
import com.clockmods.pro.CalendarDayNumberView;
import com.clockmods.pro.CalendarTheme;
import com.clockmods.pro.CalendarWordmarkView;
import com.clockmods.pro.MonthGestureLayout;
import com.clockmods.pro.style.CalendarLayoutCapabilities.Capability;
import com.clockmods.ui.ClockTypefaceResolver;
import com.clockmods.weather.WeatherModels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 墨白排版: an oversized month wordmark, a hairline, and a grid of bare numbers. No cards, no lunar
 * text, no footer — the composition is the content, which is why it asks the host for
 * {@linkplain #requiredDayDetails() nothing at all} per day and costs zero almanac constructions a
 * page.
 *
 * <p>It keeps the dashboard's two-layer paging geometry, because that part is about gestures rather
 * than composition. Everything else differs: the toolbar is gone, the cell is a number over a mark
 * band, and today is a dot rather than a disc.</p>
 */
public final class PosterCalendarLayout implements CalendarLayout, CalendarPager {
    /** Out-of-month days, faded harder than the dashboard's 0.38 to keep the grid airy. */
    private static final float OTHER_MONTH_ALPHA = 0.18f;
    /** The hairline under the masthead, as a fraction of {@link CalendarTheme#secondary}. */
    private static final float RULE_ALPHA = 0.32f;

    private final CalendarLayoutCapabilities capabilities;
    /** Index-aligned with the bound page's days, so selection never goes through positions. */
    private final List<DayCell> dayCells = new ArrayList<>();

    private Context context;
    private View root;
    private CalendarLayoutHost host;

    private MonthGestureLayout monthPanel;
    private CalendarWordmarkView wordmark;
    private TextView todayButton;
    private View rule;
    private GridLayout weekdayGrid, grid, previewGrid;

    private ClockPreferences preferences;
    private CalendarTheme theme = CalendarTheme.resolve(CalendarTheme.ID_POSTER);
    private CalendarPageState pageState;
    private int previewDirection;

    private boolean responsiveSizingPosted;
    private final Runnable responsiveSizingRunnable = () -> {
        responsiveSizingPosted = false;
        if (root != null) applyResponsiveSizing();
    };

    public PosterCalendarLayout(CalendarLayoutCapabilities capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("Capabilities must not be null");
        }
        this.capabilities = capabilities;
    }

    // region lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            CalendarLayoutHost host) {
        context = inflater.getContext();
        this.host = host;
        root = inflater.inflate(R.layout.calendar_layout_poster, container, false);
        monthPanel = root.findViewById(R.id.calendar_month_panel);
        wordmark = root.findViewById(R.id.calendar_poster_wordmark);
        todayButton = root.findViewById(R.id.calendar_poster_today);
        rule = root.findViewById(R.id.calendar_poster_rule);
        weekdayGrid = root.findViewById(R.id.calendar_weekdays);
        grid = root.findViewById(R.id.calendar_grid);
        previewGrid = root.findViewById(R.id.calendar_grid_preview);
        configureViews();
        return root;
    }

    private void configureViews() {
        wordmark.setAccessibilityHeading(true);
        wordmark.setOnClickListener(view -> host.onMonthPickerRequested());
        wordmark.setTooltipText(context.getString(R.string.calendar_jump_title));
        todayButton.setOnClickListener(view -> host.onTodayRequested());
        monthPanel.setMonthGestureListener(new MonthGestureLayout.Listener() {
            @Override public void onMonthDrag(float offset) {
                host.onPageDragged(offset);
            }

            @Override public void onMonthDragFinished(int direction) {
                host.onPageDragFinished(direction);
            }
        });
        monthPanel.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        grid.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleResponsiveSizing());
        scheduleResponsiveSizing();
    }

    @Override
    public void onDestroyView() {
        if (monthPanel != null) monthPanel.removeCallbacks(responsiveSizingRunnable);
        responsiveSizingPosted = false;
        dayCells.clear();
        pageState = null;
    }

    /** Nothing on this page animates on its own, so being off-screen costs nothing to begin with. */
    @Override
    public void setActive(boolean active) { }

    // endregion
    // region settings and theme

    @Override
    public void applySettings(CalendarTheme theme, ClockPreferences preferences,
            BackgroundRepository background) {
        this.theme = theme;
        this.preferences = preferences;
        root.setBackground(theme.newPageBackground());
        rule.setBackgroundColor(withAlpha(theme.secondary, RULE_ALPHA));
        wordmark.setColors(theme.text, theme.secondary);
        todayButton.setTextColor(theme.accent);
    }

    @Override
    public void applyTypefaces() {
        // Both halves of the masthead are re-asserted here rather than set once at inflation: the
        // month follows the bold-text switch, and ProFontApplier cannot reach a Canvas view to do it.
        Typeface regular = ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), false);
        Typeface display = ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), preferences.isBoldText());
        wordmark.setTypefaces(display, regular);
        todayButton.setTypeface(regular);
        for (int index = 0; index < weekdayGrid.getChildCount(); index++) {
            View child = weekdayGrid.getChildAt(index);
            if (child instanceof TextView) ((TextView) child).setTypeface(regular);
        }
        applyCellTypefaces(grid, regular);
        applyCellTypefaces(previewGrid, regular);
    }

    private void applyCellTypefaces(GridLayout target, Typeface typeface) {
        for (int index = 0; index < target.getChildCount(); index++) {
            View child = target.getChildAt(index);
            if (child instanceof DayCell) ((DayCell) child).setTypeface(typeface);
        }
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(255 * alpha) << 24) | (color & 0x00FFFFFF);
    }

    // endregion
    // region sizing

    private void scheduleResponsiveSizing() {
        if (responsiveSizingPosted || monthPanel == null) return;
        responsiveSizingPosted = true;
        if (!monthPanel.post(responsiveSizingRunnable)) responsiveSizingPosted = false;
    }

    @Override
    public void applyResponsiveSizing() {
        if (monthPanel == null || monthPanel.getWidth() <= 0 || monthPanel.getHeight() <= 0) return;
        float density = context.getResources().getDisplayMetrics().density;
        int panelHeight = monthPanel.getHeight() - monthPanel.getPaddingTop()
                - monthPanel.getPaddingBottom();
        int weekdayHeight = Math.round(
                CalendarDashboardSizing.monthWeekdayHeight(panelHeight, density));
        setViewHeight(weekdayGrid, weekdayHeight);
        // The grid is the authority on column width: in landscape it sits in a column narrower than
        // the panel, so measuring off the panel would oversize every number by the masthead's share.
        if (grid.getWidth() <= 0 || grid.getHeight() <= 0) return;
        float cellWidth = Math.max(1f, grid.getWidth() / 7f);
        float cellHeight = Math.max(1f, grid.getHeight() / 6f);
        float weekdaySize = CalendarDashboardSizing.posterWeekdaySize(cellWidth, weekdayHeight,
                density);
        for (int index = 0; index < weekdayGrid.getChildCount(); index++) {
            View child = weekdayGrid.getChildAt(index);
            if (child instanceof TextView) {
                ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_PX, weekdaySize);
            }
        }
        // The 今天 affordance is the same order of chrome as a weekday label; matching them keeps
        // the page down to two type sizes plus the masthead.
        todayButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, weekdaySize);
        float numberSize = CalendarDashboardSizing.posterCellNumberSize(cellWidth, cellHeight,
                density);
        int markHeight = Math.max(1,
                Math.round(CalendarDashboardSizing.posterMarkHeight(numberSize, density)));
        applyCellSizing(grid, numberSize, markHeight);
        applyCellSizing(previewGrid, numberSize, markHeight);
    }

    private void applyCellSizing(GridLayout target, float numberSize, int markHeight) {
        for (int index = 0; index < target.getChildCount(); index++) {
            View child = target.getChildAt(index);
            if (child instanceof DayCell) ((DayCell) child).applySizing(numberSize, markHeight);
        }
    }

    private static void setViewHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.height == height) return;
        params.height = height;
        view.setLayoutParams(params);
    }

    // endregion
    // region binding

    @Override
    public void bindWeekdays(CalendarPageState state) {
        weekdayGrid.removeAllViews();
        Typeface typeface = ClockTypefaceResolver.resolveTime(context,
                preferences.getFontFamily(), false);
        for (int offset = 0; offset < state.weekdayNames.length; offset++) {
            TextView label = new TextView(context);
            label.setText(state.weekdayNames[offset]);
            label.setGravity(Gravity.CENTER);
            label.setIncludeFontPadding(false);
            label.setSingleLine(true);
            // Letter-spaced small caps is what makes a seven-word row read as a rule rather than as
            // seven more words competing with the numbers below it.
            label.setLetterSpacing(0.14f);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    context.getResources().getDimension(R.dimen.calendar_weekday_text_size));
            label.setTextColor(state.highlightWeekends && state.weekdayWeekend[offset]
                    ? theme.weekend : theme.weekday);
            label.setTypeface(typeface);
            weekdayGrid.addView(label, cellParams());
        }
        applyResponsiveSizing();
    }

    @Override
    public void bind(CalendarPageState state) {
        pageState = state;
        wordmark.setText(state.title.month, state.title.year);
        grid.removeAllViews();
        dayCells.clear();
        for (int index = 0; index < state.days.size(); index++) {
            DayCell cell = new DayCell(true);
            cell.bind(state.days.get(index), state, index == state.selection.index);
            dayCells.add(cell);
            grid.addView(cell, cellParams());
        }
        applyResponsiveSizing();
    }

    @Override
    public void updateSelection(CalendarPageState state) {
        pageState = state;
        int count = Math.min(dayCells.size(), state.days.size());
        for (int index = 0; index < count; index++) {
            dayCells.get(index).applySelection(index == state.selection.index);
        }
    }

    @Override
    public void announcePage() {
        if (wordmark != null && pageState != null) {
            ViewCompat.setAccessibilityPaneTitle(wordmark, pageState.title.full);
        }
    }

    /** Nothing per day: no lunar label, no festivals, no statutory badge. That is the whole style. */
    @Override
    public Set<CalendarPageState.DayDetail> requiredDayDetails() {
        return Collections.emptySet();
    }

    /** No footer either, so the selected day needs no almanac lines resolved for it. */
    @Override
    public Set<CalendarPageState.DayDetail> requiredSelectionDetails() {
        return Collections.emptySet();
    }

    @Override public void bindClock(CalendarClockState clock) { }

    @Override public void bindWeather(WeatherModels.WeatherState state) { }

    @Override public void bindForecast(WeatherModels.DailyForecastState state) { }

    // endregion
    // region paging

    @Nullable
    @Override
    public CalendarPager getPager() {
        return capabilities.supports(Capability.PAGE_SWIPE) ? this : null;
    }

    @Override public PageUnit unit() { return PageUnit.MONTH; }

    @Override public float pageWidth() { return grid == null ? 0f : grid.getWidth(); }

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
            DayCell cell = new DayCell(false);
            cell.bind(adjacent.days.get(index), adjacent, index == adjacent.selection.index);
            previewGrid.addView(cell, cellParams());
        }
        previewDirection = direction;
        previewGrid.setTranslationX(direction * grid.getWidth());
        previewGrid.setVisibility(View.VISIBLE);
        applyResponsiveSizing();
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
    // region cell

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        return params;
    }

    /**
     * One number over a mark band. Holding its own {@link CalendarPageState.DayInfo} is what lets
     * {@link #updateSelection} walk cells instead of pairing {@code grid.getChildAt(i)} with
     * {@code days.get(i)} — the positional coupling the dashboard inherited.
     */
    private final class DayCell extends LinearLayout {
        private final CalendarDayNumberView number;
        private final DayMarkView mark;
        private final boolean interactive;
        private String baseDescription = "";
        private boolean today;

        DayCell(boolean interactive) {
            super(PosterCalendarLayout.this.context);
            this.interactive = interactive;
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            number = new CalendarDayNumberView(getContext());
            // No badge and no disc, ever: the poster requests neither holiday details nor a fill.
            number.setBadge("", 0);
            number.setFill(0);
            addView(number, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
            mark = new DayMarkView(getContext());
            addView(mark, new LayoutParams(LayoutParams.MATCH_PARENT, 1));
        }

        void bind(CalendarPageState.DayInfo day, CalendarPageState state, boolean selected) {
            today = day.today;
            setAlpha(day.currentMonth ? 1f : OTHER_MONTH_ALPHA);
            int color = theme.day;
            if (state.highlightWeekends && day.weekend) color = theme.weekend;
            if (day.today) color = theme.today;
            number.setDay(day.dayNumber, color);
            setTypeface(ClockTypefaceResolver.resolveTime(context,
                    preferences.getFontFamily(), false));
            baseDescription = day.contentDescription;
            applySelection(selected);
            setClickable(interactive);
            setFocusable(interactive);
            if (interactive) setOnClickListener(view -> host.onDaySelected(day));
        }

        void applySelection(boolean selected) {
            // Today and selected have to stay distinguishable when they are the same day, so the
            // rule takes today's colour rather than the two marks stacking or one winning.
            if (selected) {
                mark.setRule(today ? theme.today : theme.selectionStroke);
            } else if (today) {
                mark.setDot(theme.today);
            } else {
                mark.clear();
            }
            setContentDescription(selected
                    ? baseDescription + context.getString(R.string.calendar_selected_suffix)
                    : baseDescription);
        }

        void setTypeface(Typeface typeface) {
            number.setTypefaces(typeface, typeface);
        }

        void applySizing(float numberSize, int markHeight) {
            number.setDayTextSize(numberSize);
            setViewHeight(mark, markHeight);
        }
    }

    /** The band under a number: today's dot, or the selection rule, or nothing. */
    private static final class DayMarkView extends View {
        private enum Mode { NONE, DOT, RULE }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bar = new RectF();
        private Mode mode = Mode.NONE;

        DayMarkView(Context context) {
            super(context);
            paint.setStyle(Paint.Style.FILL);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void setDot(int color) { apply(Mode.DOT, color); }

        void setRule(int color) { apply(Mode.RULE, color); }

        void clear() { apply(Mode.NONE, 0); }

        private void apply(Mode mode, int color) {
            if (this.mode == mode && paint.getColor() == color) return;
            this.mode = mode;
            paint.setColor(color);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            if (mode == Mode.NONE || width <= 0f || height <= 0f) return;
            float density = getResources().getDisplayMetrics().density;
            if (mode == Mode.DOT) {
                float radius = Math.max(density, Math.min(width, height) * 0.22f);
                canvas.drawCircle(width / 2f, height / 2f, radius, paint);
                return;
            }
            float thickness = Math.max(density * 1.5f, height * 0.3f);
            float half = width * 0.28f;
            bar.set(width / 2f - half, (height - thickness) / 2f,
                    width / 2f + half, (height + thickness) / 2f);
            canvas.drawRoundRect(bar, thickness / 2f, thickness / 2f, paint);
        }
    }

    // endregion
}
