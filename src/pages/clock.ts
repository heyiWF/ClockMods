/**
 * The full-screen clock.
 *
 * Ported from com.clockmods.ui.ClockView + ProClockFragment. The per-second tick,
 * the size-fitting arithmetic, the landscape/portrait/stacked layouts and the
 * weather line all follow the original; drawing is DOM instead of canvas.
 */
import { prefs, MODE_IMAGE, cssColor } from '../core/prefs';
import { dateLang, t } from '../core/i18n';
import { ensureFontLoaded, fontStack } from '../core/fonts';
import { isDimScheduleActive } from '../core/dim-schedule';
import { millisUntilNextSecond, timeSource } from '../core/time-source';
import { minutesOfDay, zonedFields } from '../core/zoned-time';
import { format as formatDate } from '../format/date-formatter';
import { formatTime, hasPeriod, hasSmallSeconds } from '../format/time-formatter';
import {
  calculateWidthBasedTextSize,
  invalidateMeasurements,
  measureAtOnePixel,
  measureSupportingText,
  stableTextWidth,
  widestDigitWidth,
} from '../format/text-metrics';
import type { FontSpec } from '../format/text-metrics';
import { lunarClockLine } from '../lunar/lunar';
import { backgroundImageUrl } from '../core/background-store';
import { CharacterLine, measureColonShift, renderSupportingText } from '../ui/clock-face';
import { Carousel, plainItem, weatherItem } from '../ui/carousel';
import type { CarouselItem } from '../ui/carousel';
import { WeatherController } from '../weather/controller';
import { detailCarouselItems, locationText } from '../weather/models';
import type { WeatherState } from '../weather/models';
import type { Page } from '../app/router';

/** Fractions the main time size is fitted to (ClockView.TIME_*). */
const TIME_HEIGHT_FRACTION = 0.55;
const TIME_MAX_WIDTH_FRACTION = 0.98;
const DATE_HEIGHT_FRACTION = 0.14;
const DATE_MAX_WIDTH_FRACTION = 0.92;
const SUPPORTING_LETTER_SPACING = 0.025;
/** Space between the main time and small seconds, as a fraction of a space glyph. */
const SMALL_SECONDS_GAP_SPACE_FRACTION = 0.35;
/** Approximate CSS line box height, used where Android read font metrics. */
const LINE_HEIGHT = 1.2;

export class ClockPage implements Page {
  readonly name = 'clock';
  readonly immersive = true;

  private readonly root: HTMLElement;
  private readonly background: HTMLElement;
  private readonly dim: HTMLElement;
  private readonly stage: HTMLElement;
  private readonly dateRow: HTMLElement;
  private readonly lunarRow: HTMLElement;
  private readonly timeRow: HTMLElement;
  private readonly attribution: HTMLElement;

  /** Inline layout: the AM/PM prefix, the main time and the small seconds. */
  private mainLine: CharacterLine | null = null;
  private periodLine: CharacterLine | null = null;
  private secondsLine: CharacterLine | null = null;
  /** Stacked portrait layout: one line per hours/minutes/seconds group. */
  private readonly stackedLines: CharacterLine[] = [];
  private readonly weather: Carousel;
  private readonly detail: Carousel;
  private readonly weatherController: WeatherController;

  private tickHandle: number | null = null;
  private running = false;
  private weatherState: WeatherState | null = null;
  /** Layout inputs whose change forces a re-fit. */
  private layoutSignature = '';
  private lastLayoutSize = '';

  constructor(root: HTMLElement, private readonly onOpenSettings: () => void) {
    this.root = root;
    this.background = root.querySelector('#clock-bg')!;
    this.dim = root.querySelector('#clock-dim')!;
    this.stage = root.querySelector('#clock-stage')!;
    this.dateRow = root.querySelector('#clock-date')!;
    this.lunarRow = root.querySelector('#clock-lunar')!;
    this.timeRow = root.querySelector('#clock-time')!;
    this.attribution = root.querySelector('#clock-attribution')!;

    this.weather = new Carousel(root.querySelector('#clock-weather')!);
    this.detail = new Carousel(root.querySelector('#clock-detail')!);
    this.weatherController = new WeatherController((state) => this.onWeatherState(state));

    this.bindGestures();
    new ResizeObserver(() => this.layout(true)).observe(this.stage);
  }

  /** Double-tapping the clock opens settings (ProClockFragment.onDoubleTap). */
  private bindGestures(): void {
    let lastTap = 0;
    this.stage.addEventListener('pointerup', (event) => {
      if (event.pointerType === 'mouse' && event.button !== 0) return;
      const now = performance.now();
      if (now - lastTap < 320) {
        lastTap = 0;
        this.onOpenSettings();
      } else {
        lastTap = now;
      }
    });
    this.stage.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        this.onOpenSettings();
      }
    });
    this.stage.setAttribute('aria-label', t('open_settings_accessibility'));
  }

  start(): void {
    if (this.running) return;
    this.running = true;
    this.weather.setActive(true);
    this.detail.setActive(true);
    this.tick();
    this.startWeatherIfEnabled();
  }

  stop(): void {
    this.running = false;
    if (this.tickHandle !== null) {
      clearTimeout(this.tickHandle);
      this.tickHandle = null;
    }
    this.weather.setActive(false);
    this.detail.setActive(false);
    this.weatherController.stop();
  }

  /** Re-reads every setting; called after the settings panel applies changes. */
  async refreshSettings(): Promise<void> {
    await ensureFontLoaded(prefs.getFontFamily(), prefs.isBoldText());
    invalidateMeasurements();
    this.mainLine?.reset();
    this.periodLine?.reset();
    this.secondsLine?.reset();
    for (const line of this.stackedLines) line.reset();
    this.applyStyles();
    await this.applyBackground();
    this.layoutSignature = '';
    this.render();
    const enabled = prefs.isWeatherEnabled();
    const attributionText = t('weather_attribution');
    this.attribution.setAttribute('aria-label', attributionText);
    const attributionLabel = this.attribution.querySelector<HTMLElement>(
      '.weather-attribution-label'
    );
    if (attributionLabel) attributionLabel.textContent = attributionText;
    this.attribution.hidden = !enabled;
    if (!enabled) {
      this.weatherController.stop();
      this.weatherState = null;
    } else if (this.running) {
      this.startWeatherIfEnabled();
    }
    // Rebuild the supporting lines either way: the custom message shows there
    // even with weather switched off.
    this.updateWeatherLines();
  }

  private startWeatherIfEnabled(): void {
    if (!prefs.isWeatherEnabled()) return;
    this.attribution.hidden = false;
    this.weatherController.start(prefs.getWeatherIntervalMinutes());
  }

  private onWeatherState(state: WeatherState): void {
    this.weatherState = state;
    this.updateWeatherLines();
  }

  private applyStyles(): void {
    const style = this.stage.style;
    style.setProperty('--clock-font', fontStack(prefs.getFontFamily()));
    style.setProperty('--clock-weight', prefs.isBoldText() ? '700' : '400');
    style.setProperty('--time-color', cssColor(prefs.getTimeColor()));
    style.setProperty('--date-color', cssColor(prefs.getDateColor()));
    style.setProperty('--supporting-tracking', `${SUPPORTING_LETTER_SPACING}em`);
    style.setProperty(
      '--weather-icon-color',
      prefs.isWeatherIconDynamicColor() ? 'var(--accent)' : '#ffffff'
    );
    this.weather.setIconStyle(prefs.isWeatherIconFill());
    this.detail.setIconStyle(prefs.isWeatherIconFill());
    // Pro drives the detail line with the clock's transition style.
    this.weather.setTransition(prefs.getTimeTransition());
    this.detail.setTransition(prefs.getTimeTransition());
  }

  async applyBackground(): Promise<void> {
    const useImage = prefs.getBackgroundMode() === MODE_IMAGE;
    this.background.style.backgroundColor = cssColor(prefs.getBackgroundColor());
    if (!useImage) {
      this.background.style.removeProperty('background-image');
      return;
    }
    const url = await backgroundImageUrl();
    if (url) this.background.style.backgroundImage = `url("${url}")`;
    else this.background.style.removeProperty('background-image');
  }

  /** Schedules the next tick on the same clock the face draws from. */
  private tick(): void {
    this.render();
    if (!this.running) return;
    const now = timeSource.now();
    this.tickHandle = window.setTimeout(() => this.tick(), millisUntilNextSecond(now));
  }

  private render(): void {
    const now = timeSource.now();
    const fields = zonedFields(now, prefs.getTimeZoneId());
    const showSeconds = prefs.isShowSeconds();
    const smallSeconds = prefs.isSmallSeconds();
    const use24Hour = prefs.isUse24Hour();
    const english = prefs.isClockUseEnglish();
    const stacked = prefs.isPortraitStacked() && this.isPortrait();

    const displayTime = formatTime(
      fields.hour,
      fields.minute,
      fields.second,
      showSeconds,
      prefs.isBlinkColon(),
      smallSeconds,
      use24Hour,
      english
    );
    const dateText = formatDate(
      english ? prefs.getDatePatternEn() : prefs.getDatePatternCn(),
      fields,
      // Read the language from preferences rather than the i18n module's cached
      // value, so the date never renders half-translated if the two drift.
      dateLang(prefs.getClockLanguage())
    );
    const lunarText = prefs.isShowLunar()
      ? lunarClockLine(fields.year, fields.month0, fields.day)
      : '';

    this.applyDim(fields);

    const options = {
      animate: prefs.isAnimateTimeChanges(),
      transition: prefs.getTimeTransition(),
      colonVisible: displayTime.colonVisible,
    };

    this.root.classList.toggle('is-stacked', stacked);
    if (stacked) {
      this.renderStacked(fields, use24Hour, showSeconds, options);
    } else {
      this.renderInline(displayTime, options);
    }

    // Portrait always stacks date and lunar; landscape shares one line unless the
    // user opts into two rows.
    const singleDateLine = !lunarText || (!this.isPortrait() && !prefs.isDateLunarDualLine());
    if (!lunarText) {
      renderSupportingText(this.dateRow, dateText);
      this.dateRow.hidden = false;
      this.lunarRow.hidden = true;
    } else if (singleDateLine) {
      renderSupportingText(this.dateRow, `${dateText} ${lunarText}`);
      this.dateRow.hidden = false;
      this.lunarRow.hidden = true;
    } else {
      renderSupportingText(this.dateRow, dateText);
      renderSupportingText(this.lunarRow, lunarText);
      this.dateRow.hidden = false;
      this.lunarRow.hidden = false;
    }

    this.layout(false, {
      displayTime,
      dateText,
      lunarText,
      singleDateLine,
      stacked,
      showSeconds,
    });
  }

  private renderInline(
    displayTime: ReturnType<typeof formatTime>,
    options: { animate: boolean; transition: string; colonVisible: boolean }
  ): void {
    this.timeRow.classList.remove('is-stacked');
    const period = hasPeriod(displayTime);
    const seconds = hasSmallSeconds(displayTime);
    this.timeRow.classList.toggle('has-period', period);
    this.timeRow.classList.toggle('has-small-seconds', seconds);

    let main = this.timeRow.querySelector<HTMLElement>('.time-main');
    if (!main) {
      this.timeRow.replaceChildren();
      const periodSpan = document.createElement('span');
      periodSpan.className = 'time-period';
      main = document.createElement('span');
      main.className = 'time-main';
      const secondsSpan = document.createElement('span');
      secondsSpan.className = 'time-seconds';
      this.timeRow.append(periodSpan, main, secondsSpan);
      this.mainLine = new CharacterLine(main);
      this.periodLine = new CharacterLine(periodSpan);
      this.secondsLine = new CharacterLine(secondsSpan);
    }
    this.mainLine!.setText(displayTime.mainText, options);
    this.periodLine!.setText(period ? displayTime.periodText : '', {
      ...options,
      colonVisible: true,
    });
    this.secondsLine!.setText(seconds ? displayTime.secondsText : '', {
      ...options,
      colonVisible: true,
    });
  }

  /**
   * Portrait "stacked" layout: hours, minutes and optionally seconds as large
   * digits on separate lines, with no colon (ClockView.drawStackedPortrait).
   */
  private renderStacked(
    fields: ReturnType<typeof zonedFields>,
    use24Hour: boolean,
    showSeconds: boolean,
    options: { animate: boolean; transition: string; colonVisible: boolean }
  ): void {
    const lineCount = showSeconds ? 3 : 2;
    if (this.stackedLines.length !== lineCount || !this.timeRow.classList.contains('is-stacked')) {
      this.timeRow.classList.add('is-stacked');
      this.timeRow.replaceChildren();
      this.stackedLines.length = 0;
      this.mainLine = null;
      for (let index = 0; index < lineCount; index++) {
        const row = document.createElement('span');
        row.className = 'stacked-row';
        this.timeRow.appendChild(row);
        this.stackedLines.push(new CharacterLine(row));
      }
    }
    let displayHour = use24Hour ? fields.hour : fields.hour % 12;
    if (!use24Hour && displayHour === 0) displayHour = 12;
    const values = [pad(displayHour), pad(fields.minute)];
    if (showSeconds) values.push(pad(fields.second));
    const stackedOptions = { ...options, colonVisible: true };
    for (let index = 0; index < this.stackedLines.length; index++) {
      this.stackedLines[index].setText(values[index], stackedOptions);
    }
  }

  private applyDim(fields: ReturnType<typeof zonedFields>): void {
    if (prefs.getBackgroundMode() !== MODE_IMAGE) {
      this.dim.hidden = true;
      return;
    }
    if (prefs.isDimBackground()) {
      this.dim.hidden = false;
      return;
    }
    if (!prefs.isScheduleDimBackground()) {
      this.dim.hidden = true;
      return;
    }
    this.dim.hidden = !isDimScheduleActive(
      minutesOfDay(fields),
      prefs.getDimStartMinutes(),
      prefs.getDimEndMinutes()
    );
  }

  private updateWeatherLines(): void {
    const state = this.weatherState;
    const enabled = prefs.isWeatherEnabled();
    const message = prefs.getCustomMessage();
    const items: CarouselItem[] = [];

    if (enabled && state) {
      if (state.data) {
        const left = locationText(state.data.city, state.data.district);
        const right = `${state.data.text} ${state.data.temperature}℃`;
        items.push(weatherItem(left, state.data.icon, right));
      } else if (state.message) {
        items.push(plainItem(state.message));
      }
    }
    if (message) items.push(plainItem(message));
    this.weather.setItems(items);

    const detail = enabled && prefs.isWeatherDetailed() ? state?.data?.detail ?? null : null;
    this.detail.setItems(
      detail
        ? detailCarouselItems(detail, {
            feelsFormat: t('weather_feels_format'),
            humidityFormat: t('weather_humidity_format'),
            windScaleFormat: t('weather_wind_scale_format'),
            precipFormat: t('weather_precip_format'),
            airFormat: t('weather_air_format'),
            warningSuffix: t('weather_warning_suffix'),
          }).map(plainItem)
        : []
    );
    this.layoutSignature = '';
    this.layout(true);
  }

  private isPortrait(): boolean {
    return this.stage.clientHeight >= this.stage.clientWidth;
  }

  private font(bold = prefs.isBoldText()): FontSpec {
    return { family: fontStack(prefs.getFontFamily()), weight: bold ? 700 : 400 };
  }

  /**
   * Fits the time and date to the viewport with ClockLayoutCalculator's
   * arithmetic, then publishes the results as CSS variables.
   */
  private layout(
    force: boolean,
    context?: {
      displayTime: ReturnType<typeof formatTime>;
      dateText: string;
      lunarText: string;
      singleDateLine: boolean;
      stacked: boolean;
      showSeconds: boolean;
    }
  ): void {
    const width = this.stage.clientWidth;
    const height = this.stage.clientHeight;
    if (width === 0 || height === 0) return;
    const size = `${width}x${height}`;
    if (force) this.lastLayoutSize = '';
    if (!context) {
      if (size === this.lastLayoutSize) return;
      // A resize with no fresh text: re-render, which calls back with context.
      this.lastLayoutSize = size;
      this.render();
      return;
    }
    const signature = [
      size,
      context.displayTime.mainText.length,
      context.displayTime.periodText,
      context.displayTime.secondsText.length,
      context.dateText,
      context.lunarText,
      context.singleDateLine,
      context.stacked,
      context.showSeconds,
      prefs.getFontFamily(),
      prefs.isBoldText(),
      prefs.getTimeFontScale(),
      prefs.getDateFontScale(),
      this.weather.isEmpty,
      this.detail.isEmpty,
    ].join('|');
    if (!force && signature === this.layoutSignature) return;
    this.layoutSignature = signature;
    this.lastLayoutSize = size;

    const font = this.font();
    const portrait = this.isPortrait();
    const style = this.stage.style;
    style.setProperty('--colon-shift', `${measureColonShift(font.family, font.weight)}em`);

    // Date size first: the stacked layout caps the digits against it.
    const widestDate =
      context.singleDateLine || !context.lunarText
        ? context.dateText + (context.lunarText && context.singleDateLine ? ` ${context.lunarText}` : '')
        : longerOf(context.dateText, context.lunarText);
    const dateWidth = measureSupportingText(widestDate, font, SUPPORTING_LETTER_SPACING);
    const dateSize = calculateWidthBasedTextSize(
      width,
      height,
      dateWidth,
      prefs.getDateFontScale(),
      context.stacked ? 0.08 : DATE_HEIGHT_FRACTION,
      DATE_MAX_WIDTH_FRACTION
    );
    style.setProperty('--date-size', `${dateSize}px`);

    const timeSize = context.stacked
      ? this.stackedTimeSize(width, height, dateSize, context.showSeconds, font)
      : this.inlineTimeSize(width, height, context.displayTime, font);
    style.setProperty('--time-size', `${timeSize}px`);
    style.setProperty('--digit-w', `${widestDigitWidth(font) * timeSize}px`);

    // Gaps: ClockView derives them from the date size, wider in portrait.
    const gapFactor = portrait ? 0.9 : 0.35;
    const gap = Math.max(portrait ? 32 : 12, dateSize * gapFactor);
    style.setProperty('--time-gap', `${gap}px`);
    // The date<->lunar and weather<->detail gaps are deliberately identical.
    const supportingGap = Math.max(portrait ? 10 : 6, dateSize * (portrait ? 0.5 : 0.35));
    style.setProperty('--supporting-gap', `${supportingGap}px`);
  }

  private inlineTimeSize(
    width: number,
    height: number,
    displayTime: ReturnType<typeof formatTime>,
    font: FontSpec
  ): number {
    const spaceWidth = measureAtOnePixel(' ', font) * SMALL_SECONDS_GAP_SPACE_FRACTION;
    const mainWidth = stableTextWidth(displayTime.mainText, font);
    const leftAccessory = hasPeriod(displayTime)
      ? spaceWidth + measureAtOnePixel(displayTime.periodText, font) * 0.3
      : 0;
    const rightAccessory = hasSmallSeconds(displayTime)
      ? spaceWidth + stableTextWidth(displayTime.secondsText, font) * 0.6
      : 0;
    return calculateWidthBasedTextSize(
      width,
      height,
      mainWidth + leftAccessory + rightAccessory,
      prefs.getTimeFontScale(),
      TIME_HEIGHT_FRACTION,
      TIME_MAX_WIDTH_FRACTION
    );
  }

  /**
   * Stacked digits are fitted to a two-digit group, capped by a per-line height
   * fraction, then capped again so date + gaps + digits + weather always fit.
   */
  private stackedTimeSize(
    width: number,
    height: number,
    dateSize: number,
    showSeconds: boolean,
    font: FontSpec
  ): number {
    const lines = showSeconds ? 3 : 2;
    const weatherTwoLines = prefs.isWeatherEnabled() && prefs.isWeatherDetailed();
    const heightFraction =
      lines === 3 ? (weatherTwoLines ? 0.13 : 0.18) : weatherTwoLines ? 0.2 : 0.28;
    const digitPairWidth = stableTextWidth('00', font);
    let lineSize = calculateWidthBasedTextSize(
      width,
      height,
      digitPairWidth,
      prefs.getTimeFontScale(),
      heightFraction,
      0.66
    );

    const dateLineHeight = dateSize * LINE_HEIGHT;
    const hasLunar = prefs.isShowLunar();
    const weatherShown = prefs.isWeatherEnabled() || prefs.getCustomMessage().length > 0;
    const supportingGap = Math.max(dateLineHeight * 0.5, 10);
    const dateBlockHeight = hasLunar ? dateLineHeight * 2 + supportingGap : dateLineHeight;
    const weatherBlockHeight = !weatherShown
      ? 0
      : weatherTwoLines
        ? dateLineHeight * 2 + supportingGap
        : dateLineHeight;
    const available = height * 0.88; // the 6%/94% safe area
    const minGap = dateLineHeight * 0.25;
    const blockFactor = (lines - 1) * 1.06 + 0.72;
    const maxBlockHeight =
      available - dateBlockHeight - minGap - (weatherShown ? weatherBlockHeight + minGap : 0);
    if (maxBlockHeight > 0) lineSize = Math.min(lineSize, maxBlockHeight / blockFactor);
    return Math.max(1, lineSize);
  }
}

const pad = (value: number): string => String(value).padStart(2, '0');
const longerOf = (a: string, b: string): string => (a.length >= b.length ? a : b);
