package com.clockmods.ultimate.clock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.clockmods.sdk.clock.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.TimeZone;

/** Real Android fonts and Canvas, with deterministic viewport and font-scale inputs. */
public final class ResponsiveClockProbe {
    private ResponsiveClockProbe() {}
    public static int run(Context context, File directory) throws Exception {
        int checks = 0;
        int[][] sizes = {{1280, 720}, {720, 1280}, {1600, 240}, {240, 1600}, {320, 240}};
        for (ClockStyle style : UltimateClockStyles.sharedRegistry().getStyles()) {
            for (int[] size : sizes) for (int zoom : new int[] {1, 2}) {
                Bitmap first = render(style, size, zoom, 1791292500123L);
                Bitmap next = render(style, size, zoom, 1791292560123L);
                if (first.sameAs(next)) throw new AssertionError(style.getMetadata().getId()
                        + " primary clock does not update at " + size[0] + "x" + size[1]);
                checks++;
                File file = new File(directory, "responsive-" + style.getMetadata().getId()
                        + "-" + size[0] + "x" + size[1] + "-" + zoom + ".png");
                try (FileOutputStream output = new FileOutputStream(file)) {
                    first.compress(Bitmap.CompressFormat.PNG, 100, output);
                }
                first.recycle(); next.recycle();
            }
        }
        return checks;
    }

    private static Bitmap render(ClockStyle style, int[] size, int zoom, long now) {
        ClockState state = ClockState.builder(now).locale(Locale.SIMPLIFIED_CHINESE)
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai")).dateText("2026年10月7日 星期三")
                .weatherText("晴朗 26℃ · 空气质量良好").timeZoneText("UTC+8")
                .timeScale(1.5f).dateScale(2f).supportingScale(2f).build();
        Bitmap bitmap = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int save = canvas.getSaveCount();
        style.getRenderer().render(canvas, new ClockRenderContext(0, 0, size[0], size[1],
                2f, 2f * zoom, now), state, style.getThemeTokens());
        if (canvas.getSaveCount() != save) throw new AssertionError("Unbalanced Canvas: " + style.getMetadata().getId());
        return bitmap;
    }
}
