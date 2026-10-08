import { describe, expect, it } from 'vitest';
import { fitSupportingRows } from '../src/format/responsive-layout';

describe('primary-first layout', () => {
  it('fits four supporting rows without taking space from the primary clock', () => {
    for (const height of [90, 240, 720, 1600]) {
      const primary = height * .72;
      const fit = fitSupportingRows(height, primary, 80, 160, 2, 2, true);
      const occupied = (fit.dateSize + fit.supportingSize) * 2.4 + 2 * fit.timeGap + 2 * fit.rowGap;
      expect(primary + occupied).toBeLessThanOrEqual(height + .0001);
      expect(fit.dateSize).toBeGreaterThan(0);
    }
  });
  it('keeps default date and supporting sizes equal and allows growth when room is available', () => {
    const roomy = fitSupportingRows(800, 200, 24, 24, 1, 1, false);
    const tight = fitSupportingRows(240, 200, 24, 24, 1, 1, false);
    expect(roomy.dateSize).toBe(24);
    expect(roomy.dateSize).toBe(roomy.supportingSize);
    expect(tight.dateSize).toBe(tight.supportingSize);
    expect(tight.dateSize).toBeLessThan(roomy.dateSize);
  });
});
