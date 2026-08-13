/**
 * Character-level clock rendering.
 *
 * Ported from the drawing half of com.clockmods.ui.ClockView. Canvas drawing
 * becomes one `<span>` per character:
 *
 *   - digits get a fixed width (ClockTextLayout.stableTextWidth) so a proportional
 *     font never makes the clock jitter as digits change;
 *   - only characters that actually changed animate, each with the configured
 *     transition, by layering the outgoing glyph over the incoming one;
 *   - the colon is nudged so its optical centre matches the digits' centre
 *     (ClockTextLayout.alignedCharacterBaseline) and fades when blinking.
 */
import { hasSupportingTrackingAt } from '../format/text-spacing';
import {
  TRANSITION_FADE,
  TRANSITION_FLIP,
  TRANSITION_SCALE,
  TRANSITION_SLIDE_DOWN,
  TRANSITION_SLIDE_UP,
} from '../core/prefs';

/** Matches TIME_TRANSITION_DURATION_MILLIS in ClockView. */
export const TRANSITION_DURATION_MS = 300;

const TRANSITION_CLASSES: Record<string, string> = {
  [TRANSITION_FADE]: 'fade',
  [TRANSITION_SLIDE_UP]: 'slide-up',
  [TRANSITION_SLIDE_DOWN]: 'slide-down',
  [TRANSITION_SCALE]: 'scale',
  [TRANSITION_FLIP]: 'flip',
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
    const sameShape = text.length === this.text.length && this.element.childElementCount === text.length;
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
      if (previous === next) continue;
      this.replaceCharacter(this.element.children[index] as HTMLElement, previous, next, transitionClass);
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
    this.element.replaceChildren();
  }

  private rebuild(text: string, colonVisible: boolean): void {
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
    this.element.replaceChildren(...children);
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
    // Drop any still-running outgoing glyph so rapid changes cannot pile up.
    for (const stale of cell.querySelectorAll('.glyph.is-out')) stale.remove();

    const incoming = cell.querySelector('.glyph') as HTMLElement | null;
    if (!incoming) return;
    const outgoing = document.createElement('span');
    outgoing.className = `glyph is-out anim-${transitionClass}`;
    outgoing.textContent = previous;
    incoming.textContent = next;
    incoming.classList.remove('is-in');
    incoming.className = `glyph is-in anim-${transitionClass}`;
    cell.appendChild(outgoing);
    // Force a reflow so restarting the same animation on a fast-changing digit
    // actually replays it.
    void cell.offsetWidth;
    const cleanup = () => {
      outgoing.remove();
      incoming.classList.remove('is-in', `anim-${transitionClass}`);
    };
    outgoing.addEventListener('animationend', cleanup, { once: true });
    // animationend never fires when animations are disabled (prefers-reduced-motion
    // or a background tab), so guarantee cleanup.
    setTimeout(cleanup, TRANSITION_DURATION_MS + 80);
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
  if (element.dataset.text === text) return;
  element.dataset.text = text;
  if (!text) {
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
  for (const character of text) {
    index += character.length;
    const tight = !hasSupportingTrackingAt(text, index);
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
export function measureColonShift(fontFamily: string, weight: number | string): number {
  const canvas = document.createElement('canvas');
  const context = canvas.getContext('2d');
  if (!context) return 0;
  const size = 100;
  context.font = `${weight} ${size}px ${fontFamily}`;
  const digit = context.measureText('0');
  const colon = context.measureText(':');
  const digitCenter = (digit.actualBoundingBoxAscent - digit.actualBoundingBoxDescent) / 2;
  const colonCenter = (colon.actualBoundingBoxAscent - colon.actualBoundingBoxDescent) / 2;
  if (!Number.isFinite(digitCenter) || !Number.isFinite(colonCenter)) return 0;
  // Positive shift moves the glyph down, so the sign is inverted relative to the
  // Android baseline arithmetic.
  return (colonCenter - digitCenter) / size;
}
