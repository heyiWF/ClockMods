package com.clockmods.widget.update;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.clockmods.weather.*;
import java.util.concurrent.TimeUnit;
public final class WidgetRefreshWorker extends Worker {
    public static final String PERIODIC="clockmods_widget_weather_refresh";
    public static final String MANUAL="clockmods_widget_weather_manual";
    public WidgetRefreshWorker(@NonNull Context c,@NonNull WorkerParameters p) { super(c,p); }
    @NonNull @Override public Result doWork() {
        Context c=getApplicationContext();
        if(!WidgetRefreshPolicy.shouldSchedule(WidgetUpdateCoordinator.weatherCount(c))) return Result.success();
        WeatherRefreshUseCase.RefreshResult result=new WeatherRefreshUseCase(c).refreshForWidget(getInputData().getBoolean("force",false));
        WidgetUpdateCoordinator.updateWeatherWidgets(c);
        return WidgetRefreshPolicy.shouldRetry(QWeatherConfig.isConfigured(),result==WeatherRefreshUseCase.RefreshResult.TRANSIENT_FAILURE,getRunAttemptCount()) ? Result.retry() : Result.success();
    }
    public static void schedule(Context c,int count) {
        WorkManager manager=WorkManager.getInstance(c);
        if(!WidgetRefreshPolicy.shouldSchedule(count)) { manager.cancelUniqueWork(PERIODIC); manager.cancelUniqueWork(MANUAL); return; }
        manager.enqueueUniquePeriodicWork(PERIODIC,ExistingPeriodicWorkPolicy.UPDATE,
            new PeriodicWorkRequest.Builder(WidgetRefreshWorker.class,30,TimeUnit.MINUTES).setConstraints(network()).build());
    }
    public static void refresh(Context c) {
        if(WidgetUpdateCoordinator.weatherCount(c)==0) return;
        WorkManager.getInstance(c).enqueueUniqueWork(MANUAL,ExistingWorkPolicy.KEEP,
            new OneTimeWorkRequest.Builder(WidgetRefreshWorker.class).setConstraints(network()).setInputData(new Data.Builder().putBoolean("force",true).build()).build());
    }
    private static Constraints network() { return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(); }
}
