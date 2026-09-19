package com.clockmods.widget.update;
import android.appwidget.*;
import android.content.*;
import com.clockmods.widget.model.*;
import com.clockmods.widget.store.WidgetConfigStore;
import com.clockmods.widget.provider.*;
import com.clockmods.widget.render.WidgetRemoteViewsFactory;
import java.util.*;
import java.util.concurrent.*;
public final class WidgetUpdateCoordinator {
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
    private static final Class<?>[] PROVIDERS={DigitalClockWidgetProvider.class,AnalogClockWidgetProvider.class,WeatherClockWidgetProvider.class,CalendarWidgetProvider.class};
    private WidgetUpdateCoordinator() { }
    public static void execute(Runnable work) { EXECUTOR.execute(work); }
    public static WidgetKind kindFor(Context c,int id) {
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
        if(info==null || !c.getPackageName().equals(info.provider.getPackageName())) return null;
        for(int i=0;i<PROVIDERS.length;i++) if(PROVIDERS[i].getName().equals(info.provider.getClassName())) return WidgetKind.values()[i];
        return null;
    }
    public static List<WidgetConfig> active(Context c) {
        List<WidgetConfig> all=new ArrayList<>(); WidgetConfigStore store=new WidgetConfigStore(c);
        for(int i=0;i<PROVIDERS.length;i++) for(int id:AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,PROVIDERS[i]))) {
            WidgetConfig config=store.getOrDefault(id,WidgetKind.values()[i]);
            if(!store.contains(id)) store.save(config);
            all.add(config);
        }
        return all;
    }
    public static int weatherCount(Context c) { return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,WeatherClockWidgetProvider.class)).length; }
    public static void updateOne(Context c,int id) {
        WidgetKind kind=kindFor(c,id); if(kind==null) return;
        AppWidgetManager.getInstance(c).updateAppWidget(id,WidgetRemoteViewsFactory.createResponsive(c,id,kind));
    }
    public static void updateAll(Context c) {
        for(WidgetConfig config:active(c)) updateOne(c,config.appWidgetId);
        reconcile(c);
    }
    public static void updateWeatherWidgets(Context c) {
        for(int id:AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,WeatherClockWidgetProvider.class))) updateOne(c,id);
    }
    public static void reconcile(Context c) {
        List<WidgetConfig> configs=active(c); Set<Integer> ids=new HashSet<>(); int weather=0;
        for(WidgetConfig config:configs) { ids.add(config.appWidgetId); if(config.kind==WidgetKind.WEATHER) weather++; }
        WidgetConfigStore store=new WidgetConfigStore(c);
        for(WidgetConfig config:store.getAll()) if(!ids.contains(config.appWidgetId)) store.delete(config.appWidgetId);
        WidgetRefreshWorker.schedule(c,weather); WidgetMidnightScheduler.schedule(c,configs);
    }
}
