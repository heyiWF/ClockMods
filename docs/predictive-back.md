# Predictive Back coverage

Predictive Back requires gesture navigation. Cross-activity and back-to-home previews are owned by Android; settings roots, font/calendar libraries, widget configuration and the first setup step must not install an always-enabled application Back callback. Android 15+ supplies those system animations. Older systems retain ordinary Back navigation.

| Destination | Preview owner | Cancellation |
| --- | --- | --- |
| Settings root / font library / calendar library / widget configuration | Android window transition | Keep the current activity |
| Settings detail | Android activity transition (Views), seekable content transition (Compose) | Restore current page and controls |
| Calendar import action menu | Menu progress | Restore the expanded actions |
| Setup steps | Previous step beneath the outgoing page | Keep draft choices and current step |
| Theme delete badge | Badge scale / opacity | Restore edit affordance |
| Theme store and confirmation / editing dialogs | Dialog-window progress | Keep dialog and pending edits |
| System file/image picker, permission UI, keyboard | Owning system component | Controlled by Android |

Dialog callbacks register in the dialog window and unregister on detach. They dismiss/cancel only on completion; they never invoke a positive button or apply an edit. Application callbacks remain disabled when there is no internal destination to pop.

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
