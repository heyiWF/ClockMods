/**
 * Notifications for timer and alarm alerts.
 *
 * Replaces the NotificationChannel plumbing in the Android build. Permission is
 * requested lazily, on the first alert the user actually arms, rather than at
 * startup — a wall clock should not greet you with a permission prompt.
 */

export function notificationsAllowed(): boolean {
  return 'Notification' in window && Notification.permission === 'granted';
}

/** Asks once; safe to call from a user gesture only. */
export async function requestNotificationPermission(): Promise<boolean> {
  if (!('Notification' in window)) return false;
  if (Notification.permission === 'granted') return true;
  if (Notification.permission === 'denied') return false;
  try {
    return (await Notification.requestPermission()) === 'granted';
  } catch {
    return false;
  }
}

export function notify(title: string, body: string): void {
  if (!notificationsAllowed()) return;
  try {
    // eslint-disable-next-line no-new
    new Notification(title, {
      body,
      icon: `${import.meta.env.BASE_URL}icons/icon-192.png`,
      badge: `${import.meta.env.BASE_URL}icons/icon-192.png`,
      tag: 'clockmods',
    });
  } catch {
    // Some browsers only allow notifications from a service worker; the in-page
    // alert still fires either way.
  }
}
