# Current application captures

Captured on 2026-11-09 from the ultimate-compose branch's current Android 15 audit build. Application source revision: `fa04b22`. The audit application ID is isolated from the user's installed application.

- `ultimate-compose-clock.png`: live dual-block clock with automatic system date and real Beijing weather.
- `ultimate-compose-calendar.png`: live graphite calendar and forecast.
- `ultimate-compose-overview.mp4`: screen recording of the current clock, calendar, settings and import/font management pages, H.264, 1920 × 1080, 30 fps. Only frame-rate normalization and video compression were applied; screens were not mocked.

The `ReadmeCapture` instrumentation entry point sets demo preferences only in an isolated audit package. Inspect every captured image and the recording before replacing README assets. Weather is live, so capture times and temperatures can differ between branches.

The older `pro-features-overview.mp4` was replaced. Existing chime demonstration clips are independent examples.
