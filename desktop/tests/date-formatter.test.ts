/** Ported from app/src/test/java/com/clockmods/ui/DateFormatterTest.java. */
import { describe, expect, it } from 'vitest';
import {
  chineseCardinal,
  chineseYear,
  comboLabel,
  composeCombo,
  dateCores,
  DEFAULT_PATTERN_CN,
  DEFAULT_PATTERN_EN,
  exampleDate,
  fixedFormats,
  format,
  isValidPattern,
  LANGS,
  MAX_PATTERN_LENGTH,
  preview,
  weekdayCombos,
} from '../src/format/date-formatter';

const example = exampleDate();

describe('DateFormatter', () => {
  it('uses 2026-08-07, a Friday, as the sample date', () => {
    expect(example.year).toBe(2026);
    expect(example.month0).toBe(7);
    expect(example.day).toBe(7);
    expect(example.dayOfWeek0).toBe(5); // Friday
  });

  it('renders Chinese Arabic dates with pangu spacing', () => {
    expect(format('yyyy年M月d日', example, 'chinese')).toBe('2026 年 8 月 7 日');
    expect(format(DEFAULT_PATTERN_CN, example, 'chinese')).toBe('2026 年 8 月 7 日 星期五');
    expect(format('yyyy年MM月dd日', example, 'chinese')).toBe('2026 年 08 月 07 日');
  });

  it('renders Chinese-numeral dates', () => {
    expect(format('YYY年MMM月DD日', example, 'chinese')).toBe('二〇二六年八月七日');
    expect(format('YYYY年MMM月DD日', example, 'chinese')).toBe('二零二六年八月七日');
    expect(format('MMM月DD号', example, 'chinese')).toBe('八月七号');
  });

  it('renders English dates', () => {
    expect(format(DEFAULT_PATTERN_EN, example, 'english')).toBe('2026/8/7 Friday');
    expect(format('MMMM d, yyyy', example, 'english')).toBe('August 7, 2026');
    expect(format('MMM d, E', example, 'english')).toBe('Aug 7, Fri');
    expect(format('dd/MM/yyyy', example, 'english')).toBe('07/08/2026');
    expect(format('yy', example, 'english')).toBe('26');
  });

  it('renders Chinese weekdays short and full', () => {
    expect(format('EEEE', example, 'chinese')).toBe('星期五');
    expect(format('E', example, 'chinese')).toBe('周五');
  });

  it('differs from Traditional only in the short weekday glyph', () => {
    expect(format('E', example, 'traditional')).toBe('週五');
    expect(format('EEEE', example, 'traditional')).toBe('星期五');
    expect(format('YYY年MMM月DD日', example, 'traditional')).toBe('二〇二六年八月七日');
  });

  it('degrades Chinese-numeral tokens in English', () => {
    expect(format('YYY', example, 'english')).toBe('2026');
    expect(format('YYYY', example, 'english')).toBe('2026');
    expect(format('DD', example, 'english')).toBe('7');
  });

  it('covers the day range with Chinese cardinals', () => {
    expect(chineseCardinal(7)).toBe('七');
    expect(chineseCardinal(10)).toBe('十');
    expect(chineseCardinal(11)).toBe('十一');
    expect(chineseCardinal(20)).toBe('二十');
    expect(chineseCardinal(21)).toBe('二十一');
    expect(chineseCardinal(30)).toBe('三十');
    expect(chineseCardinal(31)).toBe('三十一');
  });

  it('uses the requested zero glyph for Chinese years', () => {
    expect(chineseYear(2026, '〇')).toBe('二〇二六');
    expect(chineseYear(2026, '零')).toBe('二零二六');
    expect(chineseYear(2000, '〇')).toBe('二〇〇〇');
  });

  it('treats unknown letters and % specifiers as literals', () => {
    // No printf-style interpretation, so there is no format-injection surface.
    expect(format('%s yyyy', example, 'english')).toBe('%s 2026');
    // "%d" is not a specifier here: 'd' is the day token, so it renders the day.
    expect(format('%d', example, 'english')).toBe('%7');
    expect(format('abc yyyy', example, 'english')).toBe('abc 2026');
  });

  it('supports quoted literals', () => {
    expect(format("'Year:' yyyy", example, 'english')).toBe('Year: 2026');
    expect(format("''", example, 'english')).toBe("'");
  });

  it('preserves emoji in custom formats', () => {
    expect(format('📅 yyyy年', example, 'chinese')).toBe('📅 2026 年');
  });

  it('validates patterns', () => {
    expect(isValidPattern('yyyy年M月d日')).toBe(true);
    expect(isValidPattern('📅EEEE')).toBe(true);
    expect(isValidPattern(null)).toBe(false);
    expect(isValidPattern('')).toBe(false);
    // Literal-only text is not a usable date format.
    expect(isValidPattern('年月日')).toBe(false);
    expect(isValidPattern("'yyyy'")).toBe(false); // quoted, so no live token
    expect(isValidPattern('y'.repeat(MAX_PATTERN_LENGTH + 1))).toBe(false);
  });

  it('renders every built-in format non-empty', () => {
    for (const lang of LANGS) {
      for (const core of dateCores(lang)) {
        expect(preview(core, lang).length, `core: ${core}`).toBeGreaterThan(0);
        expect(isValidPattern(core), `core valid: ${core}`).toBe(true);
      }
      for (const fixed of fixedFormats(lang)) {
        expect(preview(fixed, lang).length, `fixed: ${fixed}`).toBeGreaterThan(0);
        expect(isValidPattern(fixed), `fixed valid: ${fixed}`).toBe(true);
      }
      for (const combo of weekdayCombos(lang)) {
        const full = composeCombo(combo, 'yyyy年M月d日');
        expect(preview(full, lang).length, `combo: ${combo}`).toBeGreaterThan(0);
        expect(comboLabel(combo, lang).length, `combo label: ${combo}`).toBeGreaterThan(0);
      }
    }
  });

  it('substitutes the DATE placeholder in combos', () => {
    expect(format(composeCombo('DATE EEEE', 'yyyy年M月d日'), example, 'chinese')).toBe(
      '2026 年 8 月 7 日 星期五'
    );
    expect(format(composeCombo('（EEEE）DATE', 'yyyy年M月d日'), example, 'chinese')).toBe(
      '（星期五）2026 年 8 月 7 日'
    );
  });

  it('offers the documented number of options per language', () => {
    expect(dateCores('chinese')).toHaveLength(27);
    expect(dateCores('english')).toHaveLength(52);
    expect(weekdayCombos('chinese')).toHaveLength(25);
    expect(weekdayCombos('english')).toHaveLength(25);
  });
});
