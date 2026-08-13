/**
 * Inserts a half-width space (U+0020) at boundaries between Chinese ideographs
 * and half-width Latin letters or digits, following the common "pangu" spacing
 * convention.
 *
 * Only CJK <-> ASCII letter/digit transitions are touched, in both directions.
 * Existing spaces are never doubled (a space is neither CJK nor alphanumeric, so
 * a boundary that already has one produces no extra insertion). Full-width
 * punctuation, symbols such as ℃, and emoji (including surrogate pairs) are
 * copied through unchanged and never trigger a space. Number-to-unit spacing
 * (e.g. 28℃) is intentionally out of scope and handled at the concatenation
 * sites instead.
 *
 * Ported from com.clockmods.ui.TextSpacing.
 */

/**
 * @returns `text` with half-width spaces inserted between adjacent Chinese and
 *   Latin-alphanumeric characters. Pure-Chinese and pure-Latin strings, as well
 *   as empty or single-character input, are returned unchanged.
 */
export function pangu(text: string): string {
  if (text.length < 2) return text;
  let result = '';
  let previous = -1;
  for (const character of text) {
    const codePoint = character.codePointAt(0)!;
    if (previous !== -1 && needsPanguSpace(previous, codePoint)) result += ' ';
    result += character;
    previous = codePoint;
  }
  return result;
}

export function needsPanguSpace(left: number, right: number): boolean {
  return (
    (isCjk(left) && isLatinAlphanumeric(right)) || (isLatinAlphanumeric(left) && isCjk(right))
  );
}

function isLatinAlphanumeric(codePoint: number): boolean {
  return (
    (codePoint >= 0x30 && codePoint <= 0x39) || // 0-9
    (codePoint >= 0x41 && codePoint <= 0x5a) || // A-Z
    (codePoint >= 0x61 && codePoint <= 0x7a) // a-z
  );
}

function isCjk(codePoint: number): boolean {
  return (
    (codePoint >= 0x4e00 && codePoint <= 0x9fff) || // CJK Unified Ideographs
    (codePoint >= 0x3400 && codePoint <= 0x4dbf) || // Extension A
    (codePoint >= 0xf900 && codePoint <= 0xfaff) || // Compatibility Ideographs
    (codePoint >= 0x20000 && codePoint <= 0x2a6df) || // Extension B
    codePoint === 0x3007 // 〇 ideographic number zero
  );
}

/**
 * Whether the CJK-aware letter-spacing gap applies at `boundary`. Keeps a pangu
 * U+0020 space at its font-defined width instead of widening it further.
 *
 * Ported from ClockView.hasSupportingTrackingAt.
 */
export function hasSupportingTrackingAt(text: string, boundary: number): boolean {
  if (boundary <= 0 || boundary >= text.length) return false;
  if (text.charAt(boundary - 1) === ' ' && isPanguSpaceAt(text, boundary - 1)) return false;
  return text.charAt(boundary) !== ' ' || !isPanguSpaceAt(text, boundary);
}

function isPanguSpaceAt(text: string, spaceOffset: number): boolean {
  if (spaceOffset <= 0 || spaceOffset + 1 >= text.length) return false;
  if (text.charAt(spaceOffset) !== ' ') return false;
  const before = text.codePointAt(spaceOffset - 1);
  const after = text.codePointAt(spaceOffset + 1);
  if (before === undefined || after === undefined) return false;
  return needsPanguSpace(before, after);
}

/** CJK ranges that keep the system typeface for supporting text. */
export function isChineseCodePoint(codePoint: number): boolean {
  return (
    (codePoint >= 0x4e00 && codePoint <= 0x9fff) || // CJK Unified Ideographs
    (codePoint >= 0x3400 && codePoint <= 0x4dbf) || // Extension A
    (codePoint >= 0xf900 && codePoint <= 0xfaff) || // Compatibility Ideographs
    (codePoint >= 0x3000 && codePoint <= 0x303f) // CJK Symbols and Punctuation
  );
}

/** Ported from ClockView.containsChinese (excludes CJK punctuation). */
export function containsChinese(text: string): boolean {
  for (const character of text) {
    const codePoint = character.codePointAt(0)!;
    if (
      (codePoint >= 0x4e00 && codePoint <= 0x9fff) ||
      (codePoint >= 0x3400 && codePoint <= 0x4dbf) ||
      (codePoint >= 0xf900 && codePoint <= 0xfaff)
    ) {
      return true;
    }
  }
  return false;
}
