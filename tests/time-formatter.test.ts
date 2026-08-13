/**
 * Ported from the ClockTimeFormatter cases in
 * app/src/test/java/com/clockmods/ui/ClockLayoutCalculatorTest.java.
 */
import { describe, expect, it } from 'vitest';
import {
  formatHourlyChime,
  formatMinutesOfDay,
  formatTime,
  hasSmallSeconds,
} from '../src/format/time-formatter';

describe('formatTime', () => {
  it('honours the second and colon settings', () => {
    const regular = formatTime(8, 5, 7, true, false, false, true, false);
    expect(regular.mainText).toBe('08:05:07');
    expect(hasSmallSeconds(regular)).toBe(false);

    const hiddenSeconds = formatTime(8, 5, 7, false, false, true, true, false);
    expect(hiddenSeconds.mainText).toBe('08:05');
    expect(hasSmallSeconds(hiddenSeconds)).toBe(false);
  });

  it('alternates the blinking colon on the second boundary', () => {
    expect(formatTime(8, 5, 6, true, true, false, true, false).mainText).toBe('08:05:06');
    const hiddenColons = formatTime(8, 5, 7, true, true, false, true, false);
    expect(hiddenColons.mainText).toBe('08:05:07');
    expect(hiddenColons.colonVisible).toBe(false);
  });

  it('separates small seconds without a colon', () => {
    const displayTime = formatTime(8, 5, 7, true, false, true, true, false);
    expect(displayTime.mainText).toBe('08:05');
    expect(displayTime.secondsText).toBe('07');
    expect(hasSmallSeconds(displayTime)).toBe(true);
  });

  it('formats localized twelve-hour periods', () => {
    const midnight = formatTime(0, 5, 7, false, false, false, false, false);
    expect(midnight.mainText).toBe('12:05');
    expect(midnight.periodText).toBe('上午');

    const noon = formatTime(12, 5, 7, false, false, false, false, true);
    expect(noon.mainText).toBe('12:05');
    expect(noon.periodText).toBe('PM');

    const afternoon = formatTime(15, 5, 7, false, false, false, false, false);
    expect(afternoon.mainText).toBe('03:05');
    expect(afternoon.periodText).toBe('下午');
  });
});

describe('formatHourlyChime', () => {
  it('formats for both twelve- and twenty-four-hour clocks', () => {
    expect(formatHourlyChime(13, 0, true, false)).toBe('13:00');
    expect(formatHourlyChime(13, 0, false, false)).toBe('下午1:00');
    expect(formatHourlyChime(0, 0, false, false)).toBe('上午12:00');
    expect(formatHourlyChime(12, 0, false, true)).toBe('12:00 PM');
  });
});

describe('formatMinutesOfDay', () => {
  it('renders HH:mm for the settings pickers', () => {
    expect(formatMinutesOfDay(22 * 60)).toBe('22:00');
    expect(formatMinutesOfDay(6 * 60 + 5)).toBe('06:05');
    expect(formatMinutesOfDay(0)).toBe('00:00');
  });
});
