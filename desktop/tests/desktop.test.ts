// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest";
const ipc = vi.hoisted(() => ({
  invoke: vi.fn().mockResolvedValue(undefined),
}));
vi.mock("@tauri-apps/api/core", () => ipc);
vi.mock("@tauri-apps/api/event", () => ({
  listen: vi.fn().mockResolvedValue(() => {}),
  emit: vi.fn(),
}));
import { nativeUpdate, desktopFetch } from "../src/desktop/bridge";
import {
  validWidget,
  WIDGET_THEMES,
  type WidgetConfig,
} from "../src/desktop/widgets";
import { TimerPage } from "../src/pages/timer";
const widget: WidgetConfig = {
  id: "widget-test",
  kind: "digital",
  theme: "paper",
  timeZone: "Asia/Shanghai",
  use24Hour: true,
  showSeconds: true,
  opacity: 0.9,
  scale: 1,
};
beforeEach(() => {
  localStorage.clear();
  ipc.invoke.mockReset().mockResolvedValue(undefined);
  Object.defineProperty(window, "__TAURI_INTERNALS__", {
    value: {},
    configurable: true,
  });
  document.body.innerHTML = '<div id="toast-root"></div><div id="timer"></div>';
});
describe("desktop integration", () => {
  it("validates independent widget settings, including all six styles", () => {
    for (const theme of WIDGET_THEMES)
      expect(validWidget({ ...widget, theme })).toBe(true);
    for (const change of [
      { timeZone: "bad/zone" },
      { opacity: NaN },
      { scale: 3 },
      { id: "../escape" },
      { kind: "iframe" },
      { theme: "remote" },
    ])
      expect(validWidget({ ...widget, ...change })).toBe(false);
  });
  it("serializes native start and cancel operations", async () => {
    nativeUpdate("schedule_timer", {
      kind: "countdown",
      deadline: Date.now() + 10000,
    });
    nativeUpdate("schedule_timer", { kind: "countdown", deadline: null });
    await vi.waitFor(() => expect(ipc.invoke).toHaveBeenCalledTimes(2));
    expect(ipc.invoke.mock.calls.map((c) => c[1].deadline)).toEqual([
      expect.any(Number),
      null,
    ]);
  });
  it("uses the bounded native HTTPS bridge for the desktop response", async () => {
    ipc.invoke.mockResolvedValue({
      status: 200,
      body: '{"code":"200"}',
      date: "Thu, 15 Oct 2026 00:00:00 GMT",
    });
    const response = await desktopFetch(
      "https://devapi.qweather.com/v7/weather/now",
      "jwt",
    );
    expect(await response.json()).toEqual({ code: "200" });
    expect(ipc.invoke).toHaveBeenCalledWith("desktop_http", {
      url: "https://devapi.qweather.com/v7/weather/now",
      token: "jwt",
      head: false,
    });
  });
  it("completes a countdown on another page from a native event", async () => {
    const deadline = Date.now() - 1000;
    localStorage.setItem("pro_timers.countdown_running", "true");
    localStorage.setItem("pro_timers.countdown_deadline", String(deadline));
    localStorage.setItem("pro_timers.countdown_remaining", "1000");
    const page = new TimerPage(document.getElementById("timer")!, "countdown");
    page.stop();
    window.dispatchEvent(
      new CustomEvent("clockmods-alert", {
        detail: { id: "countdown-" + deadline, kind: "countdown", deadline },
      }),
    );
    expect(localStorage.getItem("pro_timers.countdown_running")).toBe("false");
    expect(document.querySelector(".timer-display")?.textContent).toBe(
      "00:00:00",
    );
  });
});
