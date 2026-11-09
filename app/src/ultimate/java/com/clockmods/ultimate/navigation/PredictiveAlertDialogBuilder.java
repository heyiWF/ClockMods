package com.clockmods.ultimate.navigation;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Material dialog with progress/cancel handling in the dialog's own window. */
public final class PredictiveAlertDialogBuilder extends MaterialAlertDialogBuilder {
    public PredictiveAlertDialogBuilder(Context context) { super(context); }

    @Override public AlertDialog create() {
        AlertDialog dialog = super.create();
        DialogBackAnimation.install(dialog.getWindow().getDecorView(), dialog::cancel);
        return dialog;
    }
}
