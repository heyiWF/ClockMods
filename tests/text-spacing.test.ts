/** Ported from app/src/test/java/com/clockmods/ui/TextSpacingTest.java. */
import { describe, expect, it } from 'vitest';
import { containsChinese, pangu } from '../src/format/text-spacing';

describe('pangu', () => {
  it('inserts a space between Chinese and digits in both directions', () => {
    expect(pangu('2026年8月7日')).toBe('2026 年 8 月 7 日');
    expect(pangu('第5圈')).toBe('第 5 圈');
  });

  it('inserts a space between Chinese and Latin letters', () => {
    expect(pangu('北京AQI')).toBe('北京 AQI');
    expect(pangu('Pro版')).toBe('Pro 版');
  });

  it('keeps the weekday suffix readable', () => {
    expect(pangu('2026年8月7日 星期五')).toBe('2026 年 8 月 7 日 星期五');
  });

  it('does not double existing spaces', () => {
    expect(pangu('2026 年 8 月')).toBe('2026 年 8 月');
    expect(pangu('北京 AQI')).toBe('北京 AQI');
  });

  it('leaves pure Chinese unchanged', () => {
    expect(pangu('二〇二六年八月七日')).toBe('二〇二六年八月七日');
    expect(pangu('星期五')).toBe('星期五');
  });

  it('leaves pure Latin and numeric unchanged', () => {
    expect(pangu('2026/8/7 Friday')).toBe('2026/8/7 Friday');
    expect(pangu('August 7, 2026')).toBe('August 7, 2026');
  });

  it('does not space around symbols or full-width punctuation', () => {
    expect(pangu('28℃')).toBe('28℃');
    expect(pangu('95%')).toBe('95%');
    expect(pangu('2026年8月7日（星期五）')).toBe('2026 年 8 月 7 日（星期五）');
  });

  it('handles emoji and surrogate pairs safely', () => {
    expect(pangu('📅2026年')).toBe('📅2026 年');
    expect(pangu('今天📅')).toBe('今天📅');
  });

  // The Android version also accepted null; TypeScript's strict mode makes that
  // unreachable, so only the empty and single-character cases remain.
  it('handles short input', () => {
    expect(pangu('')).toBe('');
    expect(pangu('年')).toBe('年');
  });
});

describe('containsChinese', () => {
  it('detects ideographs but not punctuation or Latin', () => {
    expect(containsChinese('星期五')).toBe(true);
    expect(containsChinese('2026/8/7 Friday')).toBe(false);
    expect(containsChinese('（）')).toBe(false);
  });
});
