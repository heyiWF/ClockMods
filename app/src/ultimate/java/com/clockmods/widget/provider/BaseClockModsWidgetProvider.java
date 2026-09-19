package com.clockmods.widget.provider;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import com.clockmods.widget.model.WidgetKind;
import com.clockmods.widget.store.WidgetConfigStore;
import com.clockmods.widget.update.WidgetUpdateCoordinator;
public abstract class BaseClockModsWidgetProvider extends AppWidgetProvider {
    protected abstract WidgetKind getWidgetKind();
    @Override public void onReceive(Context c,Intent i) {
        String action=i.getAction();
        if(!AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action) && !AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED.equals(action)
            && !AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action) && !AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)
            && !AppWidgetManager.ACTION_APPWIDGET_DISABLED.equals(action) && !AppWidgetManager.ACTION_APPWIDGET_RESTORED.equals(action)) return;
        super.onReceive(c,i);
    }
    private void async(Context c,Runnable work) {
        PendingResult pending=goAsync();
        WidgetUpdateCoordinator.execute(()-> { try { work.run(); } finally { if(pending!=null) pending.finish(); } });
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids) { async(c,()-> {
        for(int id:ids) WidgetUpdateCoordinator.updateOne(c,id); WidgetUpdateCoordinator.reconcile(c);
    }); }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options) { async(c,()->WidgetUpdateCoordinator.updateOne(c,id)); }
    @Override public void onDeleted(Context c,int[] ids) { async(c,()-> {
        WidgetConfigStore store=new WidgetConfigStore(c); for(int id:ids) store.delete(id); WidgetUpdateCoordinator.reconcile(c);
    }); }
    @Override public void onEnabled(Context c) { async(c,()->WidgetUpdateCoordinator.reconcile(c)); }
    @Override public void onDisabled(Context c) { async(c,()->WidgetUpdateCoordinator.reconcile(c)); }
    @Override public void onRestored(Context c,int[] oldIds,int[] newIds) { async(c,()-> {
        WidgetConfigStore store=new WidgetConfigStore(c);
        for(int n=0;n<Math.min(oldIds.length,newIds.length);n++) {
            store.save(store.getOrDefault(oldIds[n],getWidgetKind()).toBuilder().appWidgetId(newIds[n]).build()); store.delete(oldIds[n]);
        }
        WidgetUpdateCoordinator.updateAll(c);
    }); }
}
