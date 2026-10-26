# Calendar imports, typography, Back and launch

Calendar files can be imported once from HTTPS or webcal links. The outlined link action sits alongside the existing file-import FAB. Downloads have timeouts, a size limit, and validated HTTPS redirects. Subscription URLs are not persisted. Import errors use the existing calendar validation flow.

The date-carousel switch only filters labels below dates. Imported schedules remain available to Agenda. Font controls use Auto for new installations and restored defaults; explicit existing choices are preserved. Classic date and supporting percentages use the same baseline. Clock and weather slides share travel and easing, and Compose weather motion no longer restarts when clock animation is toggled.

All Ultimate activities opt into system predictive Back, including the calendar/font libraries and clock underneath settings. Enabled custom callbacks remain limited to in-app navigation, leaving Activity exits to Android. The system cross-Activity preview requires a supporting Android build and gesture navigation.

The Android launch surface uses the platform SplashScreen API and an animated vector of the launcher artwork. The coral sector fades in from outside the slate clock and assembles into a circle over 920 ms; a short exit fade then reveals the ready clock. Fast starts finish the remaining icon motion, slow starts add no extra hold. System-disabled animations are respected. There is no extra splash Activity.

## Validation

- Ultimate and Compose: unit suites, Lint, application and instrumentation APK builds pass.
- Native device runner: library/dialog/setting return paths, persisted carousel preference and Agenda retention pass; live Google public ICS HTTPS download and storage pass.
- Compose device runner: in-app Back progress, both edges, cancel/commit, library/dialog/setting returns, carousel preference, Agenda retention and live HTTPS import pass.
- Pro and Compose cold-launch recordings verify the system launch surface; the final artwork matches the launcher sector and clock. Web and desktop frontend builds and startup/recovery tests pass; the Web page visibly exits its launch overlay into the clock.
- On emulator-5554 (API 35), SystemUI reports EdgeBackGestureHandler mIsEnabled=false and mIsAttached=false despite gesture navigation mode. System cross-Activity preview is therefore not visually certified on this emulator. The runner keeps strict preview assertions for a supporting device; dispatcher/keys results are not substitutes for this check.
- Native desktop packaging was not run: Rust/Cargo is not installed in this environment.

Build SettingsBackAcceptance with `-Pclockmods.testRunner=com.clockmods.ultimate.settings.SettingsBackAcceptance` and a dedicated `-Pclockmods.auditApplicationId`. Use `am instrument -w -e mode gesture` on a supporting device, `mode dispatcher` for Compose in-app Back, or `mode keys` for native stack behavior. Optional `-e calendarLink HTTPS_URL` verifies a live import and deletes only the fixture source afterward. The runner also restores its temporary carousel and onboarding preferences.

## Official references

- [Android SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Predictive Back](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
- [Compose predictive Back setup](https://developer.android.com/develop/ui/compose/system/predictive-back-setup)
- [Material common buttons](https://github.com/material-components/material-components-android/blob/master/docs/components/CommonButton.md)
- [Tauri window background](https://v2.tauri.app/reference/config/#windowconfig)
