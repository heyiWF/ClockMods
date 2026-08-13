/** Ported from com.clockmods.weather.WeatherModels. */

export interface WeatherDetail {
  /** 体感温度，摄氏度 */
  feelsLike: string | null;
  /** 相对湿度，百分比 */
  humidity: string | null;
  /** 风向 */
  windDir: string | null;
  /** 风力等级 */
  windScale: string | null;
  /** 降水量，毫米 */
  precip: string | null;
  /** 预警事件名称，可能为空 */
  warning: string | null;
  /** 空气质量指数值，可能为空 */
  aqiValue: string | null;
  /** 空气质量类别，可能为空 */
  aqiCategory: string | null;
}

export interface WeatherDisplayData {
  locationId: string;
  city: string;
  district: string;
  text: string;
  icon: string;
  temperature: string;
  updatedAt: number;
  detail: WeatherDetail | null;
}

export interface DailyForecast {
  fxDate: string;
  tempMin: string;
  tempMax: string;
  iconDay: string;
  textDay: string;
  windDirDay: string;
  windScaleDay: string;
  humidity: string;
}

export interface DailyForecastData {
  locationId: string;
  city: string;
  district: string;
  updatedAt: number;
  entries: DailyForecast[];
}

export type Status =
  | 'idle'
  | 'loading'
  | 'success'
  | 'permission_denied'
  | 'location_unavailable'
  | 'network_error'
  | 'api_error'
  | 'config_error';

export interface WeatherState {
  status: Status;
  data: WeatherDisplayData | null;
  message: string | null;
}

export interface DailyForecastState {
  status: Status;
  data: DailyForecastData | null;
  message: string | null;
}

export function weatherState(status: Status, message: string | null): WeatherState {
  return { status, data: null, message };
}

export function forecastState(status: Status, message: string | null): DailyForecastState {
  return { status, data: null, message };
}

/** Whether a cached entry still satisfies the current detail requirement. */
export function satisfies(data: WeatherDisplayData, detailedRequired: boolean): boolean {
  return !detailedRequired || data.detail !== null;
}

export function findForecastByDate(
  data: DailyForecastData,
  date: string
): DailyForecast | null {
  return data.entries.find((entry) => entry.fxDate === date) ?? null;
}

/**
 * Language-dependent labels/units for the detail carousel. Supplied by the UI
 * layer so this module stays free of i18n dependencies and unit testable. Each
 * format string takes a single `%1$s` value.
 */
export interface DetailLabels {
  feelsFormat: string;
  humidityFormat: string;
  windScaleFormat: string;
  precipFormat: string;
  airFormat: string;
  warningSuffix: string;
}

/** Chinese labels, matching the app's historical default wording. */
export const CHINESE_LABELS: DetailLabels = {
  feelsFormat: '体感 %1$s℃',
  humidityFormat: '湿度 %1$s%%',
  windScaleFormat: '%1$s 级',
  precipFormat: '降水 %1$s mm',
  airFormat: '空气 %1$s',
  warningSuffix: '预警',
};

const isPresent = (value: string | null | undefined): boolean =>
  value !== null && value !== undefined && value.trim().length > 0;

const apply = (template: string, value: string): string =>
  template.replace(/%1\$s/g, value).replace(/%%/g, '%');

/**
 * Builds the ordered list of detail strings shown in the rotating detailed
 * weather line. Optional items (precipitation, warnings, air quality) are only
 * included when meaningful data is available.
 */
export function detailCarouselItems(
  detail: WeatherDetail,
  labels: DetailLabels = CHINESE_LABELS
): string[] {
  const items: string[] = [];
  if (isPresent(detail.feelsLike)) items.push(apply(labels.feelsFormat, detail.feelsLike!));
  if (isPresent(detail.humidity)) items.push(apply(labels.humidityFormat, detail.humidity!));
  if (isPresent(detail.windDir) || isPresent(detail.windScale)) {
    let wind = isPresent(detail.windDir) ? detail.windDir! : '';
    if (isPresent(detail.windScale)) {
      const scale = apply(labels.windScaleFormat, detail.windScale!);
      wind = (wind.length > 0 ? wind + ' ' : '') + scale;
    }
    items.push(wind);
  }
  if (hasPrecipitation(detail)) items.push(apply(labels.precipFormat, detail.precip!));
  if (isPresent(detail.warning)) {
    const warnings = detail.warning!.split('\n');
    for (let index = 0; index < warnings.length && index < 20; index++) {
      const item = warnings[index].trim();
      if (!item) continue;
      items.push(item.includes(labels.warningSuffix.trim()) ? item : item + labels.warningSuffix);
    }
  }
  if (isPresent(detail.aqiValue)) {
    let aqi = apply(labels.airFormat, detail.aqiValue!);
    if (isPresent(detail.aqiCategory)) aqi += ' ' + detail.aqiCategory;
    items.push(aqi);
  }
  return items;
}

function hasPrecipitation(detail: WeatherDetail): boolean {
  if (!isPresent(detail.precip)) return false;
  const value = Number.parseFloat(detail.precip!);
  return Number.isFinite(value) && value > 0;
}

/**
 * Joins a city and district for display. Chinese names read naturally city-first
 * (深圳宝安); English names follow Western address order, district-first with a
 * comma (Bao'an, Shenzhen).
 */
export function locationText(city: string | null, district: string | null): string {
  if (city === null || city === undefined) return district ?? '';
  // The trailing 市 suffix only appears on Chinese city names; English passes through.
  const displayCity = city.endsWith('市') ? city.slice(0, -1) : city;
  if (!district || city === district || displayCity === district) return displayCity;
  if (containsHan(displayCity) || containsHan(district)) return displayCity + district;
  return district + ', ' + displayCity;
}

function containsHan(text: string): boolean {
  for (const character of text) {
    const codePoint = character.codePointAt(0)!;
    if (codePoint >= 0x4e00 && codePoint <= 0x9fff) return true;
  }
  return false;
}
