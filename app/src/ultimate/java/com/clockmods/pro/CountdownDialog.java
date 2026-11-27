package com.clockmods.pro;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.clockmods.R;
import com.clockmods.pro.timer.CountdownKeypad;
import com.clockmods.pro.timer.CountdownTimers;
import com.clockmods.ultimate.navigation.PredictiveAlertDialogBuilder;
import com.google.android.material.color.MaterialColors;

public final class CountdownDialog extends DialogFragment {
    private String digits = "", label = "";
    private EditText rename;

    static void open(Fragment host, String id) {
        CountdownDialog dialog = new CountdownDialog();
        Bundle args = new Bundle();
        args.putString("id", id);
        dialog.setArguments(args);
        dialog.show(host.getChildFragmentManager(), "countdown_editor");
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        CountdownTimers store = new CountdownTimers(requireContext());
        String id = requireArguments().getString("id");
        digits = state == null ? "" : state.getString("digits", "");
        label = state == null ? (id == null ? "" : store.preferences.getString(id + "_label", "")) : state.getString("label", "");
        ScrollView content = new ScrollView(requireContext());
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, 0, pad, 0);
        if (id == null) {
            CountdownKeypad keypad = new CountdownKeypad(requireContext(), digits, label,
                    MaterialColors.getColor(content, com.google.android.material.R.attr.colorSurfaceContainerHigh),
                    MaterialColors.getColor(content, com.google.android.material.R.attr.colorOnSurface),
                    (value, name) -> {
                        digits = value;
                        label = name;
                        if (getDialog() instanceof AlertDialog)
                            ((AlertDialog) requireDialog()).getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(CountdownTimers.keypadMillis(digits) > 0);
                    });
            content.addView(keypad);
        } else {
            rename = new EditText(requireContext());
            rename.setSingleLine();
            rename.setHint(R.string.countdown_label);
            rename.setText(label);
            rename.setSelection(label.length());
            content.addView(rename);
        }
        ProFontApplier.apply(content);
        return new PredictiveAlertDialogBuilder(requireContext())
                .setTitle(id == null ? R.string.countdown_new : R.string.countdown_label)
                .setView(content).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(id == null ? R.string.timer_start : R.string.apply, (dialog, which) -> {
                    if (id == null) store.startNew(CountdownTimers.keypadMillis(digits), label);
                    else store.rename(id, rename.getText().toString());
                }).create();
    }

    @Override public void onStart() {
        super.onStart();
        if (rename == null) ((AlertDialog) requireDialog()).getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(CountdownTimers.keypadMillis(digits) > 0);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("digits", digits);
        state.putString("label", rename == null ? label : rename.getText().toString());
    }
}
