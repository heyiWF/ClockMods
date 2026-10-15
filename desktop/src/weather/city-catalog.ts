/**
 * The bundled China city catalog backing the manual location picker.
 *
 * Ported from com.clockmods.weather.WeatherLocationCatalog. The CSV is converted
 * to a compact tuple JSON at build time (scripts/build-data.mjs) and fetched
 * lazily — 3.5k entries only matter when the picker is opened.
 */

export interface LocationEntry {
  locationId: string;
  province: string;
  city: string;
  district: string;
  provinceEn: string;
  cityEn: string;
  districtEn: string;
  latitude: number;
  longitude: number;
}

type Tuple = [string, string, string, string, string, string, string, number, number];

let entries: LocationEntry[] | null = null;
let loading: Promise<LocationEntry[]> | null = null;

/** Builds entries from the tuple form; blank English names fall back to Chinese. */
export function parseCatalog(payload: { entries: Tuple[] }): LocationEntry[] {
  const result: LocationEntry[] = [];
  for (const [id, province, city, district, provinceEn, cityEn, districtEn, lat, lon] of payload.entries) {
    if (!id || !province || !city || !district) continue;
    result.push({
      locationId: id,
      province,
      city,
      district,
      provinceEn: provinceEn || province,
      cityEn: cityEn || city,
      districtEn: districtEn || district,
      latitude: Number.isFinite(lat) ? lat : Number.NaN,
      longitude: Number.isFinite(lon) ? lon : Number.NaN,
    });
  }
  return result;
}

export function loadCatalog(): Promise<LocationEntry[]> {
  if (entries) return Promise.resolve(entries);
  if (!loading) {
    loading = fetch(`${import.meta.env.BASE_URL}data/china-cities.json`)
      .then((response) => {
        if (!response.ok) throw new Error(`catalog ${response.status}`);
        return response.json() as Promise<{ entries: Tuple[] }>;
      })
      .then((payload) => {
        entries = parseCatalog(payload);
        return entries;
      })
      .catch((error) => {
        loading = null;
        throw error;
      });
  }
  return loading;
}

/** Already-loaded catalog, or null when the picker has never been opened. */
export function catalogIfLoaded(): LocationEntry[] | null {
  return entries;
}

export function provinces(list: LocationEntry[]): string[] {
  return unique(list.map((entry) => entry.province));
}

/** Display labels aligned index-for-index with `provinces`. */
export function provinceLabels(list: LocationEntry[], english: boolean): string[] {
  return uniqueBy(list, (entry) => entry.province, (entry) =>
    english ? entry.provinceEn : entry.province
  );
}

export function cities(list: LocationEntry[], province: string): string[] {
  return unique(list.filter((entry) => entry.province === province).map((entry) => entry.city));
}

export function cityLabels(list: LocationEntry[], province: string, english: boolean): string[] {
  return uniqueBy(
    list.filter((entry) => entry.province === province),
    (entry) => entry.city,
    (entry) => (english ? entry.cityEn : entry.city)
  );
}

export function districts(
  list: LocationEntry[],
  province: string,
  city: string
): LocationEntry[] {
  const seen = new Set<string>();
  const result: LocationEntry[] = [];
  for (const entry of list) {
    if (entry.province !== province || entry.city !== city) continue;
    if (seen.has(entry.district)) continue;
    seen.add(entry.district);
    result.push(entry);
  }
  return result;
}

export function findById(list: LocationEntry[], locationId: string): LocationEntry | null {
  if (!locationId) return null;
  return list.find((entry) => entry.locationId === locationId) ?? null;
}

export function displayDistrict(entry: LocationEntry, english: boolean): string {
  return english ? entry.districtEn : entry.district;
}

export function displayCity(entry: LocationEntry, english: boolean): string {
  return english ? entry.cityEn : entry.city;
}

function unique(values: string[]): string[] {
  return [...new Set(values)];
}

function uniqueBy(
  list: LocationEntry[],
  key: (entry: LocationEntry) => string,
  label: (entry: LocationEntry) => string
): string[] {
  const seen = new Map<string, string>();
  for (const entry of list) {
    const id = key(entry);
    if (!seen.has(id)) seen.set(id, label(entry));
  }
  return [...seen.values()];
}
