import { prefs } from '../core/prefs';
import { CLOCK_THEMES, clockTheme, themeName, transitionName } from '../core/clock-themes';
import { card, select, sliderRow, subLabel, switchRow } from './controls';
import { createColorPicker } from './color-picker';

/** A draft, like the rest of the settings sheet: cancel never changes preferences. */
export function themeSettings() {
  const language = prefs.getClockLanguage();
  const english = language === 'en';
  const traditional = language === 'zh-Hant';
  const label = (cn: string, en: string, tw = cn) => english ? en : traditional ? tw : cn;
  const theme = select(CLOCK_THEMES.map(value => ({ value: value.id, label: themeName(value, language) })), prefs.getClockTheme());
  theme.setAttribute('aria-label', label('时钟主题', 'Clock theme', '時鐘主題'));
  const colorLabels = { advanced: label('自定义颜色', 'Custom color', '自訂顏色'), swatch: (color: string) => color };
  const current = prefs.getThemePalette();
  const background = createColorPicker(parseInt(current.background.slice(1), 16) | 0xff000000, [], colorLabels);
  const panel = createColorPicker(parseInt(current.panel.slice(1), 16) | 0xff000000, [], colorLabels);
  const accent = createColorPicker(parseInt(current.accent.slice(1), 16) | 0xff000000, [], colorLabels);
  let colors = { ...current };
  let resetAll = false;
  background.onChange(value => colors.background = hex(value));
  panel.onChange(value => colors.panel = hex(value));
  accent.onChange(value => colors.accent = hex(value));
  const autoInk = switchRow(label('自动文字颜色', 'Automatic text contrast', '自動文字顏色'), prefs.isThemeAutoInk());
  const shadow = switchRow(label('卡片阴影', 'Card shadows', '卡片陰影'), prefs.isCardShadow());
  const support = sliderRow(label('辅助文字字号', 'Supporting text size', '輔助文字字號'), 50, 200, Math.round(prefs.getSupportingScale() * 100), value => `${value}%`);
  const transitions = ['fade', 'slide_up', 'slide_down', 'scale', 'flip', 'slide_right', 'scan'];
  const transition = select(transitions.map(value => ({ value, label: transitionName(value, language) })), prefs.getWeatherTransition());
  const unit = select([{ value: 'celsius', label: '℃' }, { value: 'fahrenheit', label: '℉' }], prefs.getTemperatureUnit());
  theme.addEventListener('change', () => {
    colors = { ...(resetAll ? clockTheme(theme.value) : prefs.getThemePalette(theme.value)) };
    background.setValue(parseInt(colors.background.slice(1), 16) | 0xff000000);
    panel.setValue(parseInt(colors.panel.slice(1), 16) | 0xff000000);
    accent.setValue(parseInt(colors.accent.slice(1), 16) | 0xff000000);
    transition.value = resetAll ? 'fade' : prefs.getWeatherTransition(theme.value);
  });
  const root = card(label('主题与配色', 'Theme and palette', '主題與配色'), theme,
    subLabel(label('背景', 'Background')), background.root,
    subLabel(label('卡片', 'Cards')), panel.root,
    subLabel(label('强调色', 'Accent', '強調色')), accent.root, autoInk.row, shadow.row, support.row,
    subLabel(label('天气轮播动效', 'Weather transition', '天氣輪播動效')), transition,
    subLabel(label('温度单位', 'Temperature unit', '溫度單位')), unit);
  return { root,
    onThemeChanged(callback: (id: string) => void) { theme.addEventListener('change', () => callback(theme.value)); },
    usesAutomaticInk: () => theme.value !== 'classic' && autoInk.input.checked,
    apply() {
      if (resetAll) prefs.clearThemeOverrides();
      prefs.setThemeAutoInk(autoInk.input.checked); prefs.setClockTheme(theme.value); prefs.setThemePalette(colors); prefs.setCardShadow(shadow.input.checked);
      prefs.setSupportingScale(Number(support.input.value) / 100);
      prefs.setWeatherTransition(transition.value); prefs.setTemperatureUnit(unit.value);
    },
    reset() {
      resetAll = true;
      theme.value = 'classic'; colors = { ...clockTheme('classic') };
      background.setValue(0xff000000); panel.setValue(0xff101418); accent.setValue(0xffa8c7fa);
      autoInk.input.checked = true; shadow.input.checked = true; support.input.value = '100';
      support.input.dispatchEvent(new Event('input')); transition.value = 'fade'; unit.value = 'celsius';
    },
  };
}
function hex(value: number): string { return `#${(value & 0xffffff).toString(16).padStart(6, '0')}`; }
