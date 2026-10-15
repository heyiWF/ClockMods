/**
 * The Material 3 press ripple.
 *
 * A direct port of material-web's <md-ripple>
 * (@material/web/ripple/internal/ripple.ts): the same growth constants, the
 * same soft-edge radial gradient and the same "hold the press for at least
 * MINIMUM_PRESS_MS so a tap still reads as a press" rule. The one deliberate
 * difference is that the reference element animates its own ::after
 * pseudo-element, which needs Web Animations' `pseudoElement` option; ClockMods
 * owns its markup, so the press layer is a real child node instead and works
 * without that.
 *
 * Ripples are attached lazily by delegation (see installRipples) rather than
 * per element, so buttons the pages build at runtime — calendar cells, settings
 * rows, navigation items — get one without any wiring at the call site.
 */

/** Growth of the press layer, md-ripple's PRESS_GROW_MS. */
const PRESS_GROW_MS = 450;
/** A tap shorter than this still shows a full press, md-ripple's MINIMUM_PRESS_MS. */
const MINIMUM_PRESS_MS = 225;
const INITIAL_ORIGIN_SCALE = 0.2;
const PADDING = 10;
const SOFT_EDGE_MINIMUM_SIZE = 75;
const SOFT_EDGE_CONTAINER_RATIO = 0.35;
/** md.sys.motion.easing.standard. */
const EASING_STANDARD = 'cubic-bezier(0.2, 0, 0, 1)';

/** Elements that take a ripple. Every M3 surface this app builds is listed here. */
const RIPPLE_SELECTOR = [
  '.m3-button',
  '.m3-icon-button',
  '.m3-nav-item__indicator',
  '.m3-segment',
  '.m3-list-item',
  '.m3-interactive',
  '.m3-fab',
].join(',');

interface RippleState {
  press: HTMLElement;
  growth?: Animation;
  /** Tracked separately from pressedAt, which is legitimately 0 at load. */
  pressed: boolean;
  pressedAt: number;
}

const states = new WeakMap<HTMLElement, RippleState>();

/** Creates the ripple layer on first use and keeps it for later presses. */
function layerFor(host: HTMLElement): RippleState {
  const existing = states.get(host);
  if (existing) return existing;
  const surface = document.createElement('span');
  surface.className = 'm3-ripple';
  surface.setAttribute('aria-hidden', 'true');
  const press = document.createElement('span');
  press.className = 'm3-ripple__press';
  surface.appendChild(press);
  host.insertBefore(surface, host.firstChild);
  const state: RippleState = { press, pressed: false, pressedAt: 0 };
  states.set(host, state);
  return state;
}

/**
 * md-ripple's determineRippleSize + getTranslationCoordinates, which grow a
 * small circle at the pointer out to cover the host's far corner.
 */
function pressGeometry(host: HTMLElement, event: PointerEvent) {
  const rect = host.getBoundingClientRect();
  const { width, height } = rect;
  const maxDim = Math.max(height, width);
  const softEdgeSize = Math.max(SOFT_EDGE_CONTAINER_RATIO * maxDim, SOFT_EDGE_MINIMUM_SIZE);
  const initialSize = Math.max(1, Math.floor(maxDim * INITIAL_ORIGIN_SCALE));
  const maxRadius = Math.sqrt(width ** 2 + height ** 2) + PADDING;
  const scale = (maxRadius + softEdgeSize) / initialSize;

  const end = { x: (width - initialSize) / 2, y: (height - initialSize) / 2 };
  const start = {
    x: event.clientX - rect.left - initialSize / 2,
    y: event.clientY - rect.top - initialSize / 2,
  };
  return { initialSize, scale, start, end };
}

function startPress(host: HTMLElement, event: PointerEvent): void {
  const state = layerFor(host);
  state.growth?.cancel();
  const { initialSize, scale, start, end } = pressGeometry(host, event);
  const { press } = state;
  press.style.width = `${initialSize}px`;
  press.style.height = `${initialSize}px`;
  press.classList.add('is-pressed');
  state.pressed = true;
  state.pressedAt = performance.now();
  // Without Web Animations the press layer still tints and fades through CSS;
  // only the growth is lost.
  if (typeof press.animate !== 'function') return;
  state.growth = press.animate(
    {
      transform: [
        `translate(${start.x}px, ${start.y}px) scale(1)`,
        `translate(${end.x}px, ${end.y}px) scale(${scale})`,
      ],
    },
    { duration: PRESS_GROW_MS, easing: EASING_STANDARD, fill: 'forwards' }
  );
}

function endPress(host: HTMLElement): void {
  const state = states.get(host);
  if (!state || !state.pressed) return;
  const held = performance.now() - state.pressedAt;
  state.pressed = false;
  // A quick tap would otherwise flash: let the press read for MINIMUM_PRESS_MS.
  const wait = Math.max(0, MINIMUM_PRESS_MS - held);
  window.setTimeout(() => {
    state.press.classList.remove('is-pressed');
  }, wait);
}

/**
 * Wires ripples for the whole document. Presses are captured so a handler that
 * stops propagation (the page swipe recogniser does) still leaves the ripple
 * intact, and release is watched on the window so dragging off a button ends
 * the press the way md-ripple does.
 */
export function installRipples(root: Document | HTMLElement = document): void {
  const hostFor = (target: EventTarget | null): HTMLElement | null => {
    if (!(target instanceof Element)) return null;
    const host = target.closest<HTMLElement>(RIPPLE_SELECTOR);
    if (!host || host.hasAttribute('disabled') || host.classList.contains('is-disabled')) {
      return null;
    }
    return host;
  };

  let active: HTMLElement | null = null;

  root.addEventListener(
    'pointerdown',
    (event) => {
      const pointer = event as PointerEvent;
      // Secondary buttons do not press, matching md-ripple's shouldReactToEvent.
      if (pointer.button !== 0 && pointer.pointerType === 'mouse') return;
      const host = hostFor(pointer.target);
      if (!host) return;
      active = host;
      startPress(host, pointer);
    },
    { capture: true, passive: true }
  );

  const release = () => {
    if (!active) return;
    endPress(active);
    active = null;
  };
  window.addEventListener('pointerup', release, { capture: true, passive: true });
  window.addEventListener('pointercancel', release, { capture: true, passive: true });
  // A context menu cancels the press in md-ripple too.
  window.addEventListener('contextmenu', release, { capture: true, passive: true });
}
