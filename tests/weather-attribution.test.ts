// @vitest-environment jsdom
import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';
import {WeatherAttribution} from '../src/ui/weather-attribution';
let badge: HTMLAnchorElement, popup: WeatherAttribution;
function move(type='mouse') { const event=new Event('pointermove');Object.defineProperty(event,'pointerType',{value:type});document.dispatchEvent(event); }
beforeEach(()=>{vi.useFakeTimers();document.body.innerHTML='<section><div><a hidden></a></div></section>';badge=document.querySelector('a')!;popup=new WeatherAttribution(document.querySelector('section')!,badge);popup.setEnabled(true);popup.start();});
afterEach(()=>{popup.stop();vi.useRealTimers();});
describe('weather source popup',()=>{
 it('starts hidden and disappears three seconds after mouse movement',()=>{expect(badge.hidden).toBe(true);move();expect(badge.hidden).toBe(false);vi.advanceTimersByTime(2999);expect(badge.hidden).toBe(false);vi.advanceTimersByTime(1);expect(badge.hidden).toBe(true);expect(badge.parentElement!.tagName).toBe('SECTION');});
 it('restarts the idle deadline with every mouse movement',()=>{move();vi.advanceTimersByTime(2500);move();vi.advanceTimersByTime(2999);expect(badge.hidden).toBe(false);vi.advanceTimersByTime(1);expect(badge.hidden).toBe(true);});
 it('ignores touch and pen input and hides immediately when weather is disabled',()=>{move('touch');move('pen');expect(badge.hidden).toBe(true);move();popup.setEnabled(false);expect(badge.hidden).toBe(true);move();expect(badge.hidden).toBe(true);});
 it('cleans up when leaving a page and waits for fresh movement when returning',()=>{move();popup.stop();expect(badge.hidden).toBe(true);expect(vi.getTimerCount()).toBe(0);move();expect(badge.hidden).toBe(true);popup.start();expect(badge.hidden).toBe(true);move();expect(badge.hidden).toBe(false);});
});
