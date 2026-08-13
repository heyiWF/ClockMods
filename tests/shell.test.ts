/**
 * @vitest-environment jsdom
 *
 * Guards the contract between index.html and the modules that query it: every
 * element main.ts and the pages look up must exist, with the page order the
 * ProPage enum defined. A typo here would only surface as a blank screen at
 * runtime, so it is checked statically instead.
 */
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { beforeAll, describe, expect, it } from 'vitest';

const html = readFileSync(resolve(__dirname, '../index.html'), 'utf8');

beforeAll(() => {
  document.documentElement.innerHTML = html;
});

/** Registration order in main.ts, matching com.clockmods.pro.ProPage. */
const PAGE_ORDER = ['clock', 'calendar', 'pomodoro', 'alarm', 'countdown', 'stopwatch'];

describe('application shell', () => {
  it('provides the six pages in ProPage order', () => {
    const pages = [...document.querySelectorAll('.page')].map((node) =>
      node.getAttribute('data-page')
    );
    expect(pages).toEqual(PAGE_ORDER);
  });

  it('provides a navigation button per page, in the same order', () => {
    const buttons = [...document.querySelectorAll('#nav button')].map((node) =>
      node.getAttribute('data-nav')
    );
    expect(buttons).toEqual(PAGE_ORDER);
  });

  it('contains every element main.ts resolves', () => {
    for (const id of ['pages', 'nav', 'chime-layer', 'alarm-layer', 'settings-root', 'toast-root']) {
      expect(document.getElementById(id), id).not.toBeNull();
    }
  });

  it('contains every element the clock page queries', () => {
    for (const id of [
      'clock-bg',
      'clock-dim',
      'clock-stage',
      'clock-lines',
      'clock-date',
      'clock-lunar',
      'clock-time',
      'clock-weather',
      'clock-detail',
      'clock-attribution',
    ]) {
      expect(document.getElementById(id), id).not.toBeNull();
    }
  });

  it('contains every element the calendar page queries', () => {
    for (const id of [
      'cal-time',
      'cal-seconds',
      'cal-period',
      'cal-title',
      'cal-weekdays',
      'cal-days',
      'cal-days-preview',
      'cal-days-viewport',
      'cal-footer',
      'cal-weather-card',
      'cal-forecast-card',
      'cal-attribution',
      'cal-weather-icon',
      'cal-temp',
      'cal-feels-label',
      'cal-feels',
      'cal-weather-summary',
      'cal-prev',
      'cal-next',
      'cal-today',
      'cal-forecast-0',
      'cal-forecast-1',
      'cal-forecast-2',
    ]) {
      expect(document.getElementById(id), id).not.toBeNull();
    }
    expect(document.querySelector('.cal-clock-panel')).not.toBeNull();
  });

  it('declares the viewport and PWA metadata the install prompt needs', () => {
    expect(html).toContain('viewport-fit=cover');
    expect(html).toContain('name="theme-color"');
    expect(html).toContain('rel="apple-touch-icon"');
    expect(html).toContain('apple-mobile-web-app-capable');
  });

  it('starts the overlays hidden', () => {
    expect(document.getElementById('chime-layer')!.hasAttribute('hidden')).toBe(true);
    expect(document.getElementById('alarm-layer')!.hasAttribute('hidden')).toBe(true);
  });

  it('includes readable QWeather attribution copy on both weather pages', () => {
    const labels = [...document.querySelectorAll('.weather-attribution-label')].map((node) =>
      node.textContent?.trim()
    );
    expect(labels).toHaveLength(2);
    expect(labels.every(Boolean)).toBe(true);
  });
});
