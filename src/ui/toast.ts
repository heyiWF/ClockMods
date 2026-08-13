/** Transient message, replacing android.widget.Toast. */

const DURATION_MS = 2200;

export function toast(message: string): void {
  const root = document.getElementById('toast-root');
  if (!root) return;
  const element = document.createElement('div');
  element.className = 'toast';
  element.textContent = message;
  root.appendChild(element);
  setTimeout(() => {
    element.classList.add('is-leaving');
    element.addEventListener('animationend', () => element.remove(), { once: true });
    // Guarantee removal even if the animation never runs.
    setTimeout(() => element.remove(), 400);
  }, DURATION_MS);
}
