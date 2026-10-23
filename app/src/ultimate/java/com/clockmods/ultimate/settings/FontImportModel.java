package com.clockmods.ultimate.settings;
import android.app.Application;
import android.net.Uri;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.clockmods.R;
import com.clockmods.background.UserFontStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Retains font-file operations and feedback across configuration changes. */
public final class FontImportModel extends AndroidViewModel {
    public static final class Result {
        public final int message; public final String name;
        Result(int message, String name) { this.message = message; this.name = name; }
    }

    public final MutableLiveData<Integer> state = new MutableLiveData<>(0);
    public volatile boolean busy;
    public volatile List<Result> results = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    public FontImportModel(Application application) { super(application); }
    public void importFiles(List<Uri> uris) {
        if (busy) return; busy = true; results = new ArrayList<>(); state.setValue(1);
        worker.execute(() -> {
            List<Result> next = new ArrayList<>();
            for (Uri uri : uris) {
                try {
                    UserFontStore.ImportedFile file = UserFontStore.importUri(getApplication(), uri);
                    next.add(new Result(R.string.font_import_success, file.name));
                } catch (Exception e) {
                    String reason = e instanceof UserFontStore.FontException ? ((UserFontStore.FontException)e).reason : "invalid";
                    int message = reason.equals("duplicate") ? R.string.font_import_duplicate
                            : reason.equals("language") ? R.string.font_import_no_language
                            : reason.equals("limit") ? R.string.font_import_limit : R.string.font_import_invalid;
                    next.add(new Result(message, ""));
                }
            }
            results = next; busy = false; state.postValue(2);
        });
    }
    public void delete(String id) {
        if (busy) return; busy = true; state.setValue(1);
        worker.execute(() -> {
            boolean deleted = UserFontStore.delete(id);
            List<Result> next = new ArrayList<>();
            next.add(new Result(deleted ? R.string.font_import_deleted : R.string.font_import_delete_failed, ""));
            results = next; busy = false; state.postValue(2);
        });
    }
    @Override protected void onCleared() { worker.shutdown(); }
}
