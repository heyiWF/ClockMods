/**
 * Central, data-driven registry of the clock font families.
 *
 * Ported from com.clockmods.background.FontCatalog. All eleven Pro options are
 * kept, but the two SF Pro entries resolve through a CSS system-font stack
 * instead of a bundled file: Apple's license does not permit redistribution, and
 * on Apple devices `-apple-system` / `ui-rounded` already render the real faces.
 *
 * Every stack ends with the same CJK fallback chain. That reproduces the Android
 * behaviour where `ClockTypefaceResolver.resolveSupportingForCodePoint` handed
 * CJK code points to the system typeface, since none of the bundled Latin faces
 * carry Chinese glyphs — here the browser's own font fallback does it.
 */

/** Appended to every stack so Chinese always renders with a system face. */
const CJK_FALLBACK =
  '"PingFang SC", "PingFang TC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans CJK SC", "Source Han Sans SC", sans-serif';

const SYSTEM_LATIN = 'system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial';

export interface FontOption {
  id: string;
  /** Brand name shown verbatim in every language; the system entry is localized. */
  displayName: string;
  /** CSS font-family stack. */
  stack: string;
  /** Whether the family is a system stack rather than a bundled webfont. */
  system: boolean;
  /** Whether a dedicated bold file exists (otherwise the browser synthesizes it). */
  hasBoldFile: boolean;
}

export const FONT_SYSTEM = 'system';
export const FONT_ROBOTO = 'roboto';
export const FONT_GOOGLE_SANS_DISPLAY = 'google_sans_display';
export const FONT_GOOGLE_SANS_TEXT = 'google_sans_text';
export const FONT_SF_PRO_DISPLAY = 'sf_pro_display';
export const FONT_SF_PRO_ROUNDED = 'sf_pro_rounded';
export const FONT_INTER = 'inter';
export const FONT_LATO = 'lato';
export const FONT_LORA = 'lora';
export const FONT_NOTO_SANS = 'noto_sans';
export const FONT_BITCOUNT = 'bitcount_grid_double';

/** Legacy id migrated by normalizeFontFamily, matching the Android behaviour. */
const LEGACY_FONT_GOOGLE_SANS = 'google_sans';

const bundled = (id: string, displayName: string, cssName: string): FontOption => ({
  id,
  displayName,
  stack: `"${cssName}", ${SYSTEM_LATIN}, ${CJK_FALLBACK}`,
  system: false,
  hasBoldFile: true,
});

export const FONT_OPTIONS: readonly FontOption[] = [
  {
    id: FONT_SYSTEM,
    displayName: '系统字体',
    stack: `${SYSTEM_LATIN}, ${CJK_FALLBACK}`,
    system: true,
    hasBoldFile: false,
  },
  bundled(FONT_ROBOTO, 'Roboto', 'Roboto'),
  bundled(FONT_GOOGLE_SANS_DISPLAY, 'Google Sans Display', 'Google Sans Display'),
  bundled(FONT_GOOGLE_SANS_TEXT, 'Google Sans Text', 'Google Sans Text'),
  {
    id: FONT_SF_PRO_DISPLAY,
    displayName: 'SF Pro Display',
    stack: `-apple-system, BlinkMacSystemFont, "SF Pro Display", ${SYSTEM_LATIN}, ${CJK_FALLBACK}`,
    system: true,
    hasBoldFile: false,
  },
  {
    id: FONT_SF_PRO_ROUNDED,
    displayName: 'SF Pro Rounded',
    stack: `ui-rounded, "SF Pro Rounded", -apple-system, ${SYSTEM_LATIN}, ${CJK_FALLBACK}`,
    system: true,
    hasBoldFile: false,
  },
  bundled(FONT_INTER, 'Inter', 'Inter'),
  bundled(FONT_LATO, 'Lato', 'Lato'),
  bundled(FONT_LORA, 'Lora', 'Lora'),
  bundled(FONT_NOTO_SANS, 'Noto Sans', 'Noto Sans'),
  bundled(FONT_BITCOUNT, 'Bitcount Grid Double', 'Bitcount Grid Double'),
];

/** Returns the option for `id`, or the system option when unknown. */
export function optionFor(id: string): FontOption {
  return FONT_OPTIONS.find((option) => option.id === id) ?? FONT_OPTIONS[0];
}

export function indexOfFont(id: string): number {
  const index = FONT_OPTIONS.findIndex((option) => option.id === id);
  return index < 0 ? 0 : index;
}

export function fontIdForIndex(index: number): string {
  return FONT_OPTIONS[index]?.id ?? FONT_SYSTEM;
}

export function isFontAvailable(id: string): boolean {
  return FONT_OPTIONS.some((option) => option.id === id);
}

export function normalizeFontFamily(id: string | null | undefined): string {
  if (id === LEGACY_FONT_GOOGLE_SANS) return FONT_GOOGLE_SANS_DISPLAY;
  return id && isFontAvailable(id) ? id : FONT_SYSTEM;
}

/** CSS font-family stack for a stored font id. */
export function fontStack(id: string): string {
  return optionFor(normalizeFontFamily(id)).stack;
}

/**
 * Waits for the selected family to be usable before text is measured. Bundled
 * families load lazily, so measuring too early would size the clock against a
 * fallback face.
 */
export async function ensureFontLoaded(id: string, bold: boolean): Promise<void> {
  const option = optionFor(normalizeFontFamily(id));
  if (option.system || !('fonts' in document)) return;
  const weight = bold ? 700 : 400;
  try {
    await document.fonts.load(`${weight} 100px ${option.stack}`, '0123456789:');
  } catch {
    // A missing font file must not stop the clock from rendering.
  }
}
