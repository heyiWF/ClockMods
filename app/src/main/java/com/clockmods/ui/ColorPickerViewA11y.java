package com.clockmods.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import com.clockmods.platform.ApiCompat;

/**
 * API 14+ accessibility surface for {@link ColorPickerView}.
 *
 * <p>{@code AccessibilityNodeInfo} and {@code View.onInitializeAccessibilityNodeInfo} do not exist
 * on Android 1.0; referencing them in {@link ColorPickerView} itself would make the verifier reject
 * the whole class. This subclass is therefore loaded reflectively from
 * {@link ColorPickerView#create(Context)} only when {@code SDK_INT >= 14}.</p>
 */
public class ColorPickerViewA11y extends ColorPickerView {
    public ColorPickerViewA11y(Context context) {
        super(context);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.SeekBar.class.getName());
        if (ApiCompat.SDK_INT >= 19) {
            info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                    AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0f, 360f, getHue()));
        }
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        if (ApiCompat.SDK_INT >= 24) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (ApiCompat.SDK_INT >= 24
                && action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()
                && arguments != null) {
            setHue(arguments.getFloat(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, getHue()));
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            setHue(getHue() - 5f);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            setHue(getHue() + 5f);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }
}
