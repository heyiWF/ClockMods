/**
 * @vitest-environment jsdom
 *
 * Page switching, immersive chrome and swipe navigation — the behaviour
 * ProMainActivity implemented with ViewPager2 and a BottomNavigationView.
 */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Router } from '../src/app/router';
import type { Page } from '../src/app/router';
import { prefs } from '../src/core/prefs';

const NAMES = ['clock', 'calendar', 'pomodoro', 'alarm', 'countdown', 'stopwatch'] as const;

function testPage(name: string, immersive: boolean): Page & {
  started: number;
  stopped: number;
  entered: number;
} {
  return {
    name,
    immersive,
    started: 0,
    stopped: 0,
    entered: 0,
    start() {
      this.started++;
    },
    stop() {
      this.stopped++;
    },
    onEnter() {
      this.entered++;
    },
  } as Page & { started: number; stopped: number; entered: number };
}

function setup() {
  document.body.innerHTML = `
    <main id="pages">
      ${NAMES.map((name) => `<section class="page" data-page="${name}"></section>`).join('')}
    </main>
    <nav id="nav">
      ${NAMES.map((name) => `<button data-nav="${name}"></button>`).join('')}
    </nav>`;
  const container = document.getElementById('pages')!;
  const nav = document.getElementById('nav')!;
  const router = new Router(container, nav);
  const pages = NAMES.map((name) => testPage(name, name === 'clock' || name === 'calendar'));
  for (const page of pages) router.register(page);
  return { router, container, nav, pages };
}

function swipe(container: HTMLElement, fromX: number, toX: number, y = 100): void {
  vi.useFakeTimers();
  container.dispatchEvent(
    pointerEvent('pointerdown', fromX, y)
  );
  container.dispatchEvent(pointerEvent('pointermove', toX, y));
  container.dispatchEvent(
    pointerEvent('pointerup', toX, y)
  );
  vi.advanceTimersByTime(350);
  vi.useRealTimers();
}

function pointerEvent(type: string, clientX: number, clientY: number, pointerId = 1): Event {
  return Object.assign(new Event(type, { bubbles: true, cancelable: true }), {
    clientX,
    clientY,
    pointerId,
    pointerType: 'touch',
    isPrimary: true,
  });
}

function finishTransform(element: HTMLElement): void {
  element.dispatchEvent(
    Object.assign(new Event('transitionend', { bubbles: true }), { propertyName: 'transform' })
  );
}

beforeEach(() => {
  localStorage.clear();
});

describe('Router', () => {
  it('activates the requested page and its nav button', () => {
    const { router, container, nav } = setup();
    router.navigate('calendar');

    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('calendar');
    expect(nav.querySelector('button.is-active')!.getAttribute('data-nav')).toBe('calendar');
  });

  it('starts the incoming page and stops the outgoing one', () => {
    const { router, pages } = setup();
    router.navigate('clock');
    expect(pages[0].started).toBe(1);

    router.navigate('pomodoro');
    expect(pages[0].stopped).toBe(1);
    expect(pages[2].started).toBe(1);
    expect(pages[2].entered).toBe(1);
  });

  it('hides the navigation bar on immersive pages only', () => {
    const { router, nav } = setup();
    router.navigate('clock');
    expect(nav.classList.contains('is-hidden')).toBe(true);
    expect(nav.classList.contains('is-overlay')).toBe(true);

    router.navigate('pomodoro');
    expect(nav.classList.contains('is-hidden')).toBe(false);
    expect(nav.classList.contains('is-overlay')).toBe(false);
  });

  it('reveals the chrome temporarily on an immersive page', () => {
    vi.useFakeTimers();
    const { router, nav } = setup();
    router.navigate('clock');
    expect(nav.classList.contains('is-hidden')).toBe(true);

    router.showChromeTemporarily();
    expect(nav.classList.contains('is-hidden')).toBe(false);
    expect(nav.classList.contains('is-overlay')).toBe(true);

    // Matches ProMainActivity.CHROME_VISIBLE_MILLIS.
    vi.advanceTimersByTime(3000);
    expect(nav.classList.contains('is-hidden')).toBe(true);
    vi.useRealTimers();
  });

  it('does not reveal chrome when an interactive date is clicked', () => {
    const { router, container, nav } = setup();
    router.navigate('calendar');
    const date = document.createElement('button');
    date.className = 'cal-day';
    container.querySelector('[data-page="calendar"]')!.appendChild(date);

    date.click();

    expect(nav.classList.contains('is-hidden')).toBe(true);
    expect(nav.classList.contains('is-overlay')).toBe(true);
  });

  it('navigates by clicking the bottom navigation', () => {
    const { router, nav, container } = setup();
    router.navigate('clock');
    nav.querySelector<HTMLElement>('[data-nav="stopwatch"]')!.click();

    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('stopwatch');
  });

  it('swipes to the next and previous page in registration order', () => {
    const { router, container } = setup();
    router.navigate('clock');

    swipe(container, 300, 100); // leftwards → next page
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('calendar');

    swipe(container, 100, 300); // rightwards → previous page
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('clock');
  });

  it('moves the current and adjacent pages with the pointer before navigation completes', () => {
    vi.useFakeTimers();
    const { router, container, pages } = setup();
    Object.defineProperty(container, 'clientWidth', { configurable: true, value: 400 });
    router.navigate('clock');

    container.dispatchEvent(pointerEvent('pointerdown', 300, 100));
    container.dispatchEvent(pointerEvent('pointermove', 180, 102));

    const clock = container.querySelector<HTMLElement>('[data-page="clock"]')!;
    const calendar = container.querySelector<HTMLElement>('[data-page="calendar"]')!;
    expect(clock.style.transform).toContain('-120px');
    expect(calendar.classList.contains('is-swipe-target')).toBe(true);
    expect(calendar.style.transform).toContain('280px');
    expect(pages[0].stopped).toBe(0);
    expect(pages[1].started).toBe(0);

    container.dispatchEvent(pointerEvent('pointerup', 180, 102));
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('clock');
    expect(calendar.style.transform).toBe('translateX(0)');

    vi.advanceTimersByTime(210);
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('clock');

    finishTransform(calendar);
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('calendar');
    expect(calendar.classList.contains('is-swipe-target')).toBe(false);
    expect(clock.style.transform).toBe('');
    vi.useRealTimers();
  });

  it('animates a short horizontal drag back to the current page', () => {
    vi.useFakeTimers();
    const { router, container } = setup();
    Object.defineProperty(container, 'clientWidth', { configurable: true, value: 400 });
    router.navigate('clock');

    container.dispatchEvent(pointerEvent('pointerdown', 300, 100));
    container.dispatchEvent(pointerEvent('pointermove', 270, 100));
    container.dispatchEvent(pointerEvent('pointerup', 270, 100));

    const clock = container.querySelector<HTMLElement>('[data-page="clock"]')!;
    expect(clock.style.transform).toBe('translateX(0)');
    expect(container.querySelector('[data-page="calendar"]')!.classList.contains('is-swipe-target')).toBe(
      true
    );

    finishTransform(container.querySelector<HTMLElement>('[data-page="calendar"]')!);
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('clock');
    expect(container.querySelector('.is-swipe-target')).toBeNull();
    vi.useRealTimers();
  });

  it('leaves calendar month drags to their own handler', () => {
    const { router, container } = setup();
    router.navigate('calendar');
    const calendar = container.querySelector<HTMLElement>('[data-page="calendar"]')!;
    const monthSurface = document.createElement('div');
    monthSurface.className = 'no-page-swipe';
    calendar.append(monthSurface);

    monthSurface.dispatchEvent(pointerEvent('pointerdown', 300, 100));
    monthSurface.dispatchEvent(pointerEvent('pointermove', 100, 100));
    monthSurface.dispatchEvent(pointerEvent('pointerup', 100, 100));

    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('calendar');
    expect(container.querySelector('.is-swipe-target')).toBeNull();
  });

  it('tracks horizontal drags that start on a page control while preserving taps', () => {
    vi.useFakeTimers();
    const { router, container } = setup();
    Object.defineProperty(container, 'clientWidth', { configurable: true, value: 400 });
    router.navigate('pomodoro');
    const button = document.createElement('button');
    let clicks = 0;
    button.addEventListener('click', () => clicks++);
    container.querySelector('[data-page="pomodoro"]')!.append(button);

    button.click();
    expect(clicks).toBe(1);

    button.dispatchEvent(pointerEvent('pointerdown', 300, 100));
    button.dispatchEvent(pointerEvent('pointermove', 180, 100));
    expect(container.querySelector<HTMLElement>('[data-page="pomodoro"]')!.style.transform).toBe(
      'translateX(-120px)'
    );
    button.dispatchEvent(pointerEvent('pointerup', 180, 100));
    button.click();
    expect(clicks).toBe(1);

    finishTransform(container.querySelector<HTMLElement>('[data-page="alarm"]')!);
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('alarm');
    vi.useRealTimers();
  });

  it('suppresses the click synthesized after a horizontal drag', () => {
    vi.useFakeTimers();
    const { router, container, nav } = setup();
    Object.defineProperty(container, 'clientWidth', { configurable: true, value: 400 });
    router.navigate('clock');

    container.dispatchEvent(pointerEvent('pointerdown', 300, 100));
    container.dispatchEvent(pointerEvent('pointermove', 270, 100));
    container.dispatchEvent(pointerEvent('pointerup', 270, 100));
    container.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

    expect(nav.classList.contains('is-hidden')).toBe(true);
    vi.advanceTimersByTime(330);
    vi.useRealTimers();
  });

  it('ignores short swipes and mostly-vertical drags', () => {
    const { router, container } = setup();
    router.navigate('clock');

    swipe(container, 300, 270); // below the 40px threshold
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('clock');

    container.dispatchEvent(
      Object.assign(new Event('pointerdown', { bubbles: true }), { clientX: 300, clientY: 100 })
    );
    container.dispatchEvent(
      Object.assign(new Event('pointerup', { bubbles: true }), { clientX: 200, clientY: 400 })
    );
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('clock');
  });

  it('stops at the ends of the page list', () => {
    const { router, container } = setup();
    router.navigate('clock');
    swipe(container, 100, 300); // already first
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('clock');

    router.navigate('stopwatch');
    swipe(container, 300, 100); // already last
    expect(container.querySelector('.page.is-active')!.getAttribute('data-page')).toBe('stopwatch');
  });

  it('remembers the last page for the next launch', () => {
    const { router } = setup();
    router.navigate('countdown');
    expect(prefs.getLastPage()).toBe('countdown');
  });

  it('pushes settings changes to every page, not just the visible one', async () => {
    const { router, pages } = setup();
    const refreshed: string[] = [];
    for (const page of pages) {
      (page as Page).refreshSettings = () => {
        refreshed.push(page.name);
      };
    }
    router.navigate('clock');
    await router.refreshAll();

    expect(refreshed).toEqual([...NAMES]);
  });
});
