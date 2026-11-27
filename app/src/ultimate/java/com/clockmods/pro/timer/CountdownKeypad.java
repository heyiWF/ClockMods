package com.clockmods.pro.timer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.clockmods.R;
import com.clockmods.ui.ClockTimeText;

import java.util.function.BiConsumer;

/** Right-aligned HH:MM:SS entry. The six entered digits stay visible until Start normalizes them. */
public final class CountdownKeypad extends LinearLayout {
    private final TextView display;
    private final EditText label;
    private final BiConsumer<String, String> changed;
    private String digits;

    public CountdownKeypad(Context context, String digits, String name, int keyColor, int ink,
                           BiConsumer<String, String> changed) {
        super(context);
        this.digits = digits;
        this.changed = changed;
        setOrientation(VERTICAL);
        setFocusableInTouchMode(true);
        label = new EditText(context);
        label.setSingleLine();
        label.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        label.setHint(R.string.countdown_label);
        label.setText(name);
        label.setTextColor(ink);
        label.setHintTextColor((ink & 0x00ffffff) | 0xb3000000);
        addView(label);
        display = new TextView(context);
        display.setTextColor(ink);
        display.setGravity(Gravity.CENTER);
        display.setTextSize(36);
        display.setPadding(0, dp(8), 0, dp(8));
        display.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        addView(display);
        showDigits();
        for (String[] keys : new String[][] {{"1","2","3"},{"4","5","6"},{"7","8","9"},{"00","0","⌫"}}) {
            LinearLayout row = new LinearLayout(context);
            addView(row);
            for (String key : keys) {
                Button button = new Button(context);
                button.setText(key);
                button.setTextSize(22);
                button.setTextColor(ink);
                button.setPadding(0, 0, 0, 0);
                GradientDrawable shape = new GradientDrawable();
                shape.setCornerRadius(dp(16));
                shape.setColor(keyColor);
                button.setBackground(new RippleDrawable(ColorStateList.valueOf((ink & 0xffffff) | 0x25000000), shape, null));
                LayoutParams cell = new LayoutParams(0, dp(52), 1);
                cell.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(button, cell);
                boolean erase = "⌫".equals(key);
                if (erase) {
                    button.setContentDescription(context.getString(R.string.countdown_backspace));
                    button.setOnLongClickListener(v -> { setDigits(""); return true; });
                }
                button.setOnClickListener(v -> {
                    String current = this.digits;
                    if (erase) setDigits(current.substring(0, Math.max(0, current.length() - 1)));
                    else if (current.length() < 6 && !(current.isEmpty() && key.startsWith("0"))) {
                        String next = current + key;
                        setDigits(next.substring(0, Math.min(6, next.length())));
                    }
                });
            }
        }
        label.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { changed.accept(CountdownKeypad.this.digits, s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void setDigits(String next) {
        digits = next;
        showDigits();
        changed.accept(next, label.getText().toString());
    }

    private void showDigits() {
        String six = "000000".substring(digits.length()) + digits;
        display.setText(ClockTimeText.align(six.substring(0, 2) + ":" + six.substring(2, 4) + ":" + six.substring(4)));
    }
}
