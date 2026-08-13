/**
 * @vitest-environment jsdom
 *
 * End-to-end render of the clock page against the real markup from index.html:
 * every setting combination produces the rows and characters the Android build
 * drew. Geometry is still out of scope (jsdom does not shape text), but the
 * render → layout path itself is exercised.
 */
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { prefs, MODE_COLOR, TRANSITION_FADE } from '../src/core/prefs';
import { timeSource } from '../src/core/time-source';
import { ClockPage } from '../src/pages/clock';

const CLOCK_MARKUP = `
  <section class="page" data-page="clock">
    <div class="clock-bg" id="clock-bg"></div>
    <div class="clock-dim" id="clock-dim" hidden></div>
    <div class="clock-stage" id="clock-stage">
      <div class="clock-lines" id="clock-lines">
        <div class="supporting-row" id="clock-date" hidden></div>
        <div class="supporting-row" id="clock-lunar" hidden></div>
        <div class="clock-time" id="clock-time"></div>
        <div class="supporting-row carousel-row" id="clock-weather" hidden></div>
        <div class="supporting-row carousel-row" id="clock-detail" hidden></div>
      </div>
    </div>
    <a class="weather-attribution" id="clock-attribution" href="#" hidden></a>
  </section>`;

/** 2026-08-07 13:45:07 in Shanghai — a Friday, 农历六月廿五. */
const INSTANT = Date.UTC(2026, 7, 7, 5, 45, 7);

/** jsdom reports 0x0 for every element, so the stage is given real dimensions. */
function sizeStage(root: HTMLElement, width: number, height: number): void {
  const stage = root.querySelector('#clock-stage')!;
  Object.defineProperty(stage, 'clientWidth', { value: width, configurable: true });
  Object.defineProperty(stage, 'clientHeight', { value: height, configurable: true });
}

function mount(
  orientation: 'landscape' | 'portrait' = 'landscape'
): { root: HTMLElement; page: ClockPage; openSettings: () => void } {
  document.body.innerHTML = CLOCK_MARKUP;
  const root = document.querySelector<HTMLElement>('.page')!;
  if (orientation === 'landscape') sizeStage(root, 1280, 720);
  else sizeStage(root, 400, 800);
  const openSettings = vi.fn();
  return { root, page: new ClockPage(root, openSettings), openSettings };
}

beforeAll(() => {
  // jsdom ships none of these, and the clock observes its own stage.
  vi.stubGlobal(
    'ResizeObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    }
  );
  // Without the optional `canvas` package jsdom logs a "not implemented" error
  // for every getContext call; the measurement layer already falls back when it
  // returns null, so silence the noise.
  HTMLCanvasElement.prototype.getContext = () => null;
});

beforeEach(() => {
  localStorage.clear();
  vi.spyOn(timeSource, 'now').mockReturnValue(INSTANT);
  prefs.setTimeZoneId('Asia/Shanghai');
  prefs.setBackgroundMode(MODE_COLOR);
  prefs.setTimeTransition(TRANSITION_FADE);
});

describe('ClockPage', () => {
  it('renders time, date and lunar rows for the default settings', async () => {
    const { root, page } = mount();
    await page.refreshSettings();

    expect(root.querySelector('#clock-time')!.textContent).toBe('13:45:07');
    // Default Chinese pattern with pangu spacing, plus the lunar date appended:
    // landscape keeps both on one line, so the separate lunar row stays hidden.
    expect(root.querySelector('#clock-date')!.textContent).toBe(
      '2026 年 8 月 7 日 星期五 丙午[马]年六月廿五'
    );
    expect(root.querySelector<HTMLElement>('#clock-lunar')!.hidden).toBe(true);
    page.stop();
  });

  it('appends the lunar date on the same line in landscape', async () => {
    const { root, page } = mount();
    prefs.setShowLunar(true);
    prefs.setDateLunarDualLine(false);
    await page.refreshSettings();

    expect(root.querySelector('#clock-date')!.textContent).toContain('丙午[马]年六月廿五');
    page.stop();
  });

  it('splits date and lunar onto two rows when dual-line is enabled', async () => {
    // A portrait stage forces the split regardless of the landscape option.
    const { root, page } = mount('portrait');
    prefs.setShowLunar(true);
    await page.refreshSettings();

    expect(root.querySelector<HTMLElement>('#clock-lunar')!.hidden).toBe(false);
    expect(root.querySelector('#clock-lunar')!.textContent).toBe('丙午[马]年六月廿五');
    expect(root.querySelector('#clock-date')!.textContent).toBe('2026 年 8 月 7 日 星期五');
    page.stop();
  });

  it('hides the lunar row when the setting is off', async () => {
    const { root, page } = mount();
    prefs.setShowLunar(false);
    await page.refreshSettings();

    expect(root.querySelector('#clock-date')!.textContent).toBe('2026 年 8 月 7 日 星期五');
    expect(root.querySelector<HTMLElement>('#clock-lunar')!.hidden).toBe(true);
    page.stop();
  });

  it('renders 12-hour time with the localized period marker', async () => {
    const { root, page } = mount();
    prefs.setUse24Hour(false);
    await page.refreshSettings();

    expect(root.querySelector('.time-main')!.textContent).toBe('01:45:07');
    expect(root.querySelector('.time-period')!.textContent).toBe('下午');
    page.stop();
  });

  it('moves the seconds into their own run when small seconds are enabled', async () => {
    const { root, page } = mount();
    prefs.setSmallSeconds(true);
    await page.refreshSettings();

    expect(root.querySelector('.time-main')!.textContent).toBe('13:45');
    expect(root.querySelector('.time-seconds')!.textContent).toBe('07');
    page.stop();
  });

  it('drops the seconds entirely when hidden', async () => {
    const { root, page } = mount();
    prefs.setShowSeconds(false);
    await page.refreshSettings();

    expect(root.querySelector('.time-main')!.textContent).toBe('13:45');
    page.stop();
  });

  it('stacks hours, minutes and seconds in portrait when enabled', async () => {
    const { root, page } = mount('portrait');
    prefs.setPortraitStacked(true);
    await page.refreshSettings();

    const rows = root.querySelectorAll('#clock-time .stacked-row');
    expect(rows).toHaveLength(3);
    expect([...rows].map((row) => row.textContent)).toEqual(['13', '45', '07']);
    page.stop();
  });

  it('renders the English pattern and AM/PM after a language switch', async () => {
    const { root, page } = mount();
    prefs.setClockLanguage('en');
    prefs.setUse24Hour(false);
    await page.refreshSettings();

    // The lunar date is deliberately locale-independent and stays Chinese, as it
    // did on Android (see the LocaleManager class comment).
    expect(root.querySelector('#clock-date')!.textContent).toBe(
      '2026/8/7 Friday 丙午[马]年六月廿五'
    );
    expect(root.querySelector('.time-period')!.textContent).toBe('PM');
    page.stop();
  });

  it('applies the dim overlay only for a scheduled image background', async () => {
    const { root, page } = mount();
    const dim = root.querySelector<HTMLElement>('#clock-dim')!;

    prefs.setDimBackground(true);
    await page.refreshSettings();
    // Colour mode never dims, however the dim options are set.
    expect(dim.hidden).toBe(true);

    prefs.setBackgroundMode('image');
    await page.refreshSettings();
    expect(dim.hidden).toBe(false);

    prefs.setDimBackground(false);
    prefs.setScheduleDimBackground(true);
    prefs.setDimStartMinutes(22 * 60);
    prefs.setDimEndMinutes(6 * 60);
    await page.refreshSettings();
    // 13:45 sits outside the 22:00-06:00 window.
    expect(dim.hidden).toBe(true);

    prefs.setDimStartMinutes(13 * 60);
    prefs.setDimEndMinutes(14 * 60);
    await page.refreshSettings();
    expect(dim.hidden).toBe(false);
    page.stop();
  });

  it('shows the custom message on the weather line even without weather', async () => {
    const { root, page } = mount();
    prefs.setCustomMessage('晚安');
    await page.refreshSettings();

    expect(root.querySelector<HTMLElement>('#clock-weather')!.hidden).toBe(false);
    expect(root.querySelector('#clock-weather')!.textContent).toContain('晚安');
    page.stop();
  });

  it('opens settings on a double tap', async () => {
    const { root, page, openSettings } = mount();
    await page.refreshSettings();
    const stage = root.querySelector('#clock-stage')!;

    // jsdom has no PointerEvent constructor; the listener only reads the type.
    stage.dispatchEvent(new Event('pointerup', { bubbles: true }));
    stage.dispatchEvent(new Event('pointerup', { bubbles: true }));

    expect(openSettings).toHaveBeenCalledTimes(1);
    page.stop();
  });
});
