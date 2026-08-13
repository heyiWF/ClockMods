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

  it('starts on the style board and switches to the function board', () => {
    const { sheet } = open();
    const boards = sheet.querySelectorAll<HTMLElement>('.settings-board');
    expect(boards[0].hidden).toBe(false);
    expect(boards[1].hidden).toBe(true);

    const tabs = sheet.querySelectorAll<HTMLElement>('.settings-segmented .settings-segment');
    tabs[1].click();
    expect(boards[0].hidden).toBe(true);
    expect(boards[1].hidden).toBe(false);
  });

  it('does not touch preferences until 应用 is pressed', () => {
    const { sheet } = open();
    const bold = findSwitch(sheet, '粗体文本');
    bold.checked = true;
    bold.dispatchEvent(new Event('change'));

    expect(prefs.isBoldText()).toBe(false);

    sheet.querySelector<HTMLElement>('.settings-header .button:not(.button--text)')!.click();
    expect(prefs.isBoldText()).toBe(true);
  });

  it('writes the changed style settings on apply', () => {
    const { sheet } = open();
    setSwitch(sheet, '显示农历', false);
    setSwitch(sheet, '冒号每秒闪烁', true);
    setSwitch(sheet, '竖屏时钟竖排大字', true);
    const sliders = sheet.querySelectorAll<HTMLInputElement>('.settings-slider input');
    sliders[0].value = '120';
    const transition = sheet.querySelectorAll<HTMLSelectElement>('.settings-select')[1];
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
    expect(findSwitch(sheet, '粗体文本').checked).toBe(true);

    sheet.querySelector<HTMLElement>('.settings-reset')!.click();
    expect(findSwitch(sheet, '粗体文本').checked).toBe(false);
    // Still unwritten until 应用.
    expect(prefs.isBoldText()).toBe(true);

    apply(sheet);
    expect(prefs.isBoldText()).toBe(false);
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
