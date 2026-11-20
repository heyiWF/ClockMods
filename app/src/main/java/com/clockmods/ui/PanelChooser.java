package com.clockmods.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.clockmods.R;

/**
 * Dark single-choice sheet used for every "pick one of these" step of the settings panel.
 *
 * <p>Two framework surfaces used to break the dark theme here: Spinner's popup (a light window
 * with white row dividers that {@code setPopupBackgroundDrawable} could only fix on API 16+) and
 * the {@code AlertDialog} behind the region list. Both are replaced by this dialog, which is drawn
 * from our own palette and doubles as the spinner's popup, so a font change, a date format and a
 * time zone all look the same.</p>
 */
final class PanelChooser {
    interface OnPick {
        void onPick(int index);
    }

    private static final int ROW_HEIGHT_DP = 48;
    private static final int MAX_LIST_DP = 300;
    private static final int HORIZONTAL_INSET_DP = 24;
    /** Inset shared by the plate, the title and every row, so all three line up. */
    private static final int ROW_INSET_DP = 8;

    private PanelChooser() {
    }

    static void show(Context context, CharSequence title, CharSequence[] items, int selected,
            OnPick onPick) {
        if (items == null || items.length == 0) {
            return;
        }
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final int dp = Math.round(context.getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable plate = new GradientDrawable();
        plate.setColor(PanelTheme.SURFACE);
        plate.setCornerRadius(14 * dp);
        plate.setStroke(Math.max(1, dp), PanelTheme.FIELD_STROKE);
        root.setBackgroundDrawable(plate);
        // The plate pads by ROW_INSET and the title/rows repeat it, so the highlight of the
        // selected row may start at the plate inset while its label still lines up with the title.
        root.setPadding(ROW_INSET_DP * dp, 14 * dp, ROW_INSET_DP * dp, 12 * dp);

        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextColor(PanelTheme.TITLE);
        heading.setTextSize(17f);
        heading.setPadding(ROW_INSET_DP * dp, 0, ROW_INSET_DP * dp, 0);
        root.addView(heading, wrap());

        ScrollView scroller = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int index = 0; index < items.length; index++) {
            final int position = index;
            list.addView(createRow(context, items[index], index == selected, dp, view -> {
                dialog.dismiss();
                onPick.onPick(position);
            }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ROW_HEIGHT_DP * dp));
        }
        scroller.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int listHeight = Math.min(items.length * ROW_HEIGHT_DP, MAX_LIST_DP) * dp;
        LinearLayout.LayoutParams scrollerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, listHeight);
        scrollerParams.topMargin = 8 * dp;
        root.addView(scroller, scrollerParams);

        Button cancel = new Button(context);
        cancel.setText(R.string.cancel);
        WidgetStyles.styleButton(cancel, false);
        cancel.setOnClickListener(view -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ROW_HEIGHT_DP * dp);
        cancelParams.topMargin = 10 * dp;
        root.addView(cancel, cancelParams);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = metrics.widthPixels - HORIZONTAL_INSET_DP * dp;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        dialog.show();
    }

    private static View createRow(Context context, CharSequence label, boolean selected, int dp,
            View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ROW_INSET_DP * dp, 0, ROW_INSET_DP * dp, 0);
        row.setBackgroundDrawable(rowBackground(context, selected, dp));
        row.setOnClickListener(onClick);

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextSize(15f);
        text.setTextColor(selected ? PanelTheme.ACCENT : PanelTheme.PRIMARY_TEXT);
        text.setSingleLine(true);
        row.addView(text, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (selected) {
            View dot = new View(context);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(PanelTheme.ACCENT);
            dot.setBackgroundDrawable(circle);
            row.addView(dot, new LinearLayout.LayoutParams(8 * dp, 8 * dp));
        }
        return row;
    }

    /** Rows are flat until pressed; the current value is tinted instead of carrying a tick. */
    private static Drawable rowBackground(Context context, boolean selected, int dp) {
        GradientDrawable rest = new GradientDrawable();
        rest.setColor(selected ? PanelTheme.CARD : Color.TRANSPARENT);
        rest.setCornerRadius(8 * dp);
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(PanelTheme.FIELD_PRESSED);
        pressed.setCornerRadius(8 * dp);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, pressed);
        states.addState(new int[0], rest);
        return states;
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
