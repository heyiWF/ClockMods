# Predictive Back coverage

Predictive Back requires gesture navigation. Cross-activity and back-to-home previews are owned by Android; settings roots, font/calendar libraries, widget configuration and the first setup step must not install an always-enabled application Back callback. Android 15+ supplies those system animations. Older systems retain ordinary Back navigation.

| Destination | Preview owner | Cancellation |
| --- | --- | --- |
| Settings root / font library / calendar library / widget configuration | Android window transition | Keep the current activity |
| Settings detail | App-drawn page card over the settings window (Views) | Restore the page without restarting it |
| Calendar import action menu | Menu progress | Restore the expanded actions |
| Setup steps | Previous step beneath the outgoing page | Keep draft choices and current step |
| Theme delete badge | Badge scale / opacity | Restore edit affordance |
| Theme store and confirmation / editing dialogs | Dialog-window progress | Keep dialog and pending edits |
| System file/image picker, permission UI, keyboard | Owning system component | Controlled by Android |

Dialog callbacks register in the dialog window and unregister on detach. They dismiss/cancel only on completion; they never invoke a positive button or apply an edit. Application callbacks remain disabled when there is no internal destination to pop.

## Page card (2026-11-26)

Compact settings pages follow Telegram's Back preview. The settings window beneath stays live and never moves; the page on top stays on top, and the gesture only shrinks it toward the edge opposite the swipe: 56dp of travel scales it to 0.90 around a pivot 82dp from that edge, at the height of the touch, clipping 32dp corners, while the window beneath is dimmed. Committing slides the card out (380ms, EaseOutQuint) and then finishes the page without a second transition; cancelling rewinds the same timeline (320ms). The top bar's arrow, key Back and an embedded page have no gesture preview, so they keep their ordinary exits, and an embedded page disables the callback entirely so Android owns its Back.

`UltimateSubSettingsActivity` declares `windowIsTranslucent` in the manifest: Android keeps the window underneath visible only for activities that are translucent where they are declared, not where `setTheme()` is called. That is also why the sub page has no starting window; on the emulator it gained window focus 43–70ms after the row tap, and the settings window was already there.

`PageCardBack` wraps the content view in the sub page, draws the scrim and animates the page itself; because the sub page registers the callback, Android leaves the whole transition to the app instead of animating the window as well.

## Device verification

Build an isolated package with `-Pclockmods.auditApplicationId=com.clockmods.predictivecompose` (Compose) or `com.clockmods.predictivenative` (Views), and `-Pclockmods.testRunner=com.clockmods.ultimate.settings.SettingsBackAcceptance`.

The runner's default `gesture` mode injects actual edge touches and checks screenshot changes away from the system arrow, cancellation, and final navigation. `-e mode surfaces` covers setup and the theme store. `dispatcher` and `keys` are explicit logic-only fallbacks and do not prove system animation support.

The MuMu device initially had hardware-key navigation (`qemu.hw.mainkeys=1`) and an unattached `EdgeBackGestureHandler`. Real gesture validation requires an attached, enabled gesture handler, not only setting the predictive animation flag. The current device also cancels direct UiAutomation touch streams; the test uses Android's shell touch stream instead.

Verified on 2026-11-09 with Android 15 and an attached system edge-back handler: each branch passed 40 primary gesture checks and 10 setup/store gesture checks. Both left/right cancellation and completion are exercised for settings detail, with cancellation/completion for library activities, root settings, import actions, link dialog, setup steps and the theme store. These are real shell touch gestures, not dispatcher fallback results.

Unit tests: Views 291 total (1 skipped), Compose 310 total (1 skipped), no failures or errors. Both APK builds and Lint completed successfully; existing Lint warnings remain.

## README capture

`-e mode media` captures the running audit app after rendering and weather initialization, using the current automatic system date. It writes screenshots to the audit application's external `readme` directory and a screen recording to `/sdcard/<audit-package>-overview.mp4`. Recordings are H.264 MP4 and the README uses GitHub video attachments, since GitHub strips arbitrary HTML video tags pointing to repository files. Repository MP4 copies remain available for offline viewing.

## Cancellation recovery (2026-11-10)

Font and calendar import activities inherit predictive Back from the application opt-in. Their root Back remains system-owned; an expanded calendar action menu
or a dialog consumes only its own internal Back step.

Compose settings, setup and the calendar menu rewind the existing seekable timeline to
fraction zero before settling the origin. They do not retarget the transition to the current
page: doing that changes the enter/exit specifications and can replay an enter animation at
the end of cancellation. A new gesture cancels and joins any outstanding recovery first.
The native calendar action menu also restores preview progress directly, without its normal
staggered opening animation.

Regression coverage includes a hold at the edge before releasing a reversed gesture,
left/right library cancellation, and frame-by-frame Compose cancellation at partial and
zero progress (`BackPreviewCancellationTest`). The user confirmed normal behavior on stock Android; no HyperOS-specific workaround is applied.

Local validation: 291 unit tests (1 skipped), no failures/errors; debug and instrumentation APKs
build successfully; Lint has no errors (existing warnings remain).
The native key/back-stack acceptance runner passed 19 checks.
The current emulator still reports an unattached edge-back handler after changing navigation
mode, so this run does not certify real system cross-activity predictive animations.

The device report was confirmed as HyperOS-specific on 2026-11-10. Import activities
now inherit the application opt-in again; no vendor workaround is needed. The generic
seekable timeline cancellation fix and its regression tests remain.

Alarm editing, multi-date selection and shift-cycle editing use system-owned activity Back. Returning without Apply discards that page’s draft. Native time/date dialogs install dialog-window cancellation handling; the system ringtone picker owns its own Back transition.

## Page card verification (2026-11-26)

Built as `-Pclockmods.auditApplicationId=com.clockmods.predictivenative` and installed on the MuMu emulator (Android 15, 640dp portrait). That emulator again has no attached edge-back handler (`qemu.hw.mainkeys=1`; restarting SystemUI no longer creates a navigation bar), so the preview frames were captured by dispatching the platform's own `BackEvent` sequence into the sub page's dispatcher, which reaches the same `handleOnBackStarted/Progressed/Cancelled/Invoked` path the platform calls. Left and right edges, cancellation at partial progress and a commit from mid progress show the settings window beneath, the shrinking card, the dimmed list, the rewind and the slide away, then the settings window without a black frame. `SettingsBackAcceptance -e mode dispatcher` (25 checks) passes. The sub page gained window focus 43–70ms after the row tap, so dropping the starting window did not delay it.

## Entry transitions and wizard steps (2026-11-26)

The sub page only animated on its first visit, and later visits cut in. The platform drops the
theme's window animation once the task has already hosted and closed a translucent activity,
and vendor builds ignore `ActivityOptions` anyway. The window now opens with no animation at
all (`android:windowAnimationStyle` plus a zeroed per-launch animation) and the page animates
itself: it travels 10% of its width while fading in over 300ms on an emphasised decelerate
curve, and stops immediately if a Back gesture takes over. The list behind stays put, matching
the card that leaves on Back.

Wizard steps cross-slide: the outgoing step is captured as a snapshot that slides away in the
direction of travel while the rebuilt step slides in with an emphasised decelerate curve
(320ms in, 260ms out). The language rebuild, the back gesture and its cancellation still swap
steps without that slide, because the gesture preview owns the motion there.

Switches that gate other rows (network time, chime cues, quiet hours, anti-burn) used to call
`showPage()` from their listener, which replaced every view while the switch was still
animating and read as a flash. They now enable or dim the dependent rows in place, so the
toggle animation runs to completion and the page keeps its scroll position.

Settings categories and the wizard headings use Material Symbols Outlined vector drawables
(`palette_24`, `calendar_month_24`, `image_24`, `schedule_24`, `cloud_24`, `today_24`,
`alarm_24`, `tune_24`, `language_24`). The wizard's own radio dots are drawn 5dp further
inside their chip without moving the labels.
