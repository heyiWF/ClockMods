/**
 * Screen orientation preference.
 *
 * Replaces ClockPreferences.toActivityInfoOrientation + setRequestedOrientation.
 * The Screen Orientation API only grants a lock to installed/full-screen PWAs, so
 * when it is refused the app falls back to a body class that lets the layout
 * follow the requested orientation by aspect ratio instead.
 */
import { ORIENTATION_LANDSCAPE, ORIENTATION_PORTRAIT, prefs } from './prefs';

export async function applyScreenOrientation(): Promise<boolean> {
  const mode = prefs.getScreenOrientation();
  const root = document.documentElement;
  root.classList.toggle('force-portrait', mode === ORIENTATION_PORTRAIT);
  root.classList.toggle('force-landscape', mode === ORIENTATION_LANDSCAPE);

  const orientation = screen.orientation as
    | (ScreenOrientation & { lock?: (value: string) => Promise<void> })
    | undefined;
  if (!orientation) return false;
  if (mode === ORIENTATION_PORTRAIT || mode === ORIENTATION_LANDSCAPE) {
    try {
      await orientation.lock?.(mode === ORIENTATION_PORTRAIT ? 'portrait' : 'landscape');
      return true;
    } catch {
      // Not installed, or the browser does not allow locking: the CSS fallback
      // above already applies.
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
