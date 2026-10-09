import {secondaryFontSize,secondarySizeValue,saveSecondarySize} from '../ui/secondary-font-size';
import type { StylePreview, PreviewDraft } from '../ui/style-preview';
import { fontWeightControl } from '../ui/font-weight';
import { ultimateSettings } from '../ui/ultimate-settings';
import { themeSettings } from '../ui/theme-settings';
import { transitionName } from '../core/clock-themes';
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
  MODE_THEME,
  MODE_COLOR,
  MODE_IMAGE,
  ORIENTATION_FOLLOW_SYSTEM,
  ORIENTATION_LANDSCAPE,
  ORIENTATION_PORTRAIT,
  TRANSITION_FADE,
  TRANSITION_SLIDE_RIGHT,
  TRANSITION_SCAN,
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
import { fontPicker, setFontSelection, pendingFontSelections } from '../ui/font-picker';
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
  private readonly themes = themeSettings();
  private readonly ultimate = ultimateSettings();
  private pendingImage: File | null = null;
  private preview?: StylePreview;
  private readonly previewHost = element('div', 'settings-preview-host');
  private previewCategory = 0;
  private activeTheme = prefs.getClockTheme();
  private readonly typography = new Map<string, {font:string;bold:boolean;weight:number;time:number;date:number;transition:string;animate:boolean}>();
  private resetThemes = false;
  private captureTypography(): void {
    const c=this.controls;
    this.typography.set(this.activeTheme,{font:c.fontFamily.value,bold:c.boldText.input.checked,weight:c.fontWeight.value(),time:+c.timeSize.input.value/100,date:c.dateSize.value(),transition:c.transition.value,animate:c.animate.input.checked});
  }
  private selectTheme(id: string): void {
    this.captureTypography(); this.activeTheme=id;
    const v=this.typography.get(id) ?? {font:this.resetThemes?'system':prefs.getFontFamily(id),bold:!this.resetThemes&&prefs.isBoldText(id),weight:this.resetThemes?400:prefs.getFontWeight(id),time:this.resetThemes ? .88:prefs.getTimeFontScale(id),date:secondarySizeValue('date',id,this.resetThemes),transition:this.resetThemes?'fade':prefs.getTimeTransition(id),animate:this.resetThemes||prefs.isAnimateTimeChanges(id)};
    const c=this.controls;setFontSelection(c.fontFamily,v.font);c.boldText.input.checked=v.bold;c.fontWeight.set(v.font,v.weight);setSlider(c.timeSize,v.time);c.dateSize.setTheme(id,v.date);c.transition.value=v.transition;c.animate.input.checked=v.animate;
    this.ultimate.changeTheme(id);
    for(const row of [c.blinkColon.row,c.portraitStacked.row,c.smallSeconds.row,c.dualLine.row]) row.hidden=id!=='classic';
    c.transition.disabled=id==='digital.grid'||!c.animate.input.checked;c.animate.row.hidden=id==='digital.grid';
  }
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
  private resetDateDefaults = false;
  private readonly initializedDates = new Set<boolean>();
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
    void import('../ui/style-preview').then(({StylePreview}) => {
      if (!this.dialog.open) return;
      this.preview = new StylePreview(() => this.previewDraft());
      this.previewHost.append(this.preview.element);
      this.preview.setCategory(this.previewCategory);
      this.preview.mount();
    }).catch(() => { if (this.dialog.open) this.previewHost.textContent = prefs.isClockUseEnglish() ? 'Preview unavailable' : '预览暂不可用'; });
    for (const event of ['input','change','click','reset-settings']) this.dialog.addEventListener(event, () => this.preview?.request());
    this.dialog.addEventListener('close', () => { this.preview?.destroy(); this.dialog.remove(); });
  }

  private previewDraft(): PreviewDraft {
    const c = this.controls, extra = this.ultimate.previewSettings();
    const typography: Partial<typeof prefs> = this.previewCategory === 1 || this.previewCategory === 5 ? extra : {
      getFontFamily:()=>c.fontFamily.value,getFontWeight:()=>c.fontWeight.value(),hasFontWeight:()=>true,
      isBoldText:()=>c.fontWeight.value()>=700,getTimeFontScale:()=>+c.timeSize.input.value/100,getDateFontScale:()=>c.dateSize.value()/100,getDateFontSize:()=>c.dateSize.value(),
    };
    const settings: typeof prefs = {...prefs,...this.themes.previewSettings(),...typography,
      getUltimateOptions:extra.getUltimateOptions!, getBackgroundMode:()=>this.backgroundMode,getBackgroundColor:()=>this.backgroundColor,
      getTimeColor:()=>this.timeColor,getDateColor:()=>this.dateColor,
      isDimBackground:()=>c.dimBackground.input.checked,isScheduleDimBackground:()=>c.scheduleDim.input.checked,
      getDimStartMinutes:()=>c.dimStart.minutes(),getDimEndMinutes:()=>c.dimEnd.minutes(),
      isShowSeconds:()=>c.showSeconds.input.checked,isSmallSeconds:()=>c.smallSeconds.input.checked,isShowLunar:()=>c.showLunar.input.checked,
      isUse24Hour:()=>c.use24Hour.input.checked,isBlinkColon:()=>c.blinkColon.input.checked,isPortraitStacked:()=>c.portraitStacked.input.checked,
      isDateLunarDualLine:()=>c.dualLine.input.checked,getTimeZoneId:()=>c.region.value,
      getClockLanguage:()=>this.selectedLanguage,isClockUseEnglish:()=>this.selectedLanguage==='en',
      getDatePatternCn:()=>this.pendingPatternCn,getDatePatternEn:()=>this.pendingPatternEn,
      getCustomMessage:()=>c.customMessage.value,isWeatherEnabled:()=>c.weatherEnabled.input.checked,isWeatherDetailed:()=>c.weatherDetailed.input.checked,
      isWeatherIconFill:()=>c.iconFill.input.checked,isWeatherIconDynamicColor:()=>c.iconDynamic.input.checked,
      getCalendarWeekStart:()=>c.weekStart.value(),isCalendarHighlightWeekends:()=>c.highlightWeekends.input.checked,
      isAnimateTimeChanges:()=>false,getTimeTransition:()=>c.transition.value,
    };
    return {settings,image:this.pendingImage};
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
        { value: MODE_THEME, label: prefs.isClockUseEnglish() ? 'Theme' : '主题' },
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

    const font = fontPicker(prefs.getFontFamily(), prefs.isClockUseEnglish());
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
      fontPicker: font.row,
      fontFamily: font.input,
      fontWeight: fontWeightControl(prefs.isClockUseEnglish()?'Font weight':'字重',prefs.getFontFamily(),prefs.getFontWeight()),
      boldText: switchRow(t('bold_text'), prefs.isBoldText()),
      timeSize: sliderRow(
        t('font_size'),
        MIN_FONT_PERCENT,
        MAX_FONT_PERCENT,
        Math.round(prefs.getTimeFontScale() * 100),
        percent
      ),
      dateSize: secondaryFontSize(prefs.isClockUseEnglish()?'Date size':'日期字号','date',this.activeTheme),
      transition: select(
        [TRANSITION_FADE, TRANSITION_SLIDE_UP, TRANSITION_SLIDE_DOWN, TRANSITION_SCALE, TRANSITION_FLIP, TRANSITION_SLIDE_RIGHT, TRANSITION_SCAN].map(
          value => ({ value, label: transitionName(value, prefs.getClockLanguage()) })
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
        this.pendingImage = file;
        this.backgroundMode = MODE_IMAGE;
        c.modeGroup.setValue(MODE_IMAGE);
        chooseImage.textContent = file.name;
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
      colorControls.hidden = mode !== MODE_COLOR;
      imageControls.hidden = mode !== MODE_IMAGE;
    });
    colorControls.hidden = this.backgroundMode !== MODE_COLOR;
    imageControls.hidden = this.backgroundMode !== MODE_IMAGE;

    this.styleBoard.append(
      this.themes.root,
      card(t('background_settings_group'), c.modeGroup.row, colorControls, imageControls),
      card(
        t('font_settings_group'),
        subLabel(t('font_family')),
        c.fontPicker,
        c.fontWeight.row,
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
        this.themes.supportRow,
        summaryLabel(prefs.isClockUseEnglish()?'Date and supporting text share one size range. When space is tight, both scale together.':'日期与辅助文字使用相同字号范围；空间不足时同步缩小。'),
        subLabel(t('font_color')),
        c.datePicker.root,
        c.showLunar.row,
        c.dualLine.row
      )
    );
    const syncThemeInk = () => {
      const enabled = !this.themes.usesAutomaticInk();
      setTreeEnabled(c.timePicker.root, enabled);
      setTreeEnabled(c.datePicker.root, enabled);
    };
    this.themes.root.addEventListener('change', syncThemeInk);
    this.themes.onThemeChanged(id => this.selectTheme(id));
    this.selectTheme(this.activeTheme);
    c.fontFamily.addEventListener('change',()=>c.fontWeight.set(c.fontFamily.value,c.fontWeight.value()));
    c.animate.input.addEventListener('change', () => { c.transition.disabled = !c.animate.input.checked || this.activeTheme === 'digital.grid'; });
    syncThemeInk();
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

    const originalDateSection=this.dateSectionEnglish;this.dateSectionEnglish=!originalDateSection;this.rebuildDateFormat();this.dateSectionEnglish=originalDateSection;
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
      c.iconFill.input.disabled = !enabled; c.iconDynamic.input.disabled = !enabled;
      if (manual) void this.ensureCatalog();
    };
    c.weatherEnabled.input.addEventListener('change', syncWeatherState);
    c.locationMode.addEventListener('change', syncWeatherState);
    syncWeatherState();

    // ---- Footer ----
    const reset = element('button', 'm3-button m3-button--outlined settings-reset', t('reset_default'));
    reset.type = 'button';
    reset.addEventListener('click', () => this.restoreDefaults());


    // Ultimate's category home on phones, persistent navigation on wide screens.
    const en=prefs.isClockUseEnglish();
    const names=en?['Clock style','Calendar style','Background & display','Time & date','Weather & message','Calendar','Chime','System']:['时钟样式','日历样式','背景与显示','时间与日期','天气与消息','日历','报时','系统'];
    const descriptions=en?['Themes, colours and typography','Calendar composition and typography','Wallpaper and device status','Time source, time zone and date','Location, forecast and custom text','Week layout','Hourly and half-hour effects','Language and display protection']:['主题、配色与排版','主题、字体与滚动','壁纸、亮度与设备状态','时区、时间源与日期格式','天气、位置与自定义消息','每周起始日与周末','整点、半点与静默时段','语言、方向与显示保护'];
    const icons=['◷','▦','◐','◴','☁','▤','◉','⚙'];
    const panes=names.map((name,i)=>{const pane=element('section','ultimate-settings-pane');pane.id='settings-category-'+i;pane.dataset.category=String(i);pane.setAttribute('aria-label',name);pane.append(element('h2','settings-page-title',name));return pane;});
    const styles=[...this.styleBoard.children];const functions=[...this.functionBoard.children];
    panes[0].append(styles[0],styles[2],this.ultimate.seconds,this.ultimate.world);
    panes[1].append(this.ultimate.calendar);
    panes[2].append(styles[1],this.ultimate.display);
    panes[3].append(functions[1],functions[2],functions[4],card(en?'Date display':'日期显示',c.use24Hour.row,c.showSeconds.row,c.showLunar.row));
    panes[4].append(functions[5],functions[6],this.themes.temperature,functions[7]);
    panes[5].append(functions[8]);
    const quiet=quietRow(c);const syncQuiet=()=>setTreeEnabled(quiet,c.hourlyQuiet.input.checked);c.hourlyQuiet.input.addEventListener('change',syncQuiet);syncQuiet();
    panes[6].append(card(en?'Visual chime':'视觉报时',c.hourlyChime.row,c.hourlyQuiet.row,quiet),this.ultimate.chime);
    panes[7].append(functions[0],functions[3],this.ultimate.system,reset);
    const navigation=element('nav','ultimate-settings-nav');navigation.setAttribute('aria-label',en?'Settings categories':'设置分类');
    const back=element('button','m3-button m3-button--text settings-back',en?'← Back':'← 返回');back.type='button';
    const workspace=element('div','ultimate-settings-workspace');
    const content=element('div','ultimate-settings-content');content.append(...panes);
    const editor=element('div','ultimate-settings-editor');
    editor.append(this.previewHost,content);
    const buttons: HTMLButtonElement[]=[];
    const show=(index:number,focus=false)=>{this.previewCategory=index;this.preview?.setCategory(index);panes.forEach((pane,i)=>{pane.hidden=i!==index;});buttons.forEach((button,i)=>button.setAttribute('aria-current',i===index?'page':'false'));this.dialog.classList.add('is-detail');content.scrollTop=0;if(focus)back.focus();};
    names.forEach((name,i)=>{const button=element('button','settings-category');button.type='button';button.setAttribute('aria-controls',panes[i].id);button.append(element('span','settings-category-icon',icons[i]),element('span','settings-category-copy'));button.lastElementChild!.append(element('strong',undefined,name),element('small',undefined,descriptions[i]));button.append(element('span',undefined,'›'));button.addEventListener('click',()=>show(i,true));navigation.append(button);buttons.push(button);});
    back.addEventListener('click',()=>{this.dialog.classList.remove('is-detail');buttons.find(button=>button.getAttribute('aria-current')==='page')?.focus();});
    header.prepend(back);header.querySelector('h2')!.textContent=en?'Settings':'设置';
    workspace.append(navigation,editor);this.dialog.classList.add('ultimate-settings');this.dialog.setAttribute('aria-label',en?'Settings':'设置');
    this.dialog.replaceChildren(header,workspace);show(0);this.dialog.classList.remove('is-detail');
    c.timeSize.input.setAttribute('aria-label',en?'Time size':'时间字号');c.dateSize.input.setAttribute('aria-label',en?'Date size':'日期字号');
    c.fontFamily.setAttribute('aria-label',en?'Clock font':'时钟字体');c.transition.setAttribute('aria-label',en?'Digit transition':'数字过渡动画');
    for(const pane of panes) for(const input of pane.querySelectorAll('select,input:not([type=checkbox]):not([type=range]):not([type=file])')) if(!input.hasAttribute('aria-label')) input.setAttribute('aria-label', input.previousElementSibling?.textContent || input.getAttribute('placeholder') || pane.getAttribute('aria-label')!);
    const syncChime = () => this.ultimate.setHourlyChime(c.hourlyChime.input.checked);
    c.hourlyChime.input.addEventListener('change', syncChime);
    syncChime();
    this.dialog.addEventListener('reset-settings',()=>{syncChime();syncQuiet();syncWeatherState();syncDimState();syncSmallSeconds();syncNetworkState();syncThemeInk();});
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
    const storedCore = this.resetDateDefaults ? (english ? 'yyyy/M/d' : 'yyyy年M月d日') : prefs.getDateCore(english);
    const storedCombo = this.resetDateDefaults ? 'DATE EEEE' : prefs.getDateCombo(english);
    const customEnabled = !this.resetDateDefaults && prefs.isDateCustomEnabled(english);
    const coreIndex = this.initializedDates.has(english) ? (english ? this.coreIndexEn : this.coreIndexCn) : customEnabled
      ? cores.length
      : Math.max(0, cores.indexOf(storedCore) >= 0 ? cores.indexOf(storedCore) : defaultCoreIndex(cores, english));
    const comboIndex = this.initializedDates.has(english) ? (english ? this.comboIndexEn : this.comboIndexCn) :
      combos.indexOf(storedCombo) >= 0 ? combos.indexOf(storedCombo) : combos.indexOf('DATE EEEE');
    this.initializedDates.add(english);
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
    const fonts = pendingFontSelections(this.dialog);
    if(fonts.length) { await Promise.all(fonts); if(!this.dialog.open) return; }
    const c = this.controls;
    const manual = c.locationMode.value === WEATHER_LOCATION_MANUAL;
    if (c.weatherEnabled.input.checked && manual && !this.weatherLocation.id) {
      toast(t('weather_location_not_selected'));
      return;
    }
    if (this.backgroundMode === MODE_IMAGE && !this.pendingImage && !(await hasBackgroundImage())) {
      toast(t('select_image_first'));
      return;
    }

    if (this.pendingImage && this.backgroundMode === MODE_IMAGE) {
      try { await saveBackgroundImage(this.pendingImage, Math.round(Math.max(screen.width,screen.height)*(devicePixelRatio||1))); }
      catch { toast(t('image_error')); return; }
    }
    this.captureTypography();
    this.themes.apply();
    for(const[id,v]of this.typography){prefs.setFontFamily(v.font,id);prefs.setBoldText(v.weight>=700,id);prefs.setFontWeight(v.weight,id);prefs.setTimeFontScale(v.time,id);saveSecondarySize('date',id,v.date);prefs.setTimeTransition(v.transition,id);prefs.setAnimateTimeChanges(v.animate,id);}
    this.ultimate.apply();
    prefs.setBackgroundMode(this.backgroundMode);
    prefs.setBackgroundColor(this.backgroundColor);
    prefs.setDimBackground(c.dimBackground.input.checked);
    prefs.setScheduleDimBackground(c.scheduleDim.input.checked);
    prefs.setDimStartMinutes(c.dimStart.minutes());
    prefs.setDimEndMinutes(c.dimEnd.minutes());





    prefs.setTimeColor(this.timeColor);
    prefs.setDateColor(this.dateColor);

    prefs.setBlinkColon(c.blinkColon.input.checked);

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
    this.resetThemes=true;this.typography.clear();this.activeTheme='classic';this.pendingImage=null;this.ultimate.reset();
    this.themes.reset();
    this.themes.root.dispatchEvent(new Event('change', { bubbles: true }));
    const c = this.controls;
    this.backgroundMode = MODE_THEME;
    this.backgroundColor = DEFAULT_BACKGROUND_COLOR;
    this.timeColor = DEFAULT_TEXT_COLOR;
    this.dateColor = DEFAULT_TEXT_COLOR;
    c.modeGroup.setValue(MODE_THEME);
    c.modeGroup.row.querySelector<HTMLButtonElement>('[aria-pressed="true"]')?.click();
    c.backgroundPicker.setValue(DEFAULT_BACKGROUND_COLOR);
    c.timePicker.setValue(DEFAULT_TEXT_COLOR);
    c.datePicker.setValue(DEFAULT_TEXT_COLOR);
    c.dimBackground.input.checked = DEFAULT_DIM_BACKGROUND;
    c.scheduleDim.input.checked = DEFAULT_SCHEDULE_DIM_BACKGROUND;
    setTimeValue(c.dimStart, DEFAULT_DIM_START_MINUTES);
    setTimeValue(c.dimEnd, DEFAULT_DIM_END_MINUTES);
    setFontSelection(c.fontFamily, 'system');
    c.boldText.input.checked = DEFAULT_BOLD_TEXT;c.fontWeight.set('system',400);
    setSlider(c.timeSize, DEFAULT_TIME_FONT_SCALE);
    c.dateSize.setTheme('classic',DEFAULT_DATE_FONT_SCALE*100);
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
    this.resetDateDefaults=true;this.initializedDates.clear();
    this.pendingPatternCn = 'yyyy年M月d日 EEEE';
    this.pendingPatternEn = 'yyyy/M/d EEEE';
    this.dateSectionEnglish=true;this.rebuildDateFormat();this.dateSectionEnglish=false;
    this.rebuildDateFormat();
    c.networkTime.input.checked = false; c.syncInterval.setValue(60);c.timeSourceUrl.value='';c.region.value=ZONE_IDS[0];
    c.customMessage.value = '';
    c.weatherEnabled.input.checked = DEFAULT_WEATHER_ENABLED;
    c.locationMode.value = WEATHER_LOCATION_AUTOMATIC;
    c.weatherInterval.value = String(DEFAULT_WEATHER_INTERVAL_MINUTES);
    c.weatherDetailed.input.checked = DEFAULT_WEATHER_DETAILED;
    c.iconFill.input.checked = DEFAULT_WEATHER_ICON_FILL;
    c.iconDynamic.input.checked = DEFAULT_WEATHER_ICON_DYNAMIC_COLOR;
    c.weekStart.setValue(DEFAULT_CALENDAR_WEEK_START);
    c.highlightWeekends.input.checked = DEFAULT_CALENDAR_HIGHLIGHT_WEEKENDS;
    this.dialog.dispatchEvent(new Event('reset-settings'));
    this.selectTheme('classic');
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
