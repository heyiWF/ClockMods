package com.clockmods.ultimate.settings;
import android.app.Application;
import android.net.Uri;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.clockmods.R;
import com.clockmods.calendar.imports.UserCalendarStore;
import com.clockmods.calendar.imports.CalendarImportException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Retains calendar-file operations and feedback across configuration changes. */
public final class CalendarImportModel extends AndroidViewModel {
    public static final class Result {
        public final int message; public final String name;
        Result(int message, String name) { this.message = message; this.name = name; }
    }

    public final MutableLiveData<Integer> state = new MutableLiveData<>(0);
    public volatile boolean busy;
    public volatile List<Result> results = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    public CalendarImportModel(Application application) { super(application); }
    public void importFiles(List<Uri> uris) {
        if (busy) return; busy = true; results = new ArrayList<>(); state.setValue(1);
        worker.execute(() -> {
            List<Result> next = new ArrayList<>();
            for (Uri uri : uris) {
                try {
                    UserCalendarStore.Source file = UserCalendarStore.get(getApplication()).importUri(uri);
                    next.add(new Result(R.string.calendar_import_success, file.name));
                } catch (Exception e) {
                    String reason = e instanceof CalendarImportException ? ((CalendarImportException)e).reason : "invalid";
                    int message = reason.equals("duplicate") ? R.string.calendar_import_duplicate
                            : reason.equals("empty") ? R.string.calendar_import_no_events
                            : reason.equals("unsupported") ? R.string.calendar_import_unsupported
                            : reason.equals("timezone") ? R.string.calendar_import_timezone
                            : reason.equals("limit") ? R.string.calendar_import_limit : R.string.calendar_import_invalid;
                    next.add(new Result(message, ""));
                }
            }
            results = next; busy = false; state.postValue(2);
        });
    }
    public void delete(String id) {
        if (busy) return; busy = true; state.setValue(1);
        worker.execute(() -> {
            boolean deleted;
            try { deleted = UserCalendarStore.get(getApplication()).delete(id); }
            catch (Exception failure) { deleted = false; }
            List<Result> next = new ArrayList<>();
            next.add(new Result(deleted ? R.string.calendar_import_deleted : R.string.calendar_import_delete_failed, ""));
            results = next; busy = false; state.postValue(2);
        });
    }
    @Override protected void onCleared() { worker.shutdown(); }
}
