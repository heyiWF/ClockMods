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
        assertEquals(9f, CalendarDashboardSizing.monthLunarSize(500f, 50f, 1f), 0f);
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
