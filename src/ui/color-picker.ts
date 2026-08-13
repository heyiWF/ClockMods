/**
 * HSV colour picker.
 *
 * Ported from com.clockmods.ui.ColorPickerView: a saturation/value square above a
 * hue strip, both drawn to a canvas and draggable, with the same 8 quick swatches
 * SettingsDialog offered above it.
 */
import { argbFromHex, cssColor } from '../core/prefs';
import { element } from './controls';

export interface ColorPicker {
  root: HTMLElement;
  value(): number;
  setValue(argb: number): void;
  onChange(listener: (argb: number) => void): void;
}

export interface ColorPickerLabels {
  /** Label of the button that reveals the HSV panel. */
  advanced: string;
  /** Accessible name for a quick swatch, given its hex value. */
  swatch: (hex: string) => string;
}

export function createColorPicker(
  initial: number,
  swatches: number[],
  labels: ColorPickerLabels
): ColorPicker {
  const root = element('div', 'color-picker');
  let hsv = rgbToHsv(initial);
  const listeners: Array<(argb: number) => void> = [];

  const preview = element('div', 'color-preview');
  const swatchRow = element('div', 'color-swatches');
  const advancedToggle = element('button', 'button button--text color-advanced');
  advancedToggle.type = 'button';
  const advanced = element('div', 'color-advanced-panel');
  advanced.hidden = true;

  const square = element('canvas', 'color-square');
  square.width = 240;
  square.height = 160;
  const hueStrip = element('canvas', 'color-hue');
  hueStrip.width = 240;
  hueStrip.height = 24;
  advanced.append(square, hueStrip);

  const emit = () => {
    const argb = hsvToArgb(hsv);
    preview.style.background = cssColor(argb);
    for (const listener of listeners) listener(argb);
  };

  const drawSquare = () => {
    const context = square.getContext('2d');
    if (!context) return;
    const { width, height } = square;
    const base = context.createLinearGradient(0, 0, width, 0);
    base.addColorStop(0, '#ffffff');
    base.addColorStop(1, `hsl(${hsv.h} 100% 50%)`);
    context.fillStyle = base;
    context.fillRect(0, 0, width, height);
    const shade = context.createLinearGradient(0, 0, 0, height);
    shade.addColorStop(0, 'rgba(0,0,0,0)');
    shade.addColorStop(1, '#000000');
    context.fillStyle = shade;
    context.fillRect(0, 0, width, height);
    // Selection marker, dark on light areas and light on dark ones.
    const x = hsv.s * width;
    const y = (1 - hsv.v) * height;
    context.beginPath();
    context.arc(x, y, 7, 0, Math.PI * 2);
    context.strokeStyle = hsv.v > 0.55 ? '#000000' : '#ffffff';
    context.lineWidth = 2;
    context.stroke();
  };

  const drawHue = () => {
    const context = hueStrip.getContext('2d');
    if (!context) return;
    const { width, height } = hueStrip;
    const gradient = context.createLinearGradient(0, 0, width, 0);
    for (let stop = 0; stop <= 6; stop++) {
      gradient.addColorStop(stop / 6, `hsl(${stop * 60} 100% 50%)`);
    }
    context.fillStyle = gradient;
    context.fillRect(0, 0, width, height);
    const x = (hsv.h / 360) * width;
    context.beginPath();
    context.rect(x - 2, 0, 4, height);
    context.strokeStyle = '#ffffff';
    context.lineWidth = 2;
    context.stroke();
  };

  const redraw = () => {
    drawSquare();
    drawHue();
  };

  const track = (canvas: HTMLCanvasElement, apply: (fx: number, fy: number) => void) => {
    const update = (event: PointerEvent) => {
      const rect = canvas.getBoundingClientRect();
      apply(
        Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width)),
        Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height))
      );
      redraw();
      emit();
    };
    canvas.addEventListener('pointerdown', (event) => {
      canvas.setPointerCapture(event.pointerId);
      update(event);
    });
    canvas.addEventListener('pointermove', (event) => {
      if (canvas.hasPointerCapture(event.pointerId)) update(event);
    });
  };

  track(square, (fx, fy) => {
    hsv = { ...hsv, s: fx, v: 1 - fy };
  });
  track(hueStrip, (fx) => {
    hsv = { ...hsv, h: fx * 360 };
  });

  for (const swatch of swatches) {
    const button = element('button', 'color-swatch');
    button.type = 'button';
    const hex = `#${(swatch & 0xffffff).toString(16).padStart(6, '0').toUpperCase()}`;
    button.style.background = cssColor(swatch);
    button.setAttribute('aria-label', labels.swatch(hex));
    button.addEventListener('click', () => {
      hsv = rgbToHsv(swatch);
      redraw();
      emit();
    });
    swatchRow.appendChild(button);
  }

  advancedToggle.textContent = labels.advanced;
  advancedToggle.addEventListener('click', () => {
    advanced.hidden = !advanced.hidden;
    if (!advanced.hidden) redraw();
  });

  preview.style.background = cssColor(initial);
  root.append(preview, swatchRow, advancedToggle, advanced);

  return {
    root,
    value: () => hsvToArgb(hsv),
    setValue(argb: number) {
      hsv = rgbToHsv(argb);
      preview.style.background = cssColor(argb);
      redraw();
    },
    onChange(listener) {
      listeners.push(listener);
    },
  };
}

interface Hsv {
  h: number;
  s: number;
  v: number;
}

function rgbToHsv(argb: number): Hsv {
  const r = ((argb >>> 16) & 0xff) / 255;
  const g = ((argb >>> 8) & 0xff) / 255;
  const b = (argb & 0xff) / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const delta = max - min;
  let h = 0;
  if (delta > 0) {
    if (max === r) h = 60 * (((g - b) / delta) % 6);
    else if (max === g) h = 60 * ((b - r) / delta + 2);
    else h = 60 * ((r - g) / delta + 4);
  }
  if (h < 0) h += 360;
  return { h, s: max === 0 ? 0 : delta / max, v: max };
}

function hsvToArgb({ h, s, v }: Hsv): number {
  const c = v * s;
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  const m = v - c;
  let rgb: [number, number, number];
  if (h < 60) rgb = [c, x, 0];
  else if (h < 120) rgb = [x, c, 0];
  else if (h < 180) rgb = [0, c, x];
  else if (h < 240) rgb = [0, x, c];
  else if (h < 300) rgb = [x, 0, c];
  else rgb = [c, 0, x];
  const [r, g, b] = rgb.map((value) => Math.round((value + m) * 255));
  return argbFromHex(
    `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b
      .toString(16)
      .padStart(2, '0')}`
  );
}
