/**
 * A curated list of countries/regions and the time zone that represents them,
 * modelled after Android's region-based time zone selection.
 *
 * Selecting a region determines the time zone used for clock display. The first
 * entry (FOLLOW_SYSTEM_INDEX) represents "follow the system". Display names live
 * in the `region_names` message array, parallel by index to ZONE_IDS.
 *
 * Ported from com.clockmods.time.RegionTimeZones.
 */

export const FOLLOW_SYSTEM_INDEX = 0;

/** Sentinel meaning "follow the device's own time zone". */
export const TIME_ZONE_FOLLOW_SYSTEM = '';

/** IANA time zone ids; the empty string means "follow the system". */
export const ZONE_IDS: readonly string[] = [
  '',
  'Asia/Shanghai',
  'Asia/Urumqi',
  'Asia/Hong_Kong',
  'Asia/Macau',
  'Asia/Taipei',
  'Asia/Tokyo',
  'Asia/Seoul',
  'Asia/Singapore',
  'Asia/Bangkok',
  'Asia/Jakarta',
  'Asia/Kolkata',
  'Asia/Dubai',
  'Europe/Moscow',
  'Europe/London',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Istanbul',
  'Africa/Johannesburg',
  'Africa/Cairo',
  'America/Sao_Paulo',
  'America/Argentina/Buenos_Aires',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'America/Anchorage',
  'Pacific/Honolulu',
  'America/Toronto',
  'America/Mexico_City',
  'Australia/Sydney',
  'Australia/Perth',
  'Pacific/Auckland',
];

/** @returns the index of `zoneId`, or FOLLOW_SYSTEM_INDEX when not found. */
export function indexOfZoneId(zoneId: string | null | undefined): number {
  if (!zoneId) return FOLLOW_SYSTEM_INDEX;
  const index = ZONE_IDS.indexOf(zoneId);
  return index < 0 ? FOLLOW_SYSTEM_INDEX : index;
}
