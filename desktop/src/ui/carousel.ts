/** Weather/message carousel with fixed-size marquee and shared Ultimate motion. */
import { createWeatherIcon } from '../weather/icons';
import { renderSupportingText } from './clock-face';
import { easeOutCubic, motionDuration, scanMask, supportingTravel } from './clock-motion';

const HOLD_MS = 3000, SCROLL_PAUSE_MS = 1000, SCROLL_PX_PER_SECOND = 40;
export interface CarouselItem { text: string; weather?: { left: string; iconCode: string; right: string }; }
export const plainItem = (text: string): CarouselItem => ({ text });
export const weatherItem = (left: string, iconCode: string, right: string): CarouselItem =>
  ({ text: `${left}  ${right}`, weather: { left, iconCode, right } });
export function sameItems(a: CarouselItem[], b: CarouselItem[]): boolean {
  return a.length === b.length && a.every((item, i) => item.text === b[i].text && item.weather?.iconCode === b[i].weather?.iconCode);
}
interface Swap {
  layer: HTMLElement; track: HTMLElement; startedAt: number;
  oldOffset: number; oldTravel: number; newTravel: number;
}
export class Carousel {
  private readonly viewport: HTMLElement;
  private readonly layer = document.createElement('span');
  private readonly track = document.createElement('span');
  private items: CarouselItem[] = [];
  private index = 0;
  private cycleStartedAt = 0;
  private active = false;
  private frameHandle: number | null = null;
  private iconFill = true;
  private transition = 'fade';
  private swap: Swap | null = null;
  constructor(element: HTMLElement) {
    this.viewport = element; element.classList.add('carousel');
    this.layer.className = 'carousel-layer';
    this.track.className = 'carousel-track';
    this.layer.append(this.track); element.replaceChildren(this.layer);
  }
  setIconStyle(fill: boolean): void {
    if (this.iconFill === fill) return;
    this.iconFill = fill;
    this.finishSwap(); this.render();
  }
  setTransition(transition: string): void {
    if (this.transition === transition) return;
    this.finishSwap(); this.transition = transition; this.cycleStartedAt = performance.now();
  }
  setItems(items: CarouselItem[]): void {
    if (sameItems(this.items, items)) return;
    this.finishSwap(); this.items = items; this.index = 0; this.cycleStartedAt = performance.now();
    this.viewport.hidden = !items.length;
    this.render(); this.scroll(0);
    if (items.length && this.active) this.startFrames(); else this.stopFrames();
  }
  setActive(active: boolean): void {
    if (this.active === active) return;
    this.active = active;
    if (active && this.items.length) this.startFrames(); else this.stopFrames();
  }
  get isEmpty(): boolean { return this.items.length === 0; }
  private startFrames(): void {
    if (this.frameHandle !== null) return;
    this.cycleStartedAt = performance.now();
    const step = () => { this.tick(); this.frameHandle = requestAnimationFrame(step); };
    this.frameHandle = requestAnimationFrame(step);
  }
  private stopFrames(): void {
    if (this.frameHandle !== null) cancelAnimationFrame(this.frameHandle);
    this.frameHandle = null; this.finishSwap(); this.scroll(0);
  }
  private reducedMotion(): boolean { return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false; }
  private overflow(): number { return Math.max(0, this.track.scrollWidth - this.viewport.clientWidth); }
  private holdDuration(): number { return Math.max(HOLD_MS, SCROLL_PAUSE_MS * 2 + this.overflow() / SCROLL_PX_PER_SECOND * 1000); }
  private scroll(elapsed: number): void {
    const overflow = this.overflow();
    this.layer.classList.toggle('is-scrolling', overflow > 0);
    const offset = -Math.min(overflow, Math.max(0, elapsed - SCROLL_PAUSE_MS) / 1000 * SCROLL_PX_PER_SECOND);
    this.track.style.transform = `translateX(${offset}px)`;
  }
  private tick(): void {
    if (!this.items.length) return;
    const now = performance.now();
    if (this.swap) {
      const p = Math.min(1, (now - this.swap.startedAt) / motionDuration(this.transition, true));
      if (p >= 1 || this.reducedMotion()) { this.finishSwap(); this.cycleStartedAt = now; this.scroll(0); }
      else this.animateSwap(p);
      return;
    }
    const elapsed = now - this.cycleStartedAt, duration = this.holdDuration();
    this.scroll(elapsed);
    if (elapsed < duration) return;
    if (this.items.length === 1) { this.cycleStartedAt = now; this.scroll(0); return; }
    this.beginSwap(now);
  }
  private beginSwap(now: number): void {
    const oldOverflow = this.overflow();
    const layer = this.layer.cloneNode(true) as HTMLElement;
    layer.classList.add('is-outgoing'); layer.setAttribute('aria-hidden', 'true');
    const track = layer.querySelector<HTMLElement>('.carousel-track')!;
    this.viewport.append(layer);
    const font = parseFloat(getComputedStyle(this.viewport).fontSize) || 16;
    const oldWidth = this.track.scrollWidth;
    const oldOffset = -oldOverflow;
    const viewportRect = this.viewport.getBoundingClientRect();
    const oldRoom = Math.max(0, viewportRect.right - this.track.getBoundingClientRect().right);
    this.index = (this.index + 1) % this.items.length;
    this.render(); this.scroll(0);
    const newWidth = this.track.scrollWidth;
    const newRoom = Math.max(0, this.track.getBoundingClientRect().left - viewportRect.left);
    // Freeze outgoing geometry before measuring the next sentence: different
    // lengths must not change the hold duration midway through a transition.
    this.swap = { layer, track, startedAt: now, oldOffset,
      oldTravel: Math.min(oldRoom, supportingTravel(oldWidth, font)),
      newTravel: Math.min(newRoom, supportingTravel(newWidth, font)) };
    this.animateSwap(0);
  }
  private animateSwap(p: number): void {
    const old = this.swap!;
    this.layer.style.opacity = '1'; old.layer.style.opacity = '1';
    this.layer.style.maskImage = ''; old.layer.style.maskImage = '';
    let incoming = '', outgoing = '';
    if (this.transition === 'scan') {
      const revealing = p >= .5;
      old.layer.style.opacity = revealing ? '0' : '1';
      this.layer.style.opacity = revealing ? '1' : '0';
      old.layer.style.maskImage = scanMask(Math.min(1, p * 2), false);
      this.layer.style.maskImage = scanMask(Math.max(0, (p - .5) * 2), true);
    } else if (this.transition === 'slide_right') {
      const eased = easeOutCubic(p);
      old.layer.style.opacity = String(1 - eased); this.layer.style.opacity = String(eased);
      outgoing = ` translateX(${old.oldTravel * eased}px)`;
      incoming = ` translateX(${-old.newTravel * (1 - eased)}px)`;
    } else {
      const eased = easeOutCubic(p);
      old.layer.style.opacity = String(1 - eased); this.layer.style.opacity = String(eased);
      if (this.transition === 'slide_up' || this.transition === 'slide_down') {
        const direction = this.transition === 'slide_up' ? -1 : 1;
        outgoing = ` translateY(${direction * .24 * eased}em)`;
        incoming = ` translateY(${-direction * .24 * (1 - eased)}em)`;
      } else if (this.transition === 'scale') {
        outgoing = ` scale(${1 + .08 * eased})`; incoming = ` scale(${.88 + .12 * eased})`;
      } else if (this.transition === 'flip') {
        old.layer.style.opacity = p < .5 ? String(1 - p * 2) : '0';
        this.layer.style.opacity = p < .5 ? '0' : String((p - .5) * 2);
        outgoing = ` rotateX(${Math.min(90, p * 180)}deg)`; incoming = ` rotateX(${Math.min(0, (p - 1) * 180)}deg)`;
      }
    }
    old.track.style.transform = `translateX(${old.oldOffset}px)${outgoing}`;
    this.track.style.transform = `translateX(0px)${incoming}`;
  }
  private finishSwap(): void {
    this.swap?.layer.remove(); this.swap = null;
    this.layer.style.opacity = ''; this.layer.style.maskImage = ''; this.track.style.transform = '';
  }
  private render(): void {
    const item = this.items[this.index];
    delete this.track.dataset.text;
    if (!item) { this.track.replaceChildren(); return; }
    if (!item.weather) { const text = document.createElement('span'); renderSupportingText(text, item.text); this.track.replaceChildren(text); return; }
    const { left, iconCode, right } = item.weather;
    const a = document.createElement('span'), b = document.createElement('span');
    renderSupportingText(a, left); renderSupportingText(b, right);
    const icon = createWeatherIcon(iconCode, this.iconFill);
    if (icon) this.track.replaceChildren(a, icon, b);
    else { const text = document.createElement('span'); renderSupportingText(text, `${left}  ${right}`); this.track.replaceChildren(text); }
  }
}
