/**
 * Renders dates from a compact, injection-safe pattern language.
 *
 * A pattern is a sequence of field tokens (runs of a single reserved letter) and
 * literal text. Recognized tokens, interpreted per `Lang`:
 *
 *   yyyy / yy    2026 / 26 (Arabic year)
 *   YYY / YYYY   二〇二六 / 二零二六 (Chinese-numeral year)
 *   M / MM       8 / 08 (Arabic month)
 *   MMM / MMMM   Aug / August (English) · 八 (Chinese)
 *   d / dd       7 / 07 (Arabic day)
 *   DD           七 (Chinese-numeral day)
 *   E / EEEE     Fri / Friday (English) · 周五 / 星期五 (Chinese) · 週五 (Traditional short)
 *
 * Any other character — separators, CJK, punctuation, emoji (surrogate pairs) —
 * is copied through verbatim as a literal. Text wrapped in single quotes is
 * literal too (`''` yields a literal apostrophe), giving custom patterns an
 * escape hatch for reserved letters. The pattern is never handed to a formatter
 * that interprets `%`-style specifiers, so there is no format-injection surface.
 * The rendered result is passed through `pangu` so the half-width spacing
 * convention applies uniformly (e.g. `2026 年 8 月 7 日`).
 *
 * Ported from com.clockmods.ui.DateFormatter.
 */
import { pangu } from './text-spacing';
import type { ZonedFields } from '../core/zoned-time';

/**
 * Rendering language. `chinese` and `traditional` share identical numerals,
 * month and full-weekday forms; they differ only in the short weekday glyph
 * (周五 vs 週五).
 */
export type Lang = 'chinese' | 'traditional' | 'english';

export const LANGS: readonly Lang[] = ['chinese', 'traditional', 'english'];

/** Upper bound on a user-supplied pattern, guarding against pathological input. */
export const MAX_PATTERN_LENGTH = 200;

const CN_DIGITS = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
const CN_UNITS = ['', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
const CN_WEEK_FULL = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
const CN_WEEK_SHORT = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
const CN_WEEK_SHORT_TW = ['週日', '週一', '週二', '週三', '週四', '週五', '週六'];
const EN_WEEK_FULL = [
  'Sunday',
  'Monday',
  'Tuesday',
  'Wednesday',
  'Thursday',
  'Friday',
  'Saturday',
];
const EN_WEEK_SHORT = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const EN_MONTH_FULL = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];
const EN_MONTH_SHORT = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];

// ---- Default rendering patterns ----
/** Chinese default: renders "2026 年 8 月 7 日 星期五". */
export const DEFAULT_PATTERN_CN = 'yyyy年M月d日 EEEE';
/** English default: renders "2026/8/7 Friday". */
export const DEFAULT_PATTERN_EN = 'yyyy/M/d EEEE';

// ---- Chinese date cores ----
const CN_DATE_CORES = [
  'yyyy年MM月dd日',
  'yyyy年M月d日',
  'yy年M月d日',
  'yy年MM月dd日',
  'yyyy年M月d号',
  'yyyy年MM月dd号',
  'MM月dd日',
  'M月d日',
  'yyyy/MM/dd',
  'yyyy/M/d',
  'yy/M/d',
  'yyyy-MM-dd',
  'yyyy-M-d',
  'yyyy.MM.dd',
  'yyyy.M.d',
  'MM/dd',
  'M/d',
  'dd/MM',
  'd/M',
  'MM-dd',
  'dd-MM',
  'YYY年MMM月DD日',
  'YYYY年MMM月DD日',
  'YYY年MMM月DD号',
  'YYYY年MMM月DD号',
  'MMM月DD日',
  'MMM月DD号',
];

// ---- English date cores ----
const EN_DATE_CORES = [
  'MM/dd/yyyy',
  'M/d/yyyy',
  'MM/dd/yy',
  'M/d/yy',
  'dd/MM/yyyy',
  'd/M/yyyy',
  'dd/MM/yy',
  'd/M/yy',
  'yyyy/MM/dd',
  'yyyy/M/d',
  'yy/MM/dd',
  'yyyy-MM-dd',
  'yyyy-M-d',
  'MM-dd-yyyy',
  'M-d-yyyy',
  'dd-MM-yyyy',
  'd-M-yyyy',
  'yyyy.MM.dd',
  'yyyy.M.d',
  'dd.MM.yyyy',
  'MMMM d, yyyy',
  'MMMM dd, yyyy',
  'MMM d, yyyy',
  'MMM dd, yyyy',
  'd MMMM yyyy',
  'dd MMMM yyyy',
  'd MMM yyyy',
  'dd MMM yyyy',
  'MM/dd',
  'M/d',
  'dd/MM',
  'd/M',
  'MM-dd',
  'M-d',
  'dd-MM',
  'd-M',
  'MM.dd',
  'M.d',
  'dd.MM',
  'd.M',
  'MMMM d',
  'MMM d',
  'MMMM dd',
  'MMM dd',
  'd MMMM',
  'd MMM',
  'dd MMMM',
  'dd MMM',
  'MMMM-d',
  'MMM-d',
  'd-MMMM',
  'd-MMM',
];

// ---- Weekday combos. "DATE" is the literal date-core placeholder. ----
// Chinese uses full-width comma and parentheses; the first entry omits the weekday.
const CN_WEEKDAY_COMBOS = [
  'DATE',
  'DATE EEEE',
  'DATE E',
  'DATE，EEEE',
  'DATE，E',
  'DATE（EEEE）',
  'DATE（E）',
  '（EEEE）DATE',
  '（E）DATE',
  'DATE [EEEE]',
  'DATE [E]',
  '[EEEE] DATE',
  '[E] DATE',
  'DATE - EEEE',
  'DATE - E',
  'DATE · EEEE',
  'DATE · E',
  'EEEE DATE',
  'E DATE',
  'EEEE，DATE',
  'E，DATE',
  'DATE | EEEE',
  'DATE | E',
  'EEEE | DATE',
  'E | DATE',
];
const EN_WEEKDAY_COMBOS = [
  'DATE',
  'DATE EEEE',
  'DATE E',
  'DATE, EEEE',
  'DATE, E',
  'DATE (EEEE)',
  'DATE (E)',
  '(EEEE) DATE',
  '(E) DATE',
  'DATE [EEEE]',
  'DATE [E]',
  '[EEEE] DATE',
  '[E] DATE',
  'DATE - EEEE',
  'DATE - E',
  'DATE · EEEE',
  'DATE · E',
  'EEEE DATE',
  'E DATE',
  'EEEE, DATE',
  'E, DATE',
  'DATE | EEEE',
  'DATE | E',
  'EEEE | DATE',
  'E | DATE',
];

// ---- Fixed, non-customizable format lists (kept for parity and tests) ----
const CN_FIXED_FORMATS = [
  'yyyy/MM/dd EEEE',
  'yyyy/M/d EEEE',
  'yyyy年MM月dd日 EEEE',
  'yyyy年M月d日 EEEE',
  'yyyy.MM.dd EEEE',
  'yyyy.M.d EEEE',
  'yyyy.MM.dd E',
  'yyyy.M.d E',
  'yyyy年MM月dd日 E',
  'yyyy年M月d日 E',
  'MM/dd EEEE',
  'M/d EEEE',
  'MM月dd日 EEEE',
  'M月d日 EEEE',
  'MM.dd EEEE',
  'M.d EEEE',
  'MM.dd E',
  'M.d E',
  'MM月dd日 E',
  'M月d日 E',
];
const EN_FIXED_FORMATS = [
  'yyyy/MM/dd EEEE',
  'yyyy/M/d EEEE',
  'yyyy-MM-dd EEEE',
  'yyyy-M-d EEEE',
  'yyyy.MM.dd EEEE',
  'yyyy.M.d EEEE',
  'EEEE, MMMM d, yyyy',
  'MMM d, E',
  'MMM d, yyyy',
  'MM/dd EEEE',
  'M/d EEEE',
  'MM-dd EEEE',
  'M-d EEEE',
  'MM.dd EEEE',
  'M.d EEEE',
  'EEEE, MMMM d',
  'MMM d',
];

/**
 * Renders `pattern` for `fields` under `lang`. Never throws; unrecognized input
 * degrades to literal text.
 */
export function format(pattern: string, fields: ZonedFields, lang: Lang): string {
  const { text } = scan(pattern, fields, lang);
  return pangu(text);
}

/**
 * @returns whether `pattern` is safe to apply: non-empty, within
 *   MAX_PATTERN_LENGTH, and containing at least one real date/weekday field
 *   token (so it renders something meaningful rather than pure literal text).
 */
export function isValidPattern(pattern: string | null | undefined): boolean {
  if (!pattern || pattern.length > MAX_PATTERN_LENGTH) return false;
  return scan(pattern, exampleDate(), 'chinese').tokenCount > 0;
}

/** The fixed sample date used by every settings preview: 2026-08-07 (a Friday). */
export function exampleDate(): ZonedFields {
  return { year: 2026, month0: 7, day: 7, hour: 12, minute: 0, second: 0, dayOfWeek0: 5 };
}

/** Renders `pattern` with the fixed `exampleDate()` (for preview labels). */
export function preview(pattern: string, lang: Lang): string {
  return format(pattern, exampleDate(), lang);
}

// ---- Accessors for the settings UI ----

export function dateCores(lang: Lang): string[] {
  return [...(lang === 'english' ? EN_DATE_CORES : CN_DATE_CORES)];
}

export function weekdayCombos(lang: Lang): string[] {
  return [...(lang === 'english' ? EN_WEEKDAY_COMBOS : CN_WEEKDAY_COMBOS)];
}

export function fixedFormats(lang: Lang): string[] {
  return [...(lang === 'english' ? EN_FIXED_FORMATS : CN_FIXED_FORMATS)];
}

/** Substitutes a chosen date core into a weekday-combo template. */
export function composeCombo(comboTemplate: string, core: string): string {
  return comboTemplate.replace('DATE', core);
}

/** A human-facing label for a combo template, e.g. "… 星期五" / "(Friday) …". */
export function comboLabel(comboTemplate: string, lang: Lang): string {
  return preview(composeCombo(comboTemplate, '…'), lang);
}

// ---- Core scanner ----

const TOKEN_LETTERS = new Set(['y', 'Y', 'M', 'd', 'D', 'E']);

/** Renders the pattern and reports how many field tokens it contained. */
function scan(pattern: string, fields: ZonedFields, lang: Lang): { text: string; tokenCount: number } {
  const year = fields.year;
  const month = fields.month0 + 1;
  const day = fields.day;
  const dayOfWeek = fields.dayOfWeek0 >= 0 && fields.dayOfWeek0 <= 6 ? fields.dayOfWeek0 : 0;

  let out = '';
  let tokenCount = 0;
  const length = pattern.length;
  let i = 0;
  while (i < length) {
    const c = pattern.charAt(i);
    if (c === "'") {
      // Quoted literal: copy verbatim until the closing quote; "''" is a literal quote.
      i++;
      if (i < length && pattern.charAt(i) === "'") {
        out += "'";
        i++;
        continue;
      }
      while (i < length && pattern.charAt(i) !== "'") {
        const codePoint = pattern.codePointAt(i)!;
        out += String.fromCodePoint(codePoint);
        i += codePoint > 0xffff ? 2 : 1;
      }
      if (i < length) i++; // consume closing quote
      continue;
    }
    if (TOKEN_LETTERS.has(c)) {
      let run = 1;
      while (i + run < length && pattern.charAt(i + run) === c) run++;
      out += renderToken(c, run, year, month, day, dayOfWeek, lang);
      tokenCount++;
      i += run;
      continue;
    }
    const codePoint = pattern.codePointAt(i)!;
    out += String.fromCodePoint(codePoint);
    i += codePoint > 0xffff ? 2 : 1;
  }
  return { text: out, tokenCount };
}

function renderToken(
  letter: string,
  run: number,
  year: number,
  month: number,
  day: number,
  dayOfWeek: number,
  lang: Lang
): string {
  switch (letter) {
    case 'y':
      return run === 2 ? pad2(mod(year, 100)) : String(year);
    case 'Y':
      // Chinese-numeral year token; degrades to an Arabic year in English so a
      // mistyped token never injects Chinese numerals into an English date.
      return lang === 'english' ? String(year) : chineseYear(year, run === 3 ? '〇' : '零');
    case 'M':
      if (lang === 'english') {
        if (run === 1) return String(month);
        if (run === 2) return pad2(month);
        if (run === 3) return EN_MONTH_SHORT[month - 1];
        return EN_MONTH_FULL[month - 1];
      }
      if (run === 1) return String(month);
      if (run === 2) return pad2(month);
      return chineseCardinal(month);
    case 'd':
      return run === 1 ? String(day) : pad2(day);
    case 'D':
      // Chinese-numeral day token; degrades to Arabic in English for the same reason.
      return lang === 'english' ? String(day) : chineseCardinal(day);
    case 'E':
      if (lang === 'english') return run >= 4 ? EN_WEEK_FULL[dayOfWeek] : EN_WEEK_SHORT[dayOfWeek];
      if (lang === 'traditional') {
        return run >= 4 ? CN_WEEK_FULL[dayOfWeek] : CN_WEEK_SHORT_TW[dayOfWeek];
      }
      return run >= 4 ? CN_WEEK_FULL[dayOfWeek] : CN_WEEK_SHORT[dayOfWeek];
    default:
      return '';
  }
}

/** Converts a year to per-digit Chinese numerals, using `zero` for the digit 0. */
export function chineseYear(year: number, zero: string): string {
  const digits = String(Math.abs(year));
  let out = '';
  for (const digit of digits) out += digit === '0' ? zero : CN_DIGITS[Number(digit)];
  return out;
}

/** Converts a cardinal 1..99 to Chinese numerals (7→七, 21→二十一, 30→三十). */
export function chineseCardinal(n: number): string {
  if (n <= 0 || n > 99) return String(n);
  if (n < 10) return CN_UNITS[n];
  if (n === 10) return '十';
  if (n < 20) return '十' + CN_UNITS[n - 10];
  const tens = Math.floor(n / 10);
  const ones = n % 10;
  return CN_UNITS[tens] + '十' + (ones === 0 ? '' : CN_UNITS[ones]);
}

const pad2 = (value: number): string => String(value).padStart(2, '0');
/** Floor-mod, matching Math.floorMod for the negative-year edge case. */
const mod = (value: number, m: number): number => ((value % m) + m) % m;
