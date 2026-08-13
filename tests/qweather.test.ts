/**
 * Ported from app/src/test/java/com/clockmods/weather/QWeatherSignerTest.java and
 * QWeatherClientTest.java. The Android signer used net.i2p.crypto:eddsa; this one
 * uses WebCrypto, so the test verifies the produced JWT against the matching
 * public key rather than comparing bytes.
 */
import { describe, expect, it } from 'vitest';
import { getPublicKeyAsync, verifyAsync } from '@noble/ed25519';
import {
  createToken,
  formatCoordinate,
  formatLocation,
  formatWarning,
  formatWarnings,
  parseDailyForecast,
} from '../src/weather/qweather';

/** DER prefix for a PKCS#8-wrapped Ed25519 private key, followed by the 32-byte seed. */
const PKCS8_PREFIX = Uint8Array.from([
  0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20,
]);

/** The same deterministic seed the Android test used: bytes 0..31. */
const SEED = Uint8Array.from({ length: 32 }, (_, index) => index);

function pkcs8Base64(seed: Uint8Array): string {
  const der = new Uint8Array(PKCS8_PREFIX.length + seed.length);
  der.set(PKCS8_PREFIX);
  der.set(seed, PKCS8_PREFIX.length);
  return Buffer.from(der).toString('base64');
}

function decodeBase64Url(value: string): Uint8Array {
  return new Uint8Array(Buffer.from(value.replace(/-/g, '+').replace(/_/g, '/'), 'base64'));
}

function decodeJson(value: string): Record<string, unknown> {
  return JSON.parse(Buffer.from(value.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8'));
}

describe('createToken', () => {
  it('creates a verifiable QWeather JWT', async () => {
    const token = await createToken('CREDENTIAL', 'PROJECT', pkcs8Base64(SEED), 1000);
    const parts = token.split('.');
    expect(parts).toHaveLength(3);

    const header = decodeJson(parts[0]);
    const payload = decodeJson(parts[1]);
    expect(header.alg).toBe('EdDSA');
    expect(header.kid).toBe('CREDENTIAL');
    expect(payload.sub).toBe('PROJECT');
    // iat is backdated 30s and the token lives 900s, matching the Android signer.
    expect(payload.iat).toBe(970);
    expect(payload.exp).toBe(1870);
    // base64url, unpadded.
    expect(token).not.toContain('=');

    const publicKey = await getPublicKeyAsync(SEED);
    const signed = new TextEncoder().encode(`${parts[0]}.${parts[1]}`);
    expect(await verifyAsync(decodeBase64Url(parts[2]), signed, publicKey)).toBe(true);
  });

  it('accepts a PEM-wrapped key', async () => {
    const pem = `-----BEGIN PRIVATE KEY-----\n${pkcs8Base64(SEED)}\n-----END PRIVATE KEY-----`;
    await expect(createToken('C', 'P', pem, 1000)).resolves.toContain('.');
  });
});

describe('coordinate formatting', () => {
  it('sends longitude first with two decimals', () => {
    expect(formatLocation(22.5470000001, 114.0859)).toBe('114.09,22.55');
    expect(formatCoordinate(22.5470000001)).toBe('22.55');
  });
});

describe('warning text', () => {
  const alert = (event: string, color: string) => ({
    eventType: { name: event },
    color: { code: color },
  });

  it('prefers the headline when present', () => {
    expect(formatWarning({ headline: '深圳市气象台发布暴雨橙色预警' })).toBe(
      '深圳市气象台发布暴雨橙色预警'
    );
  });

  it('composes event, colour and suffix per language', () => {
    expect(formatWarning(alert('暴雨', 'orange'), 'zh')).toBe('暴雨橙色预警');
    expect(formatWarning(alert('暴雨', 'orange'), 'zh-hant')).toBe('暴雨橙色預警');
    expect(formatWarning(alert('Rainstorm', 'orange'), 'en')).toBe('Rainstorm Orange Warning');
  });

  it('omits the colour when absent', () => {
    expect(formatWarning({ eventType: { name: 'Rainstorm' } }, 'en')).toBe('Rainstorm Warning');
    expect(formatWarning({ eventType: { name: '暴雨' } }, 'zh')).toBe('暴雨预警');
  });

  it('returns null without an event name', () => {
    expect(formatWarning({})).toBeNull();
  });

  it('joins multiple alerts with newlines and caps at 20', () => {
    expect(formatWarnings([alert('暴雨', 'orange'), alert('台风', 'red')], 'zh')).toBe(
      '暴雨橙色预警\n台风红色预警'
    );
    expect(formatWarnings([], 'zh')).toBeNull();
    expect(formatWarnings(undefined, 'zh')).toBeNull();
    expect(
      formatWarnings(
        Array.from({ length: 25 }, () => alert('暴雨', 'orange')),
        'zh'
      )?.split('\n')
    ).toHaveLength(20);
  });
});

describe('parseDailyForecast', () => {
  it('maps the daily array and keeps source order', () => {
    const data = parseDailyForecast(
      {
        daily: [
          { fxDate: '2026-08-07', tempMin: '26', tempMax: '33', iconDay: '100', textDay: '晴' },
          { fxDate: '2026-08-08', tempMin: '27', tempMax: '34', iconDay: '101', textDay: '多云' },
        ],
      },
      '101280601',
      '深圳市',
      '宝安',
      1234
    );

    expect(data.entries).toHaveLength(2);
    expect(data.entries[0].fxDate).toBe('2026-08-07');
    expect(data.entries[1].textDay).toBe('多云');
    expect(data.locationId).toBe('101280601');
    expect(data.updatedAt).toBe(1234);
  });

  it('rejects an empty or dateless payload', () => {
    expect(() => parseDailyForecast({}, '1', '', '', 0)).toThrow();
    expect(() => parseDailyForecast({ daily: [] }, '1', '', '', 0)).toThrow();
    expect(() => parseDailyForecast({ daily: [{ tempMin: '1' }] }, '1', '', '', 0)).toThrow();
  });
});
