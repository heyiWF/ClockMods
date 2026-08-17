package com.clockmods.pro.chime;

import android.os.Handler;
import android.os.Looper;

import com.clockmods.background.BackgroundRepository;

import java.util.Calendar;
import java.util.TimeZone;

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
        Calendar now = Calendar.getInstance(resolveTimeZone(repository.getTimeZoneId()));
        now.setTimeInMillis(System.currentTimeMillis());
        long chimeAtMillis = upcomingChimeAtMillis(now, repository.isHourlyChimeEnabled(),
                repository.isHalfHourChimeEnabled());
        if (chimeAtMillis == Long.MIN_VALUE) return;
        Calendar chimeAt = Calendar.getInstance(now.getTimeZone());
        chimeAt.setTimeInMillis(chimeAtMillis);
        if (isQuiet(chimeAt) || chimeAtMillis == lastChimeAtMillis) return;
        lastChimeAtMillis = chimeAtMillis;
        view.startChime(repository, chimeAtMillis);
    }

    static long upcomingChimeAtMillis(Calendar now, boolean hourlyEnabled,
            boolean halfHourEnabled) {
        int minute = now.get(Calendar.MINUTE);
        boolean approachingHour = hourlyEnabled && minute == 59;
        boolean approachingHalfHour = halfHourEnabled && minute == 29;
        if (now.get(Calendar.SECOND) < 58 || (!approachingHour && !approachingHalfHour)) {
            return Long.MIN_VALUE;
        }
        Calendar chimeAt = (Calendar) now.clone();
        chimeAt.add(Calendar.MINUTE, 1);
        chimeAt.set(Calendar.SECOND, 0);
        chimeAt.set(Calendar.MILLISECOND, 0);
        return chimeAt.getTimeInMillis();
    }

    private boolean isQuiet(Calendar now) {
        if (!repository.isHourlyChimeQuietEnabled()) return false;
        int current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return isQuietAtMinute(current, repository.getHourlyChimeQuietStart(),
                repository.getHourlyChimeQuietEnd());
    }

    static boolean isQuietAtMinute(int current, int start, int end) {
        current = normalizeMinute(current);
        start = normalizeMinute(start);
        end = normalizeMinute(end);
        if (start == end) return true;
        return start < end ? current >= start && current < end
                : current >= start || current < end;
    }

    static TimeZone resolveTimeZone(String zoneId) {
        if (zoneId == null || zoneId.length() == 0) return TimeZone.getDefault();
        return TimeZone.getTimeZone(zoneId);
    }

    private static int normalizeMinute(int value) {
        int normalized = value % (24 * 60);
        return normalized < 0 ? normalized + 24 * 60 : normalized;
    }
}
