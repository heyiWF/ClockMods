import {prefs,ULTIMATE_DEFAULTS,CALENDAR_IDS} from '../src/core/prefs';
import {CLOCK_THEMES} from '../src/core/clock-themes';
import {ClockPage} from '../src/pages/clock';
import {CalendarPage} from '../src/pages/calendar';
import {setLanguage} from '../src/core/i18n';
import {ensureFontLoaded} from '../src/core/fonts';
import markup from '../index.html?raw';
import tokens from '../src/styles/tokens.css?inline';
import fonts from '../src/styles/fonts.css?inline';
import material from '../src/styles/material.css?inline';
import base from '../src/styles/base.css?inline';
import clock from '../src/styles/clock.css?inline';
import calendar from '../src/styles/calendar.css?inline';
const fontStyle=document.createElement('style');fontStyle.textContent=fonts;document.head.append(fontStyle);
const result=document.querySelector('#result'), frame=document.querySelector('#viewport');
const doc=frame.contentDocument;doc.open();doc.write('<html><head><style>'+[tokens,fonts,material,base,clock,calendar].join('\n')+'</style></head><body><div id="app"><div id="pages"></div></div></body></html>');doc.close();
const parsed=new DOMParser().parseFromString(markup,'text/html');
for(const page of ['clock','calendar'])doc.querySelector('#pages').append(doc.importNode(parsed.querySelector(`[data-page="${page}"]`),true));
let id='classic',lang='zh-Hans',font='system',variant=0;
const settings={...prefs,getClockTheme:()=>id,getClockLanguage:()=>lang,isClockUseEnglish:()=>lang==='en',getFontFamily:()=>font,getFontWeight:()=>400,hasFontWeight:()=>true,isBoldText:()=>false,getTimeFontScale:()=>.88,getDateFontScale:()=>.55,getSupportingScale:()=>1,getDateFontSize:()=>0,getSupportingFontSize:()=>0,getUltimateOptions:()=>({...ULTIMATE_DEFAULTS,calendarTheme:id}),getThemePalette:()=>prefs.getThemePalette(id),getThemeGlass:()=>({enabled:false,strength:50,brightness:25}),getBackgroundMode:()=> 'theme',isWeatherEnabled:()=>variant%2===1,isWeatherDetailed:()=>true,getCustomMessage:()=>variant%2?'Long supporting text for a complete marquee cycle / 天气信息与留言连续轮播':'',isShowSeconds:()=>variant%3!==0,isSmallSeconds:()=>variant%3===1,isUse24Hour:()=>variant%2===0,isPortraitStacked:()=>variant%2===1,isDateLunarDualLine:()=>variant%2===0,isShowLunar:()=>true,getDatePatternCn:()=> 'yyyy年M月d日 EEEE',getDatePatternEn:()=> 'EEEE, MMMM d, yyyy',isAnimateTimeChanges:()=>false};
const clockRoot=doc.querySelector('[data-page=clock]'),calRoot=doc.querySelector('[data-page=calendar]');
const c=new ClockPage(clockRoot,()=>{},settings,async()=>null),cal=new CalendarPage(calRoot,()=>{},settings);
const fixture={status:'success',message:null,data:{locationId:'fixture',city:'深圳',district:'宝安',text:'多云',icon:'101',temperature:'25',updatedAt:0,detail:{feelsLike:'27',humidity:'81',windDir:'东北风',windScale:'3',precip:'0',warning:null,aqiValue:'42',aqiCategory:'优'}}};
const next=()=>Promise.resolve();
const failures=[];let cases=0;
try {
for(const [w,h] of [[320,568],[360,800],[800,360],[768,1024],[1024,768],[1920,1080],[1080,1920],[3840,2160]]) {
 frame.style.width=w+'px';frame.style.height=h+'px';
 for(lang of ['zh-Hans','zh-Hant','en'])for(font of ['system','lora']) {
 setLanguage(lang);await Promise.all([ensureFontLoaded(font,400,doc),ensureFontLoaded(font,400)]);
 for(id of [...CLOCK_THEMES.map(t=>t.id),...CALENDAR_IDS]) {
  variant=cases%6;const isCal=id.startsWith('calendar.');clockRoot.classList.toggle('is-active',!isCal);calRoot.classList.toggle('is-active',isCal);
  if(isCal){cal.refreshSettings();cal.bindWeather(fixture);cal.fitSecondaryTypography();}else {await c.refreshSettings();c.onWeatherState(fixture);c.render();}await next();
  const root=isCal?calRoot:clockRoot;
  const selectors=isCal?'.cal-date-gregorian,.cal-agenda-day,.cal-agenda-month,#cal-time,#cal-title,.cal-day-number':'.face-number,.face-label,#clock-date,#clock-lunar,#clock-time:not(:has(.ultimate-face))';
  for(const el of root.querySelectorAll(selectors)) {
   const rect=el.getBoundingClientRect();if(!rect.width||!rect.height)continue;
   if(rect.left < -1||rect.top < -1||rect.right>w+1||rect.bottom>h+1)failures.push({id,w,h,lang,font,node:el.id||el.className,problem:'outside viewport',rect:[rect.x,rect.y,rect.width,rect.height]});
   if(el.classList.contains('face-number')||el.id==='clock-date'||el.id==='clock-lunar') {
    const range=doc.createRange();range.selectNodeContents(el);const text=range.getBoundingClientRect();
    if(text.width>rect.width+2||text.height>rect.height+2)failures.push({id,w,h,lang,font,node:el.id||el.dataset.part,problem:'text overflow',box:[rect.width,rect.height],text:[text.width,text.height]});
   }
  }
  cases++;result.textContent=JSON.stringify({cases,failures:failures.slice(0,30),failureCount:failures.length},null,2);
 }
 }
}
result.textContent=JSON.stringify({complete:true,cases,failureCount:failures.length,failures},null,2);
}catch(error){result.textContent=JSON.stringify({error:String(error),stack:error.stack,cases,failures},null,2);}finally{c.dispose();cal.dispose();}
