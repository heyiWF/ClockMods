package com.clockmods.widget.render;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.net.Uri;
import com.clockmods.widget.config.WidgetConfigActivity;
import com.clockmods.widget.update.WidgetDataChangedReceiver;
import com.clockmods.ultimate.UltimateMainActivity;
import com.clockmods.ultimate.settings.UltimateSettingsActivity;
public final class WidgetPendingIntentFactory {
    public enum Action { CLOCK, CALENDAR, WEATHER, CONFIG, REFRESH }
    private WidgetPendingIntentFactory() { }
    public static int requestCode(int id,Action action) { return 31*id+action.ordinal(); }
    public static String identity(int id,Action action) { return "clockmods-widget://instance/"+id+"/"+action.name(); }
    public static PendingIntent create(Context c,int id,Action action) {
        Intent i;
        switch(action) {
            case CONFIG: i=new Intent(c,WidgetConfigActivity.class); break;
            case WEATHER: i=UltimateSettingsActivity.createSubpageIntent(c,"weather"); break;
            case REFRESH: i=new Intent(c,WidgetDataChangedReceiver.class).setAction(WidgetDataChangedReceiver.REFRESH); break;
            default: i=new Intent(c,UltimateMainActivity.class); break;
        }
        i.setData(Uri.parse(identity(id,action))).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;
        return action==Action.REFRESH ? PendingIntent.getBroadcast(c,requestCode(id,action),i,flags)
            : PendingIntent.getActivity(c,requestCode(id,action),i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),flags);
    }
    public static Action tap(String value) {
        switch(value) { case "open_config": return Action.CONFIG; case "open_weather": return Action.WEATHER; case "open_calendar": return Action.CALENDAR; default: return Action.CLOCK; }
    }
}
