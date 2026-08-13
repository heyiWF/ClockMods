/**
 * Covers the Intl-based replacement for the java.util.Calendar/TimeZone handling
 * the Android code relied on.
 */
import { describe, expect, it } from 'vitest';
import {
  addDays,
  addMonths,
  dateKey,
  dayOfWeekFor,
  daysInMonth,
  minutesOfDay,
  zonedFields,
} from '../src/core/zoned-time';

describe('zonedFields', () => {
  it('renders an instant in the requested zone', () => {
    // 2026-08-07T04:30:00Z = 12:30 in Shanghai, 00:30 in New York.
    const instant = Date.UTC(2026, 7, 7, 4, 30, 0);
    const shanghai = zonedFields(instant, 'Asia/Shanghai');
    expect(shanghai.year).toBe(2026);
    expect(shanghai.month0).toBe(7);
    expect(shanghai.day).toBe(7);
    expect(shanghai.hour).toBe(12);
    expect(shanghai.minute).toBe(30);
    expect(shanghai.dayOfWeek0).toBe(5); // Friday

    const newYork = zonedFields(instant, 'America/New_York');
    expect(newYork.day).toBe(7);
    expect(newYork.hour).toBe(0);
  });

  it('crosses the date line correctly', () => {
    const instant = Date.UTC(2026, 7, 6, 20, 0, 0);
    expect(zonedFields(instant, 'Asia/Shanghai').day).toBe(7);
    expect(zonedFields(instant, 'America/Los_Angeles').day).toBe(6);
  });

  it('applies daylight saving time', () => {
    // New York is UTC-4 in July and UTC-5 in January.
    expect(zonedFields(Date.UTC(2026, 6, 1, 16, 0), 'America/New_York').hour).toBe(12);
    expect(zonedFields(Date.UTC(2026, 0, 1, 17, 0), 'America/New_York').hour).toBe(12);
  });

  it('reports midnight as hour 0', () => {
    expect(zonedFields(Date.UTC(2026, 7, 7, 16, 0), 'Asia/Shanghai').hour).toBe(0);
  });

  it('falls back to device time for an unknown zone', () => {
    expect(() => zonedFields(Date.now(), 'Not/AZone')).not.toThrow();
  });
});

describe('calendar arithmetic', () => {
  it('computes minutes since midnight', () => {
    expect(
      minutesOfDay({ year: 2026, month0: 7, day: 7, hour: 22, minute: 5, second: 0, dayOfWeek0: 5 })
    ).toBe(22 * 60 + 5);
  });

  it('computes weekdays independent of zone', () => {
    expect(dayOfWeekFor(2026, 7, 7)).toBe(5); // Friday
    expect(dayOfWeekFor(2026, 6, 16)).toBe(4); // Thursday
  });

  it('reports days in month including leap years', () => {
    expect(daysInMonth(2026, 1)).toBe(28);
    expect(daysInMonth(2028, 1)).toBe(29);
    expect(daysInMonth(2026, 6)).toBe(31);
    expect(daysInMonth(2026, 8)).toBe(30);
  });

  it('formats the yyyy-MM-dd key', () => {
    expect(dateKey(2026, 7, 7)).toBe('2026-08-07');
    expect(dateKey(2026, 0, 1)).toBe('2026-01-01');
  });

  it('adds days across month and year boundaries', () => {
    expect(addDays(2026, 11, 31, 1)).toEqual({ year: 2027, month0: 0, day: 1 });
    expect(addDays(2026, 0, 1, -1)).toEqual({ year: 2025, month0: 11, day: 31 });
  });

  it('adds months and clamps the day', () => {
    expect(addMonths(2026, 0, 31, 1)).toEqual({ year: 2026, month0: 1, day: 28 });
    expect(addMonths(2026, 11, 15, 1)).toEqual({ year: 2027, month0: 0, day: 15 });
    expect(addMonths(2026, 0, 15, -1)).toEqual({ year: 2025, month0: 11, day: 15 });
  });
});
