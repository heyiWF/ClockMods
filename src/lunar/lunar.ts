/**
 * Lunar date, solar terms, festivals and 宜/忌, computed offline by
 * lunar-javascript (6tail, MIT) — the JavaScript sibling of the cn.6tail:lunar
 * 1.7.7 artifact the Android build used, so the outputs match method for method.
 *
 * This one module covers what Android split across two classes:
 *   - com.clockmods.calendar.LunarCalendar — the clock's lunar line. Android used
 *     a hand-rolled 1900–2050 bit table there; the engine is astronomical, so the
 *     same strings now render correctly well beyond 2100. The month names it
 *     returns (正 二 … 十 冬 腊, prefixed 闰) are identical to that table's, so the
 *     rendered text is unchanged for every date the old code could handle.
 *   - com.clockmods.pro.LunarAlmanac — the calendar page's per-day almanac.
 */
import { Solar } from 'lunar-javascript';
import type { Lunar } from 'lunar-javascript';

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
 * for 42 of them (plus the adjacent month while swiping). Cache by date and
 * festival mode; a month change touches at most a few hundred entries.
 */
const cache = new Map<string, Almanac>();
const MAX_CACHE_ENTRIES = 600;

/** @param month0 zero-based month, matching java.util.Calendar.MONTH. */
export function almanacOf(year: number, month0: number, day: number, includeMinor = false): Almanac {
  const key = `${year}-${month0}-${day}-${includeMinor ? 1 : 0}`;
  const cached = cache.get(key);
  if (cached) return cached;
  const solar = Solar.fromYmd(year, month0 + 1, day);
  const lunar = solar.getLunar();
  const monthInChinese = lunar.getMonthInChinese();
  const dayInChinese = lunar.getDayInChinese();
  const ganZhi = lunar.getYearInGanZhi();
  const shengXiao = lunar.getYearShengXiao();
  const almanac: Almanac = {
    bracketed: `${ganZhi}[${shengXiao}]年${monthInChinese}月${dayInChinese}`,
    natural: `${ganZhi}${shengXiao}年${monthInChinese}月${dayInChinese}`,
    shortLabel: lunar.getDay() === 1 ? `${monthInChinese}月` : dayInChinese,
    festivals: collectFestivals(solar, lunar, includeMinor),
    suitable: lunar.getDayYi(),
    avoid: lunar.getDayJi(),
  };
  if (cache.size >= MAX_CACHE_ENTRIES) cache.clear();
  cache.set(key, almanac);
  return almanac;
}

/** The clock's lunar line — LunarCalendar.format. */
export function lunarClockLine(year: number, month0: number, day: number): string {
  return almanacOf(year, month0, day).bracketed;
}

/**
 * Solar term, traditional/solar festivals and the first day of 数九/三伏, in
 * display order and de-duplicated. 母亲节/父亲节/感恩节 are always included
 * because the engine returns them from getFestivals().
 *
 * @param includeMinor when true, also include getOtherFestivals() (国际电影节,
 *   世界人道主义日, 龙头节, …); when false they are omitted to keep the grid
 *   uncluttered. Controlled by the "月历显示更多节日" setting.
 *
 * Ported from LunarAlmanac.festivals.
 */
function collectFestivals(
  solar: ReturnType<typeof Solar.fromYmd>,
  lunar: Lunar,
  includeMinor: boolean
): string[] {
  const labels = new Set<string>();
  const jieQi = lunar.getJieQi();
  if (jieQi) labels.add(jieQi);
  for (const festival of lunar.getFestivals()) labels.add(festival);
  for (const festival of solar.getFestivals()) labels.add(festival);
  if (includeMinor) {
    for (const festival of lunar.getOtherFestivals()) labels.add(festival);
    for (const festival of solar.getOtherFestivals()) labels.add(festival);
  }
  const shuJiu = lunar.getShuJiu();
  if (shuJiu && shuJiu.getIndex() === 1) labels.add(shuJiu.getName());
  const fu = lunar.getFu();
  if (fu && fu.getIndex() === 1) labels.add(fu.getName());
  return [...labels];
}
