/**
 * Time-zone aware calendar fields.
 *
 * The Android code passes a `java.util.Calendar` created for the selected zone
 * everywhere. The browser has no such type, so the whole app works on plain
 * field records extracted through `Intl.DateTimeFormat`, which is the only
 * IANA-zone-aware primitive available. `Intl` also removes the need for the
 * curated zone-offset handling Android needed — DST is applied automatically.
 */

export interface ZonedFields {
  year: number;
  /** Zero-based, matching java.util.Calendar.MONTH. */
  month0: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
  /** 0 = Sunday, matching Calendar.DAY_OF_WEEK - Calendar.SUNDAY. */
  dayOfWeek0: number;
}

const WEEKDAYS: Record<string, number> = {
  Sun: 0,
  Mon: 1,
  Tue: 2,
  Wed: 3,
  Thu: 4,
  Fri: 5,
  Sat: 6,
};

const formatters = new Map<string, Intl.DateTimeFormat>();

/** `Intl.DateTimeFormat` construction is expensive; one instance per zone. */
function formatterFor(timeZone: string): Intl.DateTimeFormat {
  let formatter = formatters.get(timeZone);
  if (!formatter) {
    formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: timeZone || undefined,
      hourCycle: 'h23',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      weekday: 'short',
    });
    formatters.set(timeZone, formatter);
  }
  return formatter;
}

/**
 * @param timeZone IANA id, or "" to follow the device zone (matching
 *   ClockPreferences.TIME_ZONE_FOLLOW_SYSTEM).
 */
export function zonedFields(instant: Date | number, timeZone = ''): ZonedFields {
  const date = typeof instant === 'number' ? new Date(instant) : instant;
  let parts: Intl.DateTimeFormatPart[];
  try {
    parts = formatterFor(timeZone).formatToParts(date);
  } catch {
    // An unknown zone id (e.g. stale preference) falls back to device time
    // rather than breaking the clock.
    formatters.delete(timeZone);
    parts = formatterFor('').formatToParts(date);
  }
  const lookup: Record<string, string> = {};
  for (const part of parts) lookup[part.type] = part.value;
  return {
    year: Number(lookup.year),
    month0: Number(lookup.month) - 1,
    day: Number(lookup.day),
    // h23 yields 00-23, but some engines emit "24" at midnight.
    hour: Number(lookup.hour) % 24,
    minute: Number(lookup.minute),
    second: Number(lookup.second),
    dayOfWeek0: WEEKDAYS[lookup.weekday] ?? 0,
  };
}

/** Minutes since midnight, as used by the dim schedule and quiet hours. */
export function minutesOfDay(fields: ZonedFields): number {
  return fields.hour * 60 + fields.minute;
}

/**
 * Day-of-week for a Gregorian date, independent of any time zone: the weekday of
 * a calendar date is the same everywhere, so UTC arithmetic is exact here.
 *
 * @returns 0 = Sunday
 */
export function dayOfWeekFor(year: number, month0: number, day: number): number {
  return new Date(Date.UTC(year, month0, day)).getUTCDay();
}

/** Number of days in a Gregorian month (month0 zero-based). */
export function daysInMonth(year: number, month0: number): number {
  return new Date(Date.UTC(year, month0 + 1, 0)).getUTCDate();
}

/** `yyyy-MM-dd` key used by the holiday table and the daily forecast lookup. */
export function dateKey(year: number, month0: number, day: number): string {
  return `${String(year).padStart(4, '0')}-${String(month0 + 1).padStart(2, '0')}-${String(
    day
  ).padStart(2, '0')}`;
}

/** Adds `days` to a calendar date, normalising month/year rollover. */
export function addDays(
  year: number,
  month0: number,
  day: number,
  days: number
): { year: number; month0: number; day: number } {
  const shifted = new Date(Date.UTC(year, month0, day + days));
  return {
    year: shifted.getUTCFullYear(),
    month0: shifted.getUTCMonth(),
    day: shifted.getUTCDate(),
  };
}

/** Adds `months` to a year/month pair, clamping the day to the target month. */
export function addMonths(
  year: number,
  month0: number,
  day: number,
  months: number
): { year: number; month0: number; day: number } {
  const total = year * 12 + month0 + months;
  const targetYear = Math.floor(total / 12);
  const targetMonth = total - targetYear * 12;
  return {
    year: targetYear,
    month0: targetMonth,
    day: Math.min(day, daysInMonth(targetYear, targetMonth)),
  };
}
