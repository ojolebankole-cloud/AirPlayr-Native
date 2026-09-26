# AirPlayr — Music/Genre/Video Folder/Queue Visual Fix Audit
Date: 2026-09-26

## Requested fixes
1. Video folders must open as their own screen, not inline underneath the Videos screen.
2. The folder screen should resemble the existing Recently Played-style collection screen and provide a floating Play button.
3. Genre cards under Music should visually follow the supplied reference: two-column cards, artwork-derived card colour, artwork offset/tilted at the right, readable genre/song count.
4. Music playback should have a swipe/fade transition when the current track changes.
5. Music Queue sheet in light mode should use the same clean sheet background treatment as Lyrics and remove the dark/now-playing background layers.

## Implementation

### Video folders
- Added `video-folder/{id}` as a real navigation destination.
- Folder rows now navigate to that destination instead of changing `selectedFolder` inside `VideoLibraryScreen`.
- Added `VideoFolderDetailScreen` with:
  - status-bar-safe header
  - folder name and video count
  - dedicated video list
  - floating Play button
  - queue creation from the folder before opening playback
- Folder playback uses `VideoQueueStore.setLoopingQueue(...)`, so next/previous remains based on that folder.
- Removed the old inline selected-folder state from `VideoLibraryScreen`.

### Genre cards
- Replaced the previous hash-based container colours with artwork-derived colours.
- Added `rememberArtworkDominantColor()` using the existing artwork bitmap cache.
- Cards now use a 2-column, rounded reference-style layout.
- The first genre artwork is displayed large, offset toward the lower-right and slightly rotated.
- Text colour automatically switches between black/white based on artwork-derived luminance for readability.

### Music track transition
- Wrapped the main PlayerScreen artwork in `AnimatedContent` keyed by current track ID.
- Track changes now use a horizontal incoming/outgoing slide combined with fade.
- Existing playback controls/state were not replaced.

### Music Queue light-mode theming
- Removed `PlaybackBackground` from `MusicQueueSheet`.
- Removed its dependency on `NowPlayingTheme` and `PreferencesRepository`.
- Queue now uses `surfaceContainerLow`, matching the normal Lyrics sheet treatment.
- Removed the translucent queue content layer that allowed the old dark/now-playing background to bleed through.
- Queue interactions and drag/reorder behavior remain intact.

## Verification
- `android/tools/phase3-static-check.sh` — PASS.
- Custom checks confirmed:
  - no remaining inline `selectedFolder`/`selectedFolderGroup` state in `VideoLibraryScreen`
  - `video-folder/{id}` route exists
  - genre artwork palette helper is wired into the Genre cards
  - PlayerScreen uses `AnimatedContent` for track changes
  - MusicQueueSheet no longer uses `PlaybackBackground`

## Build limitation
A real Kotlin/Gradle compilation was attempted with:
`./gradlew :app:compileDebugKotlin --offline --stacktrace`

It could not reach compilation because the required Gradle 9.5.0 distribution was not cached and the environment could not resolve `services.gradle.org`. Therefore this audit does **not** claim that a Gradle APK build passed.

## Visual reference notes
The supplied genre reference was used as the target direction: medium/dark artwork-derived coloured cards, two columns, large simple text on the left, and a cropped/tilted artwork image on the right.
The supplied queue screenshot was treated as the problem state: the old queue had a darkened now-playing background visible through the sheet. The new queue removes that layer and uses the same clean sheet surface approach as Lyrics.
