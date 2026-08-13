/**
 * Weather and 3-day forecast controllers.
 *
 * Ported from com.clockmods.weather.WeatherController and
 * DailyForecastController: the same cache-then-refresh flow, generation guard
 * against stale responses, 15s location timeout and interval-based rescheduling.
 * Android's LocationManager becomes `navigator.geolocation`, and the background
 * executor becomes plain async/await.
 */
import { prefs, WEATHER_LOCATION_AUTOMATIC, WEATHER_LOCATION_MANUAL } from '../core/prefs';
import { apiLang, t } from '../core/i18n';
import { timeSource } from '../core/time-source';
import { zonedFields } from '../core/zoned-time';
import {
  FORECAST_MAX_AGE_MS,
  getCachedForecast,
  getCachedWeather,
  saveForecast,
  saveWeather,
} from './cache';
import { satisfies } from './models';
import type { DailyForecastState, WeatherState } from './models';
import {
  fetchDailyByCoordinates,
  fetchDailyByLocationId,
  fetchWeatherByCoordinates,
  fetchWeatherByLocationId,
} from './qweather';
import type { QWeatherConfig } from './qweather';
import { catalogIfLoaded, findById, loadCatalog } from './city-catalog';

const LOCATION_TIMEOUT_MS = 15000;

function currentConfig(): QWeatherConfig {
  return {
    apiHost: prefs.getQWeatherApiHost(),
    credentialId: prefs.getQWeatherCredentialId(),
    projectId: prefs.getQWeatherProjectId(),
    privateKeyBase64: prefs.getQWeatherPrivateKey(),
    proxy: prefs.getQWeatherProxy(),
    lang: apiLang(),
  };
}

function nowSeconds(): number {
  return Math.floor(timeSource.now() / 1000);
}

function describeError(error: unknown): string {
  if (error instanceof Error && error.message.trim()) return error.message.trim();
  return String(error);
}

function isManualLocation(): boolean {
  return prefs.getWeatherLocationMode() === WEATHER_LOCATION_MANUAL;
}

/**
 * Browser geolocation with the same 15s ceiling the Android controller applied to
 * its location listener.
 */
function requestPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('geolocation unavailable'));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: false,
      timeout: LOCATION_TIMEOUT_MS,
      // A 10-minute-old fix was good enough for the Android path too.
      maximumAge: 10 * 60 * 1000,
    });
  });
}

export type WeatherListener = (state: WeatherState) => void;
export type ForecastListener = (state: DailyForecastState) => void;

export class WeatherController {
  private running = false;
  private generation = 0;
  private intervalMinutes = 30;
  private timer: number | null = null;

  /**
   * @param detailedOverride forces the detailed payload regardless of the user
   *   setting (the calendar dashboard always needs feels-like).
   */
  constructor(
    private readonly listener: WeatherListener,
    private readonly detailedOverride: boolean | null = null
  ) {}

  start(intervalMinutes: number): void {
    this.clearTimer();
    this.generation++;
    this.running = true;
    this.intervalMinutes = intervalMinutes;
    const cached = this.cached();
    if (cached) this.listener({ status: 'success', data: cached, message: null });
    const incompatible = cached !== null && !satisfies(cached, this.detailedRequired());
    void this.refreshIfNeeded(cached, incompatible);
  }

  refreshNow(): void {
    void this.refreshIfNeeded(this.cached(), true);
  }

  stop(): void {
    this.running = false;
    this.generation++;
    this.clearTimer();
  }

  private detailedRequired(): boolean {
    return this.detailedOverride ?? prefs.isWeatherDetailed();
  }

  private cached() {
    const source = isManualLocation() ? WEATHER_LOCATION_MANUAL : WEATHER_LOCATION_AUTOMATIC;
    return getCachedWeather(source, prefs.getWeatherLocationId());
  }

  private async refreshIfNeeded(
    cached: ReturnType<typeof getCachedWeather>,
    force: boolean
  ): Promise<void> {
    if (!this.running) return;
    const maxAge = this.intervalMinutes * 60 * 1000;
    const age = cached ? Date.now() - cached.updatedAt : Number.POSITIVE_INFINITY;
    if (!force && cached && age < maxAge) {
      this.schedule(maxAge - age);
      return;
    }
    if (!prefs.isWeatherConfigured()) {
      this.listener({ status: 'config_error', data: null, message: t('weather_not_configured') });
      return;
    }
    this.listener({ status: 'loading', data: cached, message: t('weather_fetching') });
    const generation = ++this.generation;
    try {
      const data = isManualLocation() ? await this.fetchManual() : await this.fetchAutomatic();
      saveWeather(data, isManualLocation() ? WEATHER_LOCATION_MANUAL : WEATHER_LOCATION_AUTOMATIC);
      if (!this.running || generation !== this.generation) return;
      this.listener({ status: 'success', data, message: null });
      this.schedule(this.intervalMinutes * 60 * 1000);
    } catch (error) {
      if (!this.running || generation !== this.generation) return;
      const permissionDenied =
        typeof GeolocationPositionError !== 'undefined' &&
        error instanceof GeolocationPositionError &&
        error.code === error.PERMISSION_DENIED;
      this.listener({
        status: permissionDenied ? 'permission_denied' : 'network_error',
        data: cached,
        message: permissionDenied
          ? t('weather_permission_denied')
          : t('weather_fetch_failed', describeError(error)),
      });
      this.schedule(this.intervalMinutes * 60 * 1000);
    }
  }

  private async fetchAutomatic() {
    const position = await requestPosition();
    return fetchWeatherByCoordinates(
      currentConfig(),
      position.coords.latitude,
      position.coords.longitude,
      this.detailedRequired(),
      nowSeconds()
    );
  }

  private async fetchManual() {
    const locationId = prefs.getWeatherLocationId();
    const detailed = this.detailedRequired();
    const english = prefs.isClockUseEnglish();
    let latitude = prefs.getWeatherLatitude();
    let longitude = prefs.getWeatherLongitude();
    let city = prefs.getWeatherCity();
    let district = prefs.getWeatherDistrict();

    // Load the catalog when coordinates are missing (needed for warnings and air
    // quality), or to localize the stored Chinese names for an English UI.
    const needCoordinates = detailed && (!Number.isFinite(latitude) || !Number.isFinite(longitude));
    if (english || needCoordinates) {
      const catalog = catalogIfLoaded() ?? (await loadCatalog().catch(() => null));
      const entry = catalog ? findById(catalog, locationId) : null;
      if (entry) {
        if (needCoordinates) {
          latitude = entry.latitude;
          longitude = entry.longitude;
        }
        if (english) {
          city = entry.cityEn;
          district = entry.districtEn;
        }
      }
    }
    return fetchWeatherByLocationId(
      currentConfig(),
      locationId,
      city,
      district,
      latitude,
      longitude,
      detailed,
      nowSeconds()
    );
  }

  private schedule(delay: number): void {
    this.clearTimer();
    if (!this.running) return;
    this.timer = window.setTimeout(() => this.refreshNow(), Math.max(1000, delay));
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }
}

export class DailyForecastController {
  private running = false;
  private generation = 0;
  private timer: number | null = null;

  constructor(private readonly listener: ForecastListener) {}

  start(): void {
    this.clearTimer();
    this.running = true;
    this.generation++;
    const cached = this.cached();
    if (cached) {
      this.listener({ status: 'success', data: cached, message: null });
      this.schedule(this.nextRefreshDelay(cached.updatedAt));
      return;
    }
    this.refreshNow();
  }

  refreshNow(): void {
    if (!this.running) return;
    void this.refresh();
  }

  stop(): void {
    this.running = false;
    this.generation++;
    this.clearTimer();
  }

  private cached() {
    const source = isManualLocation() ? WEATHER_LOCATION_MANUAL : WEATHER_LOCATION_AUTOMATIC;
    return getCachedForecast(
      source,
      prefs.getWeatherLocationId(),
      Date.now(),
      prefs.getTimeZoneId()
    );
  }

  private async refresh(): Promise<void> {
    if (!prefs.isWeatherConfigured()) {
      this.listener({ status: 'config_error', data: null, message: t('weather_not_configured') });
      return;
    }
    this.listener({ status: 'loading', data: null, message: t('forecast_fetching') });
    const generation = ++this.generation;
    try {
      const data = isManualLocation()
        ? await fetchDailyByLocationId(
            currentConfig(),
            prefs.getWeatherLocationId(),
            prefs.getWeatherCity(),
            prefs.getWeatherDistrict(),
            nowSeconds()
          )
        : await this.fetchAutomatic();
      saveForecast(data, isManualLocation() ? WEATHER_LOCATION_MANUAL : WEATHER_LOCATION_AUTOMATIC);
      if (!this.running || generation !== this.generation) return;
      this.listener({ status: 'success', data, message: null });
      this.schedule(this.nextRefreshDelay(data.updatedAt));
    } catch (error) {
      if (!this.running || generation !== this.generation) return;
      this.listener({
        status: 'network_error',
        data: null,
        message: t('forecast_fetch_failed', describeError(error)),
      });
      this.schedule(FORECAST_MAX_AGE_MS);
    }
  }

  private async fetchAutomatic() {
    const position = await requestPosition();
    return fetchDailyByCoordinates(
      currentConfig(),
      position.coords.latitude,
      position.coords.longitude,
      nowSeconds()
    );
  }

  /** Refresh when the cache ages out, or at the next local midnight — whichever comes first. */
  private nextRefreshDelay(updatedAt: number): number {
    const now = Date.now();
    const ageDelay = Math.max(1000, FORECAST_MAX_AGE_MS - (now - updatedAt));
    const zone = prefs.getTimeZoneId();
    const fields = zonedFields(now, zone);
    const millisIntoDay =
      ((fields.hour * 60 + fields.minute) * 60 + fields.second) * 1000 + (now % 1000);
    const untilMidnight = Math.max(1000, 24 * 60 * 60 * 1000 - millisIntoDay);
    return Math.min(ageDelay, untilMidnight);
  }

  private schedule(delay: number): void {
    this.clearTimer();
    if (!this.running) return;
    this.timer = window.setTimeout(() => this.refreshNow(), Math.max(1000, delay));
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }
}
