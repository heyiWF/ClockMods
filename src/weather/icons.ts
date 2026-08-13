/**
 * QWeather icons (CC BY 4.0), pre-extracted to their SVG path data.
 *
 * Ported from com.clockmods.ui.WeatherIcon, which parsed the same SVG assets at
 * runtime and rasterised them through android.graphics.Path. Here the paths are
 * extracted at build time and rendered as inline SVG, so `currentColor` handles
 * the tinting the Android code did with a Paint.
 */

const VIEW_BOX = 16;
const SVG_NS = 'http://www.w3.org/2000/svg';

let paths: Record<string, string> = {};
let loading: Promise<void> | null = null;

export function loadWeatherIcons(): Promise<void> {
  if (!loading) {
    loading = fetch(`${import.meta.env.BASE_URL}data/weather-icons.json`)
      .then((response) => (response.ok ? response.json() : {}))
      .then((data: Record<string, string>) => {
        paths = data;
      })
      .catch(() => {
        // Without icons the weather line still renders its text.
        paths = {};
      });
  }
  return loading;
}

/**
 * @param code QWeather icon code, e.g. "100"
 * @param fill solid style when true, outline (line) style when false
 */
export function iconPath(code: string | null | undefined, fill: boolean): string | null {
  if (!code || !/^\d+$/.test(code)) return null;
  // The asset base name doubles as the key so the two style variants never collide.
  return paths[fill ? `${code}-fill` : code] ?? null;
}

/** Creates an inline `<svg>` that inherits its colour from CSS. */
export function createWeatherIcon(code: string | null | undefined, fill: boolean): SVGSVGElement | null {
  const d = iconPath(code, fill);
  if (!d) return null;
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('viewBox', `0 0 ${VIEW_BOX} ${VIEW_BOX}`);
  svg.setAttribute('aria-hidden', 'true');
  svg.classList.add('weather-icon');
  const path = document.createElementNS(SVG_NS, 'path');
  path.setAttribute('d', d);
  path.setAttribute('fill', 'currentColor');
  svg.appendChild(path);
  return svg;
}

/** Test hook: installs a path table without fetching. */
export function setWeatherIconPaths(table: Record<string, string>): void {
  paths = table;
  loading = Promise.resolve();
}
