import { getCurrentWindow } from '@tauri-apps/api/window';
import type { FullscreenController } from '../ui/fullscreen';

/** Use the native window state, including changes from F11 and desktop settings. */
export function desktopFullscreen(): FullscreenController {
  const win = getCurrentWindow();
  return {
    isFullscreen: () => win.isFullscreen(),
    setFullscreen: full => win.setFullscreen(full),
    onChange: listener => win.onResized(listener),
  };
}
