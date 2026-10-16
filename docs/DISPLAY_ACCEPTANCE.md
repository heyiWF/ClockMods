# Display refinement acceptance

Desktop frontend: all 338 tests in 36 files pass; TypeScript checking and the Vite production build pass. Regression coverage includes invalid custom-date drafts, saved automatic typography, theme preview selection, and repeated right-slide carousel wraparound.

The shared Web renderer passed 816 real Chrome layout cases across 12 clock themes and 5 calendar themes, eight viewport sizes, three UI languages, and system/Lora fonts. `desktop/tests/browser-acceptance.html` runs the equivalent desktop frontend matrix. Native window and notification behavior is validated separately by the desktop CI and platform smoke checks.

The supplied icon artwork is retained as the raster master, including its texture and lighting. Platform sizes and Windows ICO/macOS ICNS are generated from it. Android uses the artwork with the native adaptive mask and a separate monochrome silhouette for themed icons. The artwork remains flattened to preserve the supplied pixels; it is not a reconstructed 3D asset.

Android automatic typography stores zero independently for each theme and resolves it to the renderer's fitted defaults. Existing explicit percentages and their maximums are retained. Invalid date patterns leave the previous pattern and custom-mode metadata unchanged. Device acceptance exercises persisted automatic settings and records clock and calendar layouts.
