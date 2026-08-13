/**
 * Minimal typings for the subset of lunar-javascript (6tail, MIT) this app uses.
 * The package ships no declarations; these mirror the methods the Android build
 * called on cn.6tail:lunar 1.7.7, which is the same version line.
 */
declare module 'lunar-javascript' {
  export interface ShuJiu {
    getName(): string;
    /** 1 on the first day of the period. */
    getIndex(): number;
  }

  export interface Fu {
    getName(): string;
    /** 1 on the first day of the period. */
    getIndex(): number;
  }

  export interface Lunar {
    getYear(): number;
    /** Negative for a leap month, e.g. -6 for 闰六月. */
    getMonth(): number;
    getDay(): number;
    /** 正 / 二 … 十 / 冬 / 腊, prefixed with 闰 for a leap month. */
    getMonthInChinese(): string;
    /** 初一 / 十二 / 廿五 / 三十 … */
    getDayInChinese(): string;
    getYearInGanZhi(): string;
    getYearShengXiao(): string;
    /** Solar term falling on this day, or "" when none. */
    getJieQi(): string;
    getFestivals(): string[];
    getOtherFestivals(): string[];
    getShuJiu(): ShuJiu | null;
    getFu(): Fu | null;
    /** 今日宜 */
    getDayYi(): string[];
    /** 今日忌 */
    getDayJi(): string[];
  }

  export interface Solar {
    getYear(): number;
    getMonth(): number;
    getDay(): number;
    getLunar(): Lunar;
    getFestivals(): string[];
    getOtherFestivals(): string[];
  }

  export const Solar: {
    /** month is 1-based. */
    fromYmd(year: number, month: number, day: number): Solar;
    fromDate(date: Date): Solar;
  };

  export const Lunar: {
    fromYmd(year: number, month: number, day: number): Lunar;
  };
}
