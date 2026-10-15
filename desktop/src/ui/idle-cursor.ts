/** Hide only an idle mouse cursor; dialogs always retain their normal cursors. */
export function installIdleCursor(doc:Document=document,delay=3000):()=>void {
 let timer:ReturnType<typeof setTimeout>|undefined;let mouse=false;
 const clear=()=>{if(timer!==undefined)clearTimeout(timer);timer=undefined;doc.documentElement.classList.remove('is-cursor-idle');};
 const arm=()=>{clear();if(mouse && !doc.hidden && !doc.querySelector('dialog[open]'))timer=setTimeout(()=>{if(!doc.querySelector('dialog[open]'))doc.documentElement.classList.add('is-cursor-idle');},delay);};
 const move=(event:PointerEvent)=>{if(event.pointerType!=='mouse')return;mouse=true;arm();};
 const observer=new MutationObserver(records=>{if(records.some(r=>r.type==='attributes' || [...r.addedNodes,...r.removedNodes].some(n=>n instanceof Element && (n.matches('dialog')||n.querySelector('dialog')))))arm();});observer.observe(doc.body,{subtree:true,attributes:true,attributeFilter:['open'],childList:true});
 doc.addEventListener('pointermove',move);doc.addEventListener('pointerdown',move);doc.addEventListener('visibilitychange',arm);doc.defaultView?.addEventListener('blur',clear);doc.defaultView?.addEventListener('focus',arm);
 return()=>{clear();observer.disconnect();doc.removeEventListener('pointermove',move);doc.removeEventListener('pointerdown',move);doc.removeEventListener('visibilitychange',arm);doc.defaultView?.removeEventListener('blur',clear);doc.defaultView?.removeEventListener('focus',arm);};
}
