/**
 * Ported from app/src/test/java/com/clockmods/weather/WeatherLocationCatalogTest.java.
 * The CSV is converted to tuples at build time, so the fixture uses that shape.
 */
import { describe, expect, it } from 'vitest';
import {
  cities,
  cityLabels,
  displayCity,
  displayDistrict,
  districts,
  findById,
  parseCatalog,
  provinceLabels,
  provinces,
} from '../src/weather/city-catalog';

const catalog = parseCatalog({
  entries: [
    ['101280601', '广东省', '深圳市', '深圳', 'Guangdong', 'Shenzhen', 'Shenzhen', 22.547, 114.0859],
    ['101280606', '广东省', '深圳市', '宝安', 'Guangdong', 'Shenzhen', 'Baoan', 22.7547, 113.8287],
    ['101280109', '广东省', '广州市', '天河', 'Guangdong', 'Guangzhou', 'Tianhe', 23.1246, 113.3612],
  ],
});

describe('parseCatalog', () => {
  it('filters province, city and district in source order', () => {
    expect(provinces(catalog)[0]).toBe('广东省');
    expect(cities(catalog, '广东省')).toHaveLength(2);

    const shenzhen = districts(catalog, '广东省', '深圳市');
    expect(shenzhen).toHaveLength(2);
    expect(shenzhen[1].district).toBe('宝安');
    expect(shenzhen[1].locationId).toBe('101280606');
  });

  it('exposes English names with a Chinese fallback', () => {
    const baoan = districts(catalog, '广东省', '深圳市')[1];
    expect(displayDistrict(baoan, true)).toBe('Baoan');
    expect(displayCity(baoan, true)).toBe('Shenzhen');
    expect(displayDistrict(baoan, false)).toBe('宝安');

    const fallback = parseCatalog({
      entries: [['1', '某省', '某市', '某区', '', '', '', 1, 2]],
    })[0];
    expect(fallback.provinceEn).toBe('某省');
    expect(fallback.districtEn).toBe('某区');
  });

  it('aligns labels index-for-index with the ids', () => {
    expect(provinceLabels(catalog, true)).toEqual(['Guangdong']);
    expect(cityLabels(catalog, '广东省', true)).toEqual(['Shenzhen', 'Guangzhou']);
    expect(cityLabels(catalog, '广东省', false)).toEqual(['深圳市', '广州市']);
  });

  it('skips rows missing a required column', () => {
    const partial = parseCatalog({
      entries: [
        ['', '广东省', '深圳市', '宝安', '', '', '', 1, 2],
        ['1', '', '深圳市', '宝安', '', '', '', 1, 2],
        ['2', '广东省', '深圳市', '宝安', '', '', '', 1, 2],
      ],
    });
    expect(partial).toHaveLength(1);
    expect(partial[0].locationId).toBe('2');
  });

  it('finds an entry by id and keeps its coordinates', () => {
    const entry = findById(catalog, '101280606');
    expect(entry?.district).toBe('宝安');
    expect(entry?.latitude).toBeCloseTo(22.7547, 4);
    expect(findById(catalog, '')).toBeNull();
    expect(findById(catalog, 'missing')).toBeNull();
  });
});
