/**
 * Building blocks for the settings sheet.
 *
 * These mirror the Material components SettingsDialog instantiated in code:
 * MaterialSwitch rows, Slider rows with a percentage readout,
 * MaterialButtonToggleGroup segments, Spinners and the rounded section cards.
 */

import { pangu } from '../format/text-spacing';

export function element<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className?: string,
  text?: string
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = pangu(text);
  return node;
}

/** A rounded section card with a title, matching SettingsDialog.groupIntoCards. */
export function card(title: string, ...children: Node[]): HTMLElement {
  const section = element('section', 'settings-card');
  section.appendChild(element('h3', 'settings-card-title', title));
  section.append(...children);
  return section;
}

export function subLabel(text: string): HTMLElement {
  return element('p', 'settings-sublabel', text);
}

export function summaryLabel(text: string): HTMLElement {
  return element('p', 'settings-summary', text);
}

export interface SwitchRow {
  row: HTMLElement;
  input: HTMLInputElement;
}

export function switchRow(label: string, checked: boolean, summary?: string): SwitchRow {
  const row = element('label', 'settings-switch');
  const text = element('span', 'settings-switch-label', label);
  const input = element('input');
  input.type = 'checkbox';
  input.setAttribute('role', 'switch');
  input.checked = checked;
  row.append(text, input);
  if (!summary) return { row, input };
  const wrapper = element('div', 'settings-switch-group');
  wrapper.append(row, summaryLabel(summary));
  return { row: wrapper, input };
}

export interface SliderRow {
  row: HTMLElement;
  input: HTMLInputElement;
  setEnabled(enabled: boolean): void;
}

export function sliderRow(
  label: string,
  min: number,
  max: number,
  value: number,
  format: (value: number) => string
): SliderRow {
  const row = element('div', 'settings-slider');
  const caption = element('span', 'settings-slider-label', label);
  const input = element('input');
  input.type = 'range';
  input.min = String(min);
  input.max = String(max);
  input.step = '1';
  input.value = String(value);
  const readout = element('span', 'settings-slider-value', format(value));
  input.addEventListener('input', () => {
    readout.textContent = pangu(format(Number(input.value)));
  });
  row.append(caption, input, readout);
  return {
    row,
    input,
    setEnabled(enabled: boolean) {
      input.disabled = !enabled;
      row.classList.toggle('is-disabled', !enabled);
    },
  };
}

export interface SegmentedControl<T> {
  row: HTMLElement;
  value(): T;
  setValue(value: T): void;
  onChange(listener: (value: T) => void): void;
}

/** Single-selection button group (MaterialButtonToggleGroup). */
export function segmented<T extends string | number>(
  options: Array<{ value: T; label: string }>,
  selected: T
): SegmentedControl<T> {
  const row = element('div', 'settings-segmented');
  let current = selected;
  const listeners: Array<(value: T) => void> = [];
  const buttons = options.map((option) => {
    const button = element('button', 'settings-segment', option.label);
    button.type = 'button';
    button.classList.toggle('is-active', option.value === selected);
    button.addEventListener('click', () => {
      current = option.value;
      for (const other of buttons) other.classList.remove('is-active');
      button.classList.add('is-active');
      for (const listener of listeners) listener(current);
    });
    return button;
  });
  row.append(...buttons);
  return {
    row,
    value: () => current,
    setValue(value: T) {
      current = value;
      options.forEach((option, index) => {
        buttons[index].classList.toggle('is-active', option.value === value);
      });
    },
    onChange(listener) {
      listeners.push(listener);
    },
  };
}

export function select(
  options: Array<{ value: string; label: string }>,
  selected: string
): HTMLSelectElement {
  const node = element('select', 'settings-select');
  for (const option of options) {
    const item = element('option', undefined, option.label);
    item.value = option.value;
    node.appendChild(item);
  }
  node.value = selected;
  return node;
}

export function textField(
  value: string,
  placeholder: string,
  type: 'text' | 'password' | 'url' = 'text'
): HTMLInputElement {
  const input = element('input', 'settings-input');
  input.type = type;
  input.value = value;
  input.placeholder = placeholder;
  input.autocomplete = 'off';
  input.spellcheck = false;
  return input;
}

export interface TimeButton {
  button: HTMLButtonElement;
  minutes(): number;
}

/** A button that opens the platform time picker, used for the dim and quiet windows. */
export function timeButton(labelPrefix: string, initialMinutes: number): TimeButton {
  let minutes = initialMinutes;
  const button = element('button', 'button button--outlined settings-time');
  button.type = 'button';
  const input = element('input');
  input.type = 'time';
  input.className = 'settings-time-input';
  const render = () => {
    const hh = String(Math.floor(minutes / 60)).padStart(2, '0');
    const mm = String(minutes % 60).padStart(2, '0');
    button.textContent = pangu(`${labelPrefix} ${hh}:${mm}`);
    input.value = `${hh}:${mm}`;
  };
  render();
  input.addEventListener('change', () => {
    const [hour, minute] = input.value.split(':').map(Number);
    if (Number.isFinite(hour) && Number.isFinite(minute)) {
      minutes = hour * 60 + minute;
      render();
    }
  });
  button.addEventListener('click', () => {
    input.showPicker?.();
    input.click();
  });
  button.appendChild(input);
  return { button, minutes: () => minutes };
}

/** Disables an entire subtree, matching SettingsDialog.setViewTreeEnabled. */
export function setTreeEnabled(root: HTMLElement, enabled: boolean): void {
  root.classList.toggle('is-disabled', !enabled);
  for (const control of root.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLButtonElement>(
    'input, select, button'
  )) {
    control.disabled = !enabled;
  }
}
