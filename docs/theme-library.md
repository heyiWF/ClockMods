# Theme libraries and built-in theme store

Clock and calendar themes have separate installed lists. Existing users initially keep all built-ins. The gallery follows the installed order and ends with a plus card opening the store. The gallery has no edit button: a long press reveals the deletion badge.

The store provides Discover, Favorites and Installed tabs. Users can install, select, favorite and delete bundled themes. Long-pressing a gallery card reveals a red circular minus button. Both deletion paths confirm that the theme settings will be removed, and refuse to delete the final theme in that library.

Removing the active theme selects its next installed neighbor (or the previous neighbor at the end). Scoped typography, script fonts, palettes, transitions and second-hand motion are cleared. Pro Classic also clears its original face controls. Reinstallation appends the theme with default typography and rendering settings. Favorites are independent of installation. Calendar week-start, weekend highlighting and typography are scoped to the selected theme and cleared on removal. Existing shared week/weekend choices migrate once to the active calendar theme. Unconfigured calendar typography uses calendar defaults, independent of clock typography. Global background, weather, clock-language and calendar data remain shared.

Second-hand motion now belongs to each clock theme. The previous shared value migrates to the existing theme catalog without overwriting any already-scoped value. Global settings reset resolves selection against the installed list and does not reinstall deleted themes. Widget renderer catalogs remain independent of the app gallery.

## Validation

`ThemeLibraryTest` covers persistence, fallback selection, deletion protection, exact setting scopes, default restoration, legacy motion migration, installation order and favorites. `ThemeLibraryAcceptance` exercises actual touch input: long press, minus button, cancel/confirm, final-theme guard, plus-card store entry, reinstall, favorites, append-on-reinstall ordering and Activity recreation. It restores its touched preferences.

Run the standard Ultimate unit tests, Lint and debug assembly. For device checks, build with an isolated application ID and `-Pclockmods.testRunner=com.clockmods.ultimate.settings.ThemeLibraryAcceptance`, then run instrumentation with `-e kind clock` and `-e kind calendar` in actual portrait and landscape. Verify the bounds reported by the runner instead of assuming requested orientation was applied.

Validated on 2026-10-27: debug app/test APK assembly and Lint passed; 290 unit tests reported no failures or errors (one existing skipped test). Clock and calendar device flows passed in portrait and landscape on API 35, including deletion, final-theme protection, plus-card store entry, reinstall and recreation. Temporary audit applications use separate IDs.

The follow-up cleanup removes unreferenced rendering helpers and gallery remnants, and shares one in-memory preferences fixture between theme-library and clock-preferences tests. Regression cases remain intact.

The store follows CalendarMods: three columns of rounded previews, a favorite control over the top-right corner, centered titles and full-width action buttons. Installed order is fixed by installation time; selecting or favoriting never changes it. Removing and reinstalling a theme appends it immediately before the gallery store entry. No reorder controls or reorder API are exposed.

Native clock previews render at the full clock viewport, then scale uniformly into the card. Card dimensions must never become responsive-layout inputs, which would select the compact fallback for every theme. The automatic font-size action shares the title row. Run `ThemeLibraryAcceptance` with `-e mode navigation` to verify the normal settings entry, wide master/detail navigation and compact automatic-size action; direct page fixtures deliberately bypass the settings home.

Font-size controls use whole percentage points. Layout protection and its warning use the same `FontScalePolicy` threshold: only values greater than 100% allow overflow; automatic, 99% and 100% retain layout caps.
