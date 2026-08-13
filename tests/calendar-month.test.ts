/** Ported from app/src/test/java/com/clockmods/calendar/CalendarMonthTest.java. */
import { describe, expect, it } from 'vitest';
import { CELL_COUNT, createCalendarMonth, isWeekend } from '../src/lunar/calendar-month';
import { CALENDAR_WEEK_START_MONDAY } from '../src/core/prefs';

// 2026-07-16, the date the Android test pinned as "today".
const today = { year: 2026, month0: 6, day: 16 };

describe('createCalendarMonth', () => {
  it('creates a six-week grid and highlights today', () => {
    const month = createCalendarMonth(2026, 6, today);

    expect(month.days).toHaveLength(CELL_COUNT);
    expect(month.days[0].dayOfMonth).toBe(28);
    expect(month.days[0].dayOfWeek).toBe(1); // Sunday
    expect(month.days[0].currentMonth).toBe(false);

    const highlighted = month.days[18];
    expect(highlighted.dayOfMonth).toBe(16);
    expect(highlighted.today).toBe(true);
  });

  it('creates a Monday-first grid when requested', () => {
    const month = createCalendarMonth(2026, 6, today, CALENDAR_WEEK_START_MONDAY);

    expect(month.days).toHaveLength(CELL_COUNT);
    expect(month.days[0].dayOfMonth).toBe(29);
    expect(month.days[0].dayOfWeek).toBe(2); // Monday
    expect(month.days[0].currentMonth).toBe(false);

    const highlighted = month.days[17];
    expect(highlighted.dayOfMonth).toBe(16);
    expect(highlighted.today).toBe(true);
  });

  it('marks exactly the days of the requested month as current', () => {
    const month = createCalendarMonth(2026, 6, today);
    expect(month.days.filter((day) => day.currentMonth)).toHaveLength(31);
  });

  it('rolls over year boundaries', () => {
    const january = createCalendarMonth(2026, 0, today);
    expect(january.days[0].year).toBe(2025);
    expect(january.days[0].month0).toBe(11);

    const december = createCalendarMonth(2026, 11, today);
    const last = december.days[CELL_COUNT - 1];
    expect(last.year).toBe(2027);
    expect(last.month0).toBe(0);
  });

  it('handles a leap February', () => {
    const february = createCalendarMonth(2028, 1, today);
    expect(february.days.filter((day) => day.currentMonth)).toHaveLength(29);
  });
});

describe('isWeekend', () => {
  it('covers Sunday and Saturday', () => {
    expect(isWeekend(1)).toBe(true);
    expect(isWeekend(7)).toBe(true);
    expect(isWeekend(4)).toBe(false);
  });
});
