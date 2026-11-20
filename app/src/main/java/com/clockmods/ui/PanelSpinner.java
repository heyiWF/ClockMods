package com.clockmods.ui;

import android.content.Context;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * Spinner that wears the panel's dark plate and opens {@link PanelChooser} instead of the
 * framework popup.
 *
 * <p>The framework popup is a light window with white dividers between rows; on Android 1.0 it
 * cannot be re-coloured ({@code setPopupBackgroundDrawable} is API 16). Overriding
 * {@code performClick} - the entry point both a finger and an accessibility service use - keeps
 * the selection flow unchanged: the chooser calls {@code setSelection}, which fires the same
 * {@code OnItemSelectedListener} the popup did.</p>
 */
final class PanelSpinner extends Spinner {
    private final CharSequence chooserTitle;

    PanelSpinner(Context context, CharSequence chooserTitle) {
        super(context);
        this.chooserTitle = chooserTitle;
        WidgetStyles.styleSpinner(this);
    }

    @Override
    public boolean performClick() {
        final CharSequence[] labels = labels();
        if (labels == null) {
            return super.performClick();
        }
        try {
            PanelChooser.show(getContext(), chooserTitle, labels, getSelectedItemPosition(),
                    new PanelChooser.OnPick() {
                        @Override
                        public void onPick(int index) {
                            setSelection(index);
                        }
                    });
            return true;
        } catch (RuntimeException fallback) {
            // Never leave the control dead if the custom sheet cannot be shown.
            return super.performClick();
        }
    }

    private CharSequence[] labels() {
        SpinnerAdapter adapter = getAdapter();
        int count = adapter == null ? 0 : adapter.getCount();
        if (count == 0) {
            return null;
        }
        CharSequence[] labels = new CharSequence[count];
        for (int index = 0; index < count; index++) {
            Object item = adapter.getItem(index);
            labels[index] = item == null ? "" : item.toString();
        }
        return labels;
    }
}
