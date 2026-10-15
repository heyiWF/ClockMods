/** Invoked only when the native process explicitly opts in with CLOCKMODS_SMOKE.
 * Runs against the packaged WebView, its real IPC and native scheduler. */
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { prefs } from "../core/prefs";
import { CLOCK_THEMES } from "../core/clock-themes";
import { CALENDAR_THEMES } from "../core/calendar-themes";
import type { Router } from "../app/router";
import type { NativeAlert } from "./bridge";
import type { WidgetConfig } from "./widgets";
const frame = () =>
  new Promise<void>((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve())),
  );
function assert(value: unknown, message: string): asserts value {
  if (!value) throw Error(message);
}
function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((_, reject) =>
      setTimeout(() => reject(Error("Smoke test timed out")), ms),
    ),
  ]);
}
export async function runSmoke(router: Router): Promise<void> {
  const checks: string[] = [];
  try {
    router.navigate("clock");
    for (const theme of CLOCK_THEMES) {
      prefs.setClockTheme(theme.id);
      await router.refreshAll();
      await frame();
      const root = document.querySelector<HTMLElement>("[data-page=clock]")!;
      assert(
        root.getBoundingClientRect().width > 100,
        "Clock layout has no width",
      );
      assert(root.textContent?.trim(), "Clock has no text");
      if (theme.id !== "classic")
        assert(
          root.querySelector(".ultimate-face,.clock-face"),
          "Missing themed face",
        );
      checks.push(theme.id);
    }
    router.navigate("calendar");
    for (const theme of CALENDAR_THEMES) {
      prefs.setUltimateOptions({
        ...prefs.getUltimateOptions(),
        calendarTheme: theme.id,
      });
      await router.refreshAll();
      await frame();
      assert(
        document.querySelector('[data-calendar-theme="' + theme.id + '"]'),
        "Calendar theme missing",
      );
      assert(
        document.querySelectorAll("#cal-days > *").length >= 7,
        "Calendar did not render dates",
      );
      checks.push(theme.id);
    }
    for (const page of ["pomodoro", "alarm", "countdown", "stopwatch"]) {
      router.navigate(page);
      await frame();
      assert(
        document.querySelector("[data-page=" + page + "]")?.textContent?.trim(),
        "Empty tool",
      );
      checks.push(page);
    }
    const widget: WidgetConfig = {
      id: "widget-smoke",
      kind: "digital",
      theme: "paper",
      timeZone: "Asia/Shanghai",
      use24Hour: true,
      showSeconds: true,
      opacity: 0.9,
      scale: 1,
    };
    let ready!: () => void;
    const widgetReady = new Promise<void>((r) => (ready = r));
    const unlisten = await listen<{ id: string }>("widget-ready", (e) => {
      if (e.payload.id === widget.id) ready();
    });
    await invoke("save_widget", { widget });
    await withTimeout(widgetReady, 10000);
    unlisten();
    const stored = await invoke<WidgetConfig[]>("list_widgets");
    assert(
      stored.some((w) => w.id === widget.id && w.theme === "paper"),
      "Widget persistence failed",
    );
    await invoke("remove_widget", { id: widget.id });
    checks.push("widget-window");
    let fired!: (alert: NativeAlert) => void;
    const alert = new Promise<NativeAlert>((r) => (fired = r));
    const stop = await listen<NativeAlert>("desktop-alert", (e) => {
      if (e.payload.kind === "countdown") fired(e.payload);
    });
    await invoke("schedule_timer", {
      kind: "countdown",
      deadline: Date.now() + 1500,
    });
    await getCurrentWindow().hide();
    const result = await withTimeout(alert, 10000);
    stop();
    assert(result.kind === "countdown", "Native alarm not received");
    assert(
      await getCurrentWindow().isVisible(),
      "Native reminder did not restore the hidden window",
    );
    checks.push("hidden-window-native-reminder");
    await invoke("smoke_result", { result: { ok: true, checks } });
  } catch (error) {
    await invoke("smoke_result", {
      result: { ok: false, error: String(error), checks },
    });
  }
}
