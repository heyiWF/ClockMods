/* ClockMods Legacy. ES5, IE11/Trident; no module or modern browser bootstrap. */
(function () {
  'use strict';
  var doc = document, win = window, memory = {}, prefs = {}, image = '', draftImage = '', weather = null;
  var weatherTimer = null, syncTimer = null, request = null, generation = 0, offset = 0, lastChime = '', tickTimer = null;
  var themes = {
    classic: ['#101418', '#1d2631', '#a8c7fa'],
    'ultimate.dual_blocks': ['#eef3fa', '#dce7f7', '#435f91'],
    'ultimate.orbit': ['#141b24', '#233044', '#a8c7fa'],
    'ultimate.bubbles': ['#faf0f6', '#efd9e6', '#864e78'],
    'ultimate.blend': ['#eef5ed', '#d7e8d4', '#41683b'],
    'ultimate.ribbon': ['#faf2e7', '#efddc4', '#7f5830']
  };
  var defaults = {
    'hour-format': '24', timezone: 'local', 'show-seconds': true, 'small-seconds': false,
    'blink-colon': false, animate: true, stacked: false, lunar: true, 'dual-line': false,
    bold: false, 'hourly-chime': true, 'chime-quiet': true, 'chime-start': '22:00', 'chime-end': '07:00',
    transition: 'fade', font: 'system', language: 'zh-Hans', 'date-pattern': 'yyyy年M月d日 EEEE',
    message: '', 'time-scale': '88', 'date-scale': '55', 'time-color': '#ffffff', 'date-color': '#ffffff',
    'background-color': '#101418', 'use-image': false, dim: false, 'schedule-dim': false,
    'dim-start': '22:00', 'dim-end': '06:00', weather: false, 'weather-url': '', 'weather-location': '',
    'weather-interval': '30', 'network-time': false, 'time-url': '', 'sync-interval': '60',
    theme: 'classic', 'panel-color': '#1d2631', 'accent-color': '#a8c7fa', 'card-shadow': true, 'auto-ink': true,
    'weather-transition': 'fade', 'supporting-scale': '100', 'temperature-unit': 'celsius'
  };
  function node(id) { return doc.getElementById(id); }
  function own(object, key) { return Object.prototype.hasOwnProperty.call(object, key); }
  function read(key) {
    if (own(memory, key)) return memory[key];
    try { return win.localStorage.getItem('clockmods_legacy.' + key); } catch (ignore) { return null; }
  }
  function write(key, value) {
    memory[key] = value;
    try { win.localStorage.setItem('clockmods_legacy.' + key, value); } catch (ignore) { /* session still works */ }
  }
  function text(element, value) { if (element.textContent !== value) element.textContent = value; }
  function show(element, visible) {
    if (visible) element.removeAttribute('hidden');
    element.style.display = visible ? '' : 'none';
  }
  function status(message) { text(node('clock-status'), message); }
  function pad(value) { return value < 10 ? '0' + value : String(value); }
  function finite(value, fallback, min, max) {
    value = Number(value); return isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
  }
  function minutes(value) { var parts = String(value).split(':'); return Number(parts[0]) * 60 + Number(parts[1]); }
  function within(current, start, end) { return start === end || (start < end ? current >= start && current < end : current >= start || current < end); }
  function color(value, fallback) { return /^#[0-9a-f]{6}$/i.test(value) ? value : fallback; }
  function ink(value) {
    var n = parseInt(value.substring(1), 16);
    return ((n >> 16) * 299 + ((n >> 8) & 255) * 587 + (n & 255) * 114) / 1000 > 150 ? '#17212c' : '#f4f7fc';
  }
  function migration(key) {
    var keys = { 'hour-format': 'use_24_hour', 'show-seconds': 'show_seconds', 'small-seconds': 'small_seconds',
      'blink-colon': 'blink_colon', animate: 'animate_time_changes', stacked: 'portrait_stacked', lunar: 'show_lunar',
      bold: 'bold_text', language: 'clock_language', message: 'custom_message', font: 'font_family' };
    try {
      var value = keys[key] ? win.localStorage.getItem('clock_prefs.' + keys[key]) : null;
      if (key === 'hour-format' && value !== null) return value === 'true' ? '24' : '12';
      return value;
    } catch (ignore) { return null; }
  }
  function load() {
    var key, value;
    for (key in defaults) if (own(defaults, key)) {
      value = read(key); if (value === null) value = migration(key);
      prefs[key] = value === null ? defaults[key] : typeof defaults[key] === 'boolean' ? value === 'true' : value;
    }
    image = read('background-image') || '';
    try { weather = JSON.parse(read('weather-cache') || 'null'); } catch (ignore) { weather = null; }
    if (!own(themes, prefs.theme)) prefs.theme = 'classic';
  }
  function fill(values) {
    var key, element;
    for (key in defaults) if (own(defaults, key)) {
      element = node('pref-' + key); if (!element) continue;
      if (element.type === 'checkbox') element.checked = values[key]; else element.value = values[key];
    }
    updateRange();
    updateInkControls();
  }
  function updateInkControls() {
    var automatic = node('pref-theme').value !== 'classic' && node('pref-auto-ink').checked;
    node('pref-time-color').disabled = automatic; node('pref-date-color').disabled = automatic;
  }
  function updateRange() {
    text(node('pref-time-scale-value'), node('pref-time-scale').value + '%');
    text(node('pref-date-scale-value'), node('pref-date-scale').value + '%');
  }
  function close() { show(node('settings-overlay'), false); node('settings-button').focus(); }
  function open() {
    fill(prefs); draftImage = image; show(node('settings-overlay'), true); node('settings-close').focus();
  }
  function apply() {
    var key, element, value;
    for (key in defaults) if (own(defaults, key)) {
      element = node('pref-' + key); if (!element) continue;
      value = element.type === 'checkbox' ? element.checked : element.value;
      if (key === 'message') value = String(value).substring(0, 200);
      if (key.indexOf('color') >= 0) value = color(value, defaults[key]);
      prefs[key] = value; write(key, String(value));
    }
    prefs.message = String(prefs.message).substring(0, 200);
    image = draftImage; write('background-image', image);
    close(); configure(); render();
  }
  function nthSunday(year, month, nth) { return 1 + ((7 - new Date(Date.UTC(year, month, 1)).getUTCDay()) % 7) + (nth - 1) * 7; }
  function lastSunday(year, month) { var day = new Date(Date.UTC(year, month + 1, 0)); return day.getUTCDate() - day.getUTCDay(); }
  function clockDate(now) {
    var zone = prefs.timezone, hours = 0, year = new Date(now).getUTCFullYear();
    if (zone === 'local') return new Date(now);
    if (zone === 'asia-shanghai') hours = 8;
    if (zone === 'asia-tokyo') hours = 9;
    if (zone === 'america-new-york') {
      hours = now >= Date.UTC(year, 2, nthSunday(year, 2, 2), 7) && now < Date.UTC(year, 10, nthSunday(year, 10, 1), 6) ? -4 : -5;
    }
    if (zone === 'europe-london') hours = now >= Date.UTC(year, 2, lastSunday(year, 2), 1) && now < Date.UTC(year, 9, lastSunday(year, 9), 1) ? 1 : 0;
    var shifted = new Date(now + hours * 3600000);
    return new Date(shifted.getUTCFullYear(), shifted.getUTCMonth(), shifted.getUTCDate(), shifted.getUTCHours(), shifted.getUTCMinutes(), shifted.getUTCSeconds());
  }
  function dateText(date) {
    var en = prefs.language === 'en', cn = ['日','一','二','三','四','五','六'], english = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
    var pattern = prefs['date-pattern'];
    return pattern.replace(/'([^']*)'|yyyy|yy|MMMM|MMM|MM|M|dd|d|EEEE|EEE|E/g, function (token, literal) {
      if (literal !== undefined) return literal;
      if (token === 'yyyy') return String(date.getFullYear());
      if (token === 'yy') return pad(date.getFullYear() % 100);
      if (token === 'MM') return pad(date.getMonth() + 1);
      if (token === 'M') return String(date.getMonth() + 1);
      if (token === 'dd') return pad(date.getDate());
      if (token === 'd') return String(date.getDate());
      if (token === 'MMM' || token === 'MMMM') {
        var months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
        return en ? (token === 'MMM' ? months[date.getMonth()].substring(0,3) : months[date.getMonth()]) : String(date.getMonth()+1)+'月';
      }
      return en ? (token === 'EEEE' ? english[date.getDay()] : english[date.getDay()].substring(0,3)) : '星期'+cn[date.getDay()];
    });
  }
  function lunarText(date) {
    if (!prefs.lunar || !win.Solar) return '';
    try {
      var lunar = win.Solar.fromDate(date).getLunar();
      if (prefs.language === 'en') return 'Lunar ' + lunar.getMonth() + '/' + lunar.getDay();
      var result = lunar.getYearInGanZhi() + '[' + lunar.getYearShengXiao() + ']年' + lunar.getMonthInChinese() + '月' + lunar.getDayInChinese();
      if (prefs.language === 'zh-Hant') result = result.replace(/[马龙鸡猪闰]/g,function(character){return {'马':'馬','龙':'龍','鸡':'雞','猪':'豬','闰':'閏'}[character];});
      return result;
    } catch (ignore) { return ''; }
  }
  function temperature(value) {
    var n = Number(value); return prefs['temperature-unit'] === 'fahrenheit' && isFinite(n) ? Math.round(n * 9/5 + 32) + '℉' : value + '℃';
  }
  var carouselItems = [], carouselIndex = 0, carouselStart = 0, track;
  function supportingItems() {
    var items = [], detail;
    if (prefs.weather && weather) {
      items.push((weather.city || '') + ' ' + (weather.text || '') + ' ' + temperature(weather.temperature || weather.temp || '--'));
      detail = weather.detail || weather;
      if (detail.feelsLike) items.push((prefs.language === 'en' ? 'Feels like ' : '体感 ') + temperature(detail.feelsLike));
      if (detail.humidity) items.push((prefs.language === 'en' ? 'Humidity ' : '湿度 ') + detail.humidity + '%');
      if (detail.windDir) items.push(detail.windDir + ' ' + (detail.windScale || ''));
      if (detail.warning) items.push(detail.warning);
    }
    if (prefs.message) items.push(prefs.message);
    if (items.join('\u0000') !== carouselItems.join('\u0000')) { carouselItems = items; carouselIndex = 0; carouselStart = 0; }
    show(node('clock-extra'), items.length > 0);
  }
  function carousel(now) {
    if (!carouselItems.length) return;
    if (!carouselStart) carouselStart = now;
    text(track, carouselItems[carouselIndex]);
    var viewport = node('clock-weather'), overflow = Math.max(0, track.scrollWidth - viewport.clientWidth);
    var distance = overflow > 0 ? overflow + 24 : 0, scrollMs = distance / 40 * 1000;
    var hold = Math.max(3000, 2000 + scrollMs), elapsed = now - carouselStart, phase = 0;
    var shift = -distance * Math.max(0, Math.min(1, (elapsed - 1000) / (scrollMs || 1))), opacity = 1;
    if (carouselItems.length > 1 && elapsed >= hold) {
      if (elapsed < hold + 200) { phase = (elapsed-hold)/200; opacity = 1-phase; }
      else if (elapsed < hold + 400) { text(track, carouselItems[(carouselIndex+1)%carouselItems.length]); shift = 0; phase = -(1-(elapsed-hold-200)/200); opacity = 1+phase; }
      else { carouselIndex = (carouselIndex+1)%carouselItems.length; carouselStart = now; shift = 0; text(track, carouselItems[carouselIndex]); }
    } else if (carouselItems.length === 1 && elapsed >= hold) carouselStart = now;
    var transform = 'translateX('+shift+'px)', type = prefs['weather-transition'];
    if (type === 'slide_up') transform += ' translateY('+(-phase*0.6)+'em)';
    if (type === 'slide_down') transform += ' translateY('+(phase*0.6)+'em)';
    if (type === 'slide_right') transform += ' translateX('+(phase*viewport.clientWidth)+'px)';
    if (type === 'scale') transform += ' scale('+(1-Math.abs(phase)*0.12)+')';
    if (type === 'flip') transform += ' scaleY('+Math.max(0.05, 1-Math.abs(phase))+')';
    track.style.transform = transform; track.style.opacity = String(opacity);
  }
  function render() {
    win.clearTimeout(tickTimer);
    var now = Date.now()+offset, date = clockDate(now), hour = date.getHours(), second = date.getSeconds(), current = hour*60+date.getMinutes();
    var twelve = prefs['hour-format'] === '12', displayHour = twelve ? hour%12 || 12 : hour;
    var colon = prefs['blink-colon'] && second%2 ? '\u00a0' : ':';
    var stacked = (prefs.stacked && win.innerHeight >= win.innerWidth) || win.innerWidth < win.innerHeight * .4;
    if (stacked) colon = '\n';
    var value = pad(displayHour)+colon+pad(date.getMinutes());
    if (prefs['show-seconds'] && !prefs['small-seconds']) value += colon+pad(second);
    var main = node('clock-main'), changed = main.textContent !== value;
    if (stacked) main.style.color = prefs['auto-ink'] && prefs.theme !== 'classic' ? ink(color(prefs['panel-color'],themes[prefs.theme][1])) : '';
    var grouped = !stacked && (prefs.theme === 'ultimate.dual_blocks' || prefs.theme === 'ultimate.bubbles' || prefs.theme === 'ultimate.blend');
    var groupKey=prefs.theme+'|'+prefs['panel-color']+'|'+prefs['accent-color']+'|'+prefs['auto-ink']+'|'+prefs['time-color']+'|'+value;
    if (grouped && main.getAttribute('data-groups')!==groupKey) {
      main.setAttribute('data-groups',groupKey);
      main.className = 'material-main';
      while(main.firstChild)main.removeChild(main.firstChild);
      var values=[pad(displayHour),pad(date.getMinutes())], group, index, automatic=prefs['auto-ink'];
      if(prefs['show-seconds'] && !prefs['small-seconds'])values.push(pad(second));
      for(index=0;index<values.length;index++) {
        if(index) {var separator=doc.createElement('span');separator.className='material-separator';text(separator,colon);main.appendChild(separator);}
        group=doc.createElement('span');group.className='material-part material-'+(index===0?'hours':index===1?'minutes':'seconds');text(group,values[index]);
        var fill=index===1?color(prefs['accent-color'],themes[prefs.theme][2]):color(prefs['background-color'],themes[prefs.theme][0]);
        group.style.backgroundColor=fill;group.style.color=automatic?ink(fill):color(prefs['time-color'],'#ffffff');main.appendChild(group);
      }
    } else if(!grouped) {
      if(main.getAttribute('data-groups')!==null) {while(main.firstChild)main.removeChild(main.firstChild);main.className='';}
      main.removeAttribute('data-groups');text(main,value);
    }
    text(node('clock-small-seconds'), prefs['show-seconds'] && prefs['small-seconds'] ? pad(second) : '');
    text(node('clock-period'), twelve ? (prefs.language === 'en' ? hour<12?'AM':'PM' : hour<12?'上午':'下午') : '');
    var lunar=lunarText(date), separate=prefs['dual-line'] || win.innerHeight >= win.innerWidth;
    text(node('clock-date'), dateText(date)+(lunar && !separate?' '+lunar:'')); text(node('clock-lunar'), separate?lunar:'');
    show(node('clock-lunar'), prefs.lunar && separate); node('clock-time').className = 'clock-time' + (stacked ? ' is-stacked' : '');
    if (changed && prefs.animate) {
      main.className = (grouped?'material-main ':'')+'digit-change transition-'+prefs.transition;
      win.setTimeout(function () { main.className = grouped?'material-main':''; },300);
    }
    var dim = prefs['use-image'] && (prefs.dim || prefs['schedule-dim'] && within(current,minutes(prefs['dim-start']),minutes(prefs['dim-end'])));
    show(node('clock-dim'), dim);
    supportingItems(); fit();
    if (prefs['hourly-chime'] && date.getMinutes() === 0 && second === 0 && lastChime !== date.toString() &&
        !(prefs['chime-quiet'] && within(current,minutes(prefs['chime-start']),minutes(prefs['chime-end'])))) {
      lastChime = date.toString(); node('clock-face').className = 'clock-face is-chiming';
      win.setTimeout(function () { node('clock-face').className = 'clock-face'; },1200);
    }
    tickTimer = win.setTimeout(render,1000-((Date.now()+offset)%1000));
  }
  var layoutSignature = '';
  function fit() {
    var width = node('clock-face').clientWidth || win.innerWidth || 800;
    var height = node('clock-face').clientHeight || win.innerHeight || 600;
    var date = node('clock-date'), lunar = node('clock-lunar'), extra = node('clock-extra');
    var signature = [width,height,prefs.theme,prefs['time-scale'],prefs['date-scale'],prefs['supporting-scale'],
      prefs.font,prefs.bold,prefs['show-seconds'],prefs['small-seconds'],prefs['hour-format'],prefs.stacked,
      date.textContent,lunar.textContent,(lunar.style.display === 'none'),(extra.style.display === 'none')].join('|');
    if (signature === layoutSignature) return;
    layoutSignature = signature;
    var lines = node('clock-lines'), time = node('clock-time');
    var compact = width < height*.4 || width > height*2.8 || Math.min(width,height) < 240;
    var orbit = prefs.theme === 'ultimate.orbit' && !compact;
    var diameter = Math.min(width,height)*.8;
    var padding = orbit ? diameter*.12 : Math.min(24,Math.min(width,height)*.03);
    lines.style.width = orbit ? diameter+'px' : '96%';
    lines.style.height = orbit ? diameter+'px' : '';
    lines.style.padding = padding+'px';
    lines.style.borderRadius = compact ? Math.min(width,height)*.06+'px' : '';
    var available = Math.max(1,(lines.clientWidth || width*.96)-padding*2-6);
    var usableHeight = Math.max(1,(orbit ? diameter : height*.92)-padding*2-6);
    var stacked = time.className.indexOf('is-stacked') >= 0;
    var count = stacked ? (prefs['show-seconds'] && !prefs['small-seconds'] ? 3 : 2) : 1;
    var size = Math.min(available*finite(prefs['time-scale'],88,20,150)/100/(stacked?1.4:(prefs['show-seconds']?5.4:3.6)),
      usableHeight*.62/count);
    time.style.fontSize = size+'px';
    time.style.margin = '0';
    var primaryHeight = time.offsetHeight || size*count*1.2;
    var shrink = Math.min(1,available/Math.max(available,time.scrollWidth),usableHeight*.74/Math.max(1,primaryHeight));
    size *= shrink; primaryHeight *= shrink; time.style.fontSize = size+'px';
    var dateSize = Math.min(available*finite(prefs['date-scale'],55,20,200)/100/18,usableHeight*.085);
    date.style.fontSize = dateSize+'px'; lunar.style.fontSize = dateSize+'px';
    // Fit long date formats before allocating remaining vertical space.
    dateSize *= Math.min(1,available/Math.max(available,date.scrollWidth,(lunar.style.display === 'none')?0:lunar.scrollWidth));
    var supportSize = dateSize*finite(prefs['supporting-scale'],100,50,200)/100;
    var dateRows = (lunar.style.display === 'none') ? 1 : 2, supportRows = (extra.style.display === 'none') ? 0 : 1;
    var gap = Math.min(usableHeight*.025,dateSize*.6), rowGap = Math.min(usableHeight*.015,dateSize*.35);
    var requested = (dateSize*dateRows+supportSize*supportRows)*1.2+gap*2+rowGap*(dateRows-1);
    var secondaryScale = Math.min(1,Math.max(0,usableHeight-primaryHeight)/Math.max(1,requested));
    date.style.fontSize = dateSize*secondaryScale+'px'; lunar.style.fontSize = dateSize*secondaryScale+'px';
    extra.style.fontSize = supportSize*secondaryScale+'px';
    time.style.margin = gap*secondaryScale+'px 0';
    lunar.style.marginTop = rowGap*secondaryScale+'px';
  }
  function safeUrl(value) {
    var anchor = doc.createElement('a'); anchor.href = value;
    return anchor.protocol === 'https:' || anchor.protocol === 'http:' ? anchor.href : '';
  }
  function fetchWeather() {
    if (!prefs.weather || !prefs['weather-url']) return;
    var url = prefs['weather-url'].replace(/\{location\}/g,encodeURIComponent(prefs['weather-location']));
    if (url.indexOf('location=') < 0) url += (url.indexOf('?')<0?'?':'&')+'location='+encodeURIComponent(prefs['weather-location']);
    url = safeUrl(url); if (!url) { status('Invalid weather URL'); return; }
    var token = generation, xhr = new win.XMLHttpRequest(); request = xhr;
    xhr.open('GET',url,true); xhr.timeout = 15000;
    xhr.onprogress=function(event){if(event.loaded>1048576)xhr.abort();};
    xhr.onreadystatechange = function () {
      if (xhr.readyState !== 4 || token !== generation) return;
      if (xhr.status >= 200 && xhr.status < 300 && xhr.responseText.length <= 1048576) {
        try { var body = JSON.parse(xhr.responseText); weather = body.data || body.now || (body.results && body.results[0] && body.results[0].now) || body; write('weather-cache',JSON.stringify(weather)); status(''); supportingItems(); }
        catch (ignore) { status('Weather response unavailable'); }
      } else status('Weather unavailable');
    };
    xhr.onerror = xhr.ontimeout = function () { if (token === generation) status('Weather unavailable'); };
    xhr.send(null);
  }
  function syncTime() {
    if (!prefs['network-time']) { offset = 0; return; }
    var url = safeUrl(prefs['time-url'] || win.location.href); if (!url) return;
    var token = generation, start = Date.now(), xhr = new win.XMLHttpRequest();
    xhr.open('HEAD',url,true); xhr.timeout = 10000;
    xhr.onreadystatechange = function () {
      if (xhr.readyState !== 4 || token !== generation || !prefs['network-time']) return;
      if (xhr.status >= 200 && xhr.status < 400) {
        var server = Date.parse(xhr.getResponseHeader('Date')), end = Date.now();
        if (isFinite(server)) offset = server+500-(start+end)/2;
      }
    }; xhr.send(null);
  }
  function configure() {
    layoutSignature = '';
    generation++; if (request) request.abort();
    win.clearInterval(weatherTimer); win.clearInterval(syncTimer);
    var theme = own(themes,prefs.theme) ? prefs.theme : 'classic', colors = themes[theme];
    var lines = node('clock-lines'); lines.className = 'clock-lines theme-'+theme.replace('ultimate.','')+(prefs['card-shadow']?' has-shadow':'');
    var background = color(prefs['background-color'], colors[0]), panel = color(prefs['panel-color'],colors[1]), accent = color(prefs['accent-color'],colors[2]);
    lines.style.backgroundColor = theme === 'classic' ? 'transparent' : panel;
    lines.style.borderColor = accent;
    node('clock-background').style.backgroundColor = background;
    node('clock-background').style.backgroundImage = prefs['use-image'] && /^data:image\//.test(image) ? 'url("'+image.replace(/"/g,'')+'")' : 'none';
    var automatic = theme !== 'classic' && prefs['auto-ink'];
    node('clock-time').style.color = automatic ? ink(panel) : color(prefs['time-color'],'#ffffff');
    node('clock-date').style.color = node('clock-lunar').style.color = node('clock-extra').style.color = automatic ? ink(panel) : color(prefs['date-color'],'#ffffff');
    var fonts = { system: 'Segoe UI, Microsoft YaHei, sans-serif', roboto: 'Roboto, Arial, sans-serif', segoe: 'Segoe UI, Microsoft YaHei, sans-serif', serif: 'Georgia, Times New Roman, serif', mono: 'Consolas, Courier New, monospace', inter: 'Arial, sans-serif', lora: 'Georgia, serif', bitcount_grid_double: 'Consolas, monospace' };
    lines.style.fontFamily = fonts[prefs.font] || fonts.system; lines.style.fontWeight = prefs.bold?'700':'400';
    node('clock-main').style.backgroundColor = 'transparent';
    node('clock-main').style.color = automatic && (theme === 'ultimate.dual_blocks' || theme === 'ultimate.bubbles') ? ink(accent) : '';
    node('clock-time').style.borderColor = accent;
    doc.documentElement.lang = prefs.language;
    if (prefs.weather) { fetchWeather(); weatherTimer = win.setInterval(fetchWeather,finite(prefs['weather-interval'],30,5,1440)*60000); }
    if (prefs['network-time']) { syncTime(); syncTimer = win.setInterval(syncTime,finite(prefs['sync-interval'],60,1,1440)*60000); } else offset=0;
    supportingItems();
  }
  node('settings-button').onclick = open; node('clock-face').ondblclick = open;
  node('clock-face').onkeydown = function (event) { if ((event || win.event).keyCode === 13) open(); };
  node('settings-close').onclick = node('settings-cancel').onclick = close;
  node('settings-apply').onclick = apply;
  node('settings-reset').onclick = function () { fill(defaults); draftImage = ''; };
  node('pref-time-scale').oninput = node('pref-date-scale').oninput = updateRange;
  node('pref-auto-ink').onchange = updateInkControls;
  node('pref-language').onchange = function () {
    var input=node('pref-date-pattern');
    if(input.value==='yyyy年M月d日 EEEE' || input.value==='yyyy/MM/dd EEEE')input.value=this.value==='en'?'yyyy/MM/dd EEEE':'yyyy年M月d日 EEEE';
  };
  node('clear-background').onclick=function(){draftImage='';node('pref-use-image').checked=false;};
  node('pref-theme').onchange = function () {
    var colors = themes[this.value] || themes.classic;
    node('pref-background-color').value = colors[0]; node('pref-panel-color').value = colors[1]; node('pref-accent-color').value = colors[2];
    updateInkControls();
  };
  node('pref-background-file').onchange = function () {
    var file = this.files && this.files[0]; if (!file) return;
    if (file.size > 16*1024*1024 || !/^image\//.test(file.type)) { status('Choose an image smaller than 16 MB'); return; }
    var reader = new win.FileReader(); reader.onload = function () { draftImage = String(reader.result); node('pref-use-image').checked = true; }; reader.readAsDataURL(file);
  };
  node('fullscreen-button').onclick = function () {
    var element = doc.documentElement, enter = element.requestFullscreen || element.msRequestFullscreen || element.webkitRequestFullscreen;
    var exit = doc.exitFullscreen || doc.msExitFullscreen || doc.webkitExitFullscreen;
    try { if (doc.fullscreenElement || doc.msFullscreenElement || doc.webkitFullscreenElement) { if (exit) exit.call(doc); } else if (enter) enter.call(element); } catch (ignore) { status('Fullscreen unavailable'); }
  };
  doc.onkeydown = function (event) {
    event = event || win.event;
    if (event.keyCode === 27) close();
    if (event.keyCode === 9 && node('settings-overlay').style.display !== 'none') {
      var controls = node('settings-panel').querySelectorAll('button, input, select'), first = controls[0], last = controls[controls.length-1];
      if (event.shiftKey && doc.activeElement === first) { last.focus(); event.preventDefault(); }
      else if (!event.shiftKey && doc.activeElement === last) { first.focus(); event.preventDefault(); }
    }
  };
  track = doc.createElement('span'); track.className = 'carousel-track'; node('clock-weather').appendChild(track);
  win.onresize = render;
  show(node('clock-message'),false); show(node('settings-overlay'),false);
  load(); configure(); render(); win.setInterval(function () { carousel(Date.now()); },40);
}());
