import {prefs,defaultTextSize,MIN_FONT_SCALE,MAX_SECONDARY_FONT_SCALE,MIN_TEXT_FONT_SIZE,MAX_TEXT_FONT_SIZE} from '../core/prefs';
import {sliderRow} from './controls';
export type TextRole = 'date' | 'supporting';
export function secondarySizeValue(role: TextRole,id: string,reset=false): number {
  if(id==='classic') return reset ? (role==='date'?55:100) : Math.round((role==='date'?prefs.getDateFontScale(id):prefs.getSupportingScale(id))*100);
  return reset ? defaultTextSize(role,id) : role==='date'?prefs.getDateFontSize(id):prefs.getSupportingFontSize(id);
}
export function saveSecondarySize(role:TextRole,id:string,value:number):void {
  if(id==='classic') { if(role==='date')prefs.setDateFontScale(value/100,id);else prefs.setSupportingScale(value/100,id); }
  else if(role==='date')prefs.setDateFontSize(value,id);else prefs.setSupportingFontSize(value,id);
}
/** Percent is reserved for the classic width-based layout; all other layouts use CSS pixels. */
export function secondaryFontSize(label:string,role:TextRole,theme:string) {
  let active=theme;
  const control=sliderRow(label,MIN_TEXT_FONT_SIZE,MAX_TEXT_FONT_SIZE,24,value=>value+(active==='classic'?'%':' px'));
  function setTheme(id:string,value=secondarySizeValue(role,id)) {
    active=id;control.input.min=String(id==='classic'?MIN_FONT_SCALE*100:MIN_TEXT_FONT_SIZE);control.input.max=String(id==='classic'?MAX_SECONDARY_FONT_SCALE*100:MAX_TEXT_FONT_SIZE);
    control.input.value=String(value);control.input.dispatchEvent(new Event('input'));
  }
  setTheme(theme);
  return {...control,setTheme,value:()=>Number(control.input.value)};
}
