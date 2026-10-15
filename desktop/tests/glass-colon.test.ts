// @vitest-environment jsdom
import {describe,it,expect} from 'vitest';
import {glassBrightness} from '../src/ui/gaussian-glass';
import {colonBaselineOffset} from '../src/ui/clock-face';
import {prefs} from '../src/core/prefs';
describe('Ultimate material and time geometry',()=>{
 it('keeps texture at both brightness endpoints and leaves 50 unchanged',()=>{expect(glassBrightness(0)).toEqual({alpha:.55,white:false});expect(glassBrightness(50)).toEqual({alpha:0,white:false});expect(glassBrightness(100)).toEqual({alpha:.55,white:true});});
 it('aligns colon ink centre with zero ink centre for different fonts',()=>{for(const [a,b,c,d] of [[-72,0,-54,0],[-73,2,-68,5],[-68,-1,-70,-3]]){const delta=colonBaselineOffset(a,b,c,d);expect((c+d)/2+delta).toBe((a+b)/2);}expect(colonBaselineOffset(-72,0,-54,0)).toBe(-9);});
 it('does not shift glyphs without measurable ink',()=>{expect(colonBaselineOffset(0,0,-50,0)).toBe(0);expect(colonBaselineOffset(-70,0,0,0)).toBe(0);expect(colonBaselineOffset(NaN,0,-50,0)).toBe(0);});
 it('bounds malformed glass settings and isolates themes',()=>{localStorage.clear();prefs.setThemeGlass({enabled:true,strength:150,brightness:-3},'ultimate.blend');expect(prefs.getThemeGlass('ultimate.blend')).toEqual({enabled:true,strength:100,brightness:0});expect(prefs.getThemeGlass('ultimate.bubbles')).toEqual({enabled:false,strength:50,brightness:25});});
});
