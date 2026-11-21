package com.clockmods.pro.timer;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/** The saved phase is an index into the sequence. Labels may repeat in any order. */
public final class PomodoroSequence {
    public static final String KEY = "pomodoro_sequence";
    public static final int MIN_SIZE = 2, MAX_SIZE = 15, MIN_MINUTES = 3, MAX_MINUTES = 60;
    public static final String DEFAULT = "0:25,1:5,0:25,2:15";

    private PomodoroSequence() {}

    public static final class Step {
        public final int label, minutes;
        public Step(int label, int minutes) {
            if (label < 0 || label > 2 || minutes < MIN_MINUTES || minutes > MAX_MINUTES)
                throw new IllegalArgumentException("Invalid Pomodoro step");
            this.label = label;
            this.minutes = minutes;
        }
        public static Step defaultFor(int label) {
            switch (label) {
                case 0: return new Step(label, 25);
                case 1: return new Step(label, 5);
                case 2: return new Step(label, 15);
                default: throw new IllegalArgumentException("Invalid phase label");
            }
        }
        public long duration() { return minutes * 60000L; }
    }

    public static List<Step> decode(String encoded) {
        String[] fields = encoded.split(",", -1);
        if (fields.length < MIN_SIZE || fields.length > MAX_SIZE) throw new IllegalArgumentException("Invalid sequence length");
        ArrayList<Step> steps = new ArrayList<>(fields.length);
        for (String field : fields) {
            String[] pair = field.split(":", -1);
            if (pair.length != 2) throw new IllegalArgumentException("Invalid step");
            steps.add(new Step(Integer.parseInt(pair[0]), Integer.parseInt(pair[1])));
        }
        return steps;
    }

    public static String encode(List<Step> steps) {
        if (steps.size() < MIN_SIZE || steps.size() > MAX_SIZE) throw new IllegalArgumentException("Invalid sequence length");
        StringBuilder out = new StringBuilder();
        for (Step step : steps) {
            if (out.length() > 0) out.append(',');
            out.append(step.label).append(':').append(step.minutes);
        }
        return out.toString();
    }

    public static synchronized List<Step> load(SharedPreferences prefs) {
        String saved = prefs.getString(KEY, null);
        if (saved != null) {
            try { return decode(saved); } catch (IllegalArgumentException ignored) { }
        }
        int legacy = prefs.getInt("pomodoro_phase", 0);
        int index = legacy == 2 ? 3 : legacy == 1 ? 1 : 0;
        prefs.edit().putString(KEY, DEFAULT).putInt("pomodoro_phase", index).apply();
        return decode(DEFAULT);
    }

    public static int index(SharedPreferences prefs, List<Step> steps) {
        int index = prefs.getInt("pomodoro_phase", 0);
        return index >= 0 && index < steps.size() ? index : 0;
    }

    public static void apply(Context context, List<Step> steps) {
        String encoded = encode(steps);
        long duration = steps.get(0).duration();
        context.getSharedPreferences("pro_timers", 0).edit().putString(KEY, encoded)
                .putInt("pomodoro_phase", 0).putLong("pomodoro_duration", duration)
                .putLong("pomodoro_remaining", duration).putLong("pomodoro_deadline", 0L)
                .putBoolean("pomodoro_running", false).remove(TimerCompletion.PENDING_PHASE).apply();
        TimerScheduler.cancel(context, "pomodoro");
    }
}
