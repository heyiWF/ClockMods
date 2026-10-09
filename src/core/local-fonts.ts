/** Local Font Access is opt-in. Font bytes stay in memory and are never uploaded. */
export interface LocalFace {
  family: string; fullName: string; postscriptName: string; style: string;
  weight: number; min: number; max: number; verified: boolean;
}
interface FontDataLike { family: string; fullName: string; postscriptName: string; style: string; blob(): Promise<Blob>; }
type FontWindow = Window & { queryLocalFonts?: () => Promise<FontDataLike[]> };
const PREFIX = 'local-font:';
let catalog: LocalFace[] = [];
const sources = new Map<string, FontDataLike>();
const documents = new WeakMap<Document, Map<string, Promise<void>>>();
export const localFontsSupported = () => typeof window !== 'undefined' && typeof (window as FontWindow).queryLocalFonts === 'function';
export const localFontCatalog = () => catalog;
export const localFontId = (face: LocalFace) => PREFIX + encodeURIComponent(JSON.stringify(face));
export function readLocalFont(id: string): LocalFace | null {
  if (!id.startsWith(PREFIX) || id.length > 16000) return null;
  try {
    const f = JSON.parse(decodeURIComponent(id.slice(PREFIX.length))) as LocalFace;
    if (!['family','fullName','postscriptName','style'].every(k => typeof f[k as keyof LocalFace] === 'string' && String(f[k as keyof LocalFace]).length < 1024) || !f.family || !f.postscriptName) return null;
    if (![f.weight,f.min,f.max].every(n => Number.isFinite(n) && n >= 1 && n <= 1000) || f.min > f.weight || f.weight > f.max) return null;
    return f;
  } catch { return null; }
}
export function inferredWeight(style: string): number {
  const s = style.toLowerCase().replace(/[\s_-]/g, '');
  if (/extrablack|ultrablack/.test(s)) return 950;
  if (/black|heavy/.test(s)) return 900;
  if (/extrabold|ultrabold/.test(s)) return 800;
  if (/semibold|demibold/.test(s)) return 600;
  if (/bold/.test(s)) return 700;
  if (/medium/.test(s)) return 500;
  if (/extralight|ultralight/.test(s)) return 200;
  if (/thin|hairline/.test(s)) return 100;
  if (/light/.test(s)) return 300;
  return 400;
}
export async function requestLocalFonts(): Promise<LocalFace[]> {
  const query = (window as FontWindow).queryLocalFonts;
  if (!query) throw new DOMException('Local fonts unavailable', 'NotSupportedError');
  const result = await query.call(window);
  const previous = new Map(catalog.map(f => [f.postscriptName,f]));
  sources.clear();
  for (const f of result) sources.set(f.postscriptName, f);
  catalog = [...sources.values()].map(f => previous.get(f.postscriptName) ?? {
    family:f.family, fullName:f.fullName, postscriptName:f.postscriptName, style:f.style,
    weight:inferredWeight(f.style), min:inferredWeight(f.style), max:inferredWeight(f.style), verified:false,
  }).sort((a,b) => a.family.localeCompare(b.family) || a.weight-b.weight || a.fullName.localeCompare(b.fullName));
  return catalog;
}
/** SFNT OS/2 weight and fvar wght axis. Bounds checks also cover damaged font files. */
export function fontWeightMetadata(buffer: ArrayBuffer, postscriptName?: string): {weight:number;min:number;max:number} | null {
  const v = new DataView(buffer);
  const inside = (o:number,n:number) => o>=0 && n>=0 && o+n<=v.byteLength;
  const tag = (o:number) => inside(o,4) ? String.fromCharCode(...new Uint8Array(buffer,o,4)) : '';
  const directory=(base:number)=>{
    const tables=new Map<string,{offset:number;length:number}>();
    if(!inside(base,12) || !['OTTO','true','typ1','\x00\x01\x00\x00'].includes(tag(base))) return tables;
    const count=v.getUint16(base+4);
    if(!inside(base+12,count*16)) return tables;
    for(let i=0;i<count;i++) {
      const p=base+12+i*16,offset=v.getUint32(p+8),length=v.getUint32(p+12);
      if(inside(offset,length)) tables.set(tag(p),{offset,length});
    }
    return tables;
  };
  const matchesName=(tables:ReturnType<typeof directory>)=>{
    const names=tables.get('name'); if(!names || names.length<6) return false;
    const base=names.offset,count=v.getUint16(base+2),strings=v.getUint16(base+4);
    if(6+count*12>names.length) return false;
    for(let i=0;i<count;i++) {
      const p=base+6+i*12;
      if(v.getUint16(p+6)!==6) continue;
      const length=v.getUint16(p+8),offset=strings+v.getUint16(p+10),platform=v.getUint16(p);
      if(offset+length>names.length) continue;
      const bytes=new Uint8Array(buffer,base+offset,length);
      const name=new TextDecoder(platform===0||platform===3?'utf-16be':'macintosh').decode(bytes);
      if(name===postscriptName) return true;
    }
    return false;
  };
  let tables=directory(0);
  if(tag(0)==='ttcf') {
    if(!inside(0,12)) return null;
    const count=v.getUint32(8); if(!inside(12,count*4)) return null;
    let found=false;
    for(let i=0;i<count;i++) {
      const candidate=directory(v.getUint32(12+i*4));
      if(matchesName(candidate) || count===1) {tables=candidate;found=true;break;}
    }
    if(!found) return null;
  }
  const os=tables.get('OS/2');
  let weight=os && os.length>=6 ? v.getUint16(os.offset+4) : 400;
  const variable=tables.get('fvar');
  if(variable && variable.length>=16) {
    const base=variable.offset, start=v.getUint16(base+4), count=v.getUint16(base+8), size=v.getUint16(base+10);
    if(size>=20 && start>=16 && start+count*size<=variable.length) for(let i=0;i<count;i++) {
      const p=base+start+i*size;
      if(tag(p)==='wght') {
        const min=v.getInt32(p+4)/65536, normal=v.getInt32(p+8)/65536, max=v.getInt32(p+12)/65536;
        if(min>=1 && max<=1000 && min<=normal && normal<=max) return {weight:normal,min,max};
      }
    }
  }
  if(!os || weight<1 || weight>1000) return null;
  return {weight,min:weight,max:weight};
}
export async function prepareLocalFont(id: string): Promise<string> {
  const f=readLocalFont(id);
  if(!f || f.verified) return id;
  const source=sources.get(f.postscriptName);
  if(!source) throw new Error('Local font access required');
  const metadata=fontWeightMetadata(await (await source.blob()).arrayBuffer(),f.postscriptName);
  if(!metadata) throw new Error('Cannot read font weight');
  const face={...f,...metadata,verified:true};
  catalog=catalog.map(item=>item.postscriptName===f.postscriptName?face:item);
  return localFontId(face);
}
export function cssString(value: string): string {
  return '"' + value.replace(/["\\\x00-\x1f\x7f]/g, c => '\\' + c.charCodeAt(0).toString(16) + ' ') + '"';
}
export const localFontAlias = (f:LocalFace) => 'ClockModsLocal_' + Array.from(new TextEncoder().encode(f.postscriptName),b=>b.toString(16).padStart(2,'0')).join('');
export async function loadLocalFont(id:string, doc:Document=document):Promise<void> {
  const f=readLocalFont(id);
  if(!f || !doc.fonts) return;
  let loaded=documents.get(doc);
  if(!loaded) { loaded=new Map(); documents.set(doc,loaded); }
  let task=loaded.get(id);
  if(!task) {
    task=(async()=>{
      // Exact PostScript/full names avoid collisions with the bundled web fonts.
      const face=new FontFace(localFontAlias(f), 'local('+cssString(f.postscriptName)+'), local('+cssString(f.fullName)+')', {weight:f.min===f.max?String(f.weight):f.min+' '+f.max,style:'normal'});
      await face.load(); doc.fonts.add(face);
    })();
    loaded.set(id,task);
  }
  try { await task; } catch { loaded.delete(id); /* Uninstalled/unavailable font falls back to defaults. */ }
}
