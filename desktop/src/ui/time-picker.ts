import { t } from '../core/i18n';
/** The browser's native time picker follows OS locale, not the app language. */
export function openTimePicker(minutes:number,title:string,apply:(minutes:number)=>void):void {
 const dialog=document.createElement('dialog');dialog.className='timer-dialog no-page-swipe';dialog.setAttribute('aria-label',title);
 const form=document.createElement('form');form.method='dialog';const heading=document.createElement('h2');heading.textContent=title;
 const row=document.createElement('div');row.className='timer-dialog-row';
 const inputs=[['timer_hours',23,Math.floor(minutes/60)],['timer_minutes_unit',59,minutes%60]].map(([key,max,value])=>{const label=document.createElement('label');label.className='timer-dialog-field';const caption=document.createElement('span');caption.textContent=t(String(key));const input=document.createElement('input');input.type='number';input.min='0';input.max=String(max);input.step='1';input.required=true;input.inputMode='numeric';input.value=String(value);input.setAttribute('aria-label',caption.textContent);label.append(caption,input);row.append(label);return input;});
 const actions=document.createElement('div');actions.className='timer-dialog-actions';const cancel=document.createElement('button');cancel.type='button';cancel.className='m3-button m3-button--text';cancel.textContent=t('cancel');cancel.onclick=()=>dialog.close();const save=document.createElement('button');save.type='submit';save.className='m3-button m3-button--filled';save.textContent=t('apply');actions.append(cancel,save);
 form.append(heading,row,actions);dialog.append(form);document.body.append(dialog);
 form.addEventListener('submit',event=>{event.preventDefault();if(!form.reportValidity())return;apply(Number(inputs[0].value)*60+Number(inputs[1].value));dialog.close();});
 dialog.addEventListener('close',()=>dialog.remove(),{once:true});dialog.showModal();inputs[0].focus();
}
