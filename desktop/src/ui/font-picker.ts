import { traditional } from '../core/ui-language';
import { prefs } from '../core/prefs';
import { FONT_OPTIONS, optionFor } from '../core/fonts';
import { localFontsSupported, localFontCatalog, localFontId, readLocalFont, requestLocalFonts, prepareLocalFont } from '../core/local-fonts';
import { element, select, textField, summaryLabel } from './controls';
const setters=new WeakMap<HTMLSelectElement,(id:string)=>void>();
const pending=new WeakMap<HTMLSelectElement,Promise<void>>();
export function setFontSelection(select:HTMLSelectElement,id:string):void { setters.get(select)?.(id); }
export function pendingFontSelections(root:HTMLElement):Promise<void>[] { return [...root.querySelectorAll<HTMLSelectElement>('[data-font-select]')].flatMap(s=>pending.has(s)?[pending.get(s)!]:[]); }
/** Every installed face is selectable, including italic/condensed and uncommon weights. */
export function fontPicker(selected:string,en:boolean) {
  const copy=(value:string)=>prefs.getClockLanguage()==='zh-Hant'?traditional(value):value;
  const row=element('div','settings-font-picker');
  const input=select([],selected); input.dataset.fontSelect='';
  const search=textField('',en?'Search fonts and styles':'搜索字体、字重或样式'); search.type='search';
  search.setAttribute('aria-label',en?'Search fonts':'搜索字体');
  const button=element('button','m3-button m3-button--tonal',en?'Read installed fonts':'读取本机字体'); button.type='button';
  const status=summaryLabel(''); status.setAttribute('role','status');
  const note=summaryLabel(en?'Choose an installed weight/style from the list. Variable fonts allow any supported weight. Missing Chinese glyphs use the default font.':'列表包含字体的各个字重与样式；可变字体可调节连续字重。缺失的中文字形使用默认字体。');
  let revision=0, accepted=selected;
  function render(id:string) {
    const query=search.value.trim().toLocaleLowerCase(), fragment=document.createDocumentFragment();
    const builtins=element('optgroup'); builtins.label=en?'Built-in fonts':'内置字体';
    for(const f of FONT_OPTIONS) if(f.id===id || !query || f.displayName.toLocaleLowerCase().includes(query)) {
      const option=element('option',undefined,f.id==='system'?(en?'System font':'系统字体'):f.displayName); option.value=f.id; builtins.append(option);
    }
    if(builtins.children.length) fragment.append(builtins);
    const faces=[...localFontCatalog()], saved=readLocalFont(id);
    if(saved) { const index=faces.findIndex(f=>f.postscriptName===saved.postscriptName); if(index<0) faces.push(saved); else faces[index]=saved; }
    const groups=new Map<string,HTMLOptGroupElement>();
    for(const f of faces) {
      const value=localFontId(f);
      if(value!==id && query && !(f.family+' '+f.fullName+' '+f.style+' '+f.weight).toLocaleLowerCase().includes(query)) continue;
      let group=groups.get(f.family);
      if(!group) { group=element('optgroup'); group.label=f.family; groups.set(f.family,group); fragment.append(group); }
      const option=element('option',undefined,f.fullName+' · '+f.style); option.value=value; group.append(option);
    }
    input.replaceChildren(fragment); input.value=id;
    if(!input.value) { const option=element('option',undefined,optionFor(id).displayName); option.value=id; input.append(option); input.value=id; }
  }
  const set=(id:string)=>{ revision++; accepted=id; render(id); };
  setters.set(input,set); render(selected);
  search.addEventListener('input',()=>render(input.value));
  input.addEventListener('focus',()=>render(input.value));
  input.addEventListener('change',event=>{
    const id=input.value, local=readLocalFont(id);
    if(!local || local.verified) { accepted=id; revision++; return; }
    event.stopImmediatePropagation();
    const token=++revision, previous=accepted;
    input.value=previous;
    status.textContent=copy(en?'Reading font weight…':'正在读取字体字重…');
    const task=(async()=>{
      try {
        const ready=await prepareLocalFont(id);
        if(token!==revision) return;
        accepted=ready; render(ready);
        status.textContent=copy(en?'Font ready':'字体已就绪');
        input.dispatchEvent(new Event('change',{bubbles:true}));
      } catch {
        if(token===revision) { render(previous); status.textContent=copy(en?'This font could not be read. Choose another face or retry access.':'无法读取该字体，请选择其他款式或重新读取本机字体。'); }
      }
    })();
    pending.set(input,task); void task.finally(()=>{ if(pending.get(input)===task) pending.delete(input); });
  });
  if(!localFontsSupported()) {
    button.disabled=true;
    status.textContent=copy(en?'Full local font access requires a supported desktop browser (Chrome/Edge) and HTTPS or localhost.':'完整本机字体列表需要支持此功能的桌面浏览器（Chrome / Edge），并通过 HTTPS 或 localhost 打开。');
  } else {
    status.textContent=copy(en?'Allow local font access when prompted. Fonts stay on this device.':'点击读取并允许浏览器访问本机字体；字体文件不会上传。');
  }
  button.addEventListener('click',async()=>{
    button.disabled=true; status.textContent=copy(en?'Reading installed fonts…':'正在读取本机字体…');
    try {
      const fonts=await requestLocalFonts(); render(input.value);
      status.textContent=copy(fonts.length ? (en?'Loaded '+fonts.length+' installed faces':'已读取 '+fonts.length+' 个本机字体款式') : (en?'No local fonts were returned.':'浏览器未返回本机字体。'));
    } catch(error) {
      const name=(error as Error).name;
      status.textContent=copy(['NotAllowedError','SecurityError'].includes(name)
        ? (en?'Access was not granted. Allow local fonts in site permissions and retry.':'尚未获得权限，请在网站权限中允许访问本机字体后重试。')
        : (en?'Could not read local fonts. Please retry.':'读取本机字体失败，请重试。'));
    } finally {button.disabled=false;}
  });
  row.append(button,status,search,input,note);
  return {row,input};
}
