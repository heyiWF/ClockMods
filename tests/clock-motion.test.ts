// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { Carousel, plainItem } from '../src/ui/carousel';
import { CharacterLine } from '../src/ui/clock-face';
import { changedDigitPair, motionDuration, scanMask, supportingTravel } from '../src/ui/clock-motion';

beforeEach(() => {
  vi.useFakeTimers(); vi.setSystemTime(0);
  vi.spyOn(performance, 'now').mockImplementation(() => Date.now());
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => setTimeout(() => callback(performance.now()), 16));
  vi.stubGlobal('cancelAnimationFrame', clearTimeout);
});
afterEach(() => { document.body.replaceChildren(); vi.restoreAllMocks(); vi.unstubAllGlobals(); vi.useRealTimers(); });

it('synchronizes only the changed field across a minute rollover', () => {
  expect(changedDigitPair('09:59:59', '10:00:00', 0)).toBe(true);
  expect(changedDigitPair('09:59:59', '10:00:00', 2)).toBe(false);
  expect(changedDigitPair('10:00:09', '10:00:10', 6)).toBe(true);
  expect(changedDigitPair('10:00:09', '10:00:10', 3)).toBe(false);
});
it('uses bounded slide travel and a left-to-right feather for both scan phases', () => {
  expect(motionDuration('slide_right')).toBe(440);
  expect(motionDuration('scan')).toBe(480);
  expect(motionDuration('scan', true)).toBe(660);
  expect(supportingTravel(1000, 20)).toBe(48);
  expect(supportingTravel(20, 20)).toBe(15);
  expect(scanMask(0, false)).toContain('transparent -18%, #000 0%');
  expect(scanMask(1, false)).toContain('transparent 100%, #000 118%');
  expect(scanMask(0, true)).toContain('#000 -18%, transparent 0%');
  expect(scanMask(1, true)).toContain('#000 100%, transparent 118%');
});
it('a stale digit timeout cannot clear a newer transition and reset cancels all layers', () => {
  const host = document.createElement('div'), line = new CharacterLine(host);
  const options = { animate: true, transition: 'scan', colonVisible: true };
  line.setText('08', options); line.setText('09', options);
  vi.advanceTimersByTime(200); line.setText('10', options);
  vi.advanceTimersByTime(370);
  expect(host.querySelectorAll('.is-in')).toHaveLength(2);
  expect(host.querySelectorAll('.is-out')).toHaveLength(2);
  line.reset(); expect(host.childElementCount).toBe(0); expect(vi.getTimerCount()).toBe(0);
});
function mount() {
  const host = document.createElement('div'); host.style.fontSize = '20px'; document.body.append(host);
  Object.defineProperty(host, 'clientWidth', { value: 240 });
  vi.spyOn(HTMLElement.prototype, 'scrollWidth', 'get').mockImplementation(function(this: HTMLElement) { return (this.textContent?.length ?? 0) * 12; });
  const carousel = new Carousel(host);
  carousel.setItems([plainItem('short'), plainItem('A much longer sentence that must scroll at its fixed font size')]);
  carousel.setTransition('scan'); carousel.setActive(true);
  return { host, carousel };
}
it('erases the old sentence before revealing the next and finishes even when lengths differ', () => {
  const { host, carousel } = mount();
  vi.advanceTimersByTime(3120);
  const old = host.querySelector<HTMLElement>('.is-outgoing')!;
  const incoming = host.querySelector<HTMLElement>('.carousel-layer:not(.is-outgoing)')!;
  expect(old.textContent).toBe('short');
  expect(old.style.maskImage).toContain('linear-gradient(to right, transparent');
  expect(incoming.style.opacity).toBe('0');
  vi.advanceTimersByTime(300);
  expect(old.style.opacity).toBe('0');
  expect(incoming.style.opacity).toBe('1');
  expect(incoming.style.maskImage).toContain('linear-gradient(to right, #000');
  vi.advanceTimersByTime(400);
  expect(host.querySelector('.is-outgoing')).toBeNull();
  expect(incoming.style.maskImage).toBe('');
  expect(incoming.textContent).toContain('A much longer');
  carousel.setActive(false); expect(vi.getTimerCount()).toBe(0);
});
it('stopping or replacing content clears masks and outgoing sentences immediately', () => {
  const { host, carousel } = mount();
  vi.advanceTimersByTime(3200); carousel.setActive(false);
  expect(host.querySelector('.is-outgoing')).toBeNull();
  expect(host.querySelector<HTMLElement>('.carousel-layer')!.style.maskImage).toBe('');
  carousel.setItems([plainItem('replacement')]);
  expect(host.textContent).toBe('replacement');
});
