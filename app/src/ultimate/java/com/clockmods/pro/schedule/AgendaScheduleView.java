package com.clockmods.pro.schedule;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.clockmods.R;
import com.clockmods.pro.CalendarTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * The 周程 detail card's schedule pane: a heading row (「日程」 plus an add action) and the day's
 * items beneath it. The pane spans the free height the weather and 宜忌 leave behind — the one
 * region of the card that was empty — so a quiet day reads as a clean pane rather than a void.
 *
 * <p>The view owns no persistence and no dialogs. {@code AgendaCalendarLayout} feeds it the day's
 * items and answers the add / tap / remove callbacks, which is what keeps a bounded pane from
 * having to reach the {@link ScheduleStore} itself.</p>
 */
public final class AgendaScheduleView extends LinearLayout {
    public interface Listener {
        void onAddRequested();
        void onItemSelected(ScheduleItem item);
    }

    private final TextView heading;
    private final TextView addButton;
    private final TextView emptyView;
    private final LinearLayout rows;
    private final ScrollView scroll;

    private Listener listener;
    private CalendarTheme theme;
    private List<ScheduleItem> items = new ArrayList<>();

    private float titleSize = 13f;
    private float bodySize = 13f;

    public AgendaScheduleView(Context context) {
        this(context, null);
    }

    public AgendaScheduleView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setBaselineAligned(false);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setBaselineAligned(false);
        header.setGravity(Gravity.CENTER_VERTICAL);

        heading = new TextView(context);
        heading.setIncludeFontPadding(false);
        heading.setSingleLine(true);
        heading.setText(R.string.ultimate_schedule_title);
        header.addView(heading, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        addButton = new TextView(context);
        addButton.setIncludeFontPadding(false);
        addButton.setSingleLine(true);
        addButton.setGravity(Gravity.CENTER);
        addButton.setText(R.string.ultimate_schedule_add);
        // Background before padding: setBackground adopts the drawable's own padding and would
        // otherwise discard what we set here.
        addButton.setBackground(selectableBackground(context));
        addButton.setMinHeight(Math.round(density * 36f));
        addButton.setPadding(Math.round(density * 10f), Math.round(density * 4f),
                Math.round(density * 10f), Math.round(density * 4f));
        addButton.setOnClickListener(view -> {
            if (listener != null) listener.onAddRequested();
        });
        header.addView(addButton, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        LayoutParams headerParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        // 14dp is the card's spacing between blocks; the pane is one more block, not a special case.
        headerParams.topMargin = Math.round(density * 14f);
        addView(header, headerParams);

        emptyView = new TextView(context);
        emptyView.setIncludeFontPadding(false);
        emptyView.setText(R.string.ultimate_schedule_empty);
        emptyView.setGravity(Gravity.CENTER);
        LayoutParams emptyParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        addView(emptyView, emptyParams);

        rows = new LinearLayout(context);
        rows.setOrientation(VERTICAL);
        rows.setBaselineAligned(false);

        scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(rows, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = Math.round(density * 6f);
        addView(scroll, scrollParams);
    }

    public void setListener(@Nullable Listener listener) { this.listener = listener; }

    public void setItems(List<ScheduleItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
        bind();
    }

    public boolean isEmpty() { return items.isEmpty(); }

    public void applyTheme(CalendarTheme theme) {
        this.theme = theme;
        heading.setTextColor(theme.secondary);
        addButton.setTextColor(theme.accent);
        emptyView.setTextColor(theme.secondary);
        for (int index = 0; index < rows.getChildCount(); index++) {
            Row row = (Row) rows.getChildAt(index);
            row.applyTheme();
        }
    }

    public void setTypefaces(Typeface display, Typeface regular) {
        heading.setTypeface(regular);
        addButton.setTypeface(regular);
        emptyView.setTypeface(regular);
        for (int index = 0; index < rows.getChildCount(); index++) {
            Row row = (Row) rows.getChildAt(index);
            row.setTypefaces(regular);
        }
    }

    public void applySizing(float titleSize, float bodySize) {
        this.titleSize = titleSize;
        this.bodySize = bodySize;
        heading.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize * 1.08f);
        addButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, bodySize);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_PX, bodySize);
        for (int index = 0; index < rows.getChildCount(); index++) {
            Row row = (Row) rows.getChildAt(index);
            row.applySizing(bodySize);
        }
    }

    private void bind() {
        rows.removeAllViews();
        if (items.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            scroll.setVisibility(View.GONE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);
        for (ScheduleItem item : items) {
            Row row = new Row(getContext(), item);
            row.applyTheme();
            row.applySizing(bodySize);
            rows.addView(row);
        }
    }

    /**
     * The platform's own bounded ripple. The pane used to build a colourless {@code
     * GradientDrawable} for this, which drew nothing at all - every tap in here was silent.
     */
    private static Drawable selectableBackground(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true);
        return outValue.resourceId == 0 ? null
                : ContextCompat.getDrawable(context, outValue.resourceId);
    }

    /**
     * One row, at Material's one-line list-item metrics: a 48dp touch target with the time as a
     * fixed-width leading column, the way Google Calendar's own schedule view sets one out. The
     * column holds the all-day label when an item has no time rather than collapsing, so every
     * title in the list starts on the same edge - a list of rows that each began somewhere else
     * was most of what made this pane look uneven.
     *
     * <p>There is no per-row delete. The row used to carry a delete label that was permanently
     * {@code INVISIBLE} - unreachable, yet still reserving its own width on every single row.
     * Deleting belongs to the editor dialog, which is where a tap on the row leads.</p>
     */
    private final class Row extends LinearLayout {
        private final ScheduleItem item;
        private final TextView time;
        private final TextView title;

        Row(Context context, ScheduleItem item) {
            super(context);
            this.item = item;
            float density = getResources().getDisplayMetrics().density;
            setOrientation(HORIZONTAL);
            setBaselineAligned(false);
            setGravity(Gravity.CENTER_VERTICAL);
            setBackground(selectableBackground(context));
            // Material's one-line list item. The padding only opens the row further when the text
            // is large enough to exceed the minimum, so short and tall rows share one rhythm.
            setMinimumHeight(Math.round(density * 48f));
            setPadding(0, Math.round(density * 6f), 0, Math.round(density * 6f));

            time = new TextView(context);
            time.setIncludeFontPadding(false);
            time.setSingleLine(true);
            time.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            time.setText(item.hasTime() ? item.timeLabel()
                    : context.getString(R.string.ultimate_schedule_all_day));
            time.setMinWidth(Math.round(density * 46f));
            LayoutParams timeParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            timeParams.setMarginEnd(Math.round(density * 12f));
            addView(time, timeParams);

            title = new TextView(context);
            title.setIncludeFontPadding(false);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setText(item.title);
            addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            setOnClickListener(view -> {
                if (listener != null) listener.onItemSelected(item);
            });
        }

        void applyTheme() {
            if (theme == null) return;
            // An untimed row's label is not a time, so it recedes to the secondary colour instead
            // of competing with the real clock times above and below it.
            time.setTextColor(item.hasTime() ? theme.accent : withAlpha(theme.secondary, 0.75f));
            title.setTextColor(theme.text);
        }

        void setTypefaces(Typeface regular) {
            time.setTypeface(regular);
            title.setTypeface(regular);
        }

        void applySizing(float size) {
            time.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 0.92f);
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }

        private int withAlpha(int color, float alpha) {
            return (Math.round(255 * alpha) << 24) | (color & 0x00FFFFFF);
        }
    }
}
