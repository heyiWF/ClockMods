import { isDesktop } from '../desktop/bridge';
import { getCurrentWindow, LogicalSize } from '@tauri-apps/api/window';
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
  if (isDesktop()) {
    if (mode === ORIENTATION_PORTRAIT) await getCurrentWindow().setSize(new LogicalSize(600, 900));
    if (mode === ORIENTATION_LANDSCAPE) await getCurrentWindow().setSize(new LogicalSize(1100, 740));
    return true;
  }
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
