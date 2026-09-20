# AirPlayr — Unresolved Issues Implementation Report

Date: 20 September 2026
Source: `AirPlayr_Phase1-12_completed_implementation.zip`

## Scope

This revision implements the unresolved items identified during the independent source audit. The existing project structure and previously completed work were preserved. Changes were made directly in the Android/Kotlin source rather than by editing audit claims.

## Implemented changes

### 1. Wallpaper / Material You dynamic-color refresh

- Removed the intentional 220 ms wallpaper-color debounce from `AirPlayrTheme.kt`.
- Wallpaper color callbacks are now coalesced onto the next main-loop turn without introducing a visible timer delay.
- Android 12+ dynamic light and dark Material You color schemes remain the source for dynamic theming.

### 2. Launch theme / splash re-entry

- `AppContainer` now reads the persisted launch theme synchronously from `airplayr_startup` SharedPreferences.
- `MainActivity` selects the light/dark splash theme from that synchronous mirror.
- Removed the Compose `setKeepOnScreenCondition` gate that waited for an asynchronous theme `StateFlow`.
- `PreferencesRepository.setTheme()` and reset now synchronously commit the startup mirror before updating DataStore.
- This removes the known async theme-read bottleneck from the splash lifecycle.

### 3. Three-dot / overflow control

- Removed the opaque circular `Surface` behind the three-dot control.
- Replaced it with a transparent 36 dp hit target.
- Added a 2 dp graphics-layer shadow so the white icon remains readable without a filled background.

### 4. Album / artist / artwork fallback treatment

- Added shared `AirPlayrArtworkTokens.fallbackIconAlpha = 0.52f`.
- Default artwork fallback tint now uses `onSurfaceVariant` at that shared alpha.
- Artist fallbacks in Home, Music, and Search use the same faint treatment.
- Album and generic artwork fallbacks use the same visual token instead of an opaque primary-color icon.

### 5. Recently played videos

- Video play history now persists `artworkUri ?: thumbnailUri`, ensuring history rows retain a renderable video image.
- Home history mapping now preserves video thumbnails separately from audio artwork.
- Recently-played video projection is explicitly filtered to video media, sorted newest-first, deduplicated by media ID, and bounded.

### 6. Genre / listening-watching statistics

- Genre backfill for existing libraries was shortened from 15 seconds to 1.8 seconds and remains background work so it does not block the first Home frame.
- `PlaybackStatsRepository.summary()` now resolves every referenced playback-event media ID from Room in 900-row chunks instead of relying only on the current UI media snapshot.
- This makes top-video and genre aggregation independent of a bounded Home library list.
- Activity-grid generation is now period-aware instead of always rendering exactly seven days.

### 7. Home listening/watching stats card

- The Home card's music/video play counts now come from durable playback-event aggregates rather than only the bounded playback-history rail.
- Top track/top video now come from the same durable aggregate path.
- Lightweight Home stats are persisted in a small presentation cache so last-known listening/watching values can paint immediately on re-entry.

### 8. All-screen warm re-entry / shell responsiveness

- Removed the artificial 700 ms existing-user synchronization delay from `AirPlayrApp`.
- Home already had an application-scoped persistent snapshot; this revision keeps it as the fast source for the first render and adds the persistent lightweight stats snapshot.
- Removed the async startup-theme gate and shortened non-initial navigation transitions to reduce perceptible re-entry latency.
- Existing `rememberSaveable` list/grid states and Navigation `saveState`/`restoreState` behavior remain intact.

### 9. Cast sheet

- Removed the full-screen Material scrim by setting the cast `ModalBottomSheet` scrim to transparent.

### 10. Bottom dock / MiniPlayer architecture

- The independently audited source already used `Scaffold(bottomBar = AirPlayrBottomDock(...))` and no longer wrapped the entire dock in the former opaque full-width `Surface`.
- This revision did not re-introduce an opaque dock layer.
- A physical-device visual pass is still required to prove the exact pixel-level rendering of the dock/MiniPlayer against the original device screenshot.

## Verification performed

### New focused verifier

`android/tools/unresolved-issues-verify.sh`

Result: **PASS (10/10)**

It verifies:

- wallpaper refresh has no 220 ms delay
- startup theme is synchronously mirrored
- splash has no async theme keep-on-screen gate
- cast scrim is transparent
- overflow control is transparent with a 2 dp shadow
- artwork fallback uses the shared faint token
- recently-played video history keeps artwork/thumbnail data
- stats resolve database media for large event sets
- stats activity grid is period-aware
- Material You dynamic light/dark foundation remains present

### Existing project checks

- `phase12-15-verify.sh`: **PASS**
- `phase3-static-check.sh`: **PASS**

The ZIP also retains the project's existing historical phase verifiers. Some of those older verifiers still report failures on unrelated legacy matcher expectations; they were not rewritten to manufacture a PASS.

### Build limitation

A final Gradle build was attempted with:

`./gradlew --offline :app:assembleDebug`

The build could not start because Gradle 9.5.0 is not present in the local wrapper cache and this environment has no network access to `services.gradle.org`.

Therefore:

- source/static implementation: verified
- project scripts relevant to these fixes: verified
- physical APK build: **not verified in this environment**
- emulator/device rendering and startup timings: **not verified in this environment**

## Honest status

The requested source-level fixes in this revision are implemented. The remaining verification gap is runtime/device confirmation, not an unimplemented source path.

I have intentionally not claimed that the exact 3–4 second startup target, zero-frame visual theme transition, or pixel-perfect dock transparency has been device-proven here because the APK could not be built in this environment.
