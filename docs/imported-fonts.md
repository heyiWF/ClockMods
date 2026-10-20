# Imported fonts and script pairing

System & Language opens the font manager. Android's document picker accepts multiple files; fonts are copied into private app storage, so the original document can later be moved. Supported containers are OpenType/TrueType SFNT (TTF, OTF), collections (TTC, OTC), and variable fonts with a `wght` axis. Limits are 32 MiB per file, 64 files and 128 MiB total.

Import reads bounded name, OS/2, fvar and Unicode cmap tables and also asks Android to decode every face. Coverage uses the file's cmap, not a platform fallback font. A representative alphabet for at least one app language (simplified/traditional Chinese, English, Japanese or Korean) must pass; otherwise import reports the unsupported language error. The manager additionally shows coverage across unique letters in the localized app resources. Runtime dates, user text and place names may contain other glyphs; missing glyphs use system fallback.

Files are addressed by SHA-256. Identical contents, including renamed uploads, are rejected. Matching family/style/weight and glyph-table fingerprints also reject metadata-only duplicates. Normalized typographic family names group files; real static weights or variable-axis stops determine the slider. Deletion acts on a file, including all faces in a collection; remaining files keep the family selectable. Missing families fall back to system, and unavailable weights resolve to the nearest remaining stop. Derived subset files are deleted with the source file.

Western family/weight retain the existing per-theme keys. East Asian family/weight use `script_font__<scope>__<language>` keys; calendar scopes remain `calendar:<id>`. Each language picker filters on representative glyph coverage. Mixing uses a render-only ID and script-restricted cmap copies: Han, kana and Hangul use the selected East Asian face, while Latin, numbers and punctuation use the Western face. Android supplies remaining fallback glyphs. System faces are obtained from Android text shaping to respect locale, OEM fonts and physical weight axes. The original font files are never modified.

Application clocks, calendars and previews use this resolver. Widgets retain their bundled font inventory and RemoteViews behavior. Theme colors, images and fills are not changed by font selection.

English, Japanese and Korean offer a **Chinese calendar** switch in System & Language. It defaults on. Turning it off suppresses mainland China holiday/workday marks, lunar dates, festivals and almanac content in application clocks, calendars and previews. It preserves the separate clock lunar preference and does not affect Chinese locales.

## Reproduce validation

Use Java from Android Studio and an installed SDK, then run (quote properties in PowerShell):

```text
./gradlew testUltimateDebugUnitTest assembleUltimateDebug assembleUltimateDebugAndroidTest lintUltimateDebug '-Pclockmods.testRunner=com.clockmods.ultimate.clock.FontImportAcceptance' '-Pclockmods.auditApplicationId=com.clockmods.fontaudit'
```

The optional application ID builds an isolated audit app. Install its app and test APK, then run:

```text
adb shell am instrument -w com.clockmods.fontaudit.ultimate.test/com.clockmods.ultimate.clock.FontImportAcceptance
```

The device runner uses bundled Latin files and Android's Noto Serif CJK collection. It checks import rejection, deduplication, family weights, language filtering, independent theme settings, actual mixed-render glyph pixels, deletion/fallback, fixed marquee defaults and Japanese/Korean locales. Screenshots are saved under the audit app's external files in `font-import-acceptance`. Test imports and preferences are cleaned up by the runner.

On emulators that override activity orientation requests, save `adb shell wm fixed-to-user-rotation`, temporarily set it to `enabled`, run the matrix, then restore the saved value. The runner requests each display rotation and restores the rotation lock when it disconnects.

Validation on 2026-10-20: 254 unit tests, debug/test APK assembly and Lint passed. The Android 15 device runner passed 404 checks, including document-picker callbacks, duplicate feedback, per-file deletion and actual Western/East Asian glyph pixels. Japanese and Korean settings, font management, clocks and calendars were captured in portrait and landscape. The Chinese-calendar switch was checked in English, Japanese and Korean across all five calendar themes in both orientations, including live calendar state, accessibility text and restoration of language/lunar preferences. Each locale includes all 594 translatable resources with matching format arguments and array lengths.
