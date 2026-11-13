package com.clockmods.pro.alarm;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import java.time.DayOfWeek;
import java.time.format.TextStyle;

public final class AlarmLabels {
    public static final int[] REPEATS = { R.string.alarm_once, R.string.alarm_weekdays,
            R.string.alarm_holidays, R.string.alarm_workdays, R.string.alarm_shift };
    private AlarmLabels() {}
    public static String repeat(Context context, AlarmSpec alarm) {
        return alarm.repeat == AlarmSpec.Repeat.WEEKLY ? weekdays(context, alarm.weekdays) : context.getString(REPEATS[alarm.repeat.ordinal()]);
    }
    public static String[] dayNames(Context context) {
        String[] names = new String[7];
        for (int i = 0; i < 7; i++) names[i] = DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL, LocaleManager.resolveLocale(context));
        return names;
    }
    public static String weekdays(Context context, int bits) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 7; i++) if ((bits & (1 << i)) != 0) {
            if (result.length() > 0) result.append(" · ");
            result.append(DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, LocaleManager.resolveLocale(context)));
        }
        return result.length() == 0 ? context.getString(R.string.alarm_choose_days) : result.toString();
    }
    public static String ringtone(Context context, AlarmSpec alarm) {
        if ("default".equals(alarm.ringtone)) return context.getString(R.string.alarm_default_ringtone);
        if ("silent".equals(alarm.ringtone)) return context.getString(R.string.alarm_silent);
        try { Ringtone tone = RingtoneManager.getRingtone(context, Uri.parse(alarm.ringtone));
            if (tone != null) return tone.getTitle(context); } catch (RuntimeException ignored) { }
        return context.getString(R.string.alarm_default_ringtone);
    }
}
