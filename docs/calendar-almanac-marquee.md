# Calendar almanac labels and marquees

The calendar uses softly filled circular 宜/忌 labels and ` · ` between almanac items.
The label stays pinned while the body scrolls at its configured font size. Localized labels
fit their glyphs inside the circle without changing the body size.

`CalendarMarqueeTiming` preserves the Ultimate timing: 40 dp/s with a one-second pause.
Permanent lines form a continuous belt; the following copy arrives at the original position
and pauses before the next lap. Rotating footers pause at the head, scroll once to a readable
tail, pause again, then slide upward to the next item. Short lines remain still.

This applies to the Indigo Week detail card, the Graphite/Carbon/Paper month footers,
Compose's other overflowing calendar labels, and the main branch's Pro month footer.

## Reproduce device acceptance

Build the branch's debug APK and instrumentation APK using
`'-Pclockmods.testRunner=com.clockmods.ui.CalendarAlmanacAcceptance'` (quote the property
in PowerShell). This selects the focused runner while leaving the default runner unchanged.

```text
adb shell am instrument -w com.clockmods.ultimate.test/com.clockmods.ui.CalendarAlmanacAcceptance
```

For the main branch use `assembleProDebug assembleProDebugAndroidTest` and replace the test
package with `com.clockmods.pro.test`. Install both APKs before running.

The runner probes real Android fonts at two sizes, including Chinese and English badges,
checks the head/lap/tail pauses, and captures each applicable calendar in both orientations.
It observes a full live Indigo Week lap to confirm movement, pause and resumption.
Screenshots are stored in the app's external-files `calendar-almanac-acceptance` directory.
Calendar preferences and onboarding state are restored even when acceptance fails.
Temporarily grant notification permission if necessary, then restore its previous state.
