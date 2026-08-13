/**
 * Keeps the screen on while the clock is showing.
 *
 * Replaces FLAG_KEEP_SCREEN_ON. The lock is dropped whenever the page is hidden
 * and must be reacquired on return, which is what the visibility listener does.
 */

let sentinel: WakeLockSentinel | null = null;
let wanted = false;

export async function requestWakeLock(): Promise<void> {
  wanted = true;
  if (!('wakeLock' in navigator) || sentinel) return;
  try {
    sentinel = await navigator.wakeLock.request('screen');
    sentinel.addEventListener('release', () => {
      sentinel = null;
    });
  } catch {
    // Denied (unsupported, low battery, or not user-activated yet); the clock
    // still works, the screen just sleeps normally.
    sentinel = null;
  }
}

export async function releaseWakeLock(): Promise<void> {
  wanted = false;
  if (!sentinel) return;
  try {
    await sentinel.release();
  } catch {
    /* already released */
  }
  sentinel = null;
}

/** Re-acquires the lock when the page becomes visible again. */
export function installWakeLockHandlers(): void {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && wanted) void requestWakeLock();
  });
}
