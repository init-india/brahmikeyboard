# Brahmi Keyboard

A keyboard for typing Ashokan Brahmi (Unicode block U+11000) on phones.

| Mode | What you type | What the app receives |
|---|---|---|
| EN | English | English |
| BR | English letters, read phonetically (`dhamma`) | Brahmi. The preview shows your preferred language's script and the Brahmi. |
| PB | Brahmi keys | Brahmi. The preview shows the reading in your preferred language. |

Words are sent on Space. Enter sends the word and a new line. In password fields the preview and the field show `*`, while the app receives the real text. No network access, no analytics.

## Layout

```
web/       Browser prototype. This is the specification: change rules here first.
data/      Generated shared tables, test cases and user guide (+ font).
tools/     export-data.js regenerates data/ from the prototype. fetch-font.sh downloads the font.
android/   Kotlin app. One codebase, flavors "play" and "fdroid".
ios/       (later) Swift app with a keyboard extension. Reads the same data/.
```

## Workflow

1. Change a rule in `web/index.html` and try it in a browser.
2. `node tools/export-data.js` to regenerate `data/`.
3. The Kotlin engine (`android/.../Engine.kt`) is a port of the prototype and must pass every case in `data/tests.json`.
4. Push. GitHub Actions runs the tests and builds APKs. Download them from the run's Artifacts.

## Build locally

```
sh tools/fetch-font.sh          # once, then commit the font
cd android
gradle wrapper --gradle-version 8.10.2   # once, then commit the wrapper
./gradlew :app:testPlayDebugUnitTest :app:assemblePlayDebug
```

APKs appear in `android/app/build/outputs/apk/`.

## Releases

Tag `vX.Y.Z`. Bump `versionCode` (must only go up) and `versionName` in `android/app/build.gradle.kts`. Add a changelog file per version for the stores.
F-Droid builds from the tag; its metadata lives in the F-Droid data repository. Play and App Store releases need your own signing keys, kept out of the repo.

## Status

0.9: Custom, Brahmi and Pure Brahmi modes, 21 languages, warnings and blocked letters, numerals, themes, user guide.
Not yet: custom fonts, language-specific letters (Tamil ழ ற ன and others), native-speaker checks of non-Devanagari tables.

## Licence

GPL-3.0-or-later (see `LICENSE`). The Noto Sans Brahmi font in `data/fonts/` is under the SIL Open Font License. Privacy policy: `PRIVACY.md`. How to publish: `docs/PUBLISHING.md`.
