import {secondaryFontSize,secondarySizeValue,saveSecondarySize} from './secondary-font-size';
import { prefs, DEFAULT_GLASS, type ThemeGlass } from '../core/prefs';
import { CLOCK_THEMES, clockTheme, themeName, transitionName } from '../core/clock-themes';
import { card, select, sliderRow, subLabel, switchRow, element } from './controls';
import { createColorPicker } from './color-picker';
import { UltimateFace } from './ultimate-face';

/** All theme edits remain in the sheet, including when visiting another theme. */
export function themeSettings() {
  const language = prefs.getClockLanguage();
  const label = (cn: string, en: string) => language === 'en' ? en : cn;
  const theme = select(CLOCK_THEMES.map(value => ({ value: value.id, label: themeName(value, language) })), prefs.getClockTheme());
  theme.setAttribute('aria-label', label('时钟主题', 'Clock theme'));
  type Draft = { glass: ThemeGlass; colors: ReturnType<typeof prefs.getThemePalette>; auto: boolean; shadow: boolean; support: number; transition: string };
  const drafts = new Map<string, Draft>();
  let active = theme.value, resetAll = false;
  const read = (id: string): Draft => drafts.get(id) ?? { glass: resetAll ? {...DEFAULT_GLASS} : prefs.getThemeGlass(id), colors: { ...(resetAll ? clockTheme(id) : prefs.getThemePalette(id)) }, auto: resetAll || prefs.isThemeAutoInk(id), shadow: resetAll || prefs.isCardShadow(id), support: secondarySizeValue('supporting',id,resetAll), transition: resetAll ? 'fade' : prefs.getWeatherTransition(id) };
  let colors = read(active).colors;
  const colorLabels = { advanced: label('自定义颜色', 'Custom color'), swatch: (color: string) => color };
  const int = (hex: string) => parseInt(hex.slice(1), 16) | 0xff000000;
  const background = createColorPicker(int(colors.background), [], colorLabels);
  const panel = createColorPicker(int(colors.panel), [], colorLabels);
  const accent = createColorPicker(int(colors.accent), [], colorLabels);
  background.onChange(value => { colors.background = hex(value); paint(); });
  panel.onChange(value => { colors.panel = hex(value); paint(); });
  accent.onChange(value => { colors.accent = hex(value); paint(); });
  const autoInk = switchRow(label('自动文字颜色', 'Automatic text contrast'), read(active).auto);
  const shadow = switchRow(label('卡片阴影', 'Card shadows'), read(active).shadow);
  const support = secondaryFontSize(label('辅助文字字号', 'Supporting text size'),'supporting',active);
  const transition = select(['fade', 'slide_up', 'slide_down', 'scale', 'flip', 'slide_right', 'scan'].map(value => ({ value, label: transitionName(value, language) })), read(active).transition);
  transition.setAttribute('aria-label', label('天气轮播动效', 'Weather transition'));
  const unit = select([{ value: 'celsius', label: '℃' }, { value: 'fahrenheit', label: '℉' }], prefs.getTemperatureUnit());
  unit.setAttribute('aria-label', label('温度单位', 'Temperature unit'));
  const blur = switchRow(label('卡片高斯模糊', 'Gaussian glass cards'), read(active).glass.enabled,
    label('仅在图片背景下作用于此主题的卡片，背景图片保持清晰。支持五款 Ultimate 卡片主题。', 'Blurs the image through cards only; the wallpaper stays sharp. Available for the five Ultimate card themes.'));
  const strength = sliderRow(label('模糊强度','Blur strength'),0,100,read(active).glass.strength,v=>v+'%');
  const brightness = sliderRow(label('卡片亮度','Card brightness'),0,100,read(active).glass.brightness,v=>v+'%');
  const glassControls = element('div','settings-group');glassControls.append(blur.row,strength.row,brightness.row);
  const syncGlass=()=>{const enabled=blur.input.checked;strength.setEnabled(enabled);brightness.setEnabled(enabled);};
  blur.input.addEventListener('change',syncGlass);syncGlass();
  const gallery = element('div', 'theme-gallery');
  const previews: Array<{ id: string; button: HTMLButtonElement; host: HTMLElement; face?: UltimateFace }> = [];
  for (const item of CLOCK_THEMES) {
    const button = element('button', 'theme-choice'); button.type = 'button';
    const viewport = element('div', 'theme-preview');
    const host = element('div', 'theme-preview-face'); viewport.append(host);
    let face: UltimateFace | undefined;
    if (item.id !== 'classic') {
      const rows = {date: element('div', 'clock-date', '10月08日 星期四'), lunar: element('div', 'clock-lunar', '八月廿八'), weather: element('div'), detail: element('div')};
      rows.weather.hidden = rows.detail.hidden = true;
      const settings = { ...prefs, getFontFamily: () => 'system', isBoldText: () => false, hasFontWeight: () => false, getTimeFontScale: () => .88, getDateFontScale: () => .55, getSupportingScale: () => 1, getDateFontSize:()=>24,getSupportingFontSize:()=>24,
        getThemePalette: () => item.id === active ? colors : read(item.id).colors, isThemeAutoInk: () => true };
      face = new UltimateFace(host, rows, settings);
    } else { host.classList.add('theme-preview-classic'); host.textContent = '12:08:36'; }
    button.append(viewport, element('span', 'theme-choice-name', themeName(item, language)));
    button.addEventListener('click', () => { theme.value = item.id; theme.dispatchEvent(new Event('change', {bubbles:true})); });
    previews.push({ id: item.id, button, host, face }); gallery.append(button);
  }
  function paint() {
    for (const preview of previews) {
      preview.button.setAttribute('aria-pressed', String(preview.id === theme.value));
      preview.host.style.background = (preview.id === active ? colors : read(preview.id).colors).background;
      preview.face?.render(preview.id, 320, 180, {hour:12,minute:8,second:36,month0:9,day:8,showSeconds:true,use24Hour:true,options:{animate:false,transition:'fade',colonVisible:true}});
    }
  }
  const capture = () => drafts.set(active, { glass:{enabled:blur.input.checked,strength:+strength.input.value,brightness:+brightness.input.value}, colors: {...colors}, auto: autoInk.input.checked, shadow: shadow.input.checked, support: support.value(), transition: transition.value });
  const load = () => {
    const draft = read(theme.value); colors = {...draft.colors};
    blur.input.checked=draft.glass.enabled;strength.input.value=String(draft.glass.strength);brightness.input.value=String(draft.glass.brightness);strength.input.dispatchEvent(new Event('input'));brightness.input.dispatchEvent(new Event('input'));glassControls.hidden=!active.startsWith('ultimate.');syncGlass();
    background.setValue(int(colors.background)); panel.setValue(int(colors.panel)); accent.setValue(int(colors.accent));
    autoInk.input.checked = draft.auto; shadow.input.checked = draft.shadow; support.setTheme(active,draft.support);
    support.input.dispatchEvent(new Event('input')); transition.value = draft.transition; paletteControls.hidden = active === 'classic'; paint();
  };
  theme.addEventListener('change', () => { capture(); active = theme.value; load(); });
  const restore = element('button', 'm3-button m3-button--outlined', label('恢复当前主题原版配色', 'Restore original palette'));
  restore.type = 'button';
  restore.addEventListener('click', () => { colors = {...clockTheme(active)}; capture(); load(); });
  const paletteControls = element('div','settings-group');
  paletteControls.hidden = active === 'classic';
  paletteControls.append(restore,
    subLabel(label('背景', 'Background')), background.root, subLabel(label('卡片', 'Cards')), panel.root,
    subLabel(label('强调色', 'Accent')), accent.root, autoInk.row, shadow.row);
  const root = card(label('主题与配色', 'Theme and palette'), gallery, theme, paletteControls, glassControls,
    subLabel(label('天气轮播动效', 'Weather transition')), transition);
  const temperature = card(label('温度单位', 'Temperature unit'), unit);
  glassControls.hidden=!active.startsWith('ultimate.');
  paint();
  return { root, temperature, supportRow: support.row, selected: () => theme.value,
    previewSettings(): Partial<typeof prefs> {
      return { getClockTheme:()=>active, getThemePalette:()=>({...colors}),
        getThemeGlass:()=>({enabled:blur.input.checked,strength:+strength.input.value,brightness:+brightness.input.value}),
        isThemeAutoInk:()=>autoInk.input.checked,isCardShadow:()=>shadow.input.checked,
        getSupportingScale:()=>support.value()/(active==='classic'?100:24),getSupportingFontSize:()=>support.value(),getWeatherTransition:()=>transition.value,getTemperatureUnit:()=>unit.value };
    },
    onThemeChanged(callback: (id: string) => void) { theme.addEventListener('change', () => callback(theme.value)); },
    usesAutomaticInk: () => theme.value !== 'classic' && autoInk.input.checked,
    apply() {
      capture(); if (resetAll) prefs.clearThemeOverrides();
      for (const [id, draft] of drafts) {
        prefs.setThemeGlass(draft.glass,id); prefs.setThemePalette(draft.colors, id); prefs.setThemeAutoInk(draft.auto, id); prefs.setCardShadow(draft.shadow, id);
        saveSecondarySize('supporting',id,draft.support); prefs.setWeatherTransition(draft.transition, id);
      }
      prefs.setClockTheme(theme.value); prefs.setTemperatureUnit(unit.value);
    },
    reset() { resetAll = true; drafts.clear(); theme.value = active = 'classic'; load(); unit.value = 'celsius'; },
  };
}
function hex(value: number): string { return '#' + (value & 0xffffff).toString(16).padStart(6, '0'); }
