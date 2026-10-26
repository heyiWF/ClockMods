// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
const source = readFileSync('public/launch-guard.js', 'utf8');
function installLaunchScreen() { new Function(source)(); }
beforeEach(() => {
  vi.useFakeTimers();
  delete document.documentElement.dataset.clockmodsReady;
  document.body.innerHTML = '<div id="launch-screen"><p id="launch-status"></p><button id="launch-retry" hidden></button></div><div id="app"></div>';
  vi.stubGlobal('matchMedia', () => ({ matches: false }));
  vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => { cb(0); return 0; });
});
afterEach(() => {
  window.dispatchEvent(new Event('clockmods:ready'));
  vi.runAllTimers(); vi.useRealTimers(); vi.unstubAllGlobals();
});
it('reveals only when ready, removes the overlay and restores interaction', () => {
  installLaunchScreen();
  expect(document.getElementById('app')?.hasAttribute('inert')).toBe(true);
  window.dispatchEvent(new Event('clockmods:ready'));
  expect(document.getElementById('app')?.hasAttribute('inert')).toBe(true);
  expect(document.getElementById('launch-screen')?.classList.contains('is-leaving')).toBe(false);
  vi.advanceTimersByTime(920);
  expect(document.getElementById('app')?.hasAttribute('inert')).toBe(false);
  expect(document.getElementById('launch-screen')?.classList.contains('is-leaving')).toBe(true);
  vi.advanceTimersByTime(400);
  expect(document.getElementById('launch-screen')).toBeNull();
});
it('handles already completed initialization without waiting for another event', () => {
  document.documentElement.dataset.clockmodsReady = 'true';
  vi.stubGlobal('matchMedia', () => ({ matches: true }));
  installLaunchScreen();
  expect(document.getElementById('launch-screen')).toBeNull();
});
it('offers retry after failure or a stalled bootstrap and still accepts late success', () => {
  installLaunchScreen();
  vi.advanceTimersByTime(20000);
  expect(document.getElementById('launch-retry')?.hasAttribute('hidden')).toBe(false);
  window.dispatchEvent(new Event('clockmods:ready'));
  vi.advanceTimersByTime(400);
  expect(document.getElementById('launch-screen')).toBeNull();
});
