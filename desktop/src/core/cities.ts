import data from './city-names.json';
const names:Record<string,Record<string,string>>=data;
/** Unicode CLDR exemplar cities, with IANA alias normalization. */
export function cityName(zone:string,lang:string):string {
 const table=names[lang]??names['zh-Hans'];let canonical=zone;
 try{canonical=new Intl.DateTimeFormat('en',{timeZone:zone}).resolvedOptions().timeZone;}catch{/* Preserve custom/unknown identifiers. */}
 const tail=canonical.split('/').pop()!;
 return table[zone]??table[canonical]??Object.entries(table).find(([id])=>id.split('/').pop()===tail)?.[1]??tail.replaceAll('_',' ');
}
