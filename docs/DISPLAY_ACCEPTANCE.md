# Automatic typography and date-format acceptance

- 246 unit tests, debug APK/test-APK assembly and Lint passed.
- Emulator: 1,253 settings/render checks passed, including persisted Auto modes, invalid date rollback, 12 installed clock themes and six offscreen viewport sizes.
- All five calendar themes passed 40 installed layout, selection and orientation checks with automatic typography, in portrait and landscape. Screenshots were reviewed.
- Existing explicit percentages and maxima remain available. Zero stores Auto independently per theme and resolves to the renderer's fitted defaults. Arbitrary imported fonts and text need their own visual checks.
- Carousel right-slide travel no longer collapses to zero for edge-aligned supporting text.
- The supplied icon artwork is retained as the raster master, with Android native adaptive masking and a monochrome silhouette.
