import chars from './traditional-map.json';
const mapping:Record<string,string>={...chars,钟:"鐘"};
/** Interface/lunar copy only; never rewrite user-entered messages or font identifiers. */
export function traditional(text:string):string {return [...text].map(c=>mapping[c]??c).join('');}
export function localizeUiTree(root:HTMLElement,lang:string):void {
 if(lang!=='zh-Hant')return;
 const walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);
 const nodes:Text[]=[];while(walker.nextNode())nodes.push(walker.currentNode as Text);
 for(const n of nodes){if(n.parentElement?.closest('input,textarea,[data-font-select],.settings-preview-frame'))continue;if(['简体中文','繁體中文','English'].includes(n.data.trim()))continue;n.data=traditional(n.data);}
 for(const e of root.querySelectorAll('[aria-label],[title],[placeholder]'))for(const a of ['aria-label','title','placeholder'])if(e.hasAttribute(a))e.setAttribute(a,traditional(e.getAttribute(a)!));
}
