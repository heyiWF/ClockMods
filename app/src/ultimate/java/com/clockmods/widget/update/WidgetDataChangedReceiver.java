package com.clockmods.widget.update;
import android.content.*;
import android.appwidget.AppWidgetManager;
import com.clockmods.widget.model.WidgetKind;
public final class WidgetDataChangedReceiver extends BroadcastReceiver {
    public static final String REFRESH="com.clockmods.widget.REFRESH";
    public static final String MIDNIGHT="com.clockmods.widget.MIDNIGHT";
    public static final String WEATHER_CHANGED="com.clockmods.widget.WEATHER_CHANGED";
    public static final String TIME_ZONE_CHANGED="com.clockmods.widget.TIME_ZONE_CHANGED";
    @Override public void onReceive(Context c,Intent i) {
        String a=i.getAction();
        boolean system=Intent.ACTION_DATE_CHANGED.equals(a)||Intent.ACTION_TIME_CHANGED.equals(a)||Intent.ACTION_TIMEZONE_CHANGED.equals(a)||Intent.ACTION_LOCALE_CHANGED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)||Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_USER_PRESENT.equals(a)||Intent.ACTION_CONFIGURATION_CHANGED.equals(a)||Intent.ACTION_WALLPAPER_CHANGED.equals(a);
        if(!system && !REFRESH.equals(a) && !MIDNIGHT.equals(a) && !WEATHER_CHANGED.equals(a) && !TIME_ZONE_CHANGED.equals(a)) return;
        PendingResult pending=goAsync(); Context app=c.getApplicationContext();
        WidgetUpdateCoordinator.execute(()-> { try {
            if(REFRESH.equals(a)) {
                if(WidgetUpdateCoordinator.kindFor(app,i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1))==WidgetKind.WEATHER) WidgetRefreshWorker.refresh(app);
            } else if(WEATHER_CHANGED.equals(a)) WidgetUpdateCoordinator.updateWeatherWidgets(app);
            else WidgetUpdateCoordinator.updateAll(app);
        } finally { pending.finish(); } });
    }
}
