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
    expect(inner.style.transform).toBe('translateX(-104px)');
    expect(inner.style.transition).toBe('transform 2600ms linear');
    expect(host.querySelector<HTMLElement>('.label-carousel-track')!.style.transform).toBe(
      'translateY(0)'
    );

    vi.advanceTimersByTime(3600);
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
    expect(inner.style.transform).toBe('translateX(-144px)');
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

 it('remeasures the repeated date before it enters after a layout change',()=>{
  vi.useFakeTimers();vi.spyOn(window,'requestAnimationFrame').mockImplementation(callback=>{callback(0);return 1;});
  const host=document.createElement('div'),carousel=new LabelCarousel(host);
  carousel.setItems([{text:'2026 年 10 月 9 日 星期五'},{text:'宜 祭祀',pinnedPrefix:'宜 '},{text:'忌 动土',pinnedPrefix:'忌 '}]);
  const lines=host.querySelectorAll<HTMLElement>('.label-carousel-item');let width=100;
  for(const i of [0,3]){Object.defineProperty(lines[i],'clientWidth',{get:()=>width});Object.defineProperty(lines[i].querySelector('.label-carousel-text'),'scrollWidth',{value:180});}
  carousel.setActive(true);expect(lines[3].classList.contains('is-scrolling')).toBe(true);
  width=400;carousel.advance();carousel.advance();carousel.advance();
  // Both copies must be centered during entry, not repaired after the slide.
  expect(lines[3].classList.contains('is-scrolling')).toBe(false);expect(lines[0].classList.contains('is-scrolling')).toBe(false);
  vi.advanceTimersByTime(LABEL_TRANSITION_MS);expect(host.querySelector<HTMLElement>('.label-carousel-track')!.style.transform).toBe('translateY(0)');carousel.destroy();
 });
 it('uses row capacity rather than a flex strip that changes with alignment',()=>{
  vi.spyOn(window,'requestAnimationFrame').mockImplementation(callback=>{callback(0);return 1;});const host=document.createElement('div'),carousel=new LabelCarousel(host);
  carousel.setItems([{text:'a fitting date label'},{text:'宜 祭祀'}]);
  for(const line of host.querySelectorAll<HTMLElement>('.label-carousel-item')){Object.defineProperty(line,'clientWidth',{value:300});Object.defineProperty(line.querySelector('.label-carousel-strip'),'clientWidth',{get:()=>line.classList.contains('is-scrolling')?300:179});Object.defineProperty(line.querySelector('.label-carousel-text'),'scrollWidth',{value:180});}
  carousel.setActive(true);for(const line of host.querySelectorAll('.label-carousel-item'))expect(line.classList.contains('is-scrolling')).toBe(false);carousel.destroy();
 });
 it('does not treat one pixel of rounding as a scrolling line',()=>{
  vi.spyOn(window,'requestAnimationFrame').mockImplementation(callback=>{callback(0);return 1;});const host=document.createElement('div'),carousel=new LabelCarousel(host);carousel.setItems([{text:'a fitting date label'}]);const line=host.querySelector<HTMLElement>('.label-carousel-item')!;Object.defineProperty(line,'clientWidth',{value:300});Object.defineProperty(line.querySelector('.label-carousel-text'),'scrollWidth',{value:301});carousel.setActive(true);expect(line.classList.contains('is-scrolling')).toBe(false);carousel.destroy();
 });

 it('loops permanent almanac text with a hidden trailing copy and a fixed badge',()=>{
  vi.useFakeTimers();vi.spyOn(window,'requestAnimationFrame').mockImplementation(callback=>{callback(0);return 1;});
  const host=document.createElement('div'),carousel=new LabelCarousel(host);
  carousel.setItems([{text:'宜 祭祀 · 祈福 · 纳采 · 出行',pinnedPrefix:'宜 ',continuous:true}]);
  const line=host.querySelector<HTMLElement>('.label-carousel-item')!,inner=line.querySelector<HTMLElement>('.label-carousel-text')!;
  inner.style.fontSize='20px';Object.defineProperty(line,'clientWidth',{value:100});Object.defineProperty(inner,'scrollWidth',{value:200});
  const cancel=vi.fn(),animate=vi.fn(()=>({cancel} as unknown as Animation));inner.animate=animate;
  carousel.setActive(true);
  const copy=inner.querySelector<HTMLElement>('.label-carousel-repeat')!;
  expect(copy.textContent).toBe('祭祀 · 祈福 · 纳采 · 出行');expect(copy.getAttribute('aria-hidden')).toBe('true');expect(copy.style.left).toBe('230px');
  expect(animate).toHaveBeenCalledWith([
   {transform:'translateX(0)',offset:0},{transform:'translateX(0)',offset:1000/6750},{transform:'translateX(-230px)',offset:1}
  ],{duration:6750,iterations:Infinity,easing:'linear'});
  expect(line.querySelector<HTMLElement>('.label-carousel-pin')!.style.transform).toBe('');
  vi.advanceTimersByTime(20000);expect(animate).toHaveBeenCalledTimes(1);
  carousel.setActive(false);expect(cancel).toHaveBeenCalledOnce();expect(inner.querySelector('.label-carousel-repeat')).toBeNull();carousel.destroy();
 });
 it('leaves a fitting permanent line still and cleans up an old loop when content changes',()=>{
  vi.useFakeTimers();vi.spyOn(window,'requestAnimationFrame').mockImplementation(callback=>{callback(0);return 1;});
  const host=document.createElement('div'),carousel=new LabelCarousel(host);carousel.setItems([{text:'忌 安葬 · 出行 · 动土',pinnedPrefix:'忌 ',continuous:true}]);
  const inner=host.querySelector<HTMLElement>('.label-carousel-text')!;Object.defineProperty(inner,'scrollWidth',{value:200});
  const cancel=vi.fn();inner.animate=vi.fn(()=>({cancel} as unknown as Animation));carousel.setActive(true);
  carousel.setItems([{text:'忌 无',pinnedPrefix:'忌 ',continuous:true}]);
  expect(cancel).toHaveBeenCalledOnce();expect(host.querySelector('.label-carousel-repeat')).toBeNull();expect(host.querySelector('.is-scrolling')).toBeNull();carousel.destroy();
 });
