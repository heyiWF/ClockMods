import { onLanguageChange, t } from '../core/i18n';
import { toast } from './toast';

export interface FullscreenController {
  isFullscreen(): boolean | Promise<boolean>;
  setFullscreen(fullscreen: boolean): void | Promise<void>;
  onChange(listener: () => void): (() => void) | Promise<() => void>;
}

/** Safari still exposes prefixed fullscreen APIs on some supported hosts. */
function browserFullscreen(doc: Document): FullscreenController | null {
  const root = doc.documentElement as HTMLElement & { webkitRequestFullscreen?: () => void | Promise<void> };
  const host = doc as Document & {
    webkitFullscreenElement?: Element;
    webkitExitFullscreen?: () => void | Promise<void>;
    webkitFullscreenEnabled?: boolean;
  };
  const enter = root.requestFullscreen || root.webkitRequestFullscreen;
  const exit = doc.exitFullscreen || host.webkitExitFullscreen;
  if (!enter || !exit || (doc.fullscreenEnabled ?? host.webkitFullscreenEnabled) === false) return null;
  return {
    isFullscreen: () => Boolean(doc.fullscreenElement || host.webkitFullscreenElement),
    setFullscreen: full => full ? enter.call(root) : exit.call(doc),
    onChange(listener) {
      doc.addEventListener('fullscreenchange', listener);
      doc.addEventListener('webkitfullscreenchange', listener);
      return () => {
        doc.removeEventListener('fullscreenchange', listener);
        doc.removeEventListener('webkitfullscreenchange', listener);
      };
    },
  };
}

/** Each immersive page owns its button, so navigation and swiping hide it too. */
export function installFullscreenButtons(doc: Document = document, controller = browserFullscreen(doc)): () => void {
  if (!controller) return () => {};
  const enterPath = 'M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z';
  const exitPath = 'M5 16h3v3h2v-5H5v2zM8 8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z';
  let disposed = false, revision = 0;
  const buttons = [...doc.querySelectorAll<HTMLElement>('.page[data-page="clock"], .page[data-page="calendar"]')].map(page => {
    const button = doc.createElement('button');
    button.type = 'button';
    button.className = 'fullscreen-button';
    button.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path/></svg>';
    page.append(button);
    return button;
  });
  const render = (full: boolean) => {
    for (const button of buttons) {
      const label = t(full ? 'fullscreen_exit' : 'fullscreen_enter');
      button.title = label;
      button.setAttribute('aria-label', label);
      button.setAttribute('aria-pressed', String(full));
      button.querySelector('path')!.setAttribute('d', full ? exitPath : enterPath);
    }
  };
  const refresh = async () => {
    const current = ++revision;
    try {
      const full = await controller.isFullscreen();
      if (!disposed && current === revision) render(full);
    } catch { /* Keep the last known state if the window is closing. */ }
  };
  const click = async () => {
    if (buttons.some(button => button.disabled)) return;
    buttons.forEach(button => button.disabled = true);
    try {
      // Invoke the browser API within this gesture; native state may need IPC.
      const state = controller.isFullscreen();
      await controller.setFullscreen(typeof state === 'boolean' ? !state : !(await state));
      await refresh();
    } catch (error) {
      console.warn('Fullscreen request failed', error);
      toast(t('fullscreen_unavailable'));
    }
    finally { buttons.forEach(button => button.disabled = false); }
  };
  buttons.forEach(button => button.addEventListener('click', click));
  render(false);
  void refresh();
  const unsubscribeLanguage = onLanguageChange(() => void refresh());
  const unsubscribeState = Promise.resolve(controller.onChange(() => void refresh())).catch(() => () => {});
  return () => {
    disposed = true;
    unsubscribeLanguage();
    void unsubscribeState.then(unsubscribe => unsubscribe());
    buttons.forEach(button => button.remove());
  };
}
