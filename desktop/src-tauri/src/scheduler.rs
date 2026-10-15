//! The native process owns deadlines, so navigating, hiding or throttling the
//! WebView cannot stop an alert. Fired alerts remain pending until acknowledged.
use chrono::{DateTime, Datelike, Duration, Local, LocalResult, NaiveDate, TimeZone};
use serde::{Deserialize, Serialize};
use std::{
    collections::BTreeMap,
    fs,
    path::{Path, PathBuf},
};

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct Alert {
    pub id: String,
    pub kind: String,
    pub deadline: i64,
}
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct DailyAlarm {
    pub hour: u32,
    pub minute: u32,
    pub next: i64,
}
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct Schedule {
    pub timers: BTreeMap<String, i64>,
    pub alarm: Option<DailyAlarm>,
    pub pending: Vec<Alert>,
}
pub fn next_daily<T: TimeZone>(
    tz: &T,
    now: DateTime<T>,
    hour: u32,
    minute: u32,
) -> Result<i64, String> {
    if hour > 23 || minute > 59 {
        return Err("Invalid alarm time".into());
    }
    let today =
        NaiveDate::from_ymd_opt(now.year(), now.month(), now.day()).ok_or("Invalid date")?;
    for day in 0..3 {
        let date = today + Duration::days(day);
        // During a spring-forward gap, ring at the first valid minute after the
        // requested time; during a fold choose the first future occurrence.
        for offset in 0..180 {
            let local = date.and_hms_opt(hour, minute, 0).unwrap() + Duration::minutes(offset);
            let candidates = match tz.from_local_datetime(&local) {
                LocalResult::Single(t) => vec![t],
                LocalResult::Ambiguous(a, b) => vec![a, b],
                LocalResult::None => continue,
            };
            if let Some(t) = candidates
                .into_iter()
                .min_by_key(|t| t.timestamp_millis())
                .filter(|t| t.timestamp_millis() > now.timestamp_millis())
            {
                return Ok(t.timestamp_millis());
            }
            break;
        }
    }
    Err("Cannot resolve next alarm time".into())
}
impl Schedule {
    pub fn timer(&mut self, kind: &str, deadline: Option<i64>, now: i64) -> Result<(), String> {
        if !matches!(kind, "countdown" | "pomodoro") {
            return Err("Unknown timer".into());
        }
        if let Some(deadline) = deadline {
            if deadline <= now || deadline - now > 100 * 60 * 60 * 1000 {
                return Err("Timer must be in the next 100 hours".into());
            }
            self.timers.insert(kind.into(), deadline);
        } else {
            self.timers.remove(kind);
        }
        Ok(())
    }
    pub fn due(&mut self, now: DateTime<Local>) -> Vec<Alert> {
        let timestamp = now.timestamp_millis();
        let mut due: Vec<Alert> = self
            .timers
            .iter()
            .filter(|(_, d)| **d <= timestamp)
            .map(|(kind, d)| Alert {
                id: format!("{kind}-{d}"),
                kind: kind.clone(),
                deadline: *d,
            })
            .collect();
        for a in &due {
            self.timers.remove(&a.kind);
        }
        if let Some(alarm) = self.alarm.as_mut() {
            if alarm.next <= timestamp {
                due.push(Alert {
                    id: format!("alarm-{}", alarm.next),
                    kind: "alarm".into(),
                    deadline: alarm.next,
                });
                alarm.next = next_daily(&Local, now, alarm.hour, alarm.minute)
                    .expect("validated alarm time");
            }
        }
        self.pending.extend(due.clone());
        if self.pending.len() > 64 {
            self.pending.drain(..self.pending.len() - 64);
        }
        due
    }
    pub fn acknowledge(&mut self, id: &str) {
        self.pending.retain(|a| a.id != id);
    }
}
pub struct Scheduler {
    pub schedule: Schedule,
    path: PathBuf,
}
impl Scheduler {
    pub fn open(path: PathBuf) -> Result<Self, String> {
        let schedule: Schedule = match fs::read(&path) {
            Ok(bytes) => serde_json::from_slice(&bytes)
                .map_err(|_| "Cannot read saved reminders; original file retained".to_string())?,
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Schedule::default(),
            Err(e) => return Err(e.to_string()),
        };
        if schedule
            .alarm
            .as_ref()
            .is_some_and(|a| a.hour > 23 || a.minute > 59)
            || schedule
                .timers
                .keys()
                .any(|kind| !matches!(kind.as_str(), "countdown" | "pomodoro"))
            || schedule.pending.len() > 64
        {
            return Err("Invalid saved reminders; original file retained".into());
        }
        Ok(Self { schedule, path })
    }
    pub fn update<T>(
        &mut self,
        change: impl FnOnce(&mut Schedule) -> Result<T, String>,
    ) -> Result<T, String> {
        let mut next = self.schedule.clone();
        let result = change(&mut next)?;
        write_json(&self.path, &next)?;
        self.schedule = next;
        Ok(result)
    }
}
pub fn write_json(path: &Path, value: &impl Serialize) -> Result<(), String> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }
    let temporary = path.with_extension("json.tmp");
    let bytes = serde_json::to_vec(value).map_err(|e| e.to_string())?;
    {
        use std::io::Write;
        let mut f = fs::File::create(&temporary).map_err(|e| e.to_string())?;
        f.write_all(&bytes)
            .and_then(|_| f.sync_all())
            .map_err(|e| e.to_string())?;
    }
    fs::rename(temporary, path).map_err(|e| e.to_string())
}
#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Timelike;
    #[test]
    fn hidden_timer_fires_once_and_survives_restart() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("reminders.json");
        let now = Local::now();
        let mut store = Scheduler::open(path.clone()).unwrap();
        store
            .update(|s| {
                s.timer(
                    "countdown",
                    Some(now.timestamp_millis() + 1000),
                    now.timestamp_millis(),
                )
            })
            .unwrap();
        let mut store = Scheduler::open(path.clone()).unwrap();
        let due = store
            .update(|s| Ok(s.due(now + Duration::seconds(10))))
            .unwrap();
        assert_eq!(due.len(), 1);
        assert!(store.schedule.due(now + Duration::hours(2)).is_empty());
        assert_eq!(Scheduler::open(path).unwrap().schedule.pending.len(), 1);
        store
            .update(|s| {
                s.acknowledge(&due[0].id);
                Ok(())
            })
            .unwrap();
        assert!(store.schedule.pending.is_empty());
    }
    #[test]
    fn cancellation_and_invalid_deadlines() {
        let mut s = Schedule::default();
        assert!(s.timer("unknown", Some(100), 0).is_err());
        assert!(s.timer("countdown", Some(0), 0).is_err());
        s.timer("countdown", Some(100), 0).unwrap();
        s.timer("countdown", None, 0).unwrap();
        assert!(s.timers.is_empty());
    }
    #[test]
    fn daily_alarm_rolls_over_year() {
        let now = chrono::Utc
            .with_ymd_and_hms(2026, 12, 31, 23, 59, 0)
            .unwrap();
        let next = next_daily(&chrono::Utc, now, 7, 30).unwrap();
        assert_eq!(
            chrono::Utc.timestamp_millis_opt(next).unwrap().to_rfc3339(),
            "2027-01-01T07:30:00+00:00"
        );
    }
    #[test]
    fn invalid_saved_alarm_is_rejected() {
        let d = tempfile::tempdir().unwrap();
        let p = d.path().join("state.json");
        let state = Schedule {
            alarm: Some(DailyAlarm {
                hour: 24,
                minute: 0,
                next: 1,
            }),
            ..Default::default()
        };
        fs::write(&p, serde_json::to_vec(&state).unwrap()).unwrap();
        assert!(Scheduler::open(p).is_err());
    }
    #[test]
    fn corrupted_state_is_not_silently_reset() {
        let d = tempfile::tempdir().unwrap();
        let p = d.path().join("state.json");
        fs::write(&p, b"invalid").unwrap();
        assert!(Scheduler::open(p.clone()).is_err());
        assert_eq!(fs::read(p).unwrap(), b"invalid");
    }
    #[test]
    fn local_alarm_is_rearmed_after_suspend() {
        let now = Local::now();
        let mut s = Schedule {
            alarm: Some(DailyAlarm {
                hour: now.hour(),
                minute: now.minute(),
                next: now.timestamp_millis() - 86400000,
            }),
            ..Default::default()
        };
        assert_eq!(s.due(now).len(), 1);
        assert!(s.alarm.unwrap().next > now.timestamp_millis());
    }
}
