# AirPlayr Native Asset + Feature Parity Audit — 2026-09-14

## Sources inspected

1. Old Expo/React Native reference build: `AirPlayr-user-visible-fixed-2026-09-14(1).zip`
2. Native-only build under active migration: `AirPlayr-native-only-2026-09-14.zip`

The Expo package was treated as the feature/asset reference only. No Expo runtime or project metadata was copied into the final native project.

## Findings before this parity pass

The native-only package was structurally native, but it was not yet a complete visual/feature migration:

- The seven original Home feature-card SVG assets were absent.
- The source branding assets (`icon.png`, `adaptive-icon.png`, `splash-icon.png`) were absent from the native source tree.
- The native launcher/splash resource set was incomplete: `iconBackground` and `splashscreen_background` resource definitions were missing.
- The native `HomeScreen.kt` in the supplied native-only ZIP was incorrectly a duplicate `AirPlayrApp` implementation, so the imported `HomeScreen` symbol was not actually defined in that file.
- The Expo reference contained 33 user-visible routes (excluding the router layout/index bookkeeping); the native project exposed only a subset directly.
- The Expo Home feature rail (For You, time-of-day Jam/Mix, Rediscover, Rock Your World, My Playlists) was not represented visually in the native Home screen.
- The native video speed control only cycled through 0.5x–2x even though the reference contract supports 0.25x–4x.

## Work completed

### Branding and assets

Copied the exact reference branding into native Android resources/assets:

- AirPlayr source logo (`icon.png`) — exact SHA-256 match with the Expo source asset.
- Adaptive icon source (`adaptive-icon.png`).
- Splash source (`splash-icon.png`).
- Existing density-specific Android launcher assets.
- Existing density-specific splash logo assets.
- Corrected native `colors.xml` with `splashscreen_background` and `iconBackground`.
- Added Android 12 splash theme and `core-splashscreen` integration.
- Added `installSplashScreen()` to `MainActivity`.
- Preserved the eight original AirPlayr font files; their bytes match the Expo asset copies exactly.

### Feature-card artwork

Migrated all seven original SVG sources:

- `Media player-bro.svg`
- `Headphone-amico.svg`
- `Recording-pana.svg`
- `Playing Music-bro.svg`
- `Music-cuate.svg`
- `Playlist-pana.svg`
- `DJ boy-cuate.svg`

The SVG originals are kept in native `android/app/src/main/assets/feature-cards/`.

Native-rendered PNG versions were generated for Compose display in `drawable-nodpi`, while the original SVG sources remain available for future vector-quality rendering.

### Home UI

Rebuilt the native Home screen so it is a real Compose screen and not a duplicate launcher:

- AirPlayr logo header.
- Music/video counts.
- Search/queue/stats actions.
- History/Favorites/Most played/Shuffle quick actions.
- Horizontal feature-card rail.
- For You.
- Time-of-day feature card with the reference early-morning, late-morning, afternoon, evening, night and late-night variants.
- Rediscover.
- Rock Your World.
- My Playlists.
- Recently added songs.
- Recently added videos.
- Native mini now-playing control.

### Route/feature availability

Added native navigation coverage for every normalized Expo user-facing route found in the reference package:

- music
- videos
- settings
- playlists
- search
- player
- queue
- favorites
- history
- equalizer
- diagnostics
- smart-collections
- for-you
- tuned-in
- rediscover
- rediscover/rewind
- rock-your-world
- most-played
- my-playlists
- recent/{type}
- listening-watching-stats
- video-collections
- folders
- open
- album/{id}
- artist/{id}
- genre/{id}
- folder/{id}
- music-folder/{id}
- music-playlist/{id}
- playlist/{id}
- video/{id}

The native tree also has explicit index routes for Albums, Artists and Genres.

### Other parity correction

Native video speed now cycles through the full reference range, including 0.25x through 4x.

## Verification performed

- Compared Expo and native archive file inventories.
- Compared Expo and native route coverage after normalizing `[id]`/`[type]` to native navigation arguments.
- Verified all seven SVG feature assets exist in the final native tree.
- Verified the eight font files are byte-for-byte identical to the reference assets.
- Verified the AirPlayr logo source asset is byte-for-byte identical to the reference `icon.png`.
- Parsed all Android XML resources successfully.
- Ran Kotlin compiler parsing across the native source set; there were zero Kotlin syntax errors of the checked classes.
- Confirmed only one `HomeScreen` composable exists and it is now in the correct `com.airplayr.app.ui.screens` package.
- Confirmed the native launcher still points to `MainActivity`.
- Confirmed the final project contains no Expo/React/Node runtime metadata.

## Important remaining parity limits

This pass closes the **branding, artwork, Home feature-card, and route-availability gaps** that were discovered.

It does **not** truthfully claim that every internal interaction of every original Expo screen is pixel-for-pixel identical. Several advanced reference behaviors still require dedicated native implementations or physical-device validation, notably:

- synchronized lyrics fetching/editing UI
- sleep-timer finish-current/fade-out behavior
- full video gesture system
- video screenshot/frame-step implementation
- alternate audio-track UX
- production-grade crossfade
- full ReplayGain application
- some deep playlist/album/artist management interactions

Those are recorded as parity work rather than being mislabeled as complete.

## Build limitation

A real Gradle build was attempted. The environment cannot resolve `services.gradle.org`, so Gradle cannot download the wrapper distribution. Therefore no APK build or physical-device validation is claimed by this report.

## Final status

**Native Android assets:** PASS  
**Branding/icon/splash:** PASS  
**Feature-card artwork:** PASS  
**Home feature rail:** PASS  
**Expo route coverage:** PASS  
**Kotlin syntax gate:** PASS  
**Android XML gate:** PASS  
**APK build in this environment:** BLOCKED by network  
**Physical-device validation:** NOT PERFORMED
