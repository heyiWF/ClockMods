import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { WeatherDisplayData, DailyForecastData } from '../src/weather/models';

const mocks = vi.hoisted(() => ({
  mode: 'manual', weather: vi.fn(), forecast: vi.fn(), saveWeather: vi.fn(), saveForecast: vi.fn(),
}));
vi.mock('../src/core/prefs', () => ({
  WEATHER_LOCATION_MANUAL: 'manual', WEATHER_LOCATION_AUTOMATIC: 'automatic',
  prefs: {
    getWeatherLocationMode: () => mocks.mode, getWeatherLocationId: () => 'city',
    getWeatherCity: () => 'City', getWeatherDistrict: () => 'District',
    getWeatherLatitude: () => 23, getWeatherLongitude: () => 113,
    isWeatherConfigured: () => true, isWeatherDetailed: () => false,
    isClockUseEnglish: () => false, getTimeZoneId: () => '',
    getQWeatherApiHost: () => '', getQWeatherCredentialId: () => '',
    getQWeatherProjectId: () => '', getQWeatherPrivateKey: () => '', getQWeatherProxy: () => '',
  },
}));
vi.mock('../src/core/i18n', () => ({ apiLang: () => 'zh', t: (key: string) => key }));
vi.mock('../src/core/time-source', () => ({ timeSource: { now: () => Date.now() } }));
vi.mock('../src/core/zoned-time', () => ({ zonedFields: () => ({ hour: 12, minute: 0, second: 0 }) }));
vi.mock('../src/weather/cache', () => ({
  FORECAST_MAX_AGE_MS: 3600000, getCachedWeather: () => null, getCachedForecast: () => null,
  saveWeather: mocks.saveWeather, saveForecast: mocks.saveForecast,
}));
vi.mock('../src/weather/city-catalog', () => ({ catalogIfLoaded: () => null, findById: vi.fn(), loadCatalog: vi.fn() }));
vi.mock('../src/weather/qweather', () => ({
  fetchWeatherByLocationId: mocks.weather, fetchWeatherByCoordinates: mocks.weather,
  fetchDailyByLocationId: mocks.forecast, fetchDailyByCoordinates: mocks.forecast,
}));
import { DailyForecastController, WeatherController } from '../src/weather/controller';

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(done => { resolve = done; });
  return { promise, resolve };
}
const flush = async () => { for (let i = 0; i < 8; i++) await Promise.resolve(); };
const data = (id: string) => ({ locationId: id, city: '', district: '', updatedAt: Date.now(),
  text: '晴', icon: '100', temperature: '25', detail: null, entries: [] });

beforeEach(() => {
  vi.useFakeTimers(); vi.clearAllMocks(); mocks.mode = 'manual';
  vi.stubGlobal('window', { setTimeout });
});
afterEach(() => { vi.clearAllTimers(); vi.useRealTimers(); vi.unstubAllGlobals(); });

for (const kind of ['weather', 'forecast'] as const) {
  describe(kind + ' request lifecycle', () => {
    const create = (listener = vi.fn()) => kind === 'weather'
      ? new WeatherController(listener) : new DailyForecastController(listener);
    const fetch = () => kind === 'weather' ? mocks.weather : mocks.forecast;
    const save = () => kind === 'weather' ? mocks.saveWeather : mocks.saveForecast;

    it('does not overwrite the cache after stopping', async () => {
      const pending = deferred<WeatherDisplayData & DailyForecastData>();
      fetch().mockReturnValueOnce(pending.promise);
      const listener = vi.fn(); const controller = create(listener);
      controller.start(30); controller.stop();
      pending.resolve(data('stale')); await flush();
      expect(save()).not.toHaveBeenCalled();
      expect(listener.mock.calls.filter(([state]) => state.status === 'success')).toHaveLength(0);
      expect(vi.getTimerCount()).toBe(0);
    });

    it('keeps the newer response when requests complete out of order', async () => {
      const old = deferred<WeatherDisplayData & DailyForecastData>();
      const current = deferred<WeatherDisplayData & DailyForecastData>();
      fetch().mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise);
      const controller = create(); controller.start(30); controller.refreshNow();
      current.resolve(data('new')); await flush();
      old.resolve(data('old')); await flush();
      expect(save()).toHaveBeenCalledTimes(1);
      expect(save()).toHaveBeenCalledWith(expect.objectContaining({ locationId: 'new' }), 'manual');
      controller.stop();
    });

    it('persists the source used for the request, even if preferences change', async () => {
      const pending = deferred<WeatherDisplayData & DailyForecastData>();
      fetch().mockReturnValueOnce(pending.promise);
      const controller = create(); controller.start(30); mocks.mode = 'automatic';
      pending.resolve(data('manual-city')); await flush();
      expect(save()).toHaveBeenCalledWith(expect.objectContaining({ locationId: 'manual-city' }), 'manual');
      controller.stop();
    });
  });
}
