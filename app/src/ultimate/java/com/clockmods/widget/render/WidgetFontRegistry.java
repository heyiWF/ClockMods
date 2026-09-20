package com.clockmods.widget.render;

import android.content.Context;

import com.clockmods.R;
import com.clockmods.background.FontCatalog;
import com.clockmods.widget.model.WidgetConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import static com.clockmods.background.ClockPreferences.*;

/**
 * The typefaces a widget instance may be set to.
 *
 * <p>The selectable families are exactly the ones the app offers — the list is read from
 * {@link FontCatalog} rather than duplicated — plus a leading {@link #THEME} mode. That mode is the
 * default and simply keeps the font structure the theme was designed around (the layouts ship a
 * sans, a serif and a monospace variant and each theme picks one), so changing a widget's theme
 * never silently re-fonts it.
 *
 * <p>Ids are persisted inside {@code WidgetConfig}, so they are stable API: never rename one, only
 * add. The id list itself lives in {@link WidgetConfig#FONT_IDS}.
 *
 * <p>Every id maps to one column of the layout table in {@link WidgetRemoteViewsFactory}: columns 0
 * to 2 are the theme's structural variants and the app's own families follow from column 3. The
 * app's {@code system} family is the sans column, since that is the same system typeface.
 */
public final class WidgetFontRegistry {
    /** Follow the theme's own font structure. */
    public static final String THEME = WidgetConfig.FONT_THEME;

    /** Structural columns: sans, serif, monospace. */
    private static final int STRUCTURAL_COLUMNS = 3;

    // Resource columns are a render contract, independent of the settings catalog's ordering.
    private static final List<String> BUNDLED = Collections.unmodifiableList(Arrays.asList(
            FONT_ROBOTO, FONT_GOOGLE_SANS_DISPLAY, FONT_GOOGLE_SANS_TEXT, FONT_SF_PRO_DISPLAY,
            FONT_SF_PRO_ROUNDED, FONT_INTER, FONT_LATO, FONT_LORA, FONT_NOTO_SANS, FONT_BITCOUNT));

    /** One column per structural variant plus one per bundled family that needs its own file. */
    public static final int COLUMNS = STRUCTURAL_COLUMNS + BUNDLED.size();

    private WidgetFontRegistry() {
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(Arrays.asList(WidgetConfig.FONT_IDS.clone()));
    }

    /** Stable id for {@code index}, clamped; unknown input falls back to {@link #THEME}. */
    public static String idAt(int index) {
        String[] ids = WidgetConfig.FONT_IDS;
        return index < 0 || index >= ids.length ? THEME : ids[index];
    }

    /** Position of {@code id}, or 0 when it is unknown, so a bad value degrades to the theme. */
    public static int indexOf(String id) {
        String[] ids = WidgetConfig.FONT_IDS;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(id)) return i;
        }
        return 0;
    }

    /** {@code true} when {@code id} is one this build can render. */
    public static boolean isKnown(String id) {
        for (String candidate : WidgetConfig.FONT_IDS) {
            if (candidate.equals(id)) return true;
        }
        return false;
    }

    /**
     * Dropdown labels in {@link #ids()} order: the follow-theme mode first, then the app's own
     * family names verbatim, including the localised name it shows for the system family.
     */
    public static String[] labels(Context context) {
        String[] families = FontCatalog.displayNames(context);
        String[] labels = new String[families.length + 1];
        labels[0] = context.getString(R.string.widget_font_theme);
        System.arraycopy(families, 0, labels, 1, families.length);
        return labels;
    }

    /**
     * Layout column to inflate. {@code themeColumn} is the column the theme's font structure asked
     * for, used verbatim when the instance simply follows the theme.
     */
    public static int columnOf(String fontId, int themeColumn) {
        int fallback = Math.max(0, Math.min(STRUCTURAL_COLUMNS - 1, themeColumn));
        if (FONT_SYSTEM.equals(fontId)) return 0;
        int index = BUNDLED.indexOf(fontId);
        return index < 0 ? fallback : STRUCTURAL_COLUMNS + index;
    }
}
