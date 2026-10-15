/** Ported from app/src/test/java/com/clockmods/calendar/HolidayRepositoryTest.java. */
import { describe, expect, it } from 'vitest';
import { parseHolidays } from '../src/lunar/holidays';

describe('parseHolidays', () => {
  it('parses off days and make-up work days', () => {
    const statuses = parseHolidays(
      '{"days":[{"name":"春节","date":"2026-02-17","isOffDay":true},' +
        '{"name":"春节","date":"2026-02-14","isOffDay":false}]}'
    );

    expect(statuses.get('2026-02-17')?.offDay).toBe(true);
    expect(statuses.get('2026-02-14')?.offDay).toBe(false);
    expect(statuses.get('2026-02-17')?.name).toBe('春节');
  });

  it('skips entries with a blank name or malformed date', () => {
    const statuses = parseHolidays(
      '{"days":[{"name":"","date":"2026-01-01","isOffDay":true},' +
        '{"name":"元旦","date":"2026-1-1","isOffDay":true},' +
        '{"name":"元旦","date":"2026-01-02","isOffDay":true}]}'
    );

    expect(statuses.size).toBe(1);
    expect(statuses.has('2026-01-02')).toBe(true);
  });

  it('tolerates a missing days array', () => {
    expect(parseHolidays('{}').size).toBe(0);
  });
});
