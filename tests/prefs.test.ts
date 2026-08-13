/**
 * @vitest-environment jsdom
 *
 * Replaces app/src/test/java/com/clockmods/background/ClockPreferencesTest.java.
 * The Android test only covered the status-icon scale, which the web build drops,
 * so it exercises the normalizers and defaults that survived instead.
 */
import { beforeEach, describe, expect, it } from 'vitest';
import {
  argbFromHex,
  cssColor,
  DEFAULT_BACKGROUND_COLOR,
  DEFAULT_DATE_FONT_SCALE,
  DEFAULT_TIME_FONT_SCALE,
  DEFAULT_WEATHER_INTERVAL_MINUTES,
  isValidWeatherInterval,
  LANGUAGE_ENGLISH,
  LANGUAGE_TRADITIONAL,
  MAX_CUSTOM_MESSAGE_LENGTH,
  MAX_FONT_SCALE,
  MIN_FONT_SCALE,
  normalizeClockLanguage,
  normalizeCustomMessage,
  normalizeDatePattern,
  normalizeTimeTransition,
  prefs,
  TRANSITION_FADE,
  TRANSITION_FLIP,
  WEATHER_LOCATION_AUTOMATIC,
  WEATHER_LOCATION_MANUAL,
} from '../src/core/prefs';
import { DEFAULT_PATTERN_CN } from '../src/format/date-formatter';

beforeEach(() => {
  localStorage.clear();
});

describe('defaults', () => {
  it('matches the Android defaults', () => {
    expect(prefs.getTimeFontScale()).toBeCloseTo(DEFAULT_TIME_FONT_SCALE, 5);
    expect(prefs.getDateFontScale()).toBeCloseTo(DEFAULT_DATE_FONT_SCALE, 5);
    expect(prefs.getBackgroundColor()).toBe(DEFAULT_BACKGROUND_COLOR);
    expect(prefs.isUse24Hour()).toBe(true);
    expect(prefs.isShowSeconds()).toBe(true);
    expect(prefs.isShowLunar()).toBe(true);
    expect(prefs.isBlinkColon()).toBe(false);
    expect(prefs.isAnimateTimeChanges()).toBe(true);
    expect(prefs.isHourlyChimeEnabled()).toBe(true);
    expect(prefs.getHourlyChimeQuietStart()).toBe(22 * 60);
    expect(prefs.getHourlyChimeQuietEnd()).toBe(7 * 60);
    expect(prefs.getDimStartMinutes()).toBe(22 * 60);
    expect(prefs.getDimEndMinutes()).toBe(6 * 60);
    expect(prefs.getWeatherIntervalMinutes()).toBe(DEFAULT_WEATHER_INTERVAL_MINUTES);
    expect(prefs.getFontFamily()).toBe('system');
    expect(prefs.getDatePatternCn()).toBe(DEFAULT_PATTERN_CN);
  });
});

describe('font scale clamping', () => {
  it('clamps to the supported range on both read and write', () => {
    prefs.setTimeFontScale(9);
    expect(prefs.getTimeFontScale()).toBeCloseTo(MAX_FONT_SCALE, 5);
    prefs.setTimeFontScale(0.01);
    expect(prefs.getTimeFontScale()).toBeCloseTo(MIN_FONT_SCALE, 5);
    prefs.setTimeFontScale(0.75);
    expect(prefs.getTimeFontScale()).toBeCloseTo(0.75, 5);
  });
});

describe('normalizers', () => {
  it('falls back to fade for an unknown transition', () => {
    expect(normalizeTimeTransition(TRANSITION_FLIP)).toBe(TRANSITION_FLIP);
    expect(normalizeTimeTransition('nonsense')).toBe(TRANSITION_FADE);
    expect(normalizeTimeTransition(null)).toBe(TRANSITION_FADE);
  });

  it('keeps only the three supported languages', () => {
    expect(normalizeClockLanguage(LANGUAGE_ENGLISH)).toBe(LANGUAGE_ENGLISH);
    expect(normalizeClockLanguage(LANGUAGE_TRADITIONAL)).toBe(LANGUAGE_TRADITIONAL);
    expect(normalizeClockLanguage('fr')).toBe('zh-Hans');
  });

  it('trims and caps the custom message', () => {
    expect(normalizeCustomMessage('  hello  ')).toBe('hello');
    expect(normalizeCustomMessage(null)).toBe('');
    expect(normalizeCustomMessage('x'.repeat(MAX_CUSTOM_MESSAGE_LENGTH + 50))).toHaveLength(
      MAX_CUSTOM_MESSAGE_LENGTH
    );
  });

  it('rejects invalid date patterns', () => {
    expect(normalizeDatePattern('yyyy/M/d', DEFAULT_PATTERN_CN)).toBe('yyyy/M/d');
    expect(normalizeDatePattern('年月日', DEFAULT_PATTERN_CN)).toBe(DEFAULT_PATTERN_CN);
    expect(normalizeDatePattern('', DEFAULT_PATTERN_CN)).toBe(DEFAULT_PATTERN_CN);
  });

  it('accepts only the offered weather intervals', () => {
    for (const minutes of [10, 30, 60, 180, 360, 720]) {
      expect(isValidWeatherInterval(minutes)).toBe(true);
    }
    expect(isValidWeatherInterval(45)).toBe(false);
    prefs.setWeatherIntervalMinutes(45);
    expect(prefs.getWeatherIntervalMinutes()).toBe(DEFAULT_WEATHER_INTERVAL_MINUTES);
  });
});

describe('weather location mode', () => {
  it('only reports manual once a location has been chosen', () => {
    prefs.setWeatherLocationMode(WEATHER_LOCATION_MANUAL);
    expect(prefs.getWeatherLocationMode()).toBe(WEATHER_LOCATION_AUTOMATIC);

    prefs.setManualWeatherLocation('101280601', '广东省', '深圳市', '宝安', 22.75, 113.85);
    expect(prefs.getWeatherLocationMode()).toBe(WEATHER_LOCATION_MANUAL);
    expect(prefs.getWeatherCity()).toBe('深圳市');
    expect(prefs.getWeatherLatitude()).toBeCloseTo(22.75, 5);
  });
});

describe('weather configuration', () => {
  it('requires either a full credential set or a proxy', () => {
    expect(prefs.isWeatherConfigured()).toBe(false);
    prefs.setQWeatherCredentials('devapi.qweather.com', 'replace-with-id', 'p', 'k', '');
    expect(prefs.isWeatherConfigured()).toBe(false);
    prefs.setQWeatherCredentials('devapi.qweather.com', 'cred', 'proj', 'key', '');
    expect(prefs.isWeatherConfigured()).toBe(true);
    prefs.setQWeatherCredentials('', '', '', '', 'https://proxy.example.com/');
    expect(prefs.isWeatherConfigured()).toBe(true);
    // A trailing slash would double up when paths are appended.
    expect(prefs.getQWeatherProxy()).toBe('https://proxy.example.com');
  });
});

describe('restoreDefaults', () => {
  it('resets visual settings but preserves credentials', () => {
    prefs.setTimeFontScale(1.4);
    prefs.setCustomMessage('keep me out');
    prefs.setWeatherEnabled(true);
    prefs.setQWeatherCredentials('host', 'cred', 'proj', 'key', '');

    prefs.restoreDefaults();

    expect(prefs.getTimeFontScale()).toBeCloseTo(DEFAULT_TIME_FONT_SCALE, 5);
    expect(prefs.getCustomMessage()).toBe('');
    expect(prefs.isWeatherEnabled()).toBe(false);
    expect(prefs.getQWeatherCredentialId()).toBe('cred');
  });
});

describe('color conversion', () => {
  it('round-trips opaque colors', () => {
    expect(cssColor(0xff000000)).toBe('#000000');
    expect(cssColor(0xffffffff)).toBe('#ffffff');
    expect(cssColor(0xff38bdf8)).toBe('#38bdf8');
    expect(argbFromHex('#38bdf8')).toBe(0xff38bdf8);
    expect(cssColor(argbFromHex('#101418'))).toBe('#101418');
  });

  it('emits rgba for translucent colors', () => {
    expect(cssColor(0x80000000)).toBe('rgba(0, 0, 0, 0.502)');
  });
});
