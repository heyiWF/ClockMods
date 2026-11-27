package com.clockmods.pro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.animation.LayoutTransition;
import androidx.fragment.app.Fragment;
import com.clockmods.R;
import com.clockmods.pro.timer.CountdownTimers;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.LinkedHashMap;
import java.util.Map;

/** A stable row and digit animator for each countdown, including while another timer changes. */
final class CountdownPage extends LinearLayout {
    private final Fragment host;
    private final CountdownTimers store;
    private final LinearLayout list;
    private final ScrollView scroll;
    private final Map<String, Row> rows = new LinkedHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable reload = this::reload;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> {
        if (key == null || key.startsWith("countdown")) {
            handler.removeCallbacks(reload);
            handler.post(reload);
        }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            boolean running = false;
            for (Row row : rows.values()) {
                row.renderTime(now);
                running |= row.timer.running;
            }
            if (running) handler.postDelayed(this, 250);
        }
    };

    CountdownPage(Fragment host) {
        super(host.requireContext());
        this.host = host;
        store = new CountdownTimers(getContext());
        setOrientation(VERTICAL);
        setPadding(dp(16), dp(8), dp(16), 0);
        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        addView(header);
        HorizontalScrollView presets = new HorizontalScrollView(getContext());
        presets.setHorizontalScrollBarEnabled(false);
        header.addView(presets, new LayoutParams(0, -2, 1));
        MaterialButton custom = new MaterialButton(getContext());
        custom.setText(R.string.timer_custom);
        custom.setIconResource(R.drawable.ultimate_ic_add);
        custom.setOnClickListener(v -> edit(null));
        header.addView(custom);
        LinearLayout choices = new LinearLayout(getContext());
        presets.addView(choices);
        for (int minutes : new int[]{5, 10, 30, 60}) {
            Chip chip = new Chip(getContext());
            chip.setText(getResources().getString(R.string.timer_minutes, minutes));
            chip.setOnClickListener(v -> store.startNew(minutes * 60_000L, ""));
            LayoutParams params = new LayoutParams(-2, -2);
            params.setMarginEnd(dp(8));
            choices.addView(chip, params);
        }
        scroll = new ScrollView(getContext());
        list = new LinearLayout(getContext());
        list.setOrientation(VERTICAL);
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200);
        transition.setAnimateParentHierarchy(false);
        list.setLayoutTransition(transition);
        list.setPadding(dp(1), dp(4), dp(1), dp(12));
        scroll.addView(list);
        addView(scroll, new LayoutParams(-1, 0, 1));
        ProFontApplier.apply(header);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int inset = Math.max(dp(16), (w - dp(648)) / 2);
        setPadding(inset, dp(8), inset, 0);
    }

    private void edit(String id) {
        if (host.getChildFragmentManager().findFragmentByTag("countdown_editor") == null)
            CountdownDialog.open(host, id);
    }

    void resume() {
        store.preferences.registerOnSharedPreferenceChangeListener(listener);
        reload();
    }

    void pause() {
        store.preferences.unregisterOnSharedPreferenceChangeListener(listener);
        handler.removeCallbacksAndMessages(null);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void reload() {
        Map<String, Row> next = new LinkedHashMap<>();
        int index = 0;
        for (CountdownTimers.Timer timer : store.all()) {
            Row row = rows.get(timer.id);
            if (row == null) {
                row = new Row(timer);
                LayoutParams params = new LayoutParams(-1, -2);
                params.bottomMargin = dp(10);
                list.addView(row.card, index, params);
                if (index == 0 && !rows.isEmpty()) scroll.scrollTo(0, 0);
            }
            row.bind(timer);
            next.put(timer.id, row);
            index++;
        }
        for (Row row : rows.values()) if (!next.containsKey(row.timer.id)) {
            row.digits.dispose();
            list.removeView(row.card);
        }
        rows.clear();
        rows.putAll(next);
        handler.removeCallbacks(tick);
        tick.run();
    }

    private final class Row {
        final MaterialCardView card = new MaterialCardView(getContext());
        final TextView name = new TextView(getContext());
        final CircularProgressIndicator ring = new CircularProgressIndicator(getContext());
        final MaterialButton toggle = new MaterialButton(getContext());
        final MaterialButton minute = new MaterialButton(getContext(), null,
                com.google.android.material.R.attr.materialButtonTonalStyle);
        final RollingTimeText digits;
        CountdownTimers.Timer timer;
        boolean bound;

        Row(CountdownTimers.Timer timer) {
            this.timer = timer;
            Context context = getContext();
            LinearLayout body = new LinearLayout(context);
            body.setPadding(dp(14), dp(8), dp(14), dp(12));
            body.setOrientation(VERTICAL);
            card.addView(body);
            LinearLayout heading = new LinearLayout(context);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            body.addView(heading);
            name.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setGravity(Gravity.CENTER_VERTICAL);
            name.setMinimumHeight(dp(48));
            name.setOnClickListener(v -> edit(timer.id));
            android.util.TypedValue selectable = new android.util.TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true);
            name.setBackgroundResource(selectable.resourceId);
            heading.addView(name, new LayoutParams(0, -2, 1));
            heading.addView(iconButton(R.drawable.widget_config_close, R.string.countdown_delete,
                    () -> store.remove(timer.id)), new LayoutParams(dp(48), dp(48)));
            androidx.appcompat.widget.AppCompatTextView time = new androidx.appcompat.widget.AppCompatTextView(context);
            time.setSingleLine();
            time.setGravity(Gravity.CENTER);
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(time, 15, 52, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            body.addView(time, new LayoutParams(-1, dp(60)));
            digits = new RollingTimeText(time);

            LinearLayout actions = new LinearLayout(context);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            body.addView(actions);
            FrameLayout before = new FrameLayout(context);
            actions.addView(before, new LayoutParams(0, -2, 1));
            before.addView(iconButton(R.drawable.tool_ic_reset, R.string.timer_reset,
                    () -> store.reset(timer.id)), new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.END));
            FrameLayout dial = new FrameLayout(context);
            actions.addView(dial, new LayoutParams(dp(76), dp(76)));
            ring.setIndeterminate(false);
            ring.setIndicatorSize(dp(72));
            ring.setIndicatorInset(0);
            ring.setTrackThickness(dp(4));
            ring.setMax(1000);
            dial.addView(ring, new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER));
            toggle.setInsetTop(0);
            toggle.setInsetBottom(0);
            toggle.setPadding(0, 0, 0, 0);
            toggle.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            toggle.setIconPadding(0);
            ToolButtonMotion.install(toggle);
            toggle.setOnClickListener(v -> store.toggle(timer.id));
            dial.addView(toggle, new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER));
            FrameLayout after = new FrameLayout(context);
            actions.addView(after, new LayoutParams(0, -2, 1));
            minute.setText(R.string.countdown_minute);
            minute.setSingleLine();
            minute.setPadding(dp(10), 0, dp(10), 0);
            minute.setOnClickListener(v -> store.addMinute(timer.id));
            after.addView(minute, new FrameLayout.LayoutParams(-2, -2, Gravity.START));
            ProFontApplier.apply(body);
        }

        void bind(CountdownTimers.Timer current) {
            boolean changed = bound && timer.running != current.running;
            timer = current;
            name.setText(timer.name(getContext()));
            toggle.setContentDescription(getResources().getString(timer.running ? R.string.timer_pause : R.string.timer_start));
            ToolButtonMotion.running(toggle, timer.running);
            toggle.setIconResource(timer.running ? R.drawable.tool_ic_pause : R.drawable.tool_ic_play);
            minute.setEnabled(timer.running);
            minute.setImportantForAccessibility(timer.running ? IMPORTANT_FOR_ACCESSIBILITY_YES : IMPORTANT_FOR_ACCESSIBILITY_NO);
            if (changed) {
                minute.setVisibility(VISIBLE);
                minute.animate().alpha(timer.running ? 1f : 0f).translationX(timer.running ? 0 : -dp(8))
                        .setDuration(180).withEndAction(() -> minute.setVisibility(timer.running ? VISIBLE : INVISIBLE)).start();
                toggle.setScaleX(0.9f);
                toggle.setScaleY(0.9f);
                toggle.animate().scaleX(1).scaleY(1).setDuration(180).start();
            } else if (!bound) {
                minute.setVisibility(timer.running ? VISIBLE : INVISIBLE);
                minute.setAlpha(timer.running ? 1f : 0f);
            }
            bound = true;
        }

        void renderTime(long now) {
            long remaining = timer.remainingAt(now);
            if (timer.running && remaining == 0) store.complete(timer.id);
            digits.set(CountdownTimers.time(remaining), timer.running);
            ring.setProgress((int) Math.min(1000, 1000d * remaining / Math.max(1, timer.duration)));
        }

        private MaterialButton iconButton(int icon, int label, Runnable action) {
            MaterialButton button = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialIconButtonStyle);
            button.setIconResource(icon);
            button.setIconPadding(0);
            button.setPadding(0, 0, 0, 0);
            button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            button.setContentDescription(getResources().getString(label));
            button.setOnClickListener(v -> action.run());
            return button;
        }
    }
}
