/**
 * The calendar dashboard.
 *
 * Ported from com.clockmods.pro.ProCalendarFragment: a clock panel, the current
 * weather, a three-day forecast and a six-week month grid whose cells rotate the
 * lunar date with any festivals, plus the 宜/忌 footer. Landscape puts the left
 * column beside the month; portrait stacks them (layout-land / layout-port).
 */
import { prefs, cssColor } from '../core/prefs';
import { dateLang, t, ta } from '../core/i18n';
import { fontStack } from '../core/fonts';
import { timeSource, millisUntilNextSecond } from '../core/time-source';
import { addMonths, dateKey, daysInMonth, zonedFields } from '../core/zoned-time';
import { format as formatDate } from '../format/date-formatter';
import { periodTextFor, twoDigits } from '../format/time-formatter';
import { almanacOf } from '../lunar/lunar';
import { holidayOn } from '../lunar/holidays';
import { createCalendarMonth, isWeekend } from '../lunar/calendar-month';
import type { CalendarDay } from '../lunar/calendar-month';
import { LabelCarousel } from '../ui/label-carousel';
import type { LabelItem } from '../ui/label-carousel';
import { createWeatherIcon } from '../weather/icons';
import { DailyForecastController, WeatherController } from '../weather/controller';
import { locationText } from '../weather/models';
import type { DailyForecastState, WeatherState } from '../weather/models';
import type { Page } from '../app/router';

/** Matches the 210ms month page animation in ProCalendarFragment. */
const MONTH_ANIMATION_MS = 210;
const DRAG_THRESHOLD_PX = 40;

export class CalendarPage implements Page {
  readonly name = 'calendar';
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
  private running = false;
  private animating = false;

  constructor(root: HTMLElement, private readonly onOpenSettings: () => void) {
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
    this.visibleYear = today.year;
    this.visibleMonth0 = today.month0;
    this.selected = { year: today.year, month0: today.month0, day: today.day };

    this.bindControls();
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
    let startX = 0;
    let dragging = false;
    let direction = 0;
    this.viewport.addEventListener('pointerdown', (event) => {
      if (this.animating) return;
      dragging = true;
      startX = event.clientX;
      direction = 0;
      this.grid.style.transition = 'none';
      this.preview.style.transition = 'none';
    });
    this.viewport.addEventListener('pointermove', (event) => {
      if (!dragging) return;
      const offset = event.clientX - startX;
      if (Math.abs(offset) < 4) return;
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
      this.grid.style.transition = '';
      this.preview.style.transition = '';
      const offset = event.clientX - startX;
      if (Math.abs(offset) >= DRAG_THRESHOLD_PX && direction !== 0) this.changeMonth(direction);
      else this.snapBack();
    };
    this.viewport.addEventListener('pointerup', finish);
    this.viewport.addEventListener('pointercancel', finish);
  }

  start(): void {
    if (this.running) return;
    this.running = true;
    this.footer.setActive(true);
    for (const carousel of this.cellCarousels) carousel.setActive(true);
    this.tick();
    this.startWeatherIfEnabled();
  }

  stop(): void {
    this.running = false;
    if (this.tickHandle !== null) {
      clearTimeout(this.tickHandle);
      this.tickHandle = null;
    }
    this.footer.setActive(false);
    for (const carousel of this.cellCarousels) carousel.setActive(false);
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
    const style = this.root.style;
    style.setProperty('--cal-font', fontStack(prefs.getFontFamily()));
    style.setProperty('--cal-time-color', cssColor(prefs.getTimeColor()));
    style.setProperty(
      '--cal-weather-icon-color',
      prefs.isWeatherIconDynamicColor() ? 'var(--accent)' : '#ffffff'
    );
    const weatherEnabled = prefs.isWeatherEnabled();
    this.weatherCard.hidden = !weatherEnabled;
    this.forecastCard.hidden = !weatherEnabled;
    this.attribution.hidden = !weatherEnabled;
    this.feelsLabel.textContent = t('calendar_feels_like');
    this.root.querySelector('#cal-today')!.textContent = t('calendar_today');
    this.populateWeekdays();
    this.updateTime();
    this.renderMonth();
    if (this.running) this.startWeatherIfEnabled();
  }

  private startWeatherIfEnabled(): void {
    if (!prefs.isWeatherEnabled()) return;
    this.weatherController.start(prefs.getWeatherIntervalMinutes());
    this.forecastController.start();
  }

  private today() {
    return zonedFields(timeSource.now(), prefs.getTimeZoneId());
  }

  private tick(): void {
    this.updateTime();
    if (!this.running) return;
    this.tickHandle = window.setTimeout(() => this.tick(), millisUntilNextSecond(timeSource.now()));
  }

  private updateTime(): void {
    const fields = this.today();
    const use24Hour = prefs.isUse24Hour();
    let hour = use24Hour ? fields.hour : fields.hour % 12;
    if (!use24Hour && hour === 0) hour = 12;
    this.timeView.textContent = `${twoDigits(hour)}:${twoDigits(fields.minute)}`;
    this.secondsView.hidden = !prefs.isShowSeconds();
    this.secondsView.textContent = `:${twoDigits(fields.second)}`;
    this.periodView.hidden = use24Hour;
    if (!use24Hour) {
      this.periodView.textContent = periodTextFor(fields.hour, prefs.isClockUseEnglish());
    }
  }

  // ---- Weather ----

  private bindWeather(state: WeatherState): void {
    if (!state.data) {
      this.summary.textContent = state.message ?? t('calendar_forecast_loading');
      return;
    }
    const data = state.data;
    this.weatherIcon.replaceChildren(
      createWeatherIcon(data.icon, prefs.isWeatherIconFill()) ?? document.createTextNode('')
    );
    this.temperature.textContent = t('weather_temperature_format', data.temperature);
    const feelsLike = data.detail?.feelsLike?.trim() ? data.detail.feelsLike : '--';
    this.feels.textContent = t('weather_temperature_format', feelsLike);

    const parts = [locationText(data.city, data.district), data.text];
    if (data.detail) {
      const scale = data.detail.windScale
        ? t('weather_wind_scale_format', data.detail.windScale)
        : '';
      const wind = [data.detail.windDir, scale].filter(Boolean).join(' ');
      if (wind) parts.push(wind);
      if (data.detail.humidity) parts.push(t('weather_humidity_format', data.detail.humidity));
    }
    this.summary.textContent = parts.filter(Boolean).join(' · ');
  }

  private bindForecast(state: DailyForecastState): void {
    const labels = ta('forecast_day_labels');
    if (!state.data) {
      const message = state.message ?? t('calendar_forecast_loading');
      for (const column of this.forecastColumns) {
        column.replaceChildren(text('cal-forecast-text', message));
      }
      return;
    }
    const today = this.today();
    const cursor = new Date(Date.UTC(today.year, today.month0, today.day));
    for (let index = 0; index < this.forecastColumns.length; index++) {
      const column = this.forecastColumns[index];
      const key = dateKey(cursor.getUTCFullYear(), cursor.getUTCMonth(), cursor.getUTCDate());
      const forecast = state.data.entries.find((entry) => entry.fxDate === key) ?? null;
      const nodes: Node[] = [text('cal-forecast-label' + (index === 0 ? ' is-today' : ''), labels[index] ?? '')];
      if (forecast) {
        const icon = createWeatherIcon(forecast.iconDay, prefs.isWeatherIconFill());
        if (icon) {
          icon.classList.add('cal-forecast-icon');
          if (index === 0) icon.classList.add('is-today');
          nodes.push(icon);
        }
        nodes.push(text('cal-forecast-text', forecast.textDay));
        nodes.push(
          text(
            'cal-forecast-text',
            t('weather_temperature_range_format', forecast.tempMin, forecast.tempMax)
          )
        );
      } else {
        nodes.push(text('cal-forecast-text', t('calendar_forecast_unavailable')));
      }
      column.replaceChildren(...nodes);
      cursor.setUTCDate(cursor.getUTCDate() + 1);
    }
  }

  // ---- Month grid ----

  private populateWeekdays(): void {
    const names = ta('calendar_weekday_names');
    const firstDayOfWeek = prefs.getCalendarWeekStart();
    const highlight = prefs.isCalendarHighlightWeekends();
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
    const english = prefs.isClockUseEnglish();
    this.title.textContent = formatDate(
      english ? 'MMMM yyyy' : 'yyyy年M月',
      { ...this.today(), year: this.visibleYear, month0: this.visibleMonth0, day: 1 },
      dateLang(prefs.getClockLanguage())
    );
    const month = createCalendarMonth(
      this.visibleYear,
      this.visibleMonth0,
      this.today(),
      prefs.getCalendarWeekStart()
    );
    for (const carousel of this.cellCarousels) carousel.destroy();
    this.cellCarousels.length = 0;
    this.grid.replaceChildren(...month.days.map((day) => this.createDayCell(day, true)));
    this.updateFooter();
  }

  private renderPreview(direction: number): void {
    const target = addMonths(this.visibleYear, this.visibleMonth0, 1, direction);
    const month = createCalendarMonth(
      target.year,
      target.month0,
      this.today(),
      prefs.getCalendarWeekStart()
    );
    this.preview.replaceChildren(...month.days.map((day) => this.createDayCell(day, false)));
    this.preview.hidden = false;
    this.preview.style.transform = `translateX(${direction * this.grid.clientWidth}px)`;
  }

  private createDayCell(day: CalendarDay, interactive: boolean): HTMLElement {
    const cell = document.createElement('div');
    cell.className = 'cal-day';
    if (!day.currentMonth) cell.classList.add('is-other-month');
    if (day.today) cell.classList.add('is-today');
    if (prefs.isCalendarHighlightWeekends() && isWeekend(day.dayOfWeek)) {
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
    cell.appendChild(number);

    const almanac = almanacOf(
      day.year,
      day.month0,
      day.dayOfMonth,
      prefs.isCalendarMoreFestivals()
    );
    const labelHost = document.createElement('div');
    labelHost.className = 'cal-day-label';
    if (almanac.festivals.length > 0) labelHost.classList.add('has-festival');
    cell.appendChild(labelHost);
    const carousel = new LabelCarousel(labelHost);
    carousel.setItems([almanac.shortLabel, ...almanac.festivals].map((text) => ({ text })));
    if (interactive) {
      carousel.setActive(this.running);
      this.cellCarousels.push(carousel);
      cell.addEventListener('click', () => this.selectDay(day));
    }

    const separator = prefs.isClockUseEnglish() ? ', ' : '，';
    let description = t(
      day.today ? 'calendar_day_today_accessibility' : 'calendar_day_accessibility',
      day.dayOfMonth,
      almanac.shortLabel
    );
    if (almanac.festivals.length > 0) description += separator + almanac.festivals.join(separator);
    if (status) description += t(status.offDay ? 'calendar_day_rest' : 'calendar_day_makeup');
    cell.setAttribute('aria-label', description);
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
    const almanac = almanacOf(this.selected.year, this.selected.month0, this.selected.day);
    const pattern = prefs.isClockUseEnglish() ? prefs.getDatePatternEn() : prefs.getDatePatternCn();
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
      dateLang(prefs.getClockLanguage())
    );
    const items: LabelItem[] = [
      { text: t('calendar_selected_date', formatted, almanac.natural), color: 'var(--text)' },
    ];
    if (almanac.suitable.length > 0) {
      items.push({
        text: t('calendar_suitable_prefix') + almanac.suitable.join(' '),
        color: 'var(--green)',
      });
    }
    if (almanac.avoid.length > 0) {
      items.push({
        text: t('calendar_avoid_prefix') + almanac.avoid.join(' '),
        color: 'var(--red)',
      });
    }
    this.footer.setItems(items);
    this.footer.setActive(this.running);
  }

  // ---- Month navigation ----

  private changeMonth(direction: number): void {
    if (this.animating || direction === 0) return;
    const width = this.grid.clientWidth;
    if (width === 0) {
      this.applyMonthOffset(direction);
      return;
    }
    this.renderPreview(direction);
    this.animating = true;
    const finish = () => {
      this.grid.style.transition = 'none';
      this.preview.style.transition = 'none';
      this.applyMonthOffset(direction);
      this.resetLayers();
      this.animating = false;
    };
    requestAnimationFrame(() => {
      this.grid.style.transition = `transform ${MONTH_ANIMATION_MS}ms cubic-bezier(0, 0, 0.2, 1)`;
      this.preview.style.transition = this.grid.style.transition;
      this.grid.style.transform = `translateX(${direction > 0 ? -width : width}px)`;
      this.preview.style.transform = 'translateX(0)';
      setTimeout(finish, MONTH_ANIMATION_MS);
    });
  }

  private snapBack(): void {
    this.grid.style.transform = 'translateX(0)';
    if (this.preview.hidden) return;
    const width = this.grid.clientWidth;
    const direction = this.preview.style.transform.includes('-') ? -1 : 1;
    this.preview.style.transform = `translateX(${direction * width}px)`;
    setTimeout(() => this.resetLayers(), MONTH_ANIMATION_MS);
  }

  private resetLayers(): void {
    this.grid.style.transform = 'translateX(0)';
    this.preview.hidden = true;
    this.preview.replaceChildren();
    requestAnimationFrame(() => {
      this.grid.style.transition = '';
      this.preview.style.transition = '';
    });
  }

  private applyMonthOffset(direction: number): void {
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
    const today = this.today();
    this.visibleYear = today.year;
    this.visibleMonth0 = today.month0;
    this.selected = { year: today.year, month0: today.month0, day: today.day };
    this.resetLayers();
    this.animating = false;
    this.renderMonth();
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
    cancel.className = 'button button--text';
    cancel.textContent = t('cancel');
    cancel.addEventListener('click', () => dialog.close());
    const confirm = document.createElement('button');
    confirm.className = 'button';
    confirm.textContent = t('ok');
    confirm.addEventListener('click', () => {
      this.visibleYear = Number(yearSelect.value);
      this.visibleMonth0 = Number(monthSelect.value);
      this.selected = { year: this.visibleYear, month0: this.visibleMonth0, day: 1 };
      this.resetLayers();
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
  element.textContent = value;
  return element;
}
