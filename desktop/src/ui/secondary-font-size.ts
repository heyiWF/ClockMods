import {language} from '../core/i18n';
import {prefs,defaultTextSize,MIN_FONT_SCALE,MAX_SECONDARY_FONT_SCALE,MIN_TEXT_FONT_SIZE,MAX_TEXT_FONT_SIZE} from '../core/prefs';
import {sliderRow,element} from './controls';
export type TextRole = 'date' | 'supporting';
export function secondarySizeValue(role: TextRole,id: string,reset=false): number {
  if(id==='classic') return reset ? (role==='date'?55:100) : Math.round((role==='date'?prefs.getDateFontScale(id):prefs.getSupportingScale(id))*100);
  return reset ? 0 : role==='date'?prefs.getDateFontSize(id):prefs.getSupportingFontSize(id);
}
export function saveSecondarySize(role:TextRole,id:string,value:number):void {
  if(id==='classic') { if(role==='date')prefs.setDateFontScale(value/100,id);else prefs.setSupportingScale(value/100,id); }
  else if(role==='date')prefs.setDateFontSize(value,id);else prefs.setSupportingFontSize(value,id);
}
/** Percent is reserved for the classic width-based layout; all other layouts use CSS pixels. */
export function secondaryFontSize(label:string,role:TextRole,theme:string) {
  let active=theme;
  const control=sliderRow(label,MIN_TEXT_FONT_SIZE,MAX_TEXT_FONT_SIZE,24,value=>value+(active==='classic'?'%':' px'));
  const autoLabel=()=>language()==='en'?'Auto':language()==='zh-Hant'?'自動':'自动';
  const automatic=element('button','m3-button m3-button--text',autoLabel());automatic.type='button';
  let auto=false,setting=false;
  control.row.append(automatic);
  const syncAuto=()=>{automatic.hidden=active==='classic';automatic.setAttribute('aria-pressed',String(auto));control.row.querySelector('.settings-slider-value')!.textContent=auto?autoLabel():control.input.value+(active==='classic'?'%':' px');};
  control.input.addEventListener('input',()=>{if(!setting){auto=false;syncAuto();}});
  automatic.addEventListener('click',()=>{auto=!auto;syncAuto();control.row.dispatchEvent(new Event('input',{bubbles:true}));});
  function setTheme(id:string,value=secondarySizeValue(role,id)) {
    active=id;control.input.min=String(id==='classic'?MIN_FONT_SCALE*100:MIN_TEXT_FONT_SIZE);control.input.max=String(id==='classic'?MAX_SECONDARY_FONT_SCALE*100:MAX_TEXT_FONT_SIZE);
    auto=id!=='classic'&&value===0;control.input.value=String(auto?defaultTextSize(role,id):value);setting=true;control.input.dispatchEvent(new Event('input'));setting=false;syncAuto();
  }
  setTheme(theme);
  return {...control,setTheme,value:()=>auto?0:Number(control.input.value)};
}
