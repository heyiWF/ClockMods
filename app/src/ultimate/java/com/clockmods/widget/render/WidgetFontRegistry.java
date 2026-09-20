package com.clockmods.widget.render;

import android.content.Context;
import com.clockmods.R;
import com.clockmods.widget.model.WidgetConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Named system families available to both the preview and a restricted launcher host. */
public final class WidgetFontRegistry {
    public static final String THEME = WidgetConfig.FONT_THEME;
    // Resource column order is independent of the selectable ID order.
    private static final List<String> FAMILIES = Arrays.asList("system", "serif", "monospace", "condensed", "light");
    public static final int COLUMNS = 5;
    private WidgetFontRegistry() { }

    public static List<String> ids() {
        return Collections.unmodifiableList(Arrays.asList(WidgetConfig.FONT_IDS.clone()));
    }
    public static String idAt(int index) {
        return index < 0 || index >= WidgetConfig.FONT_IDS.length ? THEME : WidgetConfig.FONT_IDS[index];
    }
    public static int indexOf(String id) {
        int index = Arrays.asList(WidgetConfig.FONT_IDS).indexOf(id);
        return Math.max(0, index);
    }
    public static boolean isKnown(String id) {
        return Arrays.asList(WidgetConfig.FONT_IDS).contains(id);
    }
    public static String[] labels(Context context) {
        return new String[]{context.getString(R.string.widget_font_theme),
                context.getString(R.string.widget_font_system), context.getString(R.string.widget_font_serif),
                context.getString(R.string.widget_font_monospace), context.getString(R.string.widget_font_condensed),
                context.getString(R.string.widget_font_light)};
    }
    public static int columnOf(String fontId, int themeColumn) {
        int column = FAMILIES.indexOf(fontId);
        return column < 0 ? Math.max(0, Math.min(2, themeColumn)) : column;
    }
}
