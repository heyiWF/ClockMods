package com.clockmods.widget.update

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.provider.AnalogClockWidgetProvider
import com.clockmods.widget.provider.CalendarWidgetProvider
import com.clockmods.widget.provider.DigitalClockWidgetProvider
import com.clockmods.widget.provider.WeatherClockWidgetProvider
import com.clockmods.widget.render.WidgetRemoteViewsFactory
import com.clockmods.widget.store.WidgetConfigStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object WidgetUpdateCoordinator {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val providers = arrayOf(
        DigitalClockWidgetProvider::class.java,
        AnalogClockWidgetProvider::class.java,
        WeatherClockWidgetProvider::class.java,
        CalendarWidgetProvider::class.java,
    )

    @JvmStatic fun execute(work: Runnable) {
        executor.execute(work)
    }

    @JvmStatic fun kindFor(context: Context, id: Int): WidgetKind? {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)
        if (info == null || context.packageName != info.provider.packageName) return null
        for (index in providers.indices) {
            if (providers[index].name == info.provider.className) return WidgetKind.values()[index]
        }
        return null
    }

    @JvmStatic fun active(context: Context): List<WidgetConfig> {
        val all = ArrayList<WidgetConfig>()
        val store = WidgetConfigStore(context)
        val manager = AppWidgetManager.getInstance(context)
        for (index in providers.indices) {
            val kind = WidgetKind.values()[index]
            for (id in manager.getAppWidgetIds(ComponentName(context, providers[index]))) {
                val config = store.getOrDefault(id, kind)
                if (!store.contains(id)) store.save(config)
                all += config
            }
        }
        return all
    }

    @JvmStatic fun weatherCount(context: Context): Int = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, WeatherClockWidgetProvider::class.java)).size

    @JvmStatic fun updateOne(context: Context, id: Int) {
        val kind = kindFor(context, id) ?: return
        AppWidgetManager.getInstance(context).updateAppWidget(id, WidgetRemoteViewsFactory.createResponsive(context, id, kind))
    }

    @JvmStatic fun updateAll(context: Context) {
        active(context).forEach { updateOne(context, it.appWidgetId) }
        reconcile(context)
    }

    @JvmStatic fun updateWeatherWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        manager.getAppWidgetIds(ComponentName(context, WeatherClockWidgetProvider::class.java))
            .forEach { updateOne(context, it) }
    }

    @JvmStatic fun reconcile(context: Context) {
        val configs = active(context)
        val ids = configs.mapTo(HashSet()) { it.appWidgetId }
        val weather = configs.count { it.kind == WidgetKind.WEATHER }
        val store = WidgetConfigStore(context)
        store.getAll().filterNot { ids.contains(it.appWidgetId) }.forEach { store.delete(it.appWidgetId) }
        WidgetRefreshWorker.schedule(context, weather)
        WidgetMidnightScheduler.schedule(context, configs)
    }
}
