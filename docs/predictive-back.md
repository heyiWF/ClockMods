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

## README capture

`-e mode media` captures the running audit app after rendering and weather initialization, using the current automatic system date. It writes screenshots to the audit application's external `readme` directory and a screen recording to `/sdcard/<audit-package>-overview.mp4`. Recordings are H.264 MP4 and the README uses GitHub video attachments, since GitHub strips arbitrary HTML video tags pointing to repository files. Repository MP4 copies remain available for offline viewing.
