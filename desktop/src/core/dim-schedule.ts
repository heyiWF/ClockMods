/** Ported from com.clockmods.background.BackgroundDimSchedule. */

/**
 * Whether the scheduled dim window covers `currentMinutes`. A window whose start
 * is after its end wraps across midnight; an empty window (start == end) never
 * activates.
 *
 * All values are minutes since midnight in the app's time zone.
 */
export function isDimScheduleActive(
  currentMinutes: number,
  startMinutes: number,
  endMinutes: number
): boolean {
  if (startMinutes === endMinutes) return false;
  if (startMinutes < endMinutes) {
    return currentMinutes >= startMinutes && currentMinutes < endMinutes;
  }
  return currentMinutes >= startMinutes || currentMinutes < endMinutes;
}
