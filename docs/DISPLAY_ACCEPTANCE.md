# Display refinement acceptance

Desktop frontend: all 338 tests in 36 files pass; TypeScript checking and the Vite production build pass. Regression coverage includes invalid custom-date drafts, saved automatic typography, theme preview selection, and repeated right-slide carousel wraparound.

Both Web and desktop frontend passed 816 real Chrome layout cases each across 12 clock themes and 5 calendar themes, eight viewport sizes, three UI languages, and system/Lora fonts. Repeat the desktop matrix at `desktop/tests/browser-acceptance.html`. These bounds checks cover the listed cases, not arbitrary imported fonts or unlimited text. Native window and notification behavior is validated separately by platform smoke checks.

The supplied icon artwork is retained as the raster master, including its texture and lighting. Platform sizes and Windows ICO/macOS ICNS are generated from it. Android uses the artwork with the native adaptive mask and a separate monochrome silhouette for themed icons. The artwork remains flattened to preserve the supplied pixels; it is not a reconstructed 3D asset.

Android automatic typography stores zero independently for each theme and resolves it to the renderer's fitted defaults. Existing explicit percentages and their maximums are retained. Invalid date patterns leave the previous pattern and custom-mode metadata unchanged. Device acceptance exercises persisted automatic settings and records clock and calendar layouts.

Compose Android: 262 unit tests, debug APK/test-APK assembly and Lint passed. The emulator suite passed 1,253 settings/render checks, followed by all five calendar themes in portrait and landscape (layout, day inventory, selection, today, bidirectional swipe, snap-back and picker). Screenshot review caught and corrected a probe that counted the system status bar as loaded content; the suite was rerun successfully. Calendar frame timings were collected under build load and are not a smoothness benchmark.
