# Icon and date-format acceptance

- Compat: 92 unit tests; Modern: 92; Pro: 108. All three debug APK builds and Lint passed.
- Pro emulator: 217 settings checks across three languages and two orientations, including invalid custom format rollback after an intermediate valid draft.
- The Android PackageManager icon was rendered on the emulator. An initial screenshot exposed adaptive-layer zoom cropping; the final drawable reserves the outer 18dp of the 108dp layer and preserves the supplied artwork inside the launcher's central 72dp viewport. The final Pro APK and device checks passed after that correction.
- Raster icons retain the supplied artwork; Android launchers provide the adaptive mask and themed monochrome appearance.
