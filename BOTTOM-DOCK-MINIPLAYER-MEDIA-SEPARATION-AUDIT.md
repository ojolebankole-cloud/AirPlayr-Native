# Bottom Dock, MiniPlayer, Floating Play & Audio/Video Separation Audit

Date: 2026-09-17

Starting point: AirPlayr-Native existing-user startup fix (current Native project supplied before this task).

## Requested scope implemented

### Bottom dock visibility
The app shell now renders the full bottom dock only on these four primary routes:
- `home`
- `music`
- `videos`
- `settings`

All other routes use the music-only MiniPlayer overlay when an audio item is active, except:
- `video/{id}` — full video screen; no MiniPlayer and no dock
- `external-video` — external/full video screen; no MiniPlayer and no dock
- `player` — full music player; no duplicate MiniPlayer overlay

The four dock icons remain Home, Music, Videos, Settings. Navigation behavior is preserved.

## Floating Play
Floating Play actions are now available on the Home-derived list/detail screens that can meaningfully play an audio collection, in addition to the previously implemented History, Favorites, Recent, Stats, and feature-collection screens.

The new/covered screens include filtered album/artist/genre/folder views and playlist detail. Search is intentionally excluded.

The FAB uses the same Play action pattern as History/Favorites and calls the real PlaybackController. It is not a placeholder.

### MiniPlayer/FAB collision prevention
The app shell reserves the MiniPlayer's bottom area for non-primary screens. Floating Play screens therefore use only the remaining in-screen bottom inset when the audio MiniPlayer is visible. This prevents the FAB from being double-offset and sitting too far above the MiniPlayer.

## Audio/video separation

### Music
- Music remains the only media type accepted by the music `PlaybackController` queue APIs.
- The MiniPlayer is rendered only when `currentMediaType == AUDIO`.
- Home's current-playing card is rendered only for audio.
- Audio artwork uses `artworkUri` when present.

### Video
- Videos are routed to the native video screen instead of the music player.
- Video collections, stats, search, history/favorites and Home video rails use video navigation rather than music playback.
- The `video-collections` route is explicitly connected to `video/{id}`.
- Video artwork rendering now generates real video thumbnails/frames through `ContentResolver.loadThumbnail()` on modern Android and `MediaMetadataRetriever` as a fallback.
- Video rows use a video-library fallback icon rather than a music-note fallback.

## Home-specific separation
- Recently added songs remain sourced from audio-only library data.
- Home current playback is audio-only.
- Home video rails explicitly render `MediaType.VIDEO` thumbnails.

## Static verification performed

- 17 targeted behavior/invariant checks: PASS.
- Changed Kotlin files: no trailing whitespace.
- ZIP/file set remained within the existing focused migration source set: the same 10 Kotlin source files already modified by the prior migration passes were changed; no additional unrelated Kotlin source file was introduced.
- Kotlin parser run with locally available `kotlinc -Xuse-k2`: no syntax-like diagnostics (`expecting`, `unexpected tokens`, `unclosed`, `syntax error`, missing delimiter) were reported. The command still reports unresolved Android/Compose dependencies because the project dependency graph is not available locally.

## Build limitation

A full Android Gradle build was not possible in this environment. The project uses Gradle 9.5.0, and the wrapper distribution is not cached here. Network access to `services.gradle.org` is unavailable. Therefore this audit does not claim a successful APK build or physical-device verification.

## Honest verification status

Source-level behavior and route wiring were inspected and checked. The final real-device confirmation still needs to be done with an APK built in an environment that can resolve the project's Gradle/dependency set.
