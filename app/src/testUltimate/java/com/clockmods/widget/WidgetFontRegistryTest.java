package com.clockmods.widget;

import com.clockmods.widget.render.WidgetFontRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetFontRegistryTest {
    @Test public void columnsFollowStableIdsRatherThanCatalogPositions() {
        String[] columns={"system", "serif", "monospace", "condensed", "light"};
        assertEquals(5,WidgetFontRegistry.COLUMNS);
        for(int i=0;i<columns.length;i++) assertEquals(i,WidgetFontRegistry.columnOf(columns[i],1));
        assertEquals(0,WidgetFontRegistry.columnOf("system",2));
        assertEquals(2,WidgetFontRegistry.columnOf("theme",2));
        assertEquals(1,WidgetFontRegistry.columnOf("unavailable-font",1));
        assertEquals(1,WidgetFontRegistry.columnOf(null,1));
    }

    @Test public void everyHostFontHasARendererAndIdsCannotBeMutated() {
        for(String id:WidgetFontRegistry.ids()) {
            assertTrue(WidgetFontRegistry.isKnown(id));
            assertEquals(id,WidgetFontRegistry.idAt(WidgetFontRegistry.indexOf(id)));
        }
        assertFalse(WidgetFontRegistry.isKnown("bitcount_grid_double"));
        try { WidgetFontRegistry.ids().set(0,"corrupted"); fail("immutable list"); }
        catch(UnsupportedOperationException expected) { }
        assertEquals("theme",WidgetFontRegistry.idAt(-1));
        assertEquals("theme",WidgetFontRegistry.idAt(100));
    }

    @Test public void oldFontChoicesMigrateWithoutLosingOtherSettings() {
        com.clockmods.widget.model.WidgetConfig original=com.clockmods.widget.model.WidgetConfig.builder(9,
                com.clockmods.widget.model.WidgetKind.DIGITAL).themeId("paper.warm").textScale(1.15f).build();
        assertEquals("serif",original.toBuilder().fontId("lora").build().fontId);
        assertEquals("monospace",original.toBuilder().fontId("bitcount_grid_double").build().fontId);
        String raw=com.clockmods.widget.store.WidgetConfigStore.encode(original).replace("\"fontId\":\"theme\"","\"fontId\":\"sf_pro_display\"");
        com.clockmods.widget.model.WidgetConfig migrated=com.clockmods.widget.store.WidgetConfigStore.decode(raw,9,original.kind);
        assertEquals("system",migrated.fontId);
        assertEquals(original.themeId,migrated.themeId);
        assertEquals(original.textScale,migrated.textScale,0f);
    }
}
