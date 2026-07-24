package com.clockmods.pro.chime;

import android.os.Handler;
import android.os.Looper;

import com.clockmods.background.BackgroundRepository;

import java.util.Calendar;

public final class HourlyChimeController {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final RadialChimeView view;
    private final BackgroundRepository repository;
    private long lastEpochHour = Long.MIN_VALUE;
    private final Runnable check = new Runnable() {
        @Override public void run() { checkNow(); handler.postDelayed(this, 250L); }
    };

    public HourlyChimeController(RadialChimeView view, BackgroundRepository repository) {
        this.view = view;
        this.repository = repository;
    }

    public void start() { handler.removeCallbacks(check); handler.post(check); }
    public void stop() { handler.removeCallbacks(check); view.stopChime(); }

    private void checkNow() {
        Calendar now = Calendar.getInstance();
        if (!repository.isHourlyChimeEnabled() || now.get(Calendar.MINUTE) != 0
                || now.get(Calendar.SECOND) >= 5 || isQuiet(now)) return;
        long epochHour = now.getTimeInMillis() / 3_600_000L;
        if (epochHour == lastEpochHour) return;
        lastEpochHour = epochHour;
        view.startChime(repository);
    }

    private boolean isQuiet(Calendar now) {
        if (!repository.isHourlyChimeQuietEnabled()) return false;
        int current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int start = repository.getHourlyChimeQuietStart();
        int end = repository.getHourlyChimeQuietEnd();
        if (start == end) return true;
        return start < end ? current >= start && current < end
                : current >= start || current < end;
    }
}