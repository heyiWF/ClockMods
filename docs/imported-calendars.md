# Imported calendar files

Ultimate and Ultimate-compose expose **System & Language → Calendar files**. A bottom-right plus opens two animated pill actions: **Import from link** and **Import from file**. The pills keep fixed baselines and reveal from the right edge, without scaling their text or icons. Opening proceeds from the bottom upward (50 ms lead-in and stagger, 200 ms reveal); closing reverses the order (100 ms per item). The plus rotates into a close control over 200 ms, and the content and toolbar fade behind the menu. Back or a tap outside collapses it. Files are copied into private application storage; moving the original document does not break an import.

HTTPS and webcal links can enable **Automatic sync**. The original normalized HTTPS address is retained only in private application storage; it is never logged. A network-constrained WorkManager job checks enabled subscriptions every six hours (Android may defer execution), survives process death/reboots, and is cancelled when no subscriptions remain enabled. Source cards expose the switch, last successful sync, retry feedback and Sync now. Disabling sync preserves local events. A failed download or invalid file retains the previous valid snapshot. A successful refresh replaces the same source ID and invalidates cached months instead of adding another calendar. Concurrent removal or disabling rejects an already-running download. Manual schedules remain untouched.

UTF-8 iCalendar (`.ics`) imports support all-day dates, timed events, floating local times, UTC, IANA zones and embedded VTIMEZONE definitions. Timed occurrences are displayed in the app's selected time zone; floating events follow that zone. All-day boundaries remain civil dates. `DTEND` is exclusive; absent ends mean one day for all-day events and a point for timed events. Cross-midnight events appear on every overlapping date, with their full time range.

Hourly and longer RRULEs, RDATE (including PERIOD), EXDATE, exception rules, UID/SEQUENCE updates, moved RECURRENCE-ID instances and cancellations are expanded by biweekly. SECONDLY/MINUTELY rules and RECURRENCE-ID RANGE changes are rejected with an explanatory import message. Unsupported or malformed dates and unknown zones are not silently dropped. Reminder alarms, attachments and URLs embedded in events are not executed or fetched. Only the calendar source URL is fetched for an explicitly enabled subscription or a link import.

Limits are 8 MiB per source, 32 sources, 32 MiB retained total, 10,000 event definitions per source and 10,000 expanded occurrences per month. Expansion runs on a background worker into a bounded month cache; parsing does not run during calendar drawing. Complexity errors are surfaced in the source-file card. Identical files (including renamed copies) are rejected by SHA-256. Files with different contents are separate manual imports; subscriptions replace their existing source. The same subscription URL cannot be imported twice.

Dashboard calendar date labels append imported event titles to their lunar/festival carousel. Agenda's portrait strip and landscape rail do the same. When Chinese-calendar content is disabled, imported event labels remain available; dates without labels retain their compact centered selection. The poster calendar has no secondary per-day label slot. Agenda combines manual and imported events, sorts all-day events first and displays timed ranges. Tapping an imported row opens read-only details with location, description and source. Deleting a source removes its events and refreshes the visible calendar; the manual schedule store is never rewritten. Widgets retain existing behavior.

## Validation

Run `testUltimateDebugUnitTest`, `assembleUltimateDebug`, `assembleUltimateDebugAndroidTest`, and `lintUltimateDebug`. Set `CLOCKMODS_TEST_ICS` to a local private sample to additionally run the optional supplied-file parser test. The supplied `basic.ics` contains 317 all-day events over 2021–2031 and is not included in the repository.

For isolated Android acceptance, build with:

```text
-Pclockmods.auditApplicationId=com.clockmods.fontaudit
-Pclockmods.testRunner=com.clockmods.calendar.imports.CalendarImportAcceptance
```

Install the app and test APKs. Grant the isolated app notification permission so its first-launch system prompt does not cover the calendar, then run:

```text
adb -s emulator-5554 shell pm grant com.clockmods.fontaudit.ultimate android.permission.POST_NOTIFICATIONS
adb -s emulator-5554 shell am instrument -w com.clockmods.fontaudit.ultimate.test/com.clockmods.calendar.imports.CalendarImportAcceptance
```

Optionally place the private sample at the audit app's external-files `basic.ics` and pass `-e requirePrivateFixture true`. The runner exercises the actual manager document-result callback, duplicate feedback, delete cancellation/confirmation, persistence, subscription replacement and rollback, cancellation of stale downloads, all-day/range presentation, portrait/landscape date labels and Agenda, Japanese/Korean manager layouts, and deletion while Agenda is visible. Pass `-e mode motion` for two recorded open/close cycles and the subscription dialog check. The reference video was inspected as 178 frames at 30 fps, including both opening and closing sequences. It restores preferences/manual schedules and removes its imports. Screenshots go to `calendar-import-acceptance` under the audit app's external files. Never run it against the user's production package.

On 2026-10-24, `ultimate` passed 283 local unit tests (zero failures or skips, including the private sample), debug app/test assembly, and Lint with zero errors (313 warnings). Android 15 acceptance on `emulator-5554` passed 89 checks, followed by an 82-check repeat after updating shared accessibility targeting, at system font scale 1.3, including both orientations, Japanese/Korean managers, the 317-event private sample, and live source deletion. Screenshots were inspected locally; private files and screenshots are not committed. Native acceptance also checks selected fonts on rebuilt Agenda rows and active carousels after asynchronous imports.

## Dependencies

ICS parsing uses [biweekly 0.6.8](https://github.com/mangstadt/biweekly) (BSD 2-Clause) and [vinnie 2.0.2](https://github.com/mangstadt/vinnie) (MIT). biweekly includes portions of Google RFC 2445 and Apache Commons Codec; their notices, and both library notices, are packaged under `assets/licenses`. The unused JSON parser dependency is excluded.
