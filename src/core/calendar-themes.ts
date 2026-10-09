import { prefs } from './prefs';
/** Palette values from Ultimate CalendarTheme; layout kind is independent of colour. */
export const CALENDAR_THEMES = [
    { id: 'calendar.graphite', bg: '#171918', end: '#171918', panel: '#363836', text: '#f2f3f2', secondary: '#d0d2d0', accent: '#2693ff', today: '#16e13b', weekend: '#ff9b9b', selection: '#33404b', radius: 10 },
    { id: 'calendar.carbon', bg: '#000000', end: '#000000', panel: '#191a19', text: '#e8e9e8', secondary: '#9a9d9a', accent: '#2693ff', today: '#16e13b', weekend: '#ff9b9b', selection: '#262a2e', radius: 10 },
    { id: 'calendar.paper', bg: '#f5f1e6', end: '#ebe4d3', panel: '#fcfaf4', text: '#262420', secondary: '#7c7466', accent: '#8a6034', today: '#b5392a', weekend: '#a8492f', selection: '#efe6d2', radius: 8 },
    { id: 'calendar.poster', bg: '#fafaf8', end: '#f2f2ee', panel: 'transparent', text: '#16181a', secondary: '#9aa0a6', accent: '#c8362f', today: '#c8362f', weekend: '#9ba1a6', selection: 'transparent', radius: 0 },
    { id: 'calendar.agenda', bg: '#f5f6fb', end: '#e7eaf6', panel: '#ffffff', text: '#1b1f3b', secondary: '#6e748f', accent: '#3b4a9e', today: '#3b4a9e', weekend: '#c24b57', selection: '#e7eaf9', radius: 18 },
];
export function applyCalendarTheme(root: HTMLElement, settings: typeof prefs = prefs): void {
    const id = settings.getUltimateOptions().calendarTheme;
    const theme = CALENDAR_THEMES.find(t => t.id === id)!;
    root.dataset.calendarTheme = id;
    root.style.background = 'linear-gradient(' + theme.bg + ',' + theme.end + ')';
    const vars: Record<string, string> = { '--text': theme.text, '--secondary': theme.secondary, '--blue': theme.accent, '--accent': theme.accent, '--green': theme.today, '--weekend': theme.weekend, '--cal-day': theme.text, '--md-sys-color-surface-container': theme.panel, '--md-sys-color-on-surface': theme.text, '--md-sys-color-on-surface-variant': theme.secondary, '--md-sys-color-secondary-container': theme.selection, '--md-sys-shape-corner-large': theme.radius + 'px', '--cal-date-size':settings.getDateFontSize(id)+'px','--cal-support-size':settings.getSupportingFontSize(id)+'px', '--cal-time-scale': String(settings.getTimeFontScale(id) / .88), '--cal-weight': String(settings.getFontWeight(id)) };
    for (const [key, value] of Object.entries(vars))
        root.style.setProperty(key, value);
}
