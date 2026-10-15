import { invoke } from "@tauri-apps/api/core";
import { listen, emit } from "@tauri-apps/api/event";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { prefs } from "../core/prefs";
import { refreshLanguage, intlLocale } from "../core/i18n";
import { almanacOf } from "../lunar/lunar";
import { WeatherController } from "../weather/controller";
import { loadWeatherIcons } from "../weather/icons";
import { desktopText } from "./shell";
import { toast } from "../ui/toast";
import "./desktop.css";
export interface WidgetConfig {
  id: string;
  kind: string;
  theme: string;
  timeZone: string;
  use24Hour: boolean;
  showSeconds: boolean;
  opacity: number;
  scale: number;
}
export const WIDGET_THEMES = [
  "dynamic",
  "glass",
  "instrument",
  "paper",
  "neon",
  "transparent",
] as const;
export function validWidget(config: WidgetConfig): boolean {
  try {
    new Intl.DateTimeFormat("en", { timeZone: config.timeZone });
  } catch {
    return false;
  }
  return (
    /^widget-[a-zA-Z0-9-]{1,25}$/.test(config.id) &&
    ["digital", "analog", "weather", "calendar"].includes(config.kind) &&
    WIDGET_THEMES.some((t) => t === config.theme) &&
    Number.isFinite(config.opacity) &&
    config.opacity >= 0.15 &&
    config.opacity <= 1 &&
    Number.isFinite(config.scale) &&
    config.scale >= 0.5 &&
    config.scale <= 2
  );
}
export async function addWidget(kind: string): Promise<void> {
  const config: WidgetConfig = {
    id: "widget-" + crypto.randomUUID().slice(0, 18),
    kind,
    theme: "dynamic",
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    use24Hour: true,
    showSeconds: true,
    opacity: 0.95,
    scale: 1,
  };
  await invoke("save_widget", { widget: config });
}
export async function editWidget(config: WidgetConfig): Promise<void> {
  const dialog = document.createElement("dialog");
  dialog.className = "desktop-dialog";
  const title = document.createElement("h2");
  title.textContent = desktopText(
    "小组件设置",
    "Widget settings",
    "小工具設定",
  );
  dialog.append(title);
  const field = (name: string, element: HTMLElement) => {
    const l = document.createElement("label");
    l.append(document.createTextNode(name), element);
    dialog.append(l);
  };
  const themes = document.createElement("select");
  for (const t of WIDGET_THEMES) themes.add(new Option(t, t));
  themes.value = config.theme;
  field(desktopText("外观", "Appearance", "外觀"), themes);
  const zone = document.createElement("input");
  zone.value = config.timeZone;
  zone.setAttribute("list", "widget-timezones");
  const zones = document.createElement("datalist");
  zones.id = "widget-timezones";
  for (const z of Intl.supportedValuesOf("timeZone"))
    zones.append(new Option(z, z));
  dialog.append(zones);
  field(desktopText("时区", "Time zone", "時區"), zone);
  const check = (name: string, value: boolean) => {
    const e = document.createElement("input");
    e.type = "checkbox";
    e.checked = value;
    field(name, e);
    return e;
  };
  const use24 = check(
    desktopText("24 小时制", "24-hour time", "24 小時制"),
    config.use24Hour,
  );
  const seconds = check(
    desktopText("显示秒", "Show seconds", "顯示秒"),
    config.showSeconds,
  );
  const range = (name: string, value: number, min: string, max: string) => {
    const e = document.createElement("input");
    e.type = "range";
    e.min = min;
    e.max = max;
    e.step = ".05";
    e.value = String(value);
    field(name, e);
    return e;
  };
  const opacity = range(
    desktopText("背景不透明度", "Background opacity", "背景不透明度"),
    config.opacity,
    ".15",
    "1",
  );
  const scale = range(
    desktopText("文字比例", "Text scale", "文字比例"),
    config.scale,
    ".5",
    "2",
  );
  const button = (name: string, fn: () => Promise<unknown> | void) => {
    const b = document.createElement("button");
    b.type = "button";
    b.className = "m3-button m3-button--outlined";
    b.textContent = name;
    b.onclick = () => {
      Promise.resolve()
        .then(fn)
        .catch((e) => toast(String(e)));
    };
    dialog.append(b);
  };
  button(desktopText("保存", "Save", "儲存"), async () => {
    const updated = {
      ...config,
      theme: themes.value,
      timeZone: zone.value,
      opacity: Number(opacity.value),
      scale: Number(scale.value),
      use24Hour: use24.checked,
      showSeconds: seconds.checked,
    };
    if (!validWidget(updated))
      throw Error(
        desktopText(
          "请输入有效时区",
          "Enter a valid time zone",
          "請輸入有效時區",
        ),
      );
    await invoke("save_widget", { widget: updated });
    dialog.close();
  });
  button(desktopText("取消", "Cancel", "取消"), () => dialog.close());
  dialog.onclose = () => dialog.remove();
  document.body.append(dialog);
  dialog.showModal();
}
export async function bootWidget(): Promise<void> {
  refreshLanguage();
  const id = new URLSearchParams(location.search).get("widget");
  let config = (await invoke<WidgetConfig[]>("list_widgets")).find(
    (w) => w.id === id,
  );
  if (!config || !validWidget(config)) throw Error("Invalid widget");
  document.documentElement.classList.add("widget-document");
  document.body.replaceChildren();
  const card = document.createElement("main");
  card.className = "desktop-widget";
  const drag = document.createElement("header");
  drag.textContent = "ClockMods";
  drag.onpointerdown = (e) => {
    if (e.button === 0) void getCurrentWindow().startDragging();
  };
  const controls = document.createElement("div");
  controls.className = "widget-controls";
  const settings = document.createElement("button");
  settings.textContent = "⚙";
  settings.ariaLabel = desktopText("设置", "Settings", "設定");
  settings.onclick = () => void editWidget(config!);
  const close = document.createElement("button");
  close.textContent = "×";
  close.ariaLabel = desktopText("删除小组件", "Remove widget", "移除小工具");
  close.onclick = () => void invoke("remove_widget", { id });
  controls.append(settings, close);
  const body = document.createElement("div");
  body.className = "widget-body";
  const weather = document.createElement("div");
  weather.className = "widget-weather";
  card.append(drag, controls, body, weather);
  document.body.append(card);
  const render = () => {
    if (!config) return;
    card.dataset.theme = config.theme;
    card.style.setProperty("--widget-opacity", String(config.opacity));
    card.style.setProperty("--widget-scale", String(config.scale));
    const now = new Date();
    const parts = new Intl.DateTimeFormat("en-GB", {
      timeZone: config.timeZone,
      year: "numeric",
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    }).formatToParts(now);
    const n = (key: string) => Number(parts.find((p) => p.type === key)!.value);
    const time = new Intl.DateTimeFormat(intlLocale(), {
      timeZone: config.timeZone,
      hour: "2-digit",
      minute: "2-digit",
      second: config.showSeconds ? "2-digit" : undefined,
      hour12: !config.use24Hour,
    }).format(now);
    body.replaceChildren();
    const main = document.createElement("div");
    main.className = "widget-time";
    if (config.kind === "analog") {
      const canvas = document.createElement("canvas");
      canvas.width = 400;
      canvas.height = 400;
      canvas.style.width = "min(100%, 180px)";
      canvas.ariaLabel = time;
      main.append(canvas);
      const ctx = canvas.getContext("2d")!;
      const color = getComputedStyle(card).color;
      ctx.strokeStyle = color;
      ctx.fillStyle = color;
      ctx.translate(200, 200);
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(0, 0, 180, 0, Math.PI * 2);
      ctx.stroke();
      for (let i = 0; i < 12; i++) {
        ctx.save();
        ctx.rotate((i * Math.PI) / 6);
        ctx.fillRect(-2, -165, 4, 15);
        ctx.restore();
      }
      for (const [angle, length, width] of [
        [(((n("hour") % 12) + n("minute") / 60) * Math.PI) / 6, 90, 8],
        [(n("minute") * Math.PI) / 30, 135, 5],
        ...(config.showSeconds ? [[(n("second") * Math.PI) / 30, 150, 2]] : []),
      ]) {
        ctx.save();
        ctx.rotate(angle);
        ctx.lineWidth = width;
        ctx.beginPath();
        ctx.moveTo(0, 12);
        ctx.lineTo(0, -length);
        ctx.stroke();
        ctx.restore();
      }
    } else
      main.textContent = config.kind === "calendar" ? String(n("day")) : time;
    const date = document.createElement("div");
    date.className = "widget-date";
    date.textContent = new Intl.DateTimeFormat(intlLocale(), {
      timeZone: config.timeZone,
      dateStyle: "full",
    }).format(now);
    const lunar = document.createElement("div");
    const almanac = almanacOf(n("year"), n("month") - 1, n("day"));
    lunar.textContent = [almanac.natural, ...almanac.festivals].join(" · ");
    const tz = document.createElement("small");
    tz.textContent = config.timeZone;
    body.append(main, date, lunar, tz);
  };
  render();
  await emit("widget-ready", { id });
  const timer = setInterval(render, 1000);
  window.addEventListener("beforeunload", () => clearInterval(timer));
  await listen("widgets-changed", async () => {
    const next = (await invoke<WidgetConfig[]>("list_widgets")).find(
      (w) => w.id === id,
    );
    if (next && validWidget(next)) {
      config = next;
      render();
    }
  });
  if (config.kind === "weather") {
    await loadWeatherIcons();
    const controller = new WeatherController((state) => {
      weather.replaceChildren();
      if (state.data) {
        const value = document.createElement("span");
        value.textContent = `${state.data.city} ${state.data.temperature}℃ ${state.data.text}`;
        const updated = document.createElement("small");
        updated.textContent = new Date(state.data.updatedAt).toLocaleString(
          intlLocale(),
        );
        weather.append(value, updated);
      } else
        weather.textContent =
          state.message ??
          desktopText(
            "请先在主窗口配置天气",
            "Configure weather in the main window",
            "請先設定天氣",
          );
    }, true);
    const refresh = document.createElement("button");
    refresh.textContent = desktopText(
      "刷新天气",
      "Refresh weather",
      "重新整理天氣",
    );
    refresh.onclick = () => controller.refreshNow();
    card.append(refresh);
    controller.start(prefs.getWeatherIntervalMinutes());
    window.addEventListener("beforeunload", () => controller.stop());
    const attribution = document.createElement("small");
    attribution.textContent = "Weather · QWeather";
    card.append(attribution);
  }
}
