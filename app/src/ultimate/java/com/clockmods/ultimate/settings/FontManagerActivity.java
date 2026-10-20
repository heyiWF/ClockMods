package com.clockmods.ultimate.settings;

import android.app.Application;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.background.OpenTypeFont;
import com.clockmods.background.UserFontStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** File-level management shared by the View and Compose editions. */
public final class FontManagerActivity extends ComponentActivity {
    private LinearLayout content;
    private ImportModel model;
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(), uris -> { if (!uris.isEmpty()) model.importFiles(uris); });

    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleManager.wrap(base)); }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) getActionBar().hide();
        UserFontStore.initialize(this);
        model = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ImportModel.class);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(16), dp(24), dp(24)); scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        setContentView(scroll);
        model.state.observe(this, ignored -> render());
    }
    private void render() {
        content.removeAllViews();
        Button back = button(getString(R.string.font_import_back), this::finish); content.addView(back);
        text(content, getString(R.string.font_import_title), 25, true);
        text(content, getString(R.string.font_import_summary), 14, false);
        Button add = button(getString(R.string.font_import_add), () -> picker.launch(new String[]{"*/*"}));
        add.setEnabled(!model.busy); content.addView(add);
        if (model.busy) text(content, getString(R.string.font_import_busy), 16, false);
        for (Result result : model.results) {
            TextView feedback = text(content, getString(result.message, result.name), 15, false);
            feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        if (UserFontStore.files().isEmpty()) text(content, getString(R.string.font_import_empty), 16, false);
        for (UserFontStore.Family family : UserFontStore.families()) {
            text(content, family.name, 21, true);
            TextView sample = text(content, "12:34 · Aa · 中文 · 日本語 · 한국어", 22, false);
            sample.setTypeface(UserFontStore.resolve(family.id, 400));
            java.util.HashSet<String> shown = new java.util.HashSet<>();
            for (UserFontStore.Face face : family.faces) {
                if (!shown.add(face.source.id)) continue;
                text(content, face.source.name + "\n" + getString(R.string.font_import_weights, weights(face.font.weights)), 15, true);
                text(content, coverage(this, face.font), 13, false);
                Button remove = button(getString(R.string.font_import_delete), () -> new AlertDialog.Builder(this)
                        .setTitle(R.string.font_import_delete)
                        .setMessage(getString(R.string.font_import_delete_confirm, face.source.name))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.font_import_delete, (dialog, which) -> model.delete(face.source.id)).show());
                remove.setContentDescription(getString(R.string.font_import_delete) + " " + face.source.name);
                remove.setEnabled(!model.busy); content.addView(remove);
            }
        }
    }
    private static String weights(int[] values) {
        StringBuilder result = new StringBuilder();
        for (int value : values) { if (result.length() > 0) result.append(" · "); result.append(value); }
        return result.toString();
    }
    public static String coverage(Context context, OpenTypeFont font) {
        return context.getString(R.string.font_import_coverage,
                font.coverage(FontLanguageSamples.CHINESE), font.coverage(FontLanguageSamples.ENGLISH),
                font.coverage(FontLanguageSamples.JAPANESE), font.coverage(FontLanguageSamples.KOREAN));
    }
    private TextView text(LinearLayout parent, String value, int size, boolean bold) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size);
        if (bold) text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setPadding(0, dp(12), 0, dp(8)); parent.addView(text); return text;
    }
    private Button button(String title, Runnable action) {
        Button button = new Button(this); button.setText(title); button.setAllCaps(false);
        button.setMinHeight(dp(48)); button.setOnClickListener(view -> action.run()); return button;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static final class Result {
        final int message; final String name;
        Result(int message, String name) { this.message = message; this.name = name; }
    }
    public static final class ImportModel extends AndroidViewModel {
        final MutableLiveData<Integer> state = new MutableLiveData<>(0);
        volatile boolean busy;
        volatile List<Result> results = new ArrayList<>();
        private final ExecutorService worker = Executors.newSingleThreadExecutor();
        public ImportModel(Application application) { super(application); }
        void importFiles(List<Uri> uris) {
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
        void delete(String id) {
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
}
