/**
 * Page switching, swipe gestures and the immersive chrome.
 *
 * Ported from com.clockmods.pro.ProMainActivity: the same six pages in the same
 * order, horizontal swiping between them (ViewPager2), a bottom navigation bar
 * that stays in sync, and the clock/calendar pages hiding the chrome until a tap
 * reveals it for three seconds.
 */
import { prefs } from '../core/prefs';

/** Matches ProMainActivity.CHROME_VISIBLE_MILLIS. */
const CHROME_VISIBLE_MS = 3000;
const SWIPE_THRESHOLD_PX = 60;

export interface Page {
  readonly name: string;
  /** Clock and calendar hide the navigation bar (ProMainActivity.isImmersivePage). */
  readonly immersive: boolean;
  start(): void;
  stop(): void;
  refreshSettings?(): void | Promise<void>;
  onEnter?(fromAnotherPage: boolean): void;
  onExit?(): void;
}

export class Router {
  private readonly pages = new Map<string, Page>();
  private readonly order: string[] = [];
  private readonly nav: HTMLElement;
  private readonly container: HTMLElement;
  private current = '';
  private chromeTimer: number | null = null;

  constructor(container: HTMLElement, nav: HTMLElement) {
    this.container = container;
    this.nav = nav;
    this.bindNavigation();
    this.bindSwipe();
    this.bindChromeTap();
  }

  register(page: Page): void {
    this.pages.set(page.name, page);
    this.order.push(page.name);
  }

  get activePage(): Page | undefined {
    return this.pages.get(this.current);
  }

  navigate(name: string, fromGesture = false): void {
    if (!this.pages.has(name) || name === this.current) return;
    const previous = this.pages.get(this.current);
    if (previous) {
      previous.onExit?.();
      previous.stop();
    }
    this.current = name;
    prefs.setLastPage(name);

    for (const element of this.container.querySelectorAll<HTMLElement>('.page')) {
      element.classList.toggle('is-active', element.dataset.page === name);
    }
    for (const button of this.nav.querySelectorAll<HTMLElement>('button')) {
      button.classList.toggle('is-active', button.dataset.nav === name);
    }

    const page = this.pages.get(name)!;
    page.onEnter?.(fromGesture || previous !== undefined);
    page.start();
    this.updateChrome();
  }

  /** Applies changed settings to every page, not just the visible one. */
  async refreshAll(): Promise<void> {
    for (const page of this.pages.values()) {
      await page.refreshSettings?.();
    }
    this.updateChrome();
  }

  private bindNavigation(): void {
    this.nav.addEventListener('click', (event) => {
      const button = (event.target as HTMLElement).closest<HTMLElement>('button[data-nav]');
      if (button?.dataset.nav) this.navigate(button.dataset.nav);
    });
  }

  /**
   * Horizontal swipes move between pages. Interactive controls and the calendar's
   * own month-drag surface opt out, the same way the Android activity skipped its
   * gesture detector for those touches.
   */
  private bindSwipe(): void {
    let startX = 0;
    let startY = 0;
    let tracking = false;
    this.container.addEventListener(
      'pointerdown',
      (event) => {
        const target = event.target as HTMLElement;
        if (target.closest('button, input, select, textarea, a, .no-page-swipe')) {
          tracking = false;
          return;
        }
        tracking = true;
        startX = event.clientX;
        startY = event.clientY;
      },
      { passive: true }
    );
    this.container.addEventListener(
      'pointerup',
      (event) => {
        if (!tracking) return;
        tracking = false;
        const deltaX = event.clientX - startX;
        const deltaY = event.clientY - startY;
        if (Math.abs(deltaX) < SWIPE_THRESHOLD_PX || Math.abs(deltaX) <= Math.abs(deltaY)) return;
        const index = this.order.indexOf(this.current);
        const next = index + (deltaX < 0 ? 1 : -1);
        if (next >= 0 && next < this.order.length) this.navigate(this.order[next], true);
      },
      { passive: true }
    );
  }

  /** A tap on an immersive page reveals the chrome for three seconds. */
  private bindChromeTap(): void {
    this.container.addEventListener('click', (event) => {
      const target = event.target as HTMLElement;
      if (target.closest('button, input, select, textarea, a')) return;
      this.showChromeTemporarily();
    });
  }

  showChromeTemporarily(): void {
    this.nav.classList.remove('is-hidden');
    this.clearChromeTimer();
    if (this.isImmersive()) {
      this.chromeTimer = window.setTimeout(() => this.updateChrome(), CHROME_VISIBLE_MS);
    }
  }

  updateChrome(): void {
    this.clearChromeTimer();
    this.nav.classList.toggle('is-hidden', this.isImmersive());
  }

  private isImmersive(): boolean {
    return this.pages.get(this.current)?.immersive ?? false;
  }

  private clearChromeTimer(): void {
    if (this.chromeTimer !== null) {
      clearTimeout(this.chromeTimer);
      this.chromeTimer = null;
    }
  }
}
