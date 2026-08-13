/**
 * User preferences, backed by localStorage.
 *
 * Ported from com.clockmods.background.ClockPreferences. Storage keys and every
 * default value are kept identical to the Android version so the two can be
 * compared directly. Dropped: `show_status_icons` / `status_icon_scale`, since the
 * network and battery indicators are not reimplemented on the web.
 *
 * Added (web only, `web_` prefix): the QWeather credentials and optional proxy —
 * on Android these came from BuildConfig, injected at build time from
 * qweather.properties; in the browser they have to be entered by the user and are
 * kept in local storage only, never sent anywhere but to the weather endpoint.
 */
import { DEFAULT_PATTERN_CN, DEFAULT_PATTERN_EN, isValidPattern } from '../format/date-formatter';
import { normalizeFontFamily } from './fonts';
import { TIME_ZONE_FOLLOW_SYSTEM } from './timezone';

export const MODE_COLOR = 'color';
export const MODE_IMAGE = 'image';

export const TRANSITION_FADE = 'fade';
export const TRANSITION_SLIDE_UP = 'slide_up';
export const TRANSITION_SLIDE_DOWN = 'slide_down';
export const TRANSITION_SCALE = 'scale';
export const TRANSITION_FLIP = 'flip';

export const LANGUAGE_SIMPLIFIED = 'zh-Hans';
export const LANGUAGE_TRADITIONAL = 'zh-Hant';
export const LANGUAGE_ENGLISH = 'en';

export const WEATHER_LOCATION_AUTOMATIC = 'automatic';
export const WEATHER_LOCATION_MANUAL = 'manual';

export const ORIENTATION_FOLLOW_SYSTEM = 0;
export const ORIENTATION_PORTRAIT = 1;
export const ORIENTATION_LANDSCAPE = 2;

/** Fraction of the screen width occupied by the time text by default. */
export const DEFAULT_TIME_FONT_SCALE = 0.88;
/** Fraction of the screen width occupied by the date text by default. */
export const DEFAULT_DATE_FONT_SCALE = 0.55;
export const DEFAULT_TEXT_COLOR = 0xffffffff;
export const DEFAULT_BACKGROUND_COLOR = 0xff000000;
export const DEFAULT_DIM_BACKGROUND = false;
export const DEFAULT_SCHEDULE_DIM_BACKGROUND = false;
export const DEFAULT_DIM_START_MINUTES = 22 * 60;
export const DEFAULT_DIM_END_MINUTES = 6 * 60;
export const DEFAULT_BLINK_COLON = false;
export const DEFAULT_ANIMATE_TIME_CHANGES = true;
export const DEFAULT_TIME_TRANSITION = TRANSITION_FADE;
export const DEFAULT_HOURLY_CHIME = true;
export const DEFAULT_HOURLY_CHIME_QUIET = true;
export const DEFAULT_HOURLY_CHIME_QUIET_START = 22 * 60;
export const DEFAULT_HOURLY_CHIME_QUIET_END = 7 * 60;
export const DEFAULT_BOLD_TEXT = false;
export const DEFAULT_FONT_FAMILY = 'system';
export const DEFAULT_SHOW_SECONDS = true;
export const DEFAULT_SHOW_LUNAR = true;
export const DEFAULT_CALENDAR_MORE_FESTIVALS = false;
/** Matching java.util.Calendar.SUNDAY / MONDAY. */
export const CALENDAR_WEEK_START_SUNDAY = 1;
export const CALENDAR_WEEK_START_MONDAY = 2;
export const DEFAULT_CALENDAR_WEEK_START = CALENDAR_WEEK_START_SUNDAY;
export const DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS = false;
export const DEFAULT_SMALL_SECONDS = false;
export const DEFAULT_PORTRAIT_STACKED = false;
/** Landscape only: whether date and lunar stack on two rows (portrait always does). */
export const DEFAULT_DATE_LUNAR_DUAL_LINE = false;
export const DEFAULT_USE_24_HOUR = true;
export const DEFAULT_CLOCK_LANGUAGE = LANGUAGE_SIMPLIFIED;
export const DEFAULT_CUSTOM_MESSAGE = '';
export const MAX_CUSTOM_MESSAGE_LENGTH = 200;
export const DEFAULT_SCREEN_ORIENTATION = ORIENTATION_FOLLOW_SYSTEM;
export const DEFAULT_USE_NETWORK_TIME = false;
export const DEFAULT_SYNC_INTERVAL_MINUTES = 60;
export const DEFAULT_TIME_ZONE_ID = TIME_ZONE_FOLLOW_SYSTEM;
export const DEFAULT_WEATHER_ENABLED = false;
export const DEFAULT_WEATHER_DETAILED = false;
export const DEFAULT_WEATHER_INTERVAL_MINUTES = 30;
export const DEFAULT_WEATHER_ICON_FILL = true;
export const DEFAULT_WEATHER_ICON_DYNAMIC_COLOR = false;
export const DEFAULT_WEATHER_LOCATION_MODE = WEATHER_LOCATION_AUTOMATIC;

/** Allowed range for the width-based font scale (fraction of screen width). */
export const MIN_FONT_SCALE = 0.2;
export const MAX_FONT_SCALE = 1.5;

/** Default host used when the user has not entered one. */
export const DEFAULT_QWEATHER_API_HOST = 'devapi.qweather.com';

const PREFIX = 'clock_prefs.';

const K = {
  backgroundMode: 'background_mode',
  backgroundColor: 'background_color',
  dimBackground: 'dim_background',
  scheduleDimBackground: 'schedule_dim_background',
  dimStartMinutes: 'dim_start_minutes',
  dimEndMinutes: 'dim_end_minutes',
  timeFontScale: 'time_font_scale',
  dateFontScale: 'date_font_scale',
  timeColor: 'time_color',
  dateColor: 'date_color',
  blinkColon: 'blink_colon',
  animateTimeChanges: 'animate_time_changes',
  timeTransition: 'time_transition',
  hourlyChime: 'hourly_visual_chime',
  hourlyChimeQuiet: 'hourly_chime_quiet',
  hourlyChimeQuietStart: 'hourly_chime_quiet_start',
  hourlyChimeQuietEnd: 'hourly_chime_quiet_end',
  boldText: 'bold_text',
  fontFamily: 'font_family',
  showSeconds: 'show_seconds',
  showLunar: 'show_lunar',
  calendarMoreFestivals: 'calendar_more_festivals',
  calendarWeekStart: 'calendar_week_start',
  calendarHighlightWeekends: 'calendar_highlight_weekends',
  smallSeconds: 'small_seconds',
  portraitStacked: 'portrait_stacked',
  dateLunarDualLine: 'date_lunar_dual_line',
  use24Hour: 'use_24_hour',
  clockLanguage: 'clock_language',
  customMessage: 'custom_message',
  datePatternCn: 'date_pattern_cn',
  datePatternEn: 'date_pattern_en',
  dateCoreCn: 'date_core_cn',
  dateCoreEn: 'date_core_en',
  dateComboCn: 'date_combo_cn',
  dateComboEn: 'date_combo_en',
  dateCustomEnabledCn: 'date_custom_enabled_cn',
  dateCustomEnabledEn: 'date_custom_enabled_en',
  dateCustomTextCn: 'date_custom_text_cn',
  dateCustomTextEn: 'date_custom_text_en',
  screenOrientation: 'screen_orientation',
  useNetworkTime: 'use_network_time',
  syncIntervalMinutes: 'sync_interval_minutes',
  timeZoneId: 'time_zone_id',
  weatherEnabled: 'weather_enabled',
  weatherDetailed: 'weather_detailed',
  weatherIntervalMinutes: 'weather_interval_minutes',
  weatherLocationMode: 'weather_location_mode',
  weatherLocationId: 'weather_location_id',
  weatherProvince: 'weather_province',
  weatherCity: 'weather_city',
  weatherDistrict: 'weather_district',
  weatherLatitude: 'weather_latitude',
  weatherLongitude: 'weather_longitude',
  weatherIconFill: 'weather_icon_fill',
  weatherIconDynamicColor: 'weather_icon_dynamic_color',
  // Web-only
  qweatherApiHost: 'web_qweather_api_host',
  qweatherCredentialId: 'web_qweather_credential_id',
  qweatherProjectId: 'web_qweather_project_id',
  qweatherPrivateKey: 'web_qweather_private_key',
  qweatherProxy: 'web_qweather_proxy',
  timeSourceUrl: 'web_time_source_url',
  lastPage: 'web_last_page',
} as const;

/**
 * Thin typed wrapper over localStorage. Access is synchronous, matching the
 * SharedPreferences call sites that read settings inside render loops. Failures
 * (private-mode quota errors, disabled storage) degrade to in-memory values so
 * the clock keeps working.
 */
class Store {
  private readonly memory = new Map<string, string>();
  private readonly available: boolean;

  constructor() {
    this.available = probeLocalStorage();
  }

  raw(key: string): string | null {
    if (!this.available) return this.memory.get(key) ?? null;
    try {
      return localStorage.getItem(PREFIX + key);
    } catch {
      return this.memory.get(key) ?? null;
    }
  }

  write(key: string, value: string): void {
    this.memory.set(key, value);
    if (!this.available) return;
    try {
      localStorage.setItem(PREFIX + key, value);
    } catch {
      // Quota or private-mode failure: the in-memory copy above still applies
      // for this session.
    }
  }

  remove(key: string): void {
    this.memory.delete(key);
    if (!this.available) return;
    try {
      localStorage.removeItem(PREFIX + key);
    } catch {
      /* ignore */
    }
  }

  has(key: string): boolean {
    return this.raw(key) !== null;
  }

  string(key: string, fallback: string): string {
    return this.raw(key) ?? fallback;
  }

  bool(key: string, fallback: boolean): boolean {
    const value = this.raw(key);
    return value === null ? fallback : value === 'true';
  }

  int(key: string, fallback: number): number {
    const value = this.raw(key);
    if (value === null) return fallback;
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  float(key: string, fallback: number): number {
    const value = this.raw(key);
    if (value === null) return fallback;
    const parsed = Number.parseFloat(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }
}

function probeLocalStorage(): boolean {
  try {
    const probe = '__clockmods_probe__';
    localStorage.setItem(probe, '1');
    localStorage.removeItem(probe);
    return true;
  } catch {
    return false;
  }
}

const store = new Store();

const clampScale = (scale: number): number =>
  Math.max(MIN_FONT_SCALE, Math.min(MAX_FONT_SCALE, scale));

export function normalizeTimeTransition(transition: string | null | undefined): string {
  if (
    transition === TRANSITION_SLIDE_UP ||
    transition === TRANSITION_SLIDE_DOWN ||
    transition === TRANSITION_SCALE ||
    transition === TRANSITION_FLIP
  ) {
    return transition;
  }
  return TRANSITION_FADE;
}

export function normalizeClockLanguage(language: string | null | undefined): string {
  if (language === LANGUAGE_ENGLISH) return LANGUAGE_ENGLISH;
  if (language === LANGUAGE_TRADITIONAL) return LANGUAGE_TRADITIONAL;
  return LANGUAGE_SIMPLIFIED;
}

/** Trims whitespace and caps the length so a pasted blob cannot break layout. */
export function normalizeCustomMessage(message: string | null | undefined): string {
  if (!message) return DEFAULT_CUSTOM_MESSAGE;
  const trimmed = message.trim();
  return trimmed.length > MAX_CUSTOM_MESSAGE_LENGTH
    ? trimmed.slice(0, MAX_CUSTOM_MESSAGE_LENGTH)
    : trimmed;
}

/** Falls back to `fallback` when `pattern` fails validation, guarding stored data. */
export function normalizeDatePattern(
  pattern: string | null | undefined,
  fallback: string
): string {
  return pattern && isValidPattern(pattern) ? pattern : fallback;
}

export function isValidWeatherInterval(minutes: number): boolean {
  return (
    minutes === 10 ||
    minutes === 30 ||
    minutes === 60 ||
    minutes === 180 ||
    minutes === 360 ||
    minutes === 720
  );
}

function normalizeCalendarWeekStart(firstDayOfWeek: number): number {
  return firstDayOfWeek === CALENDAR_WEEK_START_MONDAY
    ? CALENDAR_WEEK_START_MONDAY
    : CALENDAR_WEEK_START_SUNDAY;
}

export const prefs = {
  // ---- Background ----
  getBackgroundMode: (): string => store.string(K.backgroundMode, MODE_COLOR),
  setBackgroundMode: (mode: string): void => store.write(K.backgroundMode, mode),

  getBackgroundColor: (): number => store.int(K.backgroundColor, DEFAULT_BACKGROUND_COLOR),
  setBackgroundColor: (color: number): void => store.write(K.backgroundColor, String(color)),

  isDimBackground: (): boolean => store.bool(K.dimBackground, DEFAULT_DIM_BACKGROUND),
  setDimBackground: (value: boolean): void => store.write(K.dimBackground, String(value)),

  isScheduleDimBackground: (): boolean =>
    store.bool(K.scheduleDimBackground, DEFAULT_SCHEDULE_DIM_BACKGROUND),
  setScheduleDimBackground: (value: boolean): void =>
    store.write(K.scheduleDimBackground, String(value)),

  getDimStartMinutes: (): number => store.int(K.dimStartMinutes, DEFAULT_DIM_START_MINUTES),
  setDimStartMinutes: (minutes: number): void => store.write(K.dimStartMinutes, String(minutes)),

  getDimEndMinutes: (): number => store.int(K.dimEndMinutes, DEFAULT_DIM_END_MINUTES),
  setDimEndMinutes: (minutes: number): void => store.write(K.dimEndMinutes, String(minutes)),

  // ---- Typography ----
  getTimeFontScale: (): number => clampScale(store.float(K.timeFontScale, DEFAULT_TIME_FONT_SCALE)),
  setTimeFontScale: (scale: number): void => store.write(K.timeFontScale, String(clampScale(scale))),

  getDateFontScale: (): number => clampScale(store.float(K.dateFontScale, DEFAULT_DATE_FONT_SCALE)),
  setDateFontScale: (scale: number): void => store.write(K.dateFontScale, String(clampScale(scale))),

  getTimeColor: (): number => store.int(K.timeColor, DEFAULT_TEXT_COLOR),
  setTimeColor: (color: number): void => store.write(K.timeColor, String(color)),

  getDateColor: (): number => store.int(K.dateColor, DEFAULT_TEXT_COLOR),
  setDateColor: (color: number): void => store.write(K.dateColor, String(color)),

  isBoldText: (): boolean => store.bool(K.boldText, DEFAULT_BOLD_TEXT),
  setBoldText: (value: boolean): void => store.write(K.boldText, String(value)),

  getFontFamily: (): string => normalizeFontFamily(store.string(K.fontFamily, DEFAULT_FONT_FAMILY)),
  setFontFamily: (family: string): void => store.write(K.fontFamily, normalizeFontFamily(family)),

  // ---- Clock face ----
  isBlinkColon: (): boolean => store.bool(K.blinkColon, DEFAULT_BLINK_COLON),
  setBlinkColon: (value: boolean): void => store.write(K.blinkColon, String(value)),

  isAnimateTimeChanges: (): boolean =>
    store.bool(K.animateTimeChanges, DEFAULT_ANIMATE_TIME_CHANGES),
  setAnimateTimeChanges: (value: boolean): void => store.write(K.animateTimeChanges, String(value)),

  getTimeTransition: (): string =>
    normalizeTimeTransition(store.string(K.timeTransition, DEFAULT_TIME_TRANSITION)),
  setTimeTransition: (transition: string): void =>
    store.write(K.timeTransition, normalizeTimeTransition(transition)),

  isShowSeconds: (): boolean => store.bool(K.showSeconds, DEFAULT_SHOW_SECONDS),
  setShowSeconds: (value: boolean): void => store.write(K.showSeconds, String(value)),

  isSmallSeconds: (): boolean => store.bool(K.smallSeconds, DEFAULT_SMALL_SECONDS),
  setSmallSeconds: (value: boolean): void => store.write(K.smallSeconds, String(value)),

  isShowLunar: (): boolean => store.bool(K.showLunar, DEFAULT_SHOW_LUNAR),
  setShowLunar: (value: boolean): void => store.write(K.showLunar, String(value)),

  isPortraitStacked: (): boolean => store.bool(K.portraitStacked, DEFAULT_PORTRAIT_STACKED),
  setPortraitStacked: (value: boolean): void => store.write(K.portraitStacked, String(value)),

  /**
   * @returns whether the date and lunar lines stack on two rows in landscape.
   *   Portrait always stacks regardless of this value.
   */
  isDateLunarDualLine: (): boolean =>
    store.bool(K.dateLunarDualLine, DEFAULT_DATE_LUNAR_DUAL_LINE),
  setDateLunarDualLine: (value: boolean): void => store.write(K.dateLunarDualLine, String(value)),

  isUse24Hour: (): boolean => store.bool(K.use24Hour, DEFAULT_USE_24_HOUR),
  setUse24Hour: (value: boolean): void => store.write(K.use24Hour, String(value)),

  // ---- Hourly chime ----
  isHourlyChimeEnabled: (): boolean => store.bool(K.hourlyChime, DEFAULT_HOURLY_CHIME),
  setHourlyChimeEnabled: (value: boolean): void => store.write(K.hourlyChime, String(value)),

  isHourlyChimeQuietEnabled: (): boolean =>
    store.bool(K.hourlyChimeQuiet, DEFAULT_HOURLY_CHIME_QUIET),
  setHourlyChimeQuietEnabled: (value: boolean): void =>
    store.write(K.hourlyChimeQuiet, String(value)),

  getHourlyChimeQuietStart: (): number =>
    store.int(K.hourlyChimeQuietStart, DEFAULT_HOURLY_CHIME_QUIET_START),
  setHourlyChimeQuietStart: (minutes: number): void =>
    store.write(K.hourlyChimeQuietStart, String(minutes)),

  getHourlyChimeQuietEnd: (): number =>
    store.int(K.hourlyChimeQuietEnd, DEFAULT_HOURLY_CHIME_QUIET_END),
  setHourlyChimeQuietEnd: (minutes: number): void =>
    store.write(K.hourlyChimeQuietEnd, String(minutes)),

  // ---- Calendar ----
  isCalendarMoreFestivals: (): boolean =>
    store.bool(K.calendarMoreFestivals, DEFAULT_CALENDAR_MORE_FESTIVALS),
  setCalendarMoreFestivals: (value: boolean): void =>
    store.write(K.calendarMoreFestivals, String(value)),

  getCalendarWeekStart: (): number =>
    normalizeCalendarWeekStart(store.int(K.calendarWeekStart, DEFAULT_CALENDAR_WEEK_START)),
  setCalendarWeekStart: (firstDayOfWeek: number): void =>
    store.write(K.calendarWeekStart, String(normalizeCalendarWeekStart(firstDayOfWeek))),

  isCalendarHighlightWeekends: (): boolean =>
    store.bool(K.calendarHighlightWeekends, DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS),
  setCalendarHighlightWeekends: (value: boolean): void =>
    store.write(K.calendarHighlightWeekends, String(value)),

  // ---- Language ----
  getClockLanguage: (): string =>
    normalizeClockLanguage(store.string(K.clockLanguage, DEFAULT_CLOCK_LANGUAGE)),
  setClockLanguage: (language: string): void =>
    store.write(K.clockLanguage, normalizeClockLanguage(language)),

  isClockUseEnglish: (): boolean => prefs.getClockLanguage() === LANGUAGE_ENGLISH,

  // ---- Custom message ----
  getCustomMessage: (): string =>
    normalizeCustomMessage(store.string(K.customMessage, DEFAULT_CUSTOM_MESSAGE)),
  setCustomMessage: (message: string): void =>
    store.write(K.customMessage, normalizeCustomMessage(message)),

  // ---- Date format ----
  getDatePatternCn: (): string =>
    normalizeDatePattern(store.string(K.datePatternCn, DEFAULT_PATTERN_CN), DEFAULT_PATTERN_CN),
  setDatePatternCn: (pattern: string): void =>
    store.write(K.datePatternCn, normalizeDatePattern(pattern, DEFAULT_PATTERN_CN)),

  getDatePatternEn: (): string =>
    normalizeDatePattern(store.string(K.datePatternEn, DEFAULT_PATTERN_EN), DEFAULT_PATTERN_EN),
  setDatePatternEn: (pattern: string): void =>
    store.write(K.datePatternEn, normalizeDatePattern(pattern, DEFAULT_PATTERN_EN)),

  // Settings-UI restoration state (does not affect rendering directly).
  getDateCore: (english: boolean): string =>
    store.string(english ? K.dateCoreEn : K.dateCoreCn, ''),
  getDateCombo: (english: boolean): string =>
    store.string(english ? K.dateComboEn : K.dateComboCn, ''),
  isDateCustomEnabled: (english: boolean): boolean =>
    store.bool(english ? K.dateCustomEnabledEn : K.dateCustomEnabledCn, false),
  getDateCustomText: (english: boolean): string =>
    store.string(english ? K.dateCustomTextEn : K.dateCustomTextCn, ''),

  setDateFormatState: (
    english: boolean,
    core: string,
    combo: string,
    customEnabled: boolean,
    customText: string
  ): void => {
    store.write(english ? K.dateCoreEn : K.dateCoreCn, core);
    store.write(english ? K.dateComboEn : K.dateComboCn, combo);
    store.write(
      english ? K.dateCustomEnabledEn : K.dateCustomEnabledCn,
      String(customEnabled)
    );
    store.write(english ? K.dateCustomTextEn : K.dateCustomTextCn, customText);
  },

  // ---- Display ----
  getScreenOrientation: (): number => store.int(K.screenOrientation, DEFAULT_SCREEN_ORIENTATION),
  setScreenOrientation: (mode: number): void => store.write(K.screenOrientation, String(mode)),

  // ---- Time source ----
  isUseNetworkTime: (): boolean => store.bool(K.useNetworkTime, DEFAULT_USE_NETWORK_TIME),
  setUseNetworkTime: (value: boolean): void => store.write(K.useNetworkTime, String(value)),

  getSyncIntervalMinutes: (): number =>
    store.int(K.syncIntervalMinutes, DEFAULT_SYNC_INTERVAL_MINUTES),
  setSyncIntervalMinutes: (minutes: number): void =>
    store.write(K.syncIntervalMinutes, String(minutes)),

  /** @returns the selected zone id, or "" to follow the system. */
  getTimeZoneId: (): string => store.string(K.timeZoneId, DEFAULT_TIME_ZONE_ID),
  setTimeZoneId: (id: string | null): void =>
    store.write(K.timeZoneId, id ?? TIME_ZONE_FOLLOW_SYSTEM),

  /** Optional HTTP endpoint whose `Date` header is used for network time. */
  getTimeSourceUrl: (): string => store.string(K.timeSourceUrl, ''),
  setTimeSourceUrl: (url: string): void => store.write(K.timeSourceUrl, url.trim()),

  // ---- Weather ----
  isWeatherEnabled: (): boolean => store.bool(K.weatherEnabled, DEFAULT_WEATHER_ENABLED),
  setWeatherEnabled: (value: boolean): void => store.write(K.weatherEnabled, String(value)),

  isWeatherDetailed: (): boolean => store.bool(K.weatherDetailed, DEFAULT_WEATHER_DETAILED),
  setWeatherDetailed: (value: boolean): void => store.write(K.weatherDetailed, String(value)),

  isWeatherIconFill: (): boolean => store.bool(K.weatherIconFill, DEFAULT_WEATHER_ICON_FILL),
  setWeatherIconFill: (value: boolean): void => store.write(K.weatherIconFill, String(value)),

  isWeatherIconDynamicColor: (): boolean =>
    store.bool(K.weatherIconDynamicColor, DEFAULT_WEATHER_ICON_DYNAMIC_COLOR),
  setWeatherIconDynamicColor: (value: boolean): void =>
    store.write(K.weatherIconDynamicColor, String(value)),

  getWeatherIntervalMinutes: (): number => {
    const value = store.int(K.weatherIntervalMinutes, DEFAULT_WEATHER_INTERVAL_MINUTES);
    return isValidWeatherInterval(value) ? value : DEFAULT_WEATHER_INTERVAL_MINUTES;
  },
  setWeatherIntervalMinutes: (minutes: number): void =>
    store.write(
      K.weatherIntervalMinutes,
      String(isValidWeatherInterval(minutes) ? minutes : DEFAULT_WEATHER_INTERVAL_MINUTES)
    ),

  /** Manual mode only takes effect once a location has actually been chosen. */
  getWeatherLocationMode: (): string => {
    const mode = store.string(K.weatherLocationMode, DEFAULT_WEATHER_LOCATION_MODE);
    return mode === WEATHER_LOCATION_MANUAL && prefs.getWeatherLocationId().length > 0
      ? WEATHER_LOCATION_MANUAL
      : WEATHER_LOCATION_AUTOMATIC;
  },
  setWeatherLocationMode: (mode: string): void =>
    store.write(
      K.weatherLocationMode,
      mode === WEATHER_LOCATION_MANUAL ? WEATHER_LOCATION_MANUAL : WEATHER_LOCATION_AUTOMATIC
    ),

  getWeatherLocationId: (): string => store.string(K.weatherLocationId, ''),
  getWeatherProvince: (): string => store.string(K.weatherProvince, ''),
  getWeatherCity: (): string => store.string(K.weatherCity, ''),
  getWeatherDistrict: (): string => store.string(K.weatherDistrict, ''),
  getWeatherLatitude: (): number => store.float(K.weatherLatitude, Number.NaN),
  getWeatherLongitude: (): number => store.float(K.weatherLongitude, Number.NaN),

  setManualWeatherLocation: (
    locationId: string,
    province: string,
    city: string,
    district: string,
    latitude = Number.NaN,
    longitude = Number.NaN
  ): void => {
    store.write(K.weatherLocationId, locationId);
    store.write(K.weatherProvince, province);
    store.write(K.weatherCity, city);
    store.write(K.weatherDistrict, district);
    store.write(K.weatherLatitude, String(latitude));
    store.write(K.weatherLongitude, String(longitude));
  },

  // ---- QWeather credentials (web only) ----
  getQWeatherApiHost: (): string => store.string(K.qweatherApiHost, DEFAULT_QWEATHER_API_HOST),
  getQWeatherCredentialId: (): string => store.string(K.qweatherCredentialId, ''),
  getQWeatherProjectId: (): string => store.string(K.qweatherProjectId, ''),
  getQWeatherPrivateKey: (): string => store.string(K.qweatherPrivateKey, ''),
  /** Base URL of an optional signing proxy; when set, no key is used in-browser. */
  getQWeatherProxy: (): string => store.string(K.qweatherProxy, ''),

  setQWeatherCredentials: (
    apiHost: string,
    credentialId: string,
    projectId: string,
    privateKey: string,
    proxy: string
  ): void => {
    store.write(K.qweatherApiHost, apiHost.trim() || DEFAULT_QWEATHER_API_HOST);
    store.write(K.qweatherCredentialId, credentialId.trim());
    store.write(K.qweatherProjectId, projectId.trim());
    store.write(K.qweatherPrivateKey, privateKey.trim());
    store.write(K.qweatherProxy, proxy.trim().replace(/\/+$/, ''));
  },

  /** Mirrors QWeatherConfig.isConfigured: a proxy alone is enough. */
  isWeatherConfigured: (): boolean => {
    if (prefs.getQWeatherProxy().length > 0) return true;
    return (
      prefs.getQWeatherApiHost().length > 0 &&
      prefs.getQWeatherCredentialId().length > 0 &&
      !prefs.getQWeatherCredentialId().startsWith('replace-') &&
      prefs.getQWeatherProjectId().length > 0 &&
      prefs.getQWeatherPrivateKey().length > 0
    );
  },

  // ---- Session ----
  getLastPage: (): string => store.string(K.lastPage, 'clock'),
  setLastPage: (page: string): void => store.write(K.lastPage, page),

  /** Restores all user-configurable settings to their defaults. */
  restoreDefaults(): void {
    store.write(K.backgroundMode, MODE_COLOR);
    store.write(K.backgroundColor, String(DEFAULT_BACKGROUND_COLOR));
    store.write(K.dimBackground, String(DEFAULT_DIM_BACKGROUND));
    store.write(K.scheduleDimBackground, String(DEFAULT_SCHEDULE_DIM_BACKGROUND));
    store.write(K.dimStartMinutes, String(DEFAULT_DIM_START_MINUTES));
    store.write(K.dimEndMinutes, String(DEFAULT_DIM_END_MINUTES));
    store.write(K.timeFontScale, String(DEFAULT_TIME_FONT_SCALE));
    store.write(K.dateFontScale, String(DEFAULT_DATE_FONT_SCALE));
    store.write(K.timeColor, String(DEFAULT_TEXT_COLOR));
    store.write(K.dateColor, String(DEFAULT_TEXT_COLOR));
    store.write(K.blinkColon, String(DEFAULT_BLINK_COLON));
    store.write(K.animateTimeChanges, String(DEFAULT_ANIMATE_TIME_CHANGES));
    store.write(K.timeTransition, DEFAULT_TIME_TRANSITION);
    store.write(K.hourlyChime, String(DEFAULT_HOURLY_CHIME));
    store.write(K.hourlyChimeQuiet, String(DEFAULT_HOURLY_CHIME_QUIET));
    store.write(K.hourlyChimeQuietStart, String(DEFAULT_HOURLY_CHIME_QUIET_START));
    store.write(K.hourlyChimeQuietEnd, String(DEFAULT_HOURLY_CHIME_QUIET_END));
    store.write(K.boldText, String(DEFAULT_BOLD_TEXT));
    store.write(K.fontFamily, DEFAULT_FONT_FAMILY);
    store.write(K.showSeconds, String(DEFAULT_SHOW_SECONDS));
    store.write(K.showLunar, String(DEFAULT_SHOW_LUNAR));
    store.write(K.calendarMoreFestivals, String(DEFAULT_CALENDAR_MORE_FESTIVALS));
    store.write(K.calendarWeekStart, String(DEFAULT_CALENDAR_WEEK_START));
    store.write(K.calendarHighlightWeekends, String(DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS));
    store.write(K.smallSeconds, String(DEFAULT_SMALL_SECONDS));
    store.write(K.portraitStacked, String(DEFAULT_PORTRAIT_STACKED));
    store.write(K.dateLunarDualLine, String(DEFAULT_DATE_LUNAR_DUAL_LINE));
    store.write(K.use24Hour, String(DEFAULT_USE_24_HOUR));
    store.write(K.clockLanguage, DEFAULT_CLOCK_LANGUAGE);
    store.remove(K.customMessage);
    store.write(K.datePatternCn, DEFAULT_PATTERN_CN);
    store.write(K.datePatternEn, DEFAULT_PATTERN_EN);
    store.remove(K.dateCoreCn);
    store.remove(K.dateCoreEn);
    store.remove(K.dateComboCn);
    store.remove(K.dateComboEn);
    store.remove(K.dateCustomEnabledCn);
    store.remove(K.dateCustomEnabledEn);
    store.remove(K.dateCustomTextCn);
    store.remove(K.dateCustomTextEn);
    store.write(K.screenOrientation, String(DEFAULT_SCREEN_ORIENTATION));
    store.write(K.weatherEnabled, String(DEFAULT_WEATHER_ENABLED));
    store.write(K.weatherDetailed, String(DEFAULT_WEATHER_DETAILED));
    store.write(K.weatherIconFill, String(DEFAULT_WEATHER_ICON_FILL));
    store.write(K.weatherIconDynamicColor, String(DEFAULT_WEATHER_ICON_DYNAMIC_COLOR));
    store.write(K.weatherIntervalMinutes, String(DEFAULT_WEATHER_INTERVAL_MINUTES));
    store.write(K.weatherLocationMode, DEFAULT_WEATHER_LOCATION_MODE);
    store.remove(K.weatherLocationId);
    store.remove(K.weatherProvince);
    store.remove(K.weatherCity);
    store.remove(K.weatherDistrict);
    store.remove(K.weatherLatitude);
    store.remove(K.weatherLongitude);
    // Time source and QWeather credentials are deliberately preserved: they are
    // deployment configuration, not part of the visual defaults the reset button
    // offers to restore.
  },
};

/** Weather refresh intervals offered in settings, in minutes. */
export const WEATHER_INTERVALS: readonly number[] = [10, 30, 60, 180, 360, 720];

/** Converts a stored ARGB integer to a CSS color. */
export function cssColor(argb: number): string {
  const alpha = (argb >>> 24) & 0xff;
  const red = (argb >>> 16) & 0xff;
  const green = (argb >>> 8) & 0xff;
  const blue = argb & 0xff;
  if (alpha === 0xff) {
    return `#${((red << 16) | (green << 8) | blue).toString(16).padStart(6, '0')}`;
  }
  return `rgba(${red}, ${green}, ${blue}, ${(alpha / 255).toFixed(3)})`;
}

/** Parses `#rrggbb` into the stored opaque ARGB integer form. */
export function argbFromHex(hex: string): number {
  const value = Number.parseInt(hex.replace('#', ''), 16);
  return (0xff000000 | value) >>> 0;
}
