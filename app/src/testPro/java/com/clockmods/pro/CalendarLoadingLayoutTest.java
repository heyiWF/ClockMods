package com.clockmods.pro;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CalendarLoadingLayoutTest {
    @Test
    public void everyDashboardLayoutProvidesRequiredLoadingIndicators() throws Exception {
        File resources = new File("src/pro/res");
        File[] layouts = resources.listFiles(file -> file.isDirectory()
                && file.getName().startsWith("layout"));
        assertNotNull("Pro layout resources must be available", layouts);
        int checked = 0;
        for (File directory : layouts) {
            File layout = new File(directory, "calendar_dashboard_left.xml");
            if (!layout.isFile()) continue;
            checked++;
            NodeList elements = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(layout).getElementsByTagName("*");
            assertIndicator(elements, "calendar_weather_loading", "calendar_current_weather_card", layout);
            assertIndicator(elements, "calendar_forecast_loading", "calendar_forecast_card", layout);
        }
        assertTrue("Must check default, portrait and landscape layouts", checked >= 3);
    }

    private static void assertIndicator(NodeList elements, String id, String parentId, File layout) {
        Element indicator = null;
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (("@+id/" + id).equals(element.getAttribute("android:id"))) {
                indicator = element;
                break;
            }
        }
        assertNotNull(layout + " is missing " + id, indicator);
        assertEquals("com.google.android.material.loadingindicator.LoadingIndicator",
                indicator.getTagName());
        assertEquals("gone", indicator.getAttribute("android:visibility"));
        assertEquals("@+id/" + parentId,
                ((Element) indicator.getParentNode()).getAttribute("android:id"));
    }
}
