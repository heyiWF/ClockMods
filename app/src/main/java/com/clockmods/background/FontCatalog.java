package com.clockmods.background;

import android.content.Context;

import com.clockmods.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central, data-driven registry of the clock font families.
 *
 * <p>All bundled Ultimate families are registered here so settings and rendering share the same
 * stable IDs and asset paths.
 *
 * <p>Each family also declares which weights it can actually render, because that differs per
 * family and the settings slider shows exactly those stops rather than a uniform nine. A family
 * backed by a variable font covers its {@code wght} axis in steps of 100; a family shipped as
 * separate static files covers only the weights those files exist for.
 */
public final class FontCatalog {
    /** Every weight stop the app knows about, ascending. */
    public static final int[] WEIGHT_STOPS = {100, 200, 300, 400, 500, 600, 700, 800, 900};
    /** Regular — the weight a family renders at when the user has not chosen one. */
    public static final int DEFAULT_WEIGHT = 400;

    /** A single selectable font family. */
    public static final class FontOption {
        public final String id;
        public final String displayName;
        /**
         * Asset path of a variable font carrying a {@code wght} axis, or {@code null} when this
         * family is either the system font or a set of static files.
         */
        public final String variableAsset;
        /**
         * Non-weight axis settings pinned alongside {@code wght} for {@link #variableAsset}, in
         * {@code android.graphics.Paint#setFontVariationSettings} syntax, or {@code null}.
         */
        public final String variableAxes;
        /** The weights this family can render, ascending. Never empty. */
        private final int[] weights;
        /** Asset per entry of {@link #weights} for a static family; {@code null} otherwise. */
        private final String[] staticAssets;

        private FontOption(String id, String displayName, String variableAsset, String variableAxes,
                int[] weights, String[] staticAssets) {
            this.id = id;
            this.displayName = displayName;
            this.variableAsset = variableAsset;
            this.variableAxes = variableAxes;
            this.weights = weights;
            this.staticAssets = staticAssets;
        }

        /** The system font: no asset at all, rendered through the {@code sans-serif-*} families. */
        static FontOption system(String id, String displayName) {
            return new FontOption(id, displayName, null, null,
                    new int[] {100, 300, 400, 500, 900}, null);
        }

        /**
         * A family backed by one variable font. {@code minWeight}/{@code maxWeight} clamp the
         * declared stops to the file's real {@code wght} axis, so a family whose axis stops at 700
         * (Lora) never offers a stop the file cannot render.
         */
        static FontOption variable(String id, String displayName, String asset, String axes,
                int minWeight, int maxWeight) {
            List<Integer> stops = new ArrayList<>();
            for (int stop : WEIGHT_STOPS) {
                if (stop >= minWeight && stop <= maxWeight) stops.add(stop);
            }
            int[] weights = new int[stops.size()];
            for (int i = 0; i < weights.length; i++) weights[i] = stops.get(i);
            return new FontOption(id, displayName, asset, axes, weights, null);
        }

        /** A family shipped as one file per weight. Arrays are parallel and must be ascending. */
        static FontOption statics(String id, String displayName, int[] weights, String[] assets) {
            return new FontOption(id, displayName, null, null, weights, assets);
        }

        public boolean isSystem() {
            return variableAsset == null && staticAssets == null;
        }

        /** {@code true} when this family renders weights from a {@code wght} axis. */
        public boolean isVariable() {
            return variableAsset != null;
        }

        /** The weights this family can render, ascending. The array is a copy. */
        public int[] availableWeights() {
            return weights.clone();
        }

        /** How many stops the weight slider should offer for this family. */
        public int weightCount() {
            return weights.length;
        }

        /** The weight at {@code index}, clamped to the available range. */
        public int weightAt(int index) {
            if (index <= 0) return weights[0];
            if (index >= weights.length) return weights[weights.length - 1];
            return weights[index];
        }

        /**
         * The "next heavier" stop for a hierarchy tier, given the base the user picked. Two tiers
         * (masthead vs. body, say) need contrast, so the emphasised tier takes the nearest stop at
         * least 200 above the base that this family can render. A base already at the top of the
         * axis stays there.
         */
        public int emphasizedWeight(int baseWeight) {
            int baseAt = weightAt(indexOfNearestWeight(baseWeight));
            for (int stop : weights) {
                if (stop >= baseAt + 200) return stop;
            }
            return weights[weights.length - 1];
        }

        /** Index of the stop nearest {@code weight}, for positioning the slider. */
        public int indexOfNearestWeight(int weight) {
            int best = 0;
            int bestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < weights.length; i++) {
                int distance = Math.abs(weights[i] - weight);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = i;
                }
            }
            return best;
        }

        /** The nearest weight this family can actually render. */
        public int nearestWeight(int weight) {
            return weights[indexOfNearestWeight(weight)];
        }

        /** Asset for the static file nearest {@code weight}, or {@code null} when not static. */
        public String staticAssetFor(int weight) {
            if (staticAssets == null) return null;
            return staticAssets[indexOfNearestWeight(weight)];
        }
    }

    private static final List<FontOption> OPTIONS = buildOptions();

    private FontCatalog() {
    }

    private static List<FontOption> buildOptions() {
        List<FontOption> options = new ArrayList<>();
        options.add(FontOption.system(ClockPreferences.FONT_SYSTEM, "系统字体"));
        options.add(FontOption.variable(ClockPreferences.FONT_ROBOTO, "Roboto",
                "fonts/Roboto-Variable.ttf", null, 100, 900));
        options.add(FontOption.statics(ClockPreferences.FONT_GOOGLE_SANS_DISPLAY,
                "Google Sans Display", new int[] {400, 500, 700}, new String[] {
                        "fonts/GoogleSansDisplay-Regular.ttf",
                        "fonts/GoogleSansDisplay-Medium.ttf",
                        "fonts/GoogleSansDisplay-Bold.ttf"}));
        options.add(FontOption.statics(ClockPreferences.FONT_GOOGLE_SANS_TEXT, "Google Sans Text",
                new int[] {400, 500, 700}, new String[] {
                        "fonts/GoogleSansText-Regular.ttf",
                        "fonts/GoogleSansText-Medium.ttf",
                        "fonts/GoogleSansText-Bold.ttf"}));
        // SF Pro ships one variable file spanning Text and Display; pinning the optical-size axis
        // to its maximum is what makes it the Display cut rather than the tighter Text one.
        options.add(FontOption.variable(ClockPreferences.FONT_SF_PRO_DISPLAY, "SF Pro Display",
                "fonts/SFPro-Variable.ttf", "'opsz' 28", 100, 900));
        options.add(FontOption.statics(ClockPreferences.FONT_SF_PRO_ROUNDED, "SF Pro Rounded",
                new int[] {300, 400, 500, 600, 700}, new String[] {
                        "fonts/SFProRounded-Light.otf",
                        "fonts/SFProRounded-Regular.otf",
                        "fonts/SFProRounded-Medium.otf",
                        "fonts/SFProRounded-Semibold.otf",
                        "fonts/SFProRounded-Bold.otf"}));
        options.add(FontOption.variable(ClockPreferences.FONT_INTER, "Inter",
                "fonts/Inter-Variable.ttf", null, 100, 900));
        options.add(FontOption.statics(ClockPreferences.FONT_LATO, "Lato",
                new int[] {100, 300, 400, 700, 900}, new String[] {
                        "fonts/Lato-Thin.ttf",
                        "fonts/Lato-Light.ttf",
                        "fonts/Lato-Regular.ttf",
                        "fonts/Lato-Bold.ttf",
                        "fonts/Lato-Black.ttf"}));
        // Lora's axis genuinely stops at 700, so it offers four stops where Roboto offers nine.
        options.add(FontOption.variable(ClockPreferences.FONT_LORA, "Lora",
                "fonts/Lora-Variable.ttf", null, 400, 700));
        options.add(FontOption.variable(ClockPreferences.FONT_NOTO_SANS, "Noto Sans",
                "fonts/NotoSans-Variable.ttf", null, 100, 900));
        options.add(FontOption.variable(ClockPreferences.FONT_BITCOUNT, "Bitcount Grid Double",
                "fonts/BitcountGridDouble-Variable.ttf", null, 100, 900));
        return Collections.unmodifiableList(options);
    }

    /** Ordered list of the font families available in ClockMods Ultimate. */
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

    /** {@code true} when {@code id} is a bundled family. */
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

    /**
     * Display names in catalog order, with the system font's name localized from resources
     * (the other families are brand names shown verbatim in every language).
     */
    public static String[] displayNames(Context context) {
        String[] names = displayNames();
        for (int i = 0; i < OPTIONS.size(); i++) {
            if (OPTIONS.get(i).isSystem()) {
                names[i] = context.getString(R.string.font_system);
            }
        }
        return names;
    }
}
