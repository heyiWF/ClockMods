mod network;
mod scheduler;
use chrono::Local;
use scheduler::{next_daily, write_json, Alert, DailyAlarm, Scheduler};
use serde::{Deserialize, Serialize};
use std::{collections::BTreeMap, sync::Mutex, time::Duration};
use tauri::{
    menu::{Menu, MenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Emitter, Manager, State, WebviewUrl, WebviewWindowBuilder,
};
use tauri_plugin_notification::NotificationExt;

struct Runtime {
    scheduler: Mutex<Scheduler>,
    widgets: Mutex<BTreeMap<String, Widget>>,
    awake: Mutex<Option<keepawake::KeepAwake>>,
}
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct Widget {
    id: String,
    kind: String,
    theme: String,
    time_zone: String,
    use24_hour: bool,
    show_seconds: bool,
    opacity: f64,
    scale: f64,
}
impl Widget {
    fn validate(&self) -> Result<(), String> {
        if !self.id.starts_with("widget-")
            || self.id.len() > 32
            || !self
                .id
                .chars()
                .all(|c| c.is_ascii_alphanumeric() || c == '-')
        {
            return Err("Invalid widget identifier".into());
        }
        if !["digital", "analog", "weather", "calendar"].contains(&self.kind.as_str())
            || ![
                "dynamic",
                "glass",
                "instrument",
                "paper",
                "neon",
                "transparent",
            ]
            .contains(&self.theme.as_str())
            || !(0.15..=1.0).contains(&self.opacity)
            || !(0.5..=2.0).contains(&self.scale)
            || self.time_zone.len() > 100
        {
            return Err("Invalid widget settings".into());
        }
        Ok(())
    }
}
fn reveal(app: &tauri::AppHandle) {
    if let Some(w) = app.get_webview_window("main") {
        let _ = w.show();
        let _ = w.unminimize();
        let _ = w.set_focus();
    }
}
#[tauri::command]
fn schedule_timer(
    state: State<Runtime>,
    kind: String,
    deadline: Option<i64>,
) -> Result<(), String> {
    state
        .scheduler
        .lock()
        .map_err(|e| e.to_string())?
        .update(|s| s.timer(&kind, deadline, Local::now().timestamp_millis()))
}
#[tauri::command]
fn schedule_alarm(
    state: State<Runtime>,
    hour: u32,
    minute: u32,
    enabled: bool,
) -> Result<(), String> {
    let alarm = if enabled {
        Some(DailyAlarm {
            hour,
            minute,
            next: next_daily(&Local, Local::now(), hour, minute)?,
        })
    } else {
        None
    };
    state
        .scheduler
        .lock()
        .map_err(|e| e.to_string())?
        .update(|s| {
            if s.alarm.as_ref().map(|a| (a.hour, a.minute))
                != alarm.as_ref().map(|a| (a.hour, a.minute))
            {
                s.alarm = alarm;
            }
            Ok(())
        })
}
#[tauri::command]
fn pending_alerts(state: State<Runtime>) -> Result<Vec<Alert>, String> {
    Ok(state
        .scheduler
        .lock()
        .map_err(|e| e.to_string())?
        .schedule
        .pending
        .clone())
}
#[tauri::command]
fn acknowledge_alert(state: State<Runtime>, id: String) -> Result<(), String> {
    state
        .scheduler
        .lock()
        .map_err(|e| e.to_string())?
        .update(|s| {
            s.acknowledge(&id);
            Ok(())
        })
}
#[tauri::command]
fn keep_awake(state: State<Runtime>, enabled: bool) -> Result<(), String> {
    let mut awake = state.awake.lock().map_err(|e| e.to_string())?;
    if enabled && awake.is_none() {
        *awake = Some(
            keepawake::Builder::default()
                .display(true)
                .idle(true)
                .sleep(false)
                .reason("ClockMods display")
                .app_name("ClockMods")
                .create()
                .map_err(|e| e.to_string())?,
        );
    } else if !enabled {
        *awake = None;
    }
    Ok(())
}
#[tauri::command]
fn list_widgets(state: State<Runtime>) -> Result<Vec<Widget>, String> {
    Ok(state
        .widgets
        .lock()
        .map_err(|e| e.to_string())?
        .values()
        .cloned()
        .collect())
}
fn open_widget(app: &tauri::AppHandle, widget: &Widget) -> Result<(), String> {
    if let Some(w) = app.get_webview_window(&widget.id) {
        let _ = w.show();
        return Ok(());
    }
    WebviewWindowBuilder::new(
        app,
        &widget.id,
        WebviewUrl::App(format!("index.html?widget={}", widget.id).into()),
    )
    .title("ClockMods Widget")
    .inner_size(360., 280.)
    .min_inner_size(220., 180.)
    .decorations(false)
    .transparent(true)
    .always_on_top(true)
    .skip_taskbar(true)
    .build()
    .map_err(|e| e.to_string())?;
    Ok(())
}
#[tauri::command]
fn save_widget(app: tauri::AppHandle, state: State<Runtime>, widget: Widget) -> Result<(), String> {
    widget.validate()?;
    let mut widgets = state.widgets.lock().map_err(|e| e.to_string())?;
    if widgets.len() >= 16 && !widgets.contains_key(&widget.id) {
        return Err("At most 16 widgets".into());
    }
    let mut next = widgets.clone();
    next.insert(widget.id.clone(), widget.clone());
    write_json(
        &app.path()
            .app_data_dir()
            .map_err(|e| e.to_string())?
            .join("widgets.json"),
        &next,
    )?;
    *widgets = next;
    drop(widgets);
    open_widget(&app, &widget)?;
    app.emit("widgets-changed", ()).map_err(|e| e.to_string())
}
#[tauri::command]
fn remove_widget(app: tauri::AppHandle, state: State<Runtime>, id: String) -> Result<(), String> {
    let mut widgets = state.widgets.lock().map_err(|e| e.to_string())?;
    let mut next = widgets.clone();
    next.remove(&id);
    write_json(
        &app.path()
            .app_data_dir()
            .map_err(|e| e.to_string())?
            .join("widgets.json"),
        &next,
    )?;
    *widgets = next;
    drop(widgets);
    if id.starts_with("widget-") {
        if let Some(w) = app.get_webview_window(&id) {
            w.close().map_err(|e| e.to_string())?;
        }
    }
    Ok(())
}
#[tauri::command]
fn quit_application(app: tauri::AppHandle) {
    app.exit(0);
}
#[tauri::command]
fn desktop_info() -> serde_json::Value {
    serde_json::json!({"os":std::env::consts::OS,"arch":std::env::consts::ARCH,"smoke":std::env::var_os("CLOCKMODS_SMOKE").is_some()})
}
#[tauri::command]
fn smoke_result(app: tauri::AppHandle, result: serde_json::Value) {
    if std::env::var_os("CLOCKMODS_SMOKE").is_some() {
        println!("CLOCKMODS_SMOKE_RESULT={result}");
        app.exit(if result["ok"] == true { 0 } else { 1 });
    }
}

pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _, _| reveal(app)))
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_window_state::Builder::default().build())
        .plugin(tauri_plugin_autostart::Builder::new().build())
        .invoke_handler(tauri::generate_handler![
            schedule_timer,
            schedule_alarm,
            pending_alerts,
            acknowledge_alert,
            keep_awake,
            list_widgets,
            save_widget,
            remove_widget,
            desktop_info,
            quit_application,
            smoke_result,
            network::desktop_http
        ])
        .setup(|app| {
            let data = app.path().app_data_dir()?;
            let scheduler =
                Scheduler::open(data.join("reminders.json")).map_err(std::io::Error::other)?;
            let widgets: BTreeMap<String, Widget> = match std::fs::read(data.join("widgets.json")) {
                Ok(bytes) => serde_json::from_slice(&bytes)?,
                Err(e) if e.kind() == std::io::ErrorKind::NotFound => BTreeMap::new(),
                Err(e) => return Err(e.into()),
            };
            for widget in widgets.values() {
                widget.validate().map_err(std::io::Error::other)?;
            }
            app.manage(Runtime {
                scheduler: Mutex::new(scheduler),
                widgets: Mutex::new(widgets.clone()),
                awake: Mutex::new(None),
            });
            let show = MenuItem::with_id(app, "show", "Open ClockMods / 打开", true, None::<&str>)?;
            let quit = MenuItem::with_id(
                app,
                "quit",
                "Quit (stop reminders) / 退出",
                true,
                None::<&str>,
            )?;
            let menu = Menu::with_items(app, &[&show, &quit])?;
            // A missing tray must not leave an invisible process with no way back.
            let tray = TrayIconBuilder::new()
                .icon(app.default_window_icon().unwrap().clone())
                .tooltip("ClockMods")
                .menu(&menu)
                .show_menu_on_left_click(false)
                .on_menu_event(|app, event| match event.id.as_ref() {
                    "show" => reveal(app),
                    "quit" => app.exit(0),
                    _ => {}
                })
                .on_tray_icon_event(|tray, event| {
                    if matches!(
                        event,
                        TrayIconEvent::Click {
                            button: MouseButton::Left,
                            button_state: MouseButtonState::Up,
                            ..
                        }
                    ) {
                        reveal(tray.app_handle());
                    }
                })
                .build(app);
            let has_tray = tray.is_ok();
            if let Some(main) = app.get_webview_window("main") {
                let window = main.clone();
                main.on_window_event(move |event| {
                    if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                        api.prevent_close();
                        if has_tray {
                            let _ = window.hide();
                        } else {
                            let _ = window.minimize();
                        }
                    }
                });
            }
            for widget in widgets.values() {
                open_widget(app.handle(), widget).map_err(std::io::Error::other)?;
            }
            let handle = app.handle().clone();
            std::thread::spawn(move || loop {
                std::thread::sleep(Duration::from_millis(250));
                let state = handle.state::<Runtime>();
                let due = {
                    let mut store = match state.scheduler.lock() {
                        Ok(s) => s,
                        Err(_) => return,
                    };
                    let now = Local::now();
                    let mut next = store.schedule.clone();
                    let due = next.due(now);
                    if due.is_empty() {
                        continue;
                    }
                    match store.update(|s| {
                        *s = next;
                        Ok(due)
                    }) {
                        Ok(d) => d,
                        Err(e) => {
                            eprintln!("Reminder persistence failed: {e}");
                            continue;
                        }
                    }
                };
                for alert in due {
                    let title = match alert.kind.as_str() {
                        "alarm" => "ClockMods · Alarm / 闹钟",
                        "pomodoro" => "ClockMods · Pomodoro / 番茄钟",
                        _ => "ClockMods · Countdown / 倒计时",
                    };
                    let _ = handle
                        .notification()
                        .builder()
                        .title(title)
                        .body("Time is up · 时间到了")
                        .show();
                    reveal(&handle);
                    let _ = handle.emit("desktop-alert", &alert);
                }
            });
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("ClockMods desktop runtime failed");
}
