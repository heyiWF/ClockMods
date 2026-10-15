/** @vitest-environment jsdom */
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { fontWeightMetadata, requestLocalFonts, localFontId, readLocalFont, prepareLocalFont, loadLocalFont, localFontAlias } from '../src/core/local-fonts';
import type { LocalFace } from '../src/core/local-fonts';
import { availableWeights, fontStack, normalizeFontFamily } from '../src/core/fonts';
import { prefs } from '../src/core/prefs';
import { fontPicker, setFontSelection, pendingFontSelections } from '../src/ui/font-picker';
import { fontWeightControl } from '../src/ui/font-weight';
function sfnt(weight=400,variable=false) {
  const data=new ArrayBuffer(160), v=new DataView(data), bytes=new Uint8Array(data);
  const tag=(at:number,text:string)=>bytes.set([...text].map(c=>c.charCodeAt(0)),at);
  v.setUint32(0,0x00010000);v.setUint16(4,variable?2:1);tag(12,'OS/2');v.setUint32(20,48);v.setUint32(24,8);v.setUint16(52,weight);
  if(variable){tag(28,'fvar');v.setUint32(36,64);v.setUint32(40,36);v.setUint16(68,16);v.setUint16(72,1);v.setUint16(74,20);tag(80,'wght');v.setInt32(84,100*65536);v.setInt32(88,400*65536);v.setInt32(92,950*65536);}
  return data;
}
const face:LocalFace={family:'测试字体',fullName:'测试字体 Regular',postscriptName:'TestCJK-Regular',style:'Regular',weight:400,min:400,max:400,verified:true};
function source(overrides:Partial<LocalFace>={},weight=400,variable=false) {return {...face,...overrides,blob:vi.fn(async()=>({arrayBuffer:async()=>sfnt(weight,variable)}))};}
const query=vi.fn();
beforeEach(async()=>{localStorage.clear();document.body.replaceChildren();Object.defineProperty(window,'queryLocalFonts',{configurable:true,value:query});query.mockResolvedValue([]);await requestLocalFonts();query.mockClear();});
afterEach(()=>{vi.unstubAllGlobals();vi.restoreAllMocks();delete (window as Window & {queryLocalFonts?:unknown}).queryLocalFonts;});
describe('local fonts',()=>{
 it('reads exact static weight and continuous variable bounds',()=>{expect(fontWeightMetadata(sfnt(350))).toEqual({weight:350,min:350,max:350});expect(fontWeightMetadata(sfnt(400,true))).toEqual({weight:400,min:100,max:950});expect(fontWeightMetadata(new ArrayBuffer(3))).toBeNull();const b=sfnt();new DataView(b).setUint32(20,99999);expect(fontWeightMetadata(b)).toBeNull();});
 it('matches the selected face inside a TTC collection instead of taking the first weight',()=>{
 const buffer=new ArrayBuffer(400),v=new DataView(buffer),bytes=new Uint8Array(buffer);
 const tag=(p:number,s:string)=>bytes.set([...s].map(c=>c.charCodeAt(0)),p);
 tag(0,'ttcf');v.setUint32(8,2);v.setUint32(12,32);v.setUint32(16,200);
 for(const [base,weight,name] of [[32,300,'Family-Light'],[200,700,'Family-Bold']] as const){v.setUint32(base,0x10000);v.setUint16(base+4,2);tag(base+12,'OS/2');v.setUint32(base+20,base+48);v.setUint32(base+24,8);v.setUint16(base+52,weight);tag(base+28,'name');v.setUint32(base+36,base+64);v.setUint32(base+40,60);const n=base+64;v.setUint16(n+2,1);v.setUint16(n+4,18);v.setUint16(n+6,3);v.setUint16(n+12,6);v.setUint16(n+14,name.length*2);[...name].forEach((c,i)=>v.setUint16(n+18+i*2,c.charCodeAt(0)));}
 expect(fontWeightMetadata(buffer,'Family-Bold')).toEqual({weight:700,min:700,max:700});expect(fontWeightMetadata(buffer,'Missing')).toBeNull();
 });
 it('keeps all installed faces including CJK, italic and uncommon weights',async()=>{query.mockResolvedValue([source(),source({postscriptName:'TestCJK-Light',fullName:'测试字体 Light',style:'Light'},350),source({family:'Latin',postscriptName:'Latin-Italic',style:'Italic'})]);const fonts=await requestLocalFonts();expect(fonts).toHaveLength(3);const initial=fonts.find(f=>f.postscriptName==='TestCJK-Light')!;const id=await prepareLocalFont(localFontId(initial));expect(readLocalFont(id)?.weight).toBe(350);expect(availableWeights(id)).toEqual([350]);expect(fontStack(id)).toMatch(/^"ClockModsLocal_/);expect(fontStack(id)).toContain('system-ui');});
 it('preserves the selected face and arbitrary variable weight across themes',()=>{const id=localFontId({...face,min:100,max:950});prefs.setFontFamily(id,'classic');prefs.setFontWeight(437,'classic');expect(prefs.getFontFamily('classic')).toBe(id);expect(prefs.getFontWeight('classic')).toBe(437);expect(prefs.getFontFamily('calendar.paper')).toBe('system');expect(availableWeights(id)).toHaveLength(851);expect(normalizeFontFamily('local-font:broken')).toBe('system');expect(readLocalFont(localFontId({...face,min:950,max:100}))).toBeNull();});
 it('loads exact local faces into each document with no unicode restriction or remote source',async()=>{const descriptors:unknown[]=[],loaded:unknown[]=[];class FakeFace {constructor(...args:unknown[]){descriptors.push(args);} async load(){return this;}}
 vi.stubGlobal('FontFace',FakeFace);const a=document.implementation.createHTMLDocument(),b=document.implementation.createHTMLDocument();for(const d of [a,b])Object.defineProperty(d,'fonts',{value:{add:(f:unknown)=>loaded.push(f)}});
 const id=localFontId(face);await loadLocalFont(id,a);await loadLocalFont(id,a);await loadLocalFont(id,b);expect(loaded).toHaveLength(2);expect(descriptors[0]).toEqual([localFontAlias(face),'local("TestCJK-Regular"), local("测试字体 Regular")',{weight:'400',style:'normal'}]);});
 it('does not request permission until clicked; reports denied access without changing preferences',async()=>{query.mockRejectedValue(new DOMException('denied','NotAllowedError'));const picker=fontPicker('system',false);document.body.append(picker.row);expect(query).not.toHaveBeenCalled();picker.row.querySelector('button')!.click();await vi.waitFor(()=>expect(picker.row.textContent).toContain('尚未获得权限'));expect(picker.input.value).toBe('system');expect(localStorage.length).toBe(0);});
 it('searches a full catalog, resolves metadata before change, and keeps draft changes unsaved',async()=>{query.mockResolvedValue([source({},350),source({family:'Other',postscriptName:'Other-Regular',fullName:'Other Regular'})]);const picker=fontPicker('system',false);document.body.append(picker.row);picker.row.querySelector('button')!.click();await vi.waitFor(()=>expect(picker.row.textContent).toContain('已读取 2'));
 const search=picker.row.querySelector('input')!;search.value='测试字体';search.dispatchEvent(new Event('input'));expect([...picker.input.options].map(o=>o.textContent).join()).not.toContain('Other');
 const option=[...picker.input.options].find(o=>o.textContent?.includes('测试字体'))!;picker.input.value=option.value;const change=vi.fn();picker.input.addEventListener('change',change);picker.input.dispatchEvent(new Event('change',{bubbles:true}));expect(change).not.toHaveBeenCalled();await Promise.all(pendingFontSelections(picker.row));expect(change).toHaveBeenCalledOnce();expect(readLocalFont(picker.input.value)?.weight).toBe(350);expect(localStorage.length).toBe(0);
 setFontSelection(picker.input,'lora');expect(picker.input.value).toBe('lora');});
 it('does not overwrite another theme when an earlier font read finishes',async()=>{let finish!:(value:{arrayBuffer:()=>Promise<ArrayBuffer>})=>void;const f=source();f.blob=vi.fn(()=>new Promise(resolve=>{finish=resolve;})) as typeof f.blob;query.mockResolvedValue([f]);await requestLocalFonts();const picker=fontPicker('system',false);document.body.append(picker.row);picker.input.value=[...picker.input.options].find(o=>o.value.startsWith('local-font:'))!.value;picker.input.dispatchEvent(new Event('change'));const wait=pendingFontSelections(picker.row);setFontSelection(picker.input,'inter');finish({arrayBuffer:async()=>sfnt()});await Promise.all(wait);expect(picker.input.value).toBe('inter');});
 it('restores a saved local face without enumerating the full library',()=>{const id=localFontId(face),picker=fontPicker(id,false);expect(picker.input.value).toBe(id);expect(query).not.toHaveBeenCalled();});
 it('offers fixed and arbitrary variable weights with numeric labels',()=>{const fixed=fontWeightControl('字重',localFontId({...face,weight:350,min:350,max:350}),400);expect(fixed.input.disabled).toBe(true);expect(fixed.value()).toBe(350);expect(fixed.row.textContent).toContain('350');const id=localFontId({...face,min:100,max:950});fixed.set(id,437);expect(fixed.input.disabled).toBe(false);expect(fixed.value()).toBe(437);expect(fixed.row.textContent).toContain('437');});
 it('explains unsupported browsers and retains bundled choices',()=>{delete (window as Window & {queryLocalFonts?:unknown}).queryLocalFonts;const picker=fontPicker('roboto',false);expect(picker.row.querySelector('button')!.disabled).toBe(true);expect(picker.row.textContent).toContain('Chrome / Edge');expect(picker.input.value).toBe('roboto');});
});
