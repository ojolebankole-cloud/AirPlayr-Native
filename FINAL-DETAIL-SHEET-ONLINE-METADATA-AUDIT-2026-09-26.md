# Final Detail Sheet + Online Metadata Repair Audit — 2026-09-26

## Requested work
1. Make the detail sheet used by the Music Playback three-dot menu the single detail-sheet implementation across every screen that exposes media Details.
2. Remove the duplicate/older detail-sheet implementations.
3. Make Online Metadata perform real online lookup and real metadata writes rather than only updating the MediaStore index.

## Detail sheet repair
Created the canonical `MediaDetailsSheet` in `ui/components/MediaDetailsSheet.kt`.

The following media surfaces now route Details to that component:
- Music Playback / three-dot menu
- Search
- Library
- Favorites
- History
- Most Played
- Video Library
- Feature/parity media action screen
- Video action/detail path in the parity screen

Removed the old `HistoryFavoriteDetailsSheet`, the old parity `VideoDetailsSheet`, and the private Player-only `DetailsSheetContent`.

The canonical sheet keeps the Music Playback sheet's visual language and surface theming, while supporting both audio and video metadata. Audio screens can optionally expose the existing Edit metadata action.

## Online Metadata repair
The previous implementation had two important weaknesses:
- one combined iTunes query could easily return no results for filenames containing extra text;
- `ContentResolver.update()` was being treated as the actual metadata writer, even though that is not a reliable embedded-tag writer.

The repaired implementation:
- performs progressive online iTunes catalog searches using title+artist, title+album, title, and the original combined term;
- merges duplicate catalog results and keeps the strongest match;
- sends a clear HTTP User-Agent and JSON Accept header;
- reports network/lookup errors to the UI instead of silently turning them into `No matches`;
- downloads and caches catalog artwork;
- writes audio metadata into the actual audio file using the project's existing Jaudiotagger dependency;
- creates a default tag when a file has no tag;
- writes title, artist, album, album artist, genre, year, track and disc information;
- embeds downloaded artwork when supported by the audio format;
- writes the modified temporary file back through the original `file://` or `content://` URI;
- then refreshes MediaStore/index metadata and saves the AirPlayr metadata override.

Jaudiotagger's documented `getTagOrCreateAndSetDefault()` and `AudioFileIO.write()` APIs are used for the actual embedded-tag write.

## Verification
- `android/tools/phase3-static-check.sh`: PASS
- Old detail-sheet symbols in active UI source: none found.
- Canonical `MediaDetailsSheet` references verified across all targeted screens.
- Full Gradle compilation was attempted but could not start because this environment cannot download the configured Gradle 9.5.0 distribution from `services.gradle.org` (`UnknownHostException`). Therefore this audit does **not** claim a successful APK/Kotlin build.

## Honest limitation
The online metadata path is implemented for real audio-file tag writing, but a real device/network run is still required to verify each provider response and every individual file/provider permission combination. No build or device test has been falsely reported as passing.
