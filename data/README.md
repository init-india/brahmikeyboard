# Shared data

Single source of truth for the conversion rules, used by the Android app, the future iOS app and the web prototype.

- `tables.json`: transliteration, Devanagari to Brahmi map and per-language script tables. **Generated.**
- `tests.json`: expected results for typed sequences. **Generated.** Every platform must pass all cases.
- `guide.html`: the user guide shown in Settings. **Generated** from the prototype.
- `fonts/`: Noto Sans Brahmi (SIL OFL). Fetch with `tools/fetch-font.sh` and commit it.

Do not edit the generated files by hand. Change `web/index.html`, then run `node tools/export-data.js`.
