/**
 * @vitest-environment jsdom
 *
 * Ported from app/src/testPro/java/com/clockmods/pro/ProTimerFragmentTest.java,
 * plus behavioural coverage of the pomodoro cycle, the stopwatch and the alarm's
 * next-trigger arithmetic (AlarmScheduler.nextTrigger).
 */
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { customDurationMillis, TimerPage } from '../src/pages/timer';
import { StopwatchPage } from '../src/pages/stopwatch';
import { nextTrigger } from '../src/pages/alarm';

beforeAll(() => {
  HTMLCanvasElement.prototype.getContext = () => null;
  // Neither exists in jsdom; the pages only use them for alerts.
  vi.stubGlobal('AudioContext', undefined);
});

beforeEach(() => {
  localStorage.clear();
  document.body.innerHTML = '<section class="page" data-page="pomodoro"></section>';
});

function host(): HTMLElement {
  return document.querySelector<HTMLElement>('.page')!;
}

describe('customDurationMillis', () => {
  it('accepts the maximum value', () => {
    expect(customDurationMillis(99, 59, 59)).toBe(359_999_000);
  });

  it('rejects zero and out-of-range values', () => {
    expect(customDurationMillis(0, 0, 0)).toBe(0);
    expect(customDurationMillis(100, 0, 0)).toBe(0);
    expect(customDurationMillis(1, 60, 0)).toBe(0);
    expect(customDurationMillis(1, 0, 60)).toBe(0);
  });
});

describe('TimerPage', () => {
  it('starts a pomodoro at 25 minutes in the focus phase', () => {
    const page = new TimerPage(host(), 'pomodoro');
    page.refreshSettings();

    expect(host().querySelector('.timer-display')!.textContent).toBe('25:00');
    expect(host().querySelector('.timer-phase')!.textContent).toBe('专注');
    expect(host().querySelector('[data-act="toggle"]')!.textContent).toBe('开始');
  });

  it('cycles focus → short break → long break when skipping', () => {
    const page = new TimerPage(host(), 'pomodoro');
    page.refreshSettings();
    const skip = host().querySelector<HTMLElement>('[data-act="skip"]')!;
    const display = host().querySelector('.timer-display')!;
    const phase = host().querySelector('.timer-phase')!;

    skip.click();
    expect(display.textContent).toBe('05:00');
    expect(phase.textContent).toBe('短休息');

    skip.click();
    expect(display.textContent).toBe('15:00');
    expect(phase.textContent).toBe('长休息');

    skip.click();
    expect(display.textContent).toBe('25:00');
    expect(phase.textContent).toBe('专注');
  });

  it('counts down from the stored deadline and persists it', () => {
    vi.useFakeTimers();
    vi.setSystemTime(1_000_000);
    const page = new TimerPage(host(), 'countdown');
    page.refreshSettings();
    expect(host().querySelector('.timer-display')!.textContent).toBe('00:10:00');

    host().querySelector<HTMLElement>('[data-act="toggle"]')!.click();
    expect(localStorage.getItem('pro_timers.countdown_running')).toBe('true');
    expect(Number(localStorage.getItem('pro_timers.countdown_deadline'))).toBe(1_000_000 + 600_000);

    vi.setSystemTime(1_000_000 + 5_000);
    page.start();
    expect(host().querySelector('.timer-display')!.textContent).toBe('00:09:55');
    page.stop();
    vi.useRealTimers();
  });

  it('offers the documented presets per mode', () => {
    const pomodoro = new TimerPage(host(), 'pomodoro');
    pomodoro.refreshSettings();
    expect([...host().querySelectorAll('.timer-preset')].map((b) => b.textContent)).toEqual([
      '5 分钟',
      '15 分钟',
      '25 分钟',
    ]);

    document.body.innerHTML = '<section class="page" data-page="countdown"></section>';
    const countdown = new TimerPage(host(), 'countdown');
    countdown.refreshSettings();
    expect([...host().querySelectorAll('.timer-preset')].map((b) => b.textContent)).toEqual([
      '5 分钟',
      '10 分钟',
      '30 分钟',
      '60 分钟',
      '自定义',
    ]);
  });

  it('labels every timer action button', () => {
    new TimerPage(host(), 'pomodoro').refreshSettings();

    expect(host().querySelector('[data-act="reset"]')?.textContent).toBe('重置');
    expect(host().querySelector('[data-act="toggle"]')?.textContent).toBe('开始');
    expect(host().querySelector('[data-act="skip"]')?.textContent).toBe('跳过');

    new TimerPage(host(), 'countdown').refreshSettings();
    expect(host().querySelector('[data-act="reset"]')?.textContent).toBe('重置');
  });

  it('resets a pomodoro back to the first phase', () => {
    const page = new TimerPage(host(), 'pomodoro');
    page.refreshSettings();
    host().querySelector<HTMLElement>('[data-act="skip"]')!.click();
    host().querySelector<HTMLElement>('[data-act="reset"]')!.click();

    expect(host().querySelector('.timer-display')!.textContent).toBe('25:00');
    expect(host().querySelector('.timer-phase')!.textContent).toBe('专注');
  });
});

describe('StopwatchPage', () => {
  it('formats elapsed time with centiseconds and records laps', () => {
    vi.useFakeTimers();
    const page = new StopwatchPage(host());
    page.refreshSettings();
    expect(host().querySelector('.timer-display')!.textContent).toBe('00:00.00');

    host().querySelector<HTMLElement>('[data-act="toggle"]')!.click();
    vi.advanceTimersByTime(1234);
    page.start();

    const shown = host().querySelector('.timer-display')!.textContent!;
    expect(shown).toMatch(/^00:01\.\d\d$/);

    host().querySelector<HTMLElement>('[data-act="lap"]')!.click();
    host().querySelector<HTMLElement>('[data-act="lap"]')!.click();
    const laps = [...host().querySelectorAll('.stopwatch-laps li')];
    expect(laps).toHaveLength(2);
    // Newest first.
    expect(laps[0].textContent).toContain('第 2 圈');
    page.stop();
    vi.useRealTimers();
  });

  it('clears laps and elapsed time on reset', () => {
    const page = new StopwatchPage(host());
    page.refreshSettings();
    host().querySelector<HTMLElement>('[data-act="toggle"]')!.click();
    host().querySelector<HTMLElement>('[data-act="lap"]')!.click();
    host().querySelector<HTMLElement>('[data-act="reset"]')!.click();

    expect(host().querySelector('.timer-display')!.textContent).toBe('00:00.00');
    expect(host().querySelectorAll('.stopwatch-laps li')).toHaveLength(0);
  });
});

describe('nextTrigger', () => {
  it('picks today when the time is still ahead', () => {
    const now = new Date(2026, 7, 7, 6, 0, 0).getTime();
    expect(nextTrigger(7, 30, now)).toBe(new Date(2026, 7, 7, 7, 30, 0, 0).getTime());
  });

  it('rolls to tomorrow once the time has passed', () => {
    const now = new Date(2026, 7, 7, 8, 0, 0).getTime();
    expect(nextTrigger(7, 30, now)).toBe(new Date(2026, 7, 8, 7, 30, 0, 0).getTime());
  });

  it('treats the exact minute as already passed', () => {
    const now = new Date(2026, 7, 7, 7, 30, 0, 0).getTime();
    expect(nextTrigger(7, 30, now)).toBe(new Date(2026, 7, 8, 7, 30, 0, 0).getTime());
  });
});
