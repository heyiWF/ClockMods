# Automatic typography and date-format acceptance

- 262 unit tests passed; debug APK, test APK and Lint passed.
- Emulator: 1,253 settings/render checks passed, including persisted Auto modes, invalid date rollback, 12 installed clock themes and six offscreen viewport sizes. Screenshots were reviewed after fixing the probe to exclude the system status bar when detecting loaded content.
- All five calendar themes passed portrait and landscape checks for layout, day inventory, selection, today, bidirectional swipe, snap-back and month picker using Auto typography.
- Existing explicit percentages and maxima remain available. Zero stores Auto independently per theme and resolves to the renderer's fitted defaults; imported fonts and arbitrary long text still require their own visual checks.
- Carousel slide-right travel remains independent of edge whitespace, so edge-aligned swaps retain horizontal motion.
- The supplied icon artwork is retained as the raster master, with native Android adaptive masking and a monochrome silhouette. Its texture and lighting are preserved as flattened artwork.
