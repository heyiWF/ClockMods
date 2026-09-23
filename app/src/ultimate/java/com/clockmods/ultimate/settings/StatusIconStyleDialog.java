package com.clockmods.ultimate.settings;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.clockmods.R;
import com.clockmods.ui.StatusIconStyle;
import com.clockmods.ui.StatusSymbolRenderer;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** One dialog keeps the legacy settings page compact while previewing every symbol axis. */
final class StatusIconStyleDialog {
    private StatusIconStyleDialog() {}

    static void show(AppCompatActivity activity, Runnable onSaved) {
        StatusIconStyle[] draft = {StatusIconStyle.read(activity)};
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 24), dp(activity, 8), dp(activity, 24), dp(activity, 12));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.addView(content);

        View preview = new View(activity) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(MaterialColors.getColor(activity,
                        androidx.appcompat.R.attr.colorPrimary, 0xFF6750A4));
                float size = dp(activity, 32);
                float gap = dp(activity, 20);
                float left = (getWidth() - size * 2 - gap) / 2f;
                float top = (getHeight() - size) / 2f;
                StatusSymbolRenderer.draw(canvas, activity, StatusSymbolRenderer.WIFI_FULL,
                        draft[0], left, top, size, paint);
                StatusSymbolRenderer.draw(canvas, activity, StatusSymbolRenderer.BATTERY_BOLT,
                        draft[0], left + size + gap, top, size, paint);
            }
        };
        preview.setContentDescription(activity.getString(R.string.ultimate_status_icon_preview));
        content.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 64)));

        section(activity, content, R.string.ultimate_status_icon_shape);
        choices(activity, content,
                new int[] {R.string.ultimate_status_icon_outlined,
                        R.string.ultimate_status_icon_rounded, R.string.ultimate_status_icon_sharp},
                new String[] {StatusIconStyle.OUTLINED, StatusIconStyle.ROUNDED,
                        StatusIconStyle.SHARP}, draft[0].family,
                value -> { draft[0] = draft[0].withFamily(value); preview.invalidate(); });

        section(activity, content, R.string.ultimate_status_icon_fill);
        choices(activity, content,
                new int[] {R.string.ultimate_status_icon_fill_auto,
                        R.string.ultimate_status_icon_outlined, R.string.ultimate_status_icon_solid},
                new String[] {StatusIconStyle.FILL_AUTO, StatusIconStyle.FILL_OUTLINE,
                        StatusIconStyle.FILL_SOLID}, draft[0].fill,
                value -> { draft[0] = draft[0].withFill(value); preview.invalidate(); });

        axis(activity, content, R.string.ultimate_status_icon_weight,
                draft[0].weight, 100, 700, 100,
                value -> { draft[0] = draft[0].withWeight(value); preview.invalidate(); });
        axis(activity, content, R.string.ultimate_status_icon_grade,
                draft[0].grade, -50, 200, 25,
                value -> { draft[0] = draft[0].withGrade(value); preview.invalidate(); });
        axis(activity, content, R.string.ultimate_status_icon_optical_size,
                draft[0].opticalSize, 20, 48, 4,
                value -> { draft[0] = draft[0].withOpticalSize(value); preview.invalidate(); });
        TextView opticalHint = new TextView(activity);
        opticalHint.setText(R.string.ultimate_status_icon_optical_size_summary);
        content.addView(opticalHint);

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.ultimate_status_icon_style)
                .setView(scroll)
                .setNegativeButton(R.string.ultimate_cancel, null)
                .setNeutralButton(R.string.ultimate_status_icon_reset, (dialog, which) -> {
                    StatusIconStyle.defaults().save(activity);
                    onSaved.run();
                })
                .setPositiveButton(R.string.ultimate_apply, (dialog, which) -> {
                    draft[0].save(activity);
                    onSaved.run();
                })
                .show();
    }

    private static void section(AppCompatActivity activity, LinearLayout content, int title) {
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextSize(14f);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(activity, 12);
        content.addView(label, params);
    }

    private static void choices(AppCompatActivity activity, LinearLayout content,
            int[] labels, String[] values, String selected, Consumer<String> onChange) {
        RadioGroup group = new RadioGroup(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        for (int index = 0; index < values.length; index++) {
            String value = values[index];
            RadioButton button = new RadioButton(activity);
            button.setId(View.generateViewId());
            button.setText(labels[index]);
            button.setChecked(value.equals(selected));
            button.setOnClickListener(view -> onChange.accept(value));
            group.addView(button);
        }
        content.addView(group);
    }

    private static void axis(AppCompatActivity activity, LinearLayout content, int title,
            int current, int minimum, int maximum, int step, IntConsumer onChange) {
        section(activity, content, title);
        TextView valueLabel = new TextView(activity);
        valueLabel.setText(Integer.toString(current));
        content.addView(valueLabel);
        Slider slider = new Slider(activity);
        slider.setValueFrom(minimum);
        slider.setValueTo(maximum);
        slider.setValue(Math.max(minimum, Math.min(maximum, current)));
        slider.setContentDescription(activity.getString(title));
        slider.addOnChangeListener((control, value, fromUser) -> {
            int snapped = minimum + Math.round((value - minimum) / step) * step;
            valueLabel.setText(Integer.toString(snapped));
            onChange.accept(snapped);
        });
        content.addView(slider);
    }

    private static int dp(AppCompatActivity activity, float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
