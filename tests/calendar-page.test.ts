/**
 * @vitest-environment jsdom
 *
 * Calendar dashboard rendering, driven by the real markup from index.html so the
 * test fails if a required element is renamed or removed.
 */
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { prefs, CALENDAR_WEEK_START_MONDAY } from '../src/core/prefs';
import { timeSource } from '../src/core/time-source';
import { setHolidayTable } from '../src/lunar/holidays';
import { CalendarPage } from '../src/pages/calendar';

/** 2026-08-07 13:45 in Shanghai. */
const INSTANT = Date.UTC(2026, 7, 7, 5, 45, 0);

const markup = (() => {
  const html = readFileSync(resolve(__dirname, '../index.html'), 'utf8');
  const match = html.match(/<section class="page page--immersive" data-page="calendar"[\s\S]*?<\/section>/);
  if (!match) throw new Error('calendar section not found in index.html');
  return match[0];
})();

function mount(): { root: HTMLElement; page: CalendarPage } {
  document.body.innerHTML = markup;
  const root = document.querySelector<HTMLElement>('.page')!;
  const page = new CalendarPage(root, vi.fn());
  page.refreshSettings();
  return { root, page };
}

beforeAll(() => {
  HTMLCanvasElement.prototype.getContext = () => null;
});

beforeEach(() => {
  localStorage.clear();
  vi.spyOn(timeSource, 'now').mockReturnValue(INSTANT);
  prefs.setTimeZoneId('Asia/Shanghai');
  // 2026-10-01 is a public holiday; 2026-10-10 is a make-up work day.
  setHolidayTable(
    new Map([
      ['2026-10-01', { name: '国庆节', date: '2026-10-01', offDay: true }],
      ['2026-10-10', { name: '国庆节', date: '2026-10-10', offDay: false }],
    ])
  );
});

describe('CalendarPage', () => {
  it('renders a six-week grid with the weekday header', () => {
    const { root } = mount();

    expect(root.querySelectorAll('.cal-day')).toHaveLength(42);
    expect(root.querySelectorAll('.cal-weekday')).toHaveLength(7);
    expect([...root.querySelectorAll('.cal-weekday')].map((cell) => cell.textContent)).toEqual([
      '日',
      '一',
      '二',
      '三',
      '四',
      '五',
      '六',
    ]);
  });

  it('starts the week on Monday when configured', () => {
    prefs.setCalendarWeekStart(CALENDAR_WEEK_START_MONDAY);
    const { root } = mount();

    expect(root.querySelector('.cal-weekday')!.textContent).toBe('一');
  });

  it('titles the month and marks today and the selection', () => {
    const { root } = mount();

    expect(root.querySelector('#cal-title')!.textContent).toBe('2026 年 8 月');
    const today = root.querySelector('.cal-day.is-today')!;
    expect(today.querySelector('.cal-day-number')!.textContent).toContain('7');
    expect(root.querySelector('.cal-day.is-selected')).toBe(today);
  });

  it('labels each cell with its lunar date and any festival', () => {
    const { root } = mount();
    const today = root.querySelector('.cal-day.is-today')!;
    const labels = [...today.querySelectorAll('.label-carousel-item')].map((item) => item.textContent);

    // 2026-08-07 is 六月廿五 and 立秋.
    expect(labels[0]).toBe('廿五');
    expect(labels).toContain('立秋');
  });

  it('shows 休 and 班 badges from the holiday table', () => {
    const { root, page } = mount();
    // Move to October, where the fixture has both kinds of day. With a zero-width
    // grid in jsdom the month change applies synchronously, without animating.
    root.querySelector<HTMLElement>('#cal-next')!.click();
    root.querySelector<HTMLElement>('#cal-next')!.click();

    const off = root.querySelector('.cal-day[data-date="2026-10-01"] .cal-day-badge');
    const work = root.querySelector('.cal-day[data-date="2026-10-10"] .cal-day-badge');
    expect(off?.textContent).toBe('休');
    expect(off?.classList.contains('is-off')).toBe(true);
    expect(work?.textContent).toBe('班');
    expect(work?.classList.contains('is-work')).toBe(true);
    page.stop();
  });

  it('lists the selected date with 宜 and 忌 in the footer', () => {
    const { root } = mount();
    const items = [...root.querySelectorAll('#cal-footer .label-carousel-item')];

    expect(items[0].textContent).toContain('丙午马年六月廿五');
    expect(items.some((item) => item.textContent?.startsWith('宜 '))).toBe(true);
    expect(items.some((item) => item.textContent?.startsWith('忌 '))).toBe(true);
  });

  it('moves the selection without rebuilding the grid', () => {
    const { root } = mount();
    const target = root.querySelector<HTMLElement>('.cal-day[data-date="2026-08-20"]')!;
    target.click();

    expect(target.classList.contains('is-selected')).toBe(true);
    expect(root.querySelectorAll('.cal-day.is-selected')).toHaveLength(1);
    expect(root.querySelector('#cal-footer')!.textContent).toContain('2026 年 8 月 20 日');
  });

  it('jumps back to today after visiting another month', () => {
    const { root } = mount();
    root.querySelector<HTMLElement>('#cal-next')!.click();
    expect(root.querySelector('#cal-title')!.textContent).toBe('2026 年 9 月');

    root.querySelector<HTMLElement>('#cal-today')!.click();
    expect(root.querySelector('#cal-title')!.textContent).toBe('2026 年 8 月');
    expect(root.querySelector('.cal-day.is-selected')).toBe(root.querySelector('.cal-day.is-today'));
  });

  it('rolls the year over at December', () => {
    const { root } = mount();
    for (let month = 0; month < 5; month++) {
      root.querySelector<HTMLElement>('#cal-next')!.click();
    }
    expect(root.querySelector('#cal-title')!.textContent).toBe('2027 年 1 月');
  });

  it('renders the English month title after a language switch', () => {
    prefs.setClockLanguage('en');
    const { root } = mount();

    expect(root.querySelector('#cal-title')!.textContent).toBe('August 2026');
  });

  it('hides the weather cards when weather is disabled', () => {
    const { root } = mount();

    expect(root.querySelector<HTMLElement>('#cal-weather-card')!.hidden).toBe(true);
    expect(root.querySelector<HTMLElement>('#cal-forecast-card')!.hidden).toBe(true);
    expect(root.querySelector<HTMLElement>('#cal-attribution')!.hidden).toBe(true);
  });

  it('shows the clock panel with seconds and the 12-hour marker', () => {
    prefs.setUse24Hour(false);
    const { root } = mount();

    expect(root.querySelector('#cal-time')!.textContent).toBe('01:45');
    expect(root.querySelector('#cal-seconds')!.textContent).toBe(':00');
    expect(root.querySelector('#cal-period')!.textContent).toBe('下午');
  });
});
