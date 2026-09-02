/**
 * Vertically-sliding label carousel used by the calendar grid cells and the
 * 宜/忌 footer.
 *
 * Ported from com.clockmods.ui.CalendarLabelCarouselView and
 * CalendarFooterCarouselView: each item is held for 3s, then slides up while the
 * next slides in over 200ms. A label wider than its cell pauses for 1s, moves
 * left once at 40px/s, pauses for another second, then advances vertically.
 *
 * An item may name a `pinnedPrefix` — the 宜/忌 glyph that is the line's identity.
 * It is drawn bold and pinned at the left edge, and only the items after it
 * scroll: the glyph lives outside the strip that clips them, so it never slides
 * away with them. A line that fits keeps the glyph bold but stays centred as a
 * whole, exactly as CalendarFooterCarouselView draws it.
 */

import { pangu } from '../format/text-spacing';

const HOLD_MS = 3000;
const TRANSITION_MS = 200;
const SCROLL_PAUSE_MS = 1000;
const SCROLL_PX_PER_SECOND = 40;
/** Labels of at most this many characters never scroll (MAX_STATIC_CHARS). */
const MAX_STATIC_CHARS = 3;

export interface LabelItem {
  text: string;
  /** CSS colour; the footer colours 宜 green and 忌 red. */
  color?: string;
  /**
   * When set and `text` starts with it, this head is split off as the bold glyph
   * that stays pinned at the left edge while the rest scrolls past it.
   */
  pinnedPrefix?: string;
}

export class LabelCarousel {
  private readonly viewport: HTMLElement;
  private readonly track: HTMLElement;
  private items: LabelItem[] = [];
  private index = 0;
  private active = false;
  private phaseHandle: number | null = null;
  private scrollFrame: number | null = null;
  private layoutFrame: number | null = null;

  constructor(viewport: HTMLElement) {
    this.viewport = viewport;
    this.viewport.classList.add('label-carousel');
    this.viewport.classList.add('is-measuring');
    this.track = document.createElement('div');
    this.track.className = 'label-carousel-track';
    this.viewport.replaceChildren(this.track);
  }

  get isActive(): boolean {
    return this.active;
  }

  setItems(items: LabelItem[]): void {
    this.clearCycle();
    this.items = items;
    this.index = 0;
    this.snapTrackToStart();
    // The repeated first item lets the final item keep sliding in the same
    // direction. Once it arrives, the track snaps invisibly back to item zero.
    const lines = items.map((item) => this.createLine(item));
    if (items.length > 1) {
      const repeatedFirst = this.createLine(items[0]);
      repeatedFirst.setAttribute('aria-hidden', 'true');
      lines.push(repeatedFirst);
    }
    this.track.replaceChildren(...lines);
    this.viewport.classList.add('is-measuring');
    if (this.active) this.scheduleAfterLayout();
  }

  setActive(active: boolean): void {
    if (this.active === active) return;
    this.active = active;
    if (active) {
      this.scheduleAfterLayout();
    } else {
      this.clearCycle();
      this.resetItemTransforms();
    }
  }

  /** Called by the shared ticker; moves to the next item. */
  advance(): void {
    if (this.items.length < 2) return;
    this.clearCycle();
    this.index += 1;
    this.track.style.transform = `translateY(${-this.index * 100}%)`;
    this.phaseHandle = window.setTimeout(() => {
      this.phaseHandle = null;
      if (this.index >= this.items.length) {
        // The visible clone and the real first item are identical. Flush the
        // transition-free reset before restoring the transition so the browser
        // cannot interpolate it as a downward slide.
        this.index = 0;
        this.snapTrackToStart();
      }
      this.resetItemTransforms();
      if (this.active) this.scheduleCurrentItem();
    }, TRANSITION_MS);
  }

  destroy(): void {
    this.setActive(false);
    this.clearCycle();
  }

  private createLine(item: LabelItem): HTMLElement {
    const line = document.createElement('div');
    line.className = 'label-carousel-item';
    if (item.color) line.style.color = item.color;
    const displayText = pangu(item.text);
    const hint = item.pinnedPrefix ?? '';
    const pinned = hint.length > 0 && displayText.startsWith(hint)
      && displayText.length > hint.length;
    // Split, never rewritten: the two halves still read as the original line, so
    // the accessible text and the tests see 宜/忌 followed by its items.
    const body = pinned ? displayText.slice(hint.length) : displayText;
    if (pinned) {
      const pin = document.createElement('strong');
      pin.className = 'label-carousel-pin';
      // The hint's own trailing space collapses away inside the flex item; the
      // gap to the items is CSS, so it survives a hint that has no space at all.
      pin.textContent = hint;
      line.appendChild(pin);
    }
    // The strip is what clips the scrolling text; the pinned glyph sits outside
    // it, so a body scrolled to its tail can never slide over the glyph.
    const strip = document.createElement('span');
    strip.className = 'label-carousel-strip';
    const text = document.createElement('span');
    text.className = 'label-carousel-text';
    text.textContent = body;
    // Only labels longer than three characters need overflow measurement.
    if (body.trim().length > MAX_STATIC_CHARS) text.classList.add('can-scroll');
    strip.appendChild(text);
    line.appendChild(strip);
    return line;
  }

  private scheduleCurrentItem(): void {
    this.clearCycle();
    const line = this.track.children[this.index] as HTMLElement | undefined;
    if (!line) return;
    const inner = line.querySelector<HTMLElement>('.label-carousel-text');
    if (!inner) return;

    const overflow = this.measureLine(line);
    if (overflow === 0) {
      if (this.items.length > 1) {
        this.phaseHandle = window.setTimeout(() => this.advance(), HOLD_MS);
      }
      return;
    }

    const scrollMs = Math.ceil((overflow / SCROLL_PX_PER_SECOND) * 1000);
    this.phaseHandle = window.setTimeout(() => {
      this.phaseHandle = null;
      inner.style.transition = `transform ${scrollMs}ms linear`;
      this.scrollFrame = requestAnimationFrame(() => {
        this.scrollFrame = null;
        inner.style.transform = `translateX(${-overflow}px)`;
      });
      this.phaseHandle = window.setTimeout(() => {
        this.phaseHandle = null;
        if (this.items.length > 1) {
          this.advance();
        } else {
          this.resetItemTransforms();
          if (this.active) this.scheduleCurrentItem();
        }
      }, scrollMs + SCROLL_PAUSE_MS);
    }, SCROLL_PAUSE_MS);
  }

  private scheduleAfterLayout(): void {
    if (this.layoutFrame !== null) cancelAnimationFrame(this.layoutFrame);
    this.layoutFrame = requestAnimationFrame(() => {
      this.layoutFrame = null;
      if (!this.active) return;
      for (const line of this.track.querySelectorAll<HTMLElement>('.label-carousel-item')) {
        this.measureLine(line);
      }
      this.viewport.classList.remove('is-measuring');
      this.scheduleCurrentItem();
    });
  }

  /**
   * How far the body of a line overruns the room it has. The room is the strip
   * rather than the whole line, so a pinned glyph's width is already taken out
   * of it — which is what keeps the glyph on screen once the body starts moving.
   */
  private measureLine(line: HTMLElement): number {
    const strip = line.querySelector<HTMLElement>('.label-carousel-strip');
    const inner = line.querySelector<HTMLElement>('.label-carousel-text');
    if (!strip || !inner) return 0;
    const overflow = Math.max(0, inner.scrollWidth - strip.clientWidth);
    line.classList.toggle('is-scrolling', overflow > 0);
    return overflow;
  }

  private snapTrackToStart(): void {
    this.track.style.transition = 'none';
    this.track.style.transform = 'translateY(0)';
    void this.track.offsetHeight;
    this.track.style.transition = '';
  }

  private resetItemTransforms(): void {
    for (const inner of this.track.querySelectorAll<HTMLElement>('.label-carousel-text')) {
      inner.style.transition = 'none';
      inner.style.transform = 'translateX(0)';
    }
    void this.track.offsetHeight;
    for (const inner of this.track.querySelectorAll<HTMLElement>('.label-carousel-text')) {
      inner.style.transition = '';
    }
  }

  private clearCycle(): void {
    if (this.phaseHandle !== null) {
      clearTimeout(this.phaseHandle);
      this.phaseHandle = null;
    }
    if (this.scrollFrame !== null) {
      cancelAnimationFrame(this.scrollFrame);
      this.scrollFrame = null;
    }
    if (this.layoutFrame !== null) {
      cancelAnimationFrame(this.layoutFrame);
      this.layoutFrame = null;
    }
  }
}

export { TRANSITION_MS as LABEL_TRANSITION_MS, HOLD_MS as LABEL_HOLD_MS };
