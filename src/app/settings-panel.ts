/**
 * The settings sheet.
 *
 * Ported from com.clockmods.ui.SettingsDialog (the Material 3 variant): a bottom
 * sheet with 样式 / 功能 tabs, each section in a rounded card, edits held locally
 * until 应用 is pressed, and 恢复默认 restoring the documented defaults.
 *
 * Dropped: the 状态栏 group (network and battery icons are not reimplemented).
 * Added: the QWeather credential / proxy fields, which BuildConfig supplied on
 * Android, and the time-source URL for the HTTP calibration.
 */
import {
  DEFAULT_BACKGROUND_COLOR,
  DEFAULT_TEXT_COLOR,
  DEFAULT_ANIMATE_TIME_CHANGES,
  DEFAULT_BLINK_COLON,
  DEFAULT_BOLD_TEXT,
  DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS,
  DEFAULT_CALENDAR_WEEK_START,
  DEFAULT_DATE_FONT_SCALE,
  DEFAULT_DATE_LUNAR_DUAL_LINE,
  DEFAULT_DIM_BACKGROUND,
  DEFAULT_DIM_END_MINUTES,
  DEFAULT_DIM_START_MINUTES,
  DEFAULT_HOURLY_CHIME,
  DEFAULT_HOURLY_CHIME_QUIET,
  DEFAULT_HOURLY_CHIME_QUIET_END,
  DEFAULT_HOURLY_CHIME_QUIET_START,
  DEFAULT_PORTRAIT_STACKED,
  DEFAULT_SCHEDULE_DIM_BACKGROUND,
  DEFAULT_SCREEN_ORIENTATION,
  DEFAULT_SHOW_LUNAR,
  DEFAULT_SHOW_SECONDS,
  DEFAULT_SMALL_SECONDS,
  DEFAULT_TIME_FONT_SCALE,
  DEFAULT_USE_24_HOUR,
  DEFAULT_WEATHER_ENABLED,
  DEFAULT_WEATHER_DETAILED,
  DEFAULT_WEATHER_ICON_DYNAMIC_COLOR,
  DEFAULT_WEATHER_ICON_FILL,
  DEFAULT_WEATHER_INTERVAL_MINUTES,
  CALENDAR_WEEK_START_MONDAY,
  CALENDAR_WEEK_START_SUNDAY,
  LANGUAGE_ENGLISH,
  LANGUAGE_SIMPLIFIED,
  LANGUAGE_TRADITIONAL,
  MAX_FONT_SCALE,
  MIN_FONT_SCALE,
  MODE_COLOR,
  MODE_IMAGE,
  ORIENTATION_FOLLOW_SYSTEM,
  ORIENTATION_LANDSCAPE,
  ORIENTATION_PORTRAIT,
  TRANSITION_FADE,
  TRANSITION_FLIP,
  TRANSITION_SCALE,
  TRANSITION_SLIDE_DOWN,
  TRANSITION_SLIDE_UP,
  WEATHER_INTERVALS,
  WEATHER_LOCATION_AUTOMATIC,
  WEATHER_LOCATION_MANUAL,
  prefs,
} from '../core/prefs';
import { t, ta } from '../core/i18n';
import { FONT_OPTIONS } from '../core/fonts';
import { ZONE_IDS, indexOfZoneId } from '../core/timezone';
import {
  composeCombo,
  comboLabel,
  dateCores,
  isValidPattern,
  preview as previewPattern,
  weekdayCombos,
} from '../format/date-formatter';
import type { Lang } from '../format/date-formatter';
import { hasBackgroundImage, saveBackgroundImage } from '../core/background-store';
import {
  cities,
  cityLabels,
  districts,
  loadCatalog,
  provinceLabels,
  provinces,
} from '../weather/city-catalog';
import type { LocationEntry } from '../weather/city-catalog';
import {
  card,
  element,
  segmented,
  select,
  setTreeEnabled,
  sliderRow,
  subLabel,
  summaryLabel,
  switchRow,
  textField,
  timeButton,
} from '../ui/controls';
import { createColorPicker } from '../ui/color-picker';
import { toast } from '../ui/toast';

const MIN_FONT_PERCENT = Math.round(MIN_FONT_SCALE * 100);
const MAX_FONT_PERCENT = Math.round(MAX_FONT_SCALE * 100);

/** The eight quick swatches SettingsDialog offered for each colour. */
const BACKGROUND_SWATCHES = [
  0xff1d4ed8, 0xff0f766e, 0xff9f1239, 0xff101418, 0xff334155, 0xff3f6212, 0xffb45309, 0xfff8fafc,
];
const TEXT_SWATCHES = [
  0xff38bdf8, 0xff34d399, 0xfff472b6, 0xffffffff, 0xfff8fafc, 0xfffacc15, 0xfff87171, 0xff111827,
];

export type SettingsApplied = (languageChanged: boolean) => void | Promise<void>;

export function openSettings(onApplied: SettingsApplied): void {
  new SettingsPanel(onApplied).show();
}

class SettingsPanel {
  private readonly dialog = element('dialog', 'settings-sheet');
  private readonly styleBoard = element('div', 'settings-board');
  private readonly functionBoard = element('div', 'settings-board');
  private readonly dateFormatContainer = element('div', 'settings-date-format');

  // Local edit state; nothing is written until 应用.
  private backgroundMode = prefs.getBackgroundMode();
  private backgroundColor = prefs.getBackgroundColor();
  private timeColor = prefs.getTimeColor();
  private dateColor = prefs.getDateColor();
  private readonly originalLanguage = prefs.getClockLanguage();
  private selectedLanguage = prefs.getClockLanguage();
  private pendingPatternCn = prefs.getDatePatternCn();
  private pendingPatternEn = prefs.getDatePatternEn();
  private dateSectionEnglish = prefs.getClockLanguage() === LANGUAGE_ENGLISH;
  private coreIndexCn = 0;
  private coreIndexEn = 0;
  private comboIndexCn = 0;
  private comboIndexEn = 0;
  private customTextCn = prefs.getDateCustomText(false);
  private customTextEn = prefs.getDateCustomText(true);
  private weatherLocation = {
    id: prefs.getWeatherLocationId(),
    province: prefs.getWeatherProvince(),
    city: prefs.getWeatherCity(),
    district: prefs.getWeatherDistrict(),
    latitude: prefs.getWeatherLatitude(),
    longitude: prefs.getWeatherLongitude(),
  };

  // Controls that later code reads back.
  private controls!: ReturnType<SettingsPanel['buildControls']>;
  private catalog: LocationEntry[] | null = null;

  constructor(private readonly onApplied: SettingsApplied) {}

  show(): void {
    this.controls = this.buildControls();
    this.buildLayout();
    document.body.appendChild(this.dialog);
    this.dialog.showModal();
    this.dialog.addEventListener('close', () => this.dialog.remove());
  }

  // ---- Construction ----

  private buildControls() {
    const backgroundPicker = createColorPicker(this.backgroundColor, BACKGROUND_SWATCHES, {
      advanced: t('advanced'),
      swatch: (hex) => t('use_color_value', hex),
    });
    backgroundPicker.onChange((color) => {
      this.backgroundColor = color;
      this.backgroundMode = MODE_COLOR;
      modeGroup.setValue(MODE_COLOR);
    });

    const modeGroup = segmented(
      [
        { value: MODE_COLOR, label: t('solid_color') },
        { value: MODE_IMAGE, label: t('background_image') },
      ],
      this.backgroundMode
    );

    const timePicker = createColorPicker(this.timeColor, TEXT_SWATCHES, {
      advanced: t('advanced'),
      swatch: (hex) => t('use_color_value', hex),
    });
    timePicker.onChange((color) => {
      this.timeColor = color;
    });
    const datePicker = createColorPicker(this.dateColor, TEXT_SWATCHES, {
      advanced: t('advanced'),
      swatch: (hex) => t('use_color_value', hex),
    });
    datePicker.onChange((color) => {
      this.dateColor = color;
    });

    const percent = (value: number) => t('font_size_percent', Math.round(value));
    return {
      modeGroup,
      backgroundPicker,
      timePicker,
      datePicker,
      dimBackground: switchRow(t('dim_background'), prefs.isDimBackground()),
      scheduleDim: switchRow(t('schedule_dim_background'), prefs.isScheduleDimBackground()),
      dimStart: timeButton(t('dim_start_time'), prefs.getDimStartMinutes()),
      dimEnd: timeButton(t('dim_end_time'), prefs.getDimEndMinutes()),
      fontFamily: select(
        FONT_OPTIONS.map((option) => ({
          value: option.id,
          label: option.system && option.id === 'system' ? t('font_system') : option.displayName,
        })),
        prefs.getFontFamily()
      ),
      boldText: switchRow(t('bold_text'), prefs.isBoldText()),
      timeSize: sliderRow(
        t('font_size'),
        MIN_FONT_PERCENT,
        MAX_FONT_PERCENT,
        Math.round(prefs.getTimeFontScale() * 100),
        percent
      ),
      dateSize: sliderRow(
        t('font_size'),
        MIN_FONT_PERCENT,
        MAX_FONT_PERCENT,
        Math.round(prefs.getDateFontScale() * 100),
        percent
      ),
      transition: select(
        [TRANSITION_FADE, TRANSITION_SLIDE_UP, TRANSITION_SLIDE_DOWN, TRANSITION_SCALE, TRANSITION_FLIP].map(
          (value, index) => ({ value, label: ta('pro_time_transitions')[index] ?? value })
        ),
        prefs.getTimeTransition()
      ),
      blinkColon: switchRow(t('blink_colon'), prefs.isBlinkColon()),
      animate: switchRow(t('animate_time_changes'), prefs.isAnimateTimeChanges()),
      portraitStacked: switchRow(t('portrait_stacked_clock'), prefs.isPortraitStacked()),
      hourlyChime: switchRow(t('pro_hourly_chime'), prefs.isHourlyChimeEnabled()),
      hourlyQuiet: switchRow(t('pro_hourly_chime_quiet'), prefs.isHourlyChimeQuietEnabled()),
      quietStart: timeButton(t('pro_quiet_start', ''), prefs.getHourlyChimeQuietStart()),
      quietEnd: timeButton(t('pro_quiet_end', ''), prefs.getHourlyChimeQuietEnd()),
      showSeconds: switchRow(t('show_seconds'), prefs.isShowSeconds()),
      smallSeconds: switchRow(t('small_seconds'), prefs.isSmallSeconds()),
      showLunar: switchRow(t('show_lunar'), prefs.isShowLunar()),
      dualLine: switchRow(
        t('date_lunar_dual_line'),
        prefs.isDateLunarDualLine(),
        t('date_lunar_dual_line_desc')
      ),
      orientation: segmented(
        [
          { value: ORIENTATION_FOLLOW_SYSTEM, label: t('orientation_follow_system') },
          { value: ORIENTATION_PORTRAIT, label: t('orientation_portrait') },
          { value: ORIENTATION_LANDSCAPE, label: t('orientation_landscape') },
        ],
        prefs.getScreenOrientation()
      ),
      use24Hour: switchRow(t('use_24_hour'), prefs.isUse24Hour()),
      networkTime: switchRow(
        t('use_network_time'),
        prefs.isUseNetworkTime(),
        t('use_network_time_desc')
      ),
      syncInterval: segmented(
        [
          { value: 30, label: t('sync_interval_30min') },
          { value: 60, label: t('sync_interval_1hour') },
          { value: 360, label: t('sync_interval_6hour') },
          { value: 1440, label: t('sync_interval_1day') },
        ],
        prefs.getSyncIntervalMinutes()
      ),
      timeSourceUrl: textField(prefs.getTimeSourceUrl(), t('time_source_url_hint'), 'url'),
      region: select(
        ZONE_IDS.map((id, index) => ({ value: id, label: ta('region_names')[index] ?? id })),
        ZONE_IDS[indexOfZoneId(prefs.getTimeZoneId())]
      ),
      language: segmented(
        [
          { value: LANGUAGE_SIMPLIFIED, label: t('clock_language_simplified') },
          { value: LANGUAGE_TRADITIONAL, label: t('clock_language_traditional') },
          { value: LANGUAGE_ENGLISH, label: t('clock_language_english') },
        ],
        this.selectedLanguage
      ),
      customMessage: textField(prefs.getCustomMessage(), t('custom_message_hint')),
      weatherEnabled: switchRow(t('show_weather'), prefs.isWeatherEnabled()),
      locationMode: select(
        [
          { value: WEATHER_LOCATION_AUTOMATIC, label: t('weather_location_automatic') },
          { value: WEATHER_LOCATION_MANUAL, label: t('weather_location_manual') },
        ],
        prefs.getWeatherLocationMode()
      ),
      province: select([], ''),
      city: select([], ''),
      district: select([], ''),
      weatherInterval: select(
        WEATHER_INTERVALS.map((minutes) => ({
          value: String(minutes),
          label: t(
            minutes === 10
              ? 'weather_interval_10min'
              : minutes === 30
                ? 'weather_interval_30min'
                : minutes === 60
                  ? 'weather_interval_1hour'
                  : minutes === 180
                    ? 'weather_interval_3hour'
                    : minutes === 360
                      ? 'weather_interval_6hour'
                      : 'weather_interval_12hour'
          ),
        })),
        String(prefs.getWeatherIntervalMinutes())
      ),
      weatherDetailed: switchRow(t('show_detailed_weather'), prefs.isWeatherDetailed()),
      iconFill: switchRow(t('weather_icon_fill'), prefs.isWeatherIconFill(), t('weather_icon_fill_desc')),
      iconDynamic: switchRow(
        t('weather_icon_dynamic_color'),
        prefs.isWeatherIconDynamicColor(),
        t('weather_icon_dynamic_color_desc')
      ),
      apiHost: textField(prefs.getQWeatherApiHost(), 'devapi.qweather.com'),
      credentialId: textField(prefs.getQWeatherCredentialId(), '', 'password'),
      projectId: textField(prefs.getQWeatherProjectId(), '', 'password'),
      privateKey: textField(prefs.getQWeatherPrivateKey(), '', 'password'),
      proxy: textField(prefs.getQWeatherProxy(), 'https://…', 'url'),
      weekStart: segmented(
        [
          { value: CALENDAR_WEEK_START_SUNDAY, label: t('calendar_week_start_sunday') },
          { value: CALENDAR_WEEK_START_MONDAY, label: t('calendar_week_start_monday') },
        ],
        prefs.getCalendarWeekStart()
      ),
      highlightWeekends: switchRow(
        t('calendar_highlight_weekends'),
        prefs.isCalendarHighlightWeekends()
      ),
    };
  }

  private buildLayout(): void {
    const c = this.controls;

    // ---- Header ----
    const header = element('header', 'settings-header');
    header.appendChild(element('h2', 'settings-title', t('background_settings')));
    const cancel = element('button', 'm3-button m3-button--text', t('cancel'));
    cancel.type = 'button';
    cancel.addEventListener('click', () => this.dialog.close());
    const apply = element('button', 'm3-button m3-button--filled', t('apply'));
    apply.type = 'button';
    apply.addEventListener('click', () => void this.applySelection());
    header.append(cancel, apply);

    // ---- Tabs ----
    const tabs = segmented(
      [
        { value: 'style', label: t('tab_style') },
        { value: 'function', label: t('tab_function') },
      ],
      'style'
    );
    tabs.onChange((value) => {
      this.styleBoard.hidden = value !== 'style';
      this.functionBoard.hidden = value === 'style';
    });
    this.functionBoard.hidden = true;

    // ---- Style board ----
    const colorControls = element('div', 'settings-group');
    colorControls.append(c.backgroundPicker.root);

    const imageControls = element('div', 'settings-group');
    const chooseImage = element('button', 'm3-button m3-button--outlined', t('choose_image'));
    chooseImage.type = 'button';
    const filePicker = element('input');
    filePicker.type = 'file';
    filePicker.accept = 'image/*';
    filePicker.hidden = true;
    chooseImage.addEventListener('click', () => filePicker.click());
    filePicker.addEventListener('change', async () => {
      const file = filePicker.files?.[0];
      if (!file) return;
      try {
        const longEdge = Math.max(screen.width, screen.height) * (devicePixelRatio || 1);
        await saveBackgroundImage(file, Math.round(longEdge));
        this.backgroundMode = MODE_IMAGE;
        c.modeGroup.setValue(MODE_IMAGE);
        toast(t('image_saved'));
      } catch {
        toast(t('image_error'));
      }
    });
    const dimSchedule = element('div', 'settings-row');
    dimSchedule.append(c.dimStart.button, c.dimEnd.button);
    imageControls.append(
      chooseImage,
      filePicker,
      c.dimBackground.row,
      c.scheduleDim.row,
      summaryLabel(t('dim_schedule_note')),
      dimSchedule
    );
    const syncDimState = () => setTreeEnabled(dimSchedule, c.scheduleDim.input.checked);
    c.scheduleDim.input.addEventListener('change', syncDimState);
    syncDimState();

    c.modeGroup.onChange((mode) => {
      this.backgroundMode = mode;
      colorControls.hidden = mode === MODE_IMAGE;
      imageControls.hidden = mode !== MODE_IMAGE;
    });
    colorControls.hidden = this.backgroundMode === MODE_IMAGE;
    imageControls.hidden = this.backgroundMode !== MODE_IMAGE;

    this.styleBoard.append(
      card(t('background_settings_group'), c.modeGroup.row, colorControls, imageControls),
      card(
        t('font_settings_group'),
        subLabel(t('font_family')),
        c.fontFamily,
        c.boldText.row,
        subLabel(t('time_font_settings')),
        c.timeSize.row,
        subLabel(t('font_color')),
        c.timePicker.root,
        subLabel(t('pro_time_transition')),
        c.transition,
        c.blinkColon.row,
        c.animate.row,
        c.portraitStacked.row,
        c.hourlyChime.row,
        c.hourlyQuiet.row,
        quietRow(c),
        c.showSeconds.row,
        c.smallSeconds.row,
        subLabel(t('date_font_settings')),
        c.dateSize.row,
        subLabel(t('font_color')),
        c.datePicker.root,
        c.showLunar.row,
        c.dualLine.row
      )
    );
    const syncSmallSeconds = () => {
      c.smallSeconds.input.disabled = !c.showSeconds.input.checked;
      c.smallSeconds.row.classList.toggle('is-disabled', !c.showSeconds.input.checked);
    };
    c.showSeconds.input.addEventListener('change', syncSmallSeconds);
    syncSmallSeconds();

    // ---- Function board ----
    const syncControls = element('div', 'settings-group');
    syncControls.append(subLabel(t('sync_interval')), c.syncInterval.row, subLabel(t('time_source_url')), c.timeSourceUrl);
    const syncNetworkState = () => setTreeEnabled(syncControls, c.networkTime.input.checked);
    c.networkTime.input.addEventListener('change', syncNetworkState);
    syncNetworkState();

    this.rebuildDateFormat();
    c.language.onChange((language) => {
      this.captureDateSection();
      this.selectedLanguage = language;
      this.dateSectionEnglish = language === LANGUAGE_ENGLISH;
      this.rebuildDateFormat();
    });

    const locationRow = element('div', 'settings-group');
    locationRow.append(c.province, c.city, c.district);
    const weatherCredentials = card(
      t('weather_credentials_group'),
      summaryLabel(t('weather_credentials_desc')),
      subLabel(t('weather_api_host')),
      c.apiHost,
      subLabel(t('weather_credential_id')),
      c.credentialId,
      subLabel(t('weather_project_id')),
      c.projectId,
      subLabel(t('weather_private_key')),
      c.privateKey,
      subLabel(t('weather_proxy')),
      c.proxy,
      summaryLabel(t('weather_proxy_desc')),
      summaryLabel(t('weather_cors_hint'))
    );

    this.functionBoard.append(
      card(t('display_settings_group'), subLabel(t('screen_orientation')), c.orientation.row),
      card(t('time_settings_group'), c.use24Hour.row, c.networkTime.row, syncControls),
      card(t('region_settings_group'), subLabel(t('region_time_zone')), c.region),
      card(t('clock_language_settings_group'), c.language.row),
      card(t('date_format_settings_group'), this.dateFormatContainer),
      card(t('custom_message_settings_group'), c.customMessage),
      card(
        t('weather_settings_group'),
        c.weatherEnabled.row,
        subLabel(t('weather_location')),
        c.locationMode,
        locationRow,
        subLabel(t('weather_update_interval')),
        c.weatherInterval,
        c.weatherDetailed.row,
        c.iconFill.row,
        c.iconDynamic.row
      ),
      weatherCredentials,
      card(
        t('calendar_settings_group'),
        subLabel(t('calendar_week_start')),
        c.weekStart.row,
        c.highlightWeekends.row
      )
    );

    const syncWeatherState = () => {
      const enabled = c.weatherEnabled.input.checked;
      const manual = enabled && c.locationMode.value === WEATHER_LOCATION_MANUAL;
      c.locationMode.disabled = !enabled;
      locationRow.hidden = !manual;
      c.weatherInterval.disabled = !enabled;
      c.weatherDetailed.input.disabled = !enabled;
      if (manual) void this.ensureCatalog();
    };
    c.weatherEnabled.input.addEventListener('change', syncWeatherState);
    c.locationMode.addEventListener('change', syncWeatherState);
    syncWeatherState();

    // ---- Footer ----
    const reset = element('button', 'm3-button m3-button--outlined settings-reset', t('reset_default'));
    reset.type = 'button';
    reset.addEventListener('click', () => this.restoreDefaults());

    const body = element('div', 'settings-body');
    body.append(this.styleBoard, this.functionBoard, reset);

    this.dialog.replaceChildren(header, tabs.row, body);
  }

  // ---- Date format section ----

  private lang(english: boolean): Lang {
    if (english) return 'english';
    return this.selectedLanguage === LANGUAGE_TRADITIONAL ? 'traditional' : 'chinese';
  }

  private rebuildDateFormat(): void {
    const english = this.dateSectionEnglish;
    const lang = this.lang(english);
    const cores = dateCores(lang);
    const combos = weekdayCombos(lang);
    const storedCore = prefs.getDateCore(english);
    const storedCombo = prefs.getDateCombo(english);
    const customEnabled = prefs.isDateCustomEnabled(english);
    const coreIndex = customEnabled
      ? cores.length
      : Math.max(0, cores.indexOf(storedCore) >= 0 ? cores.indexOf(storedCore) : defaultCoreIndex(cores, english));
    const comboIndex =
      combos.indexOf(storedCombo) >= 0 ? combos.indexOf(storedCombo) : combos.indexOf('DATE EEEE');
    this.setCoreIndex(english, coreIndex);
    this.setComboIndex(english, Math.max(0, comboIndex));

    const coreSelect = select(
      [
        ...cores.map((core, index) => ({
          value: String(index),
          label: previewPattern(core, lang),
        })),
        { value: String(cores.length), label: t('date_format_custom') },
      ],
      String(coreIndex)
    );
    const comboSelect = select(
      combos.map((combo, index) => ({ value: String(index), label: comboLabel(combo, lang) })),
      String(Math.max(0, comboIndex))
    );

    const comboRow = element('div', 'settings-group');
    comboRow.append(subLabel(t('date_format_weekday_label')), comboSelect);

    const customRow = element('div', 'settings-group');
    const help = element('button', 'm3-button m3-button--outlined', t('date_format_help_button'));
    help.type = 'button';
    help.addEventListener('click', () => this.showDateFormatHelp(english));
    const customInput = textField(this.customText(english), t('date_format_custom_hint'));
    const error = element('p', 'settings-error', t('date_format_custom_invalid'));
    error.hidden = true;
    customRow.append(help, customInput, error);

    const previewLabel = element('p', 'settings-preview');

    const currentPattern = (): string => {
      const index = Number(coreSelect.value);
      if (index === cores.length) return customInput.value;
      return composeCombo(combos[Number(comboSelect.value)] ?? 'DATE', cores[index] ?? cores[0]);
    };
    const update = () => {
      const isCustom = Number(coreSelect.value) === cores.length;
      comboRow.hidden = isCustom;
      customRow.hidden = !isCustom;
      const pattern = currentPattern();
      const valid = isValidPattern(pattern);
      error.hidden = !isCustom || valid;
      if (valid) this.setPendingPattern(english, pattern);
      previewLabel.textContent = t(
        'date_format_preview',
        previewPattern(this.pendingPattern(english), lang)
      );
      this.setCoreIndex(english, Number(coreSelect.value));
      this.setComboIndex(english, Number(comboSelect.value));
      this.setCustomText(english, customInput.value);
    };
    coreSelect.addEventListener('change', update);
    comboSelect.addEventListener('change', update);
    customInput.addEventListener('input', update);

    this.dateFormatContainer.replaceChildren(
      subLabel(t('date_format_date_label')),
      coreSelect,
      comboRow,
      customRow,
      previewLabel
    );
    update();
  }

  private showDateFormatHelp(english: boolean): void {
    const dialog = element('dialog', 'help-dialog');
    const heading = element('h2', undefined, t('date_format_help_title'));
    const body = element('pre', 'help-body', t(english ? 'date_format_help_body_en' : 'date_format_help_body_cn'));
    const close = element('button', 'm3-button m3-button--filled', t('ok'));
    close.type = 'button';
    close.addEventListener('click', () => dialog.close());
    dialog.append(heading, body, close);
    dialog.addEventListener('close', () => dialog.remove());
    document.body.appendChild(dialog);
    dialog.showModal();
  }

  private captureDateSection(): void {
    // The visible section already writes back on every change; nothing further to
    // capture, but the hook mirrors SettingsDialog.captureDateSectionInto.
  }

  private pendingPattern(english: boolean): string {
    return english ? this.pendingPatternEn : this.pendingPatternCn;
  }

  private setPendingPattern(english: boolean, pattern: string): void {
    if (english) this.pendingPatternEn = pattern;
    else this.pendingPatternCn = pattern;
  }

  private customText(english: boolean): string {
    return english ? this.customTextEn : this.customTextCn;
  }

  private setCustomText(english: boolean, value: string): void {
    if (english) this.customTextEn = value;
    else this.customTextCn = value;
  }

  private setCoreIndex(english: boolean, value: number): void {
    if (english) this.coreIndexEn = value;
    else this.coreIndexCn = value;
  }

  private setComboIndex(english: boolean, value: number): void {
    if (english) this.comboIndexEn = value;
    else this.comboIndexCn = value;
  }

  // ---- Weather location ----

  private async ensureCatalog(): Promise<void> {
    if (this.catalog) return;
    try {
      this.catalog = await loadCatalog();
    } catch {
      toast(t('weather_location_list_error'));
      return;
    }
    this.populateLocationSelects();
  }

  private populateLocationSelects(): void {
    const catalog = this.catalog;
    if (!catalog) return;
    const english = this.selectedLanguage === LANGUAGE_ENGLISH;
    const c = this.controls;

    const provinceIds = provinces(catalog);
    const provinceNames = provinceLabels(catalog, english);
    fillSelect(
      c.province,
      provinceIds.map((id, index) => ({ value: id, label: provinceNames[index] })),
      this.weatherLocation.province || provinceIds[0]
    );

    const fillCities = () => {
      const province = c.province.value;
      const ids = cities(catalog, province);
      const names = cityLabels(catalog, province, english);
      fillSelect(
        c.city,
        ids.map((id, index) => ({ value: id, label: names[index] })),
        ids.includes(this.weatherLocation.city) ? this.weatherLocation.city : ids[0]
      );
      fillDistricts();
    };
    const fillDistricts = () => {
      const entries = districts(catalog, c.province.value, c.city.value);
      fillSelect(
        c.district,
        entries.map((entry) => ({
          value: entry.locationId,
          label: english ? entry.districtEn : entry.district,
        })),
        entries.some((entry) => entry.locationId === this.weatherLocation.id)
          ? this.weatherLocation.id
          : (entries[0]?.locationId ?? '')
      );
      this.captureLocation(entries);
    };
    c.province.onchange = fillCities;
    c.city.onchange = fillDistricts;
    c.district.onchange = () => this.captureLocation(districts(catalog, c.province.value, c.city.value));
    fillCities();
  }

  private captureLocation(entries: LocationEntry[]): void {
    const selected = entries.find((entry) => entry.locationId === this.controls.district.value);
    if (!selected) return;
    this.weatherLocation = {
      id: selected.locationId,
      province: selected.province,
      city: selected.city,
      district: selected.district,
      latitude: selected.latitude,
      longitude: selected.longitude,
    };
  }

  // ---- Apply / reset ----

  private async applySelection(): Promise<void> {
    const c = this.controls;
    const manual = c.locationMode.value === WEATHER_LOCATION_MANUAL;
    if (c.weatherEnabled.input.checked && manual && !this.weatherLocation.id) {
      toast(t('weather_location_not_selected'));
      return;
    }
    if (this.backgroundMode === MODE_IMAGE && !(await hasBackgroundImage())) {
      toast(t('select_image_first'));
      return;
    }

    prefs.setBackgroundMode(this.backgroundMode);
    prefs.setBackgroundColor(this.backgroundColor);
    prefs.setDimBackground(c.dimBackground.input.checked);
    prefs.setScheduleDimBackground(c.scheduleDim.input.checked);
    prefs.setDimStartMinutes(c.dimStart.minutes());
    prefs.setDimEndMinutes(c.dimEnd.minutes());

    prefs.setFontFamily(c.fontFamily.value);
    prefs.setBoldText(c.boldText.input.checked);
    prefs.setTimeFontScale(Number(c.timeSize.input.value) / 100);
    prefs.setDateFontScale(Number(c.dateSize.input.value) / 100);
    prefs.setTimeColor(this.timeColor);
    prefs.setDateColor(this.dateColor);
    prefs.setTimeTransition(c.transition.value);
    prefs.setBlinkColon(c.blinkColon.input.checked);
    prefs.setAnimateTimeChanges(c.animate.input.checked);
    prefs.setPortraitStacked(c.portraitStacked.input.checked);
    prefs.setHourlyChimeEnabled(c.hourlyChime.input.checked);
    prefs.setHourlyChimeQuietEnabled(c.hourlyQuiet.input.checked);
    prefs.setHourlyChimeQuietStart(c.quietStart.minutes());
    prefs.setHourlyChimeQuietEnd(c.quietEnd.minutes());
    prefs.setShowSeconds(c.showSeconds.input.checked);
    prefs.setSmallSeconds(c.showSeconds.input.checked && c.smallSeconds.input.checked);
    prefs.setShowLunar(c.showLunar.input.checked);
    prefs.setDateLunarDualLine(c.dualLine.input.checked);

    prefs.setScreenOrientation(c.orientation.value());
    prefs.setUse24Hour(c.use24Hour.input.checked);
    prefs.setUseNetworkTime(c.networkTime.input.checked);
    prefs.setSyncIntervalMinutes(c.syncInterval.value());
    prefs.setTimeSourceUrl(c.timeSourceUrl.value);
    prefs.setTimeZoneId(c.region.value);
    prefs.setClockLanguage(this.selectedLanguage);
    prefs.setDatePatternCn(this.pendingPatternCn);
    prefs.setDatePatternEn(this.pendingPatternEn);
    this.persistDateState(false);
    this.persistDateState(true);
    prefs.setCustomMessage(c.customMessage.value);

    prefs.setWeatherEnabled(c.weatherEnabled.input.checked);
    prefs.setWeatherLocationMode(c.locationMode.value);
    prefs.setManualWeatherLocation(
      this.weatherLocation.id,
      this.weatherLocation.province,
      this.weatherLocation.city,
      this.weatherLocation.district,
      this.weatherLocation.latitude,
      this.weatherLocation.longitude
    );
    prefs.setWeatherIntervalMinutes(Number(c.weatherInterval.value));
    prefs.setWeatherDetailed(c.weatherDetailed.input.checked);
    prefs.setWeatherIconFill(c.iconFill.input.checked);
    prefs.setWeatherIconDynamicColor(c.iconDynamic.input.checked);
    prefs.setQWeatherCredentials(
      c.apiHost.value,
      c.credentialId.value,
      c.projectId.value,
      c.privateKey.value,
      c.proxy.value
    );

    prefs.setCalendarWeekStart(c.weekStart.value());
    prefs.setCalendarHighlightWeekends(c.highlightWeekends.input.checked);

    const languageChanged = this.selectedLanguage !== this.originalLanguage;
    this.dialog.close();
    await this.onApplied(languageChanged);
  }

  private persistDateState(english: boolean): void {
    const lang = this.lang(english);
    const cores = dateCores(lang);
    const combos = weekdayCombos(lang);
    const coreIndex = english ? this.coreIndexEn : this.coreIndexCn;
    const comboIndex = english ? this.comboIndexEn : this.comboIndexCn;
    const custom = coreIndex === cores.length;
    prefs.setDateFormatState(
      english,
      custom ? '' : (cores[coreIndex] ?? ''),
      combos[comboIndex] ?? '',
      custom,
      this.customText(english)
    );
  }

  /** Restores the documented defaults in the sheet, without writing them yet. */
  private restoreDefaults(): void {
    const c = this.controls;
    this.backgroundMode = MODE_COLOR;
    this.backgroundColor = DEFAULT_BACKGROUND_COLOR;
    this.timeColor = DEFAULT_TEXT_COLOR;
    this.dateColor = DEFAULT_TEXT_COLOR;
    c.modeGroup.setValue(MODE_COLOR);
    c.backgroundPicker.setValue(DEFAULT_BACKGROUND_COLOR);
    c.timePicker.setValue(DEFAULT_TEXT_COLOR);
    c.datePicker.setValue(DEFAULT_TEXT_COLOR);
    c.dimBackground.input.checked = DEFAULT_DIM_BACKGROUND;
    c.scheduleDim.input.checked = DEFAULT_SCHEDULE_DIM_BACKGROUND;
    setTimeValue(c.dimStart, DEFAULT_DIM_START_MINUTES);
    setTimeValue(c.dimEnd, DEFAULT_DIM_END_MINUTES);
    c.fontFamily.value = 'system';
    c.boldText.input.checked = DEFAULT_BOLD_TEXT;
    setSlider(c.timeSize, DEFAULT_TIME_FONT_SCALE);
    setSlider(c.dateSize, DEFAULT_DATE_FONT_SCALE);
    c.transition.value = TRANSITION_FADE;
    c.blinkColon.input.checked = DEFAULT_BLINK_COLON;
    c.animate.input.checked = DEFAULT_ANIMATE_TIME_CHANGES;
    c.portraitStacked.input.checked = DEFAULT_PORTRAIT_STACKED;
    c.hourlyChime.input.checked = DEFAULT_HOURLY_CHIME;
    c.hourlyQuiet.input.checked = DEFAULT_HOURLY_CHIME_QUIET;
    setTimeValue(c.quietStart, DEFAULT_HOURLY_CHIME_QUIET_START);
    setTimeValue(c.quietEnd, DEFAULT_HOURLY_CHIME_QUIET_END);
    c.showSeconds.input.checked = DEFAULT_SHOW_SECONDS;
    c.smallSeconds.input.checked = DEFAULT_SMALL_SECONDS;
    c.showLunar.input.checked = DEFAULT_SHOW_LUNAR;
    c.dualLine.input.checked = DEFAULT_DATE_LUNAR_DUAL_LINE;
    c.orientation.setValue(DEFAULT_SCREEN_ORIENTATION);
    c.use24Hour.input.checked = DEFAULT_USE_24_HOUR;
    c.language.setValue(LANGUAGE_SIMPLIFIED);
    this.selectedLanguage = LANGUAGE_SIMPLIFIED;
    this.dateSectionEnglish = false;
    this.pendingPatternCn = prefs.getDatePatternCn();
    this.pendingPatternEn = prefs.getDatePatternEn();
    this.rebuildDateFormat();
    c.customMessage.value = '';
    c.weatherEnabled.input.checked = DEFAULT_WEATHER_ENABLED;
    c.locationMode.value = WEATHER_LOCATION_AUTOMATIC;
    c.weatherInterval.value = String(DEFAULT_WEATHER_INTERVAL_MINUTES);
    c.weatherDetailed.input.checked = DEFAULT_WEATHER_DETAILED;
    c.iconFill.input.checked = DEFAULT_WEATHER_ICON_FILL;
    c.iconDynamic.input.checked = DEFAULT_WEATHER_ICON_DYNAMIC_COLOR;
    c.weekStart.setValue(DEFAULT_CALENDAR_WEEK_START);
    c.highlightWeekends.input.checked = DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS;
    toast(t('reset_default'));
  }
}

function quietRow(controls: { quietStart: { button: HTMLElement }; quietEnd: { button: HTMLElement } }): HTMLElement {
  const row = element('div', 'settings-row');
  row.append(controls.quietStart.button, controls.quietEnd.button);
  return row;
}

function defaultCoreIndex(cores: string[], english: boolean): number {
  const index = cores.indexOf(english ? 'yyyy/M/d' : 'yyyy年M月d日');
  return index >= 0 ? index : 0;
}

function fillSelect(
  node: HTMLSelectElement,
  options: Array<{ value: string; label: string }>,
  selected: string
): void {
  node.replaceChildren(
    ...options.map((option) => {
      const item = element('option', undefined, option.label);
      item.value = option.value;
      return item;
    })
  );
  if (options.some((option) => option.value === selected)) node.value = selected;
  else if (options.length > 0) node.value = options[0].value;
}

function setSlider(slider: { input: HTMLInputElement }, scale: number): void {
  slider.input.value = String(Math.round(scale * 100));
  slider.input.dispatchEvent(new Event('input'));
}

function setTimeValue(button: { button: HTMLElement }, minutes: number): void {
  const input = button.button.querySelector<HTMLInputElement>('input[type="time"]');
  if (!input) return;
  input.value = `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
  input.dispatchEvent(new Event('change'));
}
