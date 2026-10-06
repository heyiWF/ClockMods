# October 2026 maintenance audit

## Shared defects and scope

- NTP replies now originate from the queried UDP peer and must echo the complete request timestamp. Short, unsynchronized, invalid-version/stratum and inconsistent timestamp replies are rejected.
- Weather requests reject redirects instead of forwarding bearer credentials. UTF-8 response reads preserve whitespace and stop at 1 MiB characters.
- Closing network time prevents re-enabling, queued callbacks and rejected-executor races.
- Java maximum font fractions are constrained by fitted width and height; persisted values are retained.
- Main only: API 14-compatible Canvas saveLayer overload and detached calendar Fragment guards. No new product features or minimum SDK changes (compat 14, modern 23, pro 31).

The recent Compose unchanged-digit transition fix uses a different renderer and is not copied into Main. Main already contains the corresponding weather failure notification fix. Existing TLS certificate and hostname validation are retained.

## Build and credential handling

Gradle build and configuration caching are enabled. CI requests explicit Debug unit tests and lint alongside APK assembly in one invocation. The Compose aggregate test/lint/assemble entry had 66 dry-run tasks versus 63 explicit entries; only three aggregate wrappers were removed, with no release compilation in that comparison. Main adds previously missing quality gates, so its total CI duration is not expected to decrease simply by removing tasks.

On an unchanged Compose tree, the final testUltimateDebugUnitTest/lintUltimateDebug/assembleUltimateDebug run reused configuration cache, completed in 28 seconds and reported 56 actionable tasks (55 up-to-date, one lint wrapper). Timings are local observations, not guaranteed CI speedups.

Public CI artifacts and task caches no longer receive QWeather signing credentials. Weather therefore reports not configured unless credentials are provided for a personal local build. Previously distributed signing keys must be rotated at QWeather; this repository change does not revoke them. Keep local qweather.properties untracked and do not distribute an APK containing a private signing key.

## Verification and boundaries

Android validation used the connected API 35 device through ADB. Debug unit tests, lint and APK builds passed across all three Android branches; Main exercised all three flavors. Clock settings validation covers persisted booleans, 12 Ultimate clock themes, three locales, portrait/landscape rendering, maximum scales, scoped font/weight and digit/weather transitions, date/seconds/timezone/status/message rendering, and opening each installed theme's settings page. The device runner reports 1082 checks per Ultimate implementation; rendering matrices retain controls that do not visibly change a particular theme rather than declaring every control universally applicable.

Main's installed UI runner covers 54 checks per flavor, three locales and two orientations, including the actual settings dialog. Calendar acceptance covers five themes in both orientations; Compose also checks selection, Today, bidirectional swipes, snap-back and month pickers. Acceptance runners restore preferences and rotation. Override the instrumentation runner with -Pclockmods.testRunner when building these APKs; production APK behavior is unchanged by the test harness.

API 14/API 23 physical devices, live signed weather endpoints, real sound output, hardware burn-in behavior and every possible background image/font/network condition are not exhaustively tested. Lint verifies API usage but cannot replace old-device execution. The API 35 device's calendar frame timings reflect that device and are not a measured performance improvement.
