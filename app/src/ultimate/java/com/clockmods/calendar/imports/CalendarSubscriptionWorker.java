package com.clockmods.calendar.imports;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

/** One network-constrained job for enabled subscriptions; WorkManager survives process death/reboots. */
public final class CalendarSubscriptionWorker extends Worker {
    private static final String WORK = "calendar-subscriptions";
    public CalendarSubscriptionWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }
    @NonNull @Override public Result doWork() {
        UserCalendarStore store=UserCalendarStore.get(getApplicationContext());
        boolean failed=false;
        for(UserCalendarStore.Source source:store.files()) {
            if(isStopped()) return Result.retry();
            if(!source.syncEnabled || source.sourceUrl.isEmpty()) continue;
            try {store.synchronize(source);} catch(java.io.IOException error) {
                failed=true;
                try {store.syncFailed(source);} catch(java.io.IOException ignored) { }
            }
        }
        return failed && getRunAttemptCount()<3 ? Result.retry() : Result.success();
    }
    public static void schedule(Context context) {
        boolean enabled=false;
        for(UserCalendarStore.Source source:UserCalendarStore.get(context).files())
            if(source.syncEnabled && !source.sourceUrl.isEmpty()) {enabled=true;break;}
        WorkManager manager=WorkManager.getInstance(context);
        if(!enabled) {manager.cancelUniqueWork(WORK);return;}
        manager.enqueueUniquePeriodicWork(WORK,ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(CalendarSubscriptionWorker.class,6,TimeUnit.HOURS)
                        .setInitialDelay(6,TimeUnit.HOURS)
                        .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build());
    }
}
