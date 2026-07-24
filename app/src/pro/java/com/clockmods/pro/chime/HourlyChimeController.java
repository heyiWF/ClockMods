package com.clockmods.pro.chime;

import android.os.Handler;
import android.os.Looper;

import com.clockmods.background.BackgroundRepository;

import java.util.Calendar;

public final class HourlyChimeController {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final RadialChimeView view;
    private final BackgroundRepository repository;
    private long lastChimeAtMillis = Long.MIN_VALUE;
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
        long chimeAtMillis = upcomingChimeAtMillis(now);
        if (!repository.isHourlyChimeEnabled() || chimeAtMillis == Long.MIN_VALUE) return;
        Calendar chimeAt = Calendar.getInstance(now.getTimeZone());
        chimeAt.setTimeInMillis(chimeAtMillis);
        if (isQuiet(chimeAt) || chimeAtMillis == lastChimeAtMillis) return;
        lastChimeAtMillis = chimeAtMillis;
        view.startChime(repository, chimeAtMillis);
    }

    static long upcomingChimeAtMillis(Calendar now) {
        if (now.get(Calendar.MINUTE) != 59 || now.get(Calendar.SECOND) < 58) {
            return Long.MIN_VALUE;
        }
        Calendar chimeAt = (Calendar) now.clone();
        chimeAt.add(Calendar.HOUR_OF_DAY, 1);
        chimeAt.set(Calendar.MINUTE, 0);
        chimeAt.set(Calendar.SECOND, 0);
        chimeAt.set(Calendar.MILLISECOND, 0);
        return chimeAt.getTimeInMillis();
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