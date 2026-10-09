import { changedDigitPair, motionDuration } from './clock-motion';
/**
 * Character-level clock rendering.
 *
 * Ported from the drawing half of com.clockmods.ui.ClockView. Canvas drawing
 * becomes one `<span>` per character:
 *
 *   - digits get a fixed width (ClockTextLayout.stableTextWidth) so a proportional
 *     font never makes the clock jitter as digits change;
 *   - both digits of a changed time field animate, each with the configured
 *     transition, by layering the outgoing glyph over the incoming one;
 *   - the colon is nudged so its optical centre matches the digits' centre
 *     (ClockTextLayout.alignedCharacterBaseline) and fades when blinking.
 */
import { hasSupportingTrackingAt, pangu } from '../format/text-spacing';
import {
  TRANSITION_FADE,
  TRANSITION_FLIP,
  TRANSITION_SCALE,
  TRANSITION_SLIDE_DOWN,
  TRANSITION_SLIDE_UP,
  TRANSITION_SLIDE_RIGHT,
  TRANSITION_SCAN,
} from '../core/prefs';

/** Matches TIME_TRANSITION_DURATION_MILLIS in ClockView. */
export const TRANSITION_DURATION_MS = 300;

const TRANSITION_CLASSES: Record<string, string> = {
  [TRANSITION_FADE]: 'fade',
  [TRANSITION_SLIDE_UP]: 'slide-up',
  [TRANSITION_SLIDE_DOWN]: 'slide-down',
  [TRANSITION_SCALE]: 'scale',
  [TRANSITION_FLIP]: 'flip',
  [TRANSITION_SLIDE_RIGHT]: 'slide-right',
  [TRANSITION_SCAN]: 'scan',
};

export interface LineOptions {
  /** Whether digit changes animate at all (ClockPreferences.animateTimeChanges). */
  animate: boolean;
  /** One of the TRANSITION_* ids. */
  transition: string;
  /** false hides the colons this tick (blink mode). */
  colonVisible: boolean;
}

/**
 * One line of clock text whose characters are updated in place.
 *
 * A length change rebuilds the line without animating, mirroring ClockView's
 * fallback when the display shape changes (seconds toggled, AM/PM appearing).
 */
export class CharacterLine {
  readonly element: HTMLElement;
  private text = '';
  private colonVisible = true;
  private readonly cleanups = new Map<HTMLElement, () => void>();

  constructor(element: HTMLElement) {
    this.element = element;
  }

  setText(text: string, options: LineOptions): void {
    if (text === this.text) {
      if (options.colonVisible !== this.colonVisible) {
        this.colonVisible = options.colonVisible;
        this.applyColonVisibility(options.animate);
      }
      return;
    }
    const cells = this.element.querySelectorAll<HTMLElement>('.clock-char');
    const sameShape = text.length === this.text.length && cells.length === text.length;
    if (!sameShape || !options.animate) {
      this.rebuild(text, options.colonVisible);
      this.text = text;
      this.colonVisible = options.colonVisible;
      return;
    }
    const transitionClass = TRANSITION_CLASSES[options.transition] ?? 'fade';
    for (let index = 0; index < text.length; index++) {
      const previous = this.text.charAt(index);
      const next = text.charAt(index);
      if (previous === next && !changedDigitPair(this.text, text, index)) continue;
      this.replaceCharacter(cells[index], previous, next, transitionClass);
    }
    this.text = text;
    if (options.colonVisible !== this.colonVisible) {
      this.colonVisible = options.colonVisible;
      this.applyColonVisibility(true);
    }
  }

  /** Clears state so the next setText rebuilds (used when the font changes). */
  reset(): void {
    this.text = '';
    this.clearAnimations();
    this.element.replaceChildren();
  }

  private rebuild(text: string, colonVisible: boolean): void {
    this.clearAnimations();
    const children: HTMLElement[] = [];
    for (const character of text) {
      const cell = document.createElement('span');
      cell.className = 'clock-char';
      if (character >= '0' && character <= '9') cell.dataset.digit = '';
      if (character === ':') {
        cell.dataset.colon = '';
        cell.classList.toggle('is-hidden', !colonVisible);
      }
      const glyph = document.createElement('span');
      glyph.className = 'glyph';
      glyph.textContent = character;
      cell.appendChild(glyph);
      children.push(cell);
    }
    if (this.element.dataset.materialGroups === 'true') {
      const groups: HTMLElement[] = [];
      let section = document.createElement('span'); section.className = 'material-time-part material-hours'; groups.push(section);
      let index = 0;
      for (const cell of children) {
        if (cell.hasAttribute('data-colon')) {
          cell.classList.add('material-separator'); groups.push(cell);
          section = document.createElement('span'); section.className = 'material-time-part ' + (++index === 1 ? 'material-minutes' : 'material-seconds'); groups.push(section);
        } else section.appendChild(cell);
      }
      this.element.replaceChildren(...groups);
    } else this.element.replaceChildren(...children);
  }

  /**
   * Layers the outgoing glyph over the incoming one for the duration of the
   * transition, which is how ClockView drew both characters during a change.
   */
  private replaceCharacter(
    cell: HTMLElement,
    previous: string,
    next: string,
    transitionClass: string
  ): void {
    if (!cell) return;
    this.cleanups.get(cell)?.();
    // Fresh nodes restart CSS animations without a per-digit forced layout.
    const incoming = document.createElement('span');
    incoming.className = `glyph is-in anim-${transitionClass}`;
    incoming.textContent = next;
    const outgoing = document.createElement('span');
    outgoing.className = `glyph is-out anim-${transitionClass}`;
    outgoing.textContent = previous;
    outgoing.setAttribute('aria-hidden', 'true');
    cell.replaceChildren(incoming, outgoing);
    const cleanup = () => {
      clearTimeout(timer);
      incoming.removeEventListener('animationend', cleanup);
      outgoing.remove();
      incoming.className = 'glyph';
      this.cleanups.delete(cell);
    };
    const timer = setTimeout(cleanup, motionDuration(transitionClass.replace('-', '_')) + 80);
    this.cleanups.set(cell, cleanup);
    incoming.addEventListener('animationend', cleanup);
  }

  private clearAnimations(): void {
    for (const cleanup of [...this.cleanups.values()]) cleanup();
  }

  private applyColonVisibility(animate: boolean): void {
    for (const cell of this.element.querySelectorAll<HTMLElement>('[data-colon]')) {
      cell.classList.toggle('no-fade', !animate);
      cell.classList.toggle('is-hidden', !this.colonVisible);
    }
  }
}

/**
 * Renders supporting text (date, lunar, weather) with the CJK-aware tracking
 * ClockView applied per code point.
 *
 * The Android version added a fixed gap after every character except around a
 * pangu-inserted space. CSS letter-spacing has no such exception, so the runs
 * that must not be widened are wrapped in a span that zeroes it.
 */
export function renderSupportingText(element: HTMLElement, text: string): void {
  const displayText = pangu(text);
  if (element.dataset.text === displayText) return;
  element.dataset.text = displayText;
  if (!displayText) {
    element.replaceChildren();
    return;
  }
  const nodes: Node[] = [];
  let buffer = '';
  let index = 0;
  const flush = (tight: boolean) => {
    if (!buffer) return;
    if (tight) {
      const span = document.createElement('span');
      span.className = 'no-tracking';
      span.textContent = buffer;
      nodes.push(span);
    } else {
      nodes.push(document.createTextNode(buffer));
    }
    buffer = '';
  };
  for (const character of displayText) {
    index += character.length;
    const tight = !hasSupportingTrackingAt(displayText, index);
    // A character whose trailing gap is suppressed goes into its own run.
    if (tight) {
      flush(false);
      buffer = character;
      flush(true);
    } else {
      buffer += character;
    }
  }
  flush(false);
  element.replaceChildren(...nodes);
}

/**
 * Vertical nudge, as a fraction of the font size, that puts a colon's optical
 * centre on the digits' centre. Mirrors ClockTextLayout.alignedCharacterBaseline.
 */
const colonMeasurements = new Map<string, number>();
if (typeof document !== 'undefined') document.fonts?.addEventListener('loadingdone', () => colonMeasurements.clear());
/** Ultimate ClockTimeText's baseline-relative ink-bound geometry. */
export function colonBaselineOffset(digitTop: number, digitBottom: number, colonTop: number, colonBottom: number): number {
  if (![digitTop,digitBottom,colonTop,colonBottom].every(Number.isFinite) || digitBottom <= digitTop || colonBottom <= colonTop) return 0;
  return (digitTop + digitBottom - colonTop - colonBottom) / 2;
}
export function measureColonShift(fontFamily: string, weight: number | string): number {
  const key = fontFamily + '|' + weight;
  const cached = colonMeasurements.get(key);
  if (cached !== undefined) return cached;
  const canvas = document.createElement('canvas');
  const context = canvas.getContext('2d');
  if (!context) return 0;
  const size = 100;
  context.font = `${weight} ${size}px ${fontFamily}`;
  const digit = context.measureText('0');
  const colon = context.measureText(':');
  const shift = colonBaselineOffset(-digit.actualBoundingBoxAscent, digit.actualBoundingBoxDescent,
    -colon.actualBoundingBoxAscent, colon.actualBoundingBoxDescent) / size;
  colonMeasurements.set(key, shift);
  return shift;
}

/** Static time labels (world clocks) share the same colon correction as animated digits. */
export function setAlignedTime(element: HTMLElement,text:string,family:string,weight:number): void {
 if(element.dataset.clockText!==text){element.dataset.clockText=text;element.replaceChildren(...text.split(':').flatMap((part,index)=>{const nodes:Node[]=[];if(index){const colon=document.createElement('span');colon.className='aligned-time-colon';colon.textContent=':';nodes.push(colon);}nodes.push(document.createTextNode(part));return nodes;}));}
 element.style.fontFamily=family;element.style.fontWeight=String(weight);element.style.setProperty('--colon-shift',measureColonShift(family,weight)+'em');
}
