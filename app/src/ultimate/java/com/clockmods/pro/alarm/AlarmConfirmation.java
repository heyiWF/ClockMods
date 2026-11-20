package com.clockmods.pro.alarm;

import android.content.Context;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.widget.Toast;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import java.util.ArrayList;

/** Foreground feedback for a user-initiated schedule change. */
public final class AlarmConfirmation {
    private static Toast current;
    private AlarmConfirmation() {}

    public static void show(Context context, long scheduledAt) {
        if (current != null) current.cancel();
        Context localized = LocaleManager.wrap(context.getApplicationContext());
        long remaining = scheduledAt - System.currentTimeMillis();
        String message;
        if (scheduledAt == 0) {
            message = localized.getString(R.string.alarm_no_next);
        } else if (remaining < 60000) {
            message = localized.getString(R.string.alarm_within_minute);
        } else {
            long minutes = remaining / 60000;
            ArrayList<Measure> units = new ArrayList<>(3);
            add(units, minutes / 1440, MeasureUnit.DAY);
            add(units, minutes / 60 % 24, MeasureUnit.HOUR);
            add(units, minutes % 60, MeasureUnit.MINUTE);
            MeasureFormat format = MeasureFormat.getInstance(LocaleManager.resolveLocale(localized), MeasureFormat.FormatWidth.WIDE);
            message = localized.getString(R.string.alarm_set_in, format.formatMeasures(units.toArray(new Measure[0])));
        }
        current = Toast.makeText(localized, message, Toast.LENGTH_SHORT);
        current.show();
    }

    private static void add(ArrayList<Measure> units, long amount, MeasureUnit unit) {
        if (amount > 0) units.add(new Measure(amount, unit));
    }
}
