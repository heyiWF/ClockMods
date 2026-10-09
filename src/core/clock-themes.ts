/** Default surfaces from UltimateClockStyles / ClockPalette, in Android order. */
const material = { background: '#154974', panel: '#23557f', accent: '#9ecafc' };
export const CLOCK_THEMES = [
  { id: 'classic', name: ['经典', '經典', 'Pro Classic'], background: '#000000', panel: '#101418', accent: '#a8c7fa' },
  { id: 'glass.atelier', name: ['玻璃工坊', '玻璃工坊', 'Glass Atelier'], background: '#f0f4f7', panel: '#f8fafc', accent: '#d9423a' },
  { id: 'noir.instrument', name: ['黑金仪表', '黑金儀表', 'Noir Instrument'], background: '#111417', panel: '#15191c', accent: '#d7a247' },
  { id: 'paper.station', name: ['纸感时站', '紙感時站', 'Paper Station'], background: '#f4f1e9', panel: '#faf7f0', accent: '#b54740' },
  { id: 'orbit.neon', name: ['霓虹轨道', '霓虹軌道', 'Orbit Neon'], background: '#10171a', panel: '#122126', accent: '#2ed9c7' },
  { id: 'digital.grid', name: ['数字网格', '數字網格', 'Digital Grid'], background: '#071110', panel: '#0a1c1a', accent: '#ffc95c' },
  { id: 'typographic.poster', name: ['字形时刻', '字形時刻', 'Typographic'], background: '#f1f0eb', panel: '#17191c', accent: '#df4b38' },
  { id: 'ultimate.dual_blocks', name: ['双块', '雙塊', 'Dual blocks'], ...material },
  { id: 'ultimate.orbit', name: ['轨道', '軌道', 'Orbit'], ...material },
  { id: 'ultimate.bubbles', name: ['气泡', '氣泡', 'Bubbles'], ...material },
  { id: 'ultimate.blend', name: ['混合', '混合', 'Blend'], ...material },
  { id: 'ultimate.ribbon', name: ['丝带', '絲帶', 'Ribbon'], ...material },
] as const;
export type ClockThemeId = (typeof CLOCK_THEMES)[number]['id'];
export function clockTheme(id: string) { return CLOCK_THEMES.find(theme => theme.id === id) ?? CLOCK_THEMES[0]; }
export function themeName(theme: (typeof CLOCK_THEMES)[number], language: string): string {
  return theme.name[language === 'en' ? 2 : language === 'zh-Hant' ? 1 : 0];
}
export function mixColor(from: string, to: string, amount: number): string {
  const a = parseInt(from.slice(1), 16), b = parseInt(to.slice(1), 16);
  return '#' + [16, 8, 0].map(shift => Math.round(((a >> shift) & 255) +
    (((b >> shift) & 255) - ((a >> shift) & 255)) * amount).toString(16).padStart(2, '0')).join('');
}
function luminance(color: string): number {
  const n = parseInt(color.slice(1), 16);
  return [16, 8, 0].reduce((sum, shift, i) => {
    const c = ((n >> shift) & 255) / 255;
    return sum + [0.2126, 0.7152, 0.0722][i] * (c <= .04045 ? c / 12.92 : ((c + .055) / 1.055) ** 2.4);
  }, 0);
}
export function contrast(a: string, b: string): number {
  const x = luminance(a), y = luminance(b);
  return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
}
/** Surface-tinted ink, adjusted to 4.5:1 as in ClockPalette.foreground. */
export function readableInk(surface: string): string {
  const light = contrast('#fefeff', surface) >= contrast('#010102', surface);
  const preferred = mixColor(surface, light ? '#f6f8fc' : '#111b2c', light ? .82 : .90);
  const extreme = light ? '#fefeff' : '#010102';
  for (let step = 0; step <= 20; step++) {
    const candidate = mixColor(preferred, extreme, step / 20);
    if (contrast(candidate, surface) >= 4.5) return candidate;
  }
  return extreme;
}
export function temperature(value: string, unit: string): string {
  const numeric = Number(value);
  if (value.trim() === '' || !Number.isFinite(numeric)) return `${value}${unit === 'fahrenheit' ? '℉' : '℃'}`;
  return unit === 'fahrenheit' ? `${Math.round(numeric * 9 / 5 + 32)}℉` : `${value}℃`;
}
export function transitionName(value: string, language: string): string {
  const ids = ['fade', 'slide_up', 'slide_down', 'scale', 'flip', 'slide_right', 'scan'];
  const index = Math.max(0, ids.indexOf(value));
  const labels = language === 'en' ? ['Fade', 'Slide up', 'Slide down', 'Scale', 'Flip', 'Slide right', 'Scan']
    : language === 'zh-Hant' ? ['淡變', '上滑', '下滑', '縮放', '翻頁', '右滑', '掃描']
    : ['淡变', '上滑', '下滑', '缩放', '翻页', '右滑', '扫描'];
  return labels[index];
}
