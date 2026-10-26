# Theme libraries and built-in theme store

Clock and calendar themes have separate installed lists. Existing users initially keep all built-ins. The gallery follows the installed order and ends with a plus card opening the store. Manage opens its Installed tab.

The store provides Discover, Favorites and Installed tabs. Users can install, select, favorite, move and delete bundled themes. Long-pressing a gallery card reveals a red circular minus button. Both deletion paths confirm that the theme settings will be removed, and refuse to delete the final theme in that library.

Removing the active theme selects its next installed neighbor (or the previous neighbor at the end). Scoped typography, script fonts, palettes, transitions and second-hand motion are cleared. Pro Classic also clears its original face controls. Reinstallation appends the theme with default typography and rendering settings. Favorites are independent of installation. Global background, weather, clock-language and calendar-data settings remain shared.

Second-hand motion now belongs to each clock theme. The previous shared value migrates to the existing theme catalog without overwriting any already-scoped value. Global settings reset resolves selection against the installed list and does not reinstall deleted themes. Widget renderer catalogs remain independent of the app gallery.

## Validation

`ThemeLibraryTest` covers persistence, fallback selection, deletion protection, exact setting scopes, default restoration, legacy motion migration, ordering and favorites. `ThemeLibraryAcceptance` exercises actual touch input: long press, minus button, cancel/confirm, final-theme guard, plus-card store entry, reinstall, favorites, reordering and Activity recreation. It restores its touched preferences.

Run the standard Ultimate unit tests, Lint and debug assembly. For device checks, build with an isolated application ID and `-Pclockmods.testRunner=com.clockmods.ultimate.settings.ThemeLibraryAcceptance`, then run instrumentation with `-e kind clock` and `-e kind calendar` in actual portrait and landscape. Verify the bounds reported by the runner instead of assuming requested orientation was applied.
