/** A small source badge, visible only during mouse activity and its 3s grace period. */
export class WeatherAttribution {
  private enabled = false;
  private active = false;
  private timer: ReturnType<typeof setTimeout> | undefined;
  private readonly onMove = (event: PointerEvent): void => {
    if (!this.enabled || event.pointerType !== 'mouse') return;
    this.badge.hidden = false;
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.hide(), 3000);
  };

  constructor(root: HTMLElement, private readonly badge: HTMLElement) {
    // Calendar attribution must float over the page, not occupy a forecast-card row.
    root.append(badge);
    this.badge.hidden = true;
  }

  setEnabled(enabled: boolean): void {
    this.enabled = enabled;
    if (!enabled) this.hide();
  }

  start(): void {
    if (this.active) return;
    this.active = true;
    this.badge.ownerDocument.addEventListener('pointermove', this.onMove, {passive:true});
  }

  stop(): void {
    this.active = false;
    this.badge.ownerDocument.removeEventListener('pointermove', this.onMove);
    this.hide();
  }

  private hide(): void {
    clearTimeout(this.timer);
    this.timer = undefined;
    this.badge.hidden = true;
  }
}
