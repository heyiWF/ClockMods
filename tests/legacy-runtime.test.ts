import { readFileSync, existsSync } from 'node:fs';
// @ts-expect-error jsdom is a runtime-only test dependency in this repository.
import { JSDOM } from 'jsdom';
import { expect, it } from 'vitest';

const html = readFileSync('index.html', 'utf8');
const script = readFileSync('public/legacy-clock.js', 'utf8');
it('ships every production asset referenced by the classic-script entry', () => {
  for (const name of ['legacy-clock.css', 'legacy-clock.js', 'legacy-lunar.js', 'legacy-language.js']) {
    expect(html).toContain(name); expect(existsSync('public/' + name)).toBe(true);
  }
  expect(html).not.toContain('type="module"');
  expect(script).not.toMatch(/\b(?:const|let|class|async|await|Promise|fetch|ResizeObserver|Map|Set)\b|=>/);
  expect(readFileSync('public/legacy-clock.css', 'utf8')).not.toMatch(/var\(--|display:\s*grid|\bdvh\b/);
});
it('boots without modern APIs and persists all theme controls', () => {
  const dom = new JSDOM(html, { url: 'https://clock.example/', runScripts: 'outside-only' });
  const win = dom.window;
  try {
    for (const api of ['Promise', 'fetch', 'ResizeObserver', 'Map', 'Set']) Object.defineProperty(win, api, { value: undefined });
    win.eval(readFileSync('public/legacy-lunar.js', 'utf8')); win.eval(script);
    expect(win.document.getElementById('clock-main')!.textContent).not.toBe('--:--');
    expect(win.document.getElementById('clock-date')!.textContent + win.document.getElementById('clock-lunar')!.textContent).toMatch(/\[[^\]]+\]年/);
    for (const theme of ['classic', 'ultimate.dual_blocks', 'ultimate.orbit', 'ultimate.bubbles', 'ultimate.blend', 'ultimate.ribbon']) {
      (win.document.getElementById('settings-button') as HTMLButtonElement).click();
      expect(win.getComputedStyle(win.document.getElementById('settings-overlay')!).display).not.toBe('none');
      const select = win.document.getElementById('pref-theme') as HTMLSelectElement;
      select.value = theme; select.dispatchEvent(new win.Event('change'));
      (win.document.getElementById('settings-apply') as HTMLButtonElement).click();
      expect(win.localStorage.getItem('clockmods_legacy.theme')).toBe(theme);
      expect(win.document.getElementById('clock-lines')!.className).toContain('theme-' + theme.replace('ultimate.', ''));
      const grouped = ['ultimate.dual_blocks','ultimate.bubbles','ultimate.blend'].includes(theme);
      expect(win.document.querySelectorAll('#clock-main .material-part').length > 0).toBe(grouped);
    }
    (win.document.getElementById('settings-button') as HTMLButtonElement).click();
    (win.document.getElementById('pref-message') as HTMLInputElement).value = '<img src=x onerror=alert(1)>';
    (win.document.getElementById('settings-cancel') as HTMLButtonElement).click();
    expect(win.localStorage.getItem('clockmods_legacy.message')).toBe('');
  } finally { win.close(); }
});

it('reflows immediately on extreme resize and resets matching date and support sizes', () => {
  const dom = new JSDOM(html, { url: 'https://clock.example/', runScripts: 'outside-only' });
  const win = dom.window;
  try {
    win.eval(readFileSync('public/legacy-lunar.js', 'utf8')); win.eval(script);
    Object.defineProperty(win, 'innerWidth', { value: 240, configurable: true });
    Object.defineProperty(win, 'innerHeight', { value: 1600, configurable: true });
    win.dispatchEvent(new win.Event('resize'));
    expect(win.document.getElementById('clock-time')!.className).toContain('is-stacked');
    Object.defineProperty(win, 'innerWidth', { value: 1600, configurable: true });
    Object.defineProperty(win, 'innerHeight', { value: 240, configurable: true });
    win.dispatchEvent(new win.Event('resize'));
    expect(win.document.getElementById('clock-time')!.className).not.toContain('is-stacked');
    (win.document.getElementById('settings-button') as HTMLButtonElement).click();
    (win.document.getElementById('pref-supporting-scale') as HTMLInputElement).value = '200';
    (win.document.getElementById('settings-apply') as HTMLButtonElement).click();
    (win.document.getElementById('settings-button') as HTMLButtonElement).click();
    (win.document.getElementById('settings-reset') as HTMLButtonElement).click();
    (win.document.getElementById('settings-apply') as HTMLButtonElement).click();
    expect(win.localStorage.getItem('clockmods_legacy.supporting-scale')).toBe('100');
    expect(win.document.getElementById('clock-date')!.style.fontSize)
      .toBe(win.document.getElementById('clock-extra')!.style.fontSize);
  } finally { win.close(); }
});

it('localizes settings and time-zone cities without translating user input',()=>{
 const dom=new JSDOM(html,{url:'https://clock.example/',runScripts:'outside-only'}),win=dom.window;
 try{win.eval(readFileSync('public/legacy-language.js','utf8'));win.eval(script);const el=(id:string)=>win.document.getElementById(id) as HTMLInputElement;
 el('settings-button').click();el('pref-message').value='保留我的留言';el('pref-language').value='en';el('pref-language').dispatchEvent(new win.Event('change'));
 expect(el('settings-title').textContent).toBe('Clock settings');expect(el('pref-timezone').textContent).toContain('New York');expect(el('pref-weather-interval').textContent).toContain('Every 30 minutes');expect(el('pref-message').value).toBe('保留我的留言');
 el('pref-language').value='zh-Hant';el('pref-language').dispatchEvent(new win.Event('change'));expect(el('settings-title').textContent).toBe('時鐘設置');expect(el('pref-timezone').textContent).toContain('紐約');el('settings-cancel').click();expect(el('settings-title').textContent).toBe('时钟设置');
 }finally{win.close();}
});
it('keeps pixel sizes independent and preserves automatic settings on reload',()=>{
 const dom=new JSDOM(html,{url:'https://clock.example/',runScripts:'outside-only'}),win=dom.window;
 try{win.eval(script);const el=(id:string)=>win.document.getElementById(id) as HTMLInputElement;
 el('settings-button').click();el('pref-theme').value='ultimate.bubbles';el('pref-theme').dispatchEvent(new win.Event('change'));el('pref-message').value='辅助文字';el('pref-date-size').value='16';el('pref-support-size').value='16';el('settings-apply').click();const size=el('clock-date').style.fontSize;
 el('settings-button').click();el('pref-support-size').value='80';el('settings-apply').click();expect(el('clock-date').style.fontSize).toBe(size);
 el('settings-button').click();el('pref-date-size').value='0';el('pref-support-size').value='0';el('settings-apply').click();expect(win.localStorage.getItem('clockmods_legacy.date-size')).toBe('0');expect(win.localStorage.getItem('clockmods_legacy.support-size')).toBe('0');
 }finally{win.close();}
});
it('keeps preview changes isolated and closes its frame on cancel',()=>{
 const dom=new JSDOM(html,{url:'https://clock.example/',runScripts:'outside-only'}),win=dom.window;
 try{win.eval(script);const el=(id:string)=>win.document.getElementById(id) as HTMLInputElement;el('settings-button').click();expect(el('settings-preview').querySelector('iframe')!.src).toBe('https://clock.example/?preview=1');el('pref-font-name').value='Microsoft YaHei';el('pref-font-weight').value='600';el('pref-font-weight').dispatchEvent(new win.Event('change'));expect(win.localStorage.getItem('clockmods_legacy.font-weight')).toBeNull();el('settings-cancel').click();expect(el('settings-preview').children).toHaveLength(0);expect(el('clock-lines').style.fontWeight).toBe('400');
 }finally{win.close();}
});

it('hides weather attribution after idle even while settings stay open',()=>{
 const dom=new JSDOM(html,{url:'https://clock.example/',runScripts:'outside-only'}),win=dom.window;
 try{win.localStorage.setItem('clockmods_legacy.weather','true');win.eval(script);win.document.getElementById('settings-button')!.click();const tasks:Array<()=>void>=[];win.setTimeout=(callback:()=>void,delay:number)=>{if(delay===3000)tasks.push(callback);return 1;};win.document.dispatchEvent(new win.MouseEvent('mousemove'));expect(win.document.getElementById('weather-attribution')!.style.display).not.toBe('none');tasks.forEach(callback=>callback());expect(win.document.getElementById('weather-attribution')!.style.display).toBe('none');expect(win.document.documentElement.className).not.toContain('is-cursor-idle');
 }finally{win.close();}
});
