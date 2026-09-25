# AirPlayr — Playlist + Video Playback Implementation Report

## Input
- App source: `AirPlayr-bliss-2026-09-25-sheets-fixed.zip`
- Playlist visual reference: user-provided screenshot
- Existing video/sheet references were inspected from the supplied reference package.

## Implemented

### Music playback
- Replaced playback repeat SVG assets with native Material repeat icons (`Repeat` / `RepeatOne`).
- Removed the two unused repeat SVG assets.
- Audio technical line now reads from the active file using `MediaMetadataRetriever` and displays actual format/bitrate/sample-rate where available.
- Metadata extraction is moved to `Dispatchers.IO` and only runs when the current song changes.

### Add-to-playlist
- Removed the legacy `PlaylistPickerDialog` from `MusicScreen`.
- Music-screen song actions and multi-selection now open `HistoryFavoritePlaylistSheet`, the canonical Add to Playlist sheet.

### Three-dot menus
- Audited all current `MoreVert`/`MoreHoriz` UI occurrences.
- No remaining empty `onClick = {}` / empty `onMore` handlers were found.
- Playlist/song overflow actions are connected to real sheets or action menus.

### Genre screen
- Removed the background surfaces from the Genre back and overflow buttons while preserving the icons and touch targets.

### Playlist detail screen
- Rebuilt `PlaylistDetailScreen` around the supplied reference composition:
  - large layered artwork hero
  - large playlist title
  - song count + total duration
  - large Play all control
  - Shuffle control
  - Pin control
  - artwork-first song rows
  - three-dot song actions
  - drag-handle affordance
- Added real row actions: play, play next, queue, add to playlist, remove from playlist.
- Added `StructuredRepository.removeFromPlaylist()` so removal is persisted.
- Existing navigation routes continue to resolve to this detail screen, including music-playlist, video-playlist and generic playlist routes.
- Video playlists now initialize the shared `VideoQueueStore` and open the video player from the selected sequence.

### Video playback
- Lock behavior changed so tapping the video while locked no longer unlocks it. Unlock is performed only through the visible lock control.
- Double-tap now toggles play/pause instead of seeking.
- Volume and brightness gestures no longer call the control `touch()` path, so they do not reveal the complete playback controls.
- Portrait-only utility cards are positioned above the progress bar: lock/audio on the left; PiP, portrait/landscape and zoom/aspect controls on the right; shuffle/repeat in the center.
- Subtitle overlay was moved closer to the transport controls while retaining the configured top/bottom setting.
- Pinch-to-zoom is connected to the Settings toggle.
- Long-press 2× speed is connected to the Settings toggle and restores the configured default speed on release.
- Configured seek interval is used by the player seek controls.
- Keep-screen-awake setting is now applied while video playback is active.
- Remember-subtitle-track setting persists subtitle enabled/disabled state per video.
- Remember-audio-track setting persists a selected audio track index per video and reapplies it when tracks become available.
- Video playback now reports actual decoder class (`HW`/`SW`, based on the Media3 decoder name) and actual video resolution quality (`HD`, `FHD`, `2K`, `4K`, etc.) in the playback header.
- Previous/next now operate against the shared video queue and navigate to real adjacent videos.
- Added video shuffle and repeat state to `VideoQueueStore` and exposed controls in the playback UI and More sheet.
- Video queue playback continues automatically to the next queued video instead of stopping at each video.
- Queue repeat loops to the beginning when enabled; shuffle chooses another queued video when enabled.
- Video playback player reuse was improved by avoiding unnecessary `ExoPlayer` release/recreation during the screen's lifetime.
- Progress sampling was relaxed from 400 ms to 650 ms to reduce UI/Room pressure while keeping progress responsive.

### External video playback
- Applied the same double-tap play/pause behavior.
- Applied long-press 2× speed and pinch zoom settings.
- Gesture-driven volume/brightness changes no longer reveal full controls.
- Keep-awake and remembered subtitle state are applied.
- Portrait orientation is restored when leaving external video playback.

## Verification

### Static verification completed
- Kotlin brace/parenthesis balance checked on all changed Kotlin files.
- Kotlin parser/compiler invocation produced no syntax/`expecting`/unclosed-declaration errors in the changed files.
- No `PlaylistPickerDialog` references remain.
- No empty `onClick = {}` or empty `onMore = {}` placeholder handlers were found for the overflow-menu audit.
- Diff against the supplied app ZIP shows exactly 8 modified Kotlin source files plus removal of the 2 obsolete repeat SVG assets.

### Full Gradle build
A full Gradle compile could not be completed in this environment because the project requires Gradle 9.5.0 and the environment does not have that distribution cached; the wrapper attempted to reach `services.gradle.org` and network access failed with `UnknownHostException`.

Therefore this report does **not** claim that a complete Android Gradle build passed here.
