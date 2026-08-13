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
  container.dispatchEvent(
    Object.assign(new Event('pointerdown', { bubbles: true }), { clientX: fromX, clientY: y })
  );
  container.dispatchEvent(
    Object.assign(new Event('pointerup', { bubbles: true }), { clientX: toX, clientY: y })
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

    router.navigate('pomodoro');
    expect(nav.classList.contains('is-hidden')).toBe(false);
  });

  it('reveals the chrome temporarily on an immersive page', () => {
    vi.useFakeTimers();
    const { router, nav } = setup();
    router.navigate('clock');
    expect(nav.classList.contains('is-hidden')).toBe(true);

    router.showChromeTemporarily();
    expect(nav.classList.contains('is-hidden')).toBe(false);

    // Matches ProMainActivity.CHROME_VISIBLE_MILLIS.
    vi.advanceTimersByTime(3000);
    expect(nav.classList.contains('is-hidden')).toBe(true);
    vi.useRealTimers();
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

  it('ignores short swipes and mostly-vertical drags', () => {
    const { router, container } = setup();
    router.navigate('clock');

    swipe(container, 300, 260); // below the 60px threshold
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
