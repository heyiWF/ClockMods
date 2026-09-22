package com.clockmods.pro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CalendarDashboardSizingTest {
    @Test
    public void cardContentGrowsWithCardSize() {
        assertTrue(CalendarDashboardSizing.clockTimeSize(800f, 300f, true, 1f)
                > CalendarDashboardSizing.clockTimeSize(400f, 150f, true, 1f));
        assertTrue(CalendarDashboardSizing.weatherIconSize(800f, 300f, 1f)
                > CalendarDashboardSizing.weatherIconSize(400f, 150f, 1f));
        assertTrue(CalendarDashboardSizing.forecastIconSize(240f, 300f, 1f)
                > CalendarDashboardSizing.forecastIconSize(120f, 150f, 1f));
    }

    @Test
    public void narrowOrShortCardsUseTheConstrainingDimension() {
        assertEquals(50f, CalendarDashboardSizing.clockTimeSize(200f, 1000f, true, 1f), 0f);
        assertEquals(52f, CalendarDashboardSizing.clockTimeSize(1000f, 100f, true, 1f), 0f);
        assertEquals(18f, CalendarDashboardSizing.monthLunarSize(90f, 500f, 1f), 0f);
        // Height-bound: 50 * 0.20. The lunar label rides the cell height, so this is the
        // number that moves when the label is made more legible.
        assertEquals(10f, CalendarDashboardSizing.monthLunarSize(500f, 50f, 1f), 0f);
    }

    @Test
    public void agendaStripLabelsRideTheCellHeightUntilTheCap() {
        // Height-bound: a short cell hands the label 0.22 of its height.
        assertEquals(11f, CalendarDashboardSizing.agendaStripLunarSize(400f, 50f, 1f), 0f);
        // Width-bound only when a cell is far narrower than it is tall.
        assertEquals(12f, CalendarDashboardSizing.agendaStripLunarSize(40f, 400f, 1f), 0f);
        // The 17dp cap is what stops a tall cell from turning the label into a heading — which
        // is where both real orientations land, so it is the cap the user actually sees.
        assertEquals(17f, CalendarDashboardSizing.agendaStripLunarSize(400f, 100f, 1f), 0f);
        assertEquals(25.5f,
                CalendarDashboardSizing.agendaStripLunarSize(1000f, 1000f, 1.5f), 0f);
    }

    @Test
    public void largeSurfacesUseDensityAwareCaps() {
        assertEquals(216f,
                CalendarDashboardSizing.clockTimeSize(4000f, 2000f, true, 1.5f), 0f);
        assertEquals(54f,
                CalendarDashboardSizing.monthDaySize(1000f, 1000f, 1.5f), 0f);
        assertEquals(27f,
                CalendarDashboardSizing.monthLunarSize(1000f, 1000f, 1.5f), 0f);
    }

    @Test
    public void monthChromeKeepsTouchTargetsWithoutConsumingShortPanels() {
        assertEquals(72f, CalendarDashboardSizing.monthToolbarHeight(500f, 1.5f), 0f);
        assertEquals(90f, CalendarDashboardSizing.monthToolbarHeight(500f, 3f), 0f);
        assertEquals(36f, CalendarDashboardSizing.monthWeekdayHeight(500f, 1.5f), 0f);
        assertEquals(42f, CalendarDashboardSizing.monthFooterHeight(500f, 1.5f), 0f);
    }
}
