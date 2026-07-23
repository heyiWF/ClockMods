package com.clockmods.background;

import com.clockmods.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central, data-driven registry of the clock font families.
 *
 * <p>The base families (system, Roboto, Google Sans Display) are available in every
 * flavor. The extra families bundled in the Pro flavor's assets are only
 * exposed when {@link BuildConfig#PRO_FONTS} is set, so the compat and modern
 * flavors neither list nor try to load fonts they do not ship.
 */
public final class FontCatalog {

    /** A single selectable font family. */
    public static final class FontOption {
        public final String id;
        public final String displayName;
        /** Asset path for the regular weight, or {@code null} for the system font. */
        public final String regularAsset;
        /** Asset path for the bold weight, or {@code null} to synthesize bold. */
        public final String boldAsset;

        FontOption(String id, String displayName, String regularAsset, String boldAsset) {
            this.id = id;
            this.displayName = displayName;
            this.regularAsset = regularAsset;
            this.boldAsset = boldAsset;
        }

        public boolean isSystem() {
            return regularAsset == null;
        }
    }

    private static final List<FontOption> OPTIONS = buildOptions();

    private FontCatalog() {
    }

    private static List<FontOption> buildOptions() {
        List<FontOption> options = new ArrayList<>();
        options.add(new FontOption(ClockPreferences.FONT_SYSTEM, "系统字体", null, null));
        options.add(new FontOption(ClockPreferences.FONT_ROBOTO, "Roboto",
                "fonts/Roboto-Regular.ttf", "fonts/Roboto-Bold.ttf"));
        options.add(new FontOption(ClockPreferences.FONT_GOOGLE_SANS_DISPLAY, "Google Sans Display",
            "fonts/GoogleSansDisplay-Regular.ttf", "fonts/GoogleSansDisplay-Bold.ttf"));
        if (BuildConfig.PRO_FONTS) {
            options.add(new FontOption(ClockPreferences.FONT_GOOGLE_SANS_TEXT, "Google Sans Text",
                "fonts/GoogleSansText-Regular.ttf", "fonts/GoogleSansText-Bold.ttf"));
            options.add(new FontOption(ClockPreferences.FONT_SF_PRO_DISPLAY, "SF Pro Display",
                    "fonts/SFProDisplay-Regular.otf", "fonts/SFProDisplay-Bold.otf"));
            options.add(new FontOption(ClockPreferences.FONT_SF_PRO_ROUNDED, "SF Pro Rounded",
                    "fonts/SFProRounded-Regular.otf", "fonts/SFProRounded-Bold.otf"));
            options.add(new FontOption(ClockPreferences.FONT_INTER, "Inter",
                    "fonts/Inter-Regular.ttf", "fonts/Inter-Bold.ttf"));
            options.add(new FontOption(ClockPreferences.FONT_LATO, "Lato",
                    "fonts/Lato-Regular.ttf", "fonts/Lato-Bold.ttf"));
            options.add(new FontOption(ClockPreferences.FONT_LORA, "Lora",
                    "fonts/Lora-Regular.ttf", "fonts/Lora-Bold.ttf"));
            options.add(new FontOption(ClockPreferences.FONT_NOTO_SANS, "Noto Sans",
                    "fonts/NotoSans-Regular.ttf", "fonts/NotoSans-Bold.ttf"));
            options.add(new FontOption(ClockPreferences.FONT_BITCOUNT, "Bitcount Grid Double",
                    "fonts/BitcountGridDouble-Regular.ttf", "fonts/BitcountGridDouble-Bold.ttf"));
        }
        return Collections.unmodifiableList(options);
    }

    /** Ordered list of the font families available in the current flavor. */
    public static List<FontOption> options() {
        return OPTIONS;
    }

    /** Returns the option for {@code id}, or the system option when unknown. */
    public static FontOption optionFor(String id) {
        for (FontOption option : OPTIONS) {
            if (option.id.equals(id)) {
                return option;
            }
        }
        return OPTIONS.get(0);
    }

    /** Position of {@code id} in {@link #options()}, or 0 (system) when unknown. */
    public static int indexOf(String id) {
        for (int i = 0; i < OPTIONS.size(); i++) {
            if (OPTIONS.get(i).id.equals(id)) {
                return i;
            }
        }
        return 0;
    }

    /** Font id at {@code index}, clamped to the valid range. */
    public static String idForIndex(int index) {
        if (index < 0 || index >= OPTIONS.size()) {
            return ClockPreferences.FONT_SYSTEM;
        }
        return OPTIONS.get(index).id;
    }

    /** {@code true} when {@code id} is a family available in the current flavor. */
    public static boolean isAvailable(String id) {
        for (FontOption option : OPTIONS) {
            if (option.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Display names in catalog order, for populating a spinner adapter. */
    public static String[] displayNames() {
        String[] names = new String[OPTIONS.size()];
        for (int i = 0; i < OPTIONS.size(); i++) {
            names[i] = OPTIONS.get(i).displayName;
        }
        return names;
    }
}
