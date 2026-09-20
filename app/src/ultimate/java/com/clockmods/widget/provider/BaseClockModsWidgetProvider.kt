package com.clockmods.widget.provider

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.store.WidgetConfigStore
import com.clockmods.widget.update.WidgetUpdateCoordinator

abstract class BaseClockModsWidgetProvider : AppWidgetProvider() {
    protected abstract fun getWidgetKind(): WidgetKind

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != AppWidgetManager.ACTION_APPWIDGET_UPDATE &&
            action != AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED &&
            action != AppWidgetManager.ACTION_APPWIDGET_DELETED &&
            action != AppWidgetManager.ACTION_APPWIDGET_ENABLED &&
            action != AppWidgetManager.ACTION_APPWIDGET_DISABLED &&
            action != AppWidgetManager.ACTION_APPWIDGET_RESTORED
        ) return
        super.onReceive(context, intent)
    }

    private fun async(context: Context, work: () -> Unit) {
        val pending = goAsync()
        WidgetUpdateCoordinator.execute {
            try {
                work()
            } finally {
                pending.finish()
            }
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async(context) {
        ids.forEach { WidgetUpdateCoordinator.updateOne(context, it) }
        WidgetUpdateCoordinator.reconcile(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) = async(context) { WidgetUpdateCoordinator.updateOne(context, appWidgetId) }

    override fun onDeleted(context: Context, ids: IntArray) = async(context) {
        val store = WidgetConfigStore(context)
        ids.forEach { store.delete(it) }
        WidgetUpdateCoordinator.reconcile(context)
    }

    override fun onEnabled(context: Context) = async(context) {
        WidgetUpdateCoordinator.reconcile(context)
    }

    override fun onDisabled(context: Context) = async(context) {
        WidgetUpdateCoordinator.reconcile(context)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) = async(context) {
        val store = WidgetConfigStore(context)
        repeat(minOf(oldWidgetIds.size, newWidgetIds.size)) { index ->
            store.save(
                store.getOrDefault(oldWidgetIds[index], getWidgetKind())
                    .toBuilder()
                    .appWidgetId(newWidgetIds[index])
                    .build(),
            )
            store.delete(oldWidgetIds[index])
        }
        WidgetUpdateCoordinator.updateAll(context)
    }
}
