/** Independent of the application graph, so a failed module load can still show Retry. */
(function installLaunchScreen() {
  const screen = document.getElementById('launch-screen');
  if (!screen) return;
  const app = document.getElementById('app');
  const status = document.getElementById('launch-status');
  const retry = document.getElementById('launch-retry');
  const zh = navigator.language.toLowerCase().startsWith('zh');
  if (status) status.textContent = zh ? '正在准备你的时光' : 'Preparing your time';
  if (retry) { retry.textContent = zh ? '重试' : 'Try again'; retry.onclick = () => location.reload(); }
  app?.setAttribute('inert', '');
  const entered = performance.now();
  let finished = false;
  const fail = () => {
    if (finished) return;
    if (status) status.textContent = zh ? '启动未完成，请检查连接后重试。' : 'Startup could not finish. Check your connection and try again.';
    retry?.removeAttribute('hidden');
  };
  const timeout = window.setTimeout(fail, 20000);
  const ready = () => {
    if (finished) return;
    finished = true;
    window.clearTimeout(timeout);
    window.removeEventListener('clockmods:ready', ready);
    window.removeEventListener('clockmods:boot-error', fail);
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { app?.removeAttribute('inert'); screen.remove(); return; }
    // Finish the sector assembly on fast starts; slow initialization adds no extra delay.
    window.setTimeout(() => requestAnimationFrame(() => requestAnimationFrame(() => {
      app?.removeAttribute('inert');
      screen.classList.add('is-leaving');
      screen.setAttribute('aria-hidden', 'true');
      screen.addEventListener('transitionend', () => screen.remove(), { once: true });
      window.setTimeout(() => screen.remove(), 400);
    })), Math.max(0, 920 - (performance.now() - entered)));
  };
  window.addEventListener('clockmods:ready', ready);
  window.addEventListener('clockmods:boot-error', fail);
  // Module execution order is not assumed, including when a service worker serves cached assets.
  if (document.documentElement.dataset.clockmodsReady === 'true') ready();
})();
