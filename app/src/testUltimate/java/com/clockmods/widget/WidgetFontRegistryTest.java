package com.clockmods.widget;

import com.clockmods.background.FontCatalog;
import com.clockmods.widget.render.WidgetFontRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetFontRegistryTest {
    @Test public void columnsFollowStableIdsRatherThanCatalogPositions() {
        String[] columns={"roboto", "google_sans_display", "google_sans_text", "sf_pro_display",
                "sf_pro_rounded", "inter", "lato", "lora", "noto_sans", "bitcount_grid_double"};
        assertEquals(13,WidgetFontRegistry.COLUMNS);
        for(int i=0;i<columns.length;i++) assertEquals(i+3,WidgetFontRegistry.columnOf(columns[i],1));
        assertEquals(0,WidgetFontRegistry.columnOf("system",2));
        assertEquals(2,WidgetFontRegistry.columnOf("theme",2));
        assertEquals(1,WidgetFontRegistry.columnOf("unavailable-font",1));
        assertEquals(1,WidgetFontRegistry.columnOf(null,1));
    }

    @Test public void everyCatalogFontHasARendererAndIdsCannotBeMutated() {
        for(FontCatalog.FontOption option:FontCatalog.options()) {
            assertTrue(WidgetFontRegistry.isKnown(option.id));
            assertNotEquals(1,WidgetFontRegistry.columnOf(option.id,1));
            assertEquals(option.id,WidgetFontRegistry.idAt(WidgetFontRegistry.indexOf(option.id)));
        }
        try { WidgetFontRegistry.ids().set(0,"corrupted"); fail("immutable list"); }
        catch(UnsupportedOperationException expected) { }
        assertEquals("theme",WidgetFontRegistry.idAt(-1));
        assertEquals("theme",WidgetFontRegistry.idAt(100));
    }
}
