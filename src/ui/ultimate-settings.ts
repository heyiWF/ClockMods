import { cityName } from '../core/cities';
import {secondaryFontSize,secondarySizeValue,saveSecondarySize} from './secondary-font-size';
import { CALENDAR_THEMES } from '../core/calendar-themes';
import { fontWeightControl } from './font-weight';
import { prefs, ULTIMATE_DEFAULTS, CALENDAR_IDS, type UltimateOptions } from '../core/prefs';
import { fontPicker, setFontSelection } from './font-picker';
import { ZONE_IDS } from '../core/timezone';
import { card, element, select, sliderRow, subLabel, summaryLabel, switchRow, setTreeEnabled } from './controls';
export function ultimateSettings() {
    const en = prefs.getClockLanguage() === 'en';
    const L = (cn: string, english: string) => en ? english : cn;
    let draft = prefs.getUltimateOptions();
    let hourlyChime = prefs.isHourlyChimeEnabled();
    const controls = new Map<keyof UltimateOptions, HTMLInputElement | HTMLSelectElement>();
    function toggle(key: keyof UltimateOptions, label: string, summary?: string) {
        const row = switchRow(label, Boolean(draft[key]), summary);
        controls.set(key, row.input);
        return row.row;
    }
    function choice(key: keyof UltimateOptions, label: string, values: Array<[
        string | number,
        string
    ]>) {
        const input = select(values.map(([value, label]) => ({ value: String(value), label })), String(draft[key]));
        input.setAttribute('aria-label', label);
        controls.set(key, input);
        const row = element('div', 'settings-group');
        row.append(subLabel(label), input);
        return row;
    }
    function slider(key: keyof UltimateOptions, label: string, min: number, max: number, unit: string) {
        const row = sliderRow(label, min, max, Number(draft[key]), v => v + unit);
        controls.set(key, row.input);
        return row.row;
    }
    const seconds = card(L('秒针', 'Second hand'), choice('secondMotion', L('秒针模式', 'Second-hand motion'), [['tick', L('跳秒', 'Tick')], ['smooth', L('平滑', 'Smooth')], ['off', L('关闭秒针', 'Hide hand')]]));
    const display = card(L('设备显示', 'Device display'), toggle('avoidCutout', L('避让刘海与屏幕边缘', 'Respect display safe area')), toggle('statusIcons', L('显示设备状态', 'Device status'), L('显示浏览器可提供的网络连接及电池状态。', 'Shows network connectivity and battery when available.')), slider('statusScale', L('状态图标大小', 'Status size'), 50, 200, '%'), choice('statusStyle', L('状态图标样式', 'Status style'), [['outline', L('描边', 'Outline')], ['filled', L('填充', 'Filled')], ['minimal', L('简约', 'Minimal')]]));
    const worldEnabled = toggle('worldEnabled', L('启用世界时钟', 'Enable world clocks'));
    const worldList = element('div', 'world-clock-list');
    const intl = Intl as typeof Intl & {
        supportedValuesOf?: (key: string) => string[];
    };
    const catalog = [...new Set([...(intl.supportedValuesOf?.('timeZone') ?? ZONE_IDS.filter(id => id.includes('/'))), ...draft.worldZones])];
    const search = element('input', 'settings-input');
    search.type = 'search';
    search.placeholder = L('搜索城市或时区', 'Search cities or time zones');
    search.setAttribute('aria-label', search.placeholder);
    const zone = select([], '');
    zone.setAttribute('aria-label', L('添加时区', 'Add time zone'));
    function filter() { const query = search.value.trim().toLowerCase(); zone.replaceChildren(...catalog.filter(id => !draft.worldZones.includes(id) && (id+' '+cityName(id,prefs.getClockLanguage())).toLowerCase().includes(query)).map(id => { const option = element('option', undefined, cityName(id,prefs.getClockLanguage())+' · '+id); option.value = id; return option; })); }
    const add = element('button', 'm3-button m3-button--outlined', L('添加城市', 'Add city'));
    add.type = 'button';
    const paintWorld = () => {
        worldList.replaceChildren(...draft.worldZones.map((id, index) => {
            const row = element('div', 'world-clock-row');
            row.append(element('span', undefined, cityName(id,prefs.getClockLanguage())+' · '+id));
            for (const [text, offset] of [['↑', -1], ['↓', 1], ['×', 0]] as const) {
                const button = element('button', 'm3-button m3-button--text', text);
                button.type = 'button';
                button.setAttribute('aria-label', (offset === 0 ? L('删除 ', 'Remove ') : offset < 0 ? L('上移 ', 'Move up ') : L('下移 ', 'Move down ')) + id);
                button.disabled = offset < 0 && index === 0 || offset > 0 && index === draft.worldZones.length - 1;
                button.addEventListener('click', () => { if (offset === 0)
                    draft.worldZones.splice(index, 1);
                else
                    [draft.worldZones[index], draft.worldZones[index + offset]] = [draft.worldZones[index + offset], draft.worldZones[index]]; paintWorld(); });
                row.append(button);
            }
            return row;
        }));
        filter();
        add.disabled = draft.worldZones.length >= 6 || !zone.options.length;
    };
    add.addEventListener('click', () => { if (zone.value && draft.worldZones.length < 6)
        draft.worldZones.push(zone.value); paintWorld(); });
    search.addEventListener('input', () => { filter(); add.disabled = draft.worldZones.length >= 6 || !zone.options.length; });
    const world = card(L('世界时钟', 'World clocks'), worldEnabled, summaryLabel(L('最多六座城市，可调整顺序。', 'Up to six cities, in your chosen order.')), worldList, search, zone, add);
    paintWorld();
    const chime = card(L('报时效果', 'Chime effects'), toggle('halfHourChime', L('半点报时', 'Half-hour chime')), choice('chimeAnimation', L('报时动画', 'Chime animation'), [['radial', L('扩散', 'Radial')], ['ripple', L('涟漪', 'Ripple')], ['pulse', L('脉冲', 'Pulse')], ['aurora', L('极光', 'Aurora')], ['orbit', L('轨道', 'Orbit')], ['comet', L('彗星', 'Comet')]]));
    const protection = element('div', 'settings-group');
    protection.append(choice('burnInterval', L('移动间隔', 'Shift interval'), [1, 10, 30, 60].map(v => [v, v + L(' 分钟', ' min')])), slider('burnAmplitude', L('移动幅度', 'Shift distance'), 0, 12, 'px'), toggle('burnDim', L('保护时降低亮度', 'Dim during protection')));
    const system = card(L('显示保护', 'Display protection'), toggle('burnIn', L('防烧屏位移', 'Burn-in protection')), protection, summaryLabel(L('浏览器不能设置 Android 开机自启；可将网页安装到主屏幕。方向锁定由设备和浏览器决定。', 'Android autostart is unavailable in a browser. Install this app to your home screen. Orientation locking depends on your browser and device.')));
    const calendarTheme = choice('calendarTheme', L('日历主题', 'Calendar theme'), CALENDAR_IDS.map((id, i) => [id, [L('石墨', 'Graphite'), L('纯黑', 'Carbon'), L('纸历', 'Paper'), L('海报', 'Poster'), L('周程', 'Agenda')][i]]));
    const calendarGallery = element('div', 'theme-gallery');
    for (const theme of CALENDAR_THEMES) {
        const button = element('button', 'theme-choice');
        button.type = 'button';
        button.dataset.calendarChoice = theme.id;
        const preview = element('div', 'calendar-theme-preview');
        preview.style.background = theme.bg;
        preview.style.color = theme.text;
        const header = element('strong', undefined, theme.id === 'calendar.poster' ? 'OCTOBER' : '2026 / 10');
        preview.append(header);
        const grid = element('div', 'calendar-preview-grid');
        grid.style.color = theme.accent;
        for (let day = 1; day <= (theme.id === 'calendar.agenda' ? 7 : 28); day++)
            grid.append(element('span', undefined, String(day)));
        preview.append(grid);
        if (theme.id === 'calendar.agenda')
            preview.append(element('strong', undefined, '08'));
        if (theme.id === 'calendar.graphite' || theme.id === 'calendar.carbon')
            preview.classList.add('has-clock');
        if (preview.classList.contains('has-clock'))
            preview.prepend(element('strong', 'calendar-preview-time', '12:08'));
        const option = (controls.get('calendarTheme') as HTMLSelectElement).querySelector<HTMLOptionElement>('option[value="' + theme.id + '"]')!;
        button.append(preview, element('span', 'theme-choice-name', option.textContent!));
        button.addEventListener('click', () => { const input = controls.get('calendarTheme')!; input.value = theme.id; input.dispatchEvent(new Event('change')); });
        calendarGallery.append(button);
    }
    const calPicker = fontPicker(prefs.getFontFamily(draft.calendarTheme), prefs.isClockUseEnglish());
    const calFont = calPicker.input;
    calFont.setAttribute('aria-label', L('日历字体', 'Calendar font'));
    const calWeight = fontWeightControl(L('字重', 'Font weight'), calFont.value, prefs.getFontWeight(draft.calendarTheme));
    calFont.addEventListener('change', () => calWeight.set(calFont.value, calWeight.value()));
    const calTime = sliderRow(L('时间字号', 'Time size'), 20, 150, prefs.getTimeFontScale(draft.calendarTheme) * 100, v => v + '%');
    const calDate = secondaryFontSize(L('日期字号','Date size'),'date',draft.calendarTheme);
    const calSupport = secondaryFontSize(L('辅助文字字号','Supporting text size'),'supporting',draft.calendarTheme);
    let activeCalendar = draft.calendarTheme;
    type TypeDraft = {
        font: string;
        weight: number;
        time: number;
        date: number;
        support: number;
    };
    const typography = new Map<string, TypeDraft>();
    const captureCalendar = () => typography.set(activeCalendar, { font: calFont.value, weight: calWeight.value(), time: +calTime.input.value / 100, date: calDate.value(), support: calSupport.value() });
    const loadCalendar = () => { const v = typography.get(activeCalendar) ?? { font: prefs.getFontFamily(activeCalendar), weight: prefs.getFontWeight(activeCalendar), time: prefs.getTimeFontScale(activeCalendar), date:secondarySizeValue('date',activeCalendar),support:secondarySizeValue('supporting',activeCalendar) }; setFontSelection(calFont, v.font); calWeight.set(v.font, v.weight); calDate.setTheme(activeCalendar,v.date);calSupport.setTheme(activeCalendar,v.support); for (const [r, value] of [[calTime, v.time]] as const) {
        r.input.value = String(value * 100);
        r.input.dispatchEvent(new Event('input'));
    } };
    controls.get('calendarTheme')!.addEventListener('change', () => { captureCalendar(); activeCalendar = controls.get('calendarTheme')!.value; loadCalendar(); });
    const calendar = card(L('日历样式', 'Calendar style'), calendarGallery, calendarTheme, subLabel(L('字体', 'Font')), calPicker.row, calWeight.row, calTime.row, calDate.row, calSupport.row, choice('marqueeSpeed', L('滚动速度', 'Marquee speed'), [[20, L('慢', 'Slow')], [40, L('标准', 'Normal')], [80, L('快', 'Fast')]]), choice('marqueePause', L('滚动停顿', 'Marquee pause'), [[0, '0 s'], [1000, '1 s'], [2000, '2 s']]), choice('marqueeGap', L('滚动间距', 'Marquee gap'), [[12, '12 px'], [24, '24 px'], [48, '48 px']]));
    function syncCalendar() { for (const button of calendarGallery.querySelectorAll('button'))
        button.setAttribute('aria-pressed', String((button as HTMLElement).dataset.calendarChoice === activeCalendar)); calTime.row.hidden = !['calendar.graphite', 'calendar.carbon'].includes(activeCalendar); calSupport.row.hidden = activeCalendar === 'calendar.poster'; for (const key of ['marqueeSpeed', 'marqueePause', 'marqueeGap'] as const)
        controls.get(key)!.parentElement!.hidden = activeCalendar === 'calendar.poster'; }
    controls.get('calendarTheme')!.addEventListener('change', syncCalendar);
    syncCalendar();
    function sync() { controls.get('chimeAnimation')!.disabled = !hourlyChime && !(controls.get('halfHourChime') as HTMLInputElement).checked; setTreeEnabled(protection, (controls.get('burnIn') as HTMLInputElement).checked); for (const k of ['statusScale', 'statusStyle'] as const)
        controls.get(k)!.disabled = !(controls.get('statusIcons') as HTMLInputElement).checked; }
    for (const key of ['statusIcons', 'burnIn', 'halfHourChime'] as const)
        controls.get(key)!.addEventListener('change', sync);
    sync();
    return { seconds, display, world, chime, system, calendar,
        previewSettings(): Partial<typeof prefs> {
            const options = {...draft, worldZones:[...draft.worldZones]};
            for(const [key,input] of controls) (options as unknown as Record<string,unknown>)[key] = input instanceof HTMLInputElement && input.type==='checkbox' ? input.checked : typeof ULTIMATE_DEFAULTS[key]==='number' ? Number(input.value) : input.value;
            return {getUltimateOptions:()=>options, getFontFamily:()=>calFont.value,getFontWeight:()=>calWeight.value(),isBoldText:()=>calWeight.value()>=700,getTimeFontScale:()=>+calTime.input.value/100,getDateFontSize:()=>calDate.value(),getSupportingFontSize:()=>calSupport.value()};
        },
        setHourlyChime(enabled: boolean) { hourlyChime = enabled; sync(); },
        changeTheme(id: string) { world.hidden = !id.startsWith('ultimate.'); seconds.hidden = !['glass.atelier', 'noir.instrument', 'paper.station', 'orbit.neon', 'ultimate.orbit', 'ultimate.blend'].includes(id); },
        apply() { captureCalendar(); for (const [key, input] of controls) {
            const value = input instanceof HTMLInputElement && input.type === 'checkbox' ? input.checked : typeof ULTIMATE_DEFAULTS[key] === 'number' ? Number(input.value) : input.value;
            (draft as unknown as Record<string, unknown>)[key] = value;
        } prefs.setUltimateOptions(draft); for (const [id, v] of typography) {
            prefs.setFontFamily(v.font, id);
            prefs.setBoldText(v.weight >= 700, id);
            prefs.setFontWeight(v.weight, id);
            prefs.setTimeFontScale(v.time, id);
            saveSecondarySize('date',id,v.date);
            saveSecondarySize('supporting',id,v.support);
        } },
        reset() { draft = { ...ULTIMATE_DEFAULTS, worldZones: [...ULTIMATE_DEFAULTS.worldZones] }; for (const [key, input] of controls) {
            if (input instanceof HTMLInputElement && input.type === 'checkbox')
                input.checked = Boolean(draft[key]);
            else
                input.value = String(draft[key]);
            input.dispatchEvent(new Event('input'));
        } typography.clear(); for (const id of CALENDAR_IDS)
            typography.set(id, { font: 'system', weight: 400, time: .88, date:secondarySizeValue('date',id,true),support:secondarySizeValue('supporting',id,true) }); activeCalendar = draft.calendarTheme; loadCalendar(); syncCalendar(); paintWorld(); sync(); },
    };
}
