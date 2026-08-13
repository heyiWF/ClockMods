/**
 * The rotating supporting line shared by the weather summary, the custom message
 * and the detailed-weather metrics.
 *
 * Ported from the Carousel/WeatherLineItem half of com.clockmods.ui.ClockView,
 * keeping its exact timing: each item is held for 3s (longer when it has to
 * scroll), an over-wide item pauses 1s, marquee-scrolls at 40px/s and pauses 1s
 * again, and items cross over with a 200ms out / 200ms in transition. The line is
 * always drawn at the configured size — overflow scrolls instead of shrinking.
 */
import { createWeatherIcon } from '../weather/icons';
import { renderSupportingText } from './clock-face';
import {
  TRANSITION_FADE,
  TRANSITION_FLIP,
  TRANSITION_SCALE,
  TRANSITION_SLIDE_DOWN,
  TRANSITION_SLIDE_UP,
} from '../core/prefs';

const HOLD_MS = 3000;
const SCROLL_PAUSE_MS = 1000;
const TRANSITION_MS = 200;
const SCROLL_PX_PER_SECOND = 40;

export interface CarouselItem {
  /** Plain text, or the concatenated form used for equality checks. */
  text: string;
  /** Weather summary items split around an icon. */
  weather?: { left: string; iconCode: string; right: string };
}

export function plainItem(text: string): CarouselItem {
  return { text };
}

export function weatherItem(left: string, iconCode: string, right: string): CarouselItem {
  return { text: `${left}  ${right}`, weather: { left, iconCode, right } };
}

export function sameItems(a: CarouselItem[], b: CarouselItem[]): boolean {
  if (a.length !== b.length) return false;
  return a.every((item, index) => item.text === b[index].text);
}

export class Carousel {
  private readonly viewport: HTMLElement;
  private readonly track: HTMLElement;
  private items: CarouselItem[] = [];
  private index = 0;
  private cycleStartedAt = 0;
  private active = false;
  private frameHandle: number | null = null;
  private iconFill = true;
  private transition = TRANSITION_FADE;
  private renderedIndex = -1;

  constructor(element: HTMLElement) {
    this.viewport = element;
    this.viewport.classList.add('carousel');
    this.track = document.createElement('span');
    this.track.className = 'carousel-track';
    this.viewport.replaceChildren(this.track);
  }

  setIconStyle(fill: boolean): void {
    if (this.iconFill === fill) return;
    this.iconFill = fill;
    this.renderedIndex = -1;
  }

  /** Pro exposes the clock's transition style here too (WEATHER_DETAIL_USES_TIME_TRANSITION). */
  setTransition(transition: string): void {
    this.transition = transition;
  }

  setItems(items: CarouselItem[]): void {
    if (sameItems(this.items, items)) return;
    this.items = items;
    this.index = 0;
    this.cycleStartedAt = 0;
    this.renderedIndex = -1;
    if (items.length === 0) {
      this.track.replaceChildren();
      this.viewport.hidden = true;
      this.stopFrames();
      return;
    }
    this.viewport.hidden = false;
    this.render(0);
    if (this.active) this.startFrames();
  }

  setActive(active: boolean): void {
    if (this.active === active) return;
    this.active = active;
    if (active && this.items.length > 0) this.startFrames();
    else this.stopFrames();
  }

  get isEmpty(): boolean {
    return this.items.length === 0;
  }

  private startFrames(): void {
    if (this.frameHandle !== null) return;
    this.cycleStartedAt = performance.now();
    const step = () => {
      this.frameHandle = requestAnimationFrame(step);
      this.tick();
    };
    this.frameHandle = requestAnimationFrame(step);
  }

  private stopFrames(): void {
    if (this.frameHandle === null) return;
    cancelAnimationFrame(this.frameHandle);
    this.frameHandle = null;
    this.track.style.removeProperty('transform');
    this.track.style.removeProperty('opacity');
  }

  /** One frame of the hold → scroll → cross-fade cycle. */
  private tick(): void {
    if (this.items.length === 0) return;
    const now = performance.now();
    if (this.cycleStartedAt === 0) this.cycleStartedAt = now;
    let elapsed = now - this.cycleStartedAt;

    const current = this.items[Math.min(this.index, this.items.length - 1)];
    const overflow = this.overflowWidth();
    const displayDuration = this.displayDuration(overflow);

    if (this.items.length === 1) {
      if (elapsed >= displayDuration) {
        this.cycleStartedAt = now;
        elapsed = 0;
      }
      this.render(this.index);
      this.applyScroll(overflow, elapsed);
      this.applyTransition(1, 0);
      return;
    }

    const fadeOutEnd = displayDuration + TRANSITION_MS;
    const fadeInEnd = fadeOutEnd + TRANSITION_MS;

    if (elapsed < displayDuration) {
      this.render(this.index);
      this.applyScroll(overflow, elapsed);
      this.applyTransition(1, 0);
    } else if (elapsed < fadeOutEnd) {
      const progress = (elapsed - displayDuration) / TRANSITION_MS;
      this.render(this.index);
      this.applyScroll(overflow, displayDuration);
      this.applyTransition(1 - progress, progress);
    } else if (elapsed < fadeInEnd) {
      const progress = (elapsed - fadeOutEnd) / TRANSITION_MS;
      this.render((this.index + 1) % this.items.length);
      this.applyScroll(this.overflowWidth(), 0);
      this.applyTransition(progress, -(1 - progress));
    } else {
      this.index = (this.index + 1) % this.items.length;
      this.cycleStartedAt = now;
      this.render(this.index);
      this.applyScroll(this.overflowWidth(), 0);
      this.applyTransition(1, 0);
    }
    void current;
  }

  private displayDuration(overflow: number): number {
    if (overflow <= 0) return HOLD_MS;
    const scrollMs = Math.ceil((overflow / SCROLL_PX_PER_SECOND) * 1000);
    return Math.max(HOLD_MS, SCROLL_PAUSE_MS * 2 + scrollMs);
  }

  private overflowWidth(): number {
    return Math.max(0, this.track.scrollWidth - this.viewport.clientWidth);
  }

  /** Marquee offset: pause, scroll at a constant speed, pause again. */
  private applyScroll(overflow: number, elapsed: number): void {
    if (overflow <= 0) {
      this.track.style.setProperty('--scroll', '0px');
      this.viewport.classList.remove('is-scrolling');
      return;
    }
    this.viewport.classList.add('is-scrolling');
    const scrollMs = Math.ceil((overflow / SCROLL_PX_PER_SECOND) * 1000);
    const progress = Math.max(0, Math.min(1, (elapsed - SCROLL_PAUSE_MS) / scrollMs));
    this.track.style.setProperty('--scroll', `${-overflow * progress}px`);
  }

  /**
   * @param opacity 0..1
   * @param phase negative while entering, positive while leaving, 0 when settled
   */
  private applyTransition(opacity: number, phase: number): void {
    const track = this.track;
    track.style.opacity = String(opacity);
    let transform = 'translateX(var(--scroll, 0px))';
    switch (this.transition) {
      case TRANSITION_SLIDE_UP:
      case TRANSITION_SLIDE_DOWN: {
        const direction = this.transition === TRANSITION_SLIDE_UP ? -1 : 1;
        transform += ` translateY(${direction * 0.6 * phase}em)`;
        break;
      }
      case TRANSITION_SCALE:
        transform += ` scale(${1 - 0.12 * Math.abs(phase)})`;
        break;
      case TRANSITION_FLIP:
        transform += ` scaleY(${Math.max(0.05, 1 - Math.abs(phase))})`;
        // Flip keeps full opacity and conveys the change through the squash alone.
        track.style.opacity = '1';
        break;
      default:
        break;
    }
    track.style.transform = transform;
  }

  private render(index: number): void {
    if (index === this.renderedIndex) return;
    this.renderedIndex = index;
    const item = this.items[index];
    if (!item) return;
    if (!item.weather) {
      renderSupportingText(this.track, item.text);
      return;
    }
    const { left, iconCode, right } = item.weather;
    const leftSpan = document.createElement('span');
    renderSupportingText(leftSpan, left);
    const rightSpan = document.createElement('span');
    renderSupportingText(rightSpan, right);
    const icon = createWeatherIcon(iconCode, this.iconFill);
    delete this.track.dataset.text;
    if (icon) {
      this.track.replaceChildren(leftSpan, icon, rightSpan);
    } else {
      // Without an icon the Android code fell back to a single plain run.
      renderSupportingText(this.track, `${left}  ${right}`);
    }
  }
}
