import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { toast } from "../ui/toast";
import { prefs } from "../core/prefs";
export const isDesktop = (): boolean =>
  typeof window !== "undefined" && "__TAURI_INTERNALS__" in window;
export interface NativeAlert {
  id: string;
  kind: "alarm" | "countdown" | "pomodoro";
  deadline: number;
}
let queue: Promise<unknown> = Promise.resolve();
/** Preserve start/pause/reset ordering even if IPC calls resolve out of order. */
export function nativeUpdate(
  command: string,
  args: Record<string, unknown>,
): void {
  if (!isDesktop()) return;
  queue = queue
    .catch(() => {})
    .then(() => invoke(command, args))
    .catch((error) => {
      const message =
        command === "keep_awake"
          ? prefs.isClockUseEnglish()
            ? "This desktop session cannot keep the display awake."
            : "当前桌面环境不支持保持屏幕常亮。"
          : prefs.isClockUseEnglish()
            ? "The reminder could not be saved. Check that the app data folder is writable."
            : "提醒未能保存，请检查应用数据目录是否可写。";
      console.error("Desktop command failed", command, error);
      toast(message);
    });
}
export function syncTimer(kind: string, deadline: number | null): void {
  if (deadline !== null && deadline <= Date.now()) return;
  nativeUpdate("schedule_timer", { kind, deadline });
}
export function syncAlarm(
  hour: number,
  minute: number,
  enabled: boolean,
): void {
  nativeUpdate("schedule_alarm", { hour, minute, enabled });
}
export async function installNativeAlerts(): Promise<() => Promise<void>> {
  if (!isDesktop()) return async () => {};
  const delivered = new Set<string>();
  const deliver = async (alert: NativeAlert) => {
    if (delivered.has(alert.id)) return;
    window.dispatchEvent(
      new CustomEvent<NativeAlert>("clockmods-alert", { detail: alert }),
    );
    delivered.add(alert.id);
    await invoke("acknowledge_alert", { id: alert.id });
  };
  await listen<NativeAlert>("desktop-alert", (event) => {
    void deliver(event.payload).catch((e) => toast(String(e)));
  });
  return async () => {
    for (const alert of await invoke<NativeAlert[]>("pending_alerts"))
      await deliver(alert);
  };
}
export async function desktopFetch(
  url: string,
  token: string | null = null,
  head = false,
): Promise<Response> {
  const reply = await invoke<{
    status: number;
    body: string;
    date: string | null;
  }>("desktop_http", { url, token, head });
  return new Response(head ? null : reply.body, {
    status: reply.status,
    headers: reply.date ? { date: reply.date } : {},
  });
}
