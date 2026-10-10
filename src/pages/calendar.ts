import { agendaSchedule } from '../ui/agenda-schedule';
import { WeatherAttribution } from '../ui/weather-attribution';
import { applyCalendarTheme } from '../core/calendar-themes';
import { temperature } from '../core/clock-themes';
/**
 * The calendar dashboard.
 *
 * Ported from com.clockmods.pro.ProCalendarFragment: a clock panel, the current
 * weather, a three-day forecast and a six-week month grid whose cells rotate the
 * lunar date with any festivals, plus the 宜/忌 footer. Landscape puts the left
 * column beside the month; portrait stacks them (layout-land / layout-port).
 */
import { prefs, cssColor } from '../core/prefs';
import { dateLang, intlLocale, t, ta } from '../core/i18n';
import { ensureFontLoaded, fontStack } from '../core/fonts';
import { timeSource, millisUntilNextSecond } from '../core/time-source';
import { addMonths, dateKey, daysInMonth, zonedFields } from '../core/zoned-time';
import { format as formatDate } from '../format/date-formatter';
import { pangu } from '../format/text-spacing';
import { periodTextFor, twoDigits } from '../format/time-formatter';
import { localizedAlmanac } from '../lunar/language';
import { holidayOn } from '../lunar/holidays';
import { createCalendarMonth, isWeekend } from '../lunar/calendar-month';
import type { CalendarDay } from '../lunar/calendar-month';
import { measureColonShift } from '../ui/clock-face';
import { LabelCarousel } from '../ui/label-carousel';
import type { LabelItem } from '../ui/label-carousel';
import { createWeatherIcon, loadWeatherIcons } from '../weather/icons';
import { DailyForecastController, WeatherController } from '../weather/controller';
import { locationText } from '../weather/models';
import type { DailyForecastState, WeatherState } from '../weather/models';
import type { Page } from '../app/router';

/** Matches the 210ms month page animation in ProCalendarFragment. */
const MONTH_ANIMATION_MS = 210;
const DRAG_THRESHOLD_PX = 40;

export class CalendarPage implements Page {
  readonly name = 'calendar';
  private typographyObserver?: ResizeObserver;
  dispose(): void { this.stop(); this.typographyObserver?.disconnect(); this.footer.destroy(); this.cellCarousels.forEach(carousel=>carousel.destroy()); this.agendaCarousels.forEach(carousel=>carousel.destroy()); }
  readonly immersive = true;

  private readonly root: HTMLElement;
  private readonly timeView: HTMLElement;
  private readonly secondsView: HTMLElement;
  private readonly periodView: HTMLElement;
  private readonly title: HTMLElement;
  private readonly weekdays: HTMLElement;
  private readonly grid: HTMLElement;
  private readonly preview: HTMLElement;
  private readonly viewport: HTMLElement;
  private readonly footer: LabelCarousel;
  private readonly weatherCard: HTMLElement;
  private readonly forecastCard: HTMLElement;
  private readonly attribution: HTMLElement;
  private readonly attributionPopup: WeatherAttribution;
  private readonly weatherIcon: HTMLElement;
  private readonly temperature: HTMLElement;
  private readonly feelsLabel: HTMLElement;
  private readonly feels: HTMLElement;
  private readonly summary: HTMLElement;
  private readonly forecastColumns: HTMLElement[];

  private readonly weatherController: WeatherController;
  private readonly forecastController: DailyForecastController;
  private readonly cellCarousels: LabelCarousel[] = [];

  private visibleYear = 0;
  private visibleMonth0 = 0;
  private selected = { year: 0, month0: 0, day: 1 };
  private tickHandle: number | null = null;
  private monthAnimationFrame: number | null = null;
  private monthAnimationTimer: number | null = null;
  private lastWeather: WeatherState = {status:'idle',data:null,message:null};
  private lastForecast: DailyForecastState = {status:'idle',data:null,message:null};
  private readonly agendaCarousels:LabelCarousel[]=[];
  private running = false;
  private animating = false;

  constructor(root: HTMLElement, private readonly onOpenSettings: () => void, private readonly settings: typeof prefs = prefs) {
    this.root = root;
    this.timeView = root.querySelector('#cal-time')!;
    this.secondsView = root.querySelector('#cal-seconds')!;
    this.periodView = root.querySelector('#cal-period')!;
    this.title = root.querySelector('#cal-title')!;
    this.weekdays = root.querySelector('#cal-weekdays')!;
    this.grid = root.querySelector('#cal-days')!;
    this.preview = root.querySelector('#cal-days-preview')!;
    this.viewport = root.querySelector('#cal-days-viewport')!;
    this.footer = new LabelCarousel(root.querySelector('#cal-footer')!);
    this.weatherCard = root.querySelector('#cal-weather-card')!;
    this.forecastCard = root.querySelector('#cal-forecast-card')!;
    this.attribution = root.querySelector('#cal-attribution')!;
    this.attributionPopup = new WeatherAttribution(root, this.attribution);
    this.weatherIcon = root.querySelector('#cal-weather-icon')!;
    this.temperature = root.querySelector('#cal-temp')!;
    this.feelsLabel = root.querySelector('#cal-feels-label')!;
    this.feels = root.querySelector('#cal-feels')!;
    this.summary = root.querySelector('#cal-weather-summary')!;
    this.forecastColumns = [0, 1, 2].map((index) => root.querySelector(`#cal-forecast-${index}`)!);

    // The dashboard always needs feels-like, regardless of the user's detail setting.
    this.weatherController = new WeatherController((state) => this.bindWeather(state), true);
    this.forecastController = new DailyForecastController((state) => this.bindForecast(state));

    const today = this.today();
    if(this.visibleYear===today.year && this.visibleMonth0===today.month0 && this.selected.day===today.day && this.grid.children.length) {this.fitSecondaryTypography();return;}
    this.visibleYear = today.year;
    this.visibleMonth0 = today.month0;
    this.selected = { year: today.year, month0: today.month0, day: today.day };

    this.bindControls();
    if (typeof ResizeObserver !== 'undefined') { this.typographyObserver = new ResizeObserver(()=>this.fitSecondaryTypography()); this.typographyObserver.observe(this.viewport); }
  }

  private bindControls(): void {
    this.root.querySelector('#cal-prev')!.addEventListener('click', () => this.changeMonth(-1));
    this.root.querySelector('#cal-next')!.addEventListener('click', () => this.changeMonth(1));
    this.root.querySelector('#cal-today')!.addEventListener('click', () => this.resetToToday());
    this.title.addEventListener('click', () => this.showMonthPicker());
    this.title.title = t('calendar_jump_title');
    this.attribution.addEventListener('click', (event) => event.stopPropagation());
    // Double-tapping the clock panel opens settings, as on the clock page.
    let lastTap = 0;
    this.root.querySelector('.cal-clock-panel')!.addEventListener('pointerup', () => {
      const now = performance.now();
      if (now - lastTap < 320) {
        lastTap = 0;
        this.onOpenSettings();
      } else {
        lastTap = now;
      }
    });
    this.bindMonthDrag();
  }

  /** Horizontal drag over the grid pages between months (MonthGestureLayout). */
  private bindMonthDrag(): void {
    let startX = 0,startY=0,horizontal=false,suppressUntil=0;
    this.viewport.addEventListener('click',event=>{if(performance.now()<suppressUntil){event.preventDefault();event.stopImmediatePropagation();}},{capture:true});
    let dragging = false;
    let direction = 0;
    this.viewport.addEventListener('pointerdown', (event) => {
      if (this.animating || (event.button !== -1 && event.button !== 0)) return;
      dragging = true;
      startX = event.clientX;startY=event.clientY;horizontal=false;
      direction = 0;
      this.grid.style.transition = 'none';
      this.preview.style.transition = 'none';
    });
    this.viewport.addEventListener('pointermove', (event) => {
      if (!dragging) return;
      const offset = event.clientX - startX;
      if(!horizontal){const dy=event.clientY-startY;if(Math.max(Math.abs(offset),Math.abs(dy))<6)return;if(Math.abs(dy)>=Math.abs(offset)){dragging=false;return;}horizontal=true;}
      event.preventDefault();
      if (!this.viewport.hasPointerCapture?.(event.pointerId)) {
        this.viewport.setPointerCapture?.(event.pointerId);
      }
      const nextDirection = offset < 0 ? 1 : -1;
      if (nextDirection !== direction) {
        direction = nextDirection;
        this.renderPreview(direction);
      }
      const width = this.grid.clientWidth;
      this.grid.style.transform = `translateX(${offset}px)`;
      this.preview.style.transform = `translateX(${offset + direction * width}px)`;
    });
    const finish = (event: PointerEvent) => {
      if (!dragging) return;
      dragging = false;
      if(horizontal) {suppressUntil=performance.now()+250;event.preventDefault();}
      const offset = event.clientX - startX;
      const shouldChange =
        event.type !== 'pointercancel' &&
        Math.abs(offset) >= DRAG_THRESHOLD_PX &&
        direction !== 0;
      if (shouldChange) this.changeMonth(direction, true);
      else this.snapBack(direction);
      if (this.viewport.hasPointerCapture?.(event.pointerId)) {
        this.viewport.releasePointerCapture(event.pointerId);
      }
    };
    this.viewport.addEventListener('pointerup', finish);
    this.viewport.addEventListener('pointercancel', finish);
  }

  start(): void {
    if (this.running) return;
    this.running = true;
    this.attributionPopup.setEnabled(this.settings.isWeatherEnabled());
    this.attributionPopup.start();
    this.footer.setActive(true);
    for (const carousel of [...this.cellCarousels,...this.agendaCarousels]) carousel.setActive(true);
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
    this.footer.setActive(false);
    for (const carousel of [...this.cellCarousels,...this.agendaCarousels]) carousel.setActive(false);
    this.weatherController.stop();
    this.forecastController.stop();
  }

  onEnter(fromAnotherPage: boolean): void {
    if (fromAnotherPage) this.resetToToday();
  }

  onExit(): void {
    this.weatherController.stop();
    this.forecastController.stop();
  }

  refreshSettings(): void {
    this.cancelMonthAnimation();
    void loadWeatherIcons().then(()=>{this.bindWeather(this.lastWeather);this.bindForecast(this.lastForecast);});
    applyCalendarTheme(this.root, this.settings);
    const style = this.root.style;
    style.setProperty('--cal-font', fontStack(this.settings.getFontFamily(this.settings.getUltimateOptions().calendarTheme)));
    this.applyColonShift();
    // A family that is still loading would measure as the fallback face.
    void ensureFontLoaded(this.settings.getFontFamily(this.settings.getUltimateOptions().calendarTheme), this.settings.getFontWeight(this.settings.getUltimateOptions().calendarTheme), this.root.ownerDocument).then(() => { this.applyColonShift(); this.fitSecondaryTypography(); });
    style.setProperty('--cal-time-color', ['calendar.graphite','calendar.carbon'].includes(this.settings.getUltimateOptions().calendarTheme) ? cssColor(this.settings.getTimeColor()) : 'var(--text)');
    style.setProperty(
      '--cal-weather-icon-color',
      this.settings.isWeatherIconDynamicColor() ? 'var(--accent)' : 'var(--cal-weather-ink)'
    );
    const weatherEnabled = this.settings.isWeatherEnabled();
    if (!weatherEnabled) {this.weatherController.stop();this.forecastController.stop();}
    this.weatherCard.hidden = !weatherEnabled;
    this.forecastCard.hidden = !weatherEnabled;
    const attributionText = t('weather_attribution');
    this.attribution.setAttribute('aria-label', attributionText);
    const attributionLabel = this.attribution.querySelector<HTMLElement>(
      '.weather-attribution-label'
    );
    if (attributionLabel) attributionLabel.textContent = attributionText;
    this.attributionPopup.setEnabled(weatherEnabled);
    this.feelsLabel.textContent = t('calendar_feels_like');
    this.root.querySelector('#cal-today')!.textContent = t('calendar_today');
    this.root.querySelector('#cal-prev')!.setAttribute('aria-label',t('calendar_previous_month'));
    this.root.querySelector('#cal-next')!.setAttribute('aria-label',t('calendar_next_month'));
    this.populateWeekdays();
    this.updateTime();
    this.renderMonth();
    if (this.running) this.startWeatherIfEnabled();
  }

  private startWeatherIfEnabled(): void {
    if (!this.settings.isWeatherEnabled()) return;
    this.weatherController.start(this.settings.getWeatherIntervalMinutes());
    this.forecastController.start();
  }

  private today() {
    return zonedFields(timeSource.now(), this.settings.getTimeZoneId());
  }

  private tick(): void {
    this.updateTime();
    if (!this.running) return;
    this.tickHandle = window.setTimeout(() => this.tick(), millisUntilNextSecond(timeSource.now()));
  }

  /**
   * The clock panel is drawn at the digits' weight, so the colon is measured
   * against a bold face regardless of the bold-text setting.
   */
  private applyColonShift(): void {
    const shift = measureColonShift(fontStack(this.settings.getFontFamily(this.settings.getUltimateOptions().calendarTheme)), this.settings.getFontWeight(this.settings.getUltimateOptions().calendarTheme));
    this.root.style.setProperty('--cal-colon-shift', `${shift}em`);
  }

  /** Wraps each colon so --cal-colon-shift can lift it onto the digits' centre. */
  private renderTime(target: HTMLElement, text: string): void {
    const parts = text.split(':').flatMap((chunk, index) => {
      const spans: HTMLElement[] = [];
      if (index > 0) {
        const colon = document.createElement('span');
        colon.className = 'cal-colon';
        colon.textContent = ':';
        spans.push(colon);
      }
      if (chunk) {
        const digits = document.createElement('span');
        digits.textContent = chunk;
        spans.push(digits);
      }
      return spans;
    });
    target.replaceChildren(...parts);
  }

  private updateTime(): void {
    const fields = this.today();
    const use24Hour = this.settings.isUse24Hour();
    let hour = use24Hour ? fields.hour : fields.hour % 12;
    if (!use24Hour && hour === 0) hour = 12;
    this.renderTime(this.timeView, `${twoDigits(hour)}:${twoDigits(fields.minute)}`);
    this.secondsView.hidden = !this.settings.isShowSeconds();
    this.renderTime(this.secondsView, `:${twoDigits(fields.second)}`);
    this.periodView.hidden = use24Hour;
    if (!use24Hour) {
      this.periodView.textContent = periodTextFor(fields.hour, this.settings.isClockUseEnglish());
    }
  }

  // ---- Weather ----

  private bindWeather(state: WeatherState): void {
    this.lastWeather=state;this.renderAgendaWeather();
    this.weatherIcon.hidden = !state.data;
    if (!state.data) {
      this.temperature.textContent = temperature('--',this.settings.getTemperatureUnit());this.feels.textContent=temperature('--',this.settings.getTemperatureUnit());
      this.summary.textContent = pangu(state.message ?? t('calendar_forecast_loading'));
      this.fitWeatherTypography();
      return;
    }
    const data = state.data;
    this.weatherIcon.replaceChildren(
      createWeatherIcon(data.icon, this.settings.isWeatherIconFill()) ?? document.createTextNode('')
    );
    this.temperature.textContent = temperature(data.temperature, this.settings.getTemperatureUnit());
    const feelsLike = data.detail?.feelsLike?.trim() ? data.detail.feelsLike : '--';
    this.feels.textContent = temperature(feelsLike, this.settings.getTemperatureUnit());

    const parts = [locationText(data.city, data.district), data.text];
    if (data.detail) {
      const scale = data.detail.windScale
        ? t('weather_wind_scale_format', data.detail.windScale)
        : '';
      const wind = [data.detail.windDir, scale].filter(Boolean).join(' ');
      if (wind) parts.push(wind);
      if (data.detail.humidity) parts.push(t('weather_humidity_format', data.detail.humidity));
    }
    this.summary.textContent = pangu(parts.filter(Boolean).join(' · '));
    this.fitWeatherTypography();
  }

  private bindForecast(state: DailyForecastState): void {
    this.lastForecast=state;this.renderAgendaWeather();
    const labels = ta('forecast_day_labels');
    this.forecastCard.querySelector('.cal-forecast-row')?.classList.toggle('is-unavailable',!state.data);
    if (!state.data) {
      const message = state.message ?? t('calendar_forecast_loading');
      for (const column of this.forecastColumns) {
        column.replaceChildren(text('cal-forecast-text', message));
      }
      this.fitWeatherTypography();
      return;
    }
    const today = this.today();
    const cursor = new Date(Date.UTC(today.year, today.month0, today.day));
    for (let index = 0; index < this.forecastColumns.length; index++) {
      const column = this.forecastColumns[index];
      const key = dateKey(cursor.getUTCFullYear(), cursor.getUTCMonth(), cursor.getUTCDate());
      const forecast = state.data.entries.find((entry) => entry.fxDate === key) ?? null;
      const heading = text('cal-forecast-label' + (index === 0 ? ' is-today' : ''), labels[index] ?? '');
      if (forecast?.textDay) heading.append(document.createTextNode(' '), text('cal-forecast-condition', forecast.textDay));
      const nodes: Node[] = [heading];
      if (forecast) {
        const icon = createWeatherIcon(forecast.iconDay, this.settings.isWeatherIconFill());
        if (icon) {
          icon.classList.add('cal-forecast-icon');
          if (index === 0) icon.classList.add('is-today');
          nodes.push(icon);
        }
        nodes.push(
          text(
            'cal-forecast-text',
            temperature(forecast.tempMin, this.settings.getTemperatureUnit()) + ' ~ ' + temperature(forecast.tempMax, this.settings.getTemperatureUnit())
          )
        );
        const details=[forecast.windDirDay,forecast.windScaleDay?t('weather_wind_scale_format',forecast.windScaleDay):'',forecast.humidity?t('weather_humidity_format',forecast.humidity):''].filter(Boolean).join(' · ');
        if(details)nodes.push(text('cal-forecast-detail',details));
      } else {
        nodes.push(text('cal-forecast-text', t('calendar_forecast_unavailable')));
      }
      column.replaceChildren(...nodes);
      cursor.setUTCDate(cursor.getUTCDate() + 1);
    }
    this.fitWeatherTypography();
  }

  // ---- Month grid ----

  private populateWeekdays(): void {
    const names = ta('calendar_weekday_names');
    const firstDayOfWeek = this.settings.getCalendarWeekStart();
    const highlight = this.settings.isCalendarHighlightWeekends();
    const cells: HTMLElement[] = [];
    for (let offset = 0; offset < 7; offset++) {
      const dayOfWeek = ((firstDayOfWeek - 1 + offset) % 7) + 1;
      const cell = document.createElement('div');
      cell.className = 'cal-weekday';
      if (highlight && isWeekend(dayOfWeek)) cell.classList.add('is-weekend');
      cell.textContent = names[dayOfWeek - 1] ?? '';
      cells.push(cell);
    }
    this.weekdays.replaceChildren(...cells);
  }

  private renderMonth(): void {
    const english = this.settings.isClockUseEnglish();
    this.title.textContent = formatDate(
      english ? 'MMMM yyyy' : 'yyyy年M月',
      { ...this.today(), year: this.visibleYear, month0: this.visibleMonth0, day: 1 },
      dateLang(this.settings.getClockLanguage())
    );
    if(this.settings.getUltimateOptions().calendarTheme==='calendar.poster') {
      const date=new Date(Date.UTC(this.visibleYear,this.visibleMonth0,1));
      this.title.replaceChildren(text('cal-poster-month',new Intl.DateTimeFormat(intlLocale(this.settings.getClockLanguage()),{month:'long',timeZone:'UTC'}).format(date).toLocaleUpperCase()),text('cal-poster-year',String(this.visibleYear)));
    }
    const month = createCalendarMonth(
      this.visibleYear,
      this.visibleMonth0,
      this.today(),
      this.settings.getCalendarWeekStart()
    );
    for (const carousel of this.cellCarousels) carousel.destroy();
    this.cellCarousels.length = 0;
    this.grid.replaceChildren(...this.visibleDays(month.days).map((day) => this.createDayCell(day, true)));
    this.updateFooter();
    this.fitSecondaryTypography();
  }

  private fitSecondaryTypography(): void {
    const cell=this.grid.querySelector<HTMLElement>('.cal-day'); if(!cell?.clientWidth || !cell.clientHeight)return;
    const id=this.settings.getUltimateOptions().calendarTheme,date=this.settings.getDateFontSize(id),support=this.settings.getSupportingFontSize(id);
    // Preserve fractional cell dimensions: integer rounding can make a header-sized
    // row alternate between two font sizes on consecutive ResizeObserver frames.
    const bounds=cell.getBoundingClientRect();
    const availableHeight=Math.max(1,(bounds.height||cell.clientHeight)-8), availableWidth=Math.max(1,(bounds.width||cell.clientWidth)-4);
    const stableSize=(size:number)=>Math.floor(size*10)/10;
    const limit=Math.min(availableHeight*.38,availableWidth*.45);
    this.root.style.setProperty('--cal-date-size',stableSize(Math.min(date||availableHeight*.38,id==='calendar.poster'?Math.min(availableHeight*.72,availableWidth*.45):limit))+'px');
    this.root.style.setProperty('--cal-support-size',stableSize(Math.min(support||availableHeight*.23,limit))+'px');
    this.fitPosterMonthTypography();
    this.fitWeatherTypography();
    const panel=this.root.querySelector<HTMLElement>('.cal-clock-panel')!;
    const timeLimit=Math.min(panel.clientWidth/(this.settings.isShowSeconds()?4.3:3.2),panel.clientHeight*.72);
    this.root.style.setProperty('--cal-clock-size',Math.max(1,Math.min(timeLimit,timeLimit*.72*this.settings.getTimeFontScale(id)/.88))+'px');
  }

  /** Poster mastheads always fit their own region, independent of manual sizes. */
  private fitPosterMonthTypography(): void {
    const month = this.title.querySelector<HTMLElement>('.cal-poster-month');
    const year = this.title.querySelector<HTMLElement>('.cal-poster-year');
    if (!month || !year || !this.title.clientWidth || !this.title.clientHeight) return;
    this.root.style.setProperty('--cal-poster-month-size', '100px');
    const range = this.root.ownerDocument.createRange();
    range.selectNodeContents(month);
    const measuredWidth = range.getBoundingClientRect().width;
    range.selectNodeContents(year);
    const measuredYearWidth = range.getBoundingClientRect().width;
    const measuredHeight = month.getBoundingClientRect().height + year.getBoundingClientRect().height
      + parseFloat(getComputedStyle(year).marginTop);
    const size = Math.max(1, Math.min(
      this.title.clientWidth * .8 / Math.max(1, measuredWidth) * 100,
      this.title.clientWidth / Math.max(1, measuredYearWidth) * 100,
      this.title.clientHeight / Math.max(1, measuredHeight) * 100
    ));
    this.root.style.setProperty('--cal-poster-month-size', size + 'px');
  }

  /** Fit the actual rows, including wrapped detail text and the card's padding. */
  private fitWeatherTypography(): void {
    const requested = this.settings.getSupportingFontSize(this.settings.getUltimateOptions().calendarTheme);
    this.fitCardTypography(this.weatherCard, '--cal-weather-size', requested,
      '.cal-weather-main, .cal-weather-icon:not([hidden]), .cal-temp, .cal-feels, .cal-weather-summary');
    this.fitCardTypography(this.forecastCard, '--cal-forecast-size', requested,
      '.cal-forecast-col > *');
  }

  private fitCardTypography(card: HTMLElement, variable: string, requested: number, selector: string): void {
    if (card.hidden || !card.clientWidth || !card.clientHeight) return;
    const bounds = card.getBoundingClientRect(), style = getComputedStyle(card);
    const left = bounds.left + parseFloat(style.paddingLeft), right = bounds.right - parseFloat(style.paddingRight);
    const top = bounds.top + parseFloat(style.paddingTop), bottom = bounds.bottom - parseFloat(style.paddingBottom);
    const content = [...card.querySelectorAll<HTMLElement>(selector)];
    if (!content.length) return;
    const setSize = (size: number) => this.root.style.setProperty(variable, size + 'px');
    const fits = () => content.every(element => {
      const rect = element.getBoundingClientRect();
      const column = element.closest<HTMLElement>('.cal-forecast-col');
      const columnBounds = column?.getBoundingClientRect(), columnStyle = column ? getComputedStyle(column) : null;
      const rowLeft = columnBounds ? Math.max(left, columnBounds.left + parseFloat(columnStyle!.paddingLeft)) : left;
      const rowRight = columnBounds ? Math.min(right, columnBounds.right - parseFloat(columnStyle!.paddingRight)) : right;
      return !rect.width || !rect.height || (rect.left >= rowLeft - .5 && rect.right <= rowRight + .5
        && rect.top >= top - .5 && rect.bottom <= bottom + .5
        && element.scrollWidth <= element.clientWidth + 1 && element.scrollHeight <= element.clientHeight + 1);
    });
    // The initial estimate is only a ceiling. Measure real wrapping rather than
    // assuming a one-line weather summary or a fixed number of forecast rows.
    let low = 1, high = Math.max(1, requested || Math.min((right - left) / 10, (bottom - top) / 4));
    setSize(high);
    if (fits()) return;
    for (let step = 0; step < 10; step++) {
      const size = (low + high) / 2;
      setSize(size);
      if (fits()) low = size; else high = size;
    }
    setSize(Math.floor(low * 10) / 10);
  }

  private renderPreview(direction: number): void {
    const target = addMonths(this.visibleYear, this.visibleMonth0, 1, direction);
    const month = createCalendarMonth(
      target.year,
      target.month0,
      this.today(),
      this.settings.getCalendarWeekStart()
    );
    this.preview.replaceChildren(...this.visibleDays(month.days, direction).map((day) => this.createDayCell(day, false)));
    this.preview.hidden = false;
    this.preview.style.transform = `translateX(${direction * this.grid.clientWidth}px)`;
  }

  private visibleDays(monthDays: CalendarDay[], offset = 0): CalendarDay[] {
    if(this.settings.getUltimateOptions().calendarTheme!=='calendar.agenda')return monthDays;
    const date=new Date(Date.UTC(this.selected.year,this.selected.month0,this.selected.day+offset*7));
    const month=createCalendarMonth(date.getUTCFullYear(),date.getUTCMonth(),this.today(),this.settings.getCalendarWeekStart());
    const index=month.days.findIndex(day=>day.year===date.getUTCFullYear()&&day.month0===date.getUTCMonth()&&day.dayOfMonth===date.getUTCDate());
    return month.days.slice(Math.floor(index/7)*7,Math.floor(index/7)*7+7);
  }

  private createDayCell(day: CalendarDay, interactive: boolean): HTMLElement {
    const cell: HTMLElement = interactive
      ? document.createElement('button')
      : document.createElement('div');
    // Only the live grid takes a ripple; the preview layer is pointer-inert.
    cell.className = interactive ? 'cal-day m3-interactive' : 'cal-day';
    if (interactive) cell.setAttribute('type', 'button');
    if (!day.currentMonth) cell.classList.add('is-other-month');
    if (day.today) cell.classList.add('is-today');
    if (this.settings.isCalendarHighlightWeekends() && isWeekend(day.dayOfWeek)) {
      cell.classList.add('is-weekend');
    }
    if (
      interactive &&
      day.year === this.selected.year &&
      day.month0 === this.selected.month0 &&
      day.dayOfMonth === this.selected.day
    ) {
      cell.classList.add('is-selected');
    }

    cell.dataset.date = dateKey(day.year, day.month0, day.dayOfMonth);

    const number = document.createElement('div');
    number.className = 'cal-day-number';
    const numberText = document.createElement('span');
    numberText.textContent = String(day.dayOfMonth);
    number.appendChild(numberText);

    const status = holidayOn(dateKey(day.year, day.month0, day.dayOfMonth));
    if (status) {
      const badge = document.createElement('span');
      badge.className = `cal-day-badge ${status.offDay ? 'is-off' : 'is-work'}`;
      badge.textContent = status.offDay ? t('calendar_day_status_off') : t('calendar_day_status_work');
      number.appendChild(badge);
    }
    if(this.settings.getUltimateOptions().calendarTheme==='calendar.agenda') cell.append(text('cal-agenda-weekday',ta('calendar_weekday_names')[day.dayOfWeek-1]??''));
    cell.appendChild(number);

    const almanac = localizedAlmanac(day.year, day.month0, day.dayOfMonth, this.settings.getClockLanguage());
    const labelHost = document.createElement('div');
    labelHost.className = 'cal-day-label';
    if (almanac.festivals.length > 0) labelHost.classList.add('has-festival');
    cell.appendChild(labelHost);
    const carousel = new LabelCarousel(labelHost);
    carousel.setItems([almanac.shortLabel, ...almanac.festivals].map((text) => ({ text })));
    if (interactive) {
      carousel.setActive(this.running);
      this.cellCarousels.push(carousel);
      cell.addEventListener('click', (event) => {
        event.stopPropagation();
        this.selectDay(day);
      });
    }

    const separator = this.settings.isClockUseEnglish() ? ', ' : '，';
    let description = t(
      day.today ? 'calendar_day_today_accessibility' : 'calendar_day_accessibility',
      day.dayOfMonth,
      almanac.shortLabel
    );
    if (almanac.festivals.length > 0) description += separator + almanac.festivals.join(separator);
    if (status) description += t(status.offDay ? 'calendar_day_rest' : 'calendar_day_makeup');
    cell.setAttribute('aria-label', pangu(description));
    return cell;
  }

  private selectDay(day: CalendarDay): void {
    const monthChanged = day.year !== this.visibleYear || day.month0 !== this.visibleMonth0;
    this.selected = { year: day.year, month0: day.month0, day: day.dayOfMonth };
    if (monthChanged) {
      // An adjacent-month day rebuilds the grid for its own month anyway.
      this.visibleYear = day.year;
      this.visibleMonth0 = day.month0;
      this.renderMonth();
      return;
    }
    // Same month: only move the highlight so the per-cell carousels keep running.
    for (const cell of this.grid.querySelectorAll('.cal-day')) cell.classList.remove('is-selected');
    const key = dateKey(day.year, day.month0, day.dayOfMonth);
    this.grid.querySelector(`.cal-day[data-date="${key}"]`)?.classList.add('is-selected');
    this.updateFooter();
  }

  private updateFooter(): void {
    const almanac = localizedAlmanac(this.selected.year, this.selected.month0, this.selected.day, this.settings.getClockLanguage());
    this.renderAgendaDetail(almanac);
    const pattern = this.settings.isClockUseEnglish() ? this.settings.getDatePatternEn() : this.settings.getDatePatternCn();
    const formatted = formatDate(
      pattern,
      {
        ...this.today(),
        year: this.selected.year,
        month0: this.selected.month0,
        day: this.selected.day,
        dayOfWeek0: new Date(
          Date.UTC(this.selected.year, this.selected.month0, this.selected.day)
        ).getUTCDay(),
      },
      dateLang(this.settings.getClockLanguage())
    );
    const items: LabelItem[] = [
      { text: t('calendar_selected_date', formatted, almanac.natural), color: 'var(--text)' },
    ];
    // 宜/忌 lead their lines: the glyph is drawn bold and stays pinned at the
    // left edge while a line too long for the footer scrolls past it.
    if (almanac.suitable.length > 0) {
      items.push({
        text: t('calendar_suitable_prefix') + almanac.suitable.join(' · '),
        color: 'var(--green)',
        pinnedPrefix: t('calendar_suitable_prefix'),
      });
    }
    if (almanac.avoid.length > 0) {
      items.push({
        text: t('calendar_avoid_prefix') + almanac.avoid.join(' · '),
        color: 'var(--red)',
        pinnedPrefix: t('calendar_avoid_prefix'),
      });
    }
    this.footer.setItems(items);
    this.footer.setActive(this.running);
  }

  private renderAgendaDetail(almanac:ReturnType<typeof localizedAlmanac>):void {
    this.agendaCarousels.splice(0).forEach(c=>c.destroy());
    let detail=this.root.querySelector<HTMLElement>('.cal-agenda-detail');
    if(!detail){detail=document.createElement('article');detail.className='cal-agenda-detail';this.viewport.after(detail);}
    detail.hidden=this.settings.getUltimateOptions().calendarTheme!=='calendar.agenda';
    if(detail.hidden)return;
    const date=new Date(Date.UTC(this.selected.year,this.selected.month0,this.selected.day));
    const heading=text('cal-agenda-heading',new Intl.DateTimeFormat(intlLocale(this.settings.getClockLanguage()),{month:'long',day:'numeric',weekday:'long',timeZone:'UTC'}).format(date));
    const lunar=text('cal-agenda-lunar',almanac.natural);
    const festival=text('cal-agenda-festivals',almanac.festivals.join(' · '));festival.hidden=!almanac.festivals.length;
    const weather=document.createElement('div');weather.className='cal-agenda-weather';
    detail.replaceChildren(heading,lunar,festival,weather);
    for(const [prefix,values,kind] of [[t('calendar_suitable_prefix'),almanac.suitable,'suitable'],[t('calendar_avoid_prefix'),almanac.avoid,'avoid']] as const){
      const line=text('cal-agenda-almanac '+kind,'');line.hidden=!values.length;detail.append(line);const carousel=new LabelCarousel(line);carousel.setItems([{text:prefix+values.join(' · '),pinnedPrefix:prefix}]);carousel.setActive(this.running);this.agendaCarousels.push(carousel);
    }
    detail.append(agendaSchedule(dateKey(this.selected.year,this.selected.month0,this.selected.day), this.settings !== prefs));
    this.renderAgendaWeather();
  }

  private renderAgendaWeather():void {
    const host=this.root.querySelector<HTMLElement>('.cal-agenda-weather');if(!host)return;
    const key=dateKey(this.selected.year,this.selected.month0,this.selected.day),today=this.today();
    const isToday=key===dateKey(today.year,today.month0,today.day);
    const forecast=this.lastForecast.data?.entries.find(day=>day.fxDate===key);
    const current=isToday?this.lastWeather.data:null;
    host.replaceChildren();host.hidden=!this.settings.isWeatherEnabled()||(!forecast&&!current);
    if(host.hidden)return;
    const icon=createWeatherIcon(forecast?.iconDay??current?.icon,this.settings.isWeatherIconFill());
    if(icon){icon.classList.add('cal-agenda-weather-icon');host.append(icon);}
    if(current)host.append(text('cal-agenda-temperature',temperature(current.temperature,this.settings.getTemperatureUnit())));
    const parts=forecast?[forecast.textDay,temperature(forecast.tempMin,this.settings.getTemperatureUnit())+' – '+temperature(forecast.tempMax,this.settings.getTemperatureUnit()),forecast.humidity?t('weather_humidity_format',forecast.humidity):'',[forecast.windDirDay,forecast.windScaleDay?t('weather_wind_scale_format',forecast.windScaleDay):''].filter(Boolean).join(' ')]:[current?.text??''];
    host.append(text('cal-agenda-weather-description',parts.filter(Boolean).join(' · ')));
  }

  // ---- Month navigation ----

  private changeMonth(direction: number, previewReady = false): void {
    if (this.animating || direction === 0) return;
    const width = this.grid.clientWidth;
    if (width === 0) {
      this.applyMonthOffset(direction);
      return;
    }
    if (!previewReady) this.renderPreview(direction);
    this.animating = true;
    this.startLayerAnimation(
      direction > 0 ? -width : width,
      0,
      () => {
      this.applyMonthOffset(direction);
      this.resetLayers();
      this.animating = false;
      }
    );
  }

  private snapBack(direction: number): void {
    if (this.preview.hidden || direction === 0) {
      this.resetLayers();
      return;
    }
    const width = this.grid.clientWidth;
    this.animating = true;
    this.startLayerAnimation(0, direction * width, () => {
      this.resetLayers();
      this.animating = false;
    });
  }

  private startLayerAnimation(
    gridTarget: number,
    previewTarget: number,
    finish: () => void
  ): void {
    this.clearMonthAnimationHandles();
    this.monthAnimationFrame = requestAnimationFrame(() => {
      this.monthAnimationFrame = null;
      const transition = `transform ${MONTH_ANIMATION_MS}ms cubic-bezier(0, 0, 0.2, 1)`;
      this.grid.style.transition = transition;
      this.preview.style.transition = transition;
      this.grid.style.transform = `translateX(${gridTarget}px)`;
      this.preview.style.transform = `translateX(${previewTarget}px)`;
      this.monthAnimationTimer = window.setTimeout(() => {
        this.monthAnimationTimer = null;
        finish();
      }, MONTH_ANIMATION_MS);
    });
  }

  private resetLayers(): void {
    this.clearMonthAnimationHandles();
    this.grid.style.transition = 'none';
    this.preview.style.transition = 'none';
    this.grid.style.transform = 'translateX(0)';
    this.preview.hidden = true;
    this.preview.replaceChildren();
    this.monthAnimationFrame = requestAnimationFrame(() => {
      this.monthAnimationFrame = null;
      this.grid.style.transition = '';
      this.preview.style.transition = '';
    });
  }

  private applyMonthOffset(direction: number): void {
    if(this.settings.getUltimateOptions().calendarTheme==='calendar.agenda'){const date=new Date(Date.UTC(this.selected.year,this.selected.month0,this.selected.day+7*direction));this.selected={year:date.getUTCFullYear(),month0:date.getUTCMonth(),day:date.getUTCDate()};this.visibleYear=this.selected.year;this.visibleMonth0=this.selected.month0;this.renderMonth();return;}
    const targetDay = this.selected.day;
    const target = addMonths(this.visibleYear, this.visibleMonth0, 1, direction);
    this.visibleYear = target.year;
    this.visibleMonth0 = target.month0;
    this.selected = {
      year: target.year,
      month0: target.month0,
      day: Math.min(targetDay, daysInMonth(target.year, target.month0)),
    };
    this.renderMonth();
  }

  private resetToToday(): void {
    this.cancelMonthAnimation();
    const today = this.today();
    this.visibleYear = today.year;
    this.visibleMonth0 = today.month0;
    this.selected = { year: today.year, month0: today.month0, day: today.day };
    this.renderMonth();
  }

  private clearMonthAnimationHandles(): void {
    if (this.monthAnimationFrame !== null) {
      cancelAnimationFrame(this.monthAnimationFrame);
      this.monthAnimationFrame = null;
    }
    if (this.monthAnimationTimer !== null) {
      clearTimeout(this.monthAnimationTimer);
      this.monthAnimationTimer = null;
    }
  }

  private cancelMonthAnimation(): void {
    this.clearMonthAnimationHandles();
    this.animating = false;
    this.resetLayers();
  }

  /** Year/month quick jump, replacing the NumberPicker dialog. */
  private showMonthPicker(): void {
    const dialog = document.createElement('dialog');
    dialog.className = 'month-picker';
    const heading = document.createElement('h2');
    heading.textContent = t('calendar_jump_title');

    const yearSelect = document.createElement('select');
    for (let year = 1901; year <= 2099; year++) {
      const option = document.createElement('option');
      option.value = String(year);
      option.textContent = `${year} ${t('calendar_jump_year')}`;
      yearSelect.appendChild(option);
    }
    yearSelect.value = String(this.visibleYear);

    const monthSelect = document.createElement('select');
    for (let month = 1; month <= 12; month++) {
      const option = document.createElement('option');
      option.value = String(month - 1);
      option.textContent = t('calendar_month_short', month);
      monthSelect.appendChild(option);
    }
    monthSelect.value = String(this.visibleMonth0);

    const row = document.createElement('div');
    row.className = 'month-picker-row';
    row.append(yearSelect, monthSelect);

    const actions = document.createElement('div');
    actions.className = 'month-picker-actions';
    const cancel = document.createElement('button');
    cancel.className = 'm3-button m3-button--text';
    cancel.textContent = t('cancel');
    cancel.addEventListener('click', () => dialog.close());
    const confirm = document.createElement('button');
    confirm.className = 'm3-button m3-button--filled';
    confirm.textContent = t('ok');
    confirm.addEventListener('click', () => {
      this.cancelMonthAnimation();
      this.visibleYear = Number(yearSelect.value);
      this.visibleMonth0 = Number(monthSelect.value);
      this.selected = { year: this.visibleYear, month0: this.visibleMonth0, day: 1 };
      this.renderMonth();
      dialog.close();
    });
    actions.append(cancel, confirm);

    dialog.append(heading, row, actions);
    dialog.addEventListener('close', () => dialog.remove());
    document.body.appendChild(dialog);
    dialog.showModal();
  }
}

function text(className: string, value: string): HTMLElement {
  const element = document.createElement('div');
  element.className = className;
  element.textContent = pangu(value);
  return element;
}
