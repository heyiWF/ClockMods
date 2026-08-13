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
const SWIPE_THRESHOLD_PX = 40;
const SWIPE_INTENT_PX = 4;
const SWIPE_ANIMATION_MS = 210;
const SWIPE_ANIMATION_FALLBACK_MS = SWIPE_ANIMATION_MS + 120;

interface SwipeState {
  readonly pointerId: number;
  readonly startX: number;
  readonly startY: number;
  readonly currentElement: HTMLElement;
  width: number;
  deltaX: number;
  horizontal: boolean;
  direction: -1 | 0 | 1;
  targetName: string | null;
  targetElement: HTMLElement | null;
}

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
  private swipeState: SwipeState | null = null;
  private swipeAnimationTimer: number | null = null;
  private swipeTransitionElement: HTMLElement | null = null;
  private swipeTransitionEnd: ((event: TransitionEvent) => void) | null = null;
  private suppressNextClick = false;
  private suppressClickTimer: number | null = null;

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
    this.cancelSwipe();
    this.activate(name, fromGesture);
  }

  private activate(name: string, fromGesture: boolean): void {
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
    this.container.addEventListener(
      'pointerdown',
      (event) => {
        const target = event.target as HTMLElement;
        if (
          this.swipeState ||
          (event.isPrimary === false) ||
          (event.pointerType === 'mouse' && event.button !== 0) ||
          target.closest('.no-page-swipe')
        ) {
          return;
        }
        const currentElement = this.pageElement(this.current);
        if (!currentElement) return;
        this.swipeState = {
          pointerId: event.pointerId,
          startX: event.clientX,
          startY: event.clientY,
          currentElement,
          width:
            this.container.getBoundingClientRect().width ||
            this.container.clientWidth ||
            window.innerWidth ||
            1,
          deltaX: 0,
          horizontal: false,
          direction: 0,
          targetName: null,
          targetElement: null,
        };
      },
      { capture: true, passive: true }
    );

    this.container.addEventListener(
      'pointermove',
      (event) => {
        const state = this.swipeState;
        if (!state || event.pointerId !== state.pointerId) return;
        const deltaX = event.clientX - state.startX;
        const deltaY = event.clientY - state.startY;

        if (!state.horizontal) {
          if (Math.max(Math.abs(deltaX), Math.abs(deltaY)) < SWIPE_INTENT_PX) return;
          if (Math.abs(deltaX) <= Math.abs(deltaY)) {
            this.swipeState = null;
            return;
          }
          state.horizontal = true;
          this.suppressNextClick = true;
          this.container.classList.add('is-page-swiping');
          this.container.setPointerCapture?.(event.pointerId);
        }

        event.preventDefault();
        event.stopPropagation();
        this.renderSwipe(deltaX);
      },
      { capture: true }
    );

    const finish = (event: PointerEvent): void => {
      const state = this.swipeState;
      if (!state || event.pointerId !== state.pointerId) return;
      const deltaX = event.clientX - state.startX;
      const deltaY = event.clientY - state.startY;

      if (
        !state.horizontal &&
        Math.abs(deltaX) >= SWIPE_INTENT_PX &&
        Math.abs(deltaX) > Math.abs(deltaY)
      ) {
        state.horizontal = true;
        this.suppressNextClick = true;
        this.container.classList.add('is-page-swiping');
        this.renderSwipe(deltaX);
      }

      if (!state.horizontal) {
        this.swipeState = null;
        return;
      }

      event.preventDefault();
      event.stopPropagation();
      if (this.container.hasPointerCapture?.(event.pointerId)) {
        this.container.releasePointerCapture(event.pointerId);
      }
      const threshold = Math.min(SWIPE_THRESHOLD_PX, state.width * 0.2);
      const complete =
        event.type !== 'pointercancel' &&
        state.targetElement !== null &&
        Math.abs(deltaX) >= threshold;
      this.animateSwipe(complete);
      this.clearSuppressClickTimer();
      this.suppressClickTimer = window.setTimeout(() => {
        this.suppressNextClick = false;
        this.suppressClickTimer = null;
      });
    };

    this.container.addEventListener(
      'pointerup',
      finish,
      { capture: true }
    );
    this.container.addEventListener('pointercancel', finish, { capture: true });

    // A completed horizontal gesture can otherwise synthesize a click on the
    // clock stage, revealing chrome or counting towards its double-tap action.
    this.container.addEventListener(
      'click',
      (event) => {
        if (!this.suppressNextClick) return;
        this.suppressNextClick = false;
        this.clearSuppressClickTimer();
        event.preventDefault();
        event.stopImmediatePropagation();
      },
      { capture: true }
    );
  }

  private renderSwipe(deltaX: number): void {
    const state = this.swipeState;
    if (!state) return;
    const direction: -1 | 1 = deltaX < 0 ? 1 : -1;
    if (direction !== state.direction) this.prepareSwipeTarget(direction);

    state.deltaX = deltaX;
    state.currentElement.classList.add('is-swipe-layer');
    state.currentElement.style.transition = 'none';
    if (!state.targetElement) {
      state.currentElement.style.transform = `translateX(${deltaX * 0.22}px)`;
      return;
    }

    state.currentElement.style.transform = `translateX(${deltaX}px)`;
    state.targetElement.style.transform = `translateX(${direction * state.width + deltaX}px)`;
  }

  private prepareSwipeTarget(direction: -1 | 1): void {
    const state = this.swipeState;
    if (!state) return;
    if (state.targetElement) this.resetSwipeElement(state.targetElement, true);

    const nextIndex = this.order.indexOf(this.current) + direction;
    const targetName = this.order[nextIndex] ?? null;
    const targetElement = targetName ? this.pageElement(targetName) : null;
    state.direction = direction;
    state.targetName = targetName;
    state.targetElement = targetElement;
    if (!targetElement) return;

    targetElement.classList.add('is-swipe-target', 'is-swipe-layer');
    targetElement.style.transition = 'none';
    targetElement.style.transform = `translateX(${direction * state.width}px)`;
  }

  private animateSwipe(complete: boolean): void {
    const state = this.swipeState;
    if (!state) return;
    const target = state.targetElement;
    const transition = `transform ${SWIPE_ANIMATION_MS}ms cubic-bezier(0.2, 0, 0, 1)`;
    let finished = false;
    const finish = (): void => {
      if (finished || this.swipeState !== state) return;
      finished = true;
      this.clearSwipeAnimationWait();
      const targetName = complete ? state.targetName : null;
      // Keep the settled target visible while ownership changes from the
      // temporary swipe layer to the active page. Removing the swipe class
      // first would briefly make it display:none before activate() restores it.
      if (targetName) this.activate(targetName, true);
      this.cleanupSwipe(state);
      this.swipeState = null;
    };

    if (this.prefersReducedMotion()) {
      finish();
      return;
    }

    state.currentElement.style.transition = transition;
    if (target) target.style.transition = transition;
    // Flush the dragged positions before assigning the settle positions.
    void state.currentElement.offsetWidth;
    state.currentElement.style.transform = complete
      ? `translateX(${-state.direction * state.width}px)`
      : 'translateX(0)';
    if (target) {
      target.style.transform = complete
        ? 'translateX(0)'
        : `translateX(${state.direction * state.width}px)`;
    }

    // A timer started in this pointer event fires before a CSS transition that
    // begins on the next paint has necessarily rendered its final frame. Commit
    // the route only after the moving layer reports that transform has ended.
    this.swipeTransitionElement = target ?? state.currentElement;
    this.swipeTransitionEnd = (event) => {
      if (event.target === this.swipeTransitionElement && event.propertyName === 'transform') {
        finish();
      }
    };
    this.swipeTransitionElement.addEventListener('transitionend', this.swipeTransitionEnd);
    this.swipeAnimationTimer = window.setTimeout(finish, SWIPE_ANIMATION_FALLBACK_MS);
  }

  private cancelSwipe(): void {
    this.clearSwipeAnimationWait();
    if (this.swipeState) this.cleanupSwipe(this.swipeState);
    this.swipeState = null;
  }

  private clearSwipeAnimationWait(): void {
    if (this.swipeAnimationTimer !== null) {
      clearTimeout(this.swipeAnimationTimer);
      this.swipeAnimationTimer = null;
    }
    if (this.swipeTransitionElement && this.swipeTransitionEnd) {
      this.swipeTransitionElement.removeEventListener(
        'transitionend',
        this.swipeTransitionEnd
      );
    }
    this.swipeTransitionElement = null;
    this.swipeTransitionEnd = null;
  }

  private cleanupSwipe(state: SwipeState): void {
    this.resetSwipeElement(state.currentElement, false);
    if (state.targetElement) this.resetSwipeElement(state.targetElement, true);
    this.container.classList.remove('is-page-swiping');
  }

  private resetSwipeElement(element: HTMLElement, target: boolean): void {
    element.classList.remove('is-swipe-layer');
    if (target) element.classList.remove('is-swipe-target');
    element.style.removeProperty('transition');
    element.style.removeProperty('transform');
  }

  private pageElement(name: string): HTMLElement | null {
    return this.container.querySelector<HTMLElement>(`.page[data-page='${name}']`);
  }

  private prefersReducedMotion(): boolean {
    return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
  }

  private clearSuppressClickTimer(): void {
    if (this.suppressClickTimer !== null) {
      clearTimeout(this.suppressClickTimer);
      this.suppressClickTimer = null;
    }
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
    this.container.classList.toggle('has-visible-overlay-nav', this.isImmersive());
    this.clearChromeTimer();
    if (this.isImmersive()) {
      this.chromeTimer = window.setTimeout(() => this.updateChrome(), CHROME_VISIBLE_MS);
    }
  }

  updateChrome(): void {
    this.clearChromeTimer();
    const immersive = this.isImmersive();
    this.nav.classList.toggle('is-overlay', immersive);
    this.nav.classList.toggle('is-hidden', immersive);
    this.container.classList.remove('has-visible-overlay-nav');
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
