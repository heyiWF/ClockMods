/**
 * Vertically-sliding label carousel used by the calendar grid cells and the
 * 宜/忌 footer.
 *
 * Ported from com.clockmods.ui.CalendarLabelCarouselView and
 * CalendarFooterCarouselView: each item is held for 3s, then slides up while the
 * next slides in over 200ms; a label wider than its cell marquee-scrolls after a
 * 1s pause. Up to 42 cells are on screen at once, so instead of 42 independent
 * timers a single shared ticker advances them all — they start together anyway,
 * which is what the Android views did in practice.
 */

const HOLD_MS = 3000;
const TRANSITION_MS = 200;
/** Labels of at most this many characters never scroll (MAX_STATIC_CHARS). */
const MAX_STATIC_CHARS = 3;

export interface LabelItem {
  text: string;
  /** CSS colour; the footer colours 宜 green and 忌 red. */
  color?: string;
}

const instances = new Set<LabelCarousel>();
let ticker: number | null = null;

function ensureTicker(): void {
  if (ticker !== null) return;
  ticker = window.setInterval(() => {
    for (const instance of instances) instance.advance();
  }, HOLD_MS + TRANSITION_MS);
}

function stopTickerIfIdle(): void {
  if (ticker === null) return;
  for (const instance of instances) {
    if (instance.isActive) return;
  }
  clearInterval(ticker);
  ticker = null;
}

export class LabelCarousel {
  private readonly viewport: HTMLElement;
  private readonly track: HTMLElement;
  private items: LabelItem[] = [];
  private index = 0;
  private active = false;

  constructor(viewport: HTMLElement) {
    this.viewport = viewport;
    this.viewport.classList.add('label-carousel');
    this.track = document.createElement('div');
    this.track.className = 'label-carousel-track';
    this.viewport.replaceChildren(this.track);
  }

  get isActive(): boolean {
    return this.active;
  }

  setItems(items: LabelItem[]): void {
    this.items = items;
    this.index = 0;
    this.track.style.transition = 'none';
    this.track.style.transform = 'translateY(0)';
    this.track.replaceChildren(
      ...items.map((item) => {
        const line = document.createElement('div');
        line.className = 'label-carousel-item';
        if (item.color) line.style.color = item.color;
        const inner = document.createElement('span');
        inner.textContent = item.text;
        // Only labels longer than three characters are allowed to scroll.
        if (item.text.length > MAX_STATIC_CHARS) inner.classList.add('can-scroll');
        line.appendChild(inner);
        return line;
      })
    );
    // Restore the transition after the reset paint.
    requestAnimationFrame(() => {
      this.track.style.transition = '';
    });
  }

  setActive(active: boolean): void {
    if (this.active === active) return;
    this.active = active;
    if (active) {
      instances.add(this);
      ensureTicker();
    } else {
      instances.delete(this);
      stopTickerIfIdle();
    }
  }

  /** Called by the shared ticker; moves to the next item. */
  advance(): void {
    if (this.items.length < 2) return;
    this.index = (this.index + 1) % this.items.length;
    this.track.style.transform = `translateY(${-this.index * 100}%)`;
  }

  destroy(): void {
    this.setActive(false);
  }
}

export { TRANSITION_MS as LABEL_TRANSITION_MS, HOLD_MS as LABEL_HOLD_MS };
