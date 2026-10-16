import { readFileSync } from 'node:fs';
// @ts-expect-error jsdom is a runtime-only test dependency in this repository.
import { JSDOM } from 'jsdom';
import { expect, it, vi } from 'vitest';

const html = readFileSync('index.html', 'utf8');
const script = readFileSync('public/legacy-clock.js', 'utf8');
it('supports IE fullscreen entry, exit, external state changes, and localized labels', () => {
  const dom = new JSDOM(html, { url: 'https://clock.example/', runScripts: 'outside-only' });
  const win = dom.window, doc = win.document;
  try {
    let full = false;
    Object.defineProperty(doc, 'msFullscreenElement', { get: () => full ? doc.documentElement : null });
    const changed = () => doc.dispatchEvent(new win.Event('MSFullscreenChange'));
    const enter = vi.fn(() => { full = true; changed(); });
    const exit = vi.fn(() => { full = false; changed(); });
    Object.assign(doc.documentElement, { msRequestFullscreen: enter });
    Object.assign(doc, { msExitFullscreen: exit });
    win.eval(script);
    const button = doc.getElementById('fullscreen-button') as HTMLButtonElement;
    expect(button.getAttribute('aria-pressed')).toBe('false');
    button.click();
    expect(enter).toHaveBeenCalledOnce();
    expect(button.title).toBe('退出全屏');
    button.click();
    expect(exit).toHaveBeenCalledOnce();
    expect(button.title).toBe('进入全屏');
    full = true; changed();
    (doc.getElementById('settings-button') as HTMLButtonElement).click();
    const language = doc.getElementById('pref-language') as HTMLSelectElement;
    language.value = 'en'; language.dispatchEvent(new win.Event('change'));
    expect(button.title).toBe('Exit fullscreen');
    full = false; changed();
    expect(button.getAttribute('aria-pressed')).toBe('false');
  } finally { win.close(); }
});
it('shares the three second cursor deadline and restores the button for mouse and keyboard input', () => {
  const dom = new JSDOM(html, { url: 'https://clock.example/', runScripts: 'outside-only' });
  const win = dom.window, doc = win.document;
  try {
    const idle: (() => void)[] = [], original = win.setTimeout.bind(win);
    win.setTimeout = ((fn: () => void, delay: number) => {
      if (delay === 3000) idle.push(fn);
      return original(fn, delay);
    }) as typeof win.setTimeout;
    win.eval(script);
    expect(idle.length).toBeGreaterThan(0);
    idle.pop()!();
    expect(doc.documentElement.className).toContain('is-cursor-idle');
    doc.dispatchEvent(new win.MouseEvent('mousemove'));
    expect(doc.documentElement.className).not.toContain('is-cursor-idle');
    idle.splice(0).forEach(fn => fn());
    expect(doc.documentElement.className).toContain('is-cursor-idle');
    doc.dispatchEvent(new win.KeyboardEvent('keydown', { keyCode: 9 }));
    expect(doc.documentElement.className).not.toContain('is-cursor-idle');
    expect(doc.documentElement.className).toContain('is-keyboard');
    const css = readFileSync('public/legacy-clock.css', 'utf8');
    expect(css).toContain('.is-cursor-idle .fullscreen-button { opacity:0; pointer-events:none; }');
  } finally { win.close(); }
});
it('reports browser request rejection without an unhandled rejection', async () => {
  const dom = new JSDOM(html, { url: 'https://clock.example/', runScripts: 'outside-only' });
  const win = dom.window;
  try {
    Object.assign(win.document.documentElement, { requestFullscreen: () => Promise.reject(new Error('denied')) });
    win.eval(script);
    (win.document.getElementById('fullscreen-button') as HTMLButtonElement).click();
    await Promise.resolve();
    expect(win.document.getElementById('clock-status')!.textContent).toContain('Fullscreen unavailable');
  } finally { win.close(); }
});
