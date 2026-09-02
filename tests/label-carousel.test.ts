/** @vitest-environment jsdom */
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  LABEL_HOLD_MS,
  LABEL_TRANSITION_MS,
  LabelCarousel,
} from '../src/ui/label-carousel';

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

describe('LabelCarousel', () => {
  it('spaces Chinese and digits in dynamic labels', () => {
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);

    carousel.setItems([{ text: '第2项' }]);

    expect(host.querySelector('.label-carousel-item')?.textContent).toBe('第 2 项');
  });

  it('adds a repeated first item for a continuous final transition', () => {
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);

    carousel.setItems([{ text: 'one' }, { text: 'two' }, { text: 'three' }]);

    const labels = [...host.querySelectorAll('.label-carousel-item')].map(
      (item) => item.textContent
    );
    expect(labels).toEqual(['one', 'two', 'three', 'one']);
    expect(host.querySelector('.label-carousel-item:last-child')?.getAttribute('aria-hidden')).toBe(
      'true'
    );
  });

  it('wraps invisibly after sliding onto the repeated first item', () => {
    vi.useFakeTimers();
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
      callback(0);
      return 1;
    });
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);
    carousel.setItems([{ text: 'one' }, { text: 'two' }]);
    const track = host.querySelector<HTMLElement>('.label-carousel-track')!;

    carousel.advance();
    expect(track.style.transform).toBe('translateY(-100%)');
    carousel.advance();
    expect(track.style.transform).toBe('translateY(-200%)');

    vi.advanceTimersByTime(LABEL_TRANSITION_MS);
    expect(track.style.transform).toBe('translateY(0)');
  });

  it('moves overflowing text left once before advancing upward', () => {
    vi.useFakeTimers();
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
      callback(0);
      return 1;
    });
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);
    carousel.setItems([{ text: 'a long label' }, { text: 'next' }]);
    const line = host.querySelector<HTMLElement>('.label-carousel-item')!;
    const strip = line.querySelector<HTMLElement>('.label-carousel-strip')!;
    const inner = line.querySelector<HTMLElement>('.label-carousel-text')!;
    Object.defineProperty(strip, 'clientWidth', { configurable: true, value: 100 });
    Object.defineProperty(inner, 'scrollWidth', { configurable: true, value: 180 });

    carousel.setActive(true);
    vi.advanceTimersByTime(1000);

    expect(line.classList.contains('is-scrolling')).toBe(true);
    expect(inner.style.transform).toBe('translateX(-80px)');
    expect(inner.style.transition).toBe('transform 2000ms linear');
    expect(host.querySelector<HTMLElement>('.label-carousel-track')!.style.transform).toBe(
      'translateY(0)'
    );

    vi.advanceTimersByTime(3000);
    expect(host.querySelector<HTMLElement>('.label-carousel-track')!.style.transform).toBe(
      'translateY(-100%)'
    );
    carousel.destroy();
  });

  it('holds a short line before advancing', () => {
    vi.useFakeTimers();
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
      callback(0);
      return 1;
    });
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);
    carousel.setItems([{ text: 'one' }, { text: 'two' }]);
    carousel.setActive(true);

    vi.advanceTimersByTime(LABEL_HOLD_MS - 1);
    expect(host.querySelector<HTMLElement>('.label-carousel-track')!.style.transform).toBe(
      'translateY(0)'
    );
    vi.advanceTimersByTime(1);
    expect(host.querySelector<HTMLElement>('.label-carousel-track')!.style.transform).toBe(
      'translateY(-100%)'
    );
    carousel.destroy();
  });

  it('stays hidden until overflow alignment is measured', () => {
    const layoutCallbacks: FrameRequestCallback[] = [];
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
      layoutCallbacks.push(callback);
      return 1;
    });
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);
    carousel.setItems([{ text: 'a long label' }, { text: 'next' }]);
    const line = host.querySelector<HTMLElement>('.label-carousel-item')!;
    const strip = line.querySelector<HTMLElement>('.label-carousel-strip')!;
    const inner = line.querySelector<HTMLElement>('.label-carousel-text')!;
    Object.defineProperty(strip, 'clientWidth', { configurable: true, value: 100 });
    Object.defineProperty(inner, 'scrollWidth', { configurable: true, value: 180 });

    carousel.setActive(true);
    expect(host.classList.contains('is-measuring')).toBe(true);

    expect(layoutCallbacks).toHaveLength(1);
    layoutCallbacks[0](0);
    expect(line.classList.contains('is-scrolling')).toBe(true);
    expect(host.classList.contains('is-measuring')).toBe(false);
    carousel.destroy();
  });

  it('splits a pinned 宜/忌 head into a bold glyph outside the scrolling strip', () => {
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);

    carousel.setItems([{ text: '宜 纳采 祭祀', pinnedPrefix: '宜 ' }]);

    const line = host.querySelector<HTMLElement>('.label-carousel-item')!;
    expect(line.querySelector('.label-carousel-pin')?.textContent).toBe('宜 ');
    expect(line.querySelector('.label-carousel-text')?.textContent).toBe('纳采 祭祀');
    // The glyph is a sibling of the strip, never inside it, so it cannot scroll away.
    expect(line.querySelector('.label-carousel-strip')!.contains(
      line.querySelector('.label-carousel-pin')
    )).toBe(false);
    expect(line.textContent).toBe('宜 纳采 祭祀');
  });

  it('leaves a line whose head is not the pinned hint as one plain run', () => {
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);

    carousel.setItems([{ text: '2026 年 8 月 20 日', pinnedPrefix: '宜 ' }]);

    const line = host.querySelector<HTMLElement>('.label-carousel-item')!;
    expect(line.querySelector('.label-carousel-pin')).toBeNull();
    expect(line.querySelector('.label-carousel-text')?.textContent).toBe('2026 年 8 月 20 日');
  });

  it('scrolls only the items of a pinned line, by the room left beside the glyph', () => {
    vi.useFakeTimers();
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
      callback(0);
      return 1;
    });
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);
    carousel.setItems([{ text: '忌 出行 嫁娶 动土 破土', pinnedPrefix: '忌 ' }]);
    const line = host.querySelector<HTMLElement>('.label-carousel-item')!;
    const pin = line.querySelector<HTMLElement>('.label-carousel-pin')!;
    const strip = line.querySelector<HTMLElement>('.label-carousel-strip')!;
    const inner = line.querySelector<HTMLElement>('.label-carousel-text')!;
    // 100px of viewport with 20px taken by the glyph leaves the strip 80px.
    Object.defineProperty(strip, 'clientWidth', { configurable: true, value: 80 });
    Object.defineProperty(inner, 'scrollWidth', { configurable: true, value: 200 });

    carousel.setActive(true);
    vi.advanceTimersByTime(1000);

    expect(line.classList.contains('is-scrolling')).toBe(true);
    expect(inner.style.transform).toBe('translateX(-120px)');
    // The glyph itself is never transformed.
    expect(pin.style.transform).toBe('');
    carousel.destroy();
  });

  it('keeps the next overflowing line left-aligned throughout the upward slide', () => {
    vi.useFakeTimers();
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
      callback(0);
      return 1;
    });
    const host = document.createElement('div');
    const carousel = new LabelCarousel(host);
    carousel.setItems([{ text: 'short' }, { text: 'a very long label' }]);
    const lines = host.querySelectorAll<HTMLElement>('.label-carousel-item');
    const longInner = lines[1].querySelector<HTMLElement>('.label-carousel-text')!;
    Object.defineProperty(lines[0].querySelector('.label-carousel-strip')!, 'clientWidth', {
      configurable: true,
      value: 100,
    });
    Object.defineProperty(lines[0].querySelector('.label-carousel-text')!, 'scrollWidth', {
      configurable: true,
      value: 80,
    });
    Object.defineProperty(lines[1].querySelector('.label-carousel-strip')!, 'clientWidth', {
      configurable: true,
      value: 100,
    });
    Object.defineProperty(longInner, 'scrollWidth', { configurable: true, value: 180 });

    carousel.setActive(true);
    expect(lines[1].classList.contains('is-scrolling')).toBe(true);

    vi.advanceTimersByTime(3000);
    expect(host.querySelector<HTMLElement>('.label-carousel-track')!.style.transform).toBe(
      'translateY(-100%)'
    );
    expect(lines[1].classList.contains('is-scrolling')).toBe(true);

    vi.advanceTimersByTime(200);
    expect(lines[1].classList.contains('is-scrolling')).toBe(true);
    carousel.destroy();
  });
});
