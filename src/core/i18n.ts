/**
 * Interface language handling.
 *
 * Replaces the Android resource-configuration approach
 * (com.clockmods.LocaleManager + res/values-*): message tables generated from the
 * original string resources are selected at call time, and changing the language
 * re-renders the UI instead of recreating an Activity.
 *
 * Placeholders keep the Android `%1$s` / `%1$d` / `%%` syntax so the generated
 * table can be compared against the original XML.
 */
import { ARRAYS, MESSAGES } from './messages.generated';
import type { ArrayKey, MessageKey } from './messages.generated';
import { LANGUAGE_ENGLISH, LANGUAGE_SIMPLIFIED, LANGUAGE_TRADITIONAL, prefs } from './prefs';
import type { Lang } from '../format/date-formatter';

export type { ArrayKey, MessageKey };

/** Web-only copy, plus overrides where the Android wording named Android APIs. */
const WEB_MESSAGES: Record<string, Record<string, string>> = {
  'zh-Hans': {
    // The browser cannot speak NTP (no UDP), so the wording and mechanism differ.
    use_network_time: '使用网络时间',
    use_network_time_desc: '按 HTTP 响应的 Date 头校准，精度约 1 秒；留空校时地址则使用设备时间',
    time_source_url: '校时地址',
    time_source_url_hint: '留空则使用天气代理或当前站点',
    ok: '确定',
    close: '关闭',
    weather_credentials_group: '天气服务凭据',
    weather_credentials_desc: '凭据仅保存在本机浏览器，不会上传到任何第三方',
    weather_api_host: 'API Host',
    weather_credential_id: '凭据 ID（kid）',
    weather_project_id: '项目 ID（sub）',
    weather_private_key: 'Ed25519 私钥（PKCS#8 Base64）',
    weather_proxy: '代理地址（可选）',
    weather_proxy_desc: '填写后改由代理签名请求，浏览器不再接触私钥，也能绕开跨域限制',
    weather_cors_hint: '若浏览器报跨域错误，请部署 proxy/cloudflare-worker.js 并填入代理地址',
    alarm_background_note: '网页闹钟需保持本页打开；页面关闭后浏览器不会唤醒它',
    alarm_notification_blocked: '通知权限未授予，到点仅显示全屏提醒',
    update_available: '有新版本可用',
    update_reload: '立即更新',
    settings_applied: '设置已应用',
    install_hint: '可在浏览器菜单中「添加到主屏幕」以全屏运行',
    calendar_jump_year: '年',
    calendar_jump_month: '月',
    orientation_lock_unsupported: '当前浏览器不支持锁定方向，已按比例调整布局',
    stopwatch_no_laps: '暂无计次',
  },
  'zh-Hant': {
    use_network_time: '使用網路時間',
    use_network_time_desc: '依 HTTP 回應的 Date 標頭校準，精度約 1 秒；校時位址留空則使用裝置時間',
    time_source_url: '校時位址',
    time_source_url_hint: '留空則使用天氣代理或目前站點',
    ok: '確定',
    close: '關閉',
    weather_credentials_group: '天氣服務憑證',
    weather_credentials_desc: '憑證僅儲存在本機瀏覽器，不會上傳到任何第三方',
    weather_api_host: 'API Host',
    weather_credential_id: '憑證 ID（kid）',
    weather_project_id: '專案 ID（sub）',
    weather_private_key: 'Ed25519 私鑰（PKCS#8 Base64）',
    weather_proxy: '代理位址（選填）',
    weather_proxy_desc: '填寫後改由代理簽署請求，瀏覽器不再接觸私鑰，也能繞過跨域限制',
    weather_cors_hint: '若瀏覽器出現跨域錯誤，請部署 proxy/cloudflare-worker.js 並填入代理位址',
    alarm_background_note: '網頁鬧鐘需保持本頁開啟；頁面關閉後瀏覽器不會喚醒它',
    alarm_notification_blocked: '未授予通知權限，到點僅顯示全螢幕提醒',
    update_available: '有新版本可用',
    update_reload: '立即更新',
    settings_applied: '設定已套用',
    install_hint: '可在瀏覽器選單中「加入主畫面」以全螢幕執行',
    calendar_jump_year: '年',
    calendar_jump_month: '月',
    orientation_lock_unsupported: '目前瀏覽器不支援鎖定方向，已依比例調整版面',
    stopwatch_no_laps: '尚無計次',
  },
  en: {
    use_network_time: 'Use network time',
    use_network_time_desc:
      "Calibrates from the HTTP Date header (about 1s accuracy); leave the URL empty to use device time",
    time_source_url: 'Time source URL',
    time_source_url_hint: 'Empty: use the weather proxy or this site',
    ok: 'OK',
    close: 'Close',
    weather_credentials_group: 'Weather credentials',
    weather_credentials_desc: 'Stored in this browser only; never sent to a third party',
    weather_api_host: 'API host',
    weather_credential_id: 'Credential ID (kid)',
    weather_project_id: 'Project ID (sub)',
    weather_private_key: 'Ed25519 private key (PKCS#8 Base64)',
    weather_proxy: 'Proxy URL (optional)',
    weather_proxy_desc:
      'With a proxy the request is signed server-side: the browser never holds the key, and CORS is bypassed',
    weather_cors_hint:
      'On a CORS error, deploy proxy/cloudflare-worker.js and enter its URL above',
    alarm_background_note:
      'A web alarm needs this page to stay open; the browser will not wake it once closed',
    alarm_notification_blocked: 'Notifications are blocked; only the full-screen alert will show',
    update_available: 'A new version is available',
    update_reload: 'Update now',
    settings_applied: 'Settings applied',
    install_hint: 'Use “Add to Home Screen” in your browser menu to run full screen',
    calendar_jump_year: 'Year',
    calendar_jump_month: 'Month',
    orientation_lock_unsupported:
      'This browser cannot lock orientation; the layout follows the aspect ratio instead',
    stopwatch_no_laps: 'No laps yet',
  },
};

type Listener = () => void;
const listeners = new Set<Listener>();

let current = prefs.getClockLanguage();

export function language(): string {
  return current;
}

/** Applies a new interface language and notifies every subscriber to re-render. */
export function setLanguage(value: string): void {
  if (value === current) return;
  current = value;
  document.documentElement.lang = htmlLang(value);
  for (const listener of listeners) listener();
}

/** Re-reads the stored language (used after the settings panel applies changes). */
export function refreshLanguage(): void {
  setLanguage(prefs.getClockLanguage());
}

export function onLanguageChange(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function htmlLang(value: string): string {
  if (value === LANGUAGE_ENGLISH) return 'en';
  if (value === LANGUAGE_TRADITIONAL) return 'zh-Hant';
  return 'zh-Hans';
}

/** Looks up `key` for the active language and substitutes `%n$s` placeholders. */
export function t(key: MessageKey | string, ...args: (string | number)[]): string {
  const template =
    WEB_MESSAGES[current]?.[key] ??
    MESSAGES[current]?.[key] ??
    WEB_MESSAGES[LANGUAGE_SIMPLIFIED]?.[key] ??
    MESSAGES[LANGUAGE_SIMPLIFIED]?.[key] ??
    key;
  return substitute(template, args);
}

/** Looks up a string array (region names, weekday names, forecast labels). */
export function ta(key: ArrayKey | string): string[] {
  return ARRAYS[current]?.[key] ?? ARRAYS[LANGUAGE_SIMPLIFIED]?.[key] ?? [];
}

function substitute(template: string, args: (string | number)[]): string {
  if (!args.length) return template.replace(/%%/g, '%');
  return template.replace(/%%|%(\d+)\$[sd]/g, (match, index) => {
    if (match === '%%') return '%';
    const value = args[Number(index) - 1];
    return value === undefined ? '' : String(value);
  });
}

/** Maps a stored interface-language code to the date-formatter language. */
export function dateLang(clockLanguage: string = current): Lang {
  if (clockLanguage === LANGUAGE_ENGLISH) return 'english';
  if (clockLanguage === LANGUAGE_TRADITIONAL) return 'traditional';
  return 'chinese';
}

/** Maps the interface language to the QWeather `lang` query value. */
export function apiLang(clockLanguage: string = current): string {
  if (clockLanguage === LANGUAGE_ENGLISH) return 'en';
  if (clockLanguage === LANGUAGE_TRADITIONAL) return 'zh-hant';
  return 'zh';
}

/** BCP 47 tag for `Intl` formatting. */
export function intlLocale(clockLanguage: string = current): string {
  if (clockLanguage === LANGUAGE_ENGLISH) return 'en-US';
  if (clockLanguage === LANGUAGE_TRADITIONAL) return 'zh-Hant';
  return 'zh-Hans';
}
