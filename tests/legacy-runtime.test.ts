import { readFileSync, existsSync } from 'node:fs';
// @ts-expect-error jsdom is a runtime-only test dependency in this repository.
import { JSDOM } from 'jsdom';
import { expect, it } from 'vitest';

const html = readFileSync('index.html', 'utf8');
const script = readFileSync('public/legacy-clock.js', 'utf8');
it('ships every production asset referenced by the classic-script entry', () => {
  for (const name of ['legacy-clock.css', 'legacy-clock.js', 'legacy-lunar.js']) {
    expect(html).toContain(name); expect(existsSync('public/' + name)).toBe(true);
  }
  expect(html).not.toContain('type="module"');
  expect(script).not.toMatch(/\b(?:const|let|class|async|await|Promise|fetch|ResizeObserver|Map|Set)\b|=>/);
  expect(readFileSync('public/legacy-clock.css', 'utf8')).not.toMatch(/var\(--|display:\s*grid|\bdvh\b/);
});
it('boots without modern APIs and persists all theme controls', () => {
  const dom = new JSDOM(html, { url: 'https://clock.example/', runScripts: 'outside-only' });
  const win = dom.window;
  try {
    for (const api of ['Promise', 'fetch', 'ResizeObserver', 'Map', 'Set']) Object.defineProperty(win, api, { value: undefined });
    win.eval(readFileSync('public/legacy-lunar.js', 'utf8')); win.eval(script);
    expect(win.document.getElementById('clock-main')!.textContent).not.toBe('--:--');
    expect(win.document.getElementById('clock-date')!.textContent + win.document.getElementById('clock-lunar')!.textContent).toMatch(/\[[^\]]+\]年/);
    for (const theme of ['classic', 'ultimate.dual_blocks', 'ultimate.orbit', 'ultimate.bubbles', 'ultimate.blend', 'ultimate.ribbon']) {
      (win.document.getElementById('settings-button') as HTMLButtonElement).click();
      expect(win.getComputedStyle(win.document.getElementById('settings-overlay')!).display).not.toBe('none');
      const select = win.document.getElementById('pref-theme') as HTMLSelectElement;
      select.value = theme; select.dispatchEvent(new win.Event('change'));
      (win.document.getElementById('settings-apply') as HTMLButtonElement).click();
      expect(win.localStorage.getItem('clockmods_legacy.theme')).toBe(theme);
      expect(win.document.getElementById('clock-lines')!.className).toContain('theme-' + theme.replace('ultimate.', ''));
    }
    (win.document.getElementById('settings-button') as HTMLButtonElement).click();
    (win.document.getElementById('pref-message') as HTMLInputElement).value = '<img src=x onerror=alert(1)>';
    (win.document.getElementById('settings-cancel') as HTMLButtonElement).click();
    expect(win.localStorage.getItem('clockmods_legacy.message')).toBe('');
  } finally { win.close(); }
});
