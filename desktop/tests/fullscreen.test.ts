/** @vitest-environment jsdom */
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { installFullscreenButtons, type FullscreenController } from '../src/ui/fullscreen';
import { installIdleCursor } from '../src/ui/idle-cursor';
import { setLanguage } from '../src/core/i18n';

let dispose: (() => void)[];
const settle = async () => { for (let i = 0; i < 8; i++) await Promise.resolve(); };
beforeEach(() => {
  dispose = [];
  document.body.innerHTML = '<main><section class="page" data-page="clock"></section><section class="page" data-page="calendar"></section><section class="page" data-page="alarm"></section></main><div id="toast-root"></div>';
  Object.defineProperty(document, 'hidden', { configurable: true, value: false });
  setLanguage('en');
});
afterEach(() => {
  dispose.reverse().forEach(fn => fn());
  vi.useRealTimers();
  vi.restoreAllMocks();
});
function controller() {
  let full = false, changed = () => {};
  const unsubscribe = vi.fn();
  const host: FullscreenController = {
    isFullscreen: vi.fn(async () => full),
    setFullscreen: vi.fn(async (value: boolean) => { full = value; changed(); }),
    onChange: listener => { changed = listener; return unsubscribe; },
  };
  return { host, unsubscribe, external: (value: boolean) => { full = value; changed(); } };
}
it('toggles the same fullscreen surface from clock and calendar and tracks external exit', async () => {
  const native = controller();
  dispose.push(installFullscreenButtons(document, native.host));
  const buttons = [...document.querySelectorAll<HTMLButtonElement>('.fullscreen-button')];
  expect(buttons.map(b => b.parentElement!.dataset.page)).toEqual(['clock', 'calendar']);
  await settle();
  buttons[0].click();
  await settle();
  expect(native.host.setFullscreen).toHaveBeenLastCalledWith(true);
  expect(buttons.every(b => b.getAttribute('aria-label') === 'Exit fullscreen')).toBe(true);
  const fullIcon = buttons[0].querySelector('path')!.getAttribute('d');
  buttons[1].click();
  await settle();
  expect(native.host.setFullscreen).toHaveBeenLastCalledWith(false);
  expect(buttons[0].querySelector('path')!.getAttribute('d')).not.toBe(fullIcon);
  native.external(true);
  await settle();
  native.external(false);
  await settle();
  expect(buttons.every(b => b.getAttribute('aria-pressed') === 'false')).toBe(true);
  setLanguage('zh-Hant');
  await settle();
  expect(buttons[1].title).toBe('進入全螢幕');
});
it('requests the document root and observes browser Escape/fullscreen changes', async () => {
  const enter = vi.fn(async () => {}), exit = vi.fn(async () => {});
  Object.defineProperty(document.documentElement, 'requestFullscreen', { configurable: true, value: enter });
  Object.defineProperty(document, 'exitFullscreen', { configurable: true, value: exit });
  dispose.push(() => { delete (document.documentElement as Partial<HTMLElement>).requestFullscreen; delete (document as Partial<Document>).exitFullscreen; Reflect.deleteProperty(document, 'fullscreenElement'); });
  dispose.push(installFullscreenButtons());
  const button = document.querySelector<HTMLButtonElement>('.fullscreen-button')!;
  button.click();
  // Browser entry must happen before the gesture handler yields.
  expect(enter).toHaveBeenCalled();
  await settle();
  expect(enter.mock.contexts[0]).toBe(document.documentElement);
  Object.defineProperty(document, 'fullscreenElement', { configurable: true, value: document.documentElement });
  document.dispatchEvent(new Event('fullscreenchange'));
  await settle();
  expect(button.title).toBe('Exit fullscreen');
  button.click();
  await settle();
  expect(exit).toHaveBeenCalled();
});
it('does not expose a control on hosts without a fullscreen API', () => {
  dispose.push(installFullscreenButtons(document, null));
  expect(document.querySelector('.fullscreen-button')).toBeNull();
});
it('reports denied fullscreen requests and leaves both controls usable', async () => {
  const native = controller();
  native.host.setFullscreen = vi.fn().mockRejectedValue(new Error('denied'));
  dispose.push(installFullscreenButtons(document, native.host));
  document.querySelector<HTMLButtonElement>('.fullscreen-button')!.click();
  await settle();
  expect(document.querySelector('#toast-root')!.textContent).toContain('Fullscreen is unavailable');
  expect([...document.querySelectorAll<HTMLButtonElement>('.fullscreen-button')].every(b => !b.disabled && b.title === 'Enter fullscreen')).toBe(true);
});
it('starts the mouse inactivity deadline on boot and restores controls on movement and keyboard use', () => {
  vi.useFakeTimers();
  Object.defineProperty(window, 'matchMedia', { configurable: true, value: vi.fn(() => ({ matches: true })) });
  dispose.push(() => { delete (window as Partial<Window>).matchMedia; });
  dispose.push(installIdleCursor());
  vi.advanceTimersByTime(3000);
  expect(document.documentElement.classList.contains('is-cursor-idle')).toBe(true);
  document.dispatchEvent(Object.assign(new Event('pointermove'), { pointerType: 'mouse' }));
  expect(document.documentElement.classList.contains('is-cursor-idle')).toBe(false);
  vi.advanceTimersByTime(3000);
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab' }));
  expect(document.documentElement.classList.contains('is-cursor-idle')).toBe(false);
});
it('removes both buttons and all state subscriptions on disposal', async () => {
  const native = controller(), stop = installFullscreenButtons(document, native.host);
  stop();
  await settle();
  expect(document.querySelector('.fullscreen-button')).toBeNull();
  expect(native.unsubscribe).toHaveBeenCalledOnce();
});
