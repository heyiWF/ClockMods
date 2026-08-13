/**
 * Application entry point.
 *
 * Replaces ProMainActivity's onCreate: apply the stored orientation, keep the
 * screen awake, load the offline data sets, wire the pages into the router and
 * restore the page that was showing last.
 */
import './styles/tokens.css';
import './styles/fonts.css';
import './styles/base.css';
import './styles/clock.css';
import './styles/calendar.css';
import './styles/timers.css';
import './styles/settings.css';

import { prefs } from './core/prefs';
import { onLanguageChange, refreshLanguage, t } from './core/i18n';
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

async function boot(): Promise<void> {
  document.documentElement.lang = document.documentElement.lang || 'zh-Hans';
  refreshLanguage();

  await Promise.all([
    ensureFontLoaded(prefs.getFontFamily(), prefs.isBoldText()),
    loadWeatherIcons(),
    loadHolidays(),
  ]);

  timeSource.configure();
  void applyScreenOrientation();
  installWakeLockHandlers();

  const container = document.getElementById('pages')!;
  const nav = document.getElementById('nav')!;
  const router = new Router(container, nav);

  const showSettings = () => {
    openSettings(async (languageChanged) => {
      timeSource.configure();
      void applyScreenOrientation();
      if (languageChanged) refreshLanguage();
      await router.refreshAll();
      toast(t('settings_applied'));
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

  onLanguageChange(() => {
    void router.refreshAll();
  });

  // A wake lock needs a user gesture on some browsers, so ask again on first tap.
  void requestWakeLock();
  document.addEventListener('pointerdown', () => void requestWakeLock(), { once: true });

  registerServiceWorker();
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
        const banner = document.createElement('div');
        banner.className = 'toast';
        banner.style.pointerEvents = 'auto';
        banner.textContent = `${t('update_available')} `;
        const button = document.createElement('button');
        button.className = 'button button--text';
        button.textContent = t('update_reload');
        button.addEventListener('click', () => void updateSW(true));
        banner.appendChild(button);
        root.appendChild(banner);
      },
    });
  });
}

void boot();
