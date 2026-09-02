/**
 * The daily alarm.
 *
 * Ported from com.clockmods.pro.ProAlarmFragment + AlarmScheduler/AlarmStore: one
 * repeating time with an on/off switch and a "next ring" line.
 *
 * Android scheduled an exact AlarmManager wake-up that fired with the app closed.
 * A web page cannot be woken once it is closed, so the alarm is armed with a
 * timer while the page lives and the limitation is stated in the UI rather than
 * pretended away.
 */
import { t } from '../core/i18n';
import { prefs } from '../core/prefs';
import { intlLocale } from '../core/i18n';
import { timeSource } from '../core/time-source';
import { twoDigits } from '../format/time-formatter';
import { notify, requestNotificationPermission } from '../ui/notifications';
import { primeAudio, startAlarmTone, vibrate } from '../ui/sound';
import type { Page } from '../app/router';

const STORE_HOUR = 'pro_alarm.hour';
const STORE_MINUTE = 'pro_alarm.minute';
const STORE_ENABLED = 'pro_alarm.enabled';

/** Next occurrence of hour:minute at or after `now` (AlarmScheduler.nextTrigger). */
export function nextTrigger(hour: number, minute: number, now: number): number {
  const trigger = new Date(now);
  trigger.setHours(hour, minute, 0, 0);
  if (trigger.getTime() <= now) trigger.setDate(trigger.getDate() + 1);
  return trigger.getTime();
}

export class AlarmPage implements Page {
  readonly name = 'alarm';
  readonly immersive = false;

  private readonly root: HTMLElement;
  private readonly ringingLayer: HTMLElement;
  private title!: HTMLElement;
  private timeView!: HTMLElement;
  private nextView!: HTMLElement;
  private toggle!: HTMLInputElement;
  private toggleLabel!: HTMLElement;
  private editButton!: HTMLButtonElement;
  private note!: HTMLElement;
  private timeInput!: HTMLInputElement;

  private hour = 7;
  private minute = 30;
  private enabled = false;
  private timer: number | null = null;
  private stopTone: (() => void) | null = null;

  constructor(root: HTMLElement) {
    this.root = root;
    this.ringingLayer = document.getElementById('alarm-layer')!;
    this.restore();
    this.build();
    this.render();
  }

  private build(): void {
    this.root.classList.add('timer-page');
    this.root.innerHTML = `
      <div class="timer alarm">
        <h1 class="timer-title"></h1>
        <div class="alarm-time"></div>
        <div class="alarm-next"></div>
        <label class="alarm-toggle">
          <input type="checkbox" role="switch" />
          <span></span>
        </label>
        <button type="button" class="m3-button m3-button--outlined alarm-edit"></button>
        <input type="time" class="alarm-time-input" hidden />
        <p class="alarm-note"></p>
      </div>`;
    this.title = this.root.querySelector('.timer-title')!;
    this.timeView = this.root.querySelector('.alarm-time')!;
    this.nextView = this.root.querySelector('.alarm-next')!;
    this.toggle = this.root.querySelector('.alarm-toggle input')!;
    this.toggleLabel = this.root.querySelector('.alarm-toggle span')!;
    this.editButton = this.root.querySelector('.alarm-edit')!;
    this.note = this.root.querySelector('.alarm-note')!;
    this.timeInput = this.root.querySelector('.alarm-time-input')!;

    this.toggle.addEventListener('change', async () => {
      this.enabled = this.toggle.checked;
      this.persist();
      if (this.enabled) {
        primeAudio();
        await requestNotificationPermission();
        this.schedule();
      } else {
        this.clearTimer();
      }
      this.render();
    });

    // A native time input gives every platform its own familiar picker.
    this.editButton.addEventListener('click', () => {
      this.timeInput.value = `${twoDigits(this.hour)}:${twoDigits(this.minute)}`;
      this.timeInput.showPicker?.();
      this.timeInput.click();
    });
    this.timeInput.addEventListener('change', async () => {
      const [hour, minute] = this.timeInput.value.split(':').map(Number);
      if (!Number.isFinite(hour) || !Number.isFinite(minute)) return;
      this.hour = hour;
      this.minute = minute;
      this.enabled = true;
      this.toggle.checked = true;
      this.persist();
      primeAudio();
      await requestNotificationPermission();
      this.schedule();
      this.render();
    });
  }

  start(): void {
    if (this.enabled) this.schedule();
    this.render();
  }

  stop(): void {
    // The alarm keeps its timer while other pages are shown; only a page teardown
    // would clear it, and there is none in this single-document app.
  }

  refreshSettings(): void {
    this.title.textContent = t('pro_page_alarm');
    this.toggleLabel.textContent = t('alarm_enabled');
    this.editButton.textContent = t('alarm_choose_time');
    this.note.textContent = t('alarm_background_note');
    this.render();
  }

  private render(): void {
    this.timeView.textContent = `${twoDigits(this.hour)}:${twoDigits(this.minute)}`;
    this.toggle.checked = this.enabled;
    if (!this.enabled) {
      this.nextView.textContent = t('alarm_disabled');
      return;
    }
    const trigger = nextTrigger(this.hour, this.minute, timeSource.now());
    const formatted = new Intl.DateTimeFormat(intlLocale(), {
      dateStyle: 'short',
      timeStyle: 'short',
      hour12: !prefs.isUse24Hour(),
    }).format(new Date(trigger));
    this.nextView.textContent = t('alarm_next_trigger', formatted);
  }

  private schedule(): void {
    this.clearTimer();
    if (!this.enabled) return;
    const delay = nextTrigger(this.hour, this.minute, timeSource.now()) - timeSource.now();
    // setTimeout saturates past ~24.8 days, well beyond the one-day maximum here.
    this.timer = window.setTimeout(() => this.ring(), Math.max(0, delay));
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  private ring(): void {
    notify(t('alarm_ringing'), t('alarm_dismiss'));
    vibrate([500, 300, 500, 300, 500]);
    this.stopTone = startAlarmTone();
    this.showRingingOverlay();
    // Re-arm for the next day, as the Android receiver did after firing.
    this.schedule();
    this.render();
  }

  private showRingingOverlay(): void {
    const layer = this.ringingLayer;
    layer.hidden = false;
    layer.innerHTML = '';
    const panel = document.createElement('div');
    panel.className = 'alarm-ringing';
    const time = document.createElement('div');
    time.className = 'alarm-ringing-time';
    time.textContent = `${twoDigits(this.hour)}:${twoDigits(this.minute)}`;
    const label = document.createElement('div');
    label.className = 'alarm-ringing-label';
    label.textContent = t('alarm_ringing');
    const dismiss = document.createElement('button');
    dismiss.className = 'm3-button m3-button--filled alarm-ringing-dismiss';
    dismiss.textContent = t('alarm_dismiss');
    dismiss.addEventListener('click', () => this.dismiss());
    panel.append(time, label, dismiss);
    layer.appendChild(panel);
  }

  private dismiss(): void {
    this.stopTone?.();
    this.stopTone = null;
    this.ringingLayer.hidden = true;
    this.ringingLayer.replaceChildren();
  }

  private restore(): void {
    try {
      this.hour = Number(localStorage.getItem(STORE_HOUR) ?? 7);
      this.minute = Number(localStorage.getItem(STORE_MINUTE) ?? 30);
      this.enabled = localStorage.getItem(STORE_ENABLED) === 'true';
    } catch {
      /* defaults already set */
    }
  }

  private persist(): void {
    try {
      localStorage.setItem(STORE_HOUR, String(this.hour));
      localStorage.setItem(STORE_MINUTE, String(this.minute));
      localStorage.setItem(STORE_ENABLED, String(this.enabled));
    } catch {
      /* storage unavailable */
    }
  }
}
