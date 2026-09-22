# AirPlayr — Current Video Playback / UI Fix Audit

Date: 2026-09-22

## Implemented

- Kept the **bottom** 10-second rewind/forward controls and removed the duplicate **middle** 10-second controls from the video player. The center row is reserved for previous / play-pause / next.
- Main and external video players both support:
  - double tap left = -10s
  - double tap right = +10s
  - horizontal drag = progressive seek (left-to-right forward, right-to-left backward)
  - vertical drag on the left = device STREAM_MUSIC volume
  - vertical drag on the right = active player window brightness
- Brightness changes are applied to the active playback window and restored when the player is disposed. Global Android brightness settings are not written, avoiding WRITE_SETTINGS special access.
- Generic three-dot controls are now transparent. The music playback More control and video-player More control retain their glass-style background exception.
- Card elevation/shadow rendering is flattened across the app's Card primitives and direct Material3 Card usages. Existing AirPlayrCard elevation arguments remain source-compatible but no longer render shadows.
- Feature-card artwork now uses 1600x1600 RGBA renders generated directly from the high-quality SVG files in `assets/feature-cards`. Each generated PNG was re-rendered with Inkscape and byte-compared with the checked-in resource; all seven matched.
- Folder screen cards/tabs were reduced slightly in size in the existing parity implementation.
- Playback screen now uses the application-shared `PlaybackController`, avoiding a second MediaController/ViewModel connection when opening the music player.
- Existing cached playback snapshot path remains responsible for restoring title/artwork/position before the service connection; MiniPlayer retains immediate cached metadata/progress and pre-sized artwork decoding.
- Listening stats keeps a short-lived summary cache to reduce repeated database aggregation when reopening the screen.
- Music screen heavy category groupings and playlist database work remain deferred to the category that needs them.
- Music playback volume slider uses Android `AudioManager.STREAM_MUSIC` volume rather than only changing ExoPlayer's internal gain.
- Android media notification exposes shuffle and repeat CommandButtons and updates their icons when the playback mode changes.
- Equalizer screen was inspected: it does not disable the rest of the controls when the master toggle is off, so no unnecessary UI lockout was added.
- Video queue drawer no longer shows a stale "COMING" placeholder; it opens the real video queue sheet.

## Verification

- Kotlin syntax parsing of the modified Kotlin source set reported **no parser/syntax errors** after the changes.
- Feature SVG -> PNG resource reproduction was verified byte-for-byte for all seven feature illustrations.
- Source inspection confirms the video 10-second buttons exist only in the bottom control groups in the active video screens.
- `./gradlew :app:compileDebugKotlin --offline --stacktrace` could not execute because Gradle 9.5.0 was not cached and `services.gradle.org` could not be resolved in this environment.
- Android device/runtime verification was therefore not possible here. In particular, actual notification rendering/tapping, device-specific brightness behavior, gesture feel, Cast discovery, and end-to-end playback need a real Android build/device run.
