# AirPlayr — Music Playback Full-Screen / Return-Path Fix

## Requested behavior
- Music playback must be a full screen again, not a persistent bottom sheet.
- It must still be dismissible by dragging downward.
- The playback header must respect the Android status bar.
- Actions opened from the playback three-dot menu must keep playback in the navigation stack so Back returns to Music Playback instead of Home.

## Changes made
1. Removed the app-level `musicPlayerSheetOpen` state and the `ModalBottomSheet` wrapper around `PlayerScreen`.
2. `player` is now a normal navigation destination, so it occupies the full screen.
3. Existing `navigate("player")` calls now navigate to that destination normally.
4. Removed the `inSheet` mode from `PlayerScreen` because playback is no longer rendered inside the shell sheet.
5. Added downward drag-to-dismiss to `PlayerScreen`. A downward drag greater than 120dp calls the existing `onBack()` navigation callback; shorter drags snap back.
6. Applied `statusBarsPadding()` to the full PlayerScreen content column, ensuring the header is below the status bar while the background remains full-screen.
7. Three-dot navigation actions (album, artist, album artist, folder) now naturally push their destination on top of the player route. Their existing Back behavior therefore returns to the PlayerScreen rather than Home.
8. Equalizer also remains a normal navigation push from PlayerScreen, so Back returns to playback.

## Verification
- Confirmed there are no remaining `musicPlayerSheetOpen` or `inSheet` references in AirPlayrApp/PlayerScreen.
- Confirmed `player` remains a dedicated navigation destination.
- Confirmed three-dot album/artist/folder actions still navigate through the PlayerScreen callback.
- Full Gradle compilation could not be executed in this environment because Gradle 9.5.0 is not cached and network access to `services.gradle.org` is unavailable. No claim of a successful APK build is made.
