# Desktop widget extension contract

The Ultimate flavor provides `DIGITAL`, `ANALOG`, `WEATHER`, and `CALENDAR` widgets on API 31+.
The calendar large layout is a complete date card; a 42-cell month grid is intentionally deferred.

## Stable data

`WidgetConfig` is immutable; use its builder and `toBuilder()`. JSON schema version 1 lives in
`clockmods_widgets`, under `widget_<appWidgetId>`. The provider, never a caller-supplied extra,
controls widget kind. Invalid fields normalize at the model boundary. `WidgetConfigStore` exposes
the codec for future migrations/preset import; do not change persisted enum names or theme IDs.
`useSystemTimeFormat` distinguishes a live system preference from fixed 12/24-hour display.
`darkText` provides the transparent theme with light-wallpaper contrast.
Widgets follow the app's timezone preference; an empty app timezone follows the system. The legacy
per-widget timezone fields remain readable for schema compatibility but no longer affect rendering
or scheduling. The configuration page has no timezone selector, and cards never show a timezone name.
Location visibility applies only to weather cities. Saving the app timezone sends an explicit private
notification to refresh existing widgets and reschedule midnight updates.

Theme IDs: `system.dynamic`, `glass.light`, `instrument.dark`, `paper.warm`, `neon.night`,
`transparent.clean`. Widget font IDs are `theme`, `system`, `serif`, `monospace`, `condensed`, and
`light`. Their layouts reference named Android system families. Restricted launcher contexts may
refuse bundled `res/font` resources even when an in-app preview loads them successfully; do not
derive this list from the full-screen app's font catalog. Old `lora` selections migrate to `serif`,
`bitcount_grid_double` to `monospace`, and the other formerly bundled choices to `system`.
Dynamic text/background colors use system resources with night variants. No asset Typeface is
injected and no custom view is hosted by a launcher. The app's full-screen font catalog is unchanged.

## Adding a kind or module

1. Add a model value and its default/normalization tests.
2. Add a provider, its label, description, preview and metadata in the Ultimate manifest.
3. Register the provider in `WidgetUpdateCoordinator`; keep its mapping to `WidgetKind` explicit.
4. Add supported RemoteViews layouts and render them exclusively in `WidgetRemoteViewsFactory`.
5. Reuse `WidgetPendingIntentFactory`; identity includes instance ID and action in both request code
   and URI. Never reuse another instance's pending intent.
6. Extend device acceptance tests to apply every theme and size and verify real host binding.

The preview applies the actual RemoteViews from a draft, using a restricted configuration context
without the AppCompat inflater, matching launcher font-loading constraints. Slider updates are coalesced before entering the shared executor; stale frames
are discarded before rendering. Child form state cannot override the serialized draft on recreation. It does not persist drafts or use the
full-screen renderer. Cancel leaves saved configuration unchanged; Done validates the bound ID again.
Weather cards use side-by-side information at wide sizes and a vertical variant at tall sizes.
Small cards keep essential modules, while the calendar minimum still includes its lunar date.
Single-row digital cards retain the full horizontal corner inset but use smaller vertical padding
to accommodate font metrics. Responsive variants share one timestamp and weather
observation via `WidgetRemoteViewsFactory.createAt`, avoiding mixed dates across a midnight update.

The calendar click opens the main app because no stable calendar-page route exists. Weather uses the
existing `UltimateSettingsActivity.createSubpageIntent(context, "weather")` contract.

## Threading and refresh

Providers/receivers use `goAsync()` and one application-context executor. Rendering (including offline
holiday assets and SVG loading), JSON I/O and scheduling run off the receiver thread. Weather network
requests run in WorkManager, never in the receiver or the foreground weather controller.

WorkManager Java runtime is pinned to **2.11.2**, the stable release listed in the
[official release notes](https://developer.android.com/jetpack/androidx/releases/work#2.11.2).
`clockmods_widget_weather_refresh` runs every 30 minutes with a connected-network constraint.
Manual refresh is unique work (`KEEP`). Transient failures retry at most twice; missing credentials
or location never retry. Deletion reconciles against system-bound IDs, not just stored preferences.

`WidgetMidnightScheduler` schedules the app timezone's *next civil midnight*,
so DST and fixed zones work. It uses an inexact, non-wakeup RTC alarm and recalculates after app or system
clock/zone changes and reboot. Android may defer date updates while asleep; no exact alarm permission
is requested for widgets. TextClock/AnalogClock remain live in the host process with no application tick.

Successful foreground/background weather saves notify only weather widgets through an explicit,
non-exported receiver. Failed refreshes retain existing cache and its visible update timestamp.
The background location path only reads permitted last-known locations, never starts listeners or
requests background permission. In-flight results are discarded after a city/language change.

## Validation

```powershell
.\gradlew.bat testUltimateDebugUnitTest lintUltimateDebug assembleUltimateDebug
.\gradlew.bat assembleUltimateDebugAndroidTest
adb install -r app\build\outputs\apk\ultimate\debug\app-ultimate-debug.apk
adb install -r app\build\outputs\apk\androidTest\ultimate\debug\app-ultimate-debug-androidTest.apk
adb shell am instrument -w com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
# Restricted host font rendering and configuration save/reopen regression:
adb shell am instrument -w -e fonts true com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
# Optional integration check using the device's configured QWeather city and credentials:
adb shell am instrument -w -e weather online com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
```

The instrumentation uses a temporary host ID (9917), deletes its bindings, checks real RemoteViews
reflection across all themes/sizes/fonts and the actual minimum pixel dimensions, and writes rendered previews only to the application's cache.
Run on a backed-up test device; it temporarily reconfigures test instances. See the acceptance report
for what was actually executed; pure-Java tests alone do not prove launcher compatibility.
