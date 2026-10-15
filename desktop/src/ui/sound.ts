/**
 * Alert tones, synthesised with the Web Audio API.
 *
 * The Android build rang through RingtoneManager and a foreground service; a web
 * page has neither, so the tone is generated on the fly — no audio asset to ship
 * and nothing to fail offline. Audio may only start after a user gesture, which
 * is satisfied by the tap that armed the timer or alarm.
 */

let context: AudioContext | null = null;

function audioContext(): AudioContext | null {
  if (context) return context;
  const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!Ctor) return null;
  context = new Ctor();
  return context;
}

/** Unlocks audio playback; call from a user gesture. */
export function primeAudio(): void {
  const ctx = audioContext();
  if (ctx && ctx.state === 'suspended') void ctx.resume();
}

function beep(ctx: AudioContext, startAt: number, frequency: number, duration: number): void {
  const oscillator = ctx.createOscillator();
  const gain = ctx.createGain();
  oscillator.type = 'sine';
  oscillator.frequency.value = frequency;
  // Short attack and release so the tone never clicks.
  gain.gain.setValueAtTime(0, startAt);
  gain.gain.linearRampToValueAtTime(0.35, startAt + 0.02);
  gain.gain.setValueAtTime(0.35, startAt + duration - 0.05);
  gain.gain.linearRampToValueAtTime(0, startAt + duration);
  oscillator.connect(gain).connect(ctx.destination);
  oscillator.start(startAt);
  oscillator.stop(startAt + duration);
}

/** Two rising notes: a timer or pomodoro phase finished. */
export function playTimerTone(): void {
  const ctx = audioContext();
  if (!ctx) return;
  void ctx.resume();
  const now = ctx.currentTime;
  beep(ctx, now, 880, 0.18);
  beep(ctx, now + 0.24, 1174.66, 0.26);
}

/** A repeating pattern for the alarm; returns a stop function. */
export function startAlarmTone(): () => void {
  const ctx = audioContext();
  if (!ctx) return () => undefined;
  void ctx.resume();
  let stopped = false;
  const ring = () => {
    if (stopped) return;
    const now = ctx.currentTime;
    for (let index = 0; index < 4; index++) {
      beep(ctx, now + index * 0.32, index % 2 === 0 ? 987.77 : 739.99, 0.2);
    }
  };
  ring();
  const handle = window.setInterval(ring, 2000);
  return () => {
    stopped = true;
    clearInterval(handle);
  };
}

/** Short vibration pattern, when the device supports it. */
export function vibrate(pattern: number | number[]): void {
  try {
    navigator.vibrate?.(pattern);
  } catch {
    /* unsupported */
  }
}
