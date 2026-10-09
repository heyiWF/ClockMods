/**
 * Screen orientation preference.
 *
 * Replaces ClockPreferences.toActivityInfoOrientation + setRequestedOrientation.
 * Orientation locking depends on the browser and installation/full-screen state.
 * When unavailable, retain the responsive layout for the actual viewport.
 */
import { ORIENTATION_LANDSCAPE, ORIENTATION_PORTRAIT, prefs } from './prefs';

export async function applyScreenOrientation(): Promise<boolean> {
  const mode = prefs.getScreenOrientation();
  const orientation = screen.orientation as
    | (ScreenOrientation & { lock?: (value: string) => Promise<void> })
    | undefined;
  if (!orientation) return mode !== ORIENTATION_PORTRAIT && mode !== ORIENTATION_LANDSCAPE;
  if (mode === ORIENTATION_PORTRAIT || mode === ORIENTATION_LANDSCAPE) {
    try {
      if (!orientation.lock) return false;
      await orientation.lock(mode === ORIENTATION_PORTRAIT ? 'portrait' : 'landscape');
      return true;
    } catch {
      return false;
    }
  }
  try {
    orientation.unlock?.();
  } catch {
    /* nothing to unlock */
  }
  return true;
}
