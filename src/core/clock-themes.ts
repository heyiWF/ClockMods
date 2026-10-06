export const CLOCK_THEMES = [
  { id: 'classic', name: ['经典', '經典', 'Classic'], background: '#000000', panel: '#101418', accent: '#a8c7fa' },
  { id: 'ultimate.dual_blocks', name: ['双区块', '雙區塊', 'Dual blocks'], background: '#eef3fa', panel: '#dce7f7', accent: '#435f91' },
  { id: 'ultimate.orbit', name: ['轨道', '軌道', 'Orbit'], background: '#141b24', panel: '#233044', accent: '#a8c7fa' },
  { id: 'ultimate.bubbles', name: ['气泡', '氣泡', 'Bubbles'], background: '#faf0f6', panel: '#efd9e6', accent: '#864e78' },
  { id: 'ultimate.blend', name: ['融合', '融合', 'Blend'], background: '#eef5ed', panel: '#d7e8d4', accent: '#41683b' },
  { id: 'ultimate.ribbon', name: ['丝带', '絲帶', 'Ribbon'], background: '#faf2e7', panel: '#efddc4', accent: '#7f5830' },
] as const;

export type ClockThemeId = (typeof CLOCK_THEMES)[number]['id'];
export function clockTheme(id: string) {
  return CLOCK_THEMES.find(theme => theme.id === id) ?? CLOCK_THEMES[0];
}
export function themeName(theme: (typeof CLOCK_THEMES)[number], language: string): string {
  return theme.name[language === 'en' ? 2 : language === 'zh-Hant' ? 1 : 0];
}
export function readableInk(color: string): string {
  const value = parseInt(color.slice(1), 16);
  const luma = ((value >> 16) * 299 + ((value >> 8) & 255) * 587 + (value & 255) * 114) / 1000;
  return luma >= 150 ? '#17212c' : '#f4f7fc';
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
