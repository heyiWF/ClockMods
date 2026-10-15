import { drawChimeEffect, easeOutQuint, smoothStep } from './chime-effects';
import { CharacterLine, measureColonShift } from './clock-face';
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
export function upcomingChimeAt(fields: ZonedFields, nowMillis: number, halfHour = false): number | null {
  if ((fields.minute !== 59 && !(halfHour && fields.minute === 29)) || fields.second < 58) return null;
  // Round up to the next hour boundary. Zone offsets are whole minutes in every
  // zone still in use, so the sub-hour part can be derived from the fields.
  const millisIntoHour = (fields.minute * 60 + fields.second) * 1000 + (nowMillis % 1000);
  return nowMillis - millisIntoHour + (fields.minute === 29 ? 1800_000 : 3600_000);
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
  private frame: number | null = null;

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
    const options = prefs.getUltimateOptions();
    if (!prefs.isHourlyChimeEnabled() && !options.halfHourChime) return;
    const now = timeSource.now();
    const zone = prefs.getTimeZoneId();
    const fields = zonedFields(now, zone);
    const chimeAt = upcomingChimeAt(fields, now, options.halfHourChime);
    if (chimeAt === null) return;
    if (fields.minute === 59 && !prefs.isHourlyChimeEnabled()) return;
    const chimeFields = zonedFields(chimeAt, zone);
    if (isQuietHour(chimeFields) || chimeAt === this.lastChimeAt) return;
    this.lastChimeAt = chimeAt;
    this.show(chimeFields);
  }

  private show(fields: ZonedFields): void {
    const use24Hour = prefs.isUse24Hour();
    const english = prefs.isClockUseEnglish();
    const text = formatHourlyChime(fields.hour, fields.minute, use24Hour, english);

    this.layer.dataset.animation = prefs.getUltimateOptions().chimeAnimation;
    this.layer.hidden = false;
    this.layer.replaceChildren();
    const canvas = document.createElement('canvas');canvas.className='chime-canvas';
    const label = document.createElement('div');
    label.className = 'chime-text';
    new CharacterLine(label).setText(text,{animate:false,transition:'fade',colonVisible:true});
    label.style.fontFamily = fontStack(prefs.getFontFamily());
    label.style.fontWeight = String(prefs.getFontWeight());
    label.style.fontSize = `${this.resolveTextSize(fields, text)}px`;
    label.style.setProperty('--digit-w','auto');label.style.setProperty('--colon-shift',measureColonShift(fontStack(prefs.getFontFamily()),prefs.getFontWeight())+'em');
    this.layer.append(canvas, label);
    const kind=prefs.getUltimateOptions().chimeAnimation,alternative=['aurora','orbit','comet'].includes(kind);
    label.style.animation='none';label.style.color=alternative?'white':'black';
    const started=performance.now(),reduced=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const draw=(time:number)=>{const width=this.layer.clientWidth||innerWidth,height=this.layer.clientHeight||innerHeight,dpr=Math.min(2,devicePixelRatio||1);
      if(canvas.width!==Math.round(width*dpr)||canvas.height!==Math.round(height*dpr)){canvas.width=Math.round(width*dpr);canvas.height=Math.round(height*dpr);}
      const ctx=canvas.getContext('2d');const progress=Math.min(1,(time-started)/DURATION_MS);
      if(ctx){ctx.setTransform(dpr,0,0,dpr,0,0);drawChimeEffect(ctx,width,height,reduced?.5:progress,kind);}
      const enter=reduced?1:smoothStep(alternative?.13:.28,alternative?.31:.48,progress);
      label.style.opacity=String(enter*(1-smoothStep(.82,1,progress)));label.style.transform='scale('+(.94+.06*easeOutQuint(enter))+')';
      if(progress<1)this.frame=requestAnimationFrame(draw);
    };
    if(this.frame!==null)cancelAnimationFrame(this.frame);this.frame=requestAnimationFrame(draw);

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
      weight: prefs.getFontWeight(),
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
    if(this.frame!==null){cancelAnimationFrame(this.frame);this.frame=null;}
    if (this.hideHandle !== null) {
      clearTimeout(this.hideHandle);
      this.hideHandle = null;
    }
    this.layer.hidden = true;
    this.layer.replaceChildren();
  }
}
