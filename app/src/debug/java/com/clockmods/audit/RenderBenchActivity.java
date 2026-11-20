package com.clockmods.audit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;

import com.clockmods.background.BackgroundRepository;
import com.clockmods.ui.ClockView;

/**
 * Debug-only render benchmark for {@link ClockView}.
 *
 * <p>Draws the clock into an offscreen bitmap of the screen size and reports, per draw, the
 * wall-clock cost and the number of objects the UI thread allocates. The numbers are what the
 * optimization work is judged on: a change is kept only if it moves them.
 *
 * <p>Three scenarios are measured:
 * <ul>
 *   <li>{@code steady} - consecutive draws inside the same second (the per-tick partial pass);</li>
 *   <li>{@code new-second} - a draw taken right after the second rolled over, which runs the
 *       digit transition for the first time;</li>
 *   <li>{@code transition} - the frames that follow, which re-draw the fading digits.</li>
 * </ul>
 *
 * <p>Results are written to logcat under the {@code RENDER_BENCH} tag.
 */
public class RenderBenchActivity extends Activity {
    private static final String TAG = "RENDER_BENCH";
    private static final int STEADY_DRAWS = 300;
    private static final int STEADY_ROUNDS = 5;
    private static final int TRANSITION_DRAWS = 12;

    private ClockView view;
    private Canvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        BackgroundRepository repository = new BackgroundRepository(this);
        view = new ClockView(this);
        view.setBackgroundRepository(repository);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        // Warm-up resolves the typefaces and fills the date/lunar/width caches, so the first
        // measured draw is not paying for one-time work.
        for (int i = 0; i < 5; i++) {
            view.draw(canvas);
        }
        Log.i(TAG, "=== render bench " + width + "x" + height + " ===");
        for (int round = 1; round <= STEADY_ROUNDS; round++) {
            measure("steady#" + round, STEADY_DRAWS, 0L);
        }

        // Wait for the second to roll over, then take the first draw (transition start) and the
        // frames that follow it (the fading digits), measured separately.
        sleepQuietly(1100L);
        measure("new-second", 1, 0L);
        measure("transition", TRANSITION_DRAWS, 8L);

        Log.i(TAG, "=== render bench done ===");
        finish();
    }

    private void measure(String label, int draws, long sleepMillis) {
        Debug.startAllocCounting();
        long objects0 = Debug.getThreadAllocCount();
        long startNanos = System.nanoTime();
        long cpu0 = Debug.threadCpuTimeNanos();
        for (int i = 0; i < draws; i++) {
            view.draw(canvas);
            if (sleepMillis > 0L) {
                sleepQuietly(sleepMillis);
            }
        }
        long elapsedNanos = System.nanoTime() - startNanos;
        long cpuNanos = Debug.threadCpuTimeNanos() - cpu0;
        long objects = Debug.getThreadAllocCount() - objects0;
        Debug.stopAllocCounting();
        Log.i(TAG, label + " | draws=" + draws
                + " ms/draw=" + round(elapsedNanos / 1e6 / draws)
                + " cpu/draw=" + round(cpuNanos / 1e6 / draws)
                + " objects/draw=" + round(objects / (double) draws));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
