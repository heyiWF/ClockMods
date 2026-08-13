/**
 * Pomodoro and countdown, which share one implementation.
 *
 * Ported from com.clockmods.pro.ProTimerFragment: the same presets, the same
 * 25/5/15 pomodoro cycle, and the same persisted deadline so a running timer
 * survives a reload. Android scheduled an AlarmManager broadcast for the finish;
 * a page can only fire while it is open, so the countdown is driven by the stored
 * deadline and alerted in-page.
 */
import { t } from '../core/i18n';
import { twoDigits } from '../format/time-formatter';
import { playTimerTone, primeAudio, vibrate } from '../ui/sound';
import { notify } from '../ui/notifications';
import type { Page } from '../app/router';

const POMODORO_DURATIONS = [25, 5, 15].map((minutes) => minutes * 60_000);
const POMODORO_PRESETS = [5, 15, 25];
const COUNTDOWN_PRESETS = [5, 10, 30, 60];
const TICK_MS = 250;

/** Ported verbatim, including its range checks (ProTimerFragment.customDurationMillis). */
export function customDurationMillis(hours: number, minutes: number, seconds: number): number {
  if (hours < 0 || hours > 99 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
    return 0;
  }
  return (hours * 3600 + minutes * 60 + seconds) * 1000;
}

type Mode = 'pomodoro' | 'countdown';

export class TimerPage implements Page {
  readonly immersive = false;
  readonly name: Mode;

  private readonly root: HTMLElement;
  private readonly pomodoro: boolean;
  private display!: HTMLElement;
  private phase!: HTMLElement;
  private title!: HTMLElement;
  private startPause!: HTMLButtonElement;
  private presets!: HTMLElement;

  private durationMillis: number;
  private remainingMillis: number;
  private deadlineMillis = 0;
  private running = false;
  private pomodoroPhase = 0;
  private ticker: number | null = null;

  constructor(root: HTMLElement, mode: Mode) {
    this.root = root;
    this.name = mode;
    this.pomodoro = mode === 'pomodoro';
    this.durationMillis = this.pomodoro ? 25 * 60_000 : 10 * 60_000;
    this.remainingMillis = this.durationMillis;
    this.build();
    this.restore();
    this.refresh();
  }

  private get prefix(): string {
    return this.pomodoro ? 'pro_timers.pomodoro_' : 'pro_timers.countdown_';
  }

  private build(): void {
    this.root.classList.add('timer-page');
    this.root.innerHTML = `
      <div class="timer">
        <h1 class="timer-title"></h1>
        <div class="timer-phase"></div>
        <div class="timer-display">00:00</div>
        <div class="timer-actions">
          <button type="button" class="button button--outlined" data-act="reset"></button>
          <button type="button" class="button" data-act="toggle"></button>
          <button type="button" class="button button--outlined" data-act="skip"></button>
        </div>
        <div class="timer-presets"></div>
      </div>`;
    this.title = this.root.querySelector('.timer-title')!;
    this.phase = this.root.querySelector('.timer-phase')!;
    this.display = this.root.querySelector('.timer-display')!;
    this.startPause = this.root.querySelector('[data-act="toggle"]')!;
    this.presets = this.root.querySelector('.timer-presets')!;

    this.root.querySelector('[data-act="reset"]')!.addEventListener('click', () => this.reset());
    this.startPause.addEventListener('click', () => this.toggle());
    const skip = this.root.querySelector<HTMLElement>('[data-act="skip"]')!;
    skip.hidden = !this.pomodoro;
    skip.addEventListener('click', () => this.advancePomodoro());
    this.phase.hidden = !this.pomodoro;
  }

  private buildPresets(): void {
    const minutes = this.pomodoro ? POMODORO_PRESETS : COUNTDOWN_PRESETS;
    const buttons = minutes.map((value) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'button button--outlined timer-preset';
      button.textContent = t('timer_minutes', value);
      button.addEventListener('click', () => this.setDuration(value * 60_000));
      return button;
    });
    if (!this.pomodoro) {
      const custom = document.createElement('button');
      custom.type = 'button';
      custom.className = 'button button--outlined timer-preset';
      custom.textContent = t('timer_custom');
      custom.addEventListener('click', () => this.showCustomDialog());
      buttons.push(custom);
    }
    this.presets.replaceChildren(...buttons);
  }

  start(): void {
    if (this.ticker !== null) return;
    this.ticker = window.setInterval(() => this.refresh(), TICK_MS);
    this.refresh();
  }

  stop(): void {
    if (this.ticker !== null) {
      clearInterval(this.ticker);
      this.ticker = null;
    }
    this.persist();
  }

  refreshSettings(): void {
    this.title.textContent = t(this.pomodoro ? 'pro_page_pomodoro' : 'pro_page_countdown');
    this.buildPresets();
    this.refresh();
  }

  private setDuration(duration: number): void {
    this.running = false;
    this.durationMillis = duration;
    this.remainingMillis = duration;
    this.persist();
    this.refresh();
  }

  private toggle(): void {
    primeAudio();
    if (this.running) {
      this.remainingMillis = Math.max(0, this.deadlineMillis - Date.now());
      this.running = false;
    } else if (this.remainingMillis > 0) {
      this.deadlineMillis = Date.now() + this.remainingMillis;
      this.running = true;
    }
    this.persist();
    this.refresh();
  }

  private reset(): void {
    this.running = false;
    if (this.pomodoro) {
      this.pomodoroPhase = 0;
      this.durationMillis = POMODORO_DURATIONS[0];
    }
    this.remainingMillis = this.durationMillis;
    this.persist();
    this.refresh();
  }

  private advancePomodoro(): void {
    this.pomodoroPhase = (this.pomodoroPhase + 1) % POMODORO_DURATIONS.length;
    this.durationMillis = POMODORO_DURATIONS[this.pomodoroPhase];
    this.remainingMillis = this.durationMillis;
    this.running = false;
    this.persist();
    this.refresh();
  }

  private refresh(): void {
    if (this.running) {
      this.remainingMillis = Math.max(0, this.deadlineMillis - Date.now());
      if (this.remainingMillis === 0) {
        this.running = false;
        this.onFinished();
        if (this.pomodoro) this.advancePomodoro();
        else this.persist();
      }
    }
    const totalSeconds = Math.ceil(this.remainingMillis / 1000);
    this.display.textContent = this.pomodoro
      ? `${twoDigits(Math.floor(totalSeconds / 60))}:${twoDigits(totalSeconds % 60)}`
      : `${twoDigits(Math.floor(totalSeconds / 3600))}:${twoDigits(
          Math.floor(totalSeconds / 60) % 60
        )}:${twoDigits(totalSeconds % 60)}`;
    if (this.pomodoro) this.phase.textContent = this.phaseLabel();
    this.startPause.textContent = t(this.running ? 'timer_pause' : 'timer_start');
  }

  private onFinished(): void {
    playTimerTone();
    vibrate([200, 120, 200]);
    notify(t(this.pomodoro ? 'pomodoro_complete' : 'countdown_complete'), t('timer_complete_open'));
  }

  private phaseLabel(): string {
    if (this.pomodoroPhase === 0) return t('pomodoro_focus');
    return t(this.pomodoroPhase === 1 ? 'pomodoro_short_break' : 'pomodoro_long_break');
  }

  /** Hours/minutes/seconds entry, replacing the three NumberPickers. */
  private showCustomDialog(): void {
    const dialog = document.createElement('dialog');
    dialog.className = 'timer-dialog';
    const heading = document.createElement('h2');
    heading.textContent = t('timer_custom_title');

    const totalSeconds = Math.min(Math.floor(this.durationMillis / 1000), 99 * 3600 + 59 * 60 + 59);
    const fields = [
      { label: t('timer_hours'), max: 99, value: Math.floor(totalSeconds / 3600) },
      { label: t('timer_minutes_unit'), max: 59, value: Math.floor(totalSeconds / 60) % 60 },
      { label: t('timer_seconds'), max: 59, value: totalSeconds % 60 },
    ].map((field) => {
      const wrapper = document.createElement('label');
      wrapper.className = 'timer-dialog-field';
      const caption = document.createElement('span');
      caption.textContent = field.label;
      const input = document.createElement('input');
      input.type = 'number';
      input.min = '0';
      input.max = String(field.max);
      input.value = String(field.value);
      input.inputMode = 'numeric';
      wrapper.append(caption, input);
      return { wrapper, input };
    });

    const row = document.createElement('div');
    row.className = 'timer-dialog-row';
    row.append(...fields.map((field) => field.wrapper));

    const error = document.createElement('p');
    error.className = 'timer-dialog-error';
    error.hidden = true;
    error.textContent = t('timer_custom_zero');

    const actions = document.createElement('div');
    actions.className = 'timer-dialog-actions';
    const cancel = document.createElement('button');
    cancel.className = 'button button--text';
    cancel.textContent = t('cancel');
    cancel.addEventListener('click', () => dialog.close());
    const apply = document.createElement('button');
    apply.className = 'button';
    apply.textContent = t('apply');
    apply.addEventListener('click', () => {
      const duration = customDurationMillis(
        Number(fields[0].input.value),
        Number(fields[1].input.value),
        Number(fields[2].input.value)
      );
      if (duration === 0) {
        error.hidden = false;
        return;
      }
      this.setDuration(duration);
      dialog.close();
    });
    actions.append(cancel, apply);

    dialog.append(heading, row, error, actions);
    dialog.addEventListener('close', () => dialog.remove());
    document.body.appendChild(dialog);
    dialog.showModal();
  }

  private restore(): void {
    this.durationMillis = readNumber(
      `${this.prefix}duration`,
      this.pomodoro ? POMODORO_DURATIONS[0] : 10 * 60_000
    );
    this.remainingMillis = readNumber(`${this.prefix}remaining`, this.durationMillis);
    this.deadlineMillis = readNumber(`${this.prefix}deadline`, 0);
    this.running = readString(`${this.prefix}running`) === 'true';
    this.pomodoroPhase = readNumber(`${this.prefix}phase`, 0);
  }

  private persist(): void {
    writeValue(`${this.prefix}duration`, this.durationMillis);
    writeValue(`${this.prefix}remaining`, this.remainingMillis);
    writeValue(`${this.prefix}deadline`, this.deadlineMillis);
    writeValue(`${this.prefix}running`, this.running);
    writeValue(`${this.prefix}phase`, this.pomodoroPhase);
  }
}

function readString(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function readNumber(key: string, fallback: number): number {
  const value = readString(key);
  if (value === null) return fallback;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function writeValue(key: string, value: number | boolean): void {
  try {
    localStorage.setItem(key, String(value));
  } catch {
    /* storage unavailable */
  }
}
