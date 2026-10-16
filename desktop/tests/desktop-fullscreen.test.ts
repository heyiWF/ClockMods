/** @vitest-environment jsdom */
import { expect, it, vi } from 'vitest';
const win = vi.hoisted(() => ({
  isFullscreen: vi.fn().mockResolvedValue(true),
  setFullscreen: vi.fn().mockResolvedValue(undefined),
  onResized: vi.fn().mockResolvedValue(() => {}),
}));
vi.mock('@tauri-apps/api/window', () => ({ getCurrentWindow: () => win }));
import { desktopFullscreen } from '../src/desktop/fullscreen';

it('uses native window fullscreen state and resize events shared with F11', async () => {
  const controller = desktopFullscreen(), changed = vi.fn();
  expect(await controller.isFullscreen()).toBe(true);
  await controller.setFullscreen(false);
  expect(win.setFullscreen).toHaveBeenCalledWith(false);
  await controller.onChange(changed);
  expect(win.onResized).toHaveBeenCalledWith(changed);
});
