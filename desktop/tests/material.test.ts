/**
 * @vitest-environment jsdom
 *
 * The Material 3 layer: the press ripple ui/ripple.ts attaches by delegation,
 * and the two settings controls whose markup carries M3 structure the CSS then
 * depends on — the segmented button's check icon and the slider's fill fraction.
 */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { installRipples } from '../src/ui/ripple';
import { segmented, sliderRow } from '../src/ui/controls';

/** jsdom has no PointerEvent; the handler only reads MouseEvent fields. */
function press(target: HTMLElement, type: 'pointerdown' | 'pointerup'): void {
  target.dispatchEvent(new MouseEvent(type, { bubbles: true, button: 0, clientX: 4, clientY: 4 }));
}

describe('press ripple', () => {
  beforeEach(() => {
    document.body.replaceChildren();
    installRipples();
  });

  it('adds a press layer to an M3 surface on the first press', () => {
    const button = document.createElement('button');
    button.className = 'm3-button m3-button--filled';
    document.body.appendChild(button);

    expect(button.querySelector('.m3-ripple')).toBeNull();
    press(button, 'pointerdown');

    const layer = button.querySelector('.m3-ripple__press');
    expect(layer).not.toBeNull();
    expect(layer!.classList.contains('is-pressed')).toBe(true);
    // The ripple must sit behind the label, never in front of it.
    expect(button.firstElementChild!.classList.contains('m3-ripple')).toBe(true);
  });

  it('reuses the same layer across presses', () => {
    const button = document.createElement('button');
    button.className = 'm3-button';
    document.body.appendChild(button);

    press(button, 'pointerdown');
    press(button, 'pointerup');
    press(button, 'pointerdown');

    expect(button.querySelectorAll('.m3-ripple')).toHaveLength(1);
  });

  it('holds the press for the M3 minimum before releasing it', () => {
    vi.useFakeTimers();
    try {
      const button = document.createElement('button');
      button.className = 'm3-button';
      document.body.appendChild(button);

      press(button, 'pointerdown');
      const layer = button.querySelector('.m3-ripple__press')!;
      press(button, 'pointerup');

      // md-ripple keeps a tap visible for MINIMUM_PRESS_MS (225ms).
      expect(layer.classList.contains('is-pressed')).toBe(true);
      vi.advanceTimersByTime(225);
      expect(layer.classList.contains('is-pressed')).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it('leaves disabled surfaces alone', () => {
    const button = document.createElement('button');
    button.className = 'm3-button';
    button.disabled = true;
    document.body.appendChild(button);

    press(button, 'pointerdown');
    expect(button.querySelector('.m3-ripple')).toBeNull();
  });

  it('ignores presses outside any M3 surface', () => {
    const plain = document.createElement('div');
    document.body.appendChild(plain);

    press(plain, 'pointerdown');
    expect(plain.querySelector('.m3-ripple')).toBeNull();
  });
});

describe('segmented button', () => {
  it('marks the selected segment with a check without changing its text', () => {
    const control = segmented(
      [
        { value: 'a', label: '样式' },
        { value: 'b', label: '功能' },
      ],
      'a'
    );
    const buttons = [...control.row.querySelectorAll<HTMLElement>('.settings-segment')];

    // The check is an SVG, so the button's text is still exactly the label —
    // several tests and the settings panel itself match segments that way.
    expect(buttons.map((node) => node.textContent)).toEqual(['样式', '功能']);
    expect(buttons.every((node) => node.querySelector('.settings-segment-check'))).toBe(true);
    expect(buttons.map((node) => node.classList.contains('is-active'))).toEqual([true, false]);
    expect(buttons.map((node) => node.getAttribute('aria-pressed'))).toEqual(['true', 'false']);
  });

  it('moves the active state and aria-pressed together on selection', () => {
    const control = segmented(
      [
        { value: 'a', label: 'A' },
        { value: 'b', label: 'B' },
      ],
      'a'
    );
    const buttons = [...control.row.querySelectorAll<HTMLElement>('.settings-segment')];

    buttons[1].click();
    expect(control.value()).toBe('b');
    expect(buttons.map((node) => node.getAttribute('aria-pressed'))).toEqual(['false', 'true']);

    control.setValue('a');
    expect(buttons.map((node) => node.getAttribute('aria-pressed'))).toEqual(['true', 'false']);
  });
});

describe('slider', () => {
  it('publishes the handle position as a fraction for the active track', () => {
    const row = sliderRow('字号', 50, 150, 100, (value) => `${value}%`);

    expect(row.input.style.getPropertyValue('--m3-slider-fraction')).toBe('0.5');

    row.input.value = '150';
    row.input.dispatchEvent(new Event('input'));
    expect(row.input.style.getPropertyValue('--m3-slider-fraction')).toBe('1');

    row.input.value = '50';
    row.input.dispatchEvent(new Event('input'));
    expect(row.input.style.getPropertyValue('--m3-slider-fraction')).toBe('0');
  });
});
