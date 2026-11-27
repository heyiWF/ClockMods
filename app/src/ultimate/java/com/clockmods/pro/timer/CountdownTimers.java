package com.clockmods.pro.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.clockmods.R;
import com.clockmods.pro.alarm.AlarmDelivery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** The original countdown keeps its preference keys and AlarmManager identity across upgrades. */
public final class CountdownTimers {
    public static final String FIRST = "countdown";
    private static final String IDS = "countdown_ids";
    private static final long DEFAULT = 10 * 60_000L;
    private final Context context;
    public final SharedPreferences preferences;

    public CountdownTimers(Context context) {
        this.context = context.getApplicationContext();
        preferences = context.getSharedPreferences("pro_timers", Context.MODE_PRIVATE);
    }

    public static List<String> ids(SharedPreferences preferences) {
        boolean legacy = preferences.contains(FIRST + "_duration") || preferences.contains(FIRST + "_running")
                || preferences.contains(FIRST + "_remaining") || preferences.contains(FIRST + "_deadline")
                || preferences.contains(FIRST + "_label");
        String saved = preferences.getString(IDS, legacy ? FIRST : "");
        return saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split(",")));
    }

    public static final class Timer {
        public final String id, label;
        public final long duration, remaining, deadline;
        public final boolean running;
        private final long displayNumber;

        Timer(SharedPreferences prefs, String id) {
            this.id = id;
            String prefix = id + "_";
            displayNumber = prefs.getLong(prefix + "number", 0L);
            label = prefs.getString(prefix + "label", "");
            duration = prefs.getLong(prefix + "duration", DEFAULT);
            remaining = prefs.getLong(prefix + "remaining", duration);
            deadline = prefs.getLong(prefix + "deadline", 0);
            running = prefs.getBoolean(prefix + "running", false);
        }

        public String name(Context context) {
            if (!label.isEmpty()) return label;
            long number = displayNumber;
            if (number == 0) number = FIRST.equals(id) ? 1 : Long.parseLong(id.substring(FIRST.length() + 1));
            return context.getString(R.string.countdown_untitled, number);
        }

        public long remainingAt(long now) {
            return running ? Math.max(0, deadline - now) : remaining;
        }
    }

    public List<Timer> all() {
        List<Timer> result = new ArrayList<>();
        for (String id : ids(preferences)) result.add(new Timer(preferences, id));
        return result;
    }

    public String startNew(long duration, String label) {
        if (duration <= 0) throw new IllegalArgumentException("Duration must be positive");
        List<String> ids = ids(preferences);
        boolean initial = !preferences.contains(IDS) && ids.isEmpty();
        long number = preferences.getLong("countdown_next_number", 2);
        String id = initial ? FIRST : FIRST + "-" + number;
        long displayNumber = ids.isEmpty() ? 1 : preferences.getLong("countdown_next_label", number);
        ids.add(0, id);
        long deadline = System.currentTimeMillis() + duration;
        preferences.edit().putLong("countdown_next_number", initial ? number : number + 1)
                .putLong("countdown_next_label", displayNumber + 1).putLong(id + "_number", displayNumber)
                .putString(IDS, TextUtils.join(",", ids)).putString(id + "_label", label.trim())
                .putLong(id + "_duration", duration).putLong(id + "_remaining", duration)
                .putLong(id + "_deadline", deadline).putBoolean(id + "_running", true).apply();
        TimerScheduler.schedule(context, id, deadline);
        return id;
    }

    public void rename(String id, String label) {
        if (ids(preferences).contains(id)) preferences.edit().putString(id + "_label", label.trim()).apply();
    }

    public void toggle(String id) {
        if (!ids(preferences).contains(id)) return;
        Timer timer = new Timer(preferences, id);
        long now = System.currentTimeMillis();
        if (timer.running) {
            long remaining = timer.remainingAt(now);
            if (remaining == 0) { complete(id); return; }
            preferences.edit().putBoolean(id + "_running", false).putLong(id + "_remaining", remaining)
                    .putLong(id + "_deadline", 0).apply();
            TimerScheduler.cancel(context, id);
        } else {
            long remaining = timer.remaining > 0 ? timer.remaining : timer.duration;
            long deadline = now + remaining;
            preferences.edit().putBoolean(id + "_running", true).putLong(id + "_deadline", deadline)
                    .putLong(id + "_remaining", remaining).apply();
            TimerScheduler.schedule(context, id, deadline);
        }
    }

    public void addMinute(String id) {
        Timer timer = new Timer(preferences, id);
        if (!timer.running) return;
        if (timer.remainingAt(System.currentTimeMillis()) == 0) { complete(id); return; }
        long deadline = timer.deadline + 60_000L;
        preferences.edit().putLong(id + "_duration", timer.duration + 60_000L)
                .putLong(id + "_deadline", deadline).apply();
        TimerScheduler.schedule(context, id, deadline);
    }

    public void reset(String id) {
        if (!ids(preferences).contains(id)) return;
        Timer timer = new Timer(preferences, id);
        preferences.edit().putLong(id + "_remaining", timer.duration).putLong(id + "_deadline", 0)
                .putBoolean(id + "_running", false).apply();
        TimerScheduler.cancel(context, id);
    }

    public void remove(String id) {
        List<String> ids = ids(preferences);
        if (!ids.remove(id)) return;
        preferences.edit().putString(IDS, TextUtils.join(",", ids)).remove(id + "_remaining")
                .remove(id + "_duration").remove(id + "_number").remove(id + "_label").remove(id + "_deadline")
                .remove(id + "_running").apply();
        TimerScheduler.cancel(context, id);
    }

    public void complete(String id) {
        Timer timer = new Timer(preferences, id);
        if (!timer.running || timer.deadline > System.currentTimeMillis()) return;
        preferences.edit().putBoolean(id + "_running", false).putLong(id + "_remaining", 0)
                .putLong(id + "_deadline", 0).apply();
        TimerScheduler.cancel(context, id);
        AlarmDelivery.timer(context, false, timer.duration, 0, timer.deadline, timer.label);
    }

    public static long keypadMillis(String digits) {
        int number = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        return (number / 10_000 * 3600L + number / 100 % 100 * 60L + number % 100) * 1000;
    }

    public static String time(long remaining) {
        long seconds = (remaining + 999) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
