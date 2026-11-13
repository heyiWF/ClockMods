/** Keep the unit's ink at half the digit height, with matching top edges. */
export function setTemperature(element: HTMLElement, value: string, compact = true): void {
  element.dataset.temperatureText = value;
  element.dataset.temperatureCompact = String(compact);
  const suffix = value.match(/(?:℃|℉|°C|°F)$/)?.[0];
  if (!compact || !suffix) { element.textContent = value; return; }
  if (!element.isConnected) {
    element.textContent = value;
    requestAnimationFrame(() => { if (element.isConnected) setTemperature(element, element.dataset.temperatureText!, element.dataset.temperatureCompact === 'true'); });
    return;
  }
  const unit = document.createElement('span');
  unit.textContent = suffix;
  unit.className = 'temperature-unit';
  const style = getComputedStyle(element);
  const canvas = document.createElement('canvas');
  canvas.lang = element.closest('[lang]')?.getAttribute('lang') || document.documentElement.lang || 'en';
  const paint = canvas.getContext('2d');
  let scale = .5, offset = -.7;
  if (paint) {
    // Language changes fallback glyphs (notably ℃ in Chinese UI). Match DOM text.
    if ('lang' in paint) paint.lang = canvas.lang;
    const size = parseFloat(style.fontSize) || 16;
    const font = (px: number) => `${style.fontStyle || 'normal'} ${style.fontWeight || '400'} ${px}px ${style.fontFamily || 'sans-serif'}`;
    paint.font = font(size);
    const number = value.slice(0, -suffix.length);
    const digit = paint.measureText(/\d/.test(number) ? number : '0'), symbol = paint.measureText(suffix);
    const height = symbol.actualBoundingBoxAscent + symbol.actualBoundingBoxDescent;
    const digits = digit.actualBoundingBoxAscent + digit.actualBoundingBoxDescent;
    if (Number.isFinite(height) && height > 0 && Number.isFinite(digits) && digits > 0) {
      scale = digits * .5 / height;
      paint.font = font(size * scale);
      const small = paint.measureText(suffix);
      offset = (-digit.actualBoundingBoxAscent + small.actualBoundingBoxAscent) / (size * scale);
    }
  }
  unit.style.display = 'inline-block';
  unit.style.fontSize = `${scale}em`;
  unit.style.transform = `translateY(${offset}em)`;
  element.replaceChildren(document.createTextNode(value.slice(0, -suffix.length)), unit);
}

if (typeof document !== 'undefined') document.fonts?.addEventListener('loadingdone', () => {
  document.querySelectorAll<HTMLElement>('[data-temperature-text]').forEach(element =>
    setTemperature(element, element.dataset.temperatureText!, element.dataset.temperatureCompact === 'true'));
});
