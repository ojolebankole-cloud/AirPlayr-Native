# AirPlayr — Existing-User Startup Performance Implementation Audit

## Scope
Implemented on the current Android-Native project only, using the existing-user launch behavior observed in the supplied recordings as the performance target. No Expo project was inspected or modified during this implementation pass.

## Goal
When an existing user returns to AirPlayr after previously playing music, show the cached Home and MiniPlayer state as early as possible, then perform freshness work in the background. Avoid making MediaStore scans, statistics calculations, secondary playback engines, or redundant controller connections compete with the first Home frame.

## Implemented changes

### 1. Cheap app-container construction
`AppContainer.kt`
- Made Room database and heavier repositories lazy.
- Made CastController, video controller and DSP repository lazy.
- Moved WorkManager scheduling out of the Activity construction path.
- Supplemental SAF-root scans run after a short delay for existing users instead of competing with first Home rendering.
- Existing-user MediaStore delta refresh supports a startup delay.

### 2. Existing-user background freshness
`AirPlayrApp.kt`
- Existing users are marked startup-ready from the persisted initial-scan flag.
- The Home screen is allowed to render before `requestLibrarySynchronization()` starts the MediaStore delta refresh.
- Freshness still occurs; it is not removed or faked.

### 3. One playback controller for Home + dock
`AirPlayrApp.kt`, `HomeScreen.kt`
- Home no longer creates a second `PlaybackController` ViewModel.
- Home consumes the same app-level playback controller used by the bottom dock.
- This removes a redundant MediaController/service connection from Home startup.

### 4. Cached MiniPlayer hydration
`PlaybackController.kt`, `QueueStore.kt`
- QueueStore persists artwork in the queue snapshot.
- The decoder remains backward-compatible with the previous seven-field queue format.
- Existing-user playback state is read from DataStore on `Dispatchers.IO` before the MediaController connection.
- Cached title, artist, album, artwork, duration, position, queue size, shuffle/repeat and play/pause intent can populate the MiniPlayer before the service connection finishes.
- The controller yields before connecting so it does not occupy the same critical scheduling moment as the first Compose frame.

### 5. Home data work moved off the UI thread
`HomeScreen.kt`, `LibraryViewModel.kt`
- Whole-library audio/video flows are no longer eagerly started before a screen needs them; they use `WhileSubscribed`.
- Expensive Home derived data (lookup map, recent rails, artists/albums/video rails) is calculated on `Dispatchers.Default` via `produceState`.
- The all-time statistics summary is intentionally deferred so its database work does not compete with the first Home frame.
- The existing Home data and behavior remain; this is a scheduling change, not a data-removal change.

### 6. Secondary crossfade ExoPlayer is lazy
`AirPlayrPlaybackService.kt`
- The second local ExoPlayer is no longer created every time the playback service starts.
- It is created only when crossfade processing actually needs a second player.
- Current pause-at-end and DSP state are applied when the second player is created so crossfade behavior is preserved.

### 7. Correct persisted playback intent when background playback is disabled
`AirPlayrPlaybackService.kt`
- When the task is removed and background playback is disabled, the player is paused before the snapshot is saved.
- This prevents a stale `playWhenReady=true` snapshot from making a relaunch look like playback was still active.

## Resulting existing-user startup flow

```text
Open AirPlayr
    |
    +--> lightweight app shell construction
    |
    +--> Home first frame
    |      |
    |      +--> cached MiniPlayer snapshot can appear immediately
    |      +--> feature/header shell is usable
    |      +--> cached Room data populates progressively
    |
    +--> after startup head-start
           |
           +--> MediaStore delta freshness
           +--> supplemental SAF refresh
           +--> deferred statistics calculation
```

The expensive work still happens. It is moved behind the first usable UI rather than being treated as a prerequisite for showing Home.

## Verification performed
- Kotlin source structural delimiter check across all application `.kt` files: PASS.
- Required native assets/static project check (`phase3-static-check.sh`): PASS.
- Custom checks confirm the Home duplicate playback controller was removed and the app-level controller is passed into Home.
- Custom checks confirm lazy secondary player references are nullable/safe and creation is isolated to `ensureSecondaryPlayer()`.
- Queue snapshot format is backward-compatible with both 7-field and new 8-field rows.
- ZIP integrity checked after packaging.

## Build/device limitation
The Android Gradle build was attempted with `./gradlew --offline :app:compileDebugKotlin --stacktrace`. The wrapper still attempted to obtain Gradle 9.5.0 and failed because `services.gradle.org` is unreachable in this environment. Therefore this audit does **not** claim an APK build/install or physical-device timing measurement.

## Important honesty note
The implementation is designed to reproduce the second recording's *startup strategy* for existing users: render cached Home early and refresh asynchronously. Exact frame-by-frame timing on a physical Tecno device still requires building/installing this revision and recording one existing-user launch for measurement.
