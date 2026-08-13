/** Ported from app/src/test/java/com/clockmods/weather/WeatherModelsTest.java. */
import { describe, expect, it } from 'vitest';
import {
  detailCarouselItems,
  findForecastByDate,
  locationText,
  satisfies,
} from '../src/weather/models';
import type { DailyForecastData, WeatherDetail, WeatherDisplayData } from '../src/weather/models';

const detail = (overrides: Partial<WeatherDetail>): WeatherDetail => ({
  feelsLike: null,
  humidity: null,
  windDir: null,
  windScale: null,
  precip: null,
  warning: null,
  aqiValue: null,
  aqiCategory: null,
  ...overrides,
});

const display = (overrides: Partial<WeatherDisplayData>): WeatherDisplayData => ({
  locationId: '101010100',
  city: '北京市',
  district: '北京市',
  text: '晴',
  icon: '100',
  temperature: '26',
  updatedAt: 1,
  detail: null,
  ...overrides,
});

describe('locationText', () => {
  it('combines city and district without duplicates', () => {
    expect(locationText('深圳市', '宝安')).toBe('深圳宝安');
    expect(locationText('北京市', '北京市')).toBe('北京');
    expect(locationText(null, '东城区')).toBe('东城区');
    expect(locationText('阿拉善盟', '左旗')).toBe('阿拉善盟左旗');
  });

  it('orders English names district-first with a comma', () => {
    expect(locationText('Shenzhen', "Bao'an")).toBe("Bao'an, Shenzhen");
    expect(locationText('Shanghai', 'Pudong')).toBe('Pudong, Shanghai');
    expect(locationText('Beijing', 'Beijing')).toBe('Beijing');
    expect(locationText(null, "Bao'an")).toBe("Bao'an");
  });
});

describe('satisfies', () => {
  it('requires detail only when detailed weather is enabled', () => {
    const basic = display({});
    const detailed = display({
      detail: detail({ feelsLike: '27', humidity: '40', windDir: '东北风', windScale: '2', precip: '0' }),
    });

    expect(satisfies(basic, false)).toBe(true);
    expect(satisfies(basic, true)).toBe(false);
    expect(satisfies(detailed, false)).toBe(true);
    expect(satisfies(detailed, true)).toBe(true);
  });
});

describe('detailCarouselItems', () => {
  it('exposes multiple warnings as separate items', () => {
    const items = detailCarouselItems(detail({ warning: '台风红色预警\n\n暴雨橙色预警' }));

    expect(items).toHaveLength(2);
    expect(items[0]).toBe('台风红色预警');
    expect(items[1]).toBe('暴雨橙色预警');
  });

  it('keeps the temperature unit attached like percent', () => {
    const items = detailCarouselItems(detail({ feelsLike: '27', humidity: '40' }));

    expect(items[0]).toBe('体感 27℃');
    expect(items[1]).toBe('湿度 40%');
  });

  it('joins wind direction and force, and omits absent metrics', () => {
    expect(detailCarouselItems(detail({ windDir: '东北风', windScale: '2' }))).toEqual([
      '东北风 2 级',
    ]);
    expect(detailCarouselItems(detail({ windScale: '3' }))).toEqual(['3 级']);
    expect(detailCarouselItems(detail({}))).toEqual([]);
  });

  it('includes precipitation only when it is above zero', () => {
    expect(detailCarouselItems(detail({ precip: '0.0' }))).toEqual([]);
    expect(detailCarouselItems(detail({ precip: '1.2' }))).toEqual(['降水 1.2 mm']);
  });

  it('appends the air-quality category to the index', () => {
    expect(detailCarouselItems(detail({ aqiValue: '42', aqiCategory: '优' }))).toEqual(['空气 42 优']);
  });

  it('does not double the warning suffix', () => {
    expect(detailCarouselItems(detail({ warning: '暴雨橙色' }))).toEqual(['暴雨橙色预警']);
    expect(detailCarouselItems(detail({ warning: '暴雨橙色预警' }))).toEqual(['暴雨橙色预警']);
  });
});

describe('findForecastByDate', () => {
  it('matches on the fxDate key', () => {
    const data: DailyForecastData = {
      locationId: '1',
      city: '深圳市',
      district: '宝安',
      updatedAt: 0,
      entries: [
        {
          fxDate: '2026-08-07',
          tempMin: '26',
          tempMax: '33',
          iconDay: '100',
          textDay: '晴',
          windDirDay: '东北风',
          windScaleDay: '1-3',
          humidity: '60',
        },
      ],
    };
    expect(findForecastByDate(data, '2026-08-07')?.textDay).toBe('晴');
    expect(findForecastByDate(data, '2026-08-08')).toBeNull();
  });
});
