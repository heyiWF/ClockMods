import { invoke } from "@tauri-apps/api/core";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { isEnabled, enable, disable } from "@tauri-apps/plugin-autostart";
import { prefs } from "../core/prefs";
import { CLOCK_THEMES, themeName } from "../core/clock-themes";
import { refreshLanguage } from "../core/i18n";
import { toast } from "../ui/toast";
import type { Router } from "../app/router";
import { addWidget, editWidget, type WidgetConfig } from "./widgets";
import "./desktop.css";
export function desktopText(zh: string, en: string, traditional = zh): string {
  return prefs.getClockLanguage() === "en"
    ? en
    : prefs.getClockLanguage() === "zh-Hant"
      ? traditional
      : zh;
}
function action(label: string, fn: () => unknown): HTMLButtonElement {
  const b = document.createElement("button");
  b.type = "button";
  b.className = "m3-button m3-button--outlined";
  b.textContent = label;
  b.onclick = () => {
    Promise.resolve()
      .then(fn)
      .catch((e) => toast(String(e)));
  };
  return b;
}
export async function installDesktopShell(router: Router): Promise<void> {
  const win = getCurrentWindow();
  const button = action(desktopText("桌面", "Desktop", "桌面"), () =>
    openDesktopSettings(),
  );
  button.id = "desktop-settings";
  button.classList.add("desktop-settings-button");
  document.body.append(button);
  await win.setAlwaysOnTop(localStorage.getItem("desktop.top") === "true");
  window.addEventListener("keydown", (event) => {
    if (event.key === "F11") {
      event.preventDefault();
      void win.isFullscreen().then((full) => win.setFullscreen(!full));
    }
    if (event.key === "Escape")
      void win.isFullscreen().then((full) => {
        if (full) void win.setFullscreen(false);
      });
  });
  const info = await invoke<{ smoke: boolean }>("desktop_info");
  if (info.smoke) {
    void import("./smoke").then((module) => module.runSmoke(router));
    return;
  }
  if (localStorage.getItem("desktop.welcomed") !== "true") {
    const dialog = document.createElement("dialog");
    dialog.className = "desktop-dialog";
    const title = document.createElement("h2");
    title.textContent = "ClockMods Desktop";
    const label = document.createElement("label");
    label.textContent = "语言 / Language";
    const language = document.createElement("select");
    for (const [value, name] of [
      ["zh-Hans", "简体中文"],
      ["zh-Hant", "繁體中文"],
      ["en", "English"],
    ])
      language.add(new Option(name, value));
    language.value = prefs.getClockLanguage();
    label.append(language);
    const theme = document.createElement("select");
    for (const t of CLOCK_THEMES)
      theme.add(new Option(themeName(t, language.value), t.id));
    theme.value = prefs.getClockTheme();
    const start = action("开始 / Start", async () => {
      prefs.setClockLanguage(language.value);
      prefs.setClockTheme(theme.value);
      refreshLanguage();
      localStorage.setItem("desktop.welcomed", "true");
      dialog.close();
      location.reload();
    });
    const note = document.createElement("p");
    note.textContent =
      "关闭窗口后保留托盘提醒；从托盘退出后停止提醒。\nClosing keeps reminders in the tray. Quit stops reminders.";
    dialog.append(title, label, theme, note, start);
    dialog.onclose = () => dialog.remove();
    document.body.append(dialog);
    dialog.showModal();
  }
}
async function openDesktopSettings(): Promise<void> {
  const dialog = document.createElement("dialog");
  dialog.className = "desktop-dialog";
  const title = document.createElement("h2");
  title.textContent = desktopText("桌面设置", "Desktop settings", "桌面設定");
  dialog.append(title);
  const toggle = (
    text: string,
    value: boolean,
    fn: (checked: boolean) => Promise<unknown> | void,
  ) => {
    const label = document.createElement("label");
    const input = document.createElement("input");
    input.type = "checkbox";
    input.checked = value;
    input.onchange = () => {
      const desired = input.checked;
      input.disabled = true;
      Promise.resolve()
        .then(() => fn(desired))
        .catch((e) => {
          input.checked = !desired;
          toast(String(e));
        })
        .finally(() => (input.disabled = false));
    };
    label.append(input, document.createTextNode(text));
    dialog.append(label);
  };
  toggle(
    desktopText("窗口置顶", "Always on top", "視窗置頂"),
    localStorage.getItem("desktop.top") === "true",
    async (enabled) => {
      await getCurrentWindow().setAlwaysOnTop(enabled);
      localStorage.setItem("desktop.top", String(enabled));
    },
  );
  toggle(
    desktopText("显示时保持屏幕常亮", "Keep display awake", "保持螢幕常亮"),
    localStorage.getItem("desktop.keepAwake") !== "false",
    async (enabled) => {
      await invoke("keep_awake", { enabled });
      localStorage.setItem("desktop.keepAwake", String(enabled));
    },
  );
  toggle(
    desktopText("登录后启动", "Start at login", "登入後啟動"),
    await isEnabled(),
    (enabled) => (enabled ? enable() : disable()),
  );
  dialog.append(
    action(
      desktopText(
        "切换全屏（F11）",
        "Toggle fullscreen (F11)",
        "切換全螢幕（F11）",
      ),
      async () => {
        const w = getCurrentWindow();
        await w.setFullscreen(!(await w.isFullscreen()));
      },
    ),
  );
  const subtitle = document.createElement("h3");
  subtitle.textContent = desktopText(
    "桌面小组件",
    "Desktop widgets",
    "桌面小工具",
  );
  dialog.append(subtitle);
  for (const [kind, label] of [
    ["digital", desktopText("数字时钟", "Digital clock", "數位時鐘")],
    ["analog", desktopText("模拟时钟", "Analog clock", "類比時鐘")],
    ["weather", desktopText("时钟天气", "Clock & weather", "時鐘天氣")],
    ["calendar", desktopText("日期日历", "Date & calendar", "日期月曆")],
  ])
    dialog.append(action("+ " + label, () => addWidget(kind)));
  for (const widget of await invoke<WidgetConfig[]>("list_widgets"))
    dialog.append(
      action(widget.kind + " · " + widget.timeZone, () => editWidget(widget)),
    );
  const note = document.createElement("p");
  note.textContent = desktopText(
    "关闭主窗口后应用留在托盘。退出应用、注销或关机时不会响铃；从睡眠恢复后补发到期提醒。系统不支持振动或蜂窝信号时不显示这些信息。",
    "Closing the main window keeps the app in the tray. Quit, sign-out and shutdown stop alerts; overdue alerts are delivered on resume. Vibration and cellular signal require hardware support.",
    "關閉主視窗後保留托盤提醒；退出、登出或關機時不會響鈴，喚醒後補發到期提醒。",
  );
  dialog.append(note);
  dialog.append(
    action(
      desktopText(
        "退出并停止提醒",
        "Quit and stop reminders",
        "退出並停止提醒",
      ),
      () => invoke("quit_application"),
    ),
  );
  dialog.append(
    action(desktopText("完成", "Done", "完成"), () => dialog.close()),
  );
  dialog.onclose = () => dialog.remove();
  document.body.append(dialog);
  dialog.showModal();
}
