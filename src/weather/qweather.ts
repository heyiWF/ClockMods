/**
 * QWeather client: JWT signing plus the five endpoints the app uses.
 *
 * Ported from com.clockmods.weather.QWeatherSigner and QWeatherClient. The Java
 * build signed with net.i2p.crypto:eddsa and injected credentials from
 * BuildConfig; the browser signs with WebCrypto (falling back to @noble/ed25519
 * where Ed25519 is unavailable) using credentials the user entered, or delegates
 * signing entirely to a proxy.
 */
import type { DailyForecast, DailyForecastData, WeatherDetail, WeatherDisplayData } from './models';

export interface QWeatherConfig {
  apiHost: string;
  credentialId: string;
  projectId: string;
  privateKeyBase64: string;
  /** When set, requests go here unsigned and the proxy adds the Authorization header. */
  proxy: string;
  /** QWeather `lang` value: zh / zh-hant / en. */
  lang: string;
}

const TIMEOUT_MS = 15000;

// ---- JWT signing ----

const encoder = new TextEncoder();

function base64Url(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** Accepts raw Base64 or a PEM block, with or without whitespace. */
function decodeBase64(value: string): Uint8Array {
  const cleaned = value
    .replace(/-----[A-Z ]+-----/g, '')
    .replace(/\s+/g, '')
    .replace(/-/g, '+')
    .replace(/_/g, '/');
  const padded = cleaned.padEnd(Math.ceil(cleaned.length / 4) * 4, '=');
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index++) bytes[index] = binary.charCodeAt(index);
  return bytes;
}

let webCryptoUsable: boolean | null = null;

/**
 * Ed25519 in WebCrypto is recent (Chrome 137+, Safari 17+, Firefox 130+); probe
 * once and remember, so older browsers pay the fallback import only when needed.
 */
async function canUseWebCrypto(pkcs8: Uint8Array): Promise<boolean> {
  if (webCryptoUsable !== null) return webCryptoUsable;
  try {
    await crypto.subtle.importKey('pkcs8', toArrayBuffer(pkcs8), { name: 'Ed25519' }, false, [
      'sign',
    ]);
    webCryptoUsable = true;
  } catch {
    webCryptoUsable = false;
  }
  return webCryptoUsable;
}

function toArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
}

async function sign(pkcs8: Uint8Array, message: Uint8Array): Promise<Uint8Array> {
  if (await canUseWebCrypto(pkcs8)) {
    const key = await crypto.subtle.importKey(
      'pkcs8',
      toArrayBuffer(pkcs8),
      { name: 'Ed25519' },
      false,
      ['sign']
    );
    const signature = await crypto.subtle.sign('Ed25519', key, toArrayBuffer(message));
    return new Uint8Array(signature);
  }
  // A PKCS#8 Ed25519 key ends with the 32-byte seed, which is what the pure-JS
  // implementation expects.
  const { signAsync } = await import('@noble/ed25519');
  return signAsync(message, pkcs8.slice(-32));
}

/**
 * Builds the QWeather JWT. `iat` is backdated 30 seconds to tolerate clock skew,
 * exactly as the Android signer did.
 */
export async function createToken(
  credentialId: string,
  projectId: string,
  privateKeyBase64: string,
  nowSeconds: number
): Promise<string> {
  const header = `{"alg":"EdDSA","kid":"${credentialId}"}`;
  const issuedAt = Math.floor(nowSeconds) - 30;
  const payload = `{"sub":"${projectId}","iat":${issuedAt},"exp":${issuedAt + 900}}`;
  const signingInput = `${base64Url(encoder.encode(header))}.${base64Url(encoder.encode(payload))}`;
  const signature = await sign(decodeBase64(privateKeyBase64), encoder.encode(signingInput));
  return `${signingInput}.${base64Url(signature)}`;
}

// ---- Request plumbing ----

export class QWeatherError extends Error {}

async function requestRaw(
  config: QWeatherConfig,
  path: string,
  nowSeconds: number
): Promise<Record<string, unknown>> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const headers: Record<string, string> = {};
    let url: string;
    if (config.proxy) {
      url = config.proxy + path;
    } else {
      url = `https://${config.apiHost}${path}`;
      headers.Authorization = `Bearer ${await createToken(
        config.credentialId,
        config.projectId,
        config.privateKeyBase64,
        nowSeconds
      )}`;
    }
    const response = await fetch(url, { headers, signal: controller.signal });
    const body = (await response.json()) as Record<string, unknown>;
    if (!response.ok) {
      throw new QWeatherError(`QWeather error: ${body.code ?? response.status}`);
    }
    return body;
  } catch (error) {
    if (error instanceof QWeatherError) throw error;
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new QWeatherError('timeout');
    }
    // A CORS rejection surfaces as an opaque TypeError; name it so the UI can
    // point at the proxy setting.
    if (error instanceof TypeError) throw new QWeatherError('network/CORS');
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

async function request(
  config: QWeatherConfig,
  path: string,
  nowSeconds: number
): Promise<Record<string, unknown>> {
  const body = await requestRaw(config, path, nowSeconds);
  if (body.code !== '200') throw new QWeatherError(`QWeather error: ${body.code}`);
  return body;
}

const str = (value: unknown, fallback = ''): string =>
  typeof value === 'string' ? value : fallback;

export function formatLocation(latitude: number, longitude: number): string {
  return `${longitude.toFixed(2)},${latitude.toFixed(2)}`;
}

export function formatCoordinate(value: number): string {
  return value.toFixed(2);
}

// ---- Endpoints ----

interface GeoLocation {
  id: string;
  adm2: string;
  name: string;
  lat: number;
  lon: number;
}

async function lookupLocation(
  config: QWeatherConfig,
  latitude: number,
  longitude: number,
  nowSeconds: number
): Promise<GeoLocation> {
  const body = await request(
    config,
    `/geo/v2/city/lookup?location=${formatLocation(latitude, longitude)}&range=cn&number=1&lang=${
      config.lang
    }`,
    nowSeconds
  );
  const locations = body.location as Array<Record<string, unknown>> | undefined;
  if (!locations || locations.length === 0) throw new QWeatherError('No QWeather location');
  const first = locations[0];
  return {
    id: str(first.id),
    adm2: str(first.adm2),
    name: str(first.name),
    lat: Number.parseFloat(str(first.lat)) || latitude,
    lon: Number.parseFloat(str(first.lon)) || longitude,
  };
}

export async function fetchWeatherByCoordinates(
  config: QWeatherConfig,
  latitude: number,
  longitude: number,
  detailed: boolean,
  nowSeconds: number
): Promise<WeatherDisplayData> {
  const location = await lookupLocation(config, latitude, longitude, nowSeconds);
  return fetchNow(
    config,
    location.id,
    location.adm2,
    location.name,
    location.lat,
    location.lon,
    detailed,
    nowSeconds
  );
}

export async function fetchWeatherByLocationId(
  config: QWeatherConfig,
  locationId: string,
  city: string,
  district: string,
  latitude: number,
  longitude: number,
  detailed: boolean,
  nowSeconds: number
): Promise<WeatherDisplayData> {
  return fetchNow(config, locationId, city, district, latitude, longitude, detailed, nowSeconds);
}

async function fetchNow(
  config: QWeatherConfig,
  locationId: string,
  city: string,
  district: string,
  latitude: number,
  longitude: number,
  detailed: boolean,
  nowSeconds: number
): Promise<WeatherDisplayData> {
  const body = await request(
    config,
    `/v7/weather/now?location=${encodeURIComponent(locationId)}&lang=${config.lang}`,
    nowSeconds
  );
  const now = body.now as Record<string, unknown> | undefined;
  if (!now) throw new QWeatherError('Empty QWeather response');
  const detail = detailed
    ? await buildDetail(config, now, latitude, longitude, nowSeconds)
    : null;
  return {
    locationId,
    city,
    district,
    text: str(now.text),
    icon: str(now.icon),
    temperature: str(now.temp, '--'),
    updatedAt: Date.now(),
    detail,
  };
}

async function buildDetail(
  config: QWeatherConfig,
  now: Record<string, unknown>,
  latitude: number,
  longitude: number,
  nowSeconds: number
): Promise<WeatherDetail> {
  let warning: string | null = null;
  let aqiValue: string | null = null;
  let aqiCategory: string | null = null;
  if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
    warning = await fetchWarning(config, latitude, longitude, nowSeconds);
    const air = await fetchAirQuality(config, latitude, longitude, nowSeconds);
    aqiValue = air.value;
    aqiCategory = air.category;
  }
  return {
    feelsLike: str(now.feelsLike),
    humidity: str(now.humidity),
    windDir: str(now.windDir),
    windScale: str(now.windScale),
    precip: str(now.precip),
    warning,
    aqiValue,
    aqiCategory,
  };
}

async function fetchWarning(
  config: QWeatherConfig,
  latitude: number,
  longitude: number,
  nowSeconds: number
): Promise<string | null> {
  try {
    const body = await requestRaw(
      config,
      `/weatheralert/v1/current/${formatCoordinate(latitude)}/${formatCoordinate(
        longitude
      )}?lang=${config.lang}`,
      nowSeconds
    );
    return formatWarnings(body.alerts as Array<Record<string, unknown>> | undefined, config.lang);
  } catch {
    // Warnings are optional; a failure must not drop the rest of the detail line.
    return null;
  }
}

async function fetchAirQuality(
  config: QWeatherConfig,
  latitude: number,
  longitude: number,
  nowSeconds: number
): Promise<{ value: string | null; category: string | null }> {
  try {
    const body = await requestRaw(
      config,
      `/airquality/v1/current/${formatCoordinate(latitude)}/${formatCoordinate(
        longitude
      )}?lang=${config.lang}`,
      nowSeconds
    );
    const indexes = body.indexes as Array<Record<string, unknown>> | undefined;
    if (!indexes || indexes.length === 0) return { value: null, category: null };
    const preferred = indexes.find((index) => str(index.code) === 'qaqi') ?? indexes[0];
    const value = str(preferred.aqiDisplay);
    const category = str(preferred.category);
    return { value: value || null, category: category || null };
  } catch {
    return { value: null, category: null };
  }
}

export async function fetchDailyByCoordinates(
  config: QWeatherConfig,
  latitude: number,
  longitude: number,
  nowSeconds: number
): Promise<DailyForecastData> {
  const location = await lookupLocation(config, latitude, longitude, nowSeconds);
  return fetchDailyByLocationId(config, location.id, location.adm2, location.name, nowSeconds);
}

export async function fetchDailyByLocationId(
  config: QWeatherConfig,
  locationId: string,
  city: string,
  district: string,
  nowSeconds: number
): Promise<DailyForecastData> {
  const body = await request(
    config,
    `/v7/weather/3d?location=${encodeURIComponent(locationId)}&lang=${config.lang}`,
    nowSeconds
  );
  return parseDailyForecast(body, locationId, city, district, Date.now());
}

export function parseDailyForecast(
  body: Record<string, unknown>,
  locationId: string,
  city: string,
  district: string,
  updatedAt: number
): DailyForecastData {
  const daily = body.daily as Array<Record<string, unknown>> | undefined;
  if (!daily || daily.length === 0) throw new QWeatherError('Empty daily forecast');
  const entries: DailyForecast[] = [];
  for (const day of daily) {
    const fxDate = str(day.fxDate).trim();
    if (!fxDate) continue;
    entries.push({
      fxDate,
      tempMin: str(day.tempMin),
      tempMax: str(day.tempMax),
      iconDay: str(day.iconDay),
      textDay: str(day.textDay),
      windDirDay: str(day.windDirDay),
      windScaleDay: str(day.windScaleDay),
      humidity: str(day.humidity),
    });
  }
  if (entries.length === 0) throw new QWeatherError('Empty daily forecast');
  return { locationId, city, district, updatedAt, entries };
}

// ---- Warning text ----

export function formatWarnings(
  alerts: Array<Record<string, unknown>> | undefined,
  lang = 'zh'
): string | null {
  if (!alerts || alerts.length === 0) return null;
  const warnings: string[] = [];
  for (const alert of alerts.slice(0, 20)) {
    const warning = formatWarning(alert, lang);
    if (warning) warnings.push(warning);
  }
  return warnings.length === 0 ? null : warnings.join('\n');
}

export function formatWarning(alert: Record<string, unknown>, lang = 'zh'): string | null {
  const headline = str(alert.headline).trim();
  if (headline) return headline;

  const eventType = alert.eventType as Record<string, unknown> | undefined;
  const event = eventType ? str(eventType.name).trim() : '';
  if (!event) return null;

  const color = alert.color as Record<string, unknown> | undefined;
  const colorCode = color ? str(color.code).trim() : '';
  if (lang === 'en') {
    const colorName = WARNING_COLORS.en[colorCode.toLowerCase()] ?? colorCode;
    return colorName ? `${event} ${colorName} Warning` : `${event} Warning`;
  }
  const traditional = lang === 'zh-hant';
  const table = traditional ? WARNING_COLORS.hant : WARNING_COLORS.hans;
  const colorName = colorCode ? (table[colorCode.toLowerCase()] ?? colorCode) : '';
  return event + colorName + (traditional ? '預警' : '预警');
}

const WARNING_COLORS: Record<'hans' | 'hant' | 'en', Record<string, string>> = {
  hans: {
    white: '白色',
    gray: '灰色',
    green: '绿色',
    blue: '蓝色',
    yellow: '黄色',
    amber: '琥珀色',
    orange: '橙色',
    red: '红色',
    purple: '紫色',
    black: '黑色',
  },
  hant: {
    white: '白色',
    gray: '灰色',
    green: '綠色',
    blue: '藍色',
    yellow: '黃色',
    amber: '琥珀色',
    orange: '橙色',
    red: '紅色',
    purple: '紫色',
    black: '黑色',
  },
  en: {
    white: 'White',
    gray: 'Gray',
    green: 'Green',
    blue: 'Blue',
    yellow: 'Yellow',
    amber: 'Amber',
    orange: 'Orange',
    red: 'Red',
    purple: 'Purple',
    black: 'Black',
  },
};
