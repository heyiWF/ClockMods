/**
 * @vitest-environment jsdom
 *
 * Guards the web-legacy contract: one self-contained clock page, a complete
 * settings surface and classic scripts that IE can execute without a module
 * loader or modern Web APIs.
 */
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { beforeAll, describe, expect, it } from 'vitest';

const html = readFileSync(resolve(__dirname, '../index.html'), 'utf8');
const script = readFileSync(resolve(__dirname, '../public/legacy-clock.js'), 'utf8');
const lunar = readFileSync(resolve(__dirname, '../public/legacy-lunar.js'), 'utf8');
const css = readFileSync(resolve(__dirname, '../public/legacy-clock.css'), 'utf8');

beforeAll(() => {
  document.documentElement.innerHTML = html;
});

describe('legacy clock shell', () => {
  it('contains only the clock experience and no modern page router', () => {
    expect(document.getElementById('clock-app')).not.toBeNull();
    expect(document.querySelectorAll('[data-page]')).toHaveLength(0);
    expect(document.getElementById('nav')).toBeNull();
  });

  it('contains every element the classic clock script resolves', () => {
    const ids = [
      'clock-background',
      'clock-dim',
      'clock-face',
      'clock-lines',
      'clock-date',
      'clock-lunar',
      'clock-time',
      'clock-main',
      'clock-period',
      'clock-small-seconds',
      'clock-extra',
      'clock-weather',
      'clock-message',
      'clock-toolbar',
      'settings-button',
      'fullscreen-button',
      'settings-overlay',
      'settings-panel',
      'clock-status',
    ];
    for (const id of ids) expect(document.getElementById(id), id).not.toBeNull();
  });

  it('starts the dim layer and settings panel hidden', () => {
    expect(document.getElementById('clock-dim')!.hasAttribute('hidden')).toBe(true);
    expect(document.getElementById('settings-overlay')!.hasAttribute('hidden')).toBe(true);
  });

  it('uses classic scripts and forces the newest IE document mode', () => {
    expect(html).toContain('http-equiv="X-UA-Compatible" content="IE=edge"');
    expect(html).toContain('<script src="./legacy-lunar.js"></script>');
    expect(html).toContain('<script src="./legacy-clock.js"></script>');
    expect(html).not.toContain('type="module"');
    expect(html).not.toContain('/src/main.ts');
  });

  it('exposes the clock settings that survive the IE downgrade', () => {
    for (const id of [
      'pref-hour-format',
      'pref-timezone',
      'pref-show-seconds',
      'pref-small-seconds',
      'pref-blink-colon',
      'pref-animate',
      'pref-stacked',
      'pref-lunar',
      'pref-date-pattern',
      'pref-background-file',
      'pref-schedule-dim',
      'pref-hourly-chime',
      'pref-weather',
      'pref-network-time',
    ]) {
      expect(document.getElementById(id), id).not.toBeNull();
    }
  });

  it('keeps the two runtime files within the ES5 syntax surface', () => {
    const modernSyntax = [
      /=>/,
      /\b(?:const|let|class|async|await)\b/,
      /\?\?/,
      /\bPromise\b/,
      /\bfetch\s*\(/,
      /\.includes\s*\(/,
      /\.startsWith\s*\(/,
      /\.endsWith\s*\(/,
      /\.padStart\s*\(/,
    ];
    const executableScript = script.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '');
    const executableLunar = lunar.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '');
    for (const pattern of modernSyntax) {
      expect(executableScript, String(pattern)).not.toMatch(pattern);
      expect(executableLunar, String(pattern)).not.toMatch(pattern);
    }
  });

  it('avoids CSS features unavailable in IE11', () => {
    const executableCss = css.replace(/\/\*[\s\S]*?\*\//g, '');
    expect(executableCss).not.toMatch(/var\s*\(/);
    expect(executableCss).not.toMatch(/display\s*:\s*grid/);
    expect(executableCss).not.toContain('backdrop-filter');
    expect(executableCss).not.toContain('color-mix(');
    expect(executableCss).not.toContain('100dvh');
  });
});
