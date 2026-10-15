import { readLocalFont, localFontAlias, cssString, loadLocalFont } from './local-fonts';
/** Bundled and opt-in local fonts share glyph fallback and measurement paths. */

/** Used only for glyphs missing from the selected face. */
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
  const local=readLocalFont(id);
  if(local) return {id,displayName:local.fullName,stack:cssString(localFontAlias(local))+', '+SYSTEM_LATIN+', '+CJK_FALLBACK,system:false,hasBoldFile:true};
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
  return !!readLocalFont(id) || FONT_OPTIONS.some((option) => option.id === id);
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
export async function ensureFontLoaded(id: string, bold: boolean | number, doc: Document = document): Promise<void> {
  await loadLocalFont(id, doc);
  const option = optionFor(normalizeFontFamily(id));
  if (option.system || !('fonts' in doc)) return;
  const weight = typeof bold === 'number' ? bold : bold ? 700 : 400;
  try {
    await doc.fonts.load(`${weight} 100px ${option.stack}`, '0123456789:年月星期天气');
  } catch {
    // A missing font file must not stop the clock from rendering.
  }
}

/** Only weights that the bundled files actually supply. System faces depend on the host OS. */
export function availableWeights(family: string): number[] {
 const local=readLocalFont(family);
 if(local) return local.min===local.max?[local.weight]:Array.from({length:Math.floor(local.max)-Math.ceil(local.min)+1},(_,i)=>Math.ceil(local.min)+i);
 if(family==='google_sans_display'||family==='google_sans_text')return [400,500,700];
 if(family==='lato')return [100,300,400,700,900];
 if(family==='lora')return [400,500,600,700];
 if(optionFor(family).system)return [400,700];
 return [100,200,300,400,500,600,700,800,900];
}
export function nearestWeight(family: string, value: number): number {
 return availableWeights(family).reduce((best,next)=>Math.abs(next-value)<Math.abs(best-value)?next:best);
}
