package com.clockmods.widget.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.clockmods.weather.QWeatherConfig
import com.clockmods.weather.WeatherRefreshUseCase
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val context = applicationContext
        if (!WidgetRefreshPolicy.shouldSchedule(WidgetUpdateCoordinator.weatherCount(context))) return Result.success()
        val result = WeatherRefreshUseCase(context).refreshForWidget(inputData.getBoolean("force", false))
        WidgetUpdateCoordinator.updateWeatherWidgets(context)
        return if (WidgetRefreshPolicy.shouldRetry(
                QWeatherConfig.isConfigured(),
                result == WeatherRefreshUseCase.RefreshResult.TRANSIENT_FAILURE,
                runAttemptCount,
            )
        ) Result.retry() else Result.success()
    }

    companion object {
        const val PERIODIC = "clockmods_widget_weather_refresh"
        const val MANUAL = "clockmods_widget_weather_manual"

        @JvmStatic fun schedule(context: Context, count: Int) {
            val manager = WorkManager.getInstance(context)
            if (!WidgetRefreshPolicy.shouldSchedule(count)) {
                manager.cancelUniqueWork(PERIODIC)
                manager.cancelUniqueWork(MANUAL)
                return
            }
            manager.enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequest.Builder(WidgetRefreshWorker::class.java, 30, TimeUnit.MINUTES)
                    .setConstraints(network())
                    .build(),
            )
        }

        @JvmStatic fun refresh(context: Context) {
            if (WidgetUpdateCoordinator.weatherCount(context) == 0) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                MANUAL,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(WidgetRefreshWorker::class.java)
                    .setConstraints(network())
                    .setInputData(Data.Builder().putBoolean("force", true).build())
                    .build(),
            )
        }

        private fun network(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
