# AirPlayr Video Playback Redesign — Implementation Audit

## Scope

Redesigned the local and external video playback chrome to follow the supplied cinematic reference images while preserving the existing Media3/ExoPlayer playback engine, progress/history, bookmarks, PiP, fullscreen and orientation behavior.

## Implemented

- Full-screen immersive video presentation with cinematic top/bottom scrims.
- Glass/translucent controls with subtle white borders and rounded surfaces.
- Reference-style top information area with title and secondary metadata.
- Reference-style top-right grouped controls:
  - Cast -> AirPlayr Cast sheet.
  - Audio track selector.
  - Subtitles toggle.
  - Equalizer -> existing Equalizer screen.
  - Playback speed indicator.
  - Bookmark.
  - Video queue.
  - More actions.
- Central previous / play-pause / next controls.
- Removed the visible 10-second rewind/forward icons from the video controls.
- Kept double-tap seeking as an interaction, so the useful gesture is preserved without cluttering the chrome.
- Thin cinematic seek bar with current/duration labels and tap/drag seeking.
- Bottom-left glass group for volume and lock.
- Bottom-right glass group for PiP, rotation and fullscreen.
- Temporary seek feedback overlay.
- Temporary vertical volume feedback overlay.
- Control auto-hide and tap-to-show/hide behavior.
- Subtitle rendering moved to a custom overlay so AirPlayr subtitle styling can be applied consistently.
- Subtitle customization sheet for size, color, position and background treatment.
- Audio-track selection using Media3 TrackSelectionOverride.
- Video queue store upgraded with current cursor, next/previous, add, play-next, remove, reorder and clear-up-next operations.
- Dedicated Video Queue sheet with:
  - current item
  - up-next list
  - play-now
  - move up/down
  - remove
  - clear up-next
  - add videos
  - search within the library when adding
- Queue actions added to relevant library/search/stats/feature media actions so videos no longer fall back to the music-only queue.
- Queue automatically advances to the next video when the current local video ends.
- Menu drawer Video Queue action now opens the real video queue instead of showing a placeholder message.
- External video playback receives the same cinematic control language and Cast/Equalizer connections.
- Added the missing `PreferencesRepository.setNowPlayingTheme()` implementation that the existing Settings screen already referenced.

## Explicitly not changed

- Music playback service behavior.
- Music queue behavior.
- Lyrics provider/repository behavior.
- Sleep timer behavior.
- Three-dots menu behavior outside the video player.
- Existing Equalizer screen implementation.
- Cast controller/session implementation.

## Verification performed

- Compared the working tree against the uploaded base ZIP and confirmed the changes are confined to the video playback/queue/navigation/preferences integration listed above.
- Balanced-brace/parenthesis checks passed for every changed Kotlin source file.
- Confirmed there are no remaining visible `Replay10`/`Forward10` video-player icons in the redesigned internal or external video screens.
- Confirmed the previous "video queue is coming" placeholder and the video/music-queue denial messages were removed from the affected UI paths.
- ZIP/package integrity will be checked after packaging.

## Build limitation

A full Android Gradle compilation could not be completed in this environment because the project wrapper requires Gradle 9.5.0 and the environment could not resolve `services.gradle.org` (`UnknownHostException`). Therefore this audit does **not** claim a successful APK build or device validation.

The source was inspected and statically checked, but final verification on an Android device/emulator is still required before calling the implementation production-verified.
