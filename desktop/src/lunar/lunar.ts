/**
 * Lunar date, solar terms, festivals and 宜/忌, computed offline by tyme4ts
 * (6tail, MIT) — the TypeScript sibling of the cn.6tail:tyme4j 1.5.1 artifact the
 * Android build uses, so the outputs match method for method. The engine is
 * astronomical rather than table bound, so it stays accurate for years 1–9999.
 *
 * This one module covers what Android split across two classes:
 *   - com.clockmods.calendar.LunarCalendar — the clock's lunar line. The Chinese
 *     rendering is this app's own wording (正…冬腊月, 干支[生肖]年, 初/廿 day names)
 *     rather than the engine's, so the on-screen text is unchanged from the
 *     releases that computed it from a 1900–2050 bit table.
 *   - com.clockmods.pro.LunarAlmanac — the calendar page's per-day almanac.
 */
import { SolarDay } from 'tyme4ts';

const TIAN_GAN = ['甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸'];
const DI_ZHI = ['子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥'];
const ZODIAC = ['鼠', '牛', '虎', '兔', '龙', '蛇', '马', '羊', '猴', '鸡', '狗', '猪'];
const MONTH_NAMES = ['正', '二', '三', '四', '五', '六', '七', '八', '九', '十', '冬', '腊'];
const DAY_PREFIX = ['初', '十', '廿', '三'];
const DAY_NUMBERS = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];

export interface Almanac {
  /** e.g. 丙午[马]年六月廿五 — the main clock's lunar line. */
  bracketed: string;
  /** e.g. 丙午马年六月廿五 — the calendar footer's natural label. */
  natural: string;
  /** Cell label: the lunar month name on a month's first day, otherwise the day. */
  shortLabel: string;
  /** Solar term, festivals and the first day of 数九/三伏, in display order. */
  festivals: string[];
  /** 今日宜 */
  suitable: string[];
  /** 今日忌 */
  avoid: string[];
}

/**
 * Building an almanac walks the astronomical tables, and a rendered month asks
 * for 42 of them (plus the adjacent month while swiping). Cache by date; a month
 * change touches at most a few hundred entries.
 */
const cache = new Map<string, Almanac>();
const MAX_CACHE_ENTRIES = 600;

/** @param month0 zero-based month, matching java.util.Calendar.MONTH. */
export function almanacOf(year: number, month0: number, day: number): Almanac {
  const key = `${year}-${month0}-${day}`;
  const cached = cache.get(key);
  if (cached) return cached;
  const solarDay = SolarDay.fromYmd(year, month0 + 1, day);
  const lunarDay = solarDay.getLunarDay();
  const lunarMonth = lunarDay.getLunarMonth();
  const monthLabel = formatMonth(Math.abs(lunarMonth.getMonthWithLeap()), lunarMonth.isLeap());
  const dayLabel = formatDay(lunarDay.getDay());
  const lunarYear = lunarMonth.getLunarYear().getYear();
  const almanac: Almanac = {
    bracketed: stemZodiacYear(lunarYear, true) + monthLabel + dayLabel,
    natural: stemZodiacYear(lunarYear, false) + monthLabel + dayLabel,
    shortLabel: lunarDay.getDay() === 1 ? monthLabel : dayLabel,
    festivals: collectFestivals(solarDay),
    suitable: lunarDay.getRecommends().map((taboo) => taboo.getName()),
    avoid: lunarDay.getAvoids().map((taboo) => taboo.getName()),
  };
  if (cache.size >= MAX_CACHE_ENTRIES) cache.clear();
  cache.set(key, almanac);
  return almanac;
}

/** The clock's lunar line — LunarCalendar.format. */
export function lunarClockLine(year: number, month0: number, day: number): string {
  return almanacOf(year, month0, day).bracketed;
}

/** 干支[生肖]年 (bracketed) or 干支生肖年 (plain), e.g. 丙午[马]年 / 丙午马年. */
export function stemZodiacYear(lunarYear: number, bracketZodiac: boolean): string {
  const index = ((((lunarYear - 4) % 60) + 60) % 60);
  const stemBranch = TIAN_GAN[index % 10] + DI_ZHI[index % 12];
  const zodiac = ZODIAC[index % 12];
  return bracketZodiac ? `${stemBranch}[${zodiac}]年` : `${stemBranch}${zodiac}年`;
}

/** 正月 … 冬月 / 腊月, prefixed 闰 for a leap month. */
export function formatMonth(month: number, leap: boolean): string {
  return `${leap ? '闰' : ''}${MONTH_NAMES[month - 1]}月`;
}

/** 初一 / 十二 / 廿五 / 三十 … */
export function formatDay(day: number): string {
  if (day === 10) return '初十';
  if (day === 20) return '二十';
  if (day === 30) return '三十';
  return DAY_PREFIX[Math.floor((day - 1) / 10)] + DAY_NUMBERS[(day - 1) % 10];
}

/**
 * Solar term (only on its 交节 day), traditional lunar/solar festivals and the
 * first day of each 数九/三伏, in display order and de-duplicated. tyme4ts returns
 * a single curated festival per calendar, so — as on Android — there is no
 * separate "minor festival" tier to switch on.
 *
 * Ported from LunarAlmanac.festivals.
 */
function collectFestivals(solarDay: SolarDay): string[] {
  const labels = new Set<string>();
  const termDay = solarDay.getTermDay();
  if (termDay && termDay.getDayIndex() === 0) labels.add(termDay.getSolarTerm().getName());
  const lunarFestival = solarDay.getLunarDay().getFestival();
  if (lunarFestival) labels.add(lunarFestival.getName());
  const solarFestival = solarDay.getFestival();
  if (solarFestival) labels.add(solarFestival.getName());
  const nineDay = solarDay.getNineDay();
  if (nineDay && nineDay.getDayIndex() === 0) labels.add(nineDay.getNine().getName());
  const dogDay = solarDay.getDogDay();
  if (dogDay && dogDay.getDayIndex() === 0) labels.add(dogDay.getDog().getName());
  return [...labels];
}
