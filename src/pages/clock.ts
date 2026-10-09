import { WeatherAttribution } from '../ui/weather-attribution';
import { setAlignedTime } from '../ui/clock-face';
import { GaussianGlass } from '../ui/gaussian-glass';
import { UltimateFace } from '../ui/ultimate-face';
import { fitSupportingRows } from '../format/responsive-layout';
import { readableInk, temperature } from '../core/clock-themes';
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
  private glass: GaussianGlass | null = null;
  private glassUrl: string | null = null;
  private readonly resizeObserver: ResizeObserver;

  dispose(): void { this.stop(); this.resizeObserver.disconnect(); this.ultimateFace?.destroy(); this.mainLine?.reset(); this.periodLine?.reset(); this.secondsLine?.reset(); this.stackedLines.forEach(line => line.reset()); }
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
  private readonly attributionPopup: WeatherAttribution;

  /** Inline layout: the AM/PM prefix, the main time and the small seconds. */
  private mainLine: CharacterLine | null = null;
  private ultimateFace: UltimateFace | null = null;
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

  constructor(root: HTMLElement, private readonly onOpenSettings: () => void, private readonly settings: typeof prefs = prefs, private readonly imageSource = backgroundImageUrl) {
    this.root = root;
    this.background = root.querySelector('#clock-bg')!;
    this.dim = root.querySelector('#clock-dim')!;
    this.stage = root.querySelector('#clock-stage')!;
    this.dateRow = root.querySelector('#clock-date')!;
    this.lunarRow = root.querySelector('#clock-lunar')!;
    this.timeRow = root.querySelector('#clock-time')!;
    this.attribution = root.querySelector('#clock-attribution')!;
    this.attributionPopup = new WeatherAttribution(root, this.attribution);

    this.weather = new Carousel(root.querySelector('#clock-weather')!);
    this.detail = new Carousel(root.querySelector('#clock-detail')!);
    this.weatherController = new WeatherController((state) => this.onWeatherState(state));

    this.bindGestures();
    this.resizeObserver = new ResizeObserver(() => this.layout(true));
    this.resizeObserver.observe(this.stage);
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
    this.attributionPopup.setEnabled(this.settings.isWeatherEnabled());
    this.attributionPopup.start();
    this.weather.setActive(true);
    this.detail.setActive(true);
    this.tick();
    this.startWeatherIfEnabled();
  }

  stop(): void {
    this.running = false;
    this.attributionPopup.stop();
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
    await ensureFontLoaded(this.settings.getFontFamily(), this.settings.getFontWeight(), this.root.ownerDocument);
    invalidateMeasurements();
    this.ultimateFace?.destroy();
    this.ultimateFace = null;
    this.mainLine?.reset();
    this.periodLine?.reset();
    this.secondsLine?.reset();
    for (const line of this.stackedLines) line.reset();
    this.applyStyles();
    await this.applyBackground();
    this.layoutSignature = '';
    this.render();
    const enabled = this.settings.isWeatherEnabled();
    const attributionText = t('weather_attribution');
    this.attribution.setAttribute('aria-label', attributionText);
    const attributionLabel = this.attribution.querySelector<HTMLElement>(
      '.weather-attribution-label'
    );
    if (attributionLabel) attributionLabel.textContent = attributionText;
    this.attributionPopup.setEnabled(enabled);
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
    if (!this.settings.isWeatherEnabled()) return;
    this.weatherController.start(this.settings.getWeatherIntervalMinutes());
  }

  private onWeatherState(state: WeatherState): void {
    this.weatherState = state;
    this.updateWeatherLines();
  }

  private applyStyles(): void {
    const style = this.stage.style;
    const theme = this.settings.getClockTheme();
    const palette = this.settings.getThemePalette();
    this.root.dataset.clockTheme = theme;
    this.root.classList.toggle('is-material', theme !== 'classic');
    this.root.classList.toggle('has-card-shadow', this.settings.isCardShadow());
    style.setProperty('--theme-panel', palette.panel);
    style.setProperty('--theme-background', palette.background);
    style.setProperty('--theme-on-background', this.settings.isThemeAutoInk() ? readableInk(palette.background) : cssColor(this.settings.getTimeColor()));
    style.setProperty('--theme-accent', palette.accent);
    style.setProperty('--theme-ink', readableInk(palette.panel));
    style.setProperty('--theme-on-accent', this.settings.isThemeAutoInk() ? readableInk(palette.accent) : cssColor(this.settings.getTimeColor()));
    style.setProperty('--supporting-scale', String(this.settings.getSupportingScale()));
    style.setProperty('--clock-font', fontStack(this.settings.getFontFamily()));
    style.setProperty('--clock-weight', String(this.settings.getFontWeight()));
    style.setProperty('--time-color', theme !== 'classic' && this.settings.isThemeAutoInk() ? readableInk(palette.panel) : cssColor(this.settings.getTimeColor()));
    style.setProperty('--date-color', theme !== 'classic' && this.settings.isThemeAutoInk() ? readableInk(palette.panel) : cssColor(this.settings.getDateColor()));
    style.setProperty('--supporting-tracking', `${SUPPORTING_LETTER_SPACING}em`);
    style.setProperty(
      '--weather-icon-color',
      this.settings.isWeatherIconDynamicColor() ? 'var(--accent)' : '#ffffff'
    );
    this.weather.setIconStyle(this.settings.isWeatherIconFill());
    this.detail.setIconStyle(this.settings.isWeatherIconFill());
    // Pro drives the detail line with the clock's transition style.
    this.weather.setTransition(this.settings.getWeatherTransition());
    this.detail.setTransition(this.settings.getWeatherTransition());
  }

  async applyBackground(): Promise<void> {
    const useImage = this.settings.getBackgroundMode() === MODE_IMAGE;
    this.root.classList.remove('has-glass');
    this.background.style.removeProperty('filter');this.background.style.removeProperty('scale');
    this.background.style.backgroundColor = this.settings.getBackgroundMode() === 'color' || this.settings.getClockTheme() === 'classic' ? cssColor(this.settings.getBackgroundColor()) : this.settings.getThemePalette().background;
    if (!useImage) {
      this.glass = null; this.glassUrl = null;
      this.background.style.removeProperty('background-image');
      return;
    }
    const url = await this.imageSource();
    if (url) {
      this.background.style.backgroundImage = `url("${url}")`;
      if(this.settings.getClockTheme().startsWith('ultimate.') && this.settings.getThemeGlass().enabled){
        try {if(!this.glass || this.glassUrl!==url){this.glass=await GaussianGlass.load(url);this.glassUrl=url;}this.root.classList.add('has-glass');this.glass.apply(this.root,this.settings.getThemeGlass());}catch{/* Retain opaque cards if the image cannot decode. */}
      }
    }
    else this.background.style.removeProperty('background-image');
  }

  /** Schedules the next tick on the same clock the face draws from. */
  private tick(): void {
    this.render();
    if (!this.running) return;
    const now = timeSource.now();
    this.tickHandle = window.setTimeout(() => this.tick(), this.settings.getClockTheme() !== 'classic' && this.settings.isShowSeconds() && this.settings.getUltimateOptions().secondMotion === 'smooth' && !window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 33 : millisUntilNextSecond(now));
  }

  private render(): void {
    const now = timeSource.now();
    const fields = zonedFields(now, this.settings.getTimeZoneId());
    const showSeconds = this.settings.isShowSeconds();
    const smallSeconds = this.settings.isSmallSeconds();
    const use24Hour = this.settings.isUse24Hour();
    const english = this.settings.isClockUseEnglish();
    const stacked = (this.settings.isPortraitStacked() && this.isPortrait())
      || this.stage.clientWidth < this.stage.clientHeight * .4;

    const displayTime = formatTime(
      fields.hour,
      fields.minute,
      fields.second,
      showSeconds,
      this.settings.isBlinkColon(),
      smallSeconds,
      use24Hour,
      english
    );
    const dateText = formatDate(
      english ? this.settings.getDatePatternEn() : this.settings.getDatePatternCn(),
      fields,
      // Read the language from preferences rather than the i18n module's cached
      // value, so the date never renders half-translated if the two drift.
      dateLang(this.settings.getClockLanguage())
    );
    const lunarText = this.settings.isShowLunar()
      ? lunarClockLine(fields.year, fields.month0, fields.day)
      : '';

    this.applyDim(fields);
    this.renderWorldClocks(now);
    this.glass?.apply(this.root,this.settings.getThemeGlass());

    const options = {
      animate: this.settings.isAnimateTimeChanges(),
      transition: this.settings.getTimeTransition(),
      colonVisible: displayTime.colonVisible,
    };

    const themed = this.settings.getClockTheme() !== 'classic';
    this.root.classList.toggle('is-stacked', !themed && stacked);
    if (themed) {
      if (!this.ultimateFace) {
        this.mainLine?.reset(); this.periodLine?.reset(); this.secondsLine?.reset();
        this.mainLine = this.periodLine = this.secondsLine = null;
        this.stackedLines.forEach(line => line.reset()); this.stackedLines.length = 0;
        this.ultimateFace = new UltimateFace(this.timeRow, { date: this.dateRow, lunar: this.lunarRow, weather: this.root.querySelector('#clock-weather')!, detail: this.root.querySelector('#clock-detail')! }, this.settings);
      }
    } else if (stacked) {
      this.renderStacked(fields, use24Hour, showSeconds, options);
    } else {
      this.renderInline(displayTime, options);
    }

    // Portrait always stacks date and lunar; landscape shares one line unless the
    // user opts into two rows.
    const singleDateLine = !lunarText || (!themed && !this.isPortrait() && !this.settings.isDateLunarDualLine());
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

    if (themed) {
      this.ultimateFace!.render(this.settings.getClockTheme(), this.stage.clientWidth, this.stage.clientHeight, { ...fields, fraction: (now % 1000) / 1000, showSeconds, use24Hour, options });
      return;
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

  private renderWorldClocks(now: number): void {
    const options=this.settings.getUltimateOptions();
    const enabled=options.worldEnabled && this.settings.getClockTheme().startsWith('ultimate.') && options.worldZones.length>0;
    this.root.classList.toggle('has-world-clocks',enabled);
    let strip=this.root.querySelector<HTMLElement>('.world-clock-strip');
    if(!enabled){if(strip)strip.hidden=true;this.stage.style.removeProperty('bottom');return;}
    if(!strip){strip=document.createElement('div');strip.className='world-clock-strip';this.root.append(strip);}
    strip.hidden=false;
    strip.style.setProperty('--world-count',String(options.worldZones.length));
    strip.style.setProperty('--world-portrait-count',String(Math.min(3,options.worldZones.length)));
    const key=options.worldZones.join('|');if(strip.dataset.zones!==key){strip.dataset.zones=key;strip.replaceChildren(...options.worldZones.map(zone=>{const city=document.createElement('div');city.className='world-clock-city';const name=document.createElement('span');name.textContent=zone.split('/').pop()!.replaceAll('_',' ');const time=document.createElement('strong');city.append(name,time);return city;}));}
    options.worldZones.forEach((zone,i)=>{setAlignedTime(strip!.children[i].querySelector('strong')!,new Intl.DateTimeFormat(this.settings.isClockUseEnglish()?'en-GB':'zh-CN',{timeZone:zone,hour:'2-digit',minute:'2-digit',hour12:!this.settings.isUse24Hour()}).format(now),fontStack(this.settings.getFontFamily()),this.settings.getFontWeight());});
    this.stage.style.bottom = (strip.offsetHeight + parseFloat(getComputedStyle(strip).bottom) + 8) + 'px';
    if(this.root.classList.contains('has-glass')){const parent=this.root.getBoundingClientRect();for(const item of strip.children){const city=item as HTMLElement,rect=city.getBoundingClientRect();city.style.setProperty('--glass-left',-(rect.x-parent.x)+'px');city.style.setProperty('--glass-top',-(rect.y-parent.y)+'px');}}
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
    main.dataset.materialGroups = 'false';
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
    if (this.settings.getBackgroundMode() !== MODE_IMAGE) {
      this.dim.hidden = true;
      return;
    }
    if (this.settings.isDimBackground()) {
      this.dim.hidden = false;
      return;
    }
    if (!this.settings.isScheduleDimBackground()) {
      this.dim.hidden = true;
      return;
    }
    this.dim.hidden = !isDimScheduleActive(
      minutesOfDay(fields),
      this.settings.getDimStartMinutes(),
      this.settings.getDimEndMinutes()
    );
  }

  private updateWeatherLines(): void {
    const state = this.weatherState;
    const enabled = this.settings.isWeatherEnabled();
    const message = this.settings.getCustomMessage();
    const items: CarouselItem[] = [];

    if (enabled && state) {
      if (state.data) {
        const left = locationText(state.data.city, state.data.district);
        const right = `${state.data.text} ${temperature(state.data.temperature, this.settings.getTemperatureUnit())}`;
        items.push(weatherItem(left, state.data.icon, right));
      } else if (state.message) {
        items.push(plainItem(state.message));
      }
    }
    if (message) items.push(plainItem(message));
    this.weather.setItems(items);

    const detail = enabled && this.settings.isWeatherDetailed() ? state?.data?.detail ?? null : null;
    this.detail.setItems(
      detail
        ? detailCarouselItems(detail, {
            feelsFormat: t('weather_feels_format'),
            humidityFormat: t('weather_humidity_format'),
            windScaleFormat: t('weather_wind_scale_format'),
            precipFormat: t('weather_precip_format'),
            airFormat: t('weather_air_format'),
            warningSuffix: t('weather_warning_suffix'),
          }).map(text => plainItem(text.replace(/(-?\d+(?:\.\d+)?)℃/g, (_, value: string) => temperature(value, this.settings.getTemperatureUnit()))))
        : []
    );
    this.layoutSignature = '';
    this.layout(true);
  }

  private isPortrait(): boolean {
    return this.stage.clientHeight >= this.stage.clientWidth;
  }

  private font(): FontSpec {
    return { family: fontStack(this.settings.getFontFamily()), weight: this.settings.getFontWeight() };
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
    const themed = this.settings.getClockTheme() !== 'classic';
    if (themed) { this.render(); return; }
    const compact = this.stage.clientWidth < this.stage.clientHeight * .4
      || this.stage.clientWidth > this.stage.clientHeight * 2.8
      || Math.min(this.stage.clientWidth, this.stage.clientHeight) < 240;
    this.root.classList.toggle('is-compact', compact);
    const orbit = !compact && this.settings.getClockTheme() === 'ultimate.orbit';
    const orbitSize = Math.min(this.stage.clientWidth, this.stage.clientHeight) * 0.62;
    const width = orbit ? orbitSize : this.stage.clientWidth * (themed ? 0.78 : 1);
    const height = orbit ? orbitSize : this.stage.clientHeight * (themed ? 0.78 : 1);
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
      this.settings.getFontFamily(),
      this.settings.isBoldText(),
      this.settings.getTimeFontScale(),
      this.settings.getDateFontScale(),
      this.settings.getSupportingScale(),
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

    // Measure desired secondary type, but reserve the primary clock before allocating its space.
    const widestDate =
      context.singleDateLine || !context.lunarText
        ? context.dateText + (context.lunarText && context.singleDateLine ? ` ${context.lunarText}` : '')
        : longerOf(context.dateText, context.lunarText);
    const dateWidth = measureSupportingText(widestDate, font, SUPPORTING_LETTER_SPACING);
    const dateSize = calculateWidthBasedTextSize(
      width,
      height,
      dateWidth,
      this.settings.getDateFontScale(),
      context.stacked ? 0.08 : DATE_HEIGHT_FRACTION,
      DATE_MAX_WIDTH_FRACTION
    );
    const supportingSize = calculateWidthBasedTextSize(width,height,dateWidth,this.settings.getSupportingScale(),
      context.stacked ? 0.08 : DATE_HEIGHT_FRACTION,DATE_MAX_WIDTH_FRACTION);
    let timeSize = context.stacked
      ? this.stackedTimeSize(width, height, 0, context.showSeconds, font)
      : this.inlineTimeSize(width, height, context.displayTime, font);
    style.setProperty('--time-size', `${timeSize}px`);
    style.setProperty('--digit-w', `${widestDigitWidth(font) * timeSize}px`);
    // Theme chrome contributes real em padding; measure it only when layout inputs change.
    const measured = this.timeRow.getBoundingClientRect();
    const primaryHeight = measured.height || timeSize * (context.stacked ? (context.showSeconds ? 3 : 2) * 1.06 : 1);
    const fit = Math.min(1, measured.width > 0 ? width / measured.width : 1,
      height * .74 / Math.max(1, primaryHeight));
    timeSize *= fit;
    style.setProperty('--time-size', `${timeSize}px`);
    style.setProperty('--digit-w', `${widestDigitWidth(font) * timeSize}px`);
    const rows = fitSupportingRows(height, primaryHeight * fit, dateSize,
      supportingSize, context.singleDateLine ? 1 : 2,
      Number(!this.weather.isEmpty) + Number(!this.detail.isEmpty), portrait);
    style.setProperty('--date-size', `${rows.dateSize}px`);
    style.setProperty('--supporting-size', `${rows.supportingSize}px`);
    style.setProperty('--time-gap', `${rows.timeGap}px`);
    style.setProperty('--supporting-gap', `${rows.rowGap}px`);
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
      this.settings.getTimeFontScale(),
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
    const weatherTwoLines = this.settings.isWeatherEnabled() && this.settings.isWeatherDetailed();
    const heightFraction =
      lines === 3 ? (weatherTwoLines ? 0.13 : 0.18) : weatherTwoLines ? 0.2 : 0.28;
    const digitPairWidth = stableTextWidth('00', font);
    let lineSize = calculateWidthBasedTextSize(
      width,
      height,
      digitPairWidth,
      this.settings.getTimeFontScale(),
      heightFraction,
      0.66
    );

    const dateLineHeight = dateSize * LINE_HEIGHT;
    const hasLunar = this.settings.isShowLunar();
    const weatherShown = this.settings.isWeatherEnabled() || this.settings.getCustomMessage().length > 0;
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
