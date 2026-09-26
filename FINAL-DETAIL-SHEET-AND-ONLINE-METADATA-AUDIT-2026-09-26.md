# Final Detail Sheet + Online Metadata Repair — 2026-09-26

## Detail sheet consolidation

- `MediaDetailsSheet.kt` is now the single canonical media-details sheet.
- `PlayerScreen`'s three-dot `Details` action uses `MediaDetailsSheet`.
- Library, Favorites, History, Most Played, Search, Video Library, and parity/recent surfaces that expose media details use the same component.
- Removed the old `HistoryFavoriteDetailsSheet` implementation.
- Removed the old PlayerScreen-local `DetailsSheetContent` implementation.
- Removed the unused legacy `PlayerScreen.kt.bak2` containing another old details implementation.
- The canonical sheet supports both audio and video while preserving the playback-sheet music presentation and the existing Edit metadata action for audio.
- Player detail playback state now correctly reflects paused/playing state instead of always rendering as playing.

## Online metadata repair

- Kept the real iTunes Search API HTTPS lookup and strengthened query generation with progressive title+artist, title+album, title-only, and full-term searches so local filenames/metadata do not over-constrain the lookup.
- Added explicit HTTP failure reporting instead of silently converting service failures into an empty result set.
- Added a User-Agent and longer network timeouts.
- Results are scored against title, artist, album, and duration and de-duplicated before display.
- `Apply to file` now writes the selected catalog metadata into the actual embedded audio tags using Jaudiotagger rather than relying only on MediaStore index fields.
- Writes title, artist, album, album artist, genre, year, track number/total, and disc number/total when supplied.
- Downloads catalog artwork and embeds it into the audio tag when available.
- Rewrites the modified temporary audio file back to the original file/content URI, then refreshes MediaStore and saves an AirPlayr override for reliable in-app display.
- If the online service is unavailable, the UI now reports a lookup/network failure rather than claiming there are simply no matches.

## Verification

- `android/tools/phase3-static-check.sh` — PASS.
- Full Gradle Kotlin compilation was attempted with `:app:compileDebugKotlin --offline --stacktrace`, but Gradle 9.5.0 is not cached in this environment and the wrapper could not reach `services.gradle.org`. Therefore no APK/build-pass claim is made here.
