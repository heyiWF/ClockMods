/**
 * Ported from the layout cases in
 * app/src/test/java/com/clockmods/ui/ClockLayoutCalculatorTest.java.
 *
 * Only the pure arithmetic is covered: the canvas-backed measurement helpers need
 * a real text-shaping engine, which jsdom does not provide.
 */
import { describe, expect, it } from 'vitest';
import {
  calculateMainCenterOffset,
  calculateTimeGroupWidth,
  calculateWidthBasedTextSize,
  centerCropScale,
  shouldUseSingleDateLine,
} from '../src/format/text-metrics';

describe('calculateWidthBasedTextSize', () => {
  it('takes the smaller of the width and height limits, then scales it', () => {
    // Width limit: 1000 * 0.98 / 4 = 245. Height limit: 400 * 0.55 = 220.
    expect(calculateWidthBasedTextSize(1000, 400, 4, 1, 0.55, 0.98)).toBeCloseTo(220, 5);
    // Taller viewport: the width limit now binds.
    expect(calculateWidthBasedTextSize(1000, 1000, 4, 1, 0.55, 0.98)).toBeCloseTo(245, 5);
    // The user scale multiplies the fitting size.
    expect(calculateWidthBasedTextSize(1000, 1000, 4, 0.5, 0.55, 0.98)).toBeCloseTo(122.5, 5);
  });

  it('never returns a non-positive size', () => {
    expect(calculateWidthBasedTextSize(1000, 400, 0, 1, 0.55, 0.98)).toBe(1);
    expect(calculateWidthBasedTextSize(0, 0, 4, 1, 0.55, 0.98)).toBe(1);
  });
});

describe('date line and image fitting', () => {
  it('uses a single date line only when landscape and the text fits', () => {
    expect(shouldUseSingleDateLine(true, 800, 1000)).toBe(true);
    expect(shouldUseSingleDateLine(false, 800, 1000)).toBe(false);
    expect(shouldUseSingleDateLine(true, 950, 1000)).toBe(false);
  });

  it('center-crop scale fills both axes', () => {
    expect(centerCropScale(1000, 500, 1000, 1000)).toBeCloseTo(2, 5);
    expect(centerCropScale(500, 1000, 1000, 1000)).toBeCloseTo(2, 5);
  });
});

describe('time group geometry', () => {
  it('uses only the visible accessory widths', () => {
    expect(calculateTimeGroupWidth(600, 0, 200)).toBeCloseTo(800, 5);
    expect(calculateMainCenterOffset(0, 200)).toBeCloseTo(-100, 5);
    expect(calculateMainCenterOffset(150, 0)).toBeCloseTo(75, 5);
  });
});
