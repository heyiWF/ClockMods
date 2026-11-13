/** Hide the cursor (mouse) and fullscreen button after a period of inactivity.
 *  On touch devices without a mouse cursor, the idle class still hides the
 *  fullscreen button; tapping the screen reveals it again for another cycle. */
export function installIdleCursor(doc:Document=document,delay=3000):()=>void {
 let timer:ReturnType<typeof setTimeout>|undefined;
 const clear=()=>{if(timer!==undefined)clearTimeout(timer);timer=undefined;doc.documentElement.classList.remove('is-cursor-idle');};
 const arm=()=>{clear();if(!doc.hidden && !doc.querySelector('dialog[open]'))timer=setTimeout(()=>{if(!doc.querySelector('dialog[open]'))doc.documentElement.classList.add('is-cursor-idle');},delay);};
 const interact=()=>{arm();};
 const observer=new MutationObserver(records=>{if(records.some(r=>r.type==='attributes' || [...r.addedNodes,...r.removedNodes].some(n=>n instanceof Element && (n.matches('dialog')||n.querySelector('dialog')))))arm();});observer.observe(doc.body,{subtree:true,attributes:true,attributeFilter:['open'],childList:true});
 arm();
 doc.addEventListener('keydown',arm);doc.addEventListener('pointermove',interact);doc.addEventListener('pointerdown',interact);doc.addEventListener('visibilitychange',arm);doc.defaultView?.addEventListener('blur',clear);doc.defaultView?.addEventListener('focus',arm);
 return()=>{clear();observer.disconnect();doc.removeEventListener('keydown',arm);doc.removeEventListener('pointermove',interact);doc.removeEventListener('pointerdown',interact);doc.removeEventListener('visibilitychange',arm);doc.defaultView?.removeEventListener('blur',clear);doc.defaultView?.removeEventListener('focus',arm);};
}
