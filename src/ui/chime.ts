/**
 * The hourly visual chime.
 *
 * Ported from com.clockmods.pro.chime.HourlyChimeController + RadialChimeView: a
 * 5s burst that expands a filled circle from the centre, fades the upcoming hour
 * in over it, then fades out — armed in the last two seconds before the hour and
 * skipped during quiet hours (which may cross midnight).
 */
import { prefs } from '../core/prefs';
import { fontStack } from '../core/fonts';
import { timeSource } from '../core/time-source';
import { minutesOfDay, zonedFields } from '../core/zoned-time';
import type { ZonedFields } from '../core/zoned-time';
import { formatHourlyChime, formatTime } from '../format/time-formatter';
import {
  calculateWidthBasedTextSize,
  measureAtOnePixel,
  stableTextWidth,
} from '../format/text-metrics';

/** Matches RadialChimeView.DURATION. */
const DURATION_MS = 5000;
const CHECK_INTERVAL_MS = 250;
/** Same fractions the clock fits its time to (ClockView.TIME_*). */
const TIME_HEIGHT_FRACTION = 0.55;
const TIME_MAX_WIDTH_FRACTION = 0.98;

/**
 * The instant of the upcoming chime, or null outside the two-second arming
 * window (HourlyChimeController.upcomingChimeAtMillis).
 */
export function upcomingChimeAt(fields: ZonedFields, nowMillis: number): number | null {
  if (fields.minute !== 59 || fields.second < 58) return null;
  // Round up to the next hour boundary. Zone offsets are whole minutes in every
  // zone still in use, so the sub-hour part can be derived from the fields.
  const millisIntoHour = (fields.minute * 60 + fields.second) * 1000 + (nowMillis % 1000);
  return nowMillis - millisIntoHour + 3600_000;
}

/** Whether `fields` falls inside the configured quiet window. */
export function isQuietHour(fields: ZonedFields): boolean {
  if (!prefs.isHourlyChimeQuietEnabled()) return false;
  const current = minutesOfDay(fields);
  const start = prefs.getHourlyChimeQuietStart();
  const end = prefs.getHourlyChimeQuietEnd();
  if (start === end) return true;
  return start < end ? current >= start && current < end : current >= start || current < end;
}

export class HourlyChime {
  private readonly layer: HTMLElement;
  private checkHandle: number | null = null;
  private lastChimeAt = Number.NEGATIVE_INFINITY;
  private hideHandle: number | null = null;

  constructor(layer: HTMLElement) {
    this.layer = layer;
    this.layer.addEventListener('click', () => this.hide());
  }

  start(): void {
    if (this.checkHandle !== null) return;
    this.checkHandle = window.setInterval(() => this.check(), CHECK_INTERVAL_MS);
  }

  stop(): void {
    if (this.checkHandle !== null) {
      clearInterval(this.checkHandle);
      this.checkHandle = null;
    }
    this.hide();
  }

  private check(): void {
    if (!prefs.isHourlyChimeEnabled()) return;
    const now = timeSource.now();
    const zone = prefs.getTimeZoneId();
    const fields = zonedFields(now, zone);
    const chimeAt = upcomingChimeAt(fields, now);
    if (chimeAt === null) return;
    const chimeFields = zonedFields(chimeAt, zone);
    if (isQuietHour(chimeFields) || chimeAt === this.lastChimeAt) return;
    this.lastChimeAt = chimeAt;
    this.show(chimeFields);
  }

  private show(fields: ZonedFields): void {
    const use24Hour = prefs.isUse24Hour();
    const english = prefs.isClockUseEnglish();
    const text = formatHourlyChime(fields.hour, fields.minute, use24Hour, english);

    this.layer.hidden = false;
    this.layer.replaceChildren();
    const circle = document.createElement('div');
    circle.className = 'chime-circle';
    const label = document.createElement('div');
    label.className = 'chime-text';
    label.textContent = text;
    label.style.fontFamily = fontStack(prefs.getFontFamily());
    label.style.fontWeight = prefs.isBoldText() ? '700' : '400';
    label.style.fontSize = `${this.resolveTextSize(fields, text)}px`;
    this.layer.append(circle, label);

    if (this.hideHandle !== null) clearTimeout(this.hideHandle);
    this.hideHandle = window.setTimeout(() => this.hide(), DURATION_MS);
  }

  /**
   * The exact size the clock is rendering its time at, capped so the chime string
   * never spills past the same width fraction (RadialChimeView.resolveBaseTextSize).
   */
  private resolveTextSize(fields: ZonedFields, text: string): number {
    const width = this.layer.clientWidth || window.innerWidth;
    const height = this.layer.clientHeight || window.innerHeight;
    const font = {
      family: fontStack(prefs.getFontFamily()),
      weight: prefs.isBoldText() ? 700 : 400,
    };
    const clockTime = formatTime(
      fields.hour,
      fields.minute,
      fields.second,
      prefs.isShowSeconds(),
      prefs.isBlinkColon(),
      prefs.isSmallSeconds(),
      prefs.isUse24Hour(),
      prefs.isClockUseEnglish()
    );
    let size = calculateWidthBasedTextSize(
      width,
      height,
      stableTextWidth(clockTime.mainText, font),
      prefs.getTimeFontScale(),
      TIME_HEIGHT_FRACTION,
      TIME_MAX_WIDTH_FRACTION
    );
    const measured = measureAtOnePixel(text, font) * size;
    const maxWidth = width * TIME_MAX_WIDTH_FRACTION;
    if (measured > maxWidth) size *= maxWidth / measured;
    return size;
  }

  private hide(): void {
    if (this.hideHandle !== null) {
      clearTimeout(this.hideHandle);
      this.hideHandle = null;
    }
    this.layer.hidden = true;
    this.layer.replaceChildren();
  }
}
