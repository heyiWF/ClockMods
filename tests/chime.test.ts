/**
 * @vitest-environment jsdom
 *
 * Ported from
 * app/src/testPro/java/com/clockmods/pro/chime/HourlyChimeControllerTest.java.
 */
import { beforeEach, describe, expect, it } from 'vitest';
import { isQuietHour, upcomingChimeAt } from '../src/ui/chime';
import { prefs } from '../src/core/prefs';
import { zonedFields } from '../src/core/zoned-time';

const ZONE = 'Asia/Shanghai';

/** 2026-07-24 at the given Shanghai wall-clock time. */
function instantAt(hour: number, minute: number, second: number, millis = 0): number {
  return Date.UTC(2026, 6, 24, hour - 8, minute, second, millis);
}

beforeEach(() => {
  localStorage.clear();
});

describe('upcomingChimeAt', () => {
  it('arms during the final two seconds before the hour', () => {
    const now = instantAt(13, 59, 58, 250);
    const chimeAt = upcomingChimeAt(zonedFields(now, ZONE), now);

    expect(chimeAt).not.toBeNull();
    const fields = zonedFields(chimeAt!, ZONE);
    expect(fields.hour).toBe(14);
    expect(fields.minute).toBe(0);
    expect(fields.second).toBe(0);
    expect(chimeAt! % 1000).toBe(0);
  });

  it('does not arm before the final two seconds or after the hour', () => {
    const early = instantAt(13, 59, 57, 999);
    expect(upcomingChimeAt(zonedFields(early, ZONE), early)).toBeNull();

    const past = instantAt(14, 0, 0, 0);
    expect(upcomingChimeAt(zonedFields(past, ZONE), past)).toBeNull();
  });

  it('rolls over to the next day at midnight', () => {
    const now = instantAt(23, 59, 59, 0);
    const chimeAt = upcomingChimeAt(zonedFields(now, ZONE), now)!;
    const fields = zonedFields(chimeAt, ZONE);

    expect(fields.hour).toBe(0);
    expect(fields.day).toBe(25);
  });
});

describe('isQuietHour', () => {
  const fieldsAt = (hour: number) => zonedFields(instantAt(hour, 30, 0), ZONE);

  it('is off when quiet hours are disabled', () => {
    prefs.setHourlyChimeQuietEnabled(false);
    expect(isQuietHour(fieldsAt(23))).toBe(false);
  });

  it('covers an overnight window', () => {
    prefs.setHourlyChimeQuietEnabled(true);
    prefs.setHourlyChimeQuietStart(22 * 60);
    prefs.setHourlyChimeQuietEnd(7 * 60);

    expect(isQuietHour(fieldsAt(23))).toBe(true);
    expect(isQuietHour(fieldsAt(3))).toBe(true);
    expect(isQuietHour(fieldsAt(12))).toBe(false);
  });

  it('covers a same-day window', () => {
    prefs.setHourlyChimeQuietEnabled(true);
    prefs.setHourlyChimeQuietStart(9 * 60);
    prefs.setHourlyChimeQuietEnd(17 * 60);

    expect(isQuietHour(fieldsAt(12))).toBe(true);
    expect(isQuietHour(fieldsAt(20))).toBe(false);
  });

  it('treats an empty window as always quiet', () => {
    prefs.setHourlyChimeQuietEnabled(true);
    prefs.setHourlyChimeQuietStart(6 * 60);
    prefs.setHourlyChimeQuietEnd(6 * 60);

    expect(isQuietHour(fieldsAt(12))).toBe(true);
  });
});
