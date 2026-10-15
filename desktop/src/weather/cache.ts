/**
 * Weather and forecast caches.
 *
 * Ported from com.clockmods.weather.WeatherRepository and
 * DailyForecastRepository: same invalidation rules (source, location id and the
 * language the text was fetched in), backed by localStorage instead of
 * SharedPreferences.
 */
import { dateKey, zonedFields } from '../core/zoned-time';
import { prefs } from '../core/prefs';
import { findForecastByDate } from './models';
import type { DailyForecastData, WeatherDisplayData } from './models';

const WEATHER_KEY = 'weather_cache.data';
const FORECAST_KEY = 'daily_forecast_cache.data';

/** A forecast older than this is refetched even on the same day. */
export const FORECAST_MAX_AGE_MS = 6 * 60 * 60 * 1000;

interface CachedWeather extends WeatherDisplayData {
  source: string;
  lang: string;
}

interface CachedForecast extends DailyForecastData {
  source: string;
  lang: string;
}

function read<T>(key: string): T | null {
  try {
    const value = localStorage.getItem(key);
    return value ? (JSON.parse(value) as T) : null;
  } catch {
    return null;
  }
}

function write(key: string, value: unknown): void {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // A full quota only costs us the cache, not the feature.
  }
}

export function getCachedWeather(source: string, locationId: string): WeatherDisplayData | null {
  const cached = read<CachedWeather>(WEATHER_KEY);
  if (!cached) return null;
  if (cached.source !== source) return null;
  if (source === 'manual' && cached.locationId !== locationId) return null;
  // Cached text is in the language it was fetched in; drop it after a language
  // switch so the UI never shows stale, wrong-language weather.
  if (cached.lang !== prefs.getClockLanguage()) return null;
  const { source: _source, lang: _lang, ...data } = cached;
  return data;
}

export function saveWeather(data: WeatherDisplayData, source: string): void {
  write(WEATHER_KEY, { ...data, source, lang: prefs.getClockLanguage() });
}

export function getCachedForecast(
  source: string,
  locationId: string,
  now: number,
  timeZone: string
): DailyForecastData | null {
  const cached = read<CachedForecast>(FORECAST_KEY);
  if (!cached) return null;
  if (cached.source !== source) return null;
  if (source === 'manual' && cached.locationId !== locationId) return null;
  if (cached.lang !== prefs.getClockLanguage()) return null;
  const { source: _source, lang: _lang, ...data } = cached;
  return isForecastReusable(data, now, timeZone) ? data : null;
}

export function saveForecast(data: DailyForecastData, source: string): void {
  write(FORECAST_KEY, { ...data, source, lang: prefs.getClockLanguage() });
}

/**
 * A cached forecast is reusable only while it is fresh, was fetched on the same
 * calendar day in the app's zone, and still covers today plus the next two days.
 */
export function isForecastReusable(
  data: DailyForecastData | null,
  now: number,
  timeZone: string
): boolean {
  if (!data) return false;
  if (data.updatedAt > now || now - data.updatedAt > FORECAST_MAX_AGE_MS) return false;
  const updated = zonedFields(data.updatedAt, timeZone);
  const current = zonedFields(now, timeZone);
  if (
    updated.year !== current.year ||
    updated.month0 !== current.month0 ||
    updated.day !== current.day
  ) {
    return false;
  }
  const cursor = new Date(Date.UTC(current.year, current.month0, current.day));
  for (let offset = 0; offset < 3; offset++) {
    const key = dateKey(cursor.getUTCFullYear(), cursor.getUTCMonth(), cursor.getUTCDate());
    if (!findForecastByDate(data, key)) return false;
    cursor.setUTCDate(cursor.getUTCDate() + 1);
  }
  return true;
}
