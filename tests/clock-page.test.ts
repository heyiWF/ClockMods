/**
 * @vitest-environment jsdom
 *
 * Rendering smoke tests for the clock face. jsdom has no text shaping, so this
 * covers structure and content — which characters are emitted, which rows are
 * visible, how transitions are applied — not pixel geometry.
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CharacterLine, renderSupportingText } from '../src/ui/clock-face';
import { TRANSITION_FADE, TRANSITION_FLIP, TRANSITION_SLIDE_UP } from '../src/core/prefs';

const options = (overrides: Partial<{ animate: boolean; transition: string; colonVisible: boolean }> = {}) => ({
  animate: true,
  transition: TRANSITION_FADE,
  colonVisible: true,
  ...overrides,
});

describe('CharacterLine', () => {
  let host: HTMLElement;

  beforeEach(() => {
    host = document.createElement('div');
    document.body.appendChild(host);
  });

  afterEach(() => {
    host.remove();
    vi.useRealTimers();
  });

  it('emits one cell per character and marks digits and colons', () => {
    const line = new CharacterLine(host);
    line.setText('08:05', options());

    expect(host.children).toHaveLength(5);
    expect(host.textContent).toBe('08:05');
    const cells = [...host.children] as HTMLElement[];
    expect(cells.filter((cell) => 'digit' in cell.dataset)).toHaveLength(4);
    expect(cells.filter((cell) => 'colon' in cell.dataset)).toHaveLength(1);
  });

  it('animates only the characters that changed', () => {
    const line = new CharacterLine(host);
    line.setText('08:05:07', options());
    line.setText('08:05:08', options());

    const cells = [...host.children] as HTMLElement[];
    // Only the last digit gets an outgoing glyph layered over it.
    expect(cells.filter((cell) => cell.querySelector('.glyph.is-out'))).toHaveLength(1);
    expect(cells[7].querySelector('.glyph.is-out')?.textContent).toBe('7');
    expect(cells[7].querySelector('.glyph.is-in')?.textContent).toBe('8');
  });

  it('applies the configured transition class', () => {
    const line = new CharacterLine(host);
    line.setText('1', options({ transition: TRANSITION_SLIDE_UP }));
    line.setText('2', options({ transition: TRANSITION_SLIDE_UP }));

    expect(host.querySelector('.glyph.is-out')?.className).toContain('anim-slide-up');
  });

  it('rebuilds without animating when the shape changes', () => {
    const line = new CharacterLine(host);
    line.setText('08:05', options());
    line.setText('08:05:07', options());

    expect(host.children).toHaveLength(8);
    expect(host.querySelector('.glyph.is-out')).toBeNull();
  });

  it('skips animation entirely when disabled', () => {
    const line = new CharacterLine(host);
    line.setText('08:05:07', options({ animate: false }));
    line.setText('08:05:08', options({ animate: false }));

    expect(host.textContent).toBe('08:05:08');
    expect(host.querySelector('.glyph.is-out')).toBeNull();
  });

  it('toggles colon visibility without rebuilding', () => {
    const line = new CharacterLine(host);
    line.setText('08:05', options());
    const colon = host.querySelector<HTMLElement>('[data-colon]')!;
    expect(colon.classList.contains('is-hidden')).toBe(false);

    line.setText('08:05', options({ colonVisible: false }));
    expect(colon.classList.contains('is-hidden')).toBe(true);
    // Same element: the line was not rebuilt.
    expect(host.querySelector('[data-colon]')).toBe(colon);
  });

  it('cleans up the outgoing glyph after the transition', () => {
    vi.useFakeTimers();
    const line = new CharacterLine(host);
    line.setText('1', options());
    line.setText('2', options());
    expect(host.querySelector('.glyph.is-out')).not.toBeNull();

    vi.advanceTimersByTime(500);
    expect(host.querySelector('.glyph.is-out')).toBeNull();
    expect(host.textContent).toBe('2');
  });

  it('keeps the incoming flip active until its own animation finishes', () => {
    const line = new CharacterLine(host);
    line.setText('1', options({ transition: TRANSITION_FLIP }));
    line.setText('2', options({ transition: TRANSITION_FLIP }));
    const outgoing = host.querySelector<HTMLElement>('.glyph.is-out')!;
    const incoming = host.querySelector<HTMLElement>('.glyph.is-in')!;

    outgoing.dispatchEvent(new Event('animationend'));
    expect(incoming.classList.contains('anim-flip')).toBe(true);
    expect(outgoing.isConnected).toBe(true);

    incoming.dispatchEvent(new Event('animationend'));
    expect(incoming.classList.contains('anim-flip')).toBe(false);
    expect(outgoing.isConnected).toBe(false);
  });
});

describe('renderSupportingText', () => {
  let host: HTMLElement;

  beforeEach(() => {
    host = document.createElement('div');
  });

  it('renders plain text unchanged', () => {
    renderSupportingText(host, '2026 年 8 月 7 日 星期五');
    expect(host.textContent).toBe('2026 年 8 月 7 日 星期五');
  });

  it('wraps pangu spaces so their tracking is suppressed', () => {
    renderSupportingText(host, '2026 年');
    // The digit before the pangu space and the space itself must not be widened.
    expect(host.querySelectorAll('.no-tracking').length).toBeGreaterThan(0);
    expect(host.textContent).toBe('2026 年');
  });

  it('skips re-rendering identical text', () => {
    renderSupportingText(host, '星期五');
    const first = host.firstChild;
    renderSupportingText(host, '星期五');
    expect(host.firstChild).toBe(first);
  });

  it('clears on empty input', () => {
    renderSupportingText(host, '星期五');
    renderSupportingText(host, '');
    expect(host.childNodes).toHaveLength(0);
  });
});
