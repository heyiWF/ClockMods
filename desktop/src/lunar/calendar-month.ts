/**
 * The six-week (42 cell) month grid backing the calendar page.
 *
 * Ported from com.clockmods.calendar.CalendarMonth. Weekday arithmetic runs in
 * UTC because a Gregorian date's weekday is zone-independent; only "today" is
 * resolved through the app's selected zone.
 */
import { dayOfWeekFor } from '../core/zoned-time';
import type { ZonedFields } from '../core/zoned-time';
import { CALENDAR_WEEK_START_SUNDAY } from '../core/prefs';

export const CELL_COUNT = 42;

export interface CalendarDay {
  year: number;
  /** Zero-based. */
  month0: number;
  dayOfMonth: number;
  /** 1 = Sunday … 7 = Saturday, matching java.util.Calendar.DAY_OF_WEEK. */
  dayOfWeek: number;
  currentMonth: boolean;
  today: boolean;
}

export interface CalendarMonth {
  year: number;
  month0: number;
  days: CalendarDay[];
}

/**
 * @param firstDayOfWeek 1 = Sunday, 2 = Monday (Calendar constants)
 * @param today the current date in the app's time zone, used for the highlight
 */
export function createCalendarMonth(
  year: number,
  month0: number,
  today: Pick<ZonedFields, 'year' | 'month0' | 'day'>,
  firstDayOfWeek: number = CALENDAR_WEEK_START_SUNDAY
): CalendarMonth {
  const normalizedFirstDay = firstDayOfWeek >= 1 && firstDayOfWeek <= 7 ? firstDayOfWeek : 1;
  const firstWeekday = dayOfWeekFor(year, month0, 1) + 1; // 1 = Sunday
  const leadingDays = (firstWeekday - normalizedFirstDay + 7) % 7;

  const days: CalendarDay[] = [];
  const cursor = new Date(Date.UTC(year, month0, 1 - leadingDays));
  for (let index = 0; index < CELL_COUNT; index++) {
    const cellYear = cursor.getUTCFullYear();
    const cellMonth = cursor.getUTCMonth();
    const cellDay = cursor.getUTCDate();
    days.push({
      year: cellYear,
      month0: cellMonth,
      dayOfMonth: cellDay,
      dayOfWeek: cursor.getUTCDay() + 1,
      currentMonth: cellYear === year && cellMonth === month0,
      today: cellYear === today.year && cellMonth === today.month0 && cellDay === today.day,
    });
    cursor.setUTCDate(cellDay + 1);
  }
  return { year, month0, days };
}

/** 1 = Sunday, 7 = Saturday. */
export function isWeekend(dayOfWeek: number): boolean {
  return dayOfWeek === 1 || dayOfWeek === 7;
}
