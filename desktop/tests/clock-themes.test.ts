// @vitest-environment jsdom
import { beforeEach, expect, it, vi } from 'vitest';
import { CLOCK_THEMES, temperature } from '../src/core/clock-themes';
import { prefs } from '../src/core/prefs';
import { sameItems, weatherItem } from '../src/ui/carousel';

beforeEach(() => localStorage.clear());
it('stores palettes and weather transitions independently for every theme', () => {
  for (const theme of CLOCK_THEMES) {
    prefs.setClockTheme(theme.id);
    expect(prefs.getClockTheme()).toBe(theme.id);
    prefs.setThemePalette({ background: '#123456', panel: '#abcdef', accent: '#345678' });
    prefs.setWeatherTransition('slide_right');
  }
  prefs.setClockTheme('classic'); prefs.setWeatherTransition('scan');
  prefs.setClockTheme('ultimate.orbit');
  expect(prefs.getWeatherTransition()).toBe('slide_right');
  expect(prefs.getThemePalette().panel).toBe('#abcdef');
  prefs.setClockTheme('classic'); expect(prefs.getWeatherTransition()).toBe('scan');
});
it('keeps each theme digit motion independent and clears overrides on reset', () => {
  prefs.setClockTheme('ultimate.orbit'); prefs.setTimeTransition('flip'); prefs.setAnimateTimeChanges(false);
  prefs.setClockTheme('ultimate.ribbon'); prefs.setTimeTransition('slide_up');
  prefs.setClockTheme('ultimate.orbit'); expect(prefs.getTimeTransition()).toBe('flip'); expect(prefs.isAnimateTimeChanges()).toBe(false);
  prefs.clearThemeOverrides(); expect(prefs.getTimeTransition()).toBe('fade'); expect(prefs.isAnimateTimeChanges()).toBe(true);
});
it('falls back safely for corrupt theme and palette storage', () => {
  localStorage.setItem('clock_prefs.web_clock_theme', '<script>');
  localStorage.setItem('clock_prefs.web_palette__classic', 'null');
  expect(prefs.getClockTheme()).toBe('classic');
  expect(prefs.getThemePalette().background).toBe('#000000');
});
it('shows updated settings when persistent storage runs out of quota', () => {
  prefs.setCustomMessage('old');
  const write = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('quota'); });
  prefs.setCustomMessage('new'); expect(prefs.getCustomMessage()).toBe('new');
  write.mockRestore(); prefs.setCustomMessage('');
});
it('refreshes a weather icon even when its localized text is unchanged', () => {
  expect(sameItems([weatherItem('City', '100', 'Sunny')], [weatherItem('City', '150', 'Sunny')])).toBe(false);
});
it('converts Celsius fixtures and preserves an unavailable temperature', () => {
  expect(temperature('0', 'fahrenheit')).toBe('32℉');
  expect(temperature('-40', 'fahrenheit')).toBe('-40℉');
  expect(temperature('--', 'fahrenheit')).toBe('--℉');
});
