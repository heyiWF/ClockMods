/**
 * @vitest-environment jsdom
 *
 * The settings sheet is built entirely in code (as SettingsDialog was), so these
 * tests construct it for real: every section must appear, edits must stay local
 * until 应用, and applying must write exactly the preferences that were changed.
 */
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { openSettings } from '../src/app/settings-panel';
import {
  LANGUAGE_ENGLISH,
  MODE_IMAGE,
  ORIENTATION_LANDSCAPE,
  TRANSITION_FLIP,
  prefs,
} from '../src/core/prefs';

beforeAll(() => {
  HTMLCanvasElement.prototype.getContext = () => null;
  // jsdom implements <dialog> but not the modal focus trap in every version.
  if (!HTMLDialogElement.prototype.showModal) {
    HTMLDialogElement.prototype.showModal = function showModal(this: HTMLDialogElement) {
      this.open = true;
    };
    HTMLDialogElement.prototype.close = function close(this: HTMLDialogElement) {
      this.open = false;
      this.dispatchEvent(new Event('close'));
    };
  }
});

beforeEach(() => {
  localStorage.clear();
  document.body.innerHTML = '<div id="toast-root"></div>';
});

function open(): { sheet: HTMLElement; applied: ReturnType<typeof vi.fn> } {
  const applied = vi.fn();
  openSettings(applied);
  const sheet = document.querySelector<HTMLElement>('.settings-sheet')!;
  return { sheet, applied };
}

const cardTitles = (sheet: HTMLElement): string[] =>
  [...sheet.querySelectorAll('.settings-card-title')].map((node) => node.textContent!);

describe('settings sheet', () => {
  it('shows both boards with every section', () => {
    const { sheet } = open();
    const titles = cardTitles(sheet);

    expect(titles).toContain('背景');
    expect(titles).toContain('字体');
    expect(titles).toContain('显示');
    expect(titles).toContain('时间');
    expect(titles).toContain('地区');
    expect(titles).toContain('界面语言');
    expect(titles).toContain('日期格式');
    expect(titles).toContain('自定义留言');
    expect(titles).toContain('天气');
    expect(titles).toContain('天气服务凭据');
    expect(titles).toContain('月历');
    // The Android status-bar group is deliberately gone.
    expect(titles).not.toContain('状态栏');
  });

  it('opens eight categories and returns to the category home', () => {
    const { sheet } = open();
    const panes = sheet.querySelectorAll<HTMLElement>('.ultimate-settings-pane');
    const buttons = sheet.querySelectorAll<HTMLButtonElement>('.settings-category');
    expect(panes).toHaveLength(8); expect(buttons).toHaveLength(8);
    expect(panes[0].hidden).toBe(false);
    buttons[4].click(); expect(panes[0].hidden).toBe(true);expect(panes[4].hidden).toBe(false);
    expect(buttons[4].getAttribute('aria-current')).toBe('page');
    sheet.querySelector<HTMLButtonElement>('.settings-back')!.click();expect(sheet.classList.contains('is-detail')).toBe(false);
  });

  it('does not touch preferences until 应用 is pressed', () => {
    const { sheet } = open();
    const bold = sheet.querySelector<HTMLInputElement>('[data-category="0"] [aria-label="字重"]')!;
    bold.value = '1';
    bold.dispatchEvent(new Event('change'));

    expect(prefs.isBoldText()).toBe(false);

    sheet.querySelector<HTMLElement>('.settings-header .m3-button:not(.m3-button--text)')!.click();
    expect(prefs.isBoldText()).toBe(true);
  });

  it('writes the changed style settings on apply', () => {
    const { sheet } = open();
    setSwitch(sheet, '显示农历', false);
    setSwitch(sheet, '冒号每秒闪烁', true);
    setSwitch(sheet, '竖屏时钟竖排大字', true);
    sheet.querySelector<HTMLInputElement>('[data-category="0"] [aria-label="时间字号"]')!.value = '120';
    const transition = sheet.querySelector<HTMLSelectElement>('[aria-label="数字过渡动画"]')!;
    transition.value = TRANSITION_FLIP;

    apply(sheet);

    expect(prefs.isShowLunar()).toBe(false);
    expect(prefs.isBlinkColon()).toBe(true);
    expect(prefs.isPortraitStacked()).toBe(true);
    expect(prefs.getTimeFontScale()).toBeCloseTo(1.2, 5);
    expect(prefs.getTimeTransition()).toBe(TRANSITION_FLIP);
  });

  it('writes the function settings on apply', () => {
    const { sheet } = open();
    setSwitch(sheet, '使用 24 小时制', false);
    setSegment(sheet, '强制横屏');
    setSegment(sheet, 'English');
    selectByLabel(sheet, '中国（乌鲁木齐）');
    inputInCard(sheet, '自定义留言').value = '晚安';

    apply(sheet);

    expect(prefs.isUse24Hour()).toBe(false);
    expect(prefs.getScreenOrientation()).toBe(ORIENTATION_LANDSCAPE);
    expect(prefs.getClockLanguage()).toBe(LANGUAGE_ENGLISH);
    expect(prefs.getTimeZoneId()).toBe('Asia/Urumqi');
    expect(prefs.getCustomMessage()).toBe('晚安');
  });

  it('reports a language change to the caller', () => {
    const { sheet, applied } = open();
    apply(sheet);
    expect(applied).toHaveBeenCalledWith(false);

    const second = open();
    setSegment(second.sheet, 'English');
    apply(second.sheet);
    expect(second.applied).toHaveBeenCalledWith(true);
  });

  it('rebuilds the date-format section for the selected language', () => {
    const { sheet } = open();
    const preview = () => sheet.querySelector('.settings-preview')!.textContent!;
    expect(preview()).toBe('预览：2026 年 8 月 7 日 星期五');

    setSegment(sheet, 'English');
    // The section now offers English cores and previews the English default.
    expect(preview()).toBe('预览：2026/8/7 Friday');
  });

  it('validates a hand-written date pattern', () => {
    const { sheet } = open();
    const coreSelect = [...sheet.querySelectorAll<HTMLSelectElement>('.settings-select')].find(
      (node) => [...node.options].some((option) => option.textContent === '自定义…')
    )!;
    coreSelect.value = String(coreSelect.options.length - 1);
    coreSelect.dispatchEvent(new Event('change'));

    const custom = sheet.querySelector<HTMLInputElement>('.settings-date-format .settings-input')!;
    const error = sheet.querySelector<HTMLElement>('.settings-error')!;

    custom.value = '年月日'; // no field token
    custom.dispatchEvent(new Event('input'));
    expect(error.hidden).toBe(false);

    custom.value = 'M月d日';
    custom.dispatchEvent(new Event('input'));
    expect(error.hidden).toBe(true);

    apply(sheet);
    expect(prefs.getDatePatternCn()).toBe('M月d日');
  });

  it('refuses to apply image mode without a stored image', async () => {
    const { sheet, applied } = open();
    setSegment(sheet, '图片');
    apply(sheet);

    // The apply handler awaits the IndexedDB lookup before it can refuse.
    await vi.waitFor(() => {
      expect(document.querySelector('.toast')?.textContent).toBe('请先选择一张图片');
    });
    expect(applied).not.toHaveBeenCalled();
    expect(prefs.getBackgroundMode()).not.toBe(MODE_IMAGE);
  });

  it('stores QWeather credentials and strips a trailing proxy slash', () => {
    const { sheet } = open();
    const inputs = [...sheet.querySelectorAll<HTMLInputElement>('.settings-input')];
    const host = inputs.find((input) => input.value === 'devapi.qweather.com')!;
    const password = inputs.filter((input) => input.type === 'password');
    host.value = 'abc.re.qweatherapi.com';
    password[0].value = 'cred';
    password[1].value = 'proj';
    password[2].value = 'key';
    const proxy = inputs.filter((input) => input.type === 'url').at(-1)!;
    proxy.value = 'https://proxy.example.com/';

    apply(sheet);

    expect(prefs.getQWeatherApiHost()).toBe('abc.re.qweatherapi.com');
    expect(prefs.getQWeatherCredentialId()).toBe('cred');
    expect(prefs.getQWeatherProxy()).toBe('https://proxy.example.com');
    expect(prefs.isWeatherConfigured()).toBe(true);
  });

  it('restores defaults in the sheet without writing them', () => {
    prefs.setBoldText(true);
    const { sheet } = open();
    expect(sheet.querySelector<HTMLInputElement>('[data-category="0"] [aria-label="字重"]')!.value).toBe('1');

    sheet.querySelector<HTMLElement>('.settings-reset')!.click();
    expect(sheet.querySelector<HTMLInputElement>('[data-category="0"] [aria-label="字重"]')!.value).toBe('0');
    // Still unwritten until 应用.
    expect(prefs.isBoldText()).toBe(true);

    apply(sheet);
    expect(prefs.isBoldText()).toBe(false);
  });
  it('keeps separate theme drafts through switches, apply and reopening', () => {
    const {sheet}=open();
    const theme=sheet.querySelector<HTMLSelectElement>('[aria-label="时钟主题"]')!;
    const font=sheet.querySelector<HTMLSelectElement>('[aria-label="时钟字体"]')!;
    const transition=sheet.querySelector<HTMLSelectElement>('[aria-label="数字过渡动画"]')!;
    const change=(id:string)=>{theme.value=id;theme.dispatchEvent(new Event('change'));};
    change('ultimate.bubbles');font.value='lora';transition.value='scan';
    change('ultimate.blend');font.value='inter';transition.value='slide_right';
    change('ultimate.bubbles');expect(font.value).toBe('lora');expect(transition.value).toBe('scan');
    expect(prefs.getFontFamily('ultimate.bubbles')).toBe('system');apply(sheet);
    expect(prefs.getFontFamily('ultimate.bubbles')).toBe('lora');expect(prefs.getFontFamily('ultimate.blend')).toBe('inter');
    expect(prefs.getTimeTransition('ultimate.blend')).toBe('slide_right');expect(prefs.getFontFamily('classic')).toBe('system');
    const reopened=open();expect(reopened.sheet.querySelector<HTMLSelectElement>('[aria-label="时钟字体"]')!.value).toBe('lora');
  });
  it('saves all additional controls and restores their values when reopened', () => {
    const {sheet}=open();
    for(const label of ['显示设备状态','启用世界时钟','半点报时','防烧屏位移','保护时降低亮度'])setSwitch(sheet,label,true);
    setSwitch(sheet,'避让刘海与屏幕边缘',false);
    const values:Record<string,string>={'秒针模式':'smooth','状态图标大小':'150','状态图标样式':'filled','报时动画':'comet','移动间隔':'1','移动幅度':'12','日历主题':'calendar.paper','滚动速度':'80','滚动停顿':'2000','滚动间距':'48'};
    for(const[label,value]of Object.entries(values)){const input=sheet.querySelector<HTMLInputElement>('[aria-label="'+label+'"]')!;input.value=value;input.dispatchEvent(new Event('change'));}
    apply(sheet);
    expect(prefs.getUltimateOptions()).toMatchObject({secondMotion:'smooth',statusIcons:true,statusScale:150,statusStyle:'filled',avoidCutout:false,worldEnabled:true,halfHourChime:true,chimeAnimation:'comet',burnIn:true,burnInterval:1,burnAmplitude:12,burnDim:true,calendarTheme:'calendar.paper',marqueeSpeed:80,marqueePause:2000,marqueeGap:48});
    const reopened=open();for(const[label,value]of Object.entries(values))expect(reopened.sheet.querySelector<HTMLInputElement>('[aria-label="'+label+'"]')!.value).toBe(value);
  });
  it('previews and saves explicit equal pixel sizes while retaining per-theme drafts',()=>{
    const {sheet}=open();const theme=sheet.querySelector<HTMLSelectElement>('[aria-label="时钟主题"]')!;
    const choose=(id:string)=>{theme.value=id;theme.dispatchEvent(new Event('change'));};choose('ultimate.bubbles');
    const date=sheet.querySelector<HTMLInputElement>('[data-category="0"] [aria-label="日期字号"]')!,support=sheet.querySelector<HTMLInputElement>('[data-category="0"] [aria-label="辅助文字字号"]')!;
    expect([date.min,date.max]).toEqual([support.min,support.max]);date.value=support.value='36';choose('ultimate.ribbon');expect(date.value).toBe('24');choose('ultimate.bubbles');expect(date.value).toBe('36');expect(support.value).toBe('36');expect(prefs.getDateFontSize('ultimate.bubbles')).toBe(24);apply(sheet);expect(prefs.getDateFontSize('ultimate.bubbles')).toBe(36);expect(prefs.getSupportingFontSize('ultimate.bubbles')).toBe(36);
  });
  it('stores glass per theme with Ultimate defaults and preserves drafts',()=>{
    const {sheet}=open();const theme=sheet.querySelector<HTMLSelectElement>('[aria-label="时钟主题"]')!;
    const change=(id:string)=>{theme.value=id;theme.dispatchEvent(new Event('change'));};
    change('ultimate.bubbles');setSwitch(sheet,'卡片高斯模糊',true);sheet.querySelector<HTMLInputElement>('[aria-label="模糊强度"]')!.value='80';sheet.querySelector<HTMLInputElement>('[aria-label="卡片亮度"]')!.value='50';
    change('ultimate.blend');expect(sheet.querySelector<HTMLInputElement>('[aria-label="模糊强度"]')!.value).toBe('50');
    change('ultimate.bubbles');expect(sheet.querySelector<HTMLInputElement>('[aria-label="模糊强度"]')!.value).toBe('80');apply(sheet);
    expect(prefs.getThemeGlass('ultimate.bubbles')).toEqual({enabled:true,strength:80,brightness:50});expect(prefs.getThemeGlass('ultimate.blend')).toEqual({enabled:false,strength:50,brightness:25});
  });
  it('cancel discards edits across categories, themes and an imported image', () => {
    const {sheet}=open();const before=JSON.stringify(localStorage);
    setSwitch(sheet,'半点报时',true);setSwitch(sheet,'防烧屏位移',true);
    const file=sheet.querySelector<HTMLInputElement>('input[type=file]')!;
    Object.defineProperty(file,'files',{value:[new File(['not-yet-decoded'],'image.png',{type:'image/png'})]});file.dispatchEvent(new Event('change'));
    sheet.querySelector<HTMLButtonElement>('.settings-header .m3-button--text:not(.settings-back)')!.click();
    expect(JSON.stringify(localStorage)).toBe(before);expect(document.querySelector('.settings-sheet')).toBeNull();
  });
  it('preserves date drafts when switching interface language twice', () => {
    const {sheet}=open();const core=sheet.querySelector<HTMLSelectElement>('.settings-date-format select')!;
    core.value=String(core.options.length-1);core.dispatchEvent(new Event('change'));
    const custom=sheet.querySelector<HTMLInputElement>('.settings-date-format input')!;custom.value='M月d日';custom.dispatchEvent(new Event('input'));
    setSegment(sheet,'English');setSegment(sheet,'简体中文');
    expect(sheet.querySelector<HTMLInputElement>('.settings-date-format input')!.value).toBe('M月d日');apply(sheet);expect(prefs.getDatePatternCn()).toBe('M月d日');
  });
  it('keeps calendar typography separate from clock and other calendar themes', () => {
    const {sheet}=open();const theme=sheet.querySelector<HTMLSelectElement>('[aria-label="日历主题"]')!;const font=sheet.querySelector<HTMLSelectElement>('[aria-label="日历字体"]')!;
    font.value='lora';theme.value='calendar.paper';theme.dispatchEvent(new Event('change'));font.value='inter';apply(sheet);
    expect(prefs.getFontFamily('calendar.graphite')).toBe('lora');expect(prefs.getFontFamily('calendar.paper')).toBe('inter');expect(prefs.getFontFamily('classic')).toBe('system');
  });
  it('removes, reorders and adds world clocks without affecting saved order before apply', () => {
    const {sheet}=open();const initial=prefs.getUltimateOptions().worldZones;
    sheet.querySelector<HTMLButtonElement>('[aria-label="下移 Asia/Shanghai"]')!.click();
    sheet.querySelector<HTMLButtonElement>('[aria-label="删除 America/New_York"]')!.click();
    expect(prefs.getUltimateOptions().worldZones).toEqual(initial);apply(sheet);
    expect(prefs.getUltimateOptions().worldZones).toEqual(['Europe/London','Asia/Shanghai']);
  });
  it('enables chime animation only while hourly or half-hour chime is active',()=>{
    prefs.setHourlyChimeEnabled(false);const {sheet}=open();const animation=sheet.querySelector<HTMLSelectElement>('[aria-label="报时动画"]')!;
    expect(animation.disabled).toBe(true);setSwitch(sheet,'半点报时',true);expect(animation.disabled).toBe(false);setSwitch(sheet,'半点报时',false);expect(animation.disabled).toBe(true);
  });
  it('enables dependent controls and resets extras without writing until apply', () => {
    prefs.setUltimateOptions({...prefs.getUltimateOptions(),burnIn:true,halfHourChime:true});
    const {sheet}=open();setSwitch(sheet,'防烧屏位移',false);
    expect(sheet.querySelector<HTMLInputElement>('[aria-label="移动幅度"]')!.disabled).toBe(true);
    sheet.querySelector<HTMLButtonElement>('.settings-reset')!.click();expect(prefs.getUltimateOptions().halfHourChime).toBe(true);apply(sheet);
    expect(prefs.getUltimateOptions().halfHourChime).toBe(false);expect(prefs.getUltimateOptions().burnIn).toBe(false);
  });

});

/** Locates the single text input inside the card with the given title. */
function inputInCard(sheet: HTMLElement, title: string): HTMLInputElement {
  const card = [...sheet.querySelectorAll<HTMLElement>('.settings-card')].find(
    (node) => node.querySelector('.settings-card-title')?.textContent === title
  );
  const input = card?.querySelector<HTMLInputElement>('.settings-input');
  if (!input) throw new Error(`input not found in card: ${title}`);
  return input;
}

function findSwitch(sheet: HTMLElement, label: string): HTMLInputElement {
  const row = [...sheet.querySelectorAll<HTMLElement>('.settings-switch')].find(
    (node) => node.querySelector('.settings-switch-label')?.textContent === label
  );
  if (!row) throw new Error(`switch not found: ${label}`);
  return row.querySelector('input')!;
}

function setSwitch(sheet: HTMLElement, label: string, checked: boolean): void {
  const input = findSwitch(sheet, label);
  input.checked = checked;
  input.dispatchEvent(new Event('change'));
}

function setSegment(sheet: HTMLElement, label: string): void {
  const button = [...sheet.querySelectorAll<HTMLElement>('.settings-segment')].find(
    (node) => node.textContent === label
  );
  if (!button) throw new Error(`segment not found: ${label}`);
  button.click();
}

function selectByLabel(sheet: HTMLElement, label: string): void {
  for (const node of sheet.querySelectorAll<HTMLSelectElement>('.settings-select')) {
    const option = [...node.options].find((item) => item.textContent === label);
    if (option) {
      node.value = option.value;
      node.dispatchEvent(new Event('change'));
      return;
    }
  }
  throw new Error(`option not found: ${label}`);
}

function apply(sheet: HTMLElement): void {
  const buttons = sheet.querySelectorAll<HTMLElement>('.settings-header button');
  buttons[buttons.length - 1].click();
}
