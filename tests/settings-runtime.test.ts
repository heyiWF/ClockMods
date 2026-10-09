// @vitest-environment jsdom
import {afterEach,describe,expect,it,vi} from 'vitest';
import {timeButton} from '../src/ui/controls';
import {availableWeights,nearestWeight} from '../src/core/fonts';
import {prefs} from '../src/core/prefs';
import {applyScreenOrientation} from '../src/core/orientation';
import {setLanguage,t} from '../src/core/i18n';
import {timeSource} from '../src/core/time-source';
afterEach(()=>{prefs.setUseNetworkTime(false);timeSource.configure();vi.restoreAllMocks();vi.unstubAllGlobals();localStorage.clear();});
describe('settings runtime behavior',()=>{
 it('reports unsupported orientation locks without implying a layout rotation',async()=>{prefs.setScreenOrientation(1);vi.stubGlobal('screen',{orientation:{}});expect(await applyScreenOrientation()).toBe(false);});
 it('uses the Web name in English notifications',()=>{setLanguage('en');expect(t('alarm_open_to_dismiss')).toContain('ClockMods Web');expect(t('timer_complete_open')).toContain('ClockMods Web');setLanguage('zh-Hans');});
 it('allows repeated edits to a quiet/dimming time, retaining its input',()=>{const control=timeButton('Start',1320);document.body.append(control.button);const input=control.button.querySelector('input')!;input.value='01:15';input.dispatchEvent(new Event('change'));expect(control.minutes()).toBe(75);expect(control.button.querySelector('input')).toBe(input);input.value='03:45';input.dispatchEvent(new Event('change'));expect(control.minutes()).toBe(225);expect(control.button.textContent).toContain('03:45');});
 it('offers only shipped font weight stops',()=>{expect(availableWeights('lora')).toEqual([400,500,600,700]);expect(availableWeights('google_sans_display')).toEqual([400,500,700]);expect(availableWeights('roboto')).toHaveLength(9);expect(nearestWeight('lora',900)).toBe(700);});
 it('resynchronizes immediately after changing the time-source URL',async()=>{localStorage.clear();const fetch=vi.spyOn(globalThis,'fetch').mockResolvedValue(new Response('',{headers:{date:'Fri, 09 Oct 2026 00:00:00 GMT'}}));prefs.setUseNetworkTime(true);prefs.setTimeSourceUrl('https://one.example/time');timeSource.configure();await vi.waitFor(()=>expect(timeSource.isSynchronized()).toBe(true));prefs.setTimeSourceUrl('https://two.example/time');timeSource.configure();await vi.waitFor(()=>expect(fetch).toHaveBeenCalledTimes(2));expect(fetch.mock.calls[1][0]).toBe('https://two.example/time');});
});
