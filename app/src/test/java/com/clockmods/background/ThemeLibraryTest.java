package com.clockmods.background;
import com.clockmods.testing.MemorySharedPreferences;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ThemeLibraryTest {
    @Test public void deletionReinstallationAndSelectionSurviveRecreation() {
        MemorySharedPreferences store = new MemorySharedPreferences();
        MemorySharedPreferences fonts = new MemorySharedPreferences();
        List<String> ids = Arrays.asList("glass.atelier", "ultimate.orbit", "pro.classic");
        ThemeLibrary library = new ThemeLibrary(store, fonts, false, ids);
        assertEquals(ids, library.installed());
        assertEquals("glass.atelier", library.selected());
        library.select("ultimate.orbit");
        library.favorite("ultimate.orbit");
        fonts.edit().putInt("font_weight__ultimate.orbit", 900)
            .putString("script_font__ultimate.orbit__zh__weight", "900")
            .putInt("font_weight__glass.atelier", 500).apply();
        store.edit().putInt("palette_accent__ultimate.orbit", 123)
            .putBoolean("digit_animation__ultimate.orbit", false)
            .putInt("palette_accent__glass.atelier", 456).apply();
        assertTrue(library.remove("ultimate.orbit"));
        assertEquals("pro.classic", library.selected());
        assertFalse(fonts.contains("font_weight__ultimate.orbit"));
        assertFalse(fonts.contains("script_font__ultimate.orbit__zh__weight"));
        assertFalse(store.contains("palette_accent__ultimate.orbit"));
        assertFalse(store.contains("digit_animation__ultimate.orbit"));
        assertEquals(500, fonts.getInt("font_weight__glass.atelier", 0));
        assertEquals(456, store.getInt("palette_accent__glass.atelier", 0));
        library = new ThemeLibrary(store, fonts, false, ids);
        assertFalse(library.isInstalled("ultimate.orbit"));
        assertTrue(library.isFavorite("ultimate.orbit"));
        library.install("ultimate.orbit");
        assertEquals(Arrays.asList("glass.atelier", "pro.classic", "ultimate.orbit"), library.installed());
        assertFalse(store.contains("palette_accent__ultimate.orbit"));
        assertEquals("pro.classic", library.selected());
    }
    @Test public void lastThemeCannotBeDeletedAndResetCannotResurrectRemovedSelection() {
        MemorySharedPreferences store = new MemorySharedPreferences();
        ThemeLibrary library = new ThemeLibrary(store, store, true, Arrays.asList("calendar.graphite", "calendar.paper"));
        store.edit().putString("calendar_theme", "calendar.graphite")
            .putFloat("time_font_scale__calendar:calendar.graphite", 1.5f)
            .putFloat("calendar_marquee__calendar:calendar.graphite_speed", 23f)
            .putFloat("time_font_scale__calendar:calendar.paper", 2f).apply();
        assertTrue(library.remove("calendar.graphite"));
        assertFalse(store.contains("calendar_marquee__calendar:calendar.graphite_speed"));
        assertFalse(store.contains("time_font_scale__calendar:calendar.graphite"));
        assertEquals("calendar.paper", library.selected());
        Map<String, ?> before = store.getAll();
        assertFalse(library.remove("calendar.paper"));
        assertEquals(before, store.getAll());
        assertEquals("calendar.paper", ThemeLibrary.resolveSelection(store, "calendar.graphite"));
    }
    @Test public void orderFavoritesAndCatalogRemainIndependent() {
        MemorySharedPreferences store = new MemorySharedPreferences();
        ThemeLibrary library = new ThemeLibrary(store, store, false, Arrays.asList("a", "b", "c"));
        library.favorite("b"); library.move("c", -1);
        assertEquals(Arrays.asList("a", "c", "b"), library.installed());
        library.remove("b");
        assertEquals(Arrays.asList("a", "b", "c"), library.catalog());
        assertTrue(library.isFavorite("b"));
        library.favorite("b"); assertFalse(library.isFavorite("b"));
        library.install("b"); library.install("b");
        assertEquals(Arrays.asList("a", "c", "b"), library.installed());
        try { library.select("unknown"); fail(); } catch (IllegalArgumentException expected) { }
        try { library.install("unknown"); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void legacyMotionMigratesAndReinstallationCannotReuseIt() {
        MemorySharedPreferences store = new MemorySharedPreferences();
        MemorySharedPreferences fonts = new MemorySharedPreferences();
        store.edit().putString("second_motion", "off").apply();
        ThemeLibrary library = new ThemeLibrary(store, fonts, false,
                Arrays.asList("glass.atelier", "ultimate.orbit"));
        assertFalse(store.contains("second_motion"));
        assertEquals("off", store.getString("second_motion__glass.atelier", null));
        assertEquals("off", store.getString("second_motion__ultimate.orbit", null));
        library.remove("glass.atelier");
        fonts.edit().putInt("font_weight", 900).putFloat("time_font_scale", 2f).apply();
        library.install("glass.atelier");
        assertFalse(store.contains("second_motion__glass.atelier"));
        assertEquals("off", store.getString("second_motion__ultimate.orbit", null));
        assertEquals(400, fonts.getInt("font_weight__glass.atelier", 0));
        assertEquals(0f, fonts.getFloat("time_font_scale__glass.atelier", -1f), 0f);
    }
    @Test public void exactScopeBoundariesProtectOtherThemesAndGlobalSettings() {
        assertTrue(ThemeLibrary.isTypographyKey("script_font__a__ja__weight", "a"));
        assertFalse(ThemeLibrary.isTypographyKey("font_weight__ab", "a"));
        assertFalse(ThemeLibrary.isTypographyKey("script_font__ab__ja", "a"));
        assertFalse(ThemeLibrary.isTypographyKey("font_weight", "a"));
    }
}
