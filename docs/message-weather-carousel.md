# Message and weather carousel

The custom message is a separate item after the weather summary and enabled detail fields.
Overflowing messages keep the theme's supporting font size. They pause for one second, move
left at 40 dp/s, then pause at the tail for one second before the next weather item arrives.
The scroll distance includes both 8% edge fades, so the final characters stop in the opaque
part of the line. A message without weather repeats continuously.

`MessageMarqueeLayout` measures the actual renderer font, size and available width. Both
hosts use that measurement to schedule the next item through `WeatherCarousel`. Incoming
animations finish before the reading window starts. The outgoing message retains its tail
offset, font size and clipping during fade, slide, scale, flip and scan transitions. The
carousel uses its own configured transition even when digit animations are disabled.

Only the supporting context line receives message scrolling; dates and other labels retain
their normal layout. Compose advances marquee frames in the drawing phase and changes the
carousel item only at its boundary.

## Validation (2026-09-30)

Both `ultimate` and `ultimate-compose` were installed and verified separately on Android 15
`emulator-5554`.

- Unit tests: 224 on `ultimate`, 239 on `ultimate-compose`, no failures.
- APK and instrumentation APK assembly passed on both branches.
- Real Android Canvas and fonts: all 11 affected themes, portrait and landscape, supporting
  scales 1.0 and 2.0. Checks cover movement, constant font size, the opaque tail position and
  all seven animations when entering and leaving the message.
- The installed glass theme completed weather → message → weather → message with the
  configured right-slide transition and digit animations disabled. Consecutive screenshots
  retain the scrolling and tail frames.
- Clock settings, onboarding state and cached weather were restored after each run. The
  temporarily granted notification permission was restored after device validation.

The instrumentation entry point is:

```text
adb shell am instrument -w -e message true com.clockmods.ultimate.test/com.clockmods.widget.WidgetAcceptanceInstrumentation
```

Grant notification permission temporarily if the first-start permission sheet would cover
the clock, then restore its previous state after the run. Instrumentation disables visual
chimes in its temporary fixture so an hour boundary cannot obscure the carousel evidence.
Device screenshots are written to the app's external-files `message-carousel-acceptance`
directory; each run replaces its own screenshots.
