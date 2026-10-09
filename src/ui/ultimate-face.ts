import { prefs, cssColor, DEFAULT_TIME_FONT_SCALE } from '../core/prefs';
import { mixColor, readableInk } from '../core/clock-themes';
import { fontStack } from '../core/fonts';
import { stableTextWidth, widestDigitWidth, measureSupportingText } from '../format/text-metrics';
import { measureColonShift, CharacterLine, type LineOptions } from './clock-face';

type Box = [number, number, number, number];
type Rows = { date: HTMLElement; lunar: HTMLElement; weather: HTMLElement; detail: HTMLElement };
export interface FaceState {
  hour: number; minute: number; second: number; fraction?: number; month0: number; day: number;
  showSeconds: boolean; use24Hour: boolean; options: LineOptions;
}
const pad = (value: number) => String(value).padStart(2, '0');
const scallop = 'polygon(' + Array.from({ length: 96 }, (_, i) => {
  const a = Math.PI * 2 * i / 96 - Math.PI / 2, r = (1 + .055 * Math.cos(a * 12)) / 1.055 * 50;
  return `${50 + Math.cos(a) * r}% ${50 + Math.sin(a) * r}%`;
}).join(',') + ')';

/**
 * Independent compositions ported from UltimateClockStyles.java.
 * Geometry stays in viewport pixels: circles keep a circular aspect ratio and
 * portrait uses the Android composition, rather than squeezing a landscape card.
 * Artwork is SVG/CSS; live, accessible digits keep the shared CharacterLine motion.
 */
export class UltimateFace {
  readonly element = document.createElement('div');
  private parts = new Map<string, HTMLElement>();
  private lines = new Map<string, CharacterLine>();
  private used = new Set<string>();
  private w = 0; private h = 0; private u = 0;
  private landscape = true;
  private secondaryScale = 1;
  private secondaryLimit = 1;
  private fittingSecondary = false;
  private state!: FaceState;
  private family = '';
  private glassOffsetX = 0; private glassOffsetY = 0;
  private background = ''; private panel = ''; private accent = ''; private alt = ''; private badge = '';
  private ink = ''; private onPanel = ''; private onAccent = '';
  constructor(private host: HTMLElement, private rows: Rows, private settings: typeof prefs = prefs) {
    this.element.className = 'ultimate-face';
    host.replaceChildren(this.element);
    this.element.append(rows.date, rows.lunar, rows.weather, rows.detail);
  }
  destroy(): void {
    for (const line of this.lines.values()) line.reset();
    const parent = this.host.parentElement!;
    parent.replaceChildren(this.rows.date, this.rows.lunar, this.host, this.rows.weather, this.rows.detail);
    for (const row of Object.values(this.rows)) row.removeAttribute('style');
    this.host.replaceChildren();
    this.host.removeAttribute('aria-label');
  }
  render(id: string, width: number, height: number, state: FaceState): void {
    if (!width || !height) return;
    if (!this.fittingSecondary) { this.secondaryScale = 1; this.secondaryLimit = 1; }
    this.w = width; this.h = height; this.u = Math.min(width, height);
    this.landscape = width >= height; this.state = state; this.used.clear();
    this.element.dataset.face = id;
    this.family = fontStack(this.settings.getFontFamily());
    const palette = this.settings.getThemePalette();
    const glassRoot = this.host.closest<HTMLElement>('.has-glass');
    const glassInk = glassRoot?.style.getPropertyValue('--glass-ink');
    if(glassRoot){const rootRect=glassRoot.getBoundingClientRect(),hostRect=this.host.getBoundingClientRect();this.glassOffsetX=hostRect.x-rootRect.x;this.glassOffsetY=hostRect.y-rootRect.y;}
    this.background = palette.background; this.panel = palette.panel; this.accent = palette.accent;
    this.alt = mixColor(this.panel, this.accent, .12); this.badge = mixColor(this.panel, this.accent, .65);
    const ink = (surface: string) => this.settings.isThemeAutoInk() ? readableInk(surface) : cssColor(this.settings.getTimeColor());
    this.ink = glassInk || ink(this.background); this.onPanel = glassInk || ink(this.panel); this.onAccent = glassInk || ink(this.accent);
    this.element.style.setProperty('--face-ink', this.ink);
    this.element.style.setProperty('--face-accent', this.accent);
    this.element.style.setProperty('--face-panel', this.panel);
    this.element.style.setProperty('--face-unit', `${this.u}px`);
    this.host.classList.remove('is-stacked', 'has-small-seconds', 'has-period');
    this.host.setAttribute('aria-label', `${pad(this.hour())}:${pad(state.minute)}${state.showSeconds ? ':' + pad(state.second) : ''}`);
    if (id === 'ultimate.dual_blocks') this.dual();
    else if (id === 'ultimate.orbit') this.orbit();
    else if (id === 'ultimate.bubbles') this.bubbles();
    else if (id === 'ultimate.blend') this.blend();
    else if (id === 'ultimate.ribbon') this.ribbon();
    else if (id === 'typographic.poster') this.typographic();
    else if (id === 'digital.grid') this.digital();
    else if (id === 'orbit.neon') this.neon();
    else this.instrument(id);
    if (!state.use24Hour) {
      const marker = state.hour < 12 ? 'AM' : 'PM';
      this.label('period', marker, [this.w * .03, this.h * .965, this.w * .2, this.h * .025], this.ink, 'left');
    }
    for (const [key, part] of this.parts) part.hidden = !this.used.has(key);
    if (!this.fittingSecondary && this.secondaryLimit < 1) {
      this.secondaryScale = Math.max(0.01,this.secondaryLimit); this.fittingSecondary = true;
      try { this.render(id,width,height,state); } finally { this.fittingSecondary = false; }
    }
  }
  private second(): number { return this.state.second + (this.settings.getUltimateOptions().secondMotion === 'smooth' ? this.state.fraction ?? 0 : 0); }
  private handVisible(): boolean { return this.state.showSeconds && this.settings.getUltimateOptions().secondMotion !== 'off'; }
  private hour(): number { return this.state.use24Hour ? this.state.hour : this.state.hour % 12 || 12; }
  private time(): string { return `${pad(this.hour())}:${pad(this.state.minute)}`; }
  private get(key: string, className: string): HTMLElement {
    this.used.add(key);
    let part = this.parts.get(key);
    if (!part) { part = document.createElement('div'); part.className = className; part.dataset.part = key; this.parts.set(key, part); this.element.append(part); }
    part.hidden = false;
    return part;
  }
  private box(element: HTMLElement, [x, y, w, h]: Box): void {
    Object.assign(element.style, { margin: '0', left: x + 'px', top: y + 'px', width: Math.max(0, w) + 'px', height: Math.max(0, h) + 'px' });
  }
  private shape(key: string, box: Box, color: string, radius = 0, flower = false, rotation = 0): void {
    const part = this.get(key, 'face-shape');
    this.box(part, box);
    const [x,y,w,h]=box;
    part.style.setProperty('--glass-left', -(x+this.glassOffsetX)+'px');part.style.setProperty('--glass-top', -(y+this.glassOffsetY)+'px');
    part.style.setProperty('--glass-origin', (x+this.glassOffsetX+w/2)+'px '+(y+this.glassOffsetY+h/2)+'px');part.style.setProperty('--glass-counter-rotation', -rotation+'deg');
    Object.assign(part.style, { background: color, borderRadius: radius + 'px',
      clipPath: flower ? scallop : '', transform: rotation ? `rotate(${rotation}deg)` : '' });
  }
  private circle(key: string, x: number, y: number, r: number, color: string, flower = false): void {
    const size = r * (flower ? 1.055 : 1);
    this.shape(key, [x - size, y - size, size * 2, size * 2], color, size, flower);
  }
  private weight(base = 700): number {
    if (this.settings.hasFontWeight()) return this.settings.getFontWeight();
    if (this.settings.isBoldText()) return 700;
    return this.settings.getFontFamily() === 'system' ? base : 400;
  }
  private number(key: string, text: string, box: Box, size: number, color: string, align = 'center', weight = 700): number {
    const part = this.get(key, 'face-number');
    weight = this.weight(weight);
    const font = { family: this.family, weight };
    const textWidth = stableTextWidth(text, font);
    const actual = Math.max(1, Math.min(size * (this.settings.getTimeFontScale() / DEFAULT_TIME_FONT_SCALE), box[3] * .84, box[2] / Math.max(.1, textWidth)));
    this.box(part, box);
    Object.assign(part.style, { fontFamily: this.family, fontSize: actual + 'px', fontWeight: String(weight), color, justifyContent: align === 'left' ? 'flex-start' : align === 'right' ? 'flex-end' : 'center' });
    part.style.setProperty('--colon-shift', measureColonShift(this.family, weight) + 'em');
    part.style.setProperty('--digit-w', widestDigitWidth(font) * actual + 'px');
    let line = this.lines.get(key);
    if (!line) { line = new CharacterLine(part); this.lines.set(key, line); }
    line.setText(text, this.state.options);
    return actual;
  }
  private label(key: string, text: string, box: Box, color: string, align = 'left'): void {
    const part = this.get(key, 'face-label');
    this.box(part, box); part.textContent = text;
    const requested = this.settings.getSupportingFontSize();
    if (key !== 'period') this.secondaryLimit = Math.min(this.secondaryLimit, box[3] * .8 / requested);
    const size = key === 'period' ? Math.min(box[3] * .8,this.u*.027) : requested * this.secondaryScale;
    Object.assign(part.style, { color, fontWeight: String(this.weight()), fontSize: size + 'px', textAlign: align });
  }
  private date(x: number, y: number, width: number, color: string, align = 'left', _base = .032, maxHeight = this.h * .10): void {
    const desired = this.settings.getDateFontSize();
    const rows = [this.rows.date, this.rows.lunar].filter(row => !row.hidden);
    let top = y;
    for (const row of rows) {
      const measured = measureSupportingText(row.dataset.text ?? row.textContent ?? '', { family: this.family, weight: this.weight() }, .025);
      this.secondaryLimit = Math.min(this.secondaryLimit, width / Math.max(.1,measured) / desired, maxHeight / Math.max(1,rows.length * 1.4) / desired);
      const size = desired * this.secondaryScale;
      this.box(row, [x, top, width, size * 1.35]);
      Object.assign(row.style, { fontSize: size + 'px', color: this.settings.isThemeAutoInk() ? color : cssColor(this.settings.getDateColor()), textAlign: align, fontWeight: String(this.weight()) });
      top += size * 1.4;
    }
  }
  private context(x: number, y: number, width: number, color: string, align = 'left', maxHeight = this.h * .985 - y): void {
    const count = Number(!this.rows.weather.hidden) + Number(!this.rows.detail.hidden);
    const requested = this.settings.getSupportingFontSize();
    if (count) this.secondaryLimit = Math.min(this.secondaryLimit, Math.max(1,maxHeight) / Math.max(1.4,count * 1.6) / requested);
    const size = requested * this.secondaryScale;
    for (const [i, row] of [this.rows.weather, this.rows.detail].filter(row => !row.hidden).entries()) {
      this.box(row, [x, y + i * size * 1.6, width, size * 1.4]);
      Object.assign(row.style, { fontSize: size + 'px', color: this.settings.isThemeAutoInk() ? color : cssColor(this.settings.getDateColor()), textAlign: align, fontWeight: String(this.weight()) });
      row.style.setProperty('--weather-icon-color', this.settings.isWeatherIconDynamicColor() ? this.accent : color);
    }
  }
  private seconds(x: number, y: number, radius: number, color = this.badge, flower = true, size = radius * .92): void {
    if (!this.state.showSeconds) return;
    this.circle('seconds-badge', x, y, radius, color, flower);
    this.number('seconds', pad(this.state.second), [x - radius * .78, y - radius * .85, radius * 1.56, radius * 1.7],
      size / (this.settings.getTimeFontScale() / DEFAULT_TIME_FONT_SCALE), this.host.closest<HTMLElement>('.has-glass')?.style.getPropertyValue('--glass-ink') || (this.settings.isThemeAutoInk() ? readableInk(color) : cssColor(this.settings.getTimeColor())));
  }
  private dual(): void {
    const { w, h, landscape: l } = this, mx = w * (l ? .029 : .055), my = h * .037, gap = w * (l ? .022 : .035);
    const a: Box = [mx, my, l ? w / 2 - gap / 2 - mx : w - 2 * mx, l ? h - 2 * my : h / 2 - gap / 2 - my];
    const b: Box = l ? [w / 2 + gap / 2, my, a[2], a[3]] : [mx, h / 2 + gap / 2, a[2], a[3]];
    const r = Math.min(a[2], a[3]) * .075, size = Math.min(a[3] * .4, a[2] * .59);
    this.shape('hours-panel', a, this.panel, r); this.shape('minutes-panel', b, this.accent, r);
    this.number('hours', pad(this.hour()), [a[0] + a[2] * .12, a[1] + a[3] * .25, a[2] * .76, a[3] * .50], size, this.onPanel);
    this.number('minutes', pad(this.state.minute), [b[0] + b[2] * .12, b[1] + b[3] * .25, b[2] * .76, b[3] * .50], size, this.onAccent);
    this.date(a[0] + a[2] * .05, a[1] + a[3] * .045, a[2] * .88, this.onPanel, 'left', .034, a[3] * .18);
    this.context(b[0] + b[2] * .08, b[1] + b[3] * .045, b[2] * .87, this.onAccent, 'right', b[3] * .18);
    this.label('hour-label', this.word('小时', 'HOUR'), [a[0] + a[2] * .05, a[1] + a[3] * .90, a[2] * .4, a[3] * .06], this.onPanel);
    this.label('minute-label', this.word('分钟', 'MINUTE'), [b[0] + b[2] * .5, b[1] + b[3] * .90, b[2] * .45, b[3] * .06], this.onAccent, 'right');
    const rSec = Math.min(this.u * .038 * this.settings.getSupportingScale(), b[3] * .065);
    this.seconds(b[0] + b[2] * .10, b[1] + b[3] - Math.max(b[3] * .079, rSec * 1.25), rSec);
  }
  private orbit(): void {
    const { w, h } = this, cx = w / 2, cy = h * .522, r = Math.min(h * .382, w * .32);
    const color = mixColor(this.background, this.ink, .2);
    const angle = this.second() * Math.PI / 30 - Math.PI / 2;
    this.svg('orbit-rings', [cx - r - 8, cy - r - 8, r * 2 + 16, r * 2 + 16],
      [1, .755, .515].map((scale, i) => `<circle cx="100" cy="100" r="${94 * scale}" fill="none" stroke="${color}" stroke-width="${1.35 - i * .3}"/>`).join('') +
      (this.handVisible() ? `<circle cx="${100 + Math.cos(angle) * 94}" cy="${100 + Math.sin(angle) * 94}" r="2.7" fill="${this.accent}"/>` : ''));
    this.number('time', this.time(), [cx - r * .95, cy - r * .3, r * 1.9, r * .6], h * .278, this.ink);
    this.date(w * .51, h * .04, w * .461, this.ink, 'right');
    this.context(w * .029, h * .04, w * .43, this.ink);
    this.seconds(cx, cy + r, Math.min(h * .04, w * .045), this.accent);
  }
  private bubbles(): void {
    const { w, h, landscape: l } = this;
    let a: Box, mx: number, my: number, mr: number, sx: number, sy: number, sr: number;
    if (l) {
      a = [w * .029, h * .218, w * .345, h * .611];
      mx = w * .585; my = h * .516; mr = Math.min(h * .315, w * .188);
      sx = w * .884; sy = h * .522; sr = Math.min(h * .143, w * .092);
    } else {
      const gap = Math.max(14, w * .05), span = h * .78, shapeSpace = Math.max(1, span - gap);
      const ah = Math.min(w * .5, shapeSpace * .42), diameter = Math.min(w * .74, Math.max(1, shapeSpace - ah));
      const top = h * .13 + Math.max(0, span - ah - gap - diameter) * .25;
      a = [w * .09, top, w * .82, ah]; mx = w * .42; mr = diameter / 2; my = top + ah + gap + mr;
      sr = Math.min(w * .09, mr * .27); sx = Math.min(w * .97 - sr, mx + mr + sr + gap * .4); sy = my + mr * .32;
    }
    this.shape('hours-panel', a, this.panel, Math.min(a[2], a[3]) * .11);
    this.circle('minutes-flower', mx, my, mr, this.accent, true);
    this.number('hours', pad(this.hour()), [a[0] + a[2] * .14, a[1] + a[3] * .2, a[2] * .72, a[3] * .6], l ? h * .235 : Math.min(a[3] * .44, w * .24), this.onPanel);
    this.number('minutes', pad(this.state.minute), [mx - mr * .65, my - mr * .42, mr * 1.3, mr * .84], l ? h * .235 : Math.min(mr * .72, w * .24), this.onAccent);
    this.label('hour-label', this.word('小时', 'HOUR'), [a[0] + a[2] * .06, a[1] + a[3] * .86, a[2] * .5, a[3] * .09], this.onPanel);
    this.label('minute-label', this.word('分钟', 'MINUTE'), [mx - mr * .6, my + mr * .67, mr * 1.2, mr * .18], this.onAccent, 'center');
    this.seconds(sx, sy, sr, this.alt, false, l ? h * .075 * this.settings.getSupportingScale() : sr * .76);
    this.date(w * (l ? .029 : .06), h * .035, w * (l ? .55 : .88), this.ink);
    this.context(w * (l ? .62 : .06), h * (l ? .035 : .925), w * (l ? .351 : .88), this.ink, l ? 'right' : 'left');
  }
  private blend(): void {
    const { w, h, landscape: l } = this;
    const a: Box = l ? [w * .03, h * .046, w * .497, h * .92] : [w * .05, h * .035, w * .90, h * .515];
    const b: Box = l ? [w * .55, h * .046, w * .423, h * .92] : [w * .05, h * .575, w * .90, h * .39];
    const corner = Math.min(a[2], a[3]) * .06;
    this.shape('analog-panel', a, this.panel, corner); this.shape('digital-panel', b, this.accent, corner);
    this.dial('blend-dial', a[0] + a[2] / 2, a[1] + a[3] / 2, Math.min(a[2] * .46, a[3] * .44), 'blend');
    this.number('time', this.time(), [b[0] + b[2] * .06, b[1] + b[3] * .29, b[2] * .88, b[3] * .36], Math.min(b[3] * .30, b[2] * .29), this.onAccent);
    this.date(b[0] + b[2] * .045, b[1] + b[3] * .77, b[2] * .70, this.onAccent);
    this.context(b[0] + b[2] * .06, b[1] + b[3] * .035, b[2] * .895, this.onAccent, 'right', b[3] * .22);
    const r = Math.min(this.u * .038, b[3] * .075);
    this.seconds(b[0] + b[2] * .91, b[1] + b[3] * .90 - r, r);
  }
  private ribbon(): void {
    const { w, h, landscape: l } = this;
    const cy = h * .53, oh = Math.min(h * .36, w * .42), rh = Math.min(oh * .64, w * .27);
    const a: Box = l ? [w * .05, h * .205, w * .91, h * .645] : [w * .05, cy - oh / 2, w * .91, oh];
    const b: Box = l ? [w * .078, h * .265, w * .853, h * .535] : [w * .078, cy - rh / 2, w * .853, rh];
    this.shape('ribbon-back', a, this.panel, a[3] * .085, false, -2);
    this.shape('ribbon-front', b, this.accent, b[3] / 2);
    const size = this.number('time', this.time(), [b[0] + b[2] * .055, b[1] + b[3] * .1, b[2] * (this.state.showSeconds ? .58 : .86), b[3] * .8], Math.min(b[3] * .56, w * .30), this.onAccent, 'left');
    const r = Math.max(size * .38, this.u * .042);
    this.seconds(b[0] + b[2] * .945 - r, b[1] + b[3] / 2, Math.min(r, b[3] * .38), this.badge, true, size * .5);
    this.date(w * .029, h * .035, w * .52, this.ink);
    this.context(w * .59, h * .035, w * .381, this.ink, 'right');
  }
  private word(cn: string, en: string): string { return this.settings.isClockUseEnglish() ? en : cn; }
  private svg(key: string, box: Box, markup: string): void {
    const part = this.get(key, 'face-art'); this.box(part, box);
    const html = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" aria-hidden="true">${markup}</svg>`;
    if (part.innerHTML !== html) part.innerHTML = html;
  }
  private dial(key: string, cx: number, cy: number, r: number, kind: string): void {
    const blend = kind === 'blend', atelier = kind === 'glass.atelier', paper = kind === 'paper.station';
    const glass = Boolean(this.host.closest('.has-glass'));
    const color = blend ? (glass ? this.onPanel : readableInk(this.alt)) : this.ink;
    const face = blend ? this.alt : this.panel;
    let art = blend ? '' : atelier
      ? '<defs><linearGradient id="bezel"><stop stop-color="#fdfeff"/><stop offset="1" stop-color="#9eabb6"/></linearGradient><radialGradient id="glass-face" cx="30%" cy="25%"><stop stop-color="#fff"/><stop offset="1" stop-color="#dde5ea"/></radialGradient></defs><circle cx="100" cy="102" r="98" fill="#162531" opacity=".16"/><circle cx="100" cy="100" r="97" fill="url(#bezel)"/>'
      : `<circle cx="100" cy="102" r="97" fill="#000" opacity=".15"/><circle cx="100" cy="100" r="96" fill="${face}" stroke="${this.accent}" stroke-opacity=".5" stroke-width="1.4"/>`;
    art += `<circle cx="100" cy="100" r="90" fill="${atelier ? 'url(#glass-face)' : blend && glass ? 'transparent' : face}"/>`;
    if (!blend) art += `<circle cx="100" cy="100" r="83" fill="none" stroke="${color}" opacity=".25" stroke-width=".5"/>`;
    for (let i = 0; i < 60; i++) {
      const major = i % 5 === 0, outer = blend ? 84 : 79, inner = major ? 70 : 75;
      art += `<line x1="100" y1="${100 - outer}" x2="100" y2="${100 - inner}" stroke="${color}" stroke-width="${major ? blend ? 2.2 : 1.4 : .5}" stroke-linecap="round" transform="rotate(${i * 6} 100 100)"/>`;
    }
    if (!blend) for (const n of [12, 3, 6, 9]) {
      const a = n * Math.PI / 6 - Math.PI / 2;
      art += `<text x="${100 + Math.cos(a) * 60}" y="${100 + Math.sin(a) * 60}" dominant-baseline="central" text-anchor="middle" font-family="${paper ? 'serif' : 'sans-serif'}" font-size="11" font-weight="600" fill="${color}">${n}</text>`;
    }
    const hands = [
      [(this.state.hour % 12 + this.state.minute / 60) * 30, 45, blend ? 5 : paper ? 2.8 : 4, color],
      [(this.state.minute + this.second() / 60) * 6, 64, blend ? 3.5 : 2.3, color],
      ...(this.handVisible() ? [[this.second() * 6, 74, .9, blend ? color : this.accent]] : []),
    ];
    for (const [angle, length, stroke, ink] of hands) art += `<line x1="100" y1="108" x2="100" y2="${100 - Number(length)}" stroke="${ink}" stroke-width="${stroke}" stroke-linecap="${paper ? 'square' : 'round'}" transform="rotate(${angle} 100 100)"/>`;
    art += `<circle cx="100" cy="100" r="4.5" fill="${blend ? color : this.accent}"/><circle cx="100" cy="100" r="1.8" fill="${face}"/>`;
    this.svg(key, [cx - r * 1.11, cy - r * 1.11, r * 2.22, r * 2.22], art);
  }
  private instrument(id: string): void {
    const { w, h, u } = this, l = w >= h * 1.12, glass = id === 'glass.atelier', paper = id === 'paper.station';
    const cx = l ? w * (glass ? .69 : paper ? .31 : .30) : w / 2;
    const cy = h * (l ? glass ? .49 : paper ? .49 : .48 : glass ? .35 : paper ? .32 : .31);
    const r = l ? Math.min(h * (paper ? .285 : .33), w * .225) : Math.min(w * (glass ? .36 : .34), h * .225);
    this.dial('instrument-dial', cx, cy, r, id);
    const x = w * (l ? glass ? .075 : paper ? .59 : .62 : glass ? .10 : paper ? .13 : .10);
    const width = w * (l ? glass ? .33 : paper ? .32 : .305 : paper ? .74 : .80);
    const titleY = h * (l ? .14 : glass ? .06 : .59);
    this.label('title', glass ? 'ATELIER / 01' : paper ? `PAPER / ${pad(this.state.month0 + 1)}.${pad(this.state.day)}` : 'INSTRUMENT / 24',
      [x, titleY, width, u * .034], this.accent);
    const ty = h * (l ? glass ? .38 : paper ? .43 : .29 : glass ? .70 : paper ? .74 : .65);
    this.number('time', this.time(), [x, ty, width, u * .16], u * (l ? .14 : .12), this.ink, 'left');
    const dateY = h * (l ? glass || paper ? .25 : .46 : glass ? .62 : paper ? .655 : .765);
    const dateBottom = paper ? h * (l ? .35 : .72) : glass ? ty : dateY + h * .10;
    this.date(x, dateY, width, this.ink, 'left', .030, dateBottom - dateY - u * .012);
    this.context(x, h * (l ? .59 : glass ? .83 : paper ? .87 : .855), width, this.ink);
    if (paper) this.shape('paper-rule', [x, h * (l ? .35 : .72), width, 1], this.accent);
    if (!paper && !glass) {
      this.shape('instrument-rule', l ? [w * .56, h * .10, 1, h * .76] : [w * .09, h * .56, w * .82, 1], mixColor(this.background, this.ink, .2));
      if (l && this.handVisible()) {
        const sr = u * .06, sx = w * .86, sy = h * .79;
        this.svg('seconds-dial', [sx - sr, sy - sr, sr * 2, sr * 2], `<circle cx="100" cy="100" r="95" fill="none" stroke="${this.accent}" stroke-width="2"/>` +
          Array.from({ length: 12 }, (_, i) => `<line x1="100" y1="15" x2="100" y2="25" stroke="${this.accent}" stroke-width="2" transform="rotate(${i * 30} 100 100)"/>`).join('') +
          `<line x1="100" y1="100" x2="100" y2="37" stroke="${this.accent}" stroke-width="2" transform="rotate(${this.second() * 6} 100 100)"/>`);
        this.number('seconds', pad(this.state.second), [sx - sr * .45, sy - sr * .25, sr * .9, sr * .5], sr * .5, this.ink);
      }
    }
  }
  private neon(): void {
    const { w, h, u } = this, l = w >= h * 1.12, cx = l ? w * .35 : w / 2, cy = h * (l ? .49 : .36);
    const r = l ? Math.min(h * .335, w * .24) : Math.min(w * .34, h * .235);
    const values = [(this.state.hour % 12 + this.state.minute / 60) / 12, (this.state.minute + this.second() / 60) / 60, this.second() / 60];
    const colors = [this.accent, '#62aeff', '#ff725e'];
    let art = '';
    [90, 64.8, 42.3].forEach((radius, i) => {
      const c = 2 * Math.PI * radius, stroke = [6.5, 5, 4][i];
      art += `<circle cx="100" cy="100" r="${radius}" fill="none" stroke="${colors[i]}" stroke-opacity=".14" stroke-width="${stroke}"/>`;
      if (i < 2 || this.handVisible()) art += `<circle cx="100" cy="100" r="${radius}" fill="none" stroke="${colors[i]}" stroke-width="${stroke}" stroke-linecap="round" stroke-dasharray="${c * values[i]} ${c}" transform="rotate(-90 100 100)"/>`;
    });
    for (let i = 0; i < 12; i++) {
      const a = i * Math.PI / 6;
      art += `<circle cx="${100 + Math.cos(a) * 77}" cy="${100 + Math.sin(a) * 77}" r="${i % 3 ? 1 : 1.5}" fill="${this.ink}" opacity=".6"/>`;
    }
    this.svg('neon-rings', [cx - r / .9, cy - r / .9, r * 2 / .9, r * 2 / .9], art);
    this.number('time', this.time(), [cx - r * .38, cy - r * .14, r * .76, r * .28], u * .105, this.ink, 'center', 400);
    if (this.state.showSeconds) this.number('seconds', pad(this.state.second), [cx - r * .15, cy + r * .12, r * .30, r * .15], u * .023, colors[2]);
    const x = w * (l ? .65 : .10), width = w * (l ? .27 : .8);
    this.label('title', 'ORBIT / LIVE', [x, h * (l ? .14 : .06), width, u * .03], this.accent);
    this.date(x, h * (l ? .26 : .63), width, this.ink);
    this.context(x, h * (l ? .41 : .755), width, this.ink);
    const v = [pad(this.hour()), pad(this.state.minute), this.state.showSeconds ? pad(this.state.second) : '--'];
    v.forEach((text, i) => this.label('value-' + i, ['H', 'M', 'S'][i] + '  ' + text,
      l ? [x, h * (.61 + i * .095), width, u * .035] : [w * (.1 + i * .3), h * .89, w * .2, u * .04], colors[i], l ? 'left' : 'center'));
  }
  private typographic(): void {
    const { w, h, u } = this, l = w >= h * 1.12;
    this.shape('type-panel', l ? [0, 0, w * .36, h] : [0, 0, w, h * .48], this.panel);
    this.shape('type-rule', l ? [w * .36, 0, u * .008, h] : [0, h * .48, w, u * .008], this.accent);
    this.number('hours', pad(this.hour()), l ? [w * .04, h * .24, w * .28, h * .46] : [w * .12, h * .14, w * .76, h * .26], u * .34, this.onPanel);
    this.number('minutes', pad(this.state.minute), l ? [w * .45, h * .24, w * .46, h * .46] : [w * .12, h * .51, w * .76, h * .26], u * .34, this.ink);
    this.label('title', 'TYPE / 06', [w * .07, h * .075, w * .24, u * .03], this.onPanel);
    if (this.state.showSeconds) this.number('seconds', pad(this.state.second), [w * .81, h * .10, w * .12, u * .10], u * .065, this.accent, 'right');
    this.label('sec-label', 'SEC', [w * .81, h * .10 + u * .10, w * .12, u * .03], l ? this.ink : this.onPanel, 'right');
    const x = w * (l ? .43 : .10), width = w * (l ? .495 : .8), y = h * (l ? .64 : .755);
    this.shape('progress-track', [x, y, width, u * .004], mixColor(this.background, this.ink, .3));
    if (this.state.showSeconds) this.shape('progress', [x, y, width * this.second() / 60, u * .007], this.accent);
    this.date(x, h * (l ? .73 : .79), width, this.ink);
    this.context(x, h * (l ? .84 : .90), width, this.ink);
  }
  private digital(): void {
    const { w, h, u } = this, l = w >= h * 1.2;
    this.label('title', 'DIGITAL GRID', [w * .07, h * .065, w * .6, u * .045], this.accent);
    const top = h * (l ? .23 : .19), height = h * (l ? .39 : .29);
    const left = w * .075, right = w * (l ? .79 : .925), gap = w * .012, colon = w * .05;
    const dw = (right - left - gap * 2 - colon) / 4;
    const positions = [left, left + dw + gap, left + dw * 2 + gap + colon, left + dw * 3 + gap * 2 + colon];
    for (const i of [0, 2]) this.shape('grid-panel-' + i, [positions[i] - u * .012, top - u * .012, dw * 2 + gap + u * .024, height + u * .024], this.panel, u * .01);
    const digits = pad(this.hour()) + pad(this.state.minute);
    positions.forEach((x, i) => this.segment('segment-' + i, Number(digits[i]), [x, top, dw, height], '#a8f7cf'));
    if (this.state.options.colonVisible) for (const d of [-1, 1]) this.circle('colon-' + d, (positions[1] + dw + positions[2]) / 2, top + height * (.5 + d * .125), u * .01, this.accent);
    const sec: Box = l ? [w * .835, top - u * .012, w * .095, height + u * .024] : [w * .34, h * .54, w * .32, h * .095];
    this.shape('grid-seconds-panel', sec, this.panel, u * .01);
    if (this.state.showSeconds) {
      const text = pad(this.state.second);
      for (let i = 0; i < 2; i++) this.segment('second-segment-' + i, Number(text[i]), [sec[0] + sec[2] * (.06 + i * .47), sec[1] + sec[3] * .13, sec[2] * .41, sec[3] * .55], this.accent);
    }
    this.label('sec-label', 'SEC', [sec[0], sec[1] + sec[3] * .76, sec[2], sec[3] * .16], this.ink, 'center');
    this.date(w * .075, h * (l ? .71 : .685), w * .85, this.ink);
    this.context(w * .075, h * (l ? .84 : .815), w * .85, this.ink);
  }
  private segment(key: string, digit: number, box: Box, color: string): void {
    const patterns = ['1111110', '0110000', '1101101', '1111001', '0110011', '1011011', '1011111', '1110000', '1111111', '1111011'];
    const coords = [[48, 26, 104, 10], [151, 34, 10, 59], [151, 107, 10, 59], [48, 168, 104, 10], [39, 107, 10, 59], [39, 34, 10, 59], [48, 97, 104, 10]];
    const scale = Math.min(1.25, (this.settings.getTimeFontScale() / DEFAULT_TIME_FONT_SCALE));
    const art = `<g transform="translate(100 100) scale(${scale}) translate(-100 -100)">` + coords.map(([x,y,width,height],i) => `<rect x="${x}" y="${y}" width="${width}" height="${height}" rx="3" fill="${color}" opacity="${patterns[digit][i] === '1' ? 1 : .08}"/>`).join('') + '</g>';
    this.svg(key, box, art);
    const part = this.parts.get(key)!;
    part.setAttribute('aria-label', String(digit));
    part.querySelector('svg')?.setAttribute('preserveAspectRatio', 'none');
  }
}
