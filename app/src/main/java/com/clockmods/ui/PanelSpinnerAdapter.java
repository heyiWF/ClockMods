package com.clockmods.ui;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Dark rows for the settings sheet's spinners.
 *
 * <p>The framework item layouts ({@code simple_spinner_item} / {@code simple_spinner_dropdown_item})
 * are black text on the light dialog background, which is unreadable once the spinner sits on the
 * dark card, and the dropdown copy also paints a radio dot. Building the rows here keeps the text
 * colour, the padding and the selected-item highlight under our control.</p>
 */
final class PanelSpinnerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final int rowHeight;
    private final int dropdownHorizontal;
    private final int dropdownVertical;

    PanelSpinnerAdapter(Context context, String[] items) {
        super(context, 0, items);
        this.context = context;
        Resources resources = context.getResources();
        float density = resources.getDisplayMetrics().density;
        this.rowHeight = Math.round(WidgetStyles.FIELD_HEIGHT_DP * density);
        this.dropdownHorizontal = Math.round(14 * density);
        this.dropdownVertical = Math.round(11 * density);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView row = row(convertView);
        row.setText(getItem(position));
        row.setTextColor(PanelTheme.PRIMARY_TEXT);
        // The spinner carries the horizontal insets (and leaves room for the chevron), so the
        // collapsed row must not add padding of its own.
        row.setPadding(0, 0, 0, 0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(rowHeight);
        return row;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView row = row(convertView);
        row.setText(getItem(position));
        int selected = parent instanceof AdapterView
                ? ((AdapterView<?>) parent).getSelectedItemPosition() : -1;
        row.setTextColor(position == selected ? PanelTheme.ACCENT : PanelTheme.PRIMARY_TEXT);
        row.setPadding(dropdownHorizontal, dropdownVertical, dropdownHorizontal, dropdownVertical);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(rowHeight);
        row.setBackgroundColor(PanelTheme.CARD);
        return row;
    }

    private TextView row(View convertView) {
        TextView row = convertView instanceof TextView ? (TextView) convertView
                : new TextView(context);
        row.setTextSize(14f);
        row.setSingleLine(true);
        return row;
    }
}
