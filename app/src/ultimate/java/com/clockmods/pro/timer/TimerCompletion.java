package com.clockmods.pro.timer;

import android.content.Context;
import android.content.SharedPreferences;
import com.clockmods.pro.alarm.AlarmDelivery;
import com.clockmods.R;

public final class TimerCompletion {
    public static final String PENDING_PHASE = "pomodoro_completed_deadline";

    private TimerCompletion() {}

    public static int phaseLabel(int phase) {
        return phase == 1 ? R.string.pomodoro_short_break
                : phase == 2 ? R.string.pomodoro_long_break : R.string.pomodoro_focus;
    }

    static synchronized void complete(Context context, String mode) {
        if (!"pomodoro".equals(mode)) {
            if (mode == null || mode.equals(CountdownTimers.FIRST) || mode.startsWith("countdown-"))
                new CountdownTimers(context).complete(mode == null ? CountdownTimers.FIRST : mode);
            return;
        }
        String prefix = "pomodoro_";
        SharedPreferences p = context.getSharedPreferences("pro_timers", 0);
        long deadline = p.getLong(prefix + "deadline", 0);
        if (!p.getBoolean(prefix + "running", false) || deadline > System.currentTimeMillis()) return;
        java.util.List<PomodoroSequence.Step> steps = PomodoroSequence.load(p);
        int index = PomodoroSequence.index(p, steps);
        int phase = steps.get(index).label;
        long completedDuration = p.getLong(prefix + "duration", steps.get(index).duration());
        int next = (index + 1) % steps.size();
        long duration = steps.get(next).duration();
        SharedPreferences.Editor editor = p.edit().putBoolean(prefix + "running", false).putLong(prefix + "deadline", 0)
                .putInt(prefix + "phase", next).putLong(prefix + "duration", duration)
                .putLong(prefix + "remaining", duration).putLong(PENDING_PHASE, deadline);
        editor.apply();
        TimerScheduler.cancel(context, mode);
        AlarmDelivery.timer(context, true, completedDuration, phase, deadline);
    }

    public static synchronized void startNext(Context context, long completedDeadline) {
        SharedPreferences p = context.getSharedPreferences("pro_timers", 0);
        if (completedDeadline == 0 || p.getLong(PENDING_PHASE, 0) != completedDeadline
                || p.getBoolean("pomodoro_running", false)) return;
        long deadline = System.currentTimeMillis() + p.getLong("pomodoro_duration", 1500000L);
        p.edit().remove(PENDING_PHASE).putBoolean("pomodoro_running", true)
                .putLong("pomodoro_deadline", deadline).apply();
        TimerScheduler.schedule(context, "pomodoro", deadline);
    }
}
