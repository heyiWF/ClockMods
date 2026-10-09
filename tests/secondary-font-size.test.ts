// @vitest-environment jsdom
import {beforeEach,describe,expect,it} from 'vitest';
import {prefs,CALENDAR_IDS} from '../src/core/prefs';
import {CLOCK_THEMES} from '../src/core/clock-themes';
import {secondaryFontSize,secondarySizeValue,saveSecondarySize} from '../src/ui/secondary-font-size';
beforeEach(()=>localStorage.clear());
describe('secondary font size units and ranges',()=>{
 it('uses equal bounds and units for date and supporting controls in every theme',()=>{
  for(const id of [...CLOCK_THEMES.map(t=>t.id),...CALENDAR_IDS]){
   const date=secondaryFontSize('日期字号','date',id),support=secondaryFontSize('辅助文字字号','supporting',id);
   expect([date.input.min,date.input.max]).toEqual(id==='classic'?['20','200']:['8','80']);
   expect([support.input.min,support.input.max]).toEqual([date.input.min,date.input.max]);
   date.setTheme(id,24);support.setTheme(id,24);expect(date.row.querySelector('.settings-slider-value')!.textContent).toBe(support.row.querySelector('.settings-slider-value')!.textContent);
  }
 });
 it('reads old proportions without writing or retaining different implicit multipliers',()=>{
  prefs.setDateFontScale(1.1,'ultimate.bubbles');prefs.setSupportingScale(2,'ultimate.bubbles');const before=JSON.stringify(localStorage);
  expect(secondarySizeValue('date','ultimate.bubbles')).toBe(48);expect(secondarySizeValue('supporting','ultimate.bubbles')).toBe(48);expect(JSON.stringify(localStorage)).toBe(before);
 });
 it('persists pixels separately by theme and clamps both kinds to the same endpoints',()=>{
  for(const role of ['date','supporting'] as const){saveSecondarySize(role,'ultimate.bubbles',80);saveSecondarySize(role,'calendar.paper',32);expect(secondarySizeValue(role,'ultimate.bubbles')).toBe(80);expect(secondarySizeValue(role,'calendar.paper')).toBe(32);saveSecondarySize(role,'ultimate.bubbles',-3);expect(secondarySizeValue(role,'ultimate.bubbles')).toBe(8);saveSecondarySize(role,'classic',200);expect(secondarySizeValue(role,'classic')).toBe(200);}
 });
 it('switches units and limits when returning to classic',()=>{
  const control=secondaryFontSize('日期字号','date','ultimate.bubbles');control.setTheme('classic',150);expect(control.value()).toBe(150);expect(control.row.textContent).toContain('150%');control.setTheme('ultimate.bubbles',32);expect(control.value()).toBe(32);expect(control.row.textContent).toContain('32 px');expect(control.input.max).toBe('80');
 });
 it('clears the new pixel overrides on reset',()=>{
  prefs.setDateFontSize(80,'ultimate.bubbles');prefs.setSupportingFontSize(80,'ultimate.bubbles');prefs.clearThemeOverrides();expect(prefs.getDateFontSize('ultimate.bubbles')).toBe(24);expect(prefs.getSupportingFontSize('ultimate.bubbles')).toBe(24);
 });
});
