/**
 * Ported from app/src/testPro/java/com/clockmods/pro/LunarAlmanacRangeTest.java,
 * plus the output-format expectations that used to live in LunarCalendarTest.
 */
import { describe, expect, it } from 'vitest';
import { almanacOf, lunarClockLine } from '../src/lunar/lunar';
import { createCalendarMonth } from '../src/lunar/calendar-month';
import { CALENDAR_WEEK_START_MONDAY } from '../src/core/prefs';

describe('almanacOf', () => {
  it('renders the clock line in the historical bracketed form', () => {
    // 2026-08-07 is 丙午(马)年六月廿五.
    expect(lunarClockLine(2026, 7, 7)).toBe('丙午[马]年六月廿五');
  });

  it('renders the calendar footer in the natural form', () => {
    expect(almanacOf(2026, 7, 7).natural).toBe('丙午马年六月廿五');
  });

  it('labels a lunar month start with the month name and other days with the day', () => {
    expect(almanacOf(2026, 1, 17).shortLabel).toBe('正月'); // 2026-02-17 = 正月初一
    expect(almanacOf(2026, 7, 7).shortLabel).toBe('廿五');
  });

  it('uses the same month names as the legacy table, including 冬 and 腊', () => {
    expect(almanacOf(2026, 11, 20).natural).toContain('冬月');
    expect(almanacOf(2026, 0, 20).natural).toContain('腊月');
  });

  it('prefixes leap months with 闰', () => {
    // 2025-07-25 is the first day of 闰六月.
    expect(almanacOf(2025, 6, 25).shortLabel).toBe('闰六月');
  });

  it('reports the solar term, festivals and 数九/三伏 starts', () => {
    expect(almanacOf(2026, 7, 7).festivals).toContain('立秋');
    expect(almanacOf(2026, 1, 17).festivals).toContain('春节');
    // Minor festivals only appear when explicitly requested.
    expect(almanacOf(2026, 7, 7, false).festivals).not.toContain('五谷母节');
    expect(almanacOf(2026, 7, 7, true).festivals).toContain('五谷母节');
  });

  it('reports 宜 and 忌 for a day', () => {
    const almanac = almanacOf(2026, 7, 7);
    expect(almanac.suitable.length).toBeGreaterThan(0);
    expect(almanac.avoid.length).toBeGreaterThan(0);
  });

  it('supports dates far outside the legacy 1900-2050 table', () => {
    for (const [year, month0] of [
      [1800, 0],
      [1900, 11],
      [2100, 0],
      [2200, 11],
    ] as const) {
      const month = createCalendarMonth(
        year,
        month0,
        { year, month0, day: 1 },
        CALENDAR_WEEK_START_MONDAY
      );
      for (const day of month.days) {
        const almanac = almanacOf(day.year, day.month0, day.dayOfMonth, true);
        expect(almanac.shortLabel.length, `${day.year}-${day.month0 + 1}-${day.dayOfMonth}`).toBeGreaterThan(0);
        expect(Array.isArray(almanac.festivals)).toBe(true);
      }
      const first = almanacOf(year, month0, 1, true);
      expect(first.natural.length).toBeGreaterThan(0);
      expect(first.bracketed.length).toBeGreaterThan(0);
      expect(Array.isArray(first.suitable)).toBe(true);
      expect(Array.isArray(first.avoid)).toBe(true);
    }
  });
});
