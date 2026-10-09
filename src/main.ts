import { installIdleCursor } from './ui/idle-cursor';
import { DisplaySettings } from './core/display-settings';
/**
 * Application entry point.
 *
 * Replaces ProMainActivity's onCreate: apply the stored orientation, keep the
 * screen awake, load the offline data sets, wire the pages into the router and
 * restore the page that was showing last.
 */
import './styles/tokens.css';
import './styles/fonts.css';
import './styles/material.css';
import './styles/base.css';
import './styles/clock.css';
import './styles/calendar.css';
import './styles/timers.css';
import './styles/settings.css';

import { prefs } from './core/prefs';
import { refreshLanguage, t } from './core/i18n';
import { ensureFontLoaded } from './core/fonts';
import { applyScreenOrientation } from './core/orientation';
import { installWakeLockHandlers, requestWakeLock } from './core/wake-lock';
import { timeSource } from './core/time-source';
import { loadHolidays } from './lunar/holidays';
import { loadWeatherIcons } from './weather/icons';
import { Router } from './app/router';
import { openSettings } from './app/settings-panel';
import { ClockPage } from './pages/clock';
import { CalendarPage } from './pages/calendar';
import { TimerPage } from './pages/timer';
import { StopwatchPage } from './pages/stopwatch';
import { AlarmPage } from './pages/alarm';
import { HourlyChime } from './ui/chime';
import { toast } from './ui/toast';
import { installRipples } from './ui/ripple';

async function boot(): Promise<void> {
  document.documentElement.lang = document.documentElement.lang || 'zh-Hans';
  refreshLanguage();

  await Promise.all([
    ensureFontLoaded(prefs.getFontFamily(), prefs.getFontWeight()),
    loadWeatherIcons(),
    loadHolidays(),
  ]);

  refreshShellLanguage();
  timeSource.configure();
  void applyScreenOrientation();
  installWakeLockHandlers();
  // One delegated listener gives every M3 surface a press ripple, including the
  // ones the pages build later.
  installRipples();
  installIdleCursor();

  const container = document.getElementById('pages')!;
  const nav = document.getElementById('nav')!;
  const router = new Router(container, nav);

  const displaySettings = new DisplaySettings();
  const showSettings = () => {
    openSettings(async (languageChanged) => {
      timeSource.configure();
      displaySettings.refresh();
      const orientationApplied = await applyScreenOrientation();
      if (languageChanged) { refreshLanguage(); refreshShellLanguage(); }
      await router.refreshAll();
      toast(t(orientationApplied ? 'settings_applied' : 'orientation_lock_unsupported'));
    });
  };

  const clock = new ClockPage(page('clock'), showSettings);
  const calendar = new CalendarPage(page('calendar'), showSettings);
  const pomodoro = new TimerPage(page('pomodoro'), 'pomodoro');
  const alarm = new AlarmPage(page('alarm'));
  const countdown = new TimerPage(page('countdown'), 'countdown');
  const stopwatch = new StopwatchPage(page('stopwatch'));

  // Registration order defines the swipe order and matches the ProPage enum.
  router.register(clock);
  router.register(calendar);
  router.register(pomodoro);
  router.register(alarm);
  router.register(countdown);
  router.register(stopwatch);

  await router.refreshAll();
  router.navigate(prefs.getLastPage() || 'clock');

  // The hourly chime overlays every page, exactly as the Android view did.
  const chime = new HourlyChime(document.getElementById('chime-layer')!);
  chime.start();



  // A wake lock needs a user gesture on some browsers, so ask again on first tap.
  void requestWakeLock();
  document.addEventListener('pointerdown', () => void requestWakeLock(), { once: true });

  registerServiceWorker();
}

function refreshShellLanguage():void {
 document.getElementById('nav')?.setAttribute('aria-label',prefs.isClockUseEnglish()?'Navigation':prefs.getClockLanguage()==='zh-Hant'?'功能導覽':'功能导航');
 for(const button of document.querySelectorAll<HTMLElement>('[data-nav]')) { const text=t('pro_page_'+button.dataset.nav);const label=button.querySelector('.m3-nav-item__label');if(label)label.textContent=text;button.setAttribute('aria-label',text); }
 for(const page of document.querySelectorAll<HTMLElement>('.page'))page.setAttribute('aria-label',t('pro_page_'+page.dataset.page));
 for(const button of document.querySelectorAll<HTMLElement>('[data-open-settings],.settings-fab'))button.setAttribute('aria-label',t('open_settings_accessibility'));
}

function page(name: string): HTMLElement {
  const element = document.querySelector<HTMLElement>(`.page[data-page='${name}']`);
  if (!element) throw new Error(`missing page container: ${name}`);
  return element;
}

function registerServiceWorker(): void {
  if (!import.meta.env.PROD) return;
  void import('virtual:pwa-register').then(({ registerSW }) => {
    const updateSW = registerSW({
      onNeedRefresh() {
        const root = document.getElementById('toast-root');
        if (!root) return;
        if (document.getElementById('pwa-update-banner')) return;
        const banner = document.createElement('div');
        banner.id = 'pwa-update-banner';
        banner.className = 'toast';
        banner.style.pointerEvents = 'auto';
        banner.textContent = `${t('update_available')} `;
        const button = document.createElement('button');
        button.className = 'm3-button m3-button--text';
        button.textContent = t('update_reload');
        button.addEventListener('click', () => void updateSW(true));
        banner.appendChild(button);
        root.appendChild(banner);
      },
    });
  });
}

void boot();
