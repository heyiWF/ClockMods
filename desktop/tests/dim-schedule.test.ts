/** Ported from app/src/test/java/com/clockmods/background/BackgroundDimScheduleTest.java. */
import { describe, expect, it } from 'vitest';
import { isDimScheduleActive } from '../src/core/dim-schedule';

describe('isDimScheduleActive', () => {
  it('supports same-day and overnight ranges', () => {
    expect(isDimScheduleActive(13 * 60, 12 * 60, 18 * 60)).toBe(true);
    expect(isDimScheduleActive(19 * 60, 12 * 60, 18 * 60)).toBe(false);
    expect(isDimScheduleActive(23 * 60, 22 * 60, 6 * 60)).toBe(true);
    expect(isDimScheduleActive(5 * 60, 22 * 60, 6 * 60)).toBe(true);
    expect(isDimScheduleActive(12 * 60, 22 * 60, 6 * 60)).toBe(false);
  });

  it('treats equal times as a disabled range', () => {
    expect(isDimScheduleActive(12 * 60, 6 * 60, 6 * 60)).toBe(false);
  });

  it('includes the start minute and excludes the end minute', () => {
    expect(isDimScheduleActive(22 * 60, 22 * 60, 6 * 60)).toBe(true);
    expect(isDimScheduleActive(6 * 60, 22 * 60, 6 * 60)).toBe(false);
  });
});
