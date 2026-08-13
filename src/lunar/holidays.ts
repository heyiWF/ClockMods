/**
 * Statutory 休/班 arrangements.
 *
 * These are announced yearly by the State Council and cannot be computed, so they
 * ship as offline data from holiday-cn (MIT). Android read the JSON from assets
 * synchronously; the browser fetches it once at startup and answers from memory
 * afterwards.
 *
 * Ported from com.clockmods.calendar.HolidayRepository.
 */

export interface HolidayStatus {
  name: string;
  date: string;
  offDay: boolean;
}

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

let statuses = new Map<string, HolidayStatus>();
let loading: Promise<void> | null = null;

/** Parses one holiday-cn year file. Exported for tests. */
export function parseHolidays(json: string): Map<string, HolidayStatus> {
  const output = new Map<string, HolidayStatus>();
  const root = JSON.parse(json) as { days?: unknown };
  const days = Array.isArray(root.days) ? root.days : [];
  for (const entry of days) {
    if (!entry || typeof entry !== 'object') continue;
    const day = entry as { name?: unknown; date?: unknown; isOffDay?: unknown };
    const name = typeof day.name === 'string' ? day.name.trim() : '';
    const date = typeof day.date === 'string' ? day.date.trim() : '';
    if (!name || !DATE_PATTERN.test(date)) continue;
    output.set(date, { name, date, offDay: day.isOffDay === true });
  }
  return output;
}

/**
 * Loads every bundled year. Failures are swallowed: a missing file only means
 * the 休/班 badges are absent, which must never break the calendar.
 */
export function loadHolidays(): Promise<void> {
  if (loading) return loading;
  loading = (async () => {
    const base = `${import.meta.env.BASE_URL}data/holidays/`;
    let years: string[] = [];
    try {
      const response = await fetch(`${base}index.json`);
      if (response.ok) years = (await response.json()) as string[];
    } catch {
      years = [];
    }
    const merged = new Map<string, HolidayStatus>();
    await Promise.all(
      years.map(async (year) => {
        try {
          const response = await fetch(`${base}${year}.json`);
          if (!response.ok) return;
          for (const [date, status] of parseHolidays(await response.text())) {
            merged.set(date, status);
          }
        } catch {
          /* ignore a single unreadable year */
        }
      })
    );
    statuses = merged;
  })();
  return loading;
}

/** @param date `yyyy-MM-dd` */
export function holidayOn(date: string): HolidayStatus | null {
  return statuses.get(date) ?? null;
}

/** Test hook: installs a table without fetching. */
export function setHolidayTable(table: Map<string, HolidayStatus>): void {
  statuses = table;
  loading = Promise.resolve();
}
