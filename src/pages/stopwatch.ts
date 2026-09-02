/**
 * Stopwatch with laps.
 *
 * Ported from com.clockmods.pro.ProStopwatchFragment, including its centisecond
 * display and newest-first lap list. `performance.now()` replaces
 * SystemClock.elapsedRealtime as the monotonic source.
 */
import { t } from '../core/i18n';
import { twoDigits } from '../format/time-formatter';
import type { Page } from '../app/router';

const FRAME_MS = 32;

export class StopwatchPage implements Page {
  readonly name = 'stopwatch';
  readonly immersive = false;

  private readonly root: HTMLElement;
  private display!: HTMLElement;
  private startPause!: HTMLButtonElement;
  private lapButton!: HTMLButtonElement;
  private laps!: HTMLElement;
  private title!: HTMLElement;
  private resetButton!: HTMLButtonElement;

  private accumulated = 0;
  private startedAt = 0;
  private running = false;
  private lapCount = 0;
  private ticker: number | null = null;

  constructor(root: HTMLElement) {
    this.root = root;
    this.build();
    this.restore();
    this.render();
  }

  private build(): void {
    this.root.classList.add('timer-page');
    this.root.innerHTML = `
      <div class="timer">
        <h1 class="timer-title"></h1>
        <div class="timer-display stopwatch-display">00:00.00</div>
        <div class="timer-actions">
          <button type="button" class="m3-button m3-button--outlined" data-act="reset"></button>
          <button type="button" class="m3-button m3-button--filled" data-act="toggle"></button>
          <button type="button" class="m3-button m3-button--outlined" data-act="lap"></button>
        </div>
        <ol class="stopwatch-laps"></ol>
      </div>`;
    this.title = this.root.querySelector('.timer-title')!;
    this.display = this.root.querySelector('.timer-display')!;
    this.startPause = this.root.querySelector('[data-act="toggle"]')!;
    this.lapButton = this.root.querySelector('[data-act="lap"]')!;
    this.resetButton = this.root.querySelector('[data-act="reset"]')!;
    this.laps = this.root.querySelector('.stopwatch-laps')!;

    this.startPause.addEventListener('click', () => this.toggle());
    this.resetButton.addEventListener('click', () => this.reset());
    this.lapButton.addEventListener('click', () => this.addLap());
  }

  start(): void {
    if (this.ticker !== null) return;
    this.ticker = window.setInterval(() => this.render(), FRAME_MS);
    this.render();
  }

  stop(): void {
    if (this.ticker !== null) {
      clearInterval(this.ticker);
      this.ticker = null;
    }
    this.persist();
  }

  refreshSettings(): void {
    this.title.textContent = t('pro_page_stopwatch');
    this.resetButton.textContent = t('timer_reset');
    this.lapButton.textContent = t('stopwatch_lap');
    this.render();
  }

  private toggle(): void {
    if (this.running) {
      this.accumulated = this.elapsed();
      this.running = false;
    } else {
      this.startedAt = performance.now();
      this.running = true;
    }
    this.persist();
    this.render();
  }

  private reset(): void {
    this.running = false;
    this.accumulated = 0;
    this.lapCount = 0;
    this.laps.replaceChildren();
    this.persist();
    this.render();
  }

  private addLap(): void {
    if (!this.running && this.accumulated === 0) return;
    const item = document.createElement('li');
    item.textContent = t('stopwatch_lap_value', ++this.lapCount, format(this.elapsed()));
    this.laps.prepend(item);
  }

  private elapsed(): number {
    return this.accumulated + (this.running ? performance.now() - this.startedAt : 0);
  }

  private render(): void {
    this.display.textContent = format(this.elapsed());
    this.startPause.textContent = t(this.running ? 'timer_pause' : 'timer_start');
  }

  private restore(): void {
    try {
      this.accumulated = Number(localStorage.getItem('pro_stopwatch.accumulated') ?? 0) || 0;
      // A stopwatch cannot keep running across a reload: performance.now() restarts
      // with the document, so the elapsed time is resumed as paused.
      this.running = false;
    } catch {
      this.accumulated = 0;
    }
  }

  private persist(): void {
    try {
      localStorage.setItem('pro_stopwatch.accumulated', String(this.elapsed()));
      localStorage.setItem('pro_stopwatch.running', String(this.running));
    } catch {
      /* storage unavailable */
    }
    if (this.running) {
      this.accumulated = this.elapsed();
      this.startedAt = performance.now();
    }
  }
}

/** mm:ss.cc, matching ProStopwatchFragment.format. */
function format(millis: number): string {
  const centiseconds = Math.floor(millis / 10);
  return `${twoDigits(Math.floor(centiseconds / 6000))}:${twoDigits(
    Math.floor(centiseconds / 100) % 60
  )}.${twoDigits(centiseconds % 100)}`;
}
