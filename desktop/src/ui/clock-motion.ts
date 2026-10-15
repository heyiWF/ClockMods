/** Shared with Ultimate's ClockDigitTransitionTiming and ClockScanTransition. */
export const motionDuration = (transition: string, supporting = false): number =>
  supporting && (transition === 'slide_right' || transition === 'scan') ? 660
    : transition === 'slide_right' ? 440 : transition === 'scan' ? 480 : 300;
export const easeOutCubic = (progress: number): number => 1 - (1 - Math.max(0, Math.min(1, progress))) ** 3;
export function changedDigitPair(previous: string, current: string, index: number): boolean {
  if (previous.length !== current.length || !/\d/.test(current[index] ?? '')) return false;
  let start = index;
  while (start > 0 && /\d/.test(current[start - 1])) start--;
  const pair = start + Math.floor((index - start) / 2) * 2;
  return previous.slice(pair, pair + 2) !== current.slice(pair, pair + 2);
}
export function scanMask(progress: number, revealing: boolean, feather = 18): string {
  const edge = revealing ? (100 + feather) * easeOutCubic(progress)
    : -feather + (100 + feather) * easeOutCubic(progress);
  return revealing
    ? `linear-gradient(to right, #000 ${edge - feather}%, transparent ${edge}%)`
    : `linear-gradient(to right, transparent ${edge}%, #000 ${edge + feather}%)`;
}
export function supportingTravel(width: number, fontSize: number): number {
  return Math.min(fontSize * 2.4, Math.max(fontSize * .75, width * .18));
}
