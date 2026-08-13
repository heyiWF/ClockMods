/**
 * Optional network-calibrated time.
 *
 * Replaces com.clockmods.time.NetworkTimeProvider + SntpClient. A browser cannot
 * speak SNTP (no UDP), so the reference comes from the `Date` header of an HTTP
 * response, corrected by half the round trip. That lands within roughly a second
 * rather than the milliseconds NTP achieved — enough to correct a device whose
 * clock has drifted, which is what this setting is for.
 *
 * Like the Android provider, the sample is projected forward with a monotonic
 * clock (`performance.now()`), so the displayed time never jumps when the system
 * wall clock is changed, and reads never block.
 */
import { prefs } from './prefs';

/** A sample older than this is considered stale and unusable. */
const SAMPLE_MAX_AGE_MS = 24 * 60 * 60 * 1000;
/** Retry sooner after a failed sync attempt. */
const RETRY_INTERVAL_MS = 60 * 1000;
const REQUEST_TIMEOUT_MS = 10000;

class TimeSource {
  private enabled = false;
  private syncIntervalMs = 60 * 60 * 1000;
  private hasSample = false;
  /** Server time at the moment of the sample. */
  private sampleServerMs = 0;
  /** performance.now() when the sample was captured. */
  private sampleMonotonic = 0;
  private timer: number | null = null;
  private inFlight = false;

  /** Current time in milliseconds, from the network sample when available. */
  now(): number {
    if (!this.enabled || !this.hasSample) return Date.now();
    const elapsed = performance.now() - this.sampleMonotonic;
    if (elapsed > SAMPLE_MAX_AGE_MS) return Date.now();
    return this.sampleServerMs + elapsed;
  }

  /** Whether a usable network sample is currently driving `now()`. */
  isSynchronized(): boolean {
    return this.enabled && this.hasSample;
  }

  /** Applies the current preferences; triggers a sync when newly enabled. */
  configure(): void {
    const enabled = prefs.isUseNetworkTime();
    const intervalMs = Math.max(1, prefs.getSyncIntervalMinutes()) * 60 * 1000;
    const changed = enabled !== this.enabled || intervalMs !== this.syncIntervalMs;
    this.enabled = enabled;
    this.syncIntervalMs = intervalMs;
    if (!enabled) {
      this.stop();
      this.hasSample = false;
      return;
    }
    if (changed || this.timer === null) void this.sync();
  }

  stop(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  /**
   * The endpoint to read the `Date` header from: an explicit URL, else the
   * weather proxy (already same-origin-friendly), else this site itself.
   */
  private endpoint(): string {
    const configured = prefs.getTimeSourceUrl();
    if (configured) return configured;
    const proxy = prefs.getQWeatherProxy();
    if (proxy) return proxy;
    return `${location.origin}${location.pathname}`;
  }

  private async sync(): Promise<void> {
    if (!this.enabled || this.inFlight) return;
    this.inFlight = true;
    let succeeded = false;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    try {
      const before = performance.now();
      const response = await fetch(this.endpoint(), {
        method: 'HEAD',
        cache: 'no-store',
        signal: controller.signal,
      });
      const after = performance.now();
      const header = response.headers.get('date');
      if (header) {
        const serverMs = Date.parse(header);
        if (Number.isFinite(serverMs)) {
          const roundTrip = after - before;
          // The header is stamped somewhere inside the round trip; assume the
          // midpoint, which is the same correction SNTP applies.
          this.sampleServerMs = serverMs + roundTrip / 2;
          this.sampleMonotonic = after;
          this.hasSample = true;
          succeeded = true;
        }
      }
    } catch {
      // Offline or a server that hides the Date header: fall back to device time.
    } finally {
      clearTimeout(timeout);
      this.inFlight = false;
    }
    this.schedule(succeeded ? this.syncIntervalMs : RETRY_INTERVAL_MS);
  }

  private schedule(delay: number): void {
    this.stop();
    if (!this.enabled) return;
    this.timer = window.setTimeout(() => void this.sync(), delay);
  }
}

export const timeSource = new TimeSource();

/** Convenience for the clock tick: milliseconds until the next second boundary. */
export function millisUntilNextSecond(now: number): number {
  return 1000 - (now % 1000);
}
